## Multi-itinerary Evaluation

Run backend first, then execute:

```bash
python scripts/evaluate_multitrip.py --base-url http://127.0.0.1:8080 --user-id demo-user
```

Outputs:

- JSON report: reports/multitrip_eval_report.json
- Markdown report: reports/multitrip_eval_report.md

Metrics include:

- segment coverage
- constraint adherence
- stability (repeated runs)
- long-session memory retention
- session isolation correctness

