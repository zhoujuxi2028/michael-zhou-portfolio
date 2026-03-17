#!/bin/bash

# 数据库设置验证脚本
# 检查所有必要文件是否就绪

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "🔍 验证 AEP Integration 数据库设置..."
echo "项目目录: $PROJECT_DIR"
echo ""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

success_count=0
total_checks=8

check_file() {
    local file_path="$1"
    local description="$2"

    if [[ -f "$file_path" ]]; then
        echo -e "${GREEN}✓${NC} $description"
        ((success_count++))
    else
        echo -e "${RED}✗${NC} $description (文件不存在: $file_path)"
    fi
}

check_executable() {
    local file_path="$1"
    local description="$2"

    if [[ -f "$file_path" && -x "$file_path" ]]; then
        echo -e "${GREEN}✓${NC} $description"
        ((success_count++))
    elif [[ -f "$file_path" ]]; then
        echo -e "${YELLOW}⚠${NC} $description (文件存在但不可执行)"
        chmod +x "$file_path"
        echo -e "${GREEN}✓${NC} 已修复执行权限"
        ((success_count++))
    else
        echo -e "${RED}✗${NC} $description (文件不存在: $file_path)"
    fi
}

echo "📋 检查必要文件..."

# 1. 数据库架构文件
check_file "$PROJECT_DIR/db/schema_v2.sql" "数据库架构文件 v2.0"

# 2. 环境配置文件
check_file "$PROJECT_DIR/.env.database" "环境配置文件"
check_file "$PROJECT_DIR/.env.database.template" "环境配置模板"

# 3. 设置脚本
check_executable "$PROJECT_DIR/scripts/setup_database.sh" "数据库设置脚本"
check_executable "$PROJECT_DIR/scripts/test_database.sh" "数据库测试脚本"

# 4. 文档文件
check_file "$PROJECT_DIR/docs/DATABASE_DESIGN.md" "数据库设计文档"
check_file "$PROJECT_DIR/docs/DATABASE_DEPLOYMENT_GUIDE.md" "数据库部署指南"
check_file "$PROJECT_DIR/docs/DATABASE_GUIDE.md" "数据库使用指南"

echo ""
echo "📊 验证结果："
echo -e "成功: ${GREEN}$success_count${NC}/$total_checks"

if [[ $success_count -eq $total_checks ]]; then
    echo -e "${GREEN}🎉 所有检查通过！数据库设置已就绪。${NC}"
    echo ""
    echo "📌 下一步操作："
    echo "1. 确保 MySQL 已安装并运行"
    echo "2. 编辑 .env.database 文件，设置正确的数据库连接信息"
    echo "3. 运行数据库初始化: ./scripts/setup_database.sh development"
    echo "4. 运行连接测试: ./scripts/test_database.sh"
    echo ""
    echo "📚 参考文档:"
    echo "- 部署指南: docs/DATABASE_DEPLOYMENT_GUIDE.md"
    echo "- 设计文档: docs/DATABASE_DESIGN.md"
    echo "- 使用指南: docs/DATABASE_GUIDE.md"
else
    echo -e "${RED}❌ 存在问题，请检查上述错误。${NC}"
    exit 1
fi