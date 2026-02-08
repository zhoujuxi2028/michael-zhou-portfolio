#!/bin/bash

# AEP产品注册工具 - 产品注册脚本
# 功能：简化产品注册操作的命令行脚本
# 版本：1.0
# 日期：2026-01-25

# 设置脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示使用帮助
show_help() {
    echo "AEP产品注册工具 - 产品注册脚本"
    echo ""
    echo "用法: $0 [选项] <产品名称> <设备类型>"
    echo ""
    echo "位置参数:"
    echo "  产品名称       要注册的产品名称"
    echo "  设备类型       设备类型 (SENSOR, GATEWAY, DEVICE, TERMINAL, MODULE)"
    echo ""
    echo "选项:"
    echo "  -n, --network-type TYPE     网络类型 (NB-IOT, WiFi, 4G等)"
    echo "  -f, --data-format FORMAT    数据格式 (JSON, XML等)"
    echo "  -d, --description DESC      产品描述"
    echo "  -m, --device-model MODEL    设备型号"
    echo "  -M, --manufacturer MFG      制造商"
    echo "  -p, --protocol-type PROTO   协议类型 (CoAP, MQTT等)"
    echo "  -c, --max-device-count N    最大设备数量"
    echo "  -s, --enable-security       启用安全认证"
    echo "  -a, --auto-create-device    自动创建设备"
    echo "  -e, --env-file FILE         指定环境变量文件 (默认: .env)"
    echo "  -v, --verbose               详细输出"
    echo "  -h, --help                  显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 \"温度传感器\" SENSOR"
    echo "  $0 -n NB-IOT -f JSON -d \"室外温度传感器\" \"温度传感器\" SENSOR"
    echo "  $0 --enable-security --max-device-count 100 \"智能网关\" GATEWAY"
    echo ""
}

# 加载环境变量
load_env() {
    local env_file="${1:-.env}"
    local env_path="$PROJECT_DIR/$env_file"

    if [[ -f "$env_path" ]]; then
        log_info "加载环境变量文件: $env_path"

        # 导出环境变量（过滤注释和空行）
        while IFS='=' read -r key value; do
            # 跳过注释和空行
            if [[ ! "$key" =~ ^[[:space:]]*# ]] && [[ -n "$key" ]]; then
                # 移除值两端的引号
                value=$(echo "$value" | sed 's/^["'\'']//' | sed 's/["'\'']$//')
                export "$key"="$value"
                if [[ "$VERBOSE" == "true" ]]; then
                    log_info "设置 $key=***"
                fi
            fi
        done < <(grep -v '^[[:space:]]*$' "$env_path")

    else
        log_warning "环境变量文件不存在: $env_path"
        log_info "请从 .env.template 复制并配置环境变量文件"
        return 1
    fi
}

# 验证环境变量
validate_env() {
    local required_vars=("AEP_APP_KEY" "AEP_APP_SECRET" "AEP_API_HOST" "AEP_APP_ID")
    local missing_vars=()

    for var in "${required_vars[@]}"; do
        if [[ -z "${!var}" ]]; then
            missing_vars+=("$var")
        fi
    done

    if [[ ${#missing_vars[@]} -gt 0 ]]; then
        log_error "缺少必需的环境变量:"
        for var in "${missing_vars[@]}"; do
            echo "  - $var"
        done
        echo ""
        log_info "请确保在环境变量文件中设置了所有必需的变量"
        return 1
    fi

    log_success "环境变量验证通过"
    return 0
}

# 构建Maven项目
build_project() {
    log_info "构建Maven项目..."

    cd "$PROJECT_DIR" || {
        log_error "无法进入项目目录: $PROJECT_DIR"
        return 1
    }

    if [[ "$VERBOSE" == "true" ]]; then
        mvn clean compile
    else
        mvn clean compile -q
    fi

    if [[ $? -eq 0 ]]; then
        log_success "项目构建成功"
        return 0
    else
        log_error "项目构建失败"
        return 1
    fi
}

# 执行产品注册
register_product() {
    local product_name="$1"
    local device_type="$2"

    log_info "开始注册产品: $product_name"

    # 构建Java命令参数
    local java_args=(
        "--create"
        "--product-name" "$product_name"
        "--device-type" "$device_type"
    )

    # 添加可选参数
    [[ -n "$NETWORK_TYPE" ]] && java_args+=("--network-type" "$NETWORK_TYPE")
    [[ -n "$DATA_FORMAT" ]] && java_args+=("--data-format" "$DATA_FORMAT")
    [[ -n "$DESCRIPTION" ]] && java_args+=("--description" "$DESCRIPTION")
    [[ -n "$DEVICE_MODEL" ]] && java_args+=("--device-model" "$DEVICE_MODEL")
    [[ -n "$MANUFACTURER" ]] && java_args+=("--manufacturer" "$MANUFACTURER")
    [[ -n "$PROTOCOL_TYPE" ]] && java_args+=("--protocol-type" "$PROTOCOL_TYPE")
    [[ -n "$MAX_DEVICE_COUNT" ]] && java_args+=("--max-device-count" "$MAX_DEVICE_COUNT")
    [[ "$ENABLE_SECURITY" == "true" ]] && java_args+=("--enable-security")
    [[ "$AUTO_CREATE_DEVICE" == "true" ]] && java_args+=("--auto-create-device")

    # 执行Java程序
    cd "$PROJECT_DIR" || {
        log_error "无法进入项目目录: $PROJECT_DIR"
        return 1
    }

    log_info "执行命令: mvn exec:java -Dexec.args=\"${java_args[*]}\""

    mvn exec:java \
        -Dexec.mainClass="com.aep.registration.AepProductRegistration" \
        -Dexec.args="${java_args[*]}" \
        -q

    local exit_code=$?

    if [[ $exit_code -eq 0 ]]; then
        log_success "产品注册成功"
        return 0
    else
        log_error "产品注册失败 (退出代码: $exit_code)"
        return 1
    fi
}

# 主函数
main() {
    # 默认值
    ENV_FILE=".env"
    VERBOSE="false"
    ENABLE_SECURITY="false"
    AUTO_CREATE_DEVICE="false"

    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -n|--network-type)
                NETWORK_TYPE="$2"
                shift 2
                ;;
            -f|--data-format)
                DATA_FORMAT="$2"
                shift 2
                ;;
            -d|--description)
                DESCRIPTION="$2"
                shift 2
                ;;
            -m|--device-model)
                DEVICE_MODEL="$2"
                shift 2
                ;;
            -M|--manufacturer)
                MANUFACTURER="$2"
                shift 2
                ;;
            -p|--protocol-type)
                PROTOCOL_TYPE="$2"
                shift 2
                ;;
            -c|--max-device-count)
                MAX_DEVICE_COUNT="$2"
                shift 2
                ;;
            -s|--enable-security)
                ENABLE_SECURITY="true"
                shift
                ;;
            -a|--auto-create-device)
                AUTO_CREATE_DEVICE="true"
                shift
                ;;
            -e|--env-file)
                ENV_FILE="$2"
                shift 2
                ;;
            -v|--verbose)
                VERBOSE="true"
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            -*)
                log_error "未知选项: $1"
                show_help
                exit 1
                ;;
            *)
                break
                ;;
        esac
    done

    # 检查位置参数
    if [[ $# -lt 2 ]]; then
        log_error "缺少必需参数: 产品名称和设备类型"
        show_help
        exit 1
    fi

    local product_name="$1"
    local device_type="$2"

    log_info "AEP产品注册工具启动"
    log_info "产品名称: $product_name"
    log_info "设备类型: $device_type"

    # 执行步骤
    load_env "$ENV_FILE" || exit 1
    validate_env || exit 1
    build_project || exit 1
    register_product "$product_name" "$device_type" || exit 1

    log_success "产品注册流程完成"
}

# 错误处理
set -e
trap 'log_error "脚本执行过程中发生错误，行号: $LINENO"' ERR

# 执行主函数
main "$@"