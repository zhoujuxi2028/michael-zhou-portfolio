# EDI & Reporting Demo

物流/供应链领域的数据集成（EDI）与报表（Reporting）技能 Demo，用于展示 EDI 报文解析与 Jasper 报表两个核心能力。

## 项目组成

```
edi-reporting-demo/
├── edi/                          # EDI 报文解析（分层架构）
│   ├── parser.py                 # 通用解析层（纯函数，无业务语义）
│   ├── schema.py                 # Schema 定义层（ORDERS/DESADV/INVOIC 配置驱动）
│   ├── validator.py              # 校验层（schema 驱动 + 错误分级）
│   ├── cli.py                    # 命令行入口
│   ├── fixtures/                 # 黄金样本与坏数据样本
│   │   ├── sample_orders.edi
│   │   ├── sample_orders_missing_bgm.edi
│   │   └── sample_orders_unknown_segment.edi
│   └── tests/                    # pytest 单元 + 契约测试
│       ├── test_parser.py
│       ├── test_validator.py
│       └── test_contract_orders.py
└── jasper/                       # Jasper 报表
    ├── build_orders_db.py        # 生成 SQLite 订单数据
    └── orders_summary.jrxml      # Jasper 报表模板（XML + SQL 分组聚合）
```

## Tech Stack

- **EDI 解析**: Python 3（标准库，无第三方依赖）
- **测试**: pytest
- **报表**: Jasper Reports（`.jrxml`）+ SQLite

## 快速开始

### 1. EDI 报文解析

```bash
cd edi
python3 cli.py                          # 解析 fixtures/sample_orders.edi
python3 cli.py fixtures/sample_orders.edi --type ORDERS
```

**分层架构**（parser → schema → validator）：

| 层 | 职责 | 可复用性 |
|----|------|---------|
| `parser.py` | 纯文本 → 结构化（切段/拆元素/拆成分） | 100% 通用，不重写 |
| `schema.py` | 报文规格配置（段定义/必填性/限定符） | 新增报文只加配置 |
| `validator.py` | schema 驱动校验 + 错误分级 | 通用引擎，无业务耦合 |

**能讲出的知识点**：
- EDI = 纯文本 + 分隔符体系（`+` 元素 / `:` 成分 / `'` 段终止）
- 物流核心链路：`ORDERS`(订单) → `DESADV`(发货通知/ASN) → 收货确认
- ANSI X12 同概念，分隔符换成 `*` / `~`，报文号 850/856/810
- 新增报文类型 = 改配置 + 补测试，不动核心代码（开放-封闭原则）

### 2. 运行测试

```bash
cd edi
python3 -m pytest tests/ -v            # 23 个测试
```

### 3. Jasper 报表

```bash
cd jasper
python3 build_orders_db.py           # 生成 orders.db（SQLite 订单数据）
```

然后：
1. 下载 [Jaspersoft Studio](https://community.jaspersoft.com/project/jaspersoft-studio)（社区版免费）
2. 打开 `orders_summary.jrxml`
3. 数据源连 `orders.db`（JDBC + sqlite-jdbc 驱动）
4. Preview → 导出 PDF

**能讲出的知识点**：
- 报表三层结构：`数据源连接 → SQL 查询 → 版式模板`
- 报表三要素：参数（过滤）、分组（GROUP BY）、聚合（SUM/COUNT）
- `.jrxml` / `.rdl` 都是 XML 定义，可对接已有 XML 背景

## 运行要求

- Python 3.10+
- Jaspersoft Studio（仅报表渲染需要，可选）
