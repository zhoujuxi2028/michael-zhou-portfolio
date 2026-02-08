#!/bin/bash
# 便捷查询脚本入口点
# 调用 scripts/query/run_query.sh
# MOCK-001-A: 集成测试模式控制

set -e

# MOCK-001-A: 设置生产环境模式，禁用Mock测试
export AEP_TEST_MODE=real

# 检查是否为安静模式
QUIET_MODE=false
for arg in "$@"; do
    if [[ "$arg" == "--quiet" || "$arg" == "-q" ]]; then
        QUIET_MODE=true
        break
    fi
done

# Java版本检查函数
check_java_version() {
    if [[ "$QUIET_MODE" == "false" ]]; then
        echo "🔍 检查Java版本..."
    fi

    if ! command -v java &> /dev/null; then
        echo -e "\033[0;31m❌ 错误: Java未安装或未配置环境变量\033[0m"
        echo -e "\033[1;33m📋 请安装OpenJDK 25并配置JAVA_HOME\033[0m"
        exit 1
    fi

    # 获取Java版本
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)

    if [[ "$QUIET_MODE" == "false" ]]; then
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

        echo -e "\033[0;32m✅ 查询将继续执行\033[0m"
        echo
    else
        # 安静模式下只检查版本兼容性，不输出详细信息
        if [[ "$JAVA_VERSION" -lt "17" ]]; then
            echo -e "\033[1;33m⚠️ Java $JAVA_VERSION 可能不兼容，需要Java 17+\033[0m"
        fi
    fi
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

    if [[ "$QUIET_MODE" == "false" ]]; then
        echo -e "\033[0;32m[SUCCESS]\033[0m 环境变量加载成功"
    fi
}

# 执行Java版本检查
check_java_version

# 加载环境变量
check_and_load_env

# 处理默认参数
# 如果没有提供任何参数，使用默认的导出配置
if [[ $# -eq 0 ]]; then
    echo "🔧 未指定参数，使用默认配置："
    echo "   - 导出所有数据（产品和设备）"
    echo "   - 输出CSV格式"
    echo "   - 简化输出模式"
    echo ""
    echo "⏳ 准备数据导出，请稍候..."

    # 设置默认参数
    set -- "--export-all" "--format" "csv" "--quiet"
    QUIET_MODE=true
    export AEP_QUIET_MODE=true

    # IMP-004: 设置重试机制环境变量
    export AEP_MAX_RETRIES=2
    export AEP_RETRY_DELAY=3
else
    # 导出安静模式设置给下游脚本使用
    export AEP_QUIET_MODE="$QUIET_MODE"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/scripts/query/run_query.sh" "$@"
