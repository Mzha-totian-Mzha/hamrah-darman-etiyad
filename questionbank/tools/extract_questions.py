# -*- coding: utf-8 -*-
"""استخراج سؤال‌های آزمون کنگره ۶۰ از PDFهای پوشه upload.

خروجی: /home/z/my-project/data/questionbank/raw/<نام-فایل>.json
هر JSON: { file, kind, exam:{title,sources}, items:[...] }
  kind = "mcq"      → سؤال تستی با گزینه‌های الف/ب/ج/د
  kind = "mcq_ans"  → پاسخ‌نامه (تطبیق با سؤال‌ها در فاز بعد)
  kind = "qa"       → سؤال‌های تشریحی (مثلاً ۱۴ وادی)

اجرا: python3 scripts/extract_questions.py [نام-فایل ...]   (بدون آرگومان = همه)
"""
import json
import os
import re
import sys
import unicodedata
import zipfile
from difflib import SequenceMatcher

from pypdf import PdfReader

UPLOAD = "/home/z/my-project/upload"
EXTRACTED = os.path.join(UPLOAD, "آزمون_extracted")
OUT = "/home/z/my-project/data/questionbank/raw"

# ---------------------------------------------------------------- ابزار متن

ARABIC_DIGITS = str.maketrans("٠١٢٣٤٥٦٧٨٩", "۰۱۲۳۴۵۶۷۸۹")

def norm(s: str) -> str:
    s = unicodedata.normalize("NFKC", s or "")
    s = s.translate(ARABIC_DIGITS)
    s = re.sub(r"[\u200c\u200f\u200e\ufeff]", "\u200c", s)  # ZWNJ و کنترل‌ها
    s = re.sub(r"[ \t]+", " ", s)
    return s.strip()

COMMON_WORDS = set(
    """از به که است می برای این با چه کدام همه باید در را و تا خود یک شود می‌شود انسان کنگره مسافر
    راهنما همسفر مصرف درمان ماده اول دوم سوم چهارم پنجم شش هفت هشت نه ده حیات عشق رهایی وادی مقاله
    صفحه کتاب جلسه پله سقف توقف کاهش مشکل زندگی قدرت تفکر جهان بینی هستی خلق آغاز فردی جامعه لژیون
    سرپرست مرزبان کشیک عضو تازه وارد خدمت علاقه حس رها کنیم می‌کنیم نمی آن‌ها ما شما او دارد بود شد
    یکی دیگر بزرگ کوچک هرگز همیشه گاهی است هستند نیست باشد نمی‌باشد جواب گزینه صحیح غلط درست""".split()
)

def _score(t: str) -> float:
    words = re.findall(r"[\u0600-\u06FF\u200c]+", t)
    if not words:
        return 0.0
    hit = sum(1 for w in words if w.strip("\u200c") in COMMON_WORDS)
    return hit / len(words)

def fix_rtl(t: str) -> str:
    """متنِ استخراج‌شده در «ترتیب دیداری» (معکوس) را به ترتیب منطقی برمی‌گرداند."""
    t = norm(t)
    if not t:
        return t
    return t if _score(t) >= _score(t[::-1]) else t[::-1]

def is_garbled(t: str) -> bool:
    """سطرهایی که کاراکترهای فرم‌های ارائه عربی دارند احتمالاً معکوس‌اند."""
    return bool(re.search(r"[\uFB50-\uFDFF\uFE70-\uFEFF]", t))

def extract_pdf_text(path: str) -> list[str]:
    r = PdfReader(path)
    pages = []
    for p in r.pages:
        t = p.extract_text() or ""
        t = fix_rtl(t)
        pages.append(t)
    return pages

# ---------------------------------------------------------------- پاک‌سازی سربرگ

BOILER_RE = re.compile(
    r"^\s*(صفحه\s*\d+|کنگره\s*۶۰|Congress\s*60|بیایید|Let us Curb|به نام خالق|«زیباترین عمل|زیباترین عمل"
    r"|نام\s*:|شماره عضویت|نمایندگی\s*:|تاریخ\s*$|مدت زمان آزمون|موفق باشید|"
    r"آزمون (داخلی|جامع|دوم|سوم|چهارم|نهایی)[^\n]*|منابع\s*:).*$",
    re.IGNORECASE,
)

def clean_lines(pages: list[str]) -> list[str]:
    lines = []
    for pg in pages:
        for ln in pg.split("\n"):
            ln = ln.strip()
            if not ln or ln in {"", "*"}:
                continue
            if BOILER_RE.match(ln):
                continue
            lines.append(ln)
    return lines

# ---------------------------------------------------------------- پارسر تستی

Q_START = re.compile(r"^\s*(\d{1,3}|[۰-۹]{1,3})\s*[-–—_.ـ)]\s*")
OPT_LETTER = re.compile(r"(?:^|\s)(الف|ب|ج|د)\s*[:\u066B\)\-\-ـ(]\s*")
OPT_NUM = re.compile(r"(?<![\d۱-۹])([1-4]|۱|۲|۳|۴)\s*[\)\-]\s*")
NUM2LET = {"1": "الف", "2": "ب", "3": "ج", "4": "د", "۱": "الف", "۲": "ب", "۳": "ج", "۴": "د"}


def _split_options(blob: str) -> dict:
    """گزینه‌ها را از بلوک جدا می‌کند — اول حرفی، اگر نبود عددی."""
    letters = list(OPT_LETTER.finditer(blob))
    keys = []
    if len(letters) >= 2:
        parts = OPT_LETTER.split(blob)
        it = iter(parts[1:])
        for key, seg in zip(it, it):
            keys.append((key, seg))
    else:
        nums = list(OPT_NUM.finditer(blob))
        if len(nums) >= 2:
            for i, m in enumerate(nums):
                seg_end = nums[i + 1].start() if i + 1 < len(nums) else len(blob)
                seg = blob[m.end():seg_end]
                keys.append((NUM2LET.get(m.group(1), m.group(1)), seg))
    opts = {}
    for key, seg in keys:
        seg = norm(seg)
        if key in opts:
            opts[key] += " " + seg
        else:
            opts[key] = seg
    return {k: v.strip() for k, v in opts.items() if v.strip()}

def parse_mcq(pages: list[str]) -> list[dict]:
    lines = clean_lines(pages)
    # پیدا کردن شروع سؤال‌ها — خط‌های گزینهٔ عددی («1 )… 2) …») سؤال نیستند
    starts = [
        i for i, ln in enumerate(lines)
        if Q_START.match(ln) and len(ln) > 8 and len(OPT_NUM.findall(ln)) < 2
    ]
    items = []
    for k, s in enumerate(starts):
        e = starts[k + 1] if k + 1 < len(starts) else len(lines)
        block = " ".join(lines[s:e])
        m = Q_START.match(lines[s])
        num = int(norm(m.group(1)).translate(str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789")))
        # اولین شروع گزینه بعد از چند کاراکتر متن سؤال
        cut = None
        for mo in OPT_LETTER.finditer(block):
            if mo.start() > 10:
                cut = mo.start()
                break
        if cut is None:
            for mo in OPT_NUM.finditer(block):
                if mo.start() > 10:
                    cut = mo.start()
                    break
        q_text, opt_blob = (block[m.end():cut], block[cut:]) if cut else (block[m.end():], "")
        opts = _split_options(opt_blob)
        q_text = norm(q_text)
        q_text = re.sub(r"[ \u200c]+$", "", q_text)
        if q_text:
            # سؤال‌های جای‌خالی/تشریحی داخل آزمون هم حفظ می‌شوند (opts خالی)
            items.append({"n": num, "q": q_text, "opts": opts, "src": ""})
    return items

# ---------------------------------------------------------------- پاسخ‌نامه

def parse_answers(pages: list[str]) -> dict[int, str]:
    lines = clean_lines(pages)
    starts = [i for i, ln in enumerate(lines) if Q_START.match(ln) and len(ln) > 4]
    ans = {}
    for k, s in enumerate(starts):
        e = starts[k + 1] if k + 1 < len(starts) else len(lines)
        block = " ".join(lines[s:e])
        m = Q_START.match(lines[s])
        num = int(m.group(1))
        body = block[m.end():].strip()
        # ۱) حرف گزینهٔ تنها
        solo = re.findall(r"(?:^|\s)(?:\()?((الف|ب|ج|د))(?=\)|\s|$)", body)
        if solo:
            ans[num] = solo[-1][0]
            continue
        # ۲) متن گزینهٔ درست (ممکن است معکوس باشد)
        cand = fix_rtl(body) if is_garbled(body) or _score(body) < _score(body[::-1]) else body
        ans[num] = norm(cand)
    return ans

def match_answer(opts: dict, ans_raw: str) -> str | None:
    """حرف گزینهٔ درست را از متن پاسخ تشخیص می‌دهد."""
    a = norm(ans_raw)
    if a in opts:
        return a
    best, best_r = None, 0.0
    for key, text in opts.items():
        r = SequenceMatcher(None, a, norm(text)).ratio()
        if r > best_r:
            best, best_r = key, r
    return best if best_r >= 0.55 else None

# ---------------------------------------------------------------- تشریحی

QA_START = re.compile(r"^\s*([۰-۹]{1,3}|\d{1,3})\s*[.،)\-]\s*")

def parse_qa(pages: list[str]) -> list[dict]:
    lines = [ln for pg in pages for ln in pg.split("\n") if ln.strip()]
    starts = [i for i, ln in enumerate(lines) if QA_START.match(ln)]
    items = []
    for k, s in enumerate(starts):
        e = starts[k + 1] if k + 1 < len(starts) else len(lines)
        num = int(norm(QA_START.match(lines[s]).group(1)).translate(str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789")))
        body = " ".join(fix_rtl(l) for l in lines[s:e])
        body = re.sub(r"\s+", " ", body).strip()
        if len(body) > 15:
            items.append({"n": num, "q": body, "src": ""})
    return items

# ---------------------------------------------------------------- docx

def extract_docx_text(path: str) -> list[str]:
    with zipfile.ZipFile(path) as z:
        xml = z.read("word/document.xml").decode("utf-8", "ignore")
    xml = re.sub(r"<w:p[ >]", "\n<w:p ", xml)
    txt = re.sub(r"<[^>]+>", "", xml)
    return [fix_rtl(ln) for ln in txt.split("\n")]

# ---------------------------------------------------------------- نام آزمون

def exam_meta(path: str) -> dict:
    base = os.path.basename(path)
    title = os.path.splitext(base)[0]
    title = re.sub(r"\s*\(\d\)\s*$", "", title)
    return {"title": title, "sources": ""}

# ---------------------------------------------------------------- اجرا

MCQ_FILES = [
    "3سوال.pdf", "8سوال.pdf", "555سوال.pdf",
    "exam01.pdf", "exam02.pdf", "exam03.pdf", "exam033.pdf", "exam04.pdf", "exam05.pdf",
    "exam06.pdf", "exam066.pdf", "exam07.pdf", "exam08.pdf", "exam09.pdf", "exam10.pdf",
    "آزمون جامع همسفران آقا 1404.pdf", "آزمون جامع همسفران آقا 14044.pdf",
    "آزمون داخلی همسفران آقا بهمن ماه (1).pdf", "آزمون داخلی همسفران آقا شهریور ماه(1).pdf",
    "آزمون دوازدهم ویرایش.pdf", "آزمون دوم.pdf", "آزمون سوم.pdf",
    "آزمون مرداد ماه 1404 (1).pdf", "آزمون نهایی همسفران آقا 1404 - Copy.pdf",
    "آزمون نهایی همسفران آقا 144504.pdf", "آزمون همسفران آقا اردیبهشت ماه 1404.pdf",
    "آزمون همسفران آقا تهران 14030908.pdf", "آزمون همسفران آقا تیر ماه 1404.pdf",
    "آزمون همسفران آقا خرداد ماه.pdf", "آزمون چهاردهم -.pdf", "آزمون چهارم.pdf",
    "آزمون.pdf", "بهمن ماه.pdf", "سؤال اصلاح شده.pdf", "سؤالات.pdf", "سؤالق.pdf",
    "سو55ال.pdf", "سوا5555لات.pdf", "سوال آزمون چهارم.pdf", "سوال.pdf", "سوال44.pdf",
    "سوال55.pdf", "سوالات(3) (2).pdf", "سوالات2.pdf", "نهایی آزمون جامع سوم ، 18 مرداد.pdf",
]
ANSWER_FILES = {
    "exam01.pdf": "exam01_answers.pdf", "exam02.pdf": "exam02_answers.pdf",
    "exam03.pdf": "exam03_answers.pdf", "exam04.pdf": "exam04_answers.pdf",
    "exam05.pdf": "exam05_answers.pdf", "exam06.pdf": "exam06_answers.pdf",
    "exam08.pdf": "exam08_answers.pdf",
    "نهایی آزمون جامع سوم ، 18 مرداد.pdf": "نهایی_پاسخ_نامه_آزمون_جامع_سوم_،_18_مرداد.pdf",
}
QA_FILES = [
    "سوال از صفحه 85 الی 95 چهارده مقاله.pdf", "سوالات 14 وادی عشق_ناقص.pdf",
    "سوالات 14 وادی عشق_نهایی.pdf", "سوالات از 14 مقاله -2.pdf", "سوالات از 14 مقاله.pdf",
    "سوالات از 14 وادی - وادی 14.pdf", "نمونه سؤالات امتحان راهنما.pdf",
]
DOCX_FILES = ["آزمون شرح وظایف (مرزبانان).docx", "آزمون یکم.docx"]

def resolve(name: str) -> str | None:
    for d in (EXTRACTED, UPLOAD):
        p = os.path.join(d, name)
        if os.path.exists(p):
            return p
    return None

def safe_name(name: str) -> str:
    return re.sub(r"[^\w\u0600-\u06FF.-]+", "_", os.path.splitext(name)[0])

def process(name: str) -> dict | None:
    path = resolve(name)
    if not path:
        print(f"  !! پیدا نشد: {name}")
        return None
    ext = os.path.splitext(name)[1].lower()
    pages = extract_pdf_text(path) if ext == ".pdf" else extract_docx_text(path)
    is_ans = "answers" in name or "پاسخ" in name or name == "کلید آزمون چهارم.pdf"
    if name in ANSWER_FILES and not is_ans:
        kind = "mcq"
    elif is_ans:
        kind = "mcq_ans"
    elif name in QA_FILES:
        kind = "qa"
    elif ext == ".docx":
        kind = "mcq"
    else:
        kind = "mcq"
    items = parse_answers(pages) if kind == "mcq_ans" else (parse_qa(pages) if kind == "qa" else parse_mcq(pages))
    out = {"file": name, "kind": kind, "exam": exam_meta(path), "items": items}
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, safe_name(name) + ".json"), "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
    return out

def main() -> None:
    names = sys.argv[1:]
    if not names:
        names = MCQ_FILES + list(ANSWER_FILES.values()) + QA_FILES + DOCX_FILES
    tot = {"mcq": 0, "mcq_ans": 0, "qa": 0}
    ok, fail = 0, 0
    for n in names:
        try:
            r = process(n)
        except Exception as ex:
            print(f"  !! خطا در {n}: {ex}")
            fail += 1
            continue
        if r is None:
            fail += 1
            continue
        c = len(r["items"])
        tot[r["kind"]] += c
        tag = {"mcq": "سؤال", "mcq_ans": "پاسخ", "qa": "تشریحی"}[r["kind"]]
        print(f"  {n}: {c} {tag}")
        ok += 1
    print(f"\n=== جمع: سؤال تستی={tot['mcq']} · پاسخ‌نامه={tot['mcq_ans']} · تشریحی={tot['qa']} · فایل ok={ok} fail={fail} ===")

if __name__ == "__main__":
    main()
