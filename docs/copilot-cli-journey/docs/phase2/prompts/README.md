# Prompt 模板库

**位置**: Phase 2 — M5 文档生成工作流的 Prompt 实战  
**当前覆盖**: Python Google Docstring  
**模板数量**: 6 个  
**状态**: ✅ M5 完成

---

## 📋 可复用 Prompt 模板

### Python Google Docstring Prompts (6 个)

位置: [`python-google-docstring.md`](./python-google-docstring.md)

| # | 方法 | 功能 | 难度 |
|---|------|------|------|
| 1 | `saml_login()` | SAML 2.0 SSO 认证 | 🟡 中 |
| 2 | `oidc_login()` | OAuth 2.0 + OIDC 认证 | 🟡 中 |
| 3 | `ldap_search()` | LDAP 目录搜索 | 🟠 高 |
| 4 | `evaluate_device()` | Zero Trust 设备评估 | 🟠 高 |
| 5 | `create_session()` | 用户会话管理 | 🟢 低 |
| 6 | `mfa_verify()` | 多因素认证验证 | 🟡 中 |

**每个 Prompt 包含**:
- ✅ 完整的函数代码
- ✅ 详细的 Copilot CLI 调用命令
- ✅ 生成的 Google 风格 Docstring
- ✅ 最佳实践注解

---

## 🚀 已计划的 Prompt 库

| Module | 类型 | 预计模板数 | 状态 |
|--------|------|----------|------|
| **M5** | Python Docstring | 6 | ✅ 完成 |
| **M4** | Jest 测试生成 | 5 | 🔄 进行中 |
| **M4** | Pytest 测试生成 | 5 | 🔄 进行中 |
| **M6** | 代码审查 | 5+ | ⏳ 筹划中 |
| **M6** | PR 描述生成 | 3+ | ⏳ 筹划中 |

---

## 💡 使用指南

### 快速使用
1. 选择对应的 Prompt 文件
2. 复制 Prompt 模板
3. 根据你的代码调整参数
4. 在 Copilot CLI 中运行

### 示例
```bash
# 使用 M5 Python Docstring Prompt
copilot /code "你的代码" < prompt.md

# 或在 Copilot 对话中粘贴 Prompt
```

### 扩展
- 可根据项目特点定制新的 Prompt
- 建议维护项目内的 Prompt 库文件

---

## 📚 相关文档

- [M5 完成总结](../M5-COMPLETION-SUMMARY.md) — 学习成果总览
- [Python Docstring Prompts](./python-google-docstring.md) — 完整 Prompt 库
- [回到 Phase 2](../README.md)

---

**最后更新**: 2026-04-15  
**维护者**: Michael Zhou  
**版本**: 1.0
