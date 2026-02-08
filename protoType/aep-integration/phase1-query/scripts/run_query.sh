#!/bin/bash

# AEP产品管理查询脚本
# 用于编译和运行增强版产品管理demo

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
echo "AEP产品管理 - 查询您的设备产品列表"
echo "App ID: ${AEP_APP_ID:-[从环境变量读取]}"
echo "App Key: ${AEP_APP_KEY:0:8}***"
echo "============================================================"

SIMPLE_DEMO="AepProductQuerySimple"

# 进入源码目录
cd "$SRC_DIR"

echo "1. 编译简化版查询工具..."
if javac "${SIMPLE_DEMO}.java"; then
    echo "✅ 编译成功"
else
    echo "❌ 编译失败"
    echo "请检查Java环境"
    exit 1
fi

echo -e "\n2. 运行产品列表查询..."
echo "正在连接AEP平台..."
echo "使用域名: ${AEP_API_HOST:-[从环境变量读取]}"
echo "----------------------------------------"

# 运行简化版查询功能
if java "$SIMPLE_DEMO"; then
    echo -e "\n✅ 查询执行完成"
else
    echo -e "\n❌ 查询执行失败"
    echo -e "\n故障排除建议:"
    echo "1. 检查网络连接"
    echo "2. 验证认证信息是否正确"
    echo "3. 确认AEP平台服务状态"
    echo "4. 检查应用权限设置"
fi

echo -e "\n============================================================"
echo "查询脚本执行完成"
echo "如需查看详细使用说明，请参考 README.md"
echo "============================================================"