# -*- coding: utf-8 -*-
"""مکان‌یابی سؤال‌های معیوب بانک در PDFهای منبع + ساخت فایل جابِ بازیابی VLM.

آیتم‌های هدف:
  ۱) MCQهای با quality==review            (متن گاربلید / گزینهٔ افتاده)
  ۲) MCQهای exam با کمتر از ۴ گزینه       (کلیددار یا بی‌کلید)

برای هر آیتم: شمارهٔ صفحه در PDF (از text_v2 با فرم‌فید \f) + مسیر PDF.
خروجی: data/questionbank/vlm_jobs.json
اجرا: python3 scripts/vlm_locate.py
"""
import json
import os
import re
import sys
import unicodedata

BANK = "/home/z/my-project/data/questionbank/bank.json"
TXT = "/home/z/my-project/data/questionbank/text_v2"
UPLOAD = "/home/z/my-project/upload/آزمون_extracted"
OUT = "/home/z/my-project/data/questionbank/vlm_jobs.json"

FA2EN = str.maketrans("۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩", "01234567890123456789")


def norm(t: str) -> str:
    """نرمال‌سازی حفظِ خطوط (برای جستجوی (?m) خط‌محور)."""
    t = unicodedata.normalize("NFKC", t or "")
    t = t.translate(FA2EN)
    t = re.sub(r"[\u200c\u064b-\u065f]", "", t)
    return re.sub(r"[ \t\u00a0]+", " ", t).strip().lower()


def stem_of(src: str) -> str:
    """src = نام فایل با پسوند → stem همان (text_v2 با همان نام ساخته شده)."""
    return os.path.splitext(src)[0]


def find_page(txt: str, qnum: int, qfrag: str) -> int | None:
    """صفحهٔ سؤال n را پیدا کن؛ اول با شمارهٔ سؤال، بعد با قطعهٔ متن."""
    pages = txt.split("\f")
    # الگوی شمارهٔ سؤال: -N  /  ‑N  /  (N)  /  N-
    pats = [
        rf"(?m)^\s*[-–—ـ‑]\s*{qnum}\s*(?=[^\d])",
        rf"(?m)^\s*{qnum}\s*[-–—ـ‌]\s*",
        rf"(?m)\(\s*{qnum}\s*\)",
    ]
    for i, p in enumerate(pages, 1):
        np = norm(p)
        for pat in pats:
            if re.search(pat, np):
                return i
    frag = norm(qfrag)
    if len(frag) >= 15:
        # قطعهٔ ابتدای سؤال را در صفحات بگرد (متن ممکن است گاربلید باشد)
        fr = frag[:24]
        if len(fr) >= 10:
            for i, p in enumerate(pages, 1):
                if fr in norm(p):
                    return i
            # وارونهٔ قطعه (متن معکوس)
            fr_r = fr[::-1]
            for i, p in enumerate(pages, 1):
                if fr_r in norm(p):
                    return i
    return None


def rev_num(qnum: int) -> int | None:
    """شمارهٔ معکوس‌شدهٔ ارقام (خطای RTL): 31→13، 71→17."""
    s = str(qnum)
    if len(s) >= 2 and s != s[::-1]:
        return int(s[::-1])
    return None


STOP_TOKENS = {
    "کدام", "موارد", "مورد", "زیر", "است", "بود", "شود", "چهارم", "مقاله", "کتاب",
    "صفحه", "گزینه", "جملات", "موردها", "مربوط", "عبارت", "درست", "نادرست",
}


def find_page_fuzzy(txt: str, qnum: int, q: str) -> int | None:
    """فالبک: شمارهٔ معکوس + جستجوی توکن‌های شاخص (مستقیم یا معکوس)."""
    pages = txt.split("\f")
    cands = [q for q in (qnum, rev_num(qnum)) if q]
    for num in cands:
        pg = find_page(txt, num, "")
        if pg:
            return pg
    # توکن‌های شاخص از متن سؤال
    toks = re.findall(r"[\u0600-\u06FF]{4,}|[A-Za-z]{3,}", q or "")
    toks = [t for t in toks if t not in STOP_TOKENS]
    toks.sort(key=len, reverse=True)
    for t in toks[:8]:
        variants = {t, t[::-1]}
        for i, p in enumerate(pages, 1):
            np = norm(p)
            if any(v in np for v in variants):
                return i
    return None


def main() -> None:
    bank = json.load(open(BANK, encoding="utf-8"))
    jobs, misses = [], []

    targets = [q for q in bank["mcq"] if q.get("quality") == "review"]
    for q in bank["mcq"]:
        if "exam" in q.get("src", "") and len(q.get("opts") or {}) < 4 and q["id"] not in {t["id"] for t in targets}:
            targets.append(q)

    for q in targets:
        src = q["src"]
        stem = stem_of(src)
        pdf = os.path.join(UPLOAD, src)
        txtp = os.path.join(TXT, stem + ".txt")
        if not os.path.exists(pdf) or not os.path.exists(txtp):
            misses.append((q["id"], src, "missing file"))
            continue
        txt = open(txtp, encoding="utf-8").read()
        page = find_page(txt, q.get("n") or 0, q.get("q") or "")
        if page is None:
            page = find_page_fuzzy(txt, q.get("n") or 0, q.get("q") or "")
        if page is None:
            misses.append((q["id"], src, "page not found"))
            continue
        jobs.append({
            "id": q["id"],
            "n": q.get("n"),
            "pdf": pdf,
            "page": page,
            "cat": q.get("cat"),
            "have": {
                "q": q.get("q"),
                "opts": q.get("opts"),
                "ans": q.get("ans"),
            },
        })

    # مرتب‌سازی بر اساس (pdf, page) تا هر صفحه یک بار رندر شود
    jobs.sort(key=lambda j: (j["pdf"], j["page"]))
    json.dump(jobs, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    print(f"jobs: {len(jobs)}  misses: {len(misses)}")
    for m in misses:
        print("  MISS", m)
    pages = {(j["pdf"], j["page"]) for j in jobs}
    print(f"unique pages to render: {len(pages)}")


if __name__ == "__main__":
    main()
