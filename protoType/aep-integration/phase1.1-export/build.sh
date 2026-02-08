#!/bin/bash
# 便捷构建脚本入口点
# 调用 scripts/build/build.sh
# MOCK-001-A: 集成测试模式控制

# 解析输出控制参数
VERBOSE=false
QUIET=false
for arg in "$@"; do
    case $arg in
        --verbose|-v)
            VERBOSE=true
            ;;
        --quiet|-q)
            QUIET=true
            ;;
    esac
done

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Java版本检查函数
check_java_version() {
    if ! command -v java &> /dev/null; then
        echo -e "${RED}[FAIL]${NC} Java not installed or not in PATH"
        echo "Please install OpenJDK 21+ and configure JAVA_HOME"
        exit 1
    fi

    # 获取Java版本
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)

    # 简化版本检查 - 直接验证兼容性
    if [[ "$JAVA_VERSION" -ge "17" ]]; then
        if [[ "$QUIET" != "true" ]]; then
            if [[ "$VERBOSE" == "true" ]]; then
                echo -e "${GREEN}[OK]${NC} Java $JAVA_VERSION.$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f2) ($(java -version 2>&1 | grep "Temurin" | head -n 1 | awk '{print $4}')) detected - compatible"
            else
                echo -e "${GREEN}[OK]${NC} Java $JAVA_VERSION detected - compatible"
            fi
        fi
    else
        echo -e "${RED}[FAIL]${NC} Java $JAVA_VERSION detected - requires Java 17+"
        echo "Please upgrade to OpenJDK 21 LTS: https://adoptium.net/"
        exit 1
    fi
}

# MOCK-001-A: 设置生产环境模式，禁用Mock测试
export AEP_TEST_MODE=real

# 执行Java版本检查
check_java_version

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/scripts/build/build.sh" "$@"
