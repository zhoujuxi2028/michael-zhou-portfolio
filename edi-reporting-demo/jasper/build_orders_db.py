#!/usr/bin/env python3
"""生成 SQLite 订单数据，供 Jasper Reports 报表 Demo 使用。
Jaspersoft Studio 通过 JDBC 连接本 .db 文件渲染订单汇总报表。

用法:
    python build_orders_db.py   # 生成 orders.db
"""
import sqlite3
from datetime import date, timedelta

DB = "orders.db"

SCHEMA = """
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    order_id   TEXT NOT NULL,
    customer   TEXT NOT NULL,
    warehouse  TEXT NOT NULL,
    order_date TEXT NOT NULL,
    sku        TEXT NOT NULL,
    qty        INTEGER NOT NULL,
    unit_price REAL NOT NULL
);
"""

ROWS = [
    ("PO-2026-0001", "ACME Corp", "WH-SH-01", "2026-11-01", "SKU1001", 100, 12.50),
    ("PO-2026-0001", "ACME Corp", "WH-SH-01", "2026-11-01", "SKU1002", 50, 8.00),
    ("PO-2026-0002", "Globex",   "WH-BJ-02", "2026-11-02", "SKU2001", 30, 45.00),
    ("PO-2026-0002", "Globex",   "WH-BJ-02", "2026-11-02", "SKU1001", 20, 12.50),
    ("PO-2026-0003", "Initech",  "WH-SH-01", "2026-11-03", "SKU3001", 5, 300.00),
    ("PO-2026-0004", "ACME Corp", "WH-GZ-03", "2026-11-04", "SKU1002", 75, 8.00),
]


def main():
    conn = sqlite3.connect(DB)
    cur = conn.cursor()
    cur.executescript(SCHEMA)
    cur.executemany(
        "INSERT INTO orders VALUES (?, ?, ?, ?, ?, ?, ?)", ROWS
    )
    conn.commit()

    print("=== 订单数据已写入 orders.db ===\n")
    for row in cur.execute(
        "SELECT customer, warehouse, COUNT(*) AS lines, SUM(qty) AS total_qty, "
        "ROUND(SUM(qty * unit_price), 2) AS amount "
        "FROM orders GROUP BY customer, warehouse ORDER BY customer"
    ):
        print(f"{row[0]:<12} {row[1]:<10} 行数={row[2]} 总数量={row[3]} 金额={row[4]}")

    conn.close()
    print("\n下一步: 用 Jaspersoft Studio 连接 orders.db (JDBC sqlite) 打开 orders_summary.jrxml")


if __name__ == "__main__":
    main()
