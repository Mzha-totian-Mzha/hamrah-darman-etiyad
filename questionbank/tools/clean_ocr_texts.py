# -*- coding: utf-8 -*-
"""
پاک‌سازی متن‌های OCR — خوانا‌سازی متن کتابخانه و بانک سؤالات
۱) حذف کاراکترهای کنترلی جهت متن (bidi marks) که در ۲۷۴۴ متن باعث چسبیدن/جابجا شدن کلمات شده‌اند
۲) جایگزینی غلط‌های تایپی پرتکرار OCR با نسخهٔ درست
۳) بازنویسی دستی دو متنِ مشخص (وادی اول عشق + وظایف راهنمای تازه‌واردین)
۴) فاصله‌گذاری مجازی (نیم‌فاصله) فعل‌های «می ...»
خروجی: bank.json + passages.json و همهٔ کپی‌های اپ/وب
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]  # repo/
PROJECT = ROOT.parent                       # /home/z/my-project

BIDI = re.compile("[\u202a-\u202e\u2066-\u2069\u200e\u200f]")

# ترتیب مهم است: «نمیکند» قبل از «میکند»
REPLACEMENTS = [
    ("نمیکند", "نمی‌کند"),
    ("نمیشود", "نمی‌شود"),
    ("نمیشوند", "نمی‌شوند"),
    ("نمیکنند", "نمی‌کنند"),
    ("نمیکنیم", "نمی‌کنیم"),
    ("میتوانند", "می‌توانند"),
    ("میتواند", "می‌تواند"),
    ("میکند", "می‌کند"),
    ("میکنند", "می‌کنند"),
    ("میکندم", "می‌کنم"),
    ("میشود", "می‌شود"),
    ("میشوند", "می‌شوند"),
    ("میباشد", "می‌باشد"),
    ("میگوید", "می‌گوید"),
    ("می گوید", "می‌گوید"),
    ("می گیرد", "می‌گیرد"),
    ("سواالت", "سؤالات"),
    ("بالفاصله", "بلافاصله"),
    ("مشکالتی", "مشکلاتی"),
    ("تازه وادین", "تازه‌واردین"),
    ("تازه واردین", "تازه‌واردین"),
    ("تازه وارد ", "تازه‌وارد "),
    ("ایما ِ ن", "ایمان"),
    ("ایماِن", "ایمان"),
]

# بازنویسی دستی — متن‌هایی که نصفه شروع/تمام شده‌اند (بر اساس متن اصلی منابع)
MANUAL_REWRITES = {
    "pas-01-0392": (
        "انسانی که از خطوط قرمز عبور کند، تا زمانی که زنده است، خودش را در اکثر مواقع ترمیم خواهد کرد. "
        "با ایجاد مشکل در نهرهای خمر، بلافاصله در قسمت ایمنی جسم، مشکلاتی بروز می‌کند و در نتیجه، "
        "انواع و اقسام بیماری‌های جسمی، روحی و روانی وارد صحنه می‌شوند. "
        "در حقیقت، انسانی که دارای عقل سالم، عشق سالم و ایمان سالم باشد، شرایط زندگی خود را به‌گونه‌ای مهیا می‌کند "
        "که باعث سالم نگه‌داشتن و فعال بودن چشمه‌ها و نهرهای خمرِ خود باشد؛ "
        "ولی شخصی که از عقل و عشق و ایمان سالم برخوردار نیست، چنین شرایطی را نخواهد داشت."
    ),
    "pas-06-0010": (
        "وظایف راهنمای تازه‌واردین (از «راهنما و راهنمایی»): "
        "۷- راهنمای تازه‌واردین از ورود تازه‌واردین به لژیون تازه‌واردین خودداری کند و موضوع جهان‌بینی را در حد تعریف "
        "و به‌عنوان یک ضلع از مثلث درمان، و با استناد به جزوهٔ جهان‌بینی ۱، برای تازه‌وارد توضیح مختصر بدهد. "
        "۸- راهنمای تازه‌واردین بایستی با حوصله و صبر، پاسخگوی سؤالات تازه‌واردین باشد. "
        "۹- راهنمای تازه‌واردین بایستی به تازه‌واردین طریقهٔ سالمِ احوال‌پرسی در کنگره را آموزش دهد و توضیح دهد که "
        "برای جلوگیری از انتقال بیماری‌ها، از بوسیدن و دست‌دادن خودداری شود."
    ),
}


def clean_text(s: str) -> str:
    if not s:
        return s
    s = BIDI.sub("", s)
    for a, b in REPLACEMENTS:
        s = s.replace(a, b)
    s = s.replace(" ،", "،").replace(" .", ".")
    s = re.sub(r"[ \t]{2,}", " ", s)
    return s.strip()


def clean_obj(o):
    """پاک‌سازی بازگشتی همهٔ رشته‌های یک ساختار JSON"""
    if isinstance(o, str):
        return clean_text(o)
    if isinstance(o, list):
        return [clean_obj(x) for x in o]
    if isinstance(o, dict):
        return {k: (clean_obj(v) if k != "text" and not isinstance(v, str) else clean_text(v) if isinstance(v, str) else v) for k, v in o.items()}
    return o


def main():
    changed = {"bank": 0, "passages": 0, "rewrites": 0}

    # ---------- بانک سؤالات ----------
    bank_path = ROOT / "questionbank" / "bank.json"
    bank = json.loads(bank_path.read_text(encoding="utf-8"))
    for m in bank.get("mcq", []):
        for field in ("q", "ans_text"):
            if isinstance(m.get(field), str):
                new = clean_text(m[field])
                if new != m[field]:
                    m[field] = new
                    changed["bank"] += 1
        if isinstance(m.get("opts"), dict):
            for k, v in m["opts"].items():
                new = clean_text(v)
                if new != v:
                    m["opts"][k] = new
                    changed["bank"] += 1
    for q in bank.get("qa", []):
        for field in ("q", "a"):
            new = clean_text(q.get(field, ""))
            if new != q.get(field, ""):
                q[field] = new
                changed["bank"] += 1
    bank_path.write_text(json.dumps(bank, ensure_ascii=False), encoding="utf-8")

    # ---------- متن‌های کتابخانه ----------
    pas_path = ROOT / "app" / "src" / "main" / "assets" / "passages.json"
    pas = json.loads(pas_path.read_text(encoding="utf-8"))
    for item in pas.get("passages", []):
        t = item.get("text", "")
        if item.get("id") in MANUAL_REWRITES:
            item["text"] = MANUAL_REWRITES[item["id"]]
            changed["rewrites"] += 1
            continue
        new = clean_text(t)
        if new != t:
            item["text"] = new
            changed["passages"] += 1
    pas_text = json.dumps(pas, ensure_ascii=False)

    # ---------- نوشتن همهٔ کپی‌ها ----------
    targets_passages = [
        pas_path,
        ROOT / "web-preview" / "passages.json",
        PROJECT / "public" / "content" / "passages.json",
    ]
    for tp in targets_passages:
        tp.write_text(pas_text, encoding="utf-8")

    targets_bank = [
        ROOT / "app" / "src" / "main" / "assets" / "questionbank.json",
        ROOT / "web-preview" / "questionbank.json",
        PROJECT / "public" / "questionbank.json",
    ]
    bank_text = bank_path.read_text(encoding="utf-8")
    for tb in targets_bank:
        tb.write_text(bank_text, encoding="utf-8")

    print(json.dumps(changed, ensure_ascii=False))


if __name__ == "__main__":
    main()
