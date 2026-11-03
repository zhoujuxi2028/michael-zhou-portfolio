# EDI & Reporting Demo

> Language: English | [中文](./README.zh.md)

A skills demo for data integration (EDI) and reporting in the logistics / supply-chain domain, showcasing EDI message parsing and Jasper report generation.

## Project Layout

```
edi-reporting-demo/
├── edi/                          # EDI message parsing (layered architecture)
│   ├── parser.py                 # Generic parsing layer (pure functions, no business logic)
│   ├── schema.py                 # Schema definition layer (ORDERS/DESADV/INVOIC, config-driven)
│   ├── validator.py              # Validation layer (schema-driven + error severity)
│   ├── cli.py                    # Command-line entry point
│   ├── fixtures/                 # Golden samples & malformed-data samples
│   │   ├── sample_orders.edi
│   │   ├── sample_orders_missing_bgm.edi
│   │   └── sample_orders_unknown_segment.edi
│   └── tests/                    # pytest unit + contract tests
│       ├── test_parser.py
│       ├── test_validator.py
│       └── test_contract_orders.py
└── jasper/                       # Jasper reports
    ├── build_orders_db.py        # Generate SQLite order data
    └── orders_summary.jrxml      # Jasper report template (XML + SQL grouping)
```

## Tech Stack

- **EDI parsing**: Python 3 (standard library, no third-party deps)
- **Testing**: pytest
- **Reporting**: Jasper Reports (`.jrxml`) + SQLite

## Quick Start

### 1. EDI Message Parsing

```bash
cd edi
python3 cli.py                          # Parse fixtures/sample_orders.edi
python3 cli.py fixtures/sample_orders.edi --type ORDERS
```

**Layered architecture** (parser → schema → validator):

| Layer | Responsibility | Reusability |
|-------|----------------|-------------|
| `parser.py` | Raw text → structure (segment/element/component split) | 100% generic, never rewritten |
| `schema.py` | Message spec config (segments/required/qualifiers) | New message = config only |
| `validator.py` | Schema-driven validation + severity levels | Generic engine, no business coupling |

**Key takeaways**:
- EDI = plain text + delimiter system (`+` element / `:` component / `'` segment terminator)
- Logistics core flow: `ORDERS` → `DESADV` (ASN) → goods receipt confirmation
- ANSI X12 shares the concept, delimiters switch to `*` / `~`, message numbers 850/856/810
- Adding a message type = config + tests, no core-code changes (Open-Closed Principle)

### 2. Run Tests

```bash
cd edi
python3 -m pytest tests/ -v            # 23 tests
```

### 3. Jasper Reports

```bash
cd jasper
python3 build_orders_db.py           # Generate orders.db (SQLite order data)
```

Then:
1. Download [Jaspersoft Studio](https://community.jaspersoft.com/project/jaspersoft-studio) (free community edition)
2. Open `orders_summary.jrxml`
3. Connect the data source to `orders.db` (JDBC + sqlite-jdbc driver)
4. Preview → export PDF

**Key takeaways**:
- Report three-layer structure: `data source → SQL query → layout template`
- Report three essentials: parameters (filter), grouping (GROUP BY), aggregation (SUM/COUNT)
- `.jrxml` / `.rdl` are both XML definitions, portable from existing XML background

## Requirements

- Python 3.10+
- Jaspersoft Studio (optional, only needed for report rendering)
