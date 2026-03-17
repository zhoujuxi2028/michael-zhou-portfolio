#!/bin/bash

# 数据库连接测试脚本
# 自动生成

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 加载环境变量
if [[ -f "$PROJECT_DIR/.env.database" ]]; then
    source "$PROJECT_DIR/.env.database"
fi

# 设置默认值
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-3306}
DB_NAME=${DB_NAME:-aep_integration}
DB_USERNAME=${DB_USERNAME:-aep_user}

echo "测试数据库连接..."
echo "主机: $DB_HOST:$DB_PORT"
echo "数据库: $DB_NAME"
echo "用户: $DB_USERNAME"

# 测试连接
if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" \
    -e "SELECT 'Connection successful' as status, NOW() as timestamp;"; then
    echo "✓ 数据库连接成功"

    # 显示表统计
    echo ""
    echo "数据库统计:"
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" \
        -e "SELECT
            (SELECT COUNT(*) FROM t_aep_products) as products,
            (SELECT COUNT(*) FROM t_aep_platforms) as platforms,
            (SELECT COUNT(*) FROM t_aep_operations) as operations;"
else
    echo "✗ 数据库连接失败"
    exit 1
fi
