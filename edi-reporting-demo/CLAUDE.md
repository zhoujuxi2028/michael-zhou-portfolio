# CLAUDE.md - EDI & Reporting Demo

This file provides guidance to Claude Code when working within the `edi-reporting-demo/` project.

## 项目概述

物流/供应链领域的数据集成与报表技能 Demo，包含两个独立子模块：

1. **edi/** - EDIFACT 报文解析器（分层架构：parser → schema → validator）
2. **jasper/** - Jasper 报表模板（`.jrxml`）+ SQLite 数据源

## 目录结构

```
edi-reporting-demo/
├── edi/
│   ├── parser.py        # 通用解析层（纯函数，无业务语义）
│   ├── schema.py        # Schema 定义层（ORDERS/DESADV/INVOIC）
│   ├── validator.py     # 校验层（schema 驱动 + 错误分级）
│   ├── cli.py           # 命令行入口
│   ├── fixtures/        # 黄金样本 + 坏数据样本
│   └── tests/           # pytest 单元 + 契约测试
└── jasper/
    ├── build_orders_db.py
    └── orders_summary.jrxml
```

## 开发命令

```bash
# EDI 解析
cd edi-reporting-demo/edi
python3 cli.py fixtures/sample_orders.edi

# 运行测试
cd edi-reporting-demo/edi
python3 -m pytest tests/ -v

# 报表数据生成
cd edi-reporting-demo/jasper
python3 build_orders_db.py
```

## 架构约定（parser → schema → validator）

- **parser.py**: 只做字符流 → 数据结构的机械转换，禁止加入业务语义
- **schema.py**: 报文规格用声明式配置，新增报文类型（DESADV/INVOIC）只加配置 + 注册到 `SCHEMA_REGISTRY`
- **validator.py**: 校验逻辑通用，通过 schema 驱动；错误分级用 `Severity.ERROR` / `Severity.WARNING`
- **契约测试**: 用 `fixtures/*.edi` 黄金样本锁定解析结果，扩展报文时确保旧契约不破坏

## 约定

- EDI 样例报文中的参与方使用匿名化名称（`SUPPLIER01`、`BUYER01`），遵循仓库信息安全规范
- Python 使用标准库（`sqlite3`、`sys`、`dataclasses`），无第三方依赖
- 测试用 pytest（开发依赖，运行 `python3 -m pytest`）
- `orders.db` 是生成产物，不入库（见 `.gitignore`）
- 文档中文优先
