# -*- coding: utf-8 -*-
"""فاز ۲ بانک سؤال — یکپارچه‌سازی ۶۲ فایل خام به یک بانک نهایی.

ورودی : /home/z/my-project/data/questionbank/raw/*.json   (خروجی extract_questions.py)
خروجی : /home/z/my-project/data/questionbank/bank.json        ← بانک یکپارچه
        /home/z/my-project/data/questionbank/bank_summary.json ← آمار

کارهایی که انجام می‌دهد:
  ۱) نرمال‌سازی کاراکتر (ي→ی، ك→ک، ة→ه، أ/إ→ا، اعداد عربی→فارسی) و فاصله‌ها
  ۲) تشخیص و ترمیم جمله‌های معکوسِ OCR (بازِ دیداری) با امتیاز واژه‌های پرتکرار
  ۳) جداسازی پرسش/پاسخ در سؤال‌های تشریحی (مرز: علامت صفحه «(ص۷۱)» یا اولین «؟»)
  ۴) اتصال پاسخ‌نامه‌ها (exam01↔exam01_answers و…) به سؤال‌های تستی
  ۵) حذف تکراری‌ها با کلید نرمال‌شدهٔ متن پرسش + شمارش تکرار
  ۶) برچسب کیفیت (ok / review) و دسته‌بندی منبع بر اساس نام فایل و متن
اجرا: python3 scripts/build_bank.py
"""
import glob
import io
import json
import os
import re
import unicodedata
from collections import Counter, defaultdict
from difflib import SequenceMatcher

RAW = "/home/z/my-project/data/questionbank/raw"
OUT_BANK = "/home/z/my-project/data/questionbank/bank.json"
OUT_SUM = "/home/z/my-project/data/questionbank/bank_summary.json"

# ---------------------------------------------------------------- نرمال‌سازی

CHAR_MAP = str.maketrans({
    "ي": "ی", "ى": "ی", "ك": "ک", "ة": "ه", "أ": "ا", "إ": "ا", "ٱ": "ا",
    "٠": "۰", "١": "۱", "٢": "۲", "٣": "۳", "٤": "۴", "٥": "۵", "٦": "۶",
    "٧": "۷", "٨": "۸", "٩": "۹", "٠": "۰",
    "\u200f": "", "\u200e": "", "\ufeff": "",
})

def norm(s: str) -> str:
    s = unicodedata.normalize("NFKC", s or "")
    s = s.translate(CHAR_MAP)
    s = s.replace("\u200c", "\u200c")  # ZWNJ نگه داشته می‌شود
    s = re.sub(r"[\u064B-\u065F\u0670]", "", s)  # اعراب
    s = re.sub(r"[ \t]+", " ", s)
    return s.strip()

# ---------------------------------------------------------------- تشخیص معکوس

COMMON_WORDS = set(
    """از به که است می برای این با چه کدام همه باید در را و تا خود یک شود می‌شود انسان کنگره مسافر
    راهنما همسفر مصرف درمان ماده اول دوم سوم چهارم پنجم شش هفت هشت نه ده حیات عشق رهایی وادی مقاله
    صفحه کتاب جلسه پله سقف توقف کاهش مشکل زندگی قدرت تفکر جهان بینی هستی خلق آغاز فردی جامعه لژیون
    سرپرست مرزبان کشیک عضو تازه وارد خدمت علاقه حس رها کنیم می‌کنیم نمی آن‌ها ما شما او دارد بود شد
    یکی دیگر بزرگ کوچک هرگز همیشه گاهی است هستند نیست باشد نمی‌باشد جواب گزینه صحیح غلط درست
    ارزش وجودی سازندگی صلح تاریکی تاریکیت روشنایی روشناییتی هدایت پاداش جسم روان جهان‌بینی worldwide""".split()
)

def score(t: str) -> float:
    words = re.findall(r"[\u0600-\u06FF\u200c]+", t)
    if not words:
        return 0.0
    hit = sum(1 for w in words if w.strip("\u200c") in COMMON_WORDS)
    return hit / len(words)

SENT_SPLIT = re.compile(r"(?<=[.؟!؛])\s+")

# واژه‌های شکستهٔ پرتکرارِ استخراج PDF (فاصلهٔ داخل واژه / جابه‌جایی حروف) — فقط موارد مطمئن
WORD_FIX = [
    ("اعتی اد", "اعتیاد"), ("اعت یاد", "اعتیاد"), ("اعتیا د", "اعتیاد"),
    ("ه یچ", "هیچ"), ("هيچ ", "هیچ "),
    ("نم یتواند", "نمی‌تواند"), ("نمي تواند", "نمی‌تواند"),
    ("می شود", "می‌شود"), ("مي شود", "می‌شود"), ("نمی شود", "نمی‌شود"),
    ("می کنی م", "می‌کنیم"), ("می کنیم", "می‌کنیم"), ("می کند", "می‌کند"),
    ("می کنند", "می‌کنند"), ("می کنیم", "می‌کنیم"), ("می کنم", "می‌کنم"),
    ("بی رامیها", "بیماری‌ها"), ("بی رامی", "بیماری"), ("بیماريها", "بیماری‌ها"),
    ("همسر ی", "همسری"), ("مصرف کنن د", "مصرف‌کننده"),
    ("سازندگي", "سازندگی"), ("جهان بیني", "جهان‌بینی"), ("جهان بینی", "جهان‌بینی"),
    ("تشخیص مبدا", "تشخیص مبدأ"), ("مبدا", "مبدأ"),
    ("تم سق", "قسمت"), ("قسمتهاي", "قسمت‌های"),
    # بقایای معکوسِ «صفحه»
    ("هحفص", " "), (") هحف", ""), ("هحف ", " "),
    ("جهانبيني", "جهان‌بینی"), ("جهانبینی", "جهان‌بینی"),
    ("اشكا الت", "اشکالات"),
    ("کنگره ۰۶", "کنگره ۶۰"), ("کنگره06", "کنگره ۶۰"),
]

# نشانه‌های سربرگ آزمون که در OCR به متن سؤال/گزینه چسبیده‌اند — برش از اولین نشانه
# توجه: «کنگره ۶۰» واژهٔ محتوایی است (در سؤال واقعی می‌آید) — فقط در ترکیب با سربرگ بریده می‌شود
BOILER_MARKERS = [
    "بیایید این آتش", "بیا یی د ا ین آتش", "آتش و یرانگر را مهار",
    "آزمون داخلی", "آزمون جامع", "آزمون نهایی", "همسفران آقا",
    "تاریخ :", "تاریخ:", "منابع آزمون", "مدت زمان آزمون",
    "نام و نام خانوادگی", "نمایندگی :", "نمایندگی:", "شماره عضویت",
]
BOILER_RES = [
    re.compile(r"کنگره\s*(?:۶۰|۰۶|60)\s*(?:بیا|آزمون|Congress)"),
    re.compile(r"Congress\s*60"),
    re.compile(r"آزمون\s*(?:داخلی|جامع|نهایی)\s*(?:همسفران|دوم|سوم|چهارم|آقا)"),
]

def cut_boiler(t: str) -> str:
    cuts = [t.find(m) for m in BOILER_MARKERS]
    for rx in BOILER_RES:
        m = rx.search(t)
        if m:
            cuts.append(m.start())
    cuts = [c for c in cuts if c > 15]  # اگر سربرگ خودِ متن باشد برش نمی‌زنیم
    if cuts:
        t = t[: min(cuts)]
    t = re.sub(r"[\(\)«»ـ]+\s*$", "", t)          # پرانتزهای سرگردان انتها
    t = re.sub(r"\s+([؟!،؛:])", r"\1", t)          # فاصلهٔ قبل از نقطه‌گذاری
    return re.sub(r"\s+", " ", t).strip()

# تکّه‌های شکستهٔ ابتدای متن از ارجاع صفحهٔ معکوس — مثل «(۶۱و» یا «)ص -۵۲۶»
LEAD_FRAG_Q = re.compile(r"^[\(\[]?\s*[\d۰-۹]{1,4}\s*و\s*[\)\]]?\s+")
LEAD_FRAG_A = re.compile(r"^[\)\[]\s*ص?\s*[\d۰-۹\-،\s]+(?=\s)")

def fix_words(t: str) -> str:
    for a, b in WORD_FIX:
        t = t.replace(a, b)
    # سال‌های معکوس: «۹۷۹۱» ← ۱۹۷۹ — فقط وقتی وارونه‌اش سال معتبر است
    def year_sub(m):
        s = m.group(0)
        r = s[::-1]
        v = int(r.translate(str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789")))
        o = int(s.translate(str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789")))
        valid = lambda x: 1340 <= x <= 1410 or 1960 <= x <= 2010
        return r if valid(v) and not valid(o) else s
    t = re.sub(r"[۰-۹]{4}", year_sub, t)
    return re.sub(r"\s+", " ", t).strip()

def smart_fix(t: str) -> str:
    """جمله‌های معکوس OCR را با مقایسهٔ امتیاز واژه‌ها ترمیم می‌کند (محقورانه)."""
    t = norm(t)
    if not t:
        return t
    segs = SENT_SPLIT.split(t)
    fixed = []
    for seg in segs:
        if len(seg) > 10:
            fwd, rev = score(seg), score(seg[::-1])
            if rev > fwd + 0.05:
                seg = seg[::-1]
        fixed.append(seg)
    return re.sub(r"\s+", " ", " ".join(fixed)).strip()

def quality_of(*texts: str) -> str:
    """اگر امتیاز واژه‌های متداول خیلی پایین باشد، متن برای بازبینی علامت می‌خورد."""
    joined = " ".join(x for x in texts if x)
    if len(joined) < 12:
        return "ok"
    if score(joined) < 0.06:
        return "review"
    # متنِ کاملاً معکوس: امتیازِ وارونه‌اش بهتر از خودش باشد
    if len(joined) > 30 and score(joined[::-1]) > score(joined) + 0.05:
        return "review"
    # توکن‌های نشانهٔ سطر معکوس (است/می/شد/ها/بِه/ند… وارونه)
    toks = re.findall(r"[\u0600-\u06FF]+", joined)
    hits = sum(1 for w in toks if w in {"هب", "دنن", "تسا", "یم", "دوجو", "دش", "اه", "نامیا", "دشر"})
    if hits >= 2:
        return "review"
    return "ok"

def quality_q(q: str) -> str:
    """برای پرسش: علامت «؟» نباید در ابتدای متن باشد (نشانهٔ سطر معکوس)."""
    base = quality_of(q)
    if base == "review":
        return base
    i = q.find("؟")
    if i != -1 and len(q) > 40 and i < 0.3 * len(q):
        return "review"
    return "ok"

# ---------------------------------------------------------------- ابزار پرسش

LEAD_NUM = re.compile(r"^\s*([۰-۹]{1,3}|\d{1,3})\s*[-–—_.ـ)]\s*")
PAGE_REF = re.compile(r"\(?\s*(?:صفحه|ص)\s*[\d۰-۹]+(?:\s*(?:تا|الی|و)\s*[\d۰-۹]+)?\s*\(?\)?")

def strip_lead(q: str) -> tuple[int | None, str]:
    m = LEAD_NUM.match(q)
    if not m:
        return None, q
    n = int(m.group(1).translate(str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789")))
    return n, q[m.end():].strip()

def refs_of(q: str) -> tuple[list[str], str]:
    refs = [m.group(0).strip(" ()") for m in PAGE_REF.finditer(q)]
    clean = PAGE_REF.sub(" ", q)
    return refs, re.sub(r"\s+", " ", clean).strip(" ,؛.")

def dedup_key(q: str) -> str:
    s = re.sub(r"[^\u0600-\u06FF\u200c]+", "", q)
    return s[:120]

# ---------------------------------------------------------------- دسته‌بندی

def category_of(fname: str, q: str) -> str:
    f = fname.lower()
    if "وادی" in fname or "عشق" in fname:
        return "۱۴ وادی عشق"
    if "مقاله" in fname:
        return "۱۴ مقاله"
    if "راهنما" in fname and "وادی" not in fname and "مقاله" not in fname:
        return "راهنما و راهنمایی"
    if "شرح" in fname and "وظایف" in fname:
        return "شرح وظایف"
    if "جهان" in fname:
        return "جهان‌بینی"
    if "درجه" in fname:
        return "۶۰ درجه"
    if "پروتکل" in fname:
        return "پروتکل DST"
    for b, cat in [
        ("کتاب عشق", "۱۴ وادی عشق"), ("جزوه راهنما", "راهنما و راهنمایی"),
        ("چهارده مقاله", "۱۴ مقاله"), ("۱۴ مقاله", "۱۴ مقاله"),
        ("پروتکل", "پروتکل DST"), ("جهان بینی", "جهان‌بینی"), ("جهان‌بینی", "جهان‌بینی"),
        ("60 درجه", "۶۰ درجه"), ("۶۰ درجه", "۶۰ درجه"),
    ]:
        if b in q:
            return cat
    if "همسفر" in fname:
        return "آزمون همسفران"
    return "آزمون عمومی"

# ---------------------------------------------------------------- اتصال پاسخ

SOLO_LETTER = re.compile(r"(?:^|[\s(:،؛.])((?:گزینه\s*)?(الف|ب|ج|د))(?=[\s):،؛.]|$)")

def parse_answer_value(v: str, opts: dict) -> tuple[str | None, str | None]:
    """(حرف، متن) از مقدار پاسخ‌نامه استخراج می‌کند."""
    v = fix_words(smart_fix(v))
    m = re.search(r"گزینه\s*(الف|ب|ج|د)", v)
    if m:
        return m.group(1), None
    solos = SOLO_LETTER.findall(v)
    if solos:
        return solos[-1][1], None
    best, r = None, 0.0
    for key, text in opts.items():
        rr = SequenceMatcher(None, norm(v)[:200], norm(text)).ratio()
        if rr > r:
            best, r = key, rr
    if r >= 0.55:
        return best, None
    return None, (v if len(v) > 8 else None)

# ---------------------------------------------------------------- بارگذاری

def load_raw() -> list[dict]:
    out = []
    for p in sorted(glob.glob(os.path.join(RAW, "*.json"))):
        d = json.load(io.open(p, encoding="utf-8"))
        d["_name"] = os.path.basename(p)[:-5]
        out.append(d)
    return out

def pair_answers(files: list[dict]) -> dict[str, dict]:
    ans = {}
    for d in files:
        if d["kind"] != "mcq_ans":
            continue
        base = d["_name"]
        for suf in ["_answers", "_پاسخ_نامه", "_پاسخنامه", "_پاسخ"]:
            if suf in base:
                base = base.replace(suf, "")
                break
        base = re.sub(r"_+", "_", base).strip("_")
        ans[base] = d["items"]
    return ans

def main() -> None:
    files = load_raw()
    answers = pair_answers(files)
    mcq, qa = [], []
    seen = {}
    dup_count = 0

    for d in files:
        fname, kind = d["file"], d["kind"]
        if kind == "mcq":
            # نام پایه برای تطبیق پاسخ‌نامه
            stem = re.sub(r"[^\w\u0600-\u06FF]+", "_", os.path.splitext(fname)[0])
            stem = re.sub(r"_+", "_", stem).strip("_")
            keymap = None
            for base, amap in answers.items():
                if base == stem or base in stem or stem in base:
                    keymap = amap
                    break
            # اتصال فقط وقتی امن است که شماره‌های پاسخ‌نامه دقیقاً ۱..N برابر تعداد سؤال‌ها باشند
            # و اکثر مقادیر حرفِ تنها باشند (وگرنه شماره‌گذاری «بخشی» است و حرف اشتباه می‌چسبد)
            aligned = False
            if keymap:
                try:
                    keys = sorted(int(k) for k in keymap)
                except ValueError:
                    keys = []
                solo_n = sum(1 for v in keymap.values() if re.fullmatch(r"\s*(?:گزینه\s*)?(الف|ب|ج|د)\s*", str(v)))
                aligned = keys == list(range(1, len(d["items"]) + 1)) and solo_n >= 0.6 * len(d["items"])
            for it in d["items"]:
                q = cut_boiler(fix_words(smart_fix(it.get("q", ""))))
                n, q = strip_lead(q)
                n = it.get("n") or n
                refs, q = refs_of(q)
                opts_raw = it.get("opts") or {}
                opts = {}
                for k, v in opts_raw.items():
                    v = cut_boiler(fix_words(smart_fix(v)))
                    v = re.sub(r"^[\)\(ـ]\s*", "", v).strip()
                    if v:
                        opts[k] = v
                complete = len(opts) >= 2 and len(q) >= 8
                qual = "review" if not complete else quality_q(q)
                entry = {
                    "n": n, "q": q, "opts": opts, "ans": None, "ans_text": None,
                    "refs": refs, "src": fname, "cat": category_of(fname, q),
                    "quality": qual,
                }
                if keymap and n is not None and str(n) in keymap:
                    letter, atext = parse_answer_value(str(keymap[str(n)]), opts)
                    if len(opts) >= 2:
                        entry["ans"], entry["ans_text"] = letter, atext
                    else:
                        entry["ans_text"] = atext  # سؤال بدون گزینه: فقط متن پاسخ
                k = dedup_key(q)
                if k in seen:
                    dup_count += 1
                    mcq[seen[k]]["dup"] += 1
                    continue
                seen[k] = len(mcq)
                entry["id"] = f"m-{len(mcq)+1:05d}"
                entry["dup"] = 0
                mcq.append(entry)
        elif kind == "qa":
            for it in d["items"]:
                body = cut_boiler(fix_words(smart_fix(it.get("q", ""))))
                n, body = strip_lead(body)
                n = it.get("n") or n
                if len(body) < 15:
                    continue
                # مرز پرسش/پاسخ: آخرین علامت صفحه یا اولین «؟»
                marks = list(PAGE_REF.finditer(body))
                qq, aa = body, ""
                if marks:
                    m = marks[-1]
                    qq, aa = body[:m.start()], body[m.end():]
                elif "؟" in body:
                    i = body.index("؟")
                    qq, aa = body[:i + 1], body[i + 1:]
                qq = re.sub(r"\s+", " ", qq).strip(" ,؛.")
                aa = re.sub(r"\s+", " ", aa).strip(" ,؛.")
                qq = LEAD_FRAG_Q.sub("", qq).strip()
                aa = LEAD_FRAG_A.sub("", aa).strip()
                if len(qq) < 10:
                    continue
                refs, qq = refs_of(qq)
                qual = "review" if "review" in (quality_q(qq), quality_of(aa)) else "ok"
                k = dedup_key(qq)
                if k in seen:
                    dup_count += 1
                    continue
                seen[k] = -1
                qa.append({
                    "id": f"q-{len(qa)+1:05d}", "n": n, "q": qq, "a": aa,
                    "refs": refs, "src": fname, "cat": category_of(fname, qq),
                    "quality": qual,
                })
        # mcq_ans ها خودشان در مرحلهٔ pair_answers مصرف شده‌اند

    # آمار
    mcq_ans_attached = sum(1 for x in mcq if x["ans"])
    mcq_complete = sum(1 for x in mcq if len(x["opts"]) >= 2)
    mcq_review = sum(1 for x in mcq if x["quality"] == "review")
    qa_with_a = sum(1 for x in qa if len(x.get("a", "")) >= 10)
    qa_review = sum(1 for x in qa if x["quality"] == "review")
    by_cat = Counter([x["cat"] for x in mcq] + [x["cat"] for x in qa])

    bank = {
        "meta": {
            "title": "بانک سؤالات آزمون — کنگره ۶۰ / متد DST",
            "app": "همراه درمان اعتیاد (ir.mosafir.recovery)",
            "sources": "پوشهٔ upload: ۳سوال، ۵۵۵سوال، ۸سوال، پوشهٔ آزمون (ده‌ها آزمون واقعی)، سؤالات تشریحی ۱۴ وادی/۱۴ مقاله/راهنما/شرح وظایف",
            "counts": {
                "mcq": len(mcq), "qa": len(qa), "total": len(mcq) + len(qa),
                "duplicates_removed": dup_count,
            },
        },
        "mcq": mcq,
        "qa": qa,
    }
    with io.open(OUT_BANK, "w", encoding="utf-8") as f:
        json.dump(bank, f, ensure_ascii=False, indent=1)

    summary = {
        "mcq": len(mcq),
        "mcq_complete_opts": mcq_complete,
        "mcq_answer_attached": mcq_ans_attached,
        "mcq_needs_review": mcq_review,
        "qa": len(qa),
        "qa_with_answer": qa_with_a,
        "qa_needs_review": qa_review,
        "duplicates_removed": dup_count,
        "total_unique": len(mcq) + len(qa),
        "by_category": dict(by_cat.most_common()),
    }
    with io.open(OUT_SUM, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=1)

    print(json.dumps(summary, ensure_ascii=False, indent=1))
    print(f"\nbank.json -> {os.path.getsize(OUT_BANK)//1024} KB")

if __name__ == "__main__":
    main()
