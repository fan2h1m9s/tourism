## 多行程评测

请先启动后端服务，然后执行以下命令：

```bash
python scripts/evaluate_multitrip.py --base-url http://127.0.0.1:8080 --user-id demo-user
```

输出结果：

- JSON 报告：reports/multitrip_eval_report.json
- Markdown 报告：reports/multitrip_eval_report.md

评测指标包括：

- 多行程覆盖率（segment coverage）
- 约束遵守率（constraint adherence）
- 重复稳定性（stability，重复运行一致性）
- 长会话记忆保留（long-session memory retention）
- 会话隔离正确性（session isolation correctness）

