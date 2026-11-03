# -*- coding: utf-8 -*-
"""R12 — فیلتر کیفیت passages.json: حذف صفحه‌های آغازین/فهرست/تکه‌های ناقص + اصلاح غلط‌های OCR رایج.

خروجی: public/content/passages.json و repo/app/src/main/assets/passages.json
"""
import io
import json
import re
from datetime import date

FILES = [
    "/home/z/my-project/public/content/passages.json",
    "/home/z/my-project/repo/app/src/main/assets/passages.json",
]

# غلط‌های رایج OCR (الگوی «لا» → «ال» در فونت‌های عربی + فعل‌های بدون نیم‌فاصله)
EXTRA_REPLACEMENTS = [
    ("الزم", "لازم"),
    ("خالصه", "خلاصه"),
    ("اصال ", "اصل "),
    ("اصالح", "اصلاح"),
    ("صحیت", "صحبت"),
    ("تازوه", "تازه"),
    ("مسئوال", "مسئول"),
    ("بسالم", "به سالم"),
    ("میکنم", "می‌کنم"),
    ("میگویم", "می‌گویم"),
    ("میکردم", "می‌کردم"),
    ("میخواهم", "می‌خواهم"),
    ("میخواهد", "می‌خواهد"),
    ("میبینم", "می‌بینم"),
    ("میروم", "می‌روم"),
    ("میشوم", "می‌شوم"),
    ("میآید", "می‌آید"),
    ("میپرسم", "می‌پرسم"),
    ("میبخشم", "می‌بخشم"),
    ("میخورم", "می‌خورم"),
    ("میپذیرم", "می‌پذیرم"),
    ("بودن ,", "بودن،"),
]

# الگوهای «صفحهٔ آغازین کتاب / فهرست» که به‌عنوان قطعهٔ آموزشی بی‌معنا هستند
JUNK_PATTERNS = [
    "سرشناسه",
    "لیتوگرافی",
    "چاپ و صحافی",
    "چکامهای",
    "فهرست",
    "تالیف :",
    "ویراستار :",
    "ناشر :",
    "انتشارات دژاکام",
    "شابک",
    "تیراژ",
]

TOC_DOTS = re.compile(r"(\.\s*){5,}")
JUNK_RE = re.compile("|".join(JUNK_PATTERNS))


def clean_text(s: str) -> str:
    for a, b in EXTRA_REPLACEMENTS:
        s = s.replace(a, b)
    s = s.replace(" ،", "،").replace(" .", ".").replace(" ؛", "؛")
    s = re.sub(r"[ \t]{2,}", " ", s)
    return s.strip()


def is_junk(text: str) -> bool:
    t = text.strip()
    if len(t) < 60:
        return True
    if TOC_DOTS.search(t):
        return True
    if JUNK_RE.search(t):
        return True
    if t.count("؟") == 0 and t.count("؛") == 0 and len(t.split()) < 12:
        return True
    return False


for path in FILES:
    data = json.load(io.open(path, encoding="utf-8"))
    pas = data["passages"]
    before = len(pas)
    kept = []
    for p in pas:
        p["text"] = clean_text(p["text"])
        if not is_junk(p["text"]):
            kept.append(p)
    data["passages"] = kept
    data["total"] = len(kept)
    data["filtered"] = str(date.today())
    data["filter_note"] = f"R12: حذف {before - len(kept)} قطعهٔ بی‌کیفیت (صفحه‌های آغازین/فهرست/تکه‌های کوتاه) + اصلاح غلط‌های OCR"
    json.dump(data, io.open(path, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    print(f"{path}: {before} → {len(kept)} (حذف {before - len(kept)})")
