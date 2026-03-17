#!/bin/bash

# AEP环境变量导出脚本
# 用法：source export_env.sh

echo "============================================================"
echo "AEP环境变量导出脚本"
echo "============================================================"

# 检查 .env 文件是否存在
if [ ! -f ".env" ]; then
    echo "❌ 错误: .env 文件不存在"
    echo "请先创建 .env 文件或从 .env.template 复制"
    echo ""
    echo "创建方法："
    echo "  cp .env.template .env"
    echo "  # 然后编辑 .env 文件，填入您的真实认证信息"
    return 1 2>/dev/null || exit 1
fi

echo "📂 发现 .env 文件，正在导出环境变量..."

# 从.env文件读取并导出环境变量
while IFS='=' read -r key value; do
    # 跳过注释行和空行
    if [[ $key =~ ^#.* ]] || [[ -z $key ]]; then
        continue
    fi

    # 移除值两端的引号
    value=$(echo "$value" | sed 's/^["'\'']*//g' | sed 's/["'\'']*$//g')

    # 导出环境变量
    export "$key"="$value"

done < .env

echo ""
echo "✅ 环境变量导出完成！"
echo ""
echo "📋 已导出的配置："
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
echo "  ./run_query.sh"
echo ""
echo "============================================================"