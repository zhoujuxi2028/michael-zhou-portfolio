# 项目文档目录

本目录包含 Cloud Monitoring Platform Phase 1 的所有设计和规划文档，按照 `protoType/CLAUDE.md` 中定义的文档管理规范进行组织。

## 📁 目录结构

```
docs/
├── README.md                 # 本文档目录说明
├── requirements/             # 需求文档
│   ├── requirements.md       # 总体需求文档
│   ├── requirements-analysis-v1.0.md        # 需求分析文档
│   └── requirements-phase1-device-query-v1.0.md  # Phase 1 需求规格说明书
├── architecture/             # 架构设计文档
│   ├── architecture-design-phase1-v1.0.md   # 程序架构设计文档
│   └── database-design-phase1-v1.0.sql      # 数据库设计脚本
├── research/                 # 技术调研文档 (待补充)
└── prototypes/              # 原型设计文档 (待补充)
```

## 📋 文档分类说明

### 需求文档 (requirements/)
包含项目的业务需求分析、功能规格说明等文档：
- **总体需求**: 项目整体需求和目标
- **需求分析**: 详细的业务需求分析
- **Phase 1 需求**: 第一阶段具体功能需求规格

### 架构设计文档 (architecture/)
包含技术架构、系统设计等文档：
- **程序架构**: 技术架构、模块设计、API设计等
- **数据库设计**: 数据模型、表结构、初始化脚本等

### 技术调研文档 (research/)
用于存放技术方案调研、技术选型分析等文档：
- 命名规范: `research-[主题]-YYYY-MM-DD.md`
- 例如: `research-vue3-migration-analysis-2024-12-09.md`

### 原型设计文档 (prototypes/)
用于存放原型设计、概念验证等文档：
- 命名规范: `prototype-[功能名]-YYYY-MM-DD.md`
- 例如: `prototype-device-query-ui-2024-12-09.md`

## 📝 文档命名规范

根据 `protoType/CLAUDE.md` 中的规范：

| 文档类型 | 命名规范 | 示例 |
|---------|---------|------|
| 需求文档 | `requirements-[模块名]-v[版本号].md` | `requirements-device-management-v1.0.md` |
| 设计文档 | `design-[功能名]-YYYY-MM-DD.md` | `design-user-authentication-2024-12-09.md` |
| 调研文档 | `research-[主题]-YYYY-MM-DD.md` | `research-frontend-framework-2024-12-09.md` |
| 会议记录 | `meeting-YYYY-MM-DD-[主题].md` | `meeting-2024-12-09-architecture-review.md` |

## 📄 文档模板

每个文档应包含以下基本信息：

```markdown
# [文档标题]

## 文档信息
- **版本**: v1.0
- **创建日期**: YYYY-MM-DD
- **更新日期**: YYYY-MM-DD
- **负责人**: [姓名]
- **状态**: [草稿/评审中/已批准/已废弃]

## 概述
[简要说明文档目的和范围]

## 详细内容
[具体内容]

## 相关链接
- 主项目文档: ../CLAUDE.md
- 相关需求: [链接]
- 参考资料: [链接]
```

## 🔄 版本控制

- 重要文档变更需要创建 git commit
- 使用语义化的 commit 信息:
  - `docs: 添加[功能]需求分析`
  - `design: 更新[模块]架构设计`
  - `research: 完成[技术]调研报告`

## 📚 相关资源

- **项目指导文档**: `../CLAUDE.md`
- **主项目文档**: `../../CLAUDE.md`
- **Phase 1 开发项目**: `../../phase1-project/`
- **开发环境配置**: `../../dev-env/`

---

**说明**: 本文档结构遵循 `protoType/CLAUDE.md` 中定义的文档管理规范，确保项目文档的标准化和可维护性。