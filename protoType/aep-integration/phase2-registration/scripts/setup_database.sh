#!/bin/bash

# =================================================================
# AEP Integration Database Setup Script
# 数据库初始化脚本
#
# 功能:
# 1. 创建数据库和用户
# 2. 执行数据库架构脚本
# 3. 验证数据库连接
# 4. 初始化基础数据
#
# 使用方法:
# ./scripts/setup_database.sh [production|development|test]
#
# @author AEP Integration Team
# @version 1.0.0
# @since 2026-01-25
# =================================================================

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
# 检测MySQL版本并选择合适的schema
MYSQL_VERSION=$(mysql --version 2>/dev/null | grep -o '[0-9]\+\.[0-9]\+' | head -1)
MYSQL_MAJOR=$(echo "$MYSQL_VERSION" | cut -d. -f1)

if [[ "$MYSQL_MAJOR" -ge 9 ]]; then
    DB_SCHEMA_FILE="$PROJECT_DIR/db/schema_v2_mysql96.sql"
    log_info "检测到MySQL $MYSQL_VERSION，使用MySQL 9.6优化版schema"
else
    DB_SCHEMA_FILE="$PROJECT_DIR/db/schema_v2.sql"
    log_info "检测到MySQL $MYSQL_VERSION，使用标准版schema"
fi

# 环境参数
ENVIRONMENT=${1:-development}

log_info "开始设置AEP Integration数据库环境: $ENVIRONMENT"
log_info "项目目录: $PROJECT_DIR"

# =================================================================
# 1. 检查必要文件
# =================================================================

check_files() {
    log_info "检查必要文件..."

    if [[ ! -f "$DB_SCHEMA_FILE" ]]; then
        log_error "数据库架构文件不存在: $DB_SCHEMA_FILE"
        exit 1
    fi

    # 检查环境配置文件
    ENV_FILE="$PROJECT_DIR/.env.database"
    ENV_TEMPLATE="$PROJECT_DIR/.env.database.template"

    if [[ ! -f "$ENV_FILE" ]]; then
        if [[ -f "$ENV_TEMPLATE" ]]; then
            log_warn "环境配置文件不存在，正在从模板复制..."
            cp "$ENV_TEMPLATE" "$ENV_FILE"
            log_warn "请编辑 $ENV_FILE 文件，填入正确的数据库配置"
            exit 1
        else
            log_error "环境配置文件和模板都不存在"
            exit 1
        fi
    fi

    log_success "文件检查完成"
}

# =================================================================
# 2. 加载环境配置
# =================================================================

load_config() {
    log_info "加载环境配置..."

    # 加载环境变量
    if [[ -f "$PROJECT_DIR/.env.database" ]]; then
        source "$PROJECT_DIR/.env.database"
        log_info "已加载数据库环境变量"
    fi

    # 设置默认值
    DB_HOST=${DB_HOST:-localhost}
    DB_PORT=${DB_PORT:-3306}
    DB_NAME=${DB_NAME:-aep_integration_db}
    DB_USERNAME=${DB_USERNAME:-aep_user}

    if [[ -z "$DB_PASSWORD" ]]; then
        log_error "数据库密码未设置，请在 .env.database 文件中设置 DB_PASSWORD"
        exit 1
    fi

    # 管理员账户（用于创建数据库和用户）
    DB_ADMIN_USER=${DB_ADMIN_USER:-root}

    # 处理空密码的情况
    if [[ -z "$DB_ADMIN_PASSWORD" ]]; then
        log_info "管理员密码为空，使用无密码连接"
        DB_ADMIN_PASSWORD=""
    fi

    log_success "环境配置加载完成"
}

# =================================================================
# 3. 测试数据库连接
# =================================================================

test_connection() {
    log_info "测试数据库连接..."

    # 测试管理员连接
    if [[ -z "$DB_ADMIN_PASSWORD" ]]; then
        MYSQL_CMD="mysql -h $DB_HOST -P $DB_PORT -u $DB_ADMIN_USER"
    else
        MYSQL_CMD="mysql -h $DB_HOST -P $DB_PORT -u $DB_ADMIN_USER -p$DB_ADMIN_PASSWORD"
    fi

    if ! $MYSQL_CMD -e "SELECT 1;" >/dev/null 2>&1; then
        log_error "无法连接到MySQL服务器，请检查连接配置"
        exit 1
    fi

    log_success "数据库连接测试成功"
}

# =================================================================
# 4. 创建数据库和用户
# =================================================================

create_database_and_user() {
    log_info "创建数据库和用户..."

    # 创建数据库（如果不存在）
    $MYSQL_CMD <<EOF
-- 创建数据库
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

-- 创建用户（如果不存在）
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'localhost' IDENTIFIED BY '$DB_PASSWORD';

-- 授予权限
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USERNAME'@'%';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USERNAME'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;

SELECT 'Database and user created successfully' as result;
EOF

    if [[ $? -eq 0 ]]; then
        log_success "数据库和用户创建成功"
    else
        log_error "数据库和用户创建失败"
        exit 1
    fi
}

# =================================================================
# 5. 执行数据库架构脚本
# =================================================================

execute_schema() {
    log_info "执行数据库架构脚本..."

    # 执行架构脚本
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" < "$DB_SCHEMA_FILE"

    if [[ $? -eq 0 ]]; then
        log_success "数据库架构创建成功"
    else
        log_error "数据库架构创建失败"
        exit 1
    fi
}

# =================================================================
# 6. 验证数据库设置
# =================================================================

verify_setup() {
    log_info "验证数据库设置..."

    # 检查表是否创建成功
    TABLE_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" \
        -e "SHOW TABLES;" | wc -l)

    if [[ $TABLE_COUNT -gt 1 ]]; then
        log_success "数据库表创建成功，共 $((TABLE_COUNT - 1)) 个表"
    else
        log_error "数据库表创建失败"
        exit 1
    fi

    # 显示表列表
    log_info "数据库表列表:"
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" \
        -e "SHOW TABLES;" | tail -n +2
}

# =================================================================
# 7. 环境特定配置
# =================================================================

apply_environment_config() {
    log_info "应用 $ENVIRONMENT 环境特定配置..."

    case $ENVIRONMENT in
        production)
            log_info "生产环境配置:"
            log_info "- 启用SSL连接"
            log_info "- 严格的安全配置"
            log_info "- 禁用调试日志"
            ;;
        development)
            log_info "开发环境配置:"
            log_info "- 允许本地连接"
            log_info "- 启用详细日志"
            # 可以在这里插入开发环境特定的SQL
            ;;
        test)
            log_info "测试环境配置:"
            log_info "- 测试数据初始化"
            # 可以在这里插入测试数据
            ;;
        *)
            log_warn "未知环境: $ENVIRONMENT，使用默认配置"
            ;;
    esac
}

# =================================================================
# 8. 创建连接测试脚本
# =================================================================

create_test_script() {
    log_info "创建数据库连接测试脚本..."

    TEST_SCRIPT="$PROJECT_DIR/scripts/test_database.sh"

    cat > "$TEST_SCRIPT" << 'EOF'
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
            (SELECT COUNT(*) FROM aep_products) as products,
            (SELECT COUNT(*) FROM aep_operation_logs) as operation_logs,
            (SELECT COUNT(*) FROM aep_app_configs) as configs;"
else
    echo "✗ 数据库连接失败"
    exit 1
fi
EOF

    chmod +x "$TEST_SCRIPT"
    log_success "测试脚本创建完成: $TEST_SCRIPT"
}

# =================================================================
# 9. 主函数
# =================================================================

main() {
    log_info "========================================"
    log_info "AEP Integration Database Setup"
    log_info "Environment: $ENVIRONMENT"
    log_info "========================================"

    check_files
    load_config
    test_connection
    create_database_and_user
    execute_schema
    verify_setup
    apply_environment_config
    create_test_script

    log_success "========================================"
    log_success "数据库设置完成!"
    log_success "========================================"
    log_info "接下来的步骤:"
    log_info "1. 运行连接测试: ./scripts/test_database.sh"
    log_info "2. 启动应用程序进行测试"
    log_info "3. 查看应用日志确认数据库功能正常"
}

# =================================================================
# 执行主函数
# =================================================================

main "$@"