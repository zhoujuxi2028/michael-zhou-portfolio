# 技术栈深度学习

**位置**: Phase 2 — M5 文档生成工作流的进阶内容  
**涵盖**: FastAPI, gRPC, GraphQL 三大主流框架  
**学习时间**: 3-5 小时  
**完成日期**: 2026-04-15

---

## 📚 技术栈对比

| 框架 | 最佳用途 | 文档自动化方式 | 学习曲线 | 推荐指数 |
|------|---------|------------------|---------|---------|
| **FastAPI** | Web API、快速开发 | 类型注解 → OpenAPI 3.0 自动生成 | 🟢 低 | ⭐⭐⭐⭐⭐ |
| **gRPC** | 微服务、高性能通信 | Protocol Buffers → 代码自动生成 | 🟡 中 | ⭐⭐⭐⭐ |
| **GraphQL** | 灵活 API、前后端协作 | Schema → 自动内省文档 | 🟡 中 | ⭐⭐⭐⭐ |

详见 [完整对比分析](./COMPARISON.md)

---

## 🎯 三大框架深度学习

### 1️⃣ [FastAPI — 代码即文档](./TECH1-FastAPI.md)

**核心概念**:
- Pydantic 模型的自文档化
- 类型注解驱动的 OpenAPI 3.0 生成
- Swagger UI + ReDoc 自动渲染

**学习成果**:
- ✅ 5 个 REST API endpoints (GET/POST/PUT/DELETE)
- ✅ 3 个 Pydantic 数据模型
- ✅ 完整的服务器、客户端、使用示例
- ✅ 无需手写一行 API 文档

**适用场景**: 
- Web 应用后端
- 公开 API 服务
- 需要快速迭代的项目

**阅读时间**: 30-45 分钟

---

### 2️⃣ [gRPC — 高性能多语言通信](./TECH2-gRPC.md)

**核心概念**:
- Protocol Buffers 二进制序列化
- 服务定义自动代码生成
- 流式传输（unary, server stream, client stream, bidirectional）
- 多语言互操作性

**学习成果**:
- ✅ 完整的 `.proto` 文件定义
- ✅ 6 个 RPC 方法（包含流式）
- ✅ Python 服务器和客户端实现
- ✅ 高级功能：分页、搜索、流式处理

**适用场景**:
- 微服务架构
- 高性能 RPC 调用
- 多语言系统集成

**阅读时间**: 60-90 分钟

---

### 3️⃣ [GraphQL — 灵活的数据查询语言](./TECH3-GraphQL.md)

**核心概念**:
- GraphQL Schema 定义（Query、Mutation、Subscription）
- 内省机制自动生成交互式文档
- 类型系统和输入验证
- 实时数据推送

**学习成果**:
- ✅ 完整的 GraphQL Schema（使用 Strawberry）
- ✅ Query：列表、搜索、单个查询（支持分页、筛选、排序）
- ✅ Mutation：创建、更新、删除
- ✅ Subscription：实时推送
- ✅ FastAPI + GraphQL 服务器集成

**适用场景**:
- 前后端 API 协作
- 复杂数据查询需求
- 移动应用后端

**阅读时间**: 90-120 分钟

---

## 🔗 快速导航

- [回到 Phase 2](../README.md)
- [完整对比分析](./COMPARISON.md)
- [所有文档](../../README.md)

---

## 💡 学习建议

1. **按顺序学习**
   - FastAPI (最简单，打基础)
   - gRPC (进阶，理解二进制协议)
   - GraphQL (灵活，理解 Schema 驱动)

2. **边学边练**
   - 每个框架都有完整的可运行代码示例
   - 建议在本地环境尝试运行

3. **对比理解**
   - 完成所有三个框架后，看 [对比分析](./COMPARISON.md)
   - 理解何时选择哪个框架

---

**最后更新**: 2026-04-15  
**总学习时间**: 3-5 小时  
**推荐完成时间**: 1-2 周
