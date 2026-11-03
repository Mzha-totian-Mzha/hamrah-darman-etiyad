# -*- coding: utf-8 -*-
"""اعمال رونویسی‌های مدل‌بینایی + ارتقای تشریحی‌ها + کلید جدید آزمون چهارم.

ورودی: bank.json + manual_fixes.json
خروجی: bank.json (به‌روز) + گزارش
اجرا: python3 scripts/apply_manual_fixes.py
"""
import json
import re
import sys

sys.path.insert(0, "/home/z/my-project/scripts")
from build_bank import quality_of, quality_q  # noqa: E402

BASE = "/home/z/my-project/data/questionbank"
FA2EN = str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789")
EN2FA = str.maketrans("0123456789", "۰۱۲۳۴۵۶۷۸۹")


def dnorm(s: str) -> str:
    return (s or "").translate(FA2EN)


def fa_num(s: str) -> str:
    return (s or "").translate(EN2FA)


def main() -> None:
    bank = json.load(open(f"{BASE}/bank.json", encoding="utf-8"))
    fixes = json.load(open(f"{BASE}/manual_fixes.json", encoding="utf-8"))
    keys = json.load(open(f"{BASE}/answer_keys.json", encoding="utf-8"))

    # کلید جدید سوال آزمون چهارم (هایلایت زرد — خوانده‌شده بصری)
    if "azmoon4_soval" not in keys:
        keys["azmoon4_soval"] = fixes["azmoon4_key"]
        json.dump(keys, open(f"{BASE}/answer_keys.json", "w", encoding="utf-8"),
                  ensure_ascii=False, indent=1)

    mcq = {q["id"]: q for q in bank["mcq"]}
    qa = {q["id"]: q for q in bank["qa"]}
    log = {"fixed": 0, "keyed": 0, "dup": 0, "to_qa": 0, "broken": 0, "qa_promoted": 0, "qa_deleted": 0}

    # ------------------------------------------------ اصلاح MCQ
    for qid, fx in fixes["fixes"].items():
        q = mcq.get(qid)
        if q is None:
            print("!! MISSING", qid)
            continue
        act = fx["action"]
        if act == "dup_of":
            q["quality"] = "dup"
            q["dup"] = fx["target"]
            q["ans"] = None
            q["opts"] = {}
            log["dup"] += 1
            continue
        if act == "broken_source":
            q["quality"] = "broken-source"
            q["note"] = fx.get("note", "")
            log["broken"] += 1
            continue
        if act == "verify_only":
            if q.get("quality") == "review":
                q["quality"] = "ok"
            q["fix"] = "vision-verified"
            log["fixed"] += 1
            continue
        # fix / cross_fill
        if fx.get("q"):
            q["q"] = fx["q"]
        if fx.get("opts"):
            q["opts"] = fx["opts"]
        if fx.get("n"):
            q["n"] = fx["n"]
        # کلید: از answer_keys برای examهای کلیددار
        ans = fx.get("ans")
        if not ans and fx.get("exam"):
            kmap = keys.get(fx["exam"]) or {}
            ns = str(fx["n"])
            ansk = kmap.get(fa_num(ns)) or kmap.get(ns)
            if ansk and ansk in (q.get("opts") or {}):
                ans = ansk
        if ans and ans in (q.get("opts") or {}):
            q["ans"] = ans
            log["keyed"] += ans == fx.get("ans") or True
        elif ans and ans not in (q.get("opts") or {}):
            print(f"!! KEY MISMATCH {qid}: {ans} not in opts")
        # کیفیت: رونویسی دستی است؛ هیوریستیک OCR لازم ندارد
        q["quality"] = "ok"
        if len(q.get("opts") or {}) == 4 and q.get("ans"):
            q["fix"] = "vision-transcribed"
        log["fixed"] += 1

    # ------------------------------------------------ انتقال MCQ → QA
    new_qa = []
    for qid, fx in fixes["fixes"].items():
        if fx["action"] != "to_qa":
            continue
        q = mcq.get(qid)
        if q is None:
            continue
        qa_item = {
            "id": q["id"].replace("m-", "q-", 1) if q["id"].startswith("m-") else q["id"],
            "n": q.get("n"),
            "q": fx["q"],
            "a": fx.get("a", ""),
            "refs": q.get("refs") or [],
            "src": q["src"],
            "cat": q.get("cat"),
            "quality": "ok",
            "fix": "vision-transcribed" if fx.get("a") else "to-qa",
        }
        # id تداخلی؟
        if qa_item["id"] in qa:
            qa_item["id"] = qa_item["id"] + "-b"
        new_qa.append(qa_item)
        q["quality"] = "moved-to-qa"
        q["opts"] = {}
        q["ans"] = None
        log["to_qa"] += 1

    # ------------------------------------------------ ارتقای QA
    for qid, fx in fixes["qa_promote"].items():
        q = qa.get(qid)
        if q is None:
            print("!! QA MISSING", qid)
            continue
        if fx.get("q"):
            q["q"] = fx["q"]
        if fx.get("q_strip_lead"):
            q["q"] = re.sub(r"^\s*\d{1,3}\s*", "", q["q"]).strip()
        if fx.get("a"):
            q["a"] = fx["a"]
        if fx.get("refs"):
            q["refs"] = fx["refs"]
        q["quality"] = "ok"
        q["fix"] = "human-review-promoted"
        log["qa_promoted"] += 1

    for qid in fixes["qa_delete"]:
        if qid in qa:
            qa[qid]["quality"] = "deleted-fragment"
            log["qa_deleted"] += 1

    # ------------------------------------------------ کلید آزمون چهارم برای آیتم‌هایش
    k4 = keys["azmoon4_soval"]
    for q in bank["mcq"]:
        if q["src"] == fixes["azmoon4_applies_to"] and q.get("quality") == "ok" and not q.get("ans"):
            ansk = k4.get(str(q.get("n") or ""))
            if ansk and ansk in (q.get("opts") or {}):
                q["ans"] = ansk

    # ------------------------------------------------ بازنویسی بانک
    bank["mcq"] = [q for q in bank["mcq"] if q.get("quality") not in ("deleted",)]
    bank["qa"].extend(new_qa)

    n_mcq_ok = sum(1 for q in bank["mcq"] if q["quality"] == "ok")
    n_mcq_key = sum(1 for q in bank["mcq"] if q["quality"] == "ok" and q.get("ans"))
    n_qa_ok = sum(1 for q in bank["qa"] if q["quality"] == "ok" and (q.get("a") or "").strip())
    bank["meta"]["counts"] = {
        "mcq": len(bank["mcq"]),
        "mcq_ok": n_mcq_ok,
        "mcq_ok_with_key": n_mcq_key,
        "mcq_review": sum(1 for q in bank["mcq"] if q["quality"] == "review"),
        "mcq_broken_source": sum(1 for q in bank["mcq"] if q["quality"] == "broken-source"),
        "mcq_dup": sum(1 for q in bank["mcq"] if q["quality"] == "dup"),
        "qa": len(bank["qa"]),
        "qa_ok": n_qa_ok,
        "qa_review": sum(1 for q in bank["qa"] if q["quality"] == "review"),
        "total": len(bank["mcq"]) + len(bank["qa"]),
    }
    bank["meta"]["vision_review"] = {
        "date": "2026-07-04",
        "method": "manual vision transcription from 200-250dpi PDF page crops",
        "fixes_file": "manual_fixes.json",
    }
    json.dump(bank, open(f"{BASE}/bank.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(json.dumps(log, ensure_ascii=False))
    print(json.dumps(bank["meta"]["counts"], ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
