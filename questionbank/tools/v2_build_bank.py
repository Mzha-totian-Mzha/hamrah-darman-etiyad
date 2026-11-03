# -*- coding: utf-8 -*-
"""بازسازی بانک سؤال — نسخهٔ ۲ روی متنِ تمیزِ pdftotext.

منبع متن = pdftotext (ترتیب منطقی سالم) به‌جای pypdf (معکوس/تخریب)
پارسر سؤال‌های تستی چندفرمته: حرفی (الف()، عددی ()1)، میان‌خطی
کلیدها: دایرهٔ وکتوری (exam) + هایلایت زرد (jam3/کلید۴) + پاسخِ رنگیِ متن
سؤال‌های جای‌خالیِ بی‌گزینه → سبد تشریحی (با پاسخِ رنگیِ پاسخ‌نامه)
اجرا: python3 scripts/v2_build_bank.py
"""
import glob
import io
import json
import os
import re
import sys
import unicodedata
from collections import Counter, defaultdict
from difflib import SequenceMatcher

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_bank import (  # noqa: E402
    CHAR_MAP, cut_boiler, dedup_key, fix_words, refs_of, strip_lead,
    quality_of, quality_q, smart_fix,
)
import decode_answer_keys as dk  # noqa: E402
import pdfplumber  # noqa: E402

TXT = "/home/z/my-project/data/questionbank/text_v2"
OUT_BANK = "/home/z/my-project/data/questionbank/bank.json"
OUT_SUM = "/home/z/my-project/data/questionbank/bank_summary.json"
OLD_KEYS = "/home/z/my-project/data/questionbank/answer_keys.json"

# --------------------------------------------------------------- نرمال‌سازی سطر

def clean_line(t: str) -> str:
    t = unicodedata.normalize("NFKC", t or "")
    t = t.translate(CHAR_MAP)
    t = re.sub(r"[\u064B-\u065F\u0670]", "", t)  # اعراب
    t = t.replace("\u2013", "-").replace("\u2014", "-").replace("\u2012", "-").replace("\u2015", "-")
    # پرانتزِ آینه‌شدهٔ ارجاع صفحه: «)صفحه (۱۰» → «(صفحه ۱۰»
    t = re.sub(r"\)\s*(صفحه|ص)\s*\(", r"(\1 ", t)
    t = re.sub(r"\)\s*(صفحه|ص)\s+(?=[\d۰-۹])", r"(\1 ", t)
    t = re.sub(r"\(\s*(صفحه|ص)\s+([\d۰-۹]+)\s*\)?", r"(\1 \2)", t)
    t = re.sub(r"[ \t]+", " ", t)
    return t.strip()

BOILER_LINE = re.compile(
    r"^("
    r"تاریخ|مدت\s*زمان|منابع\s*[:‌]|نام\s*[:‌]|نام\s*و\s*نام|نمایندگی|شماره\s*عضویت|شماره\s*لژیون"
    r"|کنگره\s*(۶۰|60)|Congress\s*6?0|Congrss\s*6|Lt\s*s\s*Cr|بیایید\s*این\s*آتش|بیا\s*یی\s*د"
    r"|به\s*نام\s*خالق\s*عشق|م\s*خا\s*ق\s*ق|(?:\()?زیباترین\s*عمل|Let\s*us\s*Curb"
    r"|آزمون\s*(?:داخلی|جامع|نهایی|دوم|سوم|چهارم|پنجم|ششم|هفتم|هشتم|نهم|دهم)[^\n]{0,80}"
    r"|موفق\s*باشید|پاسخنامه|برگ\s*پاسخنامه|صفحه\s*[\d۰-۹]+\s*$"
    r"|عنوان\s*و\s*نام\s*پدیدآور|مشخصات\s*(نشر|ظاهری)|شابک|یادداشت|موضوع|رده\s*بندی"
    r"|درود\s*و\s*سپاس|بسم\s*(الله|اهلل)"
    r"|۱۴وادی|۱۴مقاله|۱۴\s*وادی|۱۴\s*مقاله"
    r").*",
)

def is_boiler(t: str) -> bool:
    if not t:
        return True
    if BOILER_LINE.match(t):
        return True
    if re.fullmatch(r"[\s.ـ_…\-–—*(\)=،,]+", t):
        return True
    if re.fullmatch(r"[\d۰-۹]{1,3}", t):
        return True
    return False

def fix_blanks(t: str) -> str:
    return re.sub(r"\.{3,}|_{2,}|…+", "...........", t)

# وارونگی لیگاتر «لا» در PDFهای وردی — فقط واژه‌های بی‌ابهام
LAM_ALEF_FIX = [
    ("مشکالت", "مشکلات"), ("الزم", "لازم"), ("کامال", "کاملاً"),
    ("تالش", "تلاش"), ("اطالعات", "اطلاعات"), ("معموال", "معمولاً"),
    ("سالمت", "سلامت"), ("باال", "بالا"), ("پاالیش", "پالایش"),
    ("عالوه", "علاوه"), ("عاالی", "عالی"),
    ("وصاال", "وصال"), ("ساالر", "سایر"), ("عااید", "عاید"),
]

def fix_lam_alef(t: str) -> str:
    if not t:
        return t
    for a, b in LAM_ALEF_FIX:
        t = re.sub(r"(?<![\u0600-\u06FF])" + a, b, t)
    return t

def merge_page_breaks(lines: list[str]) -> list[str]:
    """«(صفحه» در انتهای سطر و عدد در ابتدای سطر بعد — دوباره بچسبان."""
    out = []
    for ln in lines:
        if out and re.search(r"\(\s*(صفحه|ص)\s*$", out[-1]) and re.match(r"^\s*[\d۰-۹]", ln):
            out[-1] = out[-1].rstrip() + " " + ln.lstrip()
        else:
            out.append(ln)
    return out

# --------------------------------------------------------------- شمارهٔ سؤال

QNUM_RX = re.compile(r"^\s*[-–ـ.,،(]*\s*(?:[.ـ_…]{1,}\s*)?[-–ـ]?\s*([\d۰-۹]{1,3})\s*[-–—_.)ـ]?\s*(?=\S)")
DOT_Q = re.compile(r"^\s*\.\s*([\d۰-۹]{1,3})\s*(?=\S)")
DASH_Q = re.compile(r"^\s*[-–ـ]\s*([\d۰-۹]{1,3})\s*(?=\S)")
NUMDASH_Q = re.compile(r"^\s*([\d۰-۹]{1,3})\s*[-–ـ]\s*(?=\S)")
DASH_ONLY = re.compile(r"^\s*[-–ـ]\s+(?=\S)")

def _latin(v: str) -> str:
    return v.translate(str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789"))

def match_qnum(line: str, expected: int | None):
    """(شماره، باقی سطر، نقطه‌چینِ پیش از شماره) — با قید توالی."""
    mat = QNUM_RX.match(line)
    if not mat:
        return None
    num = int(_latin(mat.group(1)))
    if expected is not None and not (expected - 3 <= num <= expected + 6):
        return None  # شمارهٔ پرت (صفحه/سال/…)
    rest = line[mat.end():].strip("ـ ")
    had_dots = bool(re.match(r"^\s*[-–ـ.,،(]*\s*[.ـ_…]{2,}", line))
    return num, rest, had_dots, 0

def match_qnum_loose(line: str):
    """برای فایل‌های تشریحی: لنگر نقطه/خط/عدد-خط کافی است؛ توالی سخت‌گیرانه نمی‌خواهیم."""
    mat = DOT_Q.match(line) or DASH_Q.match(line) or NUMDASH_Q.match(line)
    if not mat:
        return None
    num = int(_latin(mat.group(1)))
    if not (1 <= num <= 500):
        return None
    return num, line[mat.end():].strip("ـ "), False, 0

# --------------------------------------------------------------- گزینه‌ها

LET = "(?:الف|ب|ج|د)"
OPT_LEAD_LET = re.compile(r"^\s*\(?\s*(" + LET + r")\s*[\)\(:ـ\-]\s*(?=\S)")
OPT_LEAD_DIG = re.compile(r"^\s*[\)\(]\s*([1-4۱۲۳۴])\s*[\)\(:ـ]?\s*(?=\S)")
OPT_IN_LET = re.compile(r"(?:^|[\s(])(" + LET + r")\s*[\)\(:ـ\-]\s*(?=\S)")
OPT_IN_DIG = re.compile(r"(?:^|[\s\)\(])([1-4۱۲۳۴])\s*[\)\(]\s*(?=\S)")
ORDER = {"الف": 1, "ب": 2, "ج": 3, "د": 4}
NUM2LET = {"1": "الف", "2": "ب", "3": "ج", "4": "د", "۱": "الف", "۲": "ب", "۳": "ج", "۴": "د"}

def _opt_key(ch: str, mode: str) -> str:
    return ch if mode == "let" else NUM2LET.get(_latin(ch), ch)

def _inline_split(text: str, mode: str) -> tuple[str, list]:
    """برش گزینه‌های درون‌خطی → (سر متن، [(کلید، متن)]) با توالی صعودی."""
    rx = OPT_IN_LET if mode == "let" else OPT_IN_DIG
    marks = []
    last = 0
    for mo in rx.finditer(text):
        ch = mo.group(1)
        v = ORDER[ch] if mode == "let" else int(_latin(ch))
        if v > last:
            marks.append((mo.start(1), mo.end(), ch))
            last = v
    if len(marks) < 2:
        return text, []
    head = text[: marks[0][0]].strip()
    out = []
    for idx, (st, en, ch) in enumerate(marks):
        end = marks[idx + 1][0] if idx + 1 < len(marks) else len(text)
        seg = text[en:end].strip(" ،,؛:.-")
        if seg:
            out.append((ch, seg))
    return head, out

def collect_options(text: str) -> tuple[str, dict]:
    """سطرِ تلفیقی → (سؤالِ سر، گزینه‌ها) — حرفی یا عددی."""
    for mode in ("let", "dig"):
        head, pairs = _inline_split(text, mode)
        if len(pairs) >= 2:
            opts = {}
            for ch, seg in pairs:
                opts[_opt_key(ch, mode)] = seg
            return head, opts
    return text, {}

# --------------------------------------------------------------- پارسر تستی

def parse_mcq_exam(text: str) -> list[dict]:
    lines = merge_page_breaks([clean_line(x) for x in text.split("\n")])
    items = []
    cur = None
    expected = 1
    in_opts = False

    def flush():
        nonlocal cur
        if cur and cur["q"]:
            items.append({"n": cur["n"], "q": " ".join(cur["q"]), "opts": dict(cur["opts"])})
        cur = None

    def new_item(num, rest, had_dots):
        nonlocal cur, expected, in_opts
        if cur:
            flush()
        cur = {"n": num, "q": [], "opts": {}}
        if num is not None:
            expected = num + 1
        in_opts = False
        if not rest:
            return
        if had_dots:
            cur["q"].append("...........")
        head, opts = collect_options(rest)
        if opts:
            cur["opts"] = opts
            in_opts = True
            if head.strip(" ،,؛"):
                cur["q"].append(head)
        else:
            cur["q"].append(rest)

    for ln in lines:
        if is_boiler(ln):
            continue
        mq = match_qnum(ln, expected if cur is not None or items else None)
        if mq:
            new_item(mq[0], mq[1], mq[2])
            continue
        if cur is None:
            if DASH_ONLY.match(ln):        # سؤالِ بی‌شمارهٔ «- متن»
                new_item(None, DASH_ONLY.sub("", ln).strip(), False)
            continue
        # گزینهٔ حرفیِ آغازسطر
        mo = OPT_LEAD_LET.match(ln)
        if mo:
            ch = mo.group(1)
            seg = ln[mo.end():].strip()
            head, pairs = _inline_split(seg, "let")
            cur["opts"][ch] = (cur["opts"].get(ch, "") + " " + head).strip()
            for c2, s2 in pairs:
                cur["opts"][c2] = s2
            in_opts = True
            continue
        # گزینهٔ عددیِ آغازسطر: «)1ترک…»
        mo = OPT_LEAD_DIG.match(ln)
        if mo:
            key = NUM2LET.get(_latin(mo.group(1)), mo.group(1))
            seg = ln[mo.end():].strip()
            head, pairs = _inline_split(seg, "dig")
            cur["opts"][key] = (cur["opts"].get(key, "") + " " + head).strip()
            for c2, s2 in pairs:
                cur["opts"][NUM2LET.get(_latin(c2), c2)] = s2
            in_opts = True
            continue
        # شمارهٔ سؤال بعدی (اگر قید توالی در فراخوان اول ناموفق ماند)
        mq2 = match_qnum(ln, expected)
        if mq2:
            new_item(mq2[0], mq2[1], mq2[2])
            continue
        # سطر ادامه
        head, opts = collect_options(ln)
        if len(opts) >= 2:
            for c, s in opts.items():
                cur["opts"][c] = (cur["opts"].get(c, "") + " " + s).strip() if c in cur["opts"] else s
            in_opts = True
        elif in_opts and cur["opts"]:
            last = list(cur["opts"])[-1]
            cur["opts"][last] = (cur["opts"][last] + " " + ln).strip()
        else:
            cur["q"].append(ln)
    flush()
    return items

# --------------------------------------------------------------- پارسر تشریحی

def _qa_block(num, parts):
    body = fix_blanks(cut_boiler(fix_words(" ".join(p for p in parts if p))))
    body = fix_lam_alef(body)
    if len(body) < 12:
        return None
    n2, body = strip_lead(body)
    refs, body = refs_of(body)
    q, a = body, ""
    i = body.find("؟")
    if i != -1 and i < len(body) - 1:
        q, a = body[: i + 1], body[i + 1:].strip(" ،,؛.")
    q = re.sub(r"\s+", " ", q).strip(" ،,؛.")
    if len(q) < 10:
        return None
    return {"n": num or n2, "q": q, "a": re.sub(r"\s+", " ", a), "refs": refs}

def parse_qa_dot(text: str) -> list[dict]:
    """قالب «.۱۱ سؤال؟ / پاسخ…» یا «-۱۱سؤال؟ / پاسخ…» — وادی/مقاله/۸۵-۹۵/ناقص."""
    lines = merge_page_breaks([clean_line(x) for x in text.split("\n")])
    out, cur = [], None
    for ln in lines:
        if is_boiler(ln):
            continue
        mq = match_qnum_loose(ln)
        if mq and (cur is None or mq[0] >= cur["n"] - 30):
            if cur:
                b = _qa_block(cur["n"], cur["parts"])
                if b:
                    out.append(b)
            cur = {"n": mq[0], "parts": [mq[1]]}
        elif cur is not None:
            cur["parts"].append(ln)
    if cur:
        b = _qa_block(cur["n"], cur["parts"])
        if b:
            out.append(b)
    return out

def parse_qa_inline(text: str) -> list[dict]:
    """قالب راهنما: «-۱۱ سؤال؟ پاسخ.» — پاسخ در همان سطر یا سطرهای بعد."""
    lines = merge_page_breaks([clean_line(x) for x in text.split("\n")])
    out, cur = [], None
    for ln in lines:
        if is_boiler(ln):
            continue
        mq = match_qnum_loose(ln)
        if mq:
            if cur:
                b = _qa_block(cur["n"], cur["parts"])
                if b:
                    out.append(b)
            cur = {"n": mq[0], "parts": [mq[1]]}
        elif cur is not None:
            if re.match(r"^(پرسش|سؤالات|سوالات|سوال و همراه جواب|سوال و جواب|وادی|متن|بخش)\b", ln):
                b = _qa_block(cur["n"], cur["parts"])
                if b:
                    out.append(b)
                cur = None
            else:
                cur["parts"].append(ln)
    if cur:
        b = _qa_block(cur["n"], cur["parts"])
        if b:
            out.append(b)
    return out

def parse_prose_qa(text: str) -> list[dict]:
    """متن پیوسته: فقط جفت‌های صریح «سؤال؟ توضیح»."""
    text = fix_blanks(cut_boiler(fix_words(clean_line(text))))
    sents = re.split(r"(?<=[.؟!؛])\s+", text)
    out = []
    i = 0
    while i < len(sents):
        s = sents[i].strip()
        if s.endswith("؟") and 15 <= len(s) <= 300:
            a_parts, j = [], i + 1
            while j < len(sents) and not sents[j].strip().endswith("؟"):
                a_parts.append(sents[j].strip())
                if sum(len(x) for x in a_parts) > 420:
                    break
                j += 1
            a = re.sub(r"\s+", " ", " ".join(a_parts)).strip(" ،,؛.")
            if len(a) >= 25:
                out.append({"n": None, "q": s, "a": a, "refs": []})
                i = j
                continue
        i += 1
    return out

# --------------------------------------------------------------- کلیدها (دایره + هایلایت + رنگ)

def accent_answers(path: str) -> tuple[dict, dict]:
    """(کلیدهای دایره‌ای، متنِ پاسخِ رنگیِ هر شماره) — رنگِ نشانگر خودکار تشخیص داده می‌شود."""
    keys, qtext = dk.decode(path)
    colored: dict[str, str] = {}
    with pdfplumber.open(path) as pdf:
        for page in pdf.pages:
            words = page.extract_words()
            q_rows = []
            for w in words:
                t = dk.norm(w["text"])
                mq = dk.QNUM.match(t)
                if mq and w["x0"] > page.width * 0.72:
                    q_rows.append((w["top"], str(int(_latin(mq.group(1))))))
            q_rows.sort()
            # رنگِ شاخصِ صفحه: پرتکرارترینِ غیرمشکی
            cc = Counter()
            for ch in page.chars:
                c = ch.get("non_stroking_color")
                if isinstance(c, (tuple, list)) and len(c) >= 3:
                    r, g, b = (tuple(c) + (0, 0, 0))[:3]
                    if max(r, g, b) - min(r, g, b) > 0.3:
                        cc[(round(r, 2), round(g, 2), round(b, 2))] += 1
            if not cc:
                continue
            accent = cc.most_common(1)[0][0]

            def is_accent(c):
                if not isinstance(c, (tuple, list)) or len(c) < 3:
                    return False
                r, g, b = (tuple(c) + (0, 0, 0))[:3]
                return (round(r, 2), round(g, 2), round(b, 2)) == accent

            rows = defaultdict(list)
            for ch in page.chars:
                if is_accent(ch.get("non_stroking_color")):
                    rows[round(ch["top"])].append(ch)
            for top, chs in rows.items():
                chs.sort(key=lambda c: -c["x0"])  # RTL
                s = dk.logical("".join(c["text"] for c in chs)).strip()
                if len(s) < 4:
                    continue
                cand = [q for q in q_rows if q[0] <= top + 4]
                if not cand:
                    continue
                qy, qnum = max(cand, key=lambda q: q[0])
                if top - qy > 160:
                    continue
                colored[qnum] = (colored.get(qnum, "") + " " + s).strip()
    return keys, colored

QNUM2 = re.compile(r"^[-–—ـ]?\s*([\d۰-۹]{1,3})\s*[-–—ـ]?$")

def highlight_answers(path: str) -> dict:
    """کلید از گزینه‌های هایلایت‌زرد (jam3 / کلید آزمون چهارم)."""
    keys = {}
    with pdfplumber.open(path) as pdf:
        for page in pdf.pages:
            words = page.extract_words()
            q_rows = []
            for w in words:
                t = dk.norm(w["text"])
                mq = QNUM2.match(t)
                if mq and w["x0"] > page.width * 0.68:
                    q_rows.append((w["top"], str(int(_latin(mq.group(1))))))
            q_rows.sort()
            hl = []
            for r in page.rects:
                c = r.get("non_stroking_color")
                if isinstance(c, (tuple, list)) and len(c) >= 3:
                    cr, cg, cb = (tuple(c) + (0, 0, 0))[:3]
                    if cr > 0.75 and cg > 0.75 and cb < 0.45:  # زرد
                        hl.append(r)
            for r in hl:
                cx, cy = (r["x0"] + r["x1"]) / 2, (r["top"] + r["bottom"]) / 2
                best, best_d = None, 1e9
                for w in words:
                    t = dk.norm(w["text"]).strip("()«» ")
                    letter = t if t in ORDER else (NUM2LET.get(t) or NUM2LET.get(_latin(t)))
                    if not letter:
                        continue
                    wx, wy = (w["x0"] + w["x1"]) / 2, (w["top"] + w["bottom"]) / 2
                    d = ((wx - cx) ** 2 + (wy - cy) ** 2) ** 0.5
                    if d < best_d:
                        best, best_d = letter, d
                if best is None or best_d > 40:
                    continue
                cand = [q for q in q_rows if q[0] <= cy + 4]
                if not cand:
                    continue
                qy, qnum = max(cand, key=lambda q: q[0])
                if cy - qy > 120:
                    continue
                keys[qnum] = best
    return keys

# --------------------------------------------------------------- پروفایل فایل‌ها

def read_txt(stem: str) -> str:
    p = os.path.join(TXT, stem + ".txt")
    return io.open(p, encoding="utf-8").read() if os.path.exists(p) else ""

MCQ_FILES = [
    "3سوال", "555سوال", "8سوال", "exam01", "exam02", "exam03", "exam033", "exam04",
    "exam05", "exam06", "exam066", "exam07", "exam08", "exam09", "exam10",
    "آزمون جامع همسفران آقا 1404", "آزمون داخلی همسفران آقا بهمن ماه (1)",
    "آزمون داخلی همسفران آقا شهریور ماه(1)", "آزمون دوازدهم ویرایش", "آزمون دوم",
    "آزمون سوم", "آزمون مرداد ماه 1404 (1)", "آزمون نهایی همسفران آقا 1404 - Copy",
    "آزمون نهایی همسفران آقا 144504", "آزمون همسفران آقا اردیبهشت ماه 1404",
    "آزمون همسفران آقا تهران 14030908", "آزمون همسفران آقا تیر ماه 1404",
    "آزمون همسفران آقا خرداد ماه", "آزمون چهاردهم -", "آزمون چهارم", "آزمون یکم",
    "آزمون", "بهمن ماه", "سؤال اصلاح شده", "سؤالات", "سؤالق", "سو55ال", "سوا5555لات",
    "سوال", "سوال44", "سوال55", "سوال آزمون چهارم", "سوالات2", "سوالات(3) (2)",
    "نهایی آزمون جامع سوم ، 18 مرداد", "آزمون شرح وظایف (مرزبانان)",
]
QA_DOT_FILES = [
    "سوال از صفحه 85 الی 95 چهارده مقاله", "سوالات 14 وادی عشق_ناقص",
    "سوالات 14 وادی عشق_نهایی", "سوالات از 14 مقاله", "سوالات از 14 مقاله -2",
    "سوالات از 14 وادی - وادی 14",
]
QA_INLINE_FILES = ["نمونه سؤالات امتحان راهنما", "راهنما و راهنمایی"]
QA_PROSE_FILES = [
    "(ok)جهان‌بینی 1", "اصلاحیه پروتکل  DST", "پروتکل DST", "مثلث‌های آموزشی کنگره ۶۰",
    "راهنمای تازه واردین", "۶۰ درجه", "راهنما و راهنمایی",
]

RAW_DIR = "/home/z/my-project/data/questionbank/raw"

def _norm_stem(s: str) -> str:
    s = re.sub(r"[^\w\u0600-\u06FF]+", "_", s)
    return re.sub(r"_+", "_", s).strip("_")

def load_v1_raw() -> dict:
    """آیتم‌های خام v1 (pypdf) برای پُرکردن شکاف‌های پارس v2."""
    out = {}
    for p in sorted(glob.glob(os.path.join(RAW_DIR, "*.json"))):
        try:
            d = json.load(io.open(p, encoding="utf-8"))
        except Exception:
            continue
        out.setdefault(d.get("kind"), {})[_norm_stem(os.path.basename(p)[:-5])] = d.get("items", [])
    return out

ANSWER_PAIRS = {
    "exam01_answers": "exam01", "exam02_answers": "exam02", "exam03_answers": "exam03",
    "exam04_answers": "exam04", "exam05_answers": "exam05", "exam06_answers": "exam06",
    "exam08_answers": "exam08",
    "نهایی_پاسخ_نامه_آزمون_جامع_سوم_،_18_مرداد": "نهایی آزمون جامع سوم ، 18 مرداد",
    "کلید آزمون چهارم": "__pick4__",
}
HIGHLIGHT_FILES = {"نهایی_پاسخ_نامه_آزمون_جامع_سوم_،_18_مرداد", "کلید آزمون چهارم"}

def category2(stem: str, q: str) -> str:
    f = stem
    if "وادی" in f or "عشق" in f:
        return "۱۴ وادی عشق"
    if "مقاله" in f:
        return "۱۴ مقاله"
    if "راهنما" in f and "وادی" not in f and "مقاله" not in f:
        return "راهنما و راهنمایی"
    if "شرح" in f and "وظایف" in f:
        return "شرح وظایف"
    if "جهان" in f:
        return "جهان‌بینی"
    if "درجه" in f:
        return "۶۰ درجه"
    if "پروتکل" in f or "DST" in f:
        return "پروتکل DST"
    for b, cat in [("کتاب عشق", "۱۴ وادی عشق"), ("جزوه راهنما", "راهنما و راهنمایی"),
                   ("چهارده مقاله", "۱۴ مقاله"), ("۱۴ مقاله", "۱۴ مقاله"),
                   ("پروتکل", "پروتکل DST"), ("جهان بینی", "جهان‌بینی"),
                   ("جهان‌بینی", "جهان‌بینی"), ("۶۰ درجه", "۶۰ درجه"), ("60 درجه", "۶۰ درجه")]:
        if b in q:
            return cat
    if "همسفر" in f:
        return "آزمون همسفران"
    return "آزمون عمومی"

def _similarity(qt: dict, items: list) -> float:
    import random
    random.seed(1)
    if not qt or not items:
        return 0.0
    nums = random.sample(list(qt.keys()), min(8, len(qt)))
    r = 0.0
    for n in nums:
        snip = qt[n][:120]
        best = max((max(SequenceMatcher(None, it["q"][:120], snip).ratio(),
                        SequenceMatcher(None, it["q"][:120], snip[::-1]).ratio())
                    for it in items), default=0)
        r += best
    return r / max(len(nums), 1)

def main() -> None:
    mcq_raw, qa_raw = {}, {}
    for stem in MCQ_FILES:
        t = read_txt(stem)
        if t.strip():
            mcq_raw[stem] = parse_mcq_exam(t)
    for stem in QA_DOT_FILES:
        t = read_txt(stem)
        if t.strip():
            qa_raw[stem] = parse_qa_dot(t)
    for stem in QA_INLINE_FILES:
        t = read_txt(stem)
        if t.strip():
            qa_raw.setdefault(stem, []).extend(parse_qa_inline(t))
    for stem in QA_PROSE_FILES:
        t = read_txt(stem)
        if t.strip():
            qa_raw.setdefault(stem, []).extend(parse_prose_qa(t))

    # ---------- اتحاد با rawهای v1 برای شکاف‌های پارس v2
    v1raw = load_v1_raw().get("mcq", {})
    filled = 0
    for stem in MCQ_FILES:
        its = mcq_raw.get(stem) or []
        v1its = v1raw.get(_norm_stem(stem)) or []
        if not v1its:
            continue
        have = {x["n"] for x in its if x["n"]}
        for it in v1its:
            q = cut_boiler(fix_words(smart_fix(it.get("q", ""))))
            n, q = strip_lead(q)
            n = it.get("n") or n
            if n and n in have:
                tgt = next((x for x in its if x["n"] == n), None)
                v1opts = {}
                for k, v in (it.get("opts") or {}).items():
                    v = cut_boiler(fix_words(smart_fix(v)))
                    v = re.sub(r"^[\)\(ـ]\s*", "", v).strip()
                    if v:
                        v1opts[k] = v
                if tgt is not None and len(tgt["opts"]) < 4 and len(v1opts) > len(tgt["opts"]):
                    for k, v in v1opts.items():
                        tgt["opts"].setdefault(k, v)
                    filled += 1
                continue
            opts = {}
            for k, v in (it.get("opts") or {}).items():
                v = cut_boiler(fix_words(smart_fix(v)))
                v = re.sub(r"^[\)\(ـ]\s*", "", v).strip()
                if v:
                    opts[k] = v
            if len(q) < 12 or len(opts) < 2:
                continue
            its.append({"n": n, "q": q, "opts": opts})
            if n:
                have.add(n)
            filled += 1
        mcq_raw[stem] = its
    print(f"اتحاد با v1: {filled} آیتم/گزینه پر شد")

    print("— پارس اولیه —")
    for stem in MCQ_FILES:
        its = mcq_raw.get(stem) or []
        full = sum(1 for x in its if len(x["opts"]) >= 4)
        if its:
            print(f"  MCQ {stem[:34]:36s} n={len(its):3d} کامل۴گزینه={full}")
    for stem, its in qa_raw.items():
        wa = sum(1 for x in its if len(x.get("a", "")) >= 15)
        print(f"  QA  {stem[:34]:36s} n={len(its):3d} پاسخ‌دار={wa}")

    # کلیدهای قدیمی (decode v1) به‌عنوان مکمل
    old = {}
    try:
        old = json.load(io.open(OLD_KEYS, encoding="utf-8"))
    except Exception:
        pass
    old_for = {
        "exam01_answers": "exam01", "exam02_answers": "exam02", "exam03_answers": "exam03",
        "exam04_answers": "exam04", "exam05_answers": "exam05", "exam06_answers": "exam06",
        "exam08_answers": "exam08",
        "نهایی_پاسخ_نامه_آزمون_جامع_سوم_،_18_مرداد": "jam3_18mordad",
    }
    keys_all, colored_all = {}, {}
    for astem, qstem in ANSWER_PAIRS.items():
        p = None
        for d in ("/home/z/my-project/upload", "/home/z/my-project/upload/آزمون_extracted"):
            c = os.path.join(d, astem + ".pdf")
            if os.path.exists(c):
                p = c
                break
        if not p:
            continue
        try:
            keys, colored = accent_answers(p)
        except Exception as ex:
            print(f"!! خطای decode {astem}: {ex}")
            keys, colored = {}, {}
        keys = {_latin(k): v for k, v in keys.items()}
        # ادغام با کلیدهای قدیمیِ همان پاسخ‌نامه
        oldentry = old.get(old_for.get(astem, ""), {})
        if oldentry:
            merged = {str(int(_latin(k))): v for k, v in oldentry.items()}
            merged.update(keys)
            keys = merged
        # هایلایت زرد برای jam3/کلید۴
        if astem in HIGHLIGHT_FILES:
            try:
                hkeys = {_latin(k): v for k, v in highlight_answers(p).items()}
            except Exception as ex:
                print(f"!! خطای highlight {astem}: {ex}")
                hkeys = {}
            for k, v in hkeys.items():
                keys.setdefault(k, v)
        if qstem == "__pick4__":
            qt = {str(x["n"]): x["q"] for x in parse_mcq_exam(read_txt("کلید آزمون چهارم")) if x["n"]}
            best, best_r = None, 0.0
            for cand in ("سوال آزمون چهارم", "آزمون چهارم"):
                r = _similarity(qt, mcq_raw.get(cand) or [])
                if r > best_r:
                    best, best_r = cand, r
            qstem = best
            print(f"کلید آزمون چهارم → {qstem} (شباهت {best_r:.2f})")
            if not qstem:
                continue
        keys_all[qstem] = keys
        colored_all[qstem] = colored
        print(f"  کلید {astem[:32]:34s} → {qstem[:28]:30s} کلید={len(keys)} رنگی={len(colored)}")

    # ---------- ساخت بانک
    mcq, qa = [], []
    seen = {}
    dup = 0

    def add_mcq(entry):
        nonlocal dup
        k = dedup_key(entry["q"])
        if k in seen and seen[k] >= 0:
            dup += 1
            prev = mcq[seen[k]]
            prev["dup"] += 1
            if len(entry["opts"]) > len(prev["opts"]):
                prev["opts"] = entry["opts"]
            if not prev["ans"] and entry["ans"] and entry["ans"] in prev["opts"]:
                prev["ans"] = entry["ans"]
                prev["ans_text"] = prev["opts"].get(entry["ans"])
            if not prev["ans_text"] and entry["ans_text"]:
                prev["ans_text"] = entry["ans_text"]
            return
        seen[k] = len(mcq)
        mcq.append(entry)

    for stem, its in mcq_raw.items():
        kmap = keys_all.get(stem, {})
        cmap = colored_all.get(stem, {})
        for it in its:
            q = fix_lam_alef(it["q"].strip())
            if len(q) < 10:
                continue
            refs, q = refs_of(q)
            opts = {c: fix_lam_alef(re.sub(r"^[\)\(:\-ــ]\s*", "", v).strip(" ،,؛")) for c, v in it["opts"].items()}
            opts = {c: v for c, v in opts.items() if v}
            num = it["n"]
            L = kmap.get(str(num) or "")
            ans = L if (L in opts) else None
            ans_text = opts.get(ans) if ans else None
            if not ans_text and num is not None:
                ans_text = cmap.get(str(num)) or None
            if len(opts) >= 2 and len(q) >= 8:
                qual = quality_q(q)
            else:
                qual = "review"
            add_mcq({"n": num, "q": q, "opts": opts, "ans": ans, "ans_text": ans_text,
                     "refs": refs, "src": stem + ".pdf", "cat": category2(stem, q),
                     "quality": qual, "dup": 0})

    for stem, its in qa_raw.items():
        for it in its:
            q, a = it["q"], it.get("a", "")
            k = dedup_key(q)
            if k in seen:
                dup += 1
                continue
            seen[k] = -1
            qual = "review" if "review" in (quality_q(q), quality_of(a)) else "ok"
            qa.append({"n": it.get("n"), "q": q, "a": a, "refs": it.get("refs", []),
                       "src": stem + ".pdf", "cat": category2(stem, q), "quality": qual,
                       "dup": 0})

    # تستیِ بی‌گزینه → تشریحی (جای‌خالی واقعی)
    moved = 0
    keep = []
    for e in mcq:
        if not e["opts"] and len(e["q"]) >= 12:
            k = dedup_key(e["q"])
            if k not in seen:
                qa.append({"n": e["n"], "q": e["q"], "a": e.get("ans_text") or "",
                           "refs": e["refs"], "src": e["src"], "cat": e["cat"],
                           "quality": "ok" if e.get("ans_text") else "review", "dup": 0})
                seen[k] = -1
                moved += 1
            continue
        keep.append(e)
    mcq = keep

    for i, e in enumerate(mcq):
        e["id"] = f"m-{i+1:05d}"
    for i, e in enumerate(qa):
        e["id"] = f"q-{i+1:05d}"

    full4 = sum(1 for x in mcq if len(x["opts"]) >= 4)
    with_ans = sum(1 for x in mcq if x["ans"])
    rev_m = sum(1 for x in mcq if x["quality"] == "review")
    qa_ok_a = sum(1 for x in qa if len(x.get("a", "")) >= 10)
    rev_q = sum(1 for x in qa if x["quality"] == "review")
    by_cat = Counter(x["cat"] for x in mcq) + Counter(x["cat"] for x in qa)

    bank = {
        "meta": {
            "title": "بانک سؤالات آزمون — کنگره ۶۰ / متد DST",
            "app": "همراه درمان اعتیاد (ir.mosafir.recovery)",
            "version": 2,
            "extraction": "pdftotext (poppler) + pdfplumber keys + colored answers",
            "counts": {"mcq": len(mcq), "qa": len(qa), "total": len(mcq) + len(qa),
                        "duplicates_removed": dup, "descriptive_moved_from_mcq": moved},
        },
        "mcq": mcq,
        "qa": qa,
    }
    with io.open(OUT_BANK, "w", encoding="utf-8") as f:
        json.dump(bank, f, ensure_ascii=False, indent=1)
    summary = {
        "mcq": len(mcq), "mcq_full4": full4, "mcq_with_key": with_ans,
        "mcq_needs_review": rev_m, "qa": len(qa), "qa_with_answer": qa_ok_a,
        "qa_needs_review": rev_q, "duplicates_removed": dup,
        "descriptive_moved_from_mcq": moved, "total_unique": len(mcq) + len(qa),
        "by_category": dict(by_cat.most_common()),
    }
    with io.open(OUT_SUM, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=1)
    print(json.dumps(summary, ensure_ascii=False, indent=1))
    print(f"bank.json -> {os.path.getsize(OUT_BANK)//1024} KB")

if __name__ == "__main__":
    main()
