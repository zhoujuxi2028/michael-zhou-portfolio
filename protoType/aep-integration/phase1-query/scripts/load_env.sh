#!/bin/bash

# AEP环境变量加载脚本
# 用于从 .env 文件加载认证信息到当前shell环境

echo "============================================================"
echo "AEP环境变量加载脚本"
echo "============================================================"

# 检查 .env 文件是否存在
if [ ! -f ".env" ]; then
    echo "❌ 错误: .env 文件不存在"
    echo "请先创建 .env 文件或从 .env.template 复制"
    echo ""
    echo "创建方法："
    echo "  cp .env.template .env"
    echo "  # 然后编辑 .env 文件，填入您的真实认证信息"
    exit 1
fi

echo "📂 发现 .env 文件，正在加载环境变量..."

# 加载 .env 文件中的环境变量
# 过滤掉注释行和空行
set -a  # 自动导出所有变量
source <(grep -v '^#' .env | grep -v '^$')
set +a

echo ""
echo "✅ 环境变量加载完成！"
echo ""
echo "📋 已加载的配置："
echo "  AEP_APP_KEY: ${AEP_APP_KEY:0:8}***"
echo "  AEP_APP_SECRET: ${AEP_APP_SECRET:0:4}***"
echo "  AEP_API_HOST: $AEP_API_HOST"

# 检查可选配置
if [ ! -z "$AEP_APP_ID" ]; then
    echo "  AEP_APP_ID: $AEP_APP_ID"
fi
if [ ! -z "$AEP_MASTER_KEY" ]; then
    echo "  AEP_MASTER_KEY: ${AEP_MASTER_KEY:0:8}***"
fi
if [ ! -z "$AEP_PRODUCT_ID" ]; then
    echo "  AEP_PRODUCT_ID: $AEP_PRODUCT_ID"
fi

echo ""
echo "🚀 现在可以运行AEP相关程序了："
echo "  java AepProductQuerySimple"
echo "  java AepProductManagementDemo_Enhanced"
echo ""
echo "============================================================"