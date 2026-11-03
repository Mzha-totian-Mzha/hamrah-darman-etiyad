# -*- coding: utf-8 -*-
"""برش دوم — مواردی که برش اول نامنطبق بود. توکن شاخص → برش ۲۵۰dpi."""
import json
import os
import re
import sys
import unicodedata

import pdfplumber

BASE = "/home/z/my-project/data/questionbank"
CROPS = os.path.join(BASE, "crops")
UPLOAD = "/home/z/my-project/upload/آزمون_extracted"

FA2EN = str.maketrans("۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩", "01234567890123456789")

# id → (pdf, page, [tokens], pad_before_lines, pad_after_lines)
FIX = {
    "m-00259": ("exam07.pdf", 2, ["هروئین"], 0, 6),
    "m-00203": ("exam06.pdf", 1, ["صحیح", "151"], 0, 7),
    "m-00353": ("آزمون جامع همسفران آقا 1404.pdf", 4, ["مواد", "برنامه"], 0, 6),
    "m-00358": ("آزمون جامع همسفران آقا 1404.pdf", 5, ["تعادل"], 0, 6),
    "m-00360": ("آزمون جامع همسفران آقا 1404.pdf", 5, ["شربت", "OT"], 0, 6),
    "m-00341": ("exam10.pdf", 2, ["خلقت"], 0, 6),
    "m-00342": ("exam10.pdf", 2, ["مرزبانان"], 0, 6),
    "m-00631": ("آزمون مرداد ماه 1404 (1).pdf", 1, ["باورها"], 0, 6),
    "m-00683": ("آزمون نهایی همسفران آقا 144504.pdf", 4, ["ذخیره"], 0, 7),
    "m-00756": ("آزمون همسفران آقا اردیبهشت ماه 1404.pdf", 8, ["صبر", "عجله"], 2, 6),
    "m-00897": ("آزمون چهارم.pdf", 1, ["کنترل"], 0, 6),
    "m-01228": ("سوال.pdf", 4, ["وادی پنجم", "پنجم"], 0, 6),
    "m-01293": ("سوالات2.pdf", 1, ["انرژی", "لذت"], 0, 6),
    "m-00197": ("exam05.pdf", 2, ["دانایی"], 0, 4),
}


def nrm(t: str) -> str:
    t = unicodedata.normalize("NFKC", t or "").translate(FA2EN)
    t = re.sub(r"[\u200c\u064b-\u065f]", "", t)
    return re.sub(r"[ \t\u00a0]+", " ", t).strip().lower()


def main() -> None:
    os.makedirs(CROPS, exist_ok=True)
    for qid, (pdf, page, toks, pad_b, pad_a) in FIX.items():
        path = os.path.join(UPLOAD, pdf)
        with pdfplumber.open(path) as doc:
            pg = doc.pages[page - 1]
            words = pg.extract_words()
        lines: dict[int, list] = {}
        for w in words:
            lines.setdefault(round(w["top"] / 8), []).append(w)
        keys = sorted(lines)
        hit = None
        for key in keys:
            line_txt = nrm(" ".join(w["text"] for w in sorted(lines[key], key=lambda x: x["x0"], reverse=True)))
            rev = line_txt[::-1]
            if any(nrm(t) in line_txt or nrm(t) in rev or nrm(t)[::-1] in line_txt for t in toks):
                hit = key
                break
        if hit is None:
            print(qid, "TOKEN NOT FOUND on page", page)
            continue
        idx = keys.index(hit)
        lo = keys[max(0, idx - pad_b)]
        hi = keys[min(len(keys) - 1, idx + pad_a)]
        y0 = min(w["top"] for w in lines[lo]) - 10
        y1 = max(w["bottom"] for w in lines[hi]) + 10
        with pdfplumber.open(path) as doc:
            im = doc.pages[page - 1].to_image(resolution=250).original
        im.crop((0, max(0, int(y0 * 250 / 72)), im.width, min(im.height, int(y1 * 250 / 72)))).save(
            os.path.join(CROPS, f"{qid}.png"))
        print(qid, "→ re-crop ok (page", page, ", y", int(y0), "-", int(y1), ")")


if __name__ == "__main__":
    main()
