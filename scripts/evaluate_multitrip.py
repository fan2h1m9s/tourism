#!/usr/bin/env python3
"""
Minimal multi-itinerary evaluation script for ai-tourism demo.

Metrics:
1) Segment coverage (keyword hit ratio)
2) Constraint adherence (keyword hit ratio)
3) Stability (average similarity across repeated runs)
4) Long-session memory retention
5) Session isolation correctness

Usage:
  python scripts/evaluate_multitrip.py --base-url http://127.0.0.1:8080 --user-id demo-user
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import random
import string
import time
from dataclasses import dataclass
from difflib import SequenceMatcher
from pathlib import Path
from typing import Dict, List, Tuple
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


@dataclass
class EvalCase:
    case_id: str
    prompt: str
    expected_segments: List[str]
    expected_constraints: List[str]


def random_id(prefix: str) -> str:
    suffix = "".join(random.choices(string.ascii_lowercase + string.digits, k=10))
    return f"{prefix}-{suffix}"


def normalize_text(text: str) -> str:
    return " ".join((text or "").lower().split())


def contains_all(text: str, keywords: List[str]) -> Tuple[int, int, List[str]]:
    t = normalize_text(text)
    hits = [k for k in keywords if normalize_text(k) in t]
    return len(hits), len(keywords), hits


def avg_pairwise_similarity(texts: List[str]) -> float:
    if len(texts) < 2:
        return 1.0
    ratios = []
    for i in range(len(texts)):
        for j in range(i + 1, len(texts)):
            r = SequenceMatcher(None, normalize_text(texts[i]), normalize_text(texts[j])).ratio()
            ratios.append(r)
    return sum(ratios) / len(ratios) if ratios else 1.0


def post_chat_send(base_url: str, user_id: str, session_id: str, message: str, timeout: int = 60) -> Dict:
    url = base_url.rstrip("/") + "/chat/send"
    payload = json.dumps({
        "sessionId": session_id,
        "userId": user_id,
        "message": message,
    }).encode("utf-8")

    req = Request(url=url, data=payload, method="POST")
    req.add_header("Content-Type", "application/json")

    try:
        with urlopen(req, timeout=timeout) as resp:
            body = resp.read().decode("utf-8", errors="replace")
            data = json.loads(body)
            return {"ok": True, "status": resp.status, "data": data, "error": ""}
    except HTTPError as e:
        body = e.read().decode("utf-8", errors="replace") if hasattr(e, "read") else ""
        return {"ok": False, "status": e.code, "data": {}, "error": f"HTTPError: {e.code} {body}"}
    except URLError as e:
        return {"ok": False, "status": 0, "data": {}, "error": f"URLError: {e}"}
    except Exception as e:  # noqa: BLE001
        return {"ok": False, "status": 0, "data": {}, "error": f"Exception: {e}"}


def evaluate_case(base_url: str, user_id: str, case: EvalCase, repeats: int) -> Dict:
    answers: List[str] = []
    errors: List[str] = []

    for idx in range(repeats):
        session_id = random_id(f"eval-{case.case_id}-{idx}")
        result = post_chat_send(base_url, user_id, session_id, case.prompt)
        if not result["ok"]:
            errors.append(result["error"])
            continue
        answer = result["data"].get("answer", "")
        answers.append(answer)

    if not answers:
        return {
            "case_id": case.case_id,
            "prompt": case.prompt,
            "ok": False,
            "error_count": len(errors),
            "errors": errors,
            "segment_coverage": 0.0,
            "constraint_adherence": 0.0,
            "stability": 0.0,
            "samples": [],
        }

    segment_hits = []
    constraint_hits = []
    for answer in answers:
        sh, stotal, _ = contains_all(answer, case.expected_segments)
        ch, ctotal, _ = contains_all(answer, case.expected_constraints)
        segment_hits.append(sh / stotal if stotal else 1.0)
        constraint_hits.append(ch / ctotal if ctotal else 1.0)

    return {
        "case_id": case.case_id,
        "prompt": case.prompt,
        "ok": True,
        "error_count": len(errors),
        "errors": errors,
        "segment_coverage": sum(segment_hits) / len(segment_hits),
        "constraint_adherence": sum(constraint_hits) / len(constraint_hits),
        "stability": avg_pairwise_similarity(answers),
        "samples": answers[:2],
    }


def evaluate_long_session_memory(base_url: str, user_id: str) -> Dict:
    session_id = random_id("eval-long")
    token = "ZX-ALPHA-917"

    first = post_chat_send(
        base_url,
        user_id,
        session_id,
        f"Remember this token for later: {token}. Reply only 'ok'.",
    )
    if not first["ok"]:
        return {"ok": False, "score": 0.0, "error": first["error"]}

    filler_questions = [
        "Give one quick trip tip for Beijing.",
        "Give one quick trip tip for Shanghai.",
        "Give one quick trip tip for Guangzhou.",
        "Give one quick trip tip for Shenzhen.",
        "Give one quick trip tip for Hangzhou.",
        "Give one quick trip tip for Suzhou.",
        "Give one quick trip tip for Chengdu.",
        "Give one quick trip tip for Chongqing.",
        "Give one quick trip tip for Xian.",
        "Give one quick trip tip for Nanjing.",
        "Give one quick trip tip for Wuhan.",
        "Give one quick trip tip for Tianjin.",
        "Give one quick trip tip for Qingdao.",
        "Give one quick trip tip for Xiamen.",
        "Give one quick trip tip for Kunming.",
        "Give one quick trip tip for Sanya.",
        "Give one quick trip tip for Harbin.",
        "Give one quick trip tip for Dalian.",
        "Give one quick trip tip for Guilin.",
        "Give one quick trip tip for Lhasa.",
        "Give one quick trip tip for Urumqi.",
        "Give one quick trip tip for Dunhuang.",
    ]

    for q in filler_questions:
        r = post_chat_send(base_url, user_id, session_id, q)
        if not r["ok"]:
            return {"ok": False, "score": 0.0, "error": r["error"]}

    final = post_chat_send(base_url, user_id, session_id, "What is the token I asked you to remember?")
    if not final["ok"]:
        return {"ok": False, "score": 0.0, "error": final["error"]}

    answer = final["data"].get("answer", "")
    score = 1.0 if token.lower() in answer.lower() else 0.0
    return {"ok": True, "score": score, "answer": answer}


def evaluate_session_isolation(base_url: str, user_id: str) -> Dict:
    session_a = random_id("eval-iso-a")
    session_b = random_id("eval-iso-b")

    code_a = "REDA"
    code_b = "BLUEB"

    r1 = post_chat_send(base_url, user_id, session_a, f"Remember my city code is {code_a}. Reply ok.")
    r2 = post_chat_send(base_url, user_id, session_b, f"Remember my city code is {code_b}. Reply ok.")
    if not r1["ok"]:
        return {"ok": False, "score": 0.0, "error": r1["error"]}
    if not r2["ok"]:
        return {"ok": False, "score": 0.0, "error": r2["error"]}

    qa = post_chat_send(base_url, user_id, session_a, "What is my city code?")
    qb = post_chat_send(base_url, user_id, session_b, "What is my city code?")
    if not qa["ok"]:
        return {"ok": False, "score": 0.0, "error": qa["error"]}
    if not qb["ok"]:
        return {"ok": False, "score": 0.0, "error": qb["error"]}

    ans_a = qa["data"].get("answer", "")
    ans_b = qb["data"].get("answer", "")

    ok_a = code_a.lower() in ans_a.lower() and code_b.lower() not in ans_a.lower()
    ok_b = code_b.lower() in ans_b.lower() and code_a.lower() not in ans_b.lower()

    score = 1.0 if (ok_a and ok_b) else 0.0
    return {"ok": True, "score": score, "answer_a": ans_a, "answer_b": ans_b}


def build_default_cases() -> List[EvalCase]:
    return [
        EvalCase(
            case_id="multi_city_budget",
            prompt=(
                "Plan a 5-day trip: first 2 days in Beijing for family travel, "
                "then 3 days in Tianjin for food and city walk, budget under 6000 RMB."
            ),
            expected_segments=["beijing", "tianjin", "day 1", "day 5"],
            expected_constraints=["6000", "family", "food"],
        ),
        EvalCase(
            case_id="three_profiles",
            prompt=(
                "For the same destination Chengdu, provide three itineraries: "
                "student low-budget, family-friendly, and slow-paced for seniors."
            ),
            expected_segments=["student", "family", "senior"],
            expected_constraints=["budget", "pace", "chengdu"],
        ),
        EvalCase(
            case_id="mixed_purpose",
            prompt=(
                "I have a mixed trip: 2 days Shanghai business, 1 day Suzhou leisure, "
                "2 days Hangzhou photography. Please separate each part clearly."
            ),
            expected_segments=["shanghai", "suzhou", "hangzhou"],
            expected_constraints=["business", "leisure", "photography"],
        ),
    ]


def compute_overall(case_results: List[Dict], long_mem: Dict, isolation: Dict) -> Dict:
    valid = [c for c in case_results if c.get("ok")]
    if valid:
        coverage = sum(c["segment_coverage"] for c in valid) / len(valid)
        adherence = sum(c["constraint_adherence"] for c in valid) / len(valid)
        stability = sum(c["stability"] for c in valid) / len(valid)
    else:
        coverage = adherence = stability = 0.0

    return {
        "segment_coverage": coverage,
        "constraint_adherence": adherence,
        "stability": stability,
        "long_session_memory": long_mem.get("score", 0.0),
        "session_isolation": isolation.get("score", 0.0),
    }


def write_markdown_report(md_path: Path, report: Dict) -> None:
    lines = []
    lines.append("# Multi-itinerary Evaluation Report")
    lines.append("")
    lines.append(f"Generated at: {report['generated_at']}")
    lines.append(f"Base URL: {report['base_url']}")
    lines.append("")

    o = report["overall"]
    lines.append("## Overall Metrics")
    lines.append("")
    lines.append("| Metric | Score |")
    lines.append("|---|---:|")
    lines.append(f"| Segment coverage | {o['segment_coverage']:.3f} |")
    lines.append(f"| Constraint adherence | {o['constraint_adherence']:.3f} |")
    lines.append(f"| Stability | {o['stability']:.3f} |")
    lines.append(f"| Long-session memory | {o['long_session_memory']:.3f} |")
    lines.append(f"| Session isolation | {o['session_isolation']:.3f} |")
    lines.append("")

    lines.append("## Case Details")
    lines.append("")
    lines.append("| Case | Segment coverage | Constraint adherence | Stability | Errors |")
    lines.append("|---|---:|---:|---:|---:|")
    for c in report["cases"]:
        lines.append(
            f"| {c['case_id']} | {c['segment_coverage']:.3f} | {c['constraint_adherence']:.3f} | {c['stability']:.3f} | {c['error_count']} |"
        )
    lines.append("")

    md_path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Evaluate multi-itinerary quality for ai-tourism demo")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080", help="Backend base URL")
    parser.add_argument("--user-id", default="demo-user", help="Business user ID")
    parser.add_argument("--repeats", type=int, default=3, help="Repeats per case for stability")
    parser.add_argument("--json-out", default="reports/multitrip_eval_report.json", help="JSON report output path")
    parser.add_argument("--md-out", default="reports/multitrip_eval_report.md", help="Markdown report output path")
    args = parser.parse_args()

    start = time.time()
    cases = build_default_cases()
    case_results = [evaluate_case(args.base_url, args.user_id, c, args.repeats) for c in cases]
    long_mem = evaluate_long_session_memory(args.base_url, args.user_id)
    isolation = evaluate_session_isolation(args.base_url, args.user_id)

    report = {
        "generated_at": dt.datetime.now().isoformat(timespec="seconds"),
        "base_url": args.base_url,
        "user_id": args.user_id,
        "repeats": args.repeats,
        "elapsed_seconds": round(time.time() - start, 3),
        "cases": case_results,
        "long_session_memory": long_mem,
        "session_isolation": isolation,
        "overall": compute_overall(case_results, long_mem, isolation),
    }

    json_path = Path(args.json_out)
    md_path = Path(args.md_out)
    json_path.parent.mkdir(parents=True, exist_ok=True)
    md_path.parent.mkdir(parents=True, exist_ok=True)

    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    write_markdown_report(md_path, report)

    print("Evaluation completed")
    print(f"JSON report: {json_path}")
    print(f"Markdown report: {md_path}")
    print(json.dumps(report["overall"], ensure_ascii=False, indent=2))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
