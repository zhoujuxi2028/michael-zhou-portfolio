#!/bin/bash

# AEP产品管理增强版demo运行脚本

# 设置变量 - 方案一：项目根目录配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PHASE_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$PHASE_DIR")"
LIB_DIR="${PHASE_DIR}/lib"
SRC_DIR="${PHASE_DIR}/src"

# 加载环境变量 - 从项目根目录读取
if [ -f "$PROJECT_ROOT/.env" ]; then
    echo "📂 加载 .env 文件..."
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
fi

echo "============================================================"
echo "AEP产品管理增强版Demo - 使用官方SDK"
echo "App ID: ${AEP_APP_ID:-[从环境变量读取]}"
echo "App Key: ${AEP_APP_KEY:0:8}***"
echo "============================================================"

ENHANCED_DEMO="AepProductManagementDemo_Enhanced"

# 进入源码目录
cd "$SRC_DIR"

echo "1. 检查依赖库..."
REQUIRED_LIBS=(
    "ctg-ag-sdk-core-2.9.0-20251210.111829-4.jar"
    "ag-sdk-biz-267848.tar.gz-20251226.210203-SNAPSHOT.jar"
    "httpclient-4.5.13.jar"
    "httpcore-4.4.13.jar"
    "commons-logging-1.2.jar"
    "commons-codec-1.15.jar"
)

for lib in "${REQUIRED_LIBS[@]}"; do
    if [ ! -f "$LIB_DIR/$lib" ]; then
        echo "❌ 错误: 依赖库未找到: $lib"
        echo "请确认路径: $LIB_DIR/$lib"
        exit 1
    fi
done

echo "✅ 所有依赖库检查通过"

echo -e "\n2. 编译增强版demo..."
CLASSPATH="$LIB_DIR/*:."

if javac -cp "$CLASSPATH" "${ENHANCED_DEMO}.java"; then
    echo "✅ 编译成功"
else
    echo "❌ 编译失败"
    echo "请检查Java环境和SDK依赖"
    exit 1
fi

echo -e "\n3. 运行增强版产品管理功能..."
echo "正在连接AEP平台（使用官方SDK）..."
echo "使用域名: ${AEP_API_HOST:-[从环境变量读取]}"
echo "----------------------------------------"

# 运行增强版demo
if java -cp "$CLASSPATH" "$ENHANCED_DEMO"; then
    echo -e "\n✅ 增强版demo执行完成"
else
    echo -e "\n❌ 增强版demo执行失败"
    echo -e "\n故障排除建议:"
    echo "1. 检查网络连接"
    echo "2. 验证认证信息是否正确"
    echo "3. 确认AEP平台服务状态"
    echo "4. 检查应用权限设置"
    echo "5. 确认所有依赖库都已安装"
fi

echo -e "\n============================================================"
echo "增强版demo执行完成"
echo "功能说明："
echo "- 使用官方AEP SDK"
echo "- 支持完整的产品管理API"
echo "- 提供详细的JSON响应信息"
echo "如需查看详细使用说明，请参考 README.md"
echo "============================================================"