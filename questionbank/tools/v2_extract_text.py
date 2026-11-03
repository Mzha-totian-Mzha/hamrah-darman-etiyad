# -*- coding: utf-8 -*-
"""فاز صفر بازسازی v2 — استخراج متنِ تمیز از همهٔ PDF/DOCXهای منبع.

مشکل پایپ‌لاین v1: pypdf متن فارسیِ «Print To PDF» را معکوس/تخریب می‌کرد.
ابزار pdftotext (poppler) همان متن را با ترتیب منطقیِ سالم برمی‌گرداند.

خروجی: data/questionbank/text_v2/<stem>.txt
اجرا: python3 scripts/v2_extract_text.py
"""
import glob
import hashlib
import io
import json
import os
import re
import subprocess
import zipfile

UPLOAD = "/home/z/my-project/upload"
EXTRACTED = os.path.join(UPLOAD, "آزمون_extracted")
OUT = "/home/z/my-project/data/questionbank/text_v2"

BIDI = dict.fromkeys(map(ord, "\u202a\u202b\u202c\u202d\u202e\u200e\u200f\ufeff\u2066\u2067\u2068\u2069"), None)

def docx_text(path: str) -> str:
    try:
        with zipfile.ZipFile(path) as z:
            xml = z.read("word/document.xml").decode("utf-8", "ignore")
    except Exception:
        return ""
    xml = re.sub(r"<w:p[ >]", "\n<w:p ", xml)
    xml = re.sub(r"<w:tab[^>]*/>", "\t", xml)
    xml = re.sub(r"<w:br[^>]*/>", "\n", xml)
    txt = re.sub(r"<[^>]+>", "", xml)
    return txt.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", '"').replace("&apos;", "'")

def main() -> None:
    os.makedirs(OUT, exist_ok=True)
    seen_hash = {}
    report = []
    pdfs = []
    for d in (UPLOAD, EXTRACTED):
        pdfs += glob.glob(os.path.join(d, "*.pdf"))
    # docxها هم اضافه شوند
    docxs = glob.glob(os.path.join(EXTRACTED, "*.docx")) + glob.glob(os.path.join(UPLOAD, "*.docx"))
    for p in sorted(pdfs):
        stem = os.path.splitext(os.path.basename(p))[0]
        try:
            r = subprocess.run(["pdftotext", "-enc", "UTF-8", p, "-"], capture_output=True, timeout=120)
            txt = r.stdout.decode("utf-8", "ignore")
        except Exception as ex:
            txt = ""
        txt = txt.translate(BIDI)
        h = hashlib.md5(txt.encode()).hexdigest()[:8]
        dup_of = seen_hash.get(h)
        if not dup_of and len(txt.strip()) > 50:
            seen_hash[h] = stem
        with io.open(os.path.join(OUT, stem + ".txt"), "w", encoding="utf-8") as f:
            f.write(txt)
        report.append({"file": stem, "bytes": len(txt), "dup_of": dup_of if dup_of != stem else None})
    for p in sorted(docxs):
        stem = os.path.splitext(os.path.basename(p))[0]
        txt = docx_text(p).translate(BIDI)
        with io.open(os.path.join(OUT, stem + ".txt"), "w", encoding="utf-8") as f:
            f.write(txt)
        report.append({"file": stem, "bytes": len(txt), "dup_of": None})
    with io.open(os.path.join(OUT, "_report.json"), "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=1)
    empty = [r["file"] for r in report if r["bytes"] < 50 and not r["dup_of"]]
    dups = [(r["file"], r["dup_of"]) for r in report if r["dup_of"]]
    print(f"متن استخراج‌شده: {len(report)} فایل → {OUT}")
    print(f"بدون متنِ قابل استخراج (اسکن تصویری) [{len(empty)}]: {', '.join(empty)}")
    print(f"محتوای تکراری [{len(dups)}]: {dups}")

if __name__ == "__main__":
    main()
