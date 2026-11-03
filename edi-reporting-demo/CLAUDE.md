# CLAUDE.md - EDI & Reporting Demo

This file provides guidance to Claude Code when working within the `edi-reporting-demo/` project.

## 项目概述

物流/供应链领域的数据集成与报表技能 Demo，包含两个独立子模块：

1. **edi/** - EDIFACT 报文解析器（纯 Python，无第三方依赖）
2. **jasper/** - Jasper 报表模板（`.jrxml`）+ SQLite 数据源

## 目录结构

```
edi-reporting-demo/
├── edi/
│   ├── edifact_parser.py     # EDIFACT 解析器
│   └── sample_orders.edi     # ORDERS 样例报文
└── jasper/
    ├── build_orders_db.py    # 生成 SQLite 订单数据
    └── orders_summary.jrxml  # Jasper 报表模板
```

## 开发命令

```bash
# EDI 解析
cd edi-reporting-demo/edi
python3 edifact_parser.py

# 报表数据生成
cd edi-reporting-demo/jasper
python3 build_orders_db.py
```

## 约定

- EDI 样例报文中的参与方使用匿名化名称（`SUPPLIER01`、`BUYER01`），遵循仓库信息安全规范
- Python 使用标准库（`sqlite3`、`sys`），无第三方依赖，无需虚拟环境
- `orders.db` 是生成产物，不入库（见 `.gitignore`）
- 文档中文优先
