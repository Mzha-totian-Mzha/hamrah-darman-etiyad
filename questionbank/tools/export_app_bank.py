# -*- coding: utf-8 -*-
"""خروجی بانک سؤال برای اپ اندروید — فیلتر تمیز و ساخت asset.

ورودی : data/questionbank/bank.json
خروجی : repo/app/src/main/assets/questionbank.json

محتوا:
  mcq — فقط تستی‌های کلیددار (ans) با گزینه‌های کامل و کیفیت ok
  qa  — فقط تشریحی‌های پاسخ‌دار (≥۱۰ نویسه) با کیفیت ok
اجرا: python3 scripts/export_app_bank.py
"""
import io
import json
import os
from datetime import date

BANK = "/home/z/my-project/data/questionbank/bank.json"
OUT_DIR = "/home/z/my-project/repo/app/src/main/assets"
OUT = os.path.join(OUT_DIR, "questionbank.json")

LETTERS = ["الف", "ب", "ج", "د"]

def main() -> None:
    bank = json.load(io.open(BANK, encoding="utf-8"))
    mcq_out, qa_out = [], []
    cats = set()

    for x in bank["mcq"]:
        if x.get("quality") != "ok" or not x.get("ans"):
            continue
        opts = x.get("opts") or {}
        if len(opts) < 2 or x["ans"] not in opts:
            continue
        if len(x.get("q", "")) < 12:
            continue
        mcq_out.append({
            "id": x["id"],
            "q": x["q"],
            "opts": opts,
            "ans": x["ans"],
            "cat": x["cat"],
            "ref": "، ".join(x.get("refs") or []),
            "src": x["src"],
        })
        cats.add(x["cat"])

    for x in bank["qa"]:
        if x.get("quality") != "ok":
            continue
        a = x.get("a") or ""
        if len(a) < 10 or len(x.get("q", "")) < 12:
            continue
        qa_out.append({
            "id": x["id"],
            "q": x["q"],
            "a": a,
            "cat": x["cat"],
            "ref": "، ".join(x.get("refs") or []),
            "src": x["src"],
        })
        cats.add(x["cat"])

    out = {
        "meta": {
            "title": "بانک سؤالات آزمون — کنگره ۶۰ / متد DST",
            "version": 2,
            "built": str(date.today()),
            "counts": {"mcq": len(mcq_out), "qa": len(qa_out), "total": len(mcq_out) + len(qa_out)},
        },
        "cats": sorted(cats),
        "mcq": mcq_out,
        "qa": qa_out,
    }
    os.makedirs(OUT_DIR, exist_ok=True)
    with io.open(OUT, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, separators=(",", ":"))
    print(f"mcq={len(mcq_out)} qa={len(qa_out)} -> {OUT} ({os.path.getsize(OUT)//1024} KB)")

if __name__ == "__main__":
    main()
