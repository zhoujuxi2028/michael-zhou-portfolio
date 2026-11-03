# Week 1 笔记：API 基础 + Prompt 工程

状态：🟡 进行中

---

## Day 1 · Introduction

**关键概念**：
- Claude 是 Anthropic 的大语言模型，API 提供 text 对话接口
- 主流模型：Claude Opus（最强）、Claude Sonnet（平衡）、Claude Haiku（最快/最便宜）
- API 交互本质：messages API，发送 `system` + `messages` 数组，返回 completion
- 上下文窗口内模型只"记得"你发送的全部历史内容

**代码片段**：
- 用 `client.messages.create()` 发送单轮请求

**问题与疑惑**：
- （学习后填写）

---

## Day 2 · API Key + 第一个请求 + 多轮对话

**关键概念**：
- API Key 在 [console.anthropic.com](https://console.anthropic.com) 申请，通过 `ANTHROPIC_API_KEY` 环境变量或请求头 `x-api-key` 传入
- 官方 Python SDK：`pip install anthropic`
- 最小请求：`model` + `max_tokens` + `messages`（必填）
- 多轮对话 = 把 `user`/`assistant` 消息按顺序追加进 `messages` 数组；`user` 消息内可带 `content` 为文本或 blocks
- 不传 `system` 时默认无系统指令

**代码片段**：
```python
from anthropic import Anthropic

client = Anthropic()
resp = client.messages.create(
    model="claude-sonnet-4-5",
    max_tokens=1024,
    messages=[
        {"role": "user", "content": "你好"},
        {"role": "assistant", "content": "你好！有什么可以帮你？"},
        {"role": "user", "content": "解释一下多轮对话"},
    ],
)
print(resp.content[0].text)
```

**问题与疑惑**：
- （学习后填写）

---

## Day 3 · System Prompt + Temperature + Streaming

**关键概念**：
- **System Prompt**：最高优先级指令，设定角色/风格/约束，独立于 `messages`
- **Temperature**：0–1，控制随机性。0 = 确定性/事实性任务；1 = 创意任务。注意：temperature 不适用于 function calling 场景的推理
- **Streaming**：`stream=True` 时返回事件流，`content_block_delta` 携带增量文本，适合打字机效果与低延迟 UX
- 响应还包含 `stop_reason`（end_turn / max_tokens / tool_use）与 `usage`（token 统计）

**代码片段**：
```python
# streaming
with client.messages.stream(
    model="claude-sonnet-4-5",
    max_tokens=1024,
    system="你是一位资深的 Python 导师，回答简洁且给示例",
    messages=[{"role": "user", "content": "解释 Python 装饰器"}],
) as stream:
    for text in stream.text_stream:
        print(text, end="", flush=True)
```

**问题与疑惑**：
- （学习后填写）

---

## Day 4 · Structured Data + Quiz

**关键概念**：
- 结构化输出：要求 Claude 返回 JSON，配合强约束 prompt 效果最好
- 最佳实践：system prompt 说明 schema + 要求只输出 JSON；`content` 可含 JSON block
- 解析：`json.loads()` 处理；可用 Pydantic/函数校验
- 进阶：可用 Tool Use / 预填 assistant 前缀提高格式稳定性（Week 2 覆盖）

**代码片段**：
```python
resp = client.messages.create(
    model="claude-sonnet-4-5",
    max_tokens=1024,
    system=(
        "你是一个信息抽取器。只输出 JSON，不要输出其他文字。\n"
        '格式：{"name": str, "age": int, "skills": [str]}'
    ),
    messages=[{"role": "user", "content": "张三 30 岁，会 Python 和 Java。"}],
)
import json
data = json.loads(resp.content[0].text)
print(data)
```

**问题与疑惑**：
- （学习后填写）

---

## Day 5 · Prompt 工程：清晰 + 具体 + XML 标签

**关键概念**：
- **清晰**：使用完整句子、明确的祈使句；避免模糊词汇
- **具体**：给出上下文、格式要求、长度限制、角色设定
- **XML 标签**：用 `<context>...</context>`、`<input>...</input>` 等标签结构化输入，让模型区分指令与数据；Claude 对 XML 标签敏感
- 常见误区：prompt 过长稀释重点，指令冲突，未限定输出格式

**代码片段**：
```xml
<task>
阅读下面的客户邮件，提取：1) 客户身份 2) 问题类型 3) 期望的解决时间。
</task>
<input>
我上周下单的鼠标一直没发货，订单号 888888，请今天内给答复。
</input>
<output_format>
JSON: {"customer_id": ..., "issue_type": ..., "expected_resolution": ...}
</output_format>
```

**问题与疑惑**：
- （学习后填写）

---

## Day 6 · Providing Examples + Quiz

**关键概念**：
- **Few-shot / Examples**：在 prompt 中给 2–5 个输入输出对，示范期望格式与风格，比抽象指令更有效
- Examples 放在 prompt 中比放在对话历史更稳定；一致性很关键
- 进阶：将 examples 组装为 Few-shot 模板函数，便于复用与参数化

**代码片段**：
```python
system = """你根据用户描述的情绪分类，只输出类别名。
示例：
描述: 今天项目上线成功了！ -> 开心
描述: 系统又崩溃了，客户一直在投诉。 -> 焦虑
描述: {user_input} ->"""
```

**问题与疑惑**：
- （学习后填写）

---

## Day 7 · 周末小项目

**项目**：带 System Prompt 的多轮对话 demo

**实现思路**：
- 状态：保存历史消息到内存列表
- 结构：`system` 固定角色设定 + 循环收集用户输入 + 追加到 `messages` + 打印响应
- 扩展：可加 `exit` 命令、请求日志、简单指令前缀（如 `/clear`）

**遇到的问题**：
- （完成后填写）

**代码位置**：`../../examples/week1/`

**收获**：
- （完成后填写）
