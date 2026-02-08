#!/bin/bash

# AEP数据导出工具运行脚本
# 支持Maven构建和传统javac构建方式
# 版本: v2.0 - Maven优先

# 设置变量 - 项目目录配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LIB_DIR="${PROJECT_ROOT}/lib"
SRC_DIR="${PROJECT_ROOT}/src"
MAIN_SRC_DIR="${SRC_DIR}/main/java"
BUILD_DIR="${PROJECT_ROOT}/target/classes"

# 检查安静模式（从环境变量获取）
QUIET_MODE="${AEP_QUIET_MODE:-false}"

# 过滤掉 --quiet 和 -q 参数，因为这是脚本级别的控制，不需要传给Java程序
FILTERED_ARGS=()
for arg in "$@"; do
    if [[ "$arg" != "--quiet" && "$arg" != "-q" ]]; then
        FILTERED_ARGS+=("$arg")
    fi
done

# 检测构建方式
USE_MAVEN=false
if [ -f "$PROJECT_ROOT/pom.xml" ] && command -v mvn &> /dev/null; then
    USE_MAVEN=true
    if [[ "$QUIET_MODE" == "false" ]]; then
        echo "🔧 检测到Maven环境，使用Maven构建"
    fi
else
    if [[ "$QUIET_MODE" == "false" ]]; then
        echo "🔧 使用传统javac构建"
    fi
fi

# 加载环境变量 - 从项目根目录读取
if [ -f "$PROJECT_ROOT/.env" ]; then
    if [[ "$QUIET_MODE" == "false" ]]; then
        echo "📂 加载 .env 文件..."
    fi
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
fi

if [[ "$QUIET_MODE" == "false" ]]; then
    echo "============================================================"
    echo "AEP数据导出工具 - 众成通信物联网数据导出系统 (Java 17)"
    echo "App ID: ${AEP_APP_ID:-[从环境变量读取]}"
    echo "App Key: ${AEP_APP_KEY:0:8}***"
    echo "构建方式: $(if $USE_MAVEN; then echo 'Maven'; else echo '传统javac'; fi)"
    echo "============================================================"
fi

MAIN_CLASS="com.aep.export.AepDataExporter"

# 构建和运行逻辑
cd "$PROJECT_ROOT"

if $USE_MAVEN; then
    if [[ "$QUIET_MODE" == "false" ]]; then
        echo -e "\n1. Maven构建..."
    fi

    # 确保Maven编译通过
    if mvn compile -q; then
        if [[ "$QUIET_MODE" == "false" ]]; then
            echo "✅ Maven编译成功"
        fi
    else
        if [[ "$QUIET_MODE" == "false" ]]; then
            echo "❌ Maven编译失败"
            echo "尝试Maven清理后重新编译..."
        fi
        mvn clean compile -q
        if [ $? -ne 0 ]; then
            if [[ "$QUIET_MODE" == "false" ]]; then
                echo "❌ Maven构建失败，回退到传统方式"
            fi
            USE_MAVEN=false
        else
            if [[ "$QUIET_MODE" == "false" ]]; then
                echo "✅ Maven重新编译成功"
            fi
        fi
    fi

    if $USE_MAVEN; then
        if [[ "$QUIET_MODE" == "false" ]]; then
            echo -e "\n2. 使用Maven运行AEP数据导出工具..."
            echo "正在连接AEP平台..."
            echo "使用域名: ${AEP_API_HOST:-[从环境变量读取]}"
            echo "----------------------------------------"
        fi

        # 使用Maven执行主程序
        # WARN-001: Maven JVM配置通过.mvn/jvm.config自动加载

        # IMP-004: 错误重试机制
        MAX_RETRIES=${AEP_MAX_RETRIES:-1}
        RETRY_DELAY=${AEP_RETRY_DELAY:-2}
        RETRY_COUNT=0

        while [ $RETRY_COUNT -le $MAX_RETRIES ]; do
            if [[ "$QUIET_MODE" == "true" ]]; then
                # 安静模式：抑制stderr警告，只显示关键信息
                mvn exec:java -Dexec.mainClass="$MAIN_CLASS" -Dexec.args="${FILTERED_ARGS[*]}" -q 2>/dev/null | grep -E "✅|❌|输出文件|产品数量|设备数量|文件大小|备份文件|AEP.*初始化.*完成|  - \./output/"
                MAVEN_EXIT_CODE=${PIPESTATUS[0]}
            else
                # 详细模式：只过滤警告，保留其他信息
                mvn exec:java -Dexec.mainClass="$MAIN_CLASS" -Dexec.args="${FILTERED_ARGS[*]}" -q 2>&1 | grep -v -E "(WARNING.*sun\.misc\.Unsafe)|(WARNING.*staticFieldBase)|(WARNING.*HiddenClassDefiner)|(WARNING.*will be removed in a future release)"
                MAVEN_EXIT_CODE=${PIPESTATUS[0]}
            fi

            # 如果成功或者已经是最后一次重试，退出循环
            if [ $MAVEN_EXIT_CODE -eq 0 ] || [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
                break
            fi

            # 重试逻辑
            RETRY_COUNT=$((RETRY_COUNT + 1))
            if [[ "$QUIET_MODE" == "false" ]]; then
                echo "⚠️  执行失败，${RETRY_DELAY}秒后进行第${RETRY_COUNT}次重试..."
            fi
            sleep $RETRY_DELAY
        done
        if [ $MAVEN_EXIT_CODE -eq 0 ]; then
            if [[ "$QUIET_MODE" == "false" ]]; then
                echo -e "\n✅ AEP数据导出工具执行完成 (Maven方式)"
            fi
            exit 0
        else
            if [[ "$QUIET_MODE" == "false" ]]; then
                echo -e "\n❌ Maven执行失败，回退到传统方式..."
                echo -e "原因: Maven对system scope依赖的exec:java支持有限制"
                echo -e "\n🔄 使用传统构建方式重试..."
            fi
            USE_MAVEN=false
        fi
    fi
fi

if ! $USE_MAVEN; then
    # 传统javac构建方式
    if [[ "$QUIET_MODE" == "false" ]]; then
        echo -e "\n1. 检查依赖库..."
    fi
    mkdir -p "$BUILD_DIR"

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

    if [[ "$QUIET_MODE" == "false" ]]; then
        echo "✅ 所有依赖库检查通过"
        echo -e "\n2. 编译AEP数据导出工具..."
    fi

    CLASSPATH="$LIB_DIR/*:$BUILD_DIR"

    # 编译项目
    if find "$MAIN_SRC_DIR" -name "*.java" -exec javac -cp "$CLASSPATH" -d "$BUILD_DIR" {} +; then
        if [[ "$QUIET_MODE" == "false" ]]; then
            echo "✅ 编译成功"
            echo -e "\n3. 运行AEP数据导出工具..."
            echo "正在连接AEP平台..."
            echo "使用域名: ${AEP_API_HOST:-[从环境变量读取]}"
            echo "----------------------------------------"
        fi
    else
        echo "❌ 编译失败"
        echo "请检查Java环境和源代码"
        exit 1
    fi

    # 运行主程序
    # WARN-001: JVM参数抑制sun.misc.Unsafe警告
    JAVA_OPTS="--add-opens=java.base/sun.misc=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED"

    # IMP-004: 错误重试机制（传统javac方式）
    MAX_RETRIES=${AEP_MAX_RETRIES:-1}
    RETRY_DELAY=${AEP_RETRY_DELAY:-2}
    RETRY_COUNT=0

    while [ $RETRY_COUNT -le $MAX_RETRIES ]; do
        if [[ "$QUIET_MODE" == "true" ]]; then
            # 安静模式：抑制stderr警告，只显示关键信息
            java $JAVA_OPTS -cp "$CLASSPATH" "$MAIN_CLASS" "${FILTERED_ARGS[@]}" 2>/dev/null | grep -E "✅|❌|输出文件|产品数量|设备数量|文件大小|备份文件|AEP.*初始化.*完成|  - \./output/"
            JAVA_EXIT_CODE=${PIPESTATUS[0]}
        else
            # 详细模式：只过滤警告，保留其他信息
            java $JAVA_OPTS -cp "$CLASSPATH" "$MAIN_CLASS" "${FILTERED_ARGS[@]}" 2>&1 | grep -v -E "(WARNING.*sun\.misc\.Unsafe)|(WARNING.*staticFieldBase)|(WARNING.*HiddenClassDefiner)|(WARNING.*will be removed in a future release)"
            JAVA_EXIT_CODE=${PIPESTATUS[0]}
        fi

        # 如果成功或者已经是最后一次重试，退出循环
        if [ $JAVA_EXIT_CODE -eq 0 ] || [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
            break
        fi

        # 重试逻辑
        RETRY_COUNT=$((RETRY_COUNT + 1))
        if [[ "$QUIET_MODE" == "false" ]]; then
            echo "⚠️  执行失败，${RETRY_DELAY}秒后进行第${RETRY_COUNT}次重试..."
        fi
        sleep $RETRY_DELAY
    done

    if [ $JAVA_EXIT_CODE -eq 0 ]; then
        if [[ "$QUIET_MODE" == "false" ]]; then
            echo -e "\n✅ AEP数据导出工具执行完成 (传统方式)"
        fi
    else
        echo -e "\n❌ AEP数据导出工具执行失败 (传统方式)"
        echo -e "\n故障排除建议:"
        echo "1. 检查网络连接"
        echo "2. 验证认证信息是否正确"
        echo "3. 确认AEP平台服务状态"
        echo "4. 检查应用权限设置"
        echo "5. 确认所有依赖库都已安装"
        exit 1
    fi
fi

if [[ "$QUIET_MODE" == "false" ]]; then
    echo -e "\n============================================================"
    echo "AEP数据导出工具执行完成"
    echo "功能说明："
    echo "- 使用官方AEP SDK进行数据导出"
    echo "- 支持产品和设备数据的CSV导出"
    echo "- 提供完整的错误处理和日志记录"
    echo "如需查看详细使用说明，请参考 README.md"
    echo "============================================================"
fi