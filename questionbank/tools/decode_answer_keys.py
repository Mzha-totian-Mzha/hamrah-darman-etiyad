# -*- coding: utf-8 -*-
"""رمزگشایی کلید آزمون‌ها از پاسخ‌نامه‌های دایره‌کشی‌شدهٔ PDF.

الگوی پاسخ‌نامه: هر سؤال با همهٔ گزینه‌ها بازچاپ می‌شود و گزینهٔ درست با یک
دایره (منحنی وکتوری) دور حرفش مشخص شده است. با pdfplumber مختصات دایره‌ها و
واژه‌ها را می‌گیریم و حرفِ داخل هر دایره را برمی‌داریم.

خروجی: /home/z/my-project/data/questionbank/answer_keys.json
    { "exam01": {"1": "ب", "2": "د", ...}, ... }

اجرا: python3 scripts/decode_answer_keys.py
"""
import io
import json
import os
import re
import unicodedata

import pdfplumber

UPLOAD = "/home/z/my-project/upload"
EXTRACTED = os.path.join(UPLOAD, "آزمون_extracted")
OUT = "/home/z/my-project/data/questionbank/answer_keys.json"
OUT_TEXT = "/home/z/my-project/data/questionbank/answer_keys_text.json"

def resolve(fname: str) -> str | None:
    for d in (UPLOAD, EXTRACTED):
        p = os.path.join(d, fname)
        if os.path.exists(p):
            return p
    return None

EXAMS = {
    "exam01": "exam01_answers.pdf",
    "exam02": "exam02_answers.pdf",
    "exam03": "exam03_answers.pdf",
    "exam04": "exam04_answers.pdf",
    "exam05": "exam05_answers.pdf",
    "exam06": "exam06_answers.pdf",
    "exam08": "exam08_answers.pdf",
    "jam3_18mordad": "نهایی_پاسخ_نامه_آزمون_جامع_سوم_،_18_مرداد.pdf",
}

AR = str.maketrans("٠١٢٣٤٥٦٧٨٩", "۰۱۲۳۴۵۶۷۸۹")

def norm(s: str) -> str:
    s = unicodedata.normalize("NFKC", s or "").translate(AR)
    return re.sub(r"\s+", " ", s).strip()

COMMON = set(
    """از به که است می برای این با چه کدام همه باید در را و تا خود یک شود انسان کنگره مسافر راهنما
    همسفر مصرف درمان ماده اول دوم سوم چهارم وادی مقاله صفحه کتاب جلسه پله سقف زندگی قدرت تفکر""".split()
)

def _score(t: str) -> float:
    ws = re.findall(r"[\u0600-\u06FF\u200c]+", t)
    if not ws:
        return 0.0
    return sum(1 for w in ws if w.strip("\u200c") in COMMON) / len(ws)

def logical(t: str) -> str:
    """متن دیداری معکوس را در صورت نیاز به ترتیب منطقی برمی‌گرداند."""
    return t if _score(t) >= _score(t[::-1]) else t[::-1]

# شمارهٔ سؤال — استخراج PDF ترتیب دیداری است: هم «١-» و هم «-١» دیده می‌شود
QNUM = re.compile(r"^[-–—ـ]?\s*([۰-۹]{1,3})\s*[-–—ـ]?$")
LETTERS = {"الف", "ب", "ج", "د"}

def circle_boxes(page) -> list[tuple[float, float, float, float]]:
    """صندوق دایره‌ها/بیضی‌های وکتوری صفحه."""
    boxes = []
    for c in page.curves + page.edges:
        x0, x1 = c.get("x0"), c.get("x1")
        top, bottom = c.get("top"), c.get("bottom")
        if None in (x0, x1, top, bottom):
            continue
        w, h = x1 - x0, bottom - top
        if 4 <= w <= 46 and 4 <= h <= 46 and 0.5 <= (w / max(h, 0.1)) <= 2.5:
            boxes.append((x0, top, x1, bottom))
    return boxes

def decode(path: str) -> tuple[dict[str, str], dict[str, str]]:
    """(کلیدها، متن هر شماره): حرف گزینهٔ دایره‌شده + استنلاف متن سؤال هر شماره."""
    keys: dict[str, str] = {}
    qtext: dict[str, str] = {}
    with pdfplumber.open(path) as pdf:
        for page in pdf.pages:
            W = page.width
            words = page.extract_words(use_text_flow=False, keep_blank_chars=False)
            # خطوط سؤال: واژهٔ عدد+خط تیره (دیداری معکوس: «-١»)
            q_rows: list[tuple[float, str]] = []  # (y, qnum)
            for w in words:
                t = norm(w["text"])
                m = QNUM.match(t)
                if m and w["x0"] > W * 0.72:
                    q_rows.append((w["top"], m.group(1)))
            q_rows.sort()
            # متن هر شماره: همان سطرِ شماره + سطرهای بعدی تا شمارهٔ بعد (RTL: از راست به چپ)
            # قطع در اولین واژهٔ حرف گزینه (شروع سطر گزینه‌ها)
            for i, (qy, qnum) in enumerate(q_rows):
                nxt = q_rows[i + 1][0] if i + 1 < len(q_rows) else qy + 90
                seg = [w for w in words if (qy - 3 <= w["top"] <= qy + 4) or (qy + 6 < w["top"] < min(nxt, qy + 90))]
                seg.sort(key=lambda w: (-1 if w["top"] <= qy + 4 else 0, -w["x0"]))
                parts = []
                for w in seg[:16]:
                    t = norm(w["text"]).strip("()«»-ـ ")
                    if t in LETTERS or t[::-1] in LETTERS:
                        break
                    if QNUM.match(t) or QNUM.match(t[::-1]):
                        continue  # واژهٔ شمارهٔ خود سؤال
                    parts.append(norm(w["text"]))
                txt = logical(" ".join(parts))
                if qnum not in qtext and len(txt) > 12:
                    qtext[qnum] = txt
            circles = circle_boxes(page)
            for (cx0, ctop, cx1, cbottom) in circles:
                ccx, ccy = (cx0 + cx1) / 2, (ctop + cbottom) / 2
                # واژهٔ حرف گزینه درون/نزدیک دایره — متن دیداری معکوس است: «(فلا» = «الف(»
                best, best_d = None, 1e9
                for w in words:
                    t = norm(w["text"])
                    cands = {t.strip("()«»-ـ "), t[::-1].strip("()«»-ـ ")}
                    if not (cands & LETTERS):
                        continue
                    wx, wy = (w["x0"] + w["x1"]) / 2, (w["top"] + w["bottom"]) / 2
                    d = ((wx - ccx) ** 2 + (wy - ccy) ** 2) ** 0.5
                    if d < best_d:
                        best, best_d = (cands & LETTERS).pop(), d
                if best is None or best_d > 26:
                    continue
                # نزدیک‌ترین شمارهٔ سؤال بالای این دایره (همین ستون/صفحه)
                cand = [q for q in q_rows if q[0] <= ctop + 4]
                if not cand:
                    continue
                qy, qnum = max(cand, key=lambda q: q[0])
                if ctop - qy > 140:  # دایره خیلی پایین‌تر از سؤال است
                    continue
                keys[qnum] = best
    return keys, qtext

def main() -> None:
    out = {}
    texts = {}
    for name, fname in EXAMS.items():
        p = resolve(fname)
        if not p:
            print(f"!! یافت نشد: {fname}")
            continue
        try:
            keys, qtext = decode(p)
        except Exception as ex:
            print(f"!! خطا در {fname}: {ex}")
            continue
        out[name] = dict(sorted(keys.items(), key=lambda kv: int(kv[0])))
        texts[name] = dict(sorted(qtext.items(), key=lambda kv: int(kv[0])))
        # آمار حروف
        from collections import Counter
        cnt = Counter(out[name].values())
        print(f"{name}: {len(keys)} کلید · {len(qtext)} متن — {dict(cnt)}")
    with io.open(OUT, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
    with io.open(OUT_TEXT, "w", encoding="utf-8") as f:
        json.dump(texts, f, ensure_ascii=False, indent=1)
    print(f"\nanswer_keys.json -> {os.path.getsize(OUT)} bytes | answer_keys_text.json -> {os.path.getsize(OUT_TEXT)} bytes")

if __name__ == "__main__":
    main()
