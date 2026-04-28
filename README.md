## 多行程评测

请先启动后端服务，然后执行以下命令：

```bash
python scripts/evaluate_multitrip.py --base-url http://127.0.0.1:8080 --user-id demo-user
```

低成本快速评测（推荐日常开发时使用）：

```bash
python scripts/evaluate_multitrip.py --base-url http://127.0.0.1:8080 --user-id demo-user --quick --repeats 1 --case-limit 1
```

参数说明：

- --quick：跳过长会话记忆和会话隔离测试
- --case-limit N：仅跑前 N 个测试用例（0 表示全部）
- --repeats N：每个用例重复次数（用于稳定性评估）

## 工具模式检查

启动后可访问：

```bash
curl http://127.0.0.1:8080/debug/tools
```

返回内容会显示：

- 当前工具模式（mcp / local / hybrid）
- MCP 开关是否打开
- MCP SSE 地址是否已配置
- 本地 POI Tool 是否仍然被装配
- MCP ToolProvider 是否已装配

输出结果：

- JSON 报告：reports/multitrip_eval_report.json
- Markdown 报告：reports/multitrip_eval_report.md

评测指标包括：

- 多行程覆盖率（segment coverage）
- 约束遵守率（constraint adherence）
- 重复稳定性（stability，重复运行一致性）
- 长会话记忆保留（long-session memory retention）
- 会话隔离正确性（session isolation correctness）

