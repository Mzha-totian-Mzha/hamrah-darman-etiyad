# -*- coding: utf-8 -*-
"""برش تصویری سؤال‌های معیوب برای رونویسی دستی (مدل بینایی).

برای هر آیتم: یافتن خط شمارهٔ سؤال با pdfplumber (شمارهٔ مستقیم یا معکوس یا
توکن شاخص) → برش پنجرهٔ عمودی حول آن خط → data/questionbank/crops/<id>.png
اجرا: python3 scripts/crop_defective.py
"""
import json
import os
import re
import sys
import unicodedata

import pdfplumber

BASE = "/home/z/my-project/data/questionbank"
JOBS = os.path.join(BASE, "vlm_jobs.json")
CROPS = os.path.join(BASE, "crops")
UPLOAD = "/home/z/my-project/upload/آزمون_extracted"

FA2EN = str.maketrans("۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩", "01234567890123456789")

sys.path.insert(0, "/home/z/my-project/scripts")
from vlm_locate import STOP_TOKENS  # noqa: E402


def nrm(t: str) -> str:
    t = unicodedata.normalize("NFKC", t or "").translate(FA2EN)
    t = re.sub(r"[\u200c\u064b-\u065f]", "", t)
    return re.sub(r"[ \t\u00a0]+", " ", t).strip().lower()


def locate_y(pdf_path: str, page_no: int, qnum: int, q: str) -> tuple[float, float] | None:
    """(y_top, y_bottom) تقریبی بلوک سؤال در صفحه."""
    with pdfplumber.open(pdf_path) as pdf:
        pg = pdf.pages[page_no - 1]
        words = pg.extract_words(use_text_flow=False, keep_blank_chars=False)
    if not words:
        return None
    lines: dict[int, list] = {}
    for w in words:
        key = round(w["top"] / 6)
        lines.setdefault(key, []).append(w)
    # تلاش برای یافتن خطِ شروع سؤال با شماره
    cands = []
    s = str(qnum) if qnum else ""
    rs = s[::-1] if len(s) >= 2 else ""
    for key in sorted(lines):
        ws = sorted(lines[key], key=lambda w: w["x0"])
        line_txt = nrm(" ".join(w["text"] for w in ws))
        hit = False
        if s and re.search(rf"(^|[\s\-–—ـ‌(،])({s}|{rs})([\s\-–—ـ‌).،]|[۰-۹]*[^\d]|$)", line_txt):
            # شماره باید نزدیک ابتدای خط باشد (خطوط RTL)
            if line_txt.find(s) < 6 or (rs and line_txt.find(rs) < 6):
                hit = True
        if hit:
            cands.append(key)
    if not cands:
        # توکن شاخص
        toks = [t for t in re.findall(r"[\u0600-\u06FF]{4,}|[A-Za-z]{3,}", q or "") if t not in STOP_TOKENS]
        toks.sort(key=len, reverse=True)
        for t in toks[:8]:
            for key in sorted(lines):
                line_txt = nrm(" ".join(w["text"] for w in sorted(lines[key], key=lambda w: w["x0"])))
                if t in line_txt or t[::-1] in line_txt:
                    cands.append(key)
                    break
            if cands:
                break
    if not cands:
        return None
    y0 = min(w["top"] for w in lines[cands[0]])
    # انتهای بلوک = ۴ خط بعدی یا خط سؤال بعدی
    nxt = cands[1] if len(cands) > 1 else cands[0] + 8
    y1 = max(w["bottom"] for k in range(cands[0], min(nxt + 1, max(lines) + 1)) for w in lines.get(k, [])) if nxt in lines else y0 + 120
    return y0, y1


def main() -> None:
    jobs = json.load(open(JOBS, encoding="utf-8"))
    os.makedirs(CROPS, exist_ok=True)
    done, fail = 0, []
    for j in jobs:
        qnum = j["n"] or 0
        try:
            loc = locate_y(j["pdf"], j["page"], qnum, (j["have"] or {}).get("q") or "")
        except Exception as e:
            loc = None
            print(j["id"], "locate err:", e)
        tag = re.sub(r"\W+", "_", os.path.splitext(os.path.basename(j["pdf"]))[0])[-40:]
        out = os.path.join(CROPS, f"{j['id']}.png")
        with pdfplumber.open(j["pdf"]) as pdf:
            pg = pdf.pages[j["page"] - 1]
            H = pg.height
            if loc:
                y0, y1 = max(0, loc[0] - 14), min(H, loc[1] + 14)
                if y1 - y0 < 90:
                    y1 = min(H, y0 + 130)
                im = pg.to_image(resolution=200).original
                im.crop((0, int(y0 * 200 / 72), im.width, int(y1 * 200 / 72))).save(out)
            else:
                # فالبک: کل صفحه
                im = pg.to_image(resolution=150).original
                im.save(out)
            done += 1
            print(j["id"], "→", "crop" if loc else "FULLPAGE", f"(page {j['page']})")
    print(f"done: {done}, out: {CROPS}")


if __name__ == "__main__":
    main()
