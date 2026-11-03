#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""ساخت بانک پاساژهای خواندنی از کتاب‌های متنی کنگره ۶۰ — برای چرخش هوشمند کتابخانه"""
import subprocess, json, re, sys, collections
from datetime import date

UPLOAD = "/home/z/my-project/upload/"
BOOKS = [
    ("۱۴وادی.pdf",                "چهارده وادی عشق"),
    ("۱۴مقاله.pdf",               "چهارده مقاله"),
    ("۶۰ درجه.pdf",               "کتاب ۶۰ درجه"),
    ("راهنما و راهنمایی.pdf",      "راهنما و راهنمایی"),
    ("(ok)جهان‌بینی 1.pdf",        "جهان‌بینی ۱"),
    ("راهنمای تازه واردین.pdf",    "راهنمای تازه‌واردین"),
    ("پروتکل DST.pdf",            "پروتکل DST"),
    ("اصلاحیه پروتکل  DST.pdf",    "اصلاحیه پروتکل DST"),
    ("مثلث‌های آموزشی کنگره ۶۰.pdf", "مثلث‌های آموزشی"),
]
MIN_PASS, MAX_PASS, MIN_KEEP = 280, 520, 130

def norm(s: str) -> str:
    s = s.replace("ي", "ی").replace("ك", "ک").replace("\u00a0", " ")
    return re.sub(r"[ \t]+", " ", s).strip()

def extract(path: str) -> str:
    txt = subprocess.run(["pdftotext", "-enc", "UTF-8", path, "-"],
                         capture_output=True, text=True).stdout
    return txt

def clean_lines(txt: str):
    lines = [norm(l) for l in txt.splitlines()]
    lines = [l for l in lines if l]
    # سرصفحه/پاصفحهٔ تکراری: خط‌های کوتاهی که بیش از ۴ بار تکرار شده‌اند
    freq = collections.Counter(l for l in lines if len(l) < 45)
    junk = {l for l, c in freq.items() if c > 4}
    out = []
    for l in lines:
        if l in junk: continue
        if re.fullmatch(r"[\d۰-۹+\-–—.,/():«»\s]+", l): continue  # فقط عدد/نشانه
        if re.fullmatch(r"page \d+ of \d+", l, re.I): continue
        out.append(l)
    return out

SENT_SPLIT = re.compile(r"(?<=[.!?؟…؛:])\s+")

def passages_from(txt: str):
    # پاراگراف‌ها را به هم بچسبان و جمله‌بندی کن
    big = re.sub(r"\s+", " ", txt)
    sents = [s.strip() for s in SENT_SPLIT.split(big) if s.strip()]
    passes, buf = [], ""
    for s in sents:
        if len(s) > MAX_PASS:
            if len(buf) >= MIN_KEEP: passes.append(buf)
            buf = ""
            # جملهٔ خیلی بلند را تکه‌تکه کن
            for i in range(0, len(s), MAX_PASS):
                piece = s[i:i + MAX_PASS].strip()
                if len(piece) >= MIN_KEEP: passes.append(piece)
            continue
        if len(buf) + len(s) + 1 > MAX_PASS and len(buf) >= MIN_KEEP:
            passes.append(buf); buf = s
        else:
            buf = (buf + " " + s).strip()
    if len(buf) >= MIN_KEEP: passes.append(buf)
    # حذف تکراری و زباله
    seen, out = set(), []
    for p in passes:
        key = p[:80]
        if key in seen: continue
        digits = sum(c.isdigit() for c in p)
        if digits > len(p) * 0.35: continue  # بیش از حد عدد
        seen.add(key); out.append(p)
    return out

def main():
    all_pass, sources = [], {}
    for i, (pdf, name) in enumerate(BOOKS, 1):
        txt = extract(UPLOAD + pdf)
        lines = clean_lines(txt)
        passes = passages_from(" \n".join(lines))
        sources[name] = len(passes)
        for n, p in enumerate(passes, 1):
            all_pass.append({"id": f"pas-{i:02d}-{n:04d}", "src": name, "n": n, "text": p})
        print(f"{name}: {len(passes)} passages ({len(txt)} chars raw)")
    doc = {"generated": str(date.today()), "total": len(all_pass), "sources": sources, "passages": all_pass}
    for out in ["/home/z/my-project/public/content/passages.json",
                "/home/z/my-project/repo/app/src/main/assets/passages.json",
                "/home/z/my-project/repo/web-preview/passages.json"]:
        with open(out, "w", encoding="utf-8") as f:
            json.dump(doc, f, ensure_ascii=False, separators=(",", ":"))
    print("TOTAL:", len(all_pass))

if __name__ == "__main__":
    main()
