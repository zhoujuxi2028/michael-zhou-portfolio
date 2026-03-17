#!/bin/bash

echo "=== 电信物联网平台API验证程序 ==="
echo "配置来源: vendor-b/zc_backend/nmps-impl/src/main/java/cn/com/git/nmps/impl/iot/utils/Constant.java"
echo "设备数据来源: 151服务器backup数据/sql/t_deviceinfo.sql"
echo ""

# 进入项目目录
cd "$(dirname "$0")"

# 检查Maven是否可用
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误: 未找到Maven命令，请先安装Maven"
    echo "macOS: brew install maven"
    echo "或者使用项目根目录的mvnw命令"
    exit 1
fi

# 检查Java是否可用
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到Java命令，请先安装JDK 8+"
    exit 1
fi

echo "→ 检查Java版本..."
java -version

echo ""
echo "→ 编译项目..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi

echo ""
echo "→ 运行SDK验证程序..."
echo "  注意: 这将使用电信官方SDK和vendor-b项目的真实配置进行测试"
echo "  使用SDK: ctg-ag-sdk-core + ag-sdk-biz"
echo ""

mvn exec:java -Dexec.mainClass="poc.TelecomSDKTest"

echo ""
echo "=== 验证完成 ==="