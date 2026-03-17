#!/bin/bash
# 便捷测试脚本入口点
# 调用 scripts/test/run_tests_enhanced.sh
# MOCK-001-A: 集成测试模式控制

set -e

# MOCK-001-A: 设置生产环境模式，禁用Mock测试
export AEP_TEST_MODE=real

# Java版本检查函数
check_java_version() {
    echo "🔍 检查Java版本..."

    if ! command -v java &> /dev/null; then
        echo -e "\033[0;31m❌ 错误: Java未安装或未配置环境变量\033[0m"
        echo -e "\033[1;33m📋 请安装OpenJDK 25并配置JAVA_HOME\033[0m"
        exit 1
    fi

    # 获取Java版本
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)

    echo -e "\033[0;34m📋 检测到Java版本: $JAVA_VERSION\033[0m"

    # 检查是否为Java 21 LTS（推荐）
    if [[ "$JAVA_VERSION" == "21" ]]; then
        echo -e "\033[0;32m✅ Java版本验证通过: OpenJDK $JAVA_VERSION LTS\033[0m"
    elif [[ "$JAVA_VERSION" -ge "17" ]]; then
        echo -e "\033[0;32m✅ 检测到Java $JAVA_VERSION，与项目兼容\033[0m"
        echo -e "\033[1;33m💡 推荐使用OpenJDK 21 LTS获得最佳稳定性\033[0m"
    else
        echo -e "\033[1;33m⚠️  警告: 检测到Java $JAVA_VERSION，可能存在兼容性问题\033[0m"
        echo -e "\033[1;33m📋 项目需要Java 17+，推荐使用OpenJDK 21 LTS\033[0m"
    fi

    echo -e "\033[0;32m✅ 测试将继续执行\033[0m"

    echo
}

# 环境变量检查和加载
check_and_load_env() {
    if [[ ! -f ".env" ]]; then
        echo -e "\033[0;31m[ERROR]\033[0m .env文件不存在!"
        echo -e "\033[1;33m[WARN]\033[0m  请创建.env文件并设置以下环境变量:"
        echo "  AEP_APP_KEY=your_app_key"
        echo "  AEP_APP_SECRET=your_app_secret"
        echo "  AEP_API_HOST=your_api_host"
        echo "  AEP_APP_ID=your_app_id"
        exit 1
    fi

    # 加载环境变量
    source .env

    # 验证关键环境变量
    if [[ -z "$AEP_APP_KEY" || -z "$AEP_APP_SECRET" || -z "$AEP_API_HOST" || -z "$AEP_APP_ID" ]]; then
        echo -e "\033[0;31m[ERROR]\033[0m .env文件中缺少必需的环境变量!"
        echo "请确保.env文件包含: AEP_APP_KEY, AEP_APP_SECRET, AEP_API_HOST, AEP_APP_ID"
        exit 1
    fi

    echo -e "\033[0;32m[SUCCESS]\033[0m 环境变量加载成功"
}

# 执行Java版本检查
check_java_version

# 加载环境变量
check_and_load_env

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/scripts/test/run_tests_enhanced.sh" "$@"
