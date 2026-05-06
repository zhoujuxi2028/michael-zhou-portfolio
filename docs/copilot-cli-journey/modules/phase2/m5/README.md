# M5 学习资料导航

这里集中收纳 **M5：文档和注释生成工作流** 的补充学习材料，解决原先专题文件散落在根目录、入口不统一的问题。

---

## 目录结构

```text
modules/phase2/m5/
├── README.md
├── summaries/
│   ├── completion-summary.md
│   └── deep-dive.md
├── tech-stacks/
│   ├── stack-comparison.md
│   ├── fastapi.md
│   ├── grpc.md
│   └── graphql.md
├── prompts/
│   └── python-google-docstring-prompts.md
└── examples/
    └── python-docstring-example.py
```

---

## 推荐阅读顺序

1. 先读 [M5 模块页](../M5-doc-generation.md)
2. 再看 [完成总结](./summaries/completion-summary.md)
3. 然后看 [深化学习总结](./summaries/deep-dive.md)
4. 最后按需进入 [技术栈对比](./tech-stacks/stack-comparison.md) 与各专题页

---

## 按需求找资料

| 你现在想做什么 | 建议入口 |
|----------------|----------|
| 快速了解 M5 学什么 | [M5 模块页](../M5-doc-generation.md) |
| 看 M5 已完成了哪些成果 | [completion-summary.md](./summaries/completion-summary.md) |
| 看 3 个技术栈的整体对比 | [stack-comparison.md](./tech-stacks/stack-comparison.md) |
| 深入学 FastAPI 文档生成 | [fastapi.md](./tech-stacks/fastapi.md) |
| 深入学 gRPC / Protobuf 文档生成 | [grpc.md](./tech-stacks/grpc.md) |
| 深入学 GraphQL 文档生成 | [graphql.md](./tech-stacks/graphql.md) |
| 直接复用 Python Docstring Prompt | [python-google-docstring-prompts.md](./prompts/python-google-docstring-prompts.md) |
| 看 Python Docstring 示例代码 | [python-docstring-example.py](./examples/python-docstring-example.py) |

---

## 一句话说明

- `summaries/`：复盘和总结
- `tech-stacks/`：按技术栈展开的专题学习
- `prompts/`：可直接复用的 Prompt 模板
- `examples/`：配套示例代码

---

*最后更新：2026-05-06*
