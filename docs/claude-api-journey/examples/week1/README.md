# Week 1 示例：带 System Prompt 的多轮对话 demo

> 对应学习计划 Day 7 小项目。运行前需设置 `ANTHROPIC_API_KEY`。

## 运行

```bash
export ANTHROPIC_API_KEY=sk-ant-...
python3 chat_demo.py
```

## 用法

- 直接输入文本与 Claude 对话（保留多轮上下文）
- `/clear`：清空当前对话历史
- `/exit` 或 Ctrl-C：退出

## 要点

- `system` 定义固定角色，不随对话变化
- `messages` 按 `user`/`assistant` 交替追加，实现多轮记忆
- 使用 `client.messages.stream` 流式输出，打字机体验
