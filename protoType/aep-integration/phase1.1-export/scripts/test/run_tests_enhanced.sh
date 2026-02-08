#!/bin/bash

# AEP数据导出工具 - 综合测试执行和报告生成脚本 (Maven集成版)
# 版本: v3.1 - MOCK-001-A
# 创建日期: 2026年1月5日
# 更新说明: 支持Maven和传统javac两种构建方式 + Mock测试控制

set +e

# MOCK-001-A-3.1: Mock测试控制配置
# 如果未设置测试模式，默认使用mock模式
if [ -z "$AEP_TEST_MODE" ]; then
    export AEP_TEST_MODE=mock
    echo -e "${CYAN}[INFO]${NC} AEP_TEST_MODE未设置，使用默认值: mock"
fi

# 其他测试控制变量的默认值
if [ -z "$ENABLE_MOCK_TESTS" ]; then
    export ENABLE_MOCK_TESTS=true
fi

if [ -z "$ENABLE_POLLUTION_DETECTION" ]; then
    export ENABLE_POLLUTION_DETECTION=true
fi

if [ -z "$TEST_CLEANUP_ENABLED" ]; then
    export TEST_CLEANUP_ENABLED=true
fi

# 显示当前测试配置
echo -e "${PURPLE}[INFO]${NC} 当前测试配置:"
echo -e "  AEP_TEST_MODE: ${AEP_TEST_MODE}"
echo -e "  ENABLE_MOCK_TESTS: ${ENABLE_MOCK_TESTS}"
echo -e "  ENABLE_POLLUTION_DETECTION: ${ENABLE_POLLUTION_DETECTION}"
echo -e "  TEST_CLEANUP_ENABLED: ${TEST_CLEANUP_ENABLED}"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# 路径配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LIB_DIR="$PROJECT_ROOT/lib"
SRC_DIR="$PROJECT_ROOT/src"
BUILD_DIR="$PROJECT_ROOT/target/classes"
TEST_BUILD_DIR="$PROJECT_ROOT/target/test-classes"
LOGS_DIR="$PROJECT_ROOT/logs"
REPORTS_DIR="$PROJECT_ROOT/reports"
TEST_RESULTS_DIR="$LOGS_DIR/test-results"
DETAILED_LOGS_DIR="$LOGS_DIR/detailed-test-results"

# 检测构建方式
USE_MAVEN=false
if [ -f "$PROJECT_ROOT/pom.xml" ] && command -v mvn &> /dev/null; then
    USE_MAVEN=true
    echo -e "${BLUE}[INFO]${NC} 检测到Maven环境，使用Maven构建和测试"
else
    echo -e "${BLUE}[INFO]${NC} 使用传统javac构建和测试"
fi

# 测试配置
CLASSPATH="$LIB_DIR/*:$BUILD_DIR:$TEST_BUILD_DIR"

# 时间戳
EXECUTION_ID=$(date +%Y%m%d_%H%M%S)
START_TIME=$(date +%s)

# 统计变量 - 确保初始化为数字
PASSED_FILES=0
FAILED_FILES=0
PASSED_TEST_CASES=0
FAILED_TEST_CASES=0

# 安全的数学计算函数
safe_divide() {
    local numerator=$(echo "$1" | sed 's/[^0-9]//g')
    local denominator=$(echo "$2" | sed 's/[^0-9]//g')
    local scale=${3:-1}

    # 确保参数是有效数字
    numerator=${numerator:-0}
    denominator=${denominator:-0}

    if [[ $denominator -eq 0 ]]; then
        echo "0"
        return
    fi

    local result=$(echo "scale=$scale; $numerator * 100 / $denominator" | bc -l 2>/dev/null | tr -d '\n' || echo "0")
    echo "${result:-0}"
}

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$MAIN_LOG"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$MAIN_LOG"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$MAIN_LOG"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$MAIN_LOG"
}

# 进度条函数
show_progress() {
    local current=$1
    local total=$2
    local test_name=$3
    local status=$4

    local percentage=$((current * 100 / total))
    local filled=$((percentage / 2))
    local empty=$((50 - filled))

    printf "\r${CYAN}进度${NC} ["
    printf "%0.s=" $(seq 1 $filled)
    printf "%0.s-" $(seq 1 $empty)
    printf "] %3d%% (%d/%d) %s %s" $percentage $current $total "$test_name" "$status"
}

# 创建目录结构
create_directories() {
    log_info "创建测试目录结构..."
    mkdir -p "$LOGS_DIR" "$REPORTS_DIR" "$TEST_RESULTS_DIR" "$DETAILED_LOGS_DIR"
    mkdir -p "$REPORTS_DIR/html" "$REPORTS_DIR/json" "$REPORTS_DIR/csv"
    log_success "目录结构创建完成"
}

# 检查依赖库
check_dependencies() {
    log_info "检查业务依赖库..."

    local business_libs=(
        "ctg-ag-sdk-core-2.9.0-20251210.111829-4.jar"
        "ag-sdk-biz-267848.tar.gz-20251226.210203-SNAPSHOT.jar"
        "httpclient-4.5.13.jar"
        "httpcore-4.4.13.jar"
        "commons-logging-1.2.jar"
        "commons-codec-1.15.jar"
    )

    for lib in "${business_libs[@]}"; do
        if [[ ! -f "$LIB_DIR/$lib" ]]; then
            log_error "业务依赖库未找到: $lib"
            log_error "请确认路径: $LIB_DIR/$lib"
            exit 1
        fi
    done
    log_success "业务依赖库检查通过"

    log_info "检查JUnit测试依赖库..."

    local test_libs=(
        "junit-jupiter-api-5.10.1.jar"
        "junit-jupiter-engine-5.10.1.jar"
        "junit-platform-launcher-1.10.1.jar"
        "junit-platform-console-standalone-1.10.1.jar"
    )

    for lib in "${test_libs[@]}"; do
        if [[ ! -f "$LIB_DIR/$lib" ]]; then
            log_error "JUnit依赖库未找到: $lib"
            log_error "请确认路径: $LIB_DIR/$lib"
            exit 1
        fi
    done
    log_success "JUnit依赖库检查通过"
}

# 环境检查
check_environment() {
    log_info "检查运行环境..."

    # 检查Java
    if ! command -v java &> /dev/null; then
        log_error "Java未安装或不在PATH中"
        exit 1
    fi

    local java_version=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}')
    log_success "Java版本: $java_version"

    # 检查依赖库
    check_dependencies

    # 检查编译文件
    if [[ ! -d "$BUILD_DIR" ]] || [[ ! "$(find "$BUILD_DIR" -name "*.class" 2>/dev/null)" ]]; then
        log_warning "代码未编译，开始编译..."
        compile_code
    fi

    # 加载环境变量
    if [[ -f "../.env" ]]; then
        source ../.env
        log_success "环境变量已从../.env加载"
    elif [[ -f ".env" ]]; then
        source .env
        log_success "环境变量已从.env加载"
    else
        log_warning "未找到环境变量文件，使用默认配置"
        export AEP_APP_KEY="CIj7aTFV1R9"
        export AEP_APP_SECRET="zGEm97sb5M"
        export AEP_API_HOST="10433748.api.ctwing.cn"
        export AEP_APP_ID="267848"
    fi

    log_success "环境检查完成"
}

# 编译代码
compile_code() {
    log_info "编译Java源代码..."

    mkdir -p "$BUILD_DIR" "$TEST_BUILD_DIR"

    # 编译主源代码
    if find "$SRC_DIR/main/java" -name "*.java" -exec javac -cp "$CLASSPATH" -d "$BUILD_DIR" {} +; then
        log_success "主源代码编译完成"
    else
        log_error "主源代码编译失败"
        exit 1
    fi

    # 编译测试代码
    if find "$SRC_DIR/test/java" -name "*.java" -exec javac -cp "$CLASSPATH" -d "$TEST_BUILD_DIR" {} +; then
        log_success "测试代码编译完成"
    else
        log_error "测试代码编译失败"
        log_error "请检查JUnit依赖和源代码"
        exit 1
    fi
}

# 执行JUnit标准测试
run_junit_test() {
    local test_class=$1
    local test_name=$2
    local test_index=$3
    local total_tests=$4
    local category=$5

    local log_file="$DETAILED_LOGS_DIR/${test_name}_${EXECUTION_ID}.log"
    local result_file="$TEST_RESULTS_DIR/${test_name}_result_${EXECUTION_ID}.json"

    show_progress $test_index $total_tests "$test_name" "运行JUnit..."

    local start_time=$(date +%s)
    local test_status="UNKNOWN"
    local error_message=""
    local exit_code=0

    # 执行测试 (使用JUnit Platform Console Standalone)
    cd "$PROJECT_ROOT"
    if java -jar "$LIB_DIR/junit-platform-console-standalone-1.10.1.jar" --cp "$LIB_DIR/*:$BUILD_DIR:$TEST_BUILD_DIR" --select-class="$test_class" > "$log_file" 2>&1; then
        test_status="PASS"
        show_progress $test_index $total_tests "$test_name" "${GREEN}✅ JUnit${NC}"
    else
        exit_code=$?
        test_status="FAIL"
        error_message=$(tail -5 "$log_file" | tr '\n' ' ' | sed 's/[^a-zA-Z0-9 .:_-]//g')
        show_progress $test_index $total_tests "$test_name" "${RED}❌ JUnit${NC}"
    fi
    cd "$PROJECT_ROOT"

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    # 统计测试用例数量 (JUnit格式)
    local test_cases_count=1
    if [[ -f "$log_file" ]]; then
        # JUnit格式: 查找测试成功和失败的数量
        local junit_successful=$(grep "tests successful" "$log_file" 2>/dev/null | sed 's/.*\[\s*\([0-9]\+\)\s*tests successful\s*\].*/\1/')
        local junit_failed=$(grep "tests failed" "$log_file" 2>/dev/null | sed 's/.*\[\s*\([0-9]\+\)\s*tests failed\s*\].*/\1/')

        junit_successful=$(echo "$junit_successful" | sed 's/[^0-9]//g')
        junit_failed=$(echo "$junit_failed" | sed 's/[^0-9]//g')
        junit_successful=${junit_successful:-0}
        junit_failed=${junit_failed:-0}

        local junit_total=$((junit_successful + junit_failed))
        test_cases_count=${junit_total:-1}
    fi

    # 生成结果JSON
    cat > "$result_file" << EOF
{
    "testClass": "$test_class",
    "testName": "$test_name",
    "category": "$category",
    "testFramework": "JUnit",
    "status": "$test_status",
    "exitCode": $exit_code,
    "duration": $duration,
    "testCasesCount": $test_cases_count,
    "startTime": "$start_time",
    "endTime": "$end_time",
    "logFile": "$log_file",
    "errorMessage": "$error_message",
    "executionId": "$EXECUTION_ID"
}
EOF

    echo "" # 换行

    # 更新统计
    test_cases_count=$(echo "$test_cases_count" | sed 's/[^0-9]//g')
    test_cases_count=${test_cases_count:-1}

    if [[ "$test_status" == "PASS" ]]; then
        PASSED_FILES=$((PASSED_FILES + 1))
        PASSED_TEST_CASES=$((PASSED_TEST_CASES + test_cases_count))
    else
        FAILED_FILES=$((FAILED_FILES + 1))
        FAILED_TEST_CASES=$((FAILED_TEST_CASES + test_cases_count))
    fi

    return $exit_code
}

# 执行自定义TDD测试
run_custom_test() {
    local test_class=$1
    local test_name=$2
    local test_index=$3
    local total_tests=$4
    local category=$5

    local log_file="$DETAILED_LOGS_DIR/${test_name}_${EXECUTION_ID}.log"
    local result_file="$TEST_RESULTS_DIR/${test_name}_result_${EXECUTION_ID}.json"

    show_progress $test_index $total_tests "$test_name" "运行TDD..."

    local start_time=$(date +%s)
    local test_status="UNKNOWN"
    local error_message=""
    local exit_code=0

    # 执行自定义TDD测试 (调用main方法)
    cd "$PROJECT_ROOT"
    if java -ea -cp "$LIB_DIR/*:$BUILD_DIR:$TEST_BUILD_DIR" "$test_class" > "$log_file" 2>&1; then
        test_status="PASS"
        show_progress $test_index $total_tests "$test_name" "${GREEN}✅ TDD${NC}"
    else
        exit_code=$?
        test_status="FAIL"
        error_message=$(tail -5 "$log_file" | tr '\n' ' ' | sed 's/[^a-zA-Z0-9 .:_-]//g')
        show_progress $test_index $total_tests "$test_name" "${RED}❌ TDD${NC}"
    fi
    cd "$PROJECT_ROOT"

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    # 统计TDD测试用例数量 - 智能识别
    local test_cases_count=1
    local test_file="$SRC_DIR/test/java/$(echo "$test_class" | tr '.' '/').java"

    # 方法1: 从源代码直接分析测试方法数量（最准确）
    if [[ -f "$test_file" ]]; then
        # 分析main方法中调用的testXxx()方法
        local source_method_count=$(grep -o 'test[A-Za-z_]*()' "$test_file" | wc -l | tr -d ' ')
        source_method_count=${source_method_count:-0}

        # 如果没找到testXxx()调用，尝试查找test方法定义
        if [[ $source_method_count -eq 0 ]]; then
            source_method_count=$(grep -c 'public static void test' "$test_file" | tr -d ' ')
            source_method_count=${source_method_count:-0}
        fi

        if [[ $source_method_count -gt 0 ]]; then
            test_cases_count=$source_method_count
        else
            # 备用：根据测试文档中的已知数量 (总计138个测试用例)
            case "$test_name" in
                # Model层测试 (46个测试用例)
                "ProductInfoTest") test_cases_count=7 ;;  # JUnit格式，已确认
                "SimpleProductInfoTest") test_cases_count=7 ;;
                "DeviceInfoTest") test_cases_count=9 ;;
                "ExportResultTest") test_cases_count=10 ;;
                "ExportConfigTest") test_cases_count=10 ;;
                "PagedResultTest") test_cases_count=10 ;;
                # Service层测试 (80个测试用例)
                "ConfigServiceTest") test_cases_count=10 ;;
                "ConfigServicePriorityTest") test_cases_count=4 ;;
                "AepClientManagerTest") test_cases_count=10 ;;
                "FileManagerTest") test_cases_count=10 ;;
                "ErrorHandlerTest") test_cases_count=10 ;;
                "ProductServiceTest") test_cases_count=10 ;;
                "DeviceServiceTest") test_cases_count=10 ;;
                "ExportServiceTest") test_cases_count=10 ;;
                # 主程序测试 (30个测试用例)
                "AepDataExporterTest") test_cases_count=30 ;;
                # TDD RED阶段测试 (9个测试用例) - 预期失败
                "ConfigServicePriorityTest") test_cases_count=4 ;;
                "AepDataExporterConfigTest") test_cases_count=5 ;;
                *) test_cases_count=10 ;;
            esac
        fi
    fi

    # 方法2: 从执行日志验证（作为校验）
    if [[ -f "$log_file" ]]; then
        # 查找TDD测试用例编号 (TC-UNIT-FUNC-xxx)
        local tc_count=$(grep -c "TC-UNIT-FUNC" "$log_file" 2>/dev/null || echo "0")
        tc_count=$(echo "$tc_count" | sed 's/[^0-9]//g' | head -1)
        tc_count=${tc_count:-0}

        # 如果日志中有更准确的信息，则使用日志信息
        if [[ $tc_count -gt 0 ]] && [[ $tc_count -ne $test_cases_count ]]; then
            # REPORT-001修复: 降低警告级别并添加解释说明
            if [[ "${VERBOSE_TEST_COUNT:-false}" == "true" ]]; then
                log_info "测试计数详情: 源码扫描${test_cases_count}个方法，实际执行${tc_count}个验证点 (差异为正常现象)"
            fi
            test_cases_count=$tc_count
        fi

        # 查找GREEN测试通过标记作为验证
        local green_count=$(grep -c "🟢 GREEN" "$log_file" 2>/dev/null || echo "0")
        green_count=$(echo "$green_count" | sed 's/[^0-9]//g' | head -1)
        green_count=${green_count:-0}

        # 如果有GREEN标记且与源码分析结果一致，确认计数正确
        if [[ $green_count -gt 0 ]] && [[ $green_count -eq $test_cases_count ]]; then
            log_info "测试用例计数验证通过: $test_cases_count"
        fi
    fi

    # 生成结果JSON
    cat > "$result_file" << EOF
{
    "testClass": "$test_class",
    "testName": "$test_name",
    "category": "$category",
    "testFramework": "Custom TDD",
    "status": "$test_status",
    "exitCode": $exit_code,
    "duration": $duration,
    "testCasesCount": $test_cases_count,
    "startTime": "$start_time",
    "endTime": "$end_time",
    "logFile": "$log_file",
    "errorMessage": "$error_message",
    "executionId": "$EXECUTION_ID"
}
EOF

    echo "" # 换行

    # 更新统计
    test_cases_count=$(echo "$test_cases_count" | sed 's/[^0-9]//g')
    test_cases_count=${test_cases_count:-1}

    if [[ "$test_status" == "PASS" ]]; then
        PASSED_FILES=$((PASSED_FILES + 1))
        PASSED_TEST_CASES=$((PASSED_TEST_CASES + test_cases_count))
    else
        FAILED_FILES=$((FAILED_FILES + 1))
        FAILED_TEST_CASES=$((FAILED_TEST_CASES + test_cases_count))
    fi

    return $exit_code
}

# 执行单个测试（智能检测测试类型）
run_single_test() {
    local test_class=$1
    local test_name=$2
    local test_index=$3
    local total_tests=$4
    local category=$5

    # 检查测试类型：是否包含@Test注解
    local test_file="$SRC_DIR/test/java/$(echo "$test_class" | tr '.' '/').java"

    if [[ -f "$test_file" ]] && grep -q "@Test" "$test_file"; then
        # JUnit 5 标准测试
        run_junit_test "$test_class" "$test_name" $test_index $total_tests "$category"
    else
        # 自定义TDD测试
        run_custom_test "$test_class" "$test_name" $test_index $total_tests "$category"
    fi
}

# 执行测试套件
run_test_suite() {
    log_info "开始执行测试套件..."

    # 定义测试文件 (支持JUnit测试)
    declare -a MODEL_TESTS=(
        "com.aep.export.model.ProductInfoTest:ProductInfoTest:MODEL"
        "com.aep.export.model.SimpleProductInfoTest:SimpleProductInfoTest:MODEL"
        "com.aep.export.model.DeviceInfoTest:DeviceInfoTest:MODEL"
        "com.aep.export.model.ExportResultTest:ExportResultTest:MODEL"
        "com.aep.export.model.ExportConfigTest:ExportConfigTest:MODEL"
        "com.aep.export.model.PagedResultTest:PagedResultTest:MODEL"
    )

    declare -a SERVICE_TESTS=(
        "com.aep.export.service.ConfigServiceTest:ConfigServiceTest:SERVICE"
        "com.aep.export.service.AepClientManagerTest:AepClientManagerTest:SERVICE"
        "com.aep.export.service.FileManagerTest:FileManagerTest:SERVICE"
        "com.aep.export.service.ErrorHandlerTest:ErrorHandlerTest:SERVICE"
        "com.aep.export.service.ProductServiceTest:ProductServiceTest:SERVICE"
        "com.aep.export.service.DeviceServiceTest:DeviceServiceTest:SERVICE"
        "com.aep.export.service.ExportServiceTest:ExportServiceTest:SERVICE"
    )

    declare -a MAIN_TESTS=(
        "com.aep.export.AepDataExporterTest:AepDataExporterTest:MAIN"
    )

    # TDD RED阶段测试 - 预期失败，用于开发验证
    declare -a TDD_RED_TESTS=(
        "com.aep.export.service.ConfigServicePriorityTest:ConfigServicePriorityTest:TDD-RED"
        "com.aep.export.AepDataExporterConfigTest:AepDataExporterConfigTest:TDD-RED"
    )

    local test_index=0
    local total_files=$((${#MODEL_TESTS[@]} + ${#SERVICE_TESTS[@]} + ${#MAIN_TESTS[@]}))

    # 根据测试过滤器选择要运行的测试
    local tests_to_run=()

    case "${TEST_FILTER:-production}" in
        "model")
            tests_to_run=("${MODEL_TESTS[@]}")
            log_info "${CYAN}仅执行Model层测试...${NC}"
            ;;
        "service")
            tests_to_run=("${SERVICE_TESTS[@]}")
            log_info "${CYAN}仅执行Service层测试...${NC}"
            ;;
        "main")
            tests_to_run=("${MAIN_TESTS[@]}")
            log_info "${CYAN}仅执行Main层测试...${NC}"
            ;;
        "tdd-red")
            tests_to_run=("${TDD_RED_TESTS[@]}")
            log_info "${YELLOW}执行TDD RED阶段测试（预期失败）...${NC}"
            ;;
        "all-with-tdd")
            tests_to_run=("${MODEL_TESTS[@]}" "${SERVICE_TESTS[@]}" "${MAIN_TESTS[@]}" "${TDD_RED_TESTS[@]}")
            total_files=$((${#MODEL_TESTS[@]} + ${#SERVICE_TESTS[@]} + ${#MAIN_TESTS[@]} + ${#TDD_RED_TESTS[@]}))
            log_info "${CYAN}执行所有测试（包含TDD测试）...${NC}"
            ;;
        "production"|*)
            tests_to_run=("${MODEL_TESTS[@]}" "${SERVICE_TESTS[@]}" "${MAIN_TESTS[@]}")
            log_info "${GREEN}执行生产验证测试...${NC}"
            ;;
    esac

    # 执行测试
    for test_entry in "${tests_to_run[@]}"; do
        IFS=':' read -r test_class test_name category <<< "$test_entry"
        ((test_index++))
        run_single_test "$test_class" "$test_name" $test_index ${#tests_to_run[@]} "$category"
    done

    log_success "测试套件执行完成"
}

# 生成Markdown报告
generate_markdown_report() {
    log_info "生成Markdown格式报告..."

    local md_report="$REPORTS_DIR/test_execution_report_${EXECUTION_ID}.md"
    local end_time=$(date +%s)
    local total_duration=$((end_time - START_TIME))
    local pass_rate=$(safe_divide $PASSED_TEST_CASES $((PASSED_TEST_CASES + FAILED_TEST_CASES)) 1)

    cat > "$md_report" << EOF
# 🧪 AEP数据导出工具测试执行报告

**执行ID**: \`$EXECUTION_ID\`
**执行时间**: $(date -r $START_TIME '+%Y-%m-%d %H:%M:%S') - $(date -r $end_time '+%H:%M:%S')
**执行环境**: $(uname -s) $(uname -r)
**Java版本**: $(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}')

## 📊 测试结果概览

| 指标 | 数值 | 百分比 |
|------|------|--------|
| **总测试用例** | $((PASSED_TEST_CASES + FAILED_TEST_CASES)) | 100% |
| **通过用例** | $PASSED_TEST_CASES | ${pass_rate}% |
| **失败用例** | $FAILED_TEST_CASES | $(safe_divide $FAILED_TEST_CASES $((PASSED_TEST_CASES + FAILED_TEST_CASES)) 1)% |
| **测试文件** | $((PASSED_FILES + FAILED_FILES)) | - |
| **执行时长** | ${total_duration}秒 | - |

## 📈 通过率分析

\`\`\`
通过率: ${pass_rate}%
$(printf '█%.0s' $(seq 1 $(( (${pass_rate%.*}+1)/2 ))))$(printf '░%.0s' $(seq 1 $(( 50-(${pass_rate%.*}+1)/2 ))))
\`\`\`

## 📋 详细测试结果

| 测试名称 | 类别 | 状态 | 用例数 | 耗时(秒) | 测试框架 | 错误信息 |
|----------|------|------|--------|----------|----------|----------|
EOF

    # 添加测试结果
    for result_file in "$TEST_RESULTS_DIR"/*_result_${EXECUTION_ID}.json; do
        if [[ -f "$result_file" ]]; then
            local test_name=$(grep '"testName"' "$result_file" | sed 's/.*"testName": "\([^"]*\)".*/\1/')
            local category=$(grep '"category"' "$result_file" | sed 's/.*"category": "\([^"]*\)".*/\1/')
            local status=$(grep '"status"' "$result_file" | sed 's/.*"status": "\([^"]*\)".*/\1/')
            local duration=$(grep '"duration"' "$result_file" | sed 's/.*"duration": \([0-9]*\).*/\1/')
            local test_cases=$(grep '"testCasesCount"' "$result_file" | sed 's/.*"testCasesCount": \([0-9]*\).*/\1/')
            local framework=$(grep '"testFramework"' "$result_file" | sed 's/.*"testFramework": "\([^"]*\)".*/\1/' || echo "Unknown")
            local error_msg=$(grep '"errorMessage"' "$result_file" | sed 's/.*"errorMessage": "\([^"]*\)".*/\1/' | cut -c1-40)

            local status_emoji="✅"
            local framework_emoji="🧪"
            if [[ "$status" == "FAIL" ]]; then
                status_emoji="❌"
            fi

            if [[ "$framework" == "JUnit" ]]; then
                framework_emoji="⚡"
            elif [[ "$framework" == "Custom TDD" ]]; then
                framework_emoji="🔧"
            fi

            echo "| $test_name | $category | $status_emoji $status | $test_cases | $duration | ${framework_emoji} ${framework} | \`${error_msg}\` |" >> "$md_report"
        fi
    done

    cat >> "$md_report" << EOF

## 🔍 日志文件位置

### 详细日志
- **主日志**: \`$MAIN_LOG\`
- **详细测试日志**: \`$DETAILED_LOGS_DIR/\`

### 使用建议
\`\`\`bash
# 查看失败测试的详细日志
find $DETAILED_LOGS_DIR -name "*${EXECUTION_ID}.log" -exec grep -l "Exception\|Error" {} \\;

# 重新运行特定测试
java -ea -cp "$CLASSPATH" [测试类名]
\`\`\`

---
**报告生成时间**: $(date)
**工具版本**: v2.1
EOF

    log_success "Markdown报告生成完成: $md_report"

    # 清理旧报告文件，只保留最近的3个
    cleanup_old_reports
}

# 清理旧报告文件，只保留指定数量的最新文件
cleanup_old_reports() {
    local max_files=${1:-3}  # 默认保留3个文件

    if [[ ! -d "$REPORTS_DIR" ]]; then
        return 0
    fi

    log_info "清理旧报告文件，保留最近${max_files}个..."

    # 清理test_execution_report文件
    if ls "$REPORTS_DIR"/test_execution_report_*.md >/dev/null 2>&1; then
        local report_count=$(ls -1 "$REPORTS_DIR"/test_execution_report_*.md | wc -l | tr -d ' ')
        if [[ $report_count -gt $max_files ]]; then
            local to_delete=$((report_count - max_files))
            ls -t "$REPORTS_DIR"/test_execution_report_*.md | tail -n +$((max_files + 1)) | xargs rm -f
            log_info "删除了 ${to_delete} 个旧的执行报告"
        fi
    fi

    # 清理failure_analysis文件
    if ls "$REPORTS_DIR"/failure_analysis_*.md >/dev/null 2>&1; then
        local failure_count=$(ls -1 "$REPORTS_DIR"/failure_analysis_*.md | wc -l | tr -d ' ')
        if [[ $failure_count -gt $max_files ]]; then
            local to_delete=$((failure_count - max_files))
            ls -t "$REPORTS_DIR"/failure_analysis_*.md | tail -n +$((max_files + 1)) | xargs rm -f
            log_info "删除了 ${to_delete} 个旧的失败分析报告"
        fi
    fi

    # 清理HTML报告目录中的旧文件
    if [[ -d "$REPORTS_DIR/html" ]] && ls "$REPORTS_DIR/html"/test_report_*.html >/dev/null 2>&1; then
        local html_count=$(ls -1 "$REPORTS_DIR/html"/test_report_*.html | wc -l | tr -d ' ')
        if [[ $html_count -gt $max_files ]]; then
            local to_delete=$((html_count - max_files))
            ls -t "$REPORTS_DIR/html"/test_report_*.html | tail -n +$((max_files + 1)) | xargs rm -f
            log_info "删除了 ${to_delete} 个旧的HTML报告"
        fi
    fi

    # 清理JSON报告目录中的旧文件
    if [[ -d "$REPORTS_DIR/json" ]] && ls "$REPORTS_DIR/json"/test_report_*.json >/dev/null 2>&1; then
        local json_count=$(ls -1 "$REPORTS_DIR/json"/test_report_*.json | wc -l | tr -d ' ')
        if [[ $json_count -gt $max_files ]]; then
            local to_delete=$((json_count - max_files))
            ls -t "$REPORTS_DIR/json"/test_report_*.json | tail -n +$((max_files + 1)) | xargs rm -f
            log_info "删除了 ${to_delete} 个旧的JSON报告"
        fi
    fi

    # 清理CSV报告目录中的旧文件
    if [[ -d "$REPORTS_DIR/csv" ]] && ls "$REPORTS_DIR/csv"/test_results_*.csv >/dev/null 2>&1; then
        local csv_count=$(ls -1 "$REPORTS_DIR/csv"/test_results_*.csv | wc -l | tr -d ' ')
        if [[ $csv_count -gt $max_files ]]; then
            local to_delete=$((csv_count - max_files))
            ls -t "$REPORTS_DIR/csv"/test_results_*.csv | tail -n +$((max_files + 1)) | xargs rm -f
            log_info "删除了 ${to_delete} 个旧的CSV报告"
        fi
    fi

    # 清理测试执行日志文件
    if ls "$LOGS_DIR"/test_execution_*.log >/dev/null 2>&1; then
        local log_count=$(ls -1 "$LOGS_DIR"/test_execution_*.log | wc -l | tr -d ' ')
        if [[ $log_count -gt $max_files ]]; then
            local to_delete=$((log_count - max_files))
            ls -t "$LOGS_DIR"/test_execution_*.log | tail -n +$((max_files + 1)) | xargs rm -f
            log_info "删除了 ${to_delete} 个旧的测试执行日志"
        fi
    fi

    # 清理详细测试结果日志目录中的旧文件
    if [[ -d "$DETAILED_LOGS_DIR" ]]; then
        # 按执行ID分组清理，保留最新的3个执行批次的所有详细日志
        local execution_ids=($(find "$DETAILED_LOGS_DIR" -name "*_*.log" -exec basename {} \; | sed 's/.*_\([0-9]*_[0-9]*\)\.log/\1/' | sort -u | sort -r))
        if [[ ${#execution_ids[@]} -gt $max_files ]]; then
            local ids_to_delete=("${execution_ids[@]:$max_files}")
            for id in "${ids_to_delete[@]}"; do
                find "$DETAILED_LOGS_DIR" -name "*_${id}.log" -delete
            done
            log_info "删除了 ${#ids_to_delete[@]} 个旧批次的详细测试日志"
        fi
    fi

    # 清理测试结果JSON文件目录中的旧文件
    if [[ -d "$TEST_RESULTS_DIR" ]]; then
        # 按执行ID分组清理，保留最新的3个执行批次的所有结果文件
        local result_execution_ids=($(find "$TEST_RESULTS_DIR" -name "*_result_*.json" -exec basename {} \; | sed 's/.*_result_\([0-9]*_[0-9]*\)\.json/\1/' | sort -u | sort -r))
        if [[ ${#result_execution_ids[@]} -gt $max_files ]]; then
            local result_ids_to_delete=("${result_execution_ids[@]:$max_files}")
            for id in "${result_ids_to_delete[@]}"; do
                find "$TEST_RESULTS_DIR" -name "*_result_${id}.json" -delete
            done
            log_info "删除了 ${#result_ids_to_delete[@]} 个旧批次的测试结果文件"
        fi
    fi

    log_success "报告清理完成，每类报告和日志保留最新${max_files}个文件/批次"
}

# 显示帮助信息
show_help() {
    cat << EOF
AEP数据导出工具 - 测试执行和报告生成脚本 v2.1

用法: $0 [选项]

选项:
    -h, --help              显示此帮助信息
    -v, --verbose          显示详细执行信息
    -c, --compile-only     仅编译代码，不运行测试
    -s, --skip-reports     跳过报告生成，仅运行测试
    -f, --format FORMAT    指定报告格式 (md,all) 默认:md
    --model-only           仅运行Model层测试
    --service-only         仅运行Service层测试
    --main-only            仅运行Main层测试
    --tdd-red              仅运行TDD RED阶段测试（预期失败）
    --all-with-tdd         运行所有测试（包含TDD测试）

环境变量:
    VERBOSE_TEST_COUNT=true  显示测试用例计数详情 (默认关闭)

示例:
    $0                     # 运行生产测试（默认，153个用例）
    $0 --model-only        # 仅运行Model层测试
    $0 --tdd-red           # 仅运行TDD RED测试（9个预期失败）
    $0 --all-with-tdd      # 运行所有测试（162个用例，包含TDD）
    $0 --skip-reports      # 运行测试但不生成报告

日志位置:
    主日志: logs/test_execution_\${TIMESTAMP}.log
    详细日志: logs/detailed-test-results/
    报告: reports/
EOF
}

# 主程序入口
main() {
    # 解析命令行参数
    VERBOSE=false
    COMPILE_ONLY=false
    SKIP_REPORTS=false
    REPORT_FORMAT="md"
    TEST_FILTER="all"

    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -c|--compile-only)
                COMPILE_ONLY=true
                shift
                ;;
            -s|--skip-reports)
                SKIP_REPORTS=true
                shift
                ;;
            -f|--format)
                REPORT_FORMAT="$2"
                shift 2
                ;;
            --model-only)
                TEST_FILTER="model"
                shift
                ;;
            --service-only)
                TEST_FILTER="service"
                shift
                ;;
            --main-only)
                TEST_FILTER="main"
                shift
                ;;
            --tdd-red)
                TEST_FILTER="tdd-red"
                shift
                ;;
            --all-with-tdd)
                TEST_FILTER="all-with-tdd"
                shift
                ;;
            *)
                log_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done

    # 设置主日志文件
    MAIN_LOG="$LOGS_DIR/test_execution_${EXECUTION_ID}.log"

    # 开始执行
    echo -e "${BLUE}============================================================${NC}"
    echo -e "${BLUE}   AEP数据导出工具 - 综合测试执行器 v2.1${NC}"
    echo -e "${BLUE}============================================================${NC}"
    echo -e "🕒 开始时间: $(date)"
    echo -e "📋 执行ID: $EXECUTION_ID"
    echo -e "📁 工作目录: $PROJECT_ROOT"
    echo ""

    # 创建目录和检查环境
    create_directories
    check_environment

    # 记录开始信息到日志
    echo "AEP数据导出工具测试执行日志 - 执行ID: $EXECUTION_ID" > "$MAIN_LOG"

    if [[ "$COMPILE_ONLY" == "true" ]]; then
        log_info "仅编译模式，编译完成后退出"
        exit 0
    fi

    # 执行测试
    log_info "开始执行测试套件..."
    run_test_suite

    # 生成报告
    if [[ "$SKIP_REPORTS" == "false" ]]; then
        case "$REPORT_FORMAT" in
            "md"|*)
                generate_markdown_report
                ;;
        esac
    fi

    # 执行完成总结
    local end_time=$(date +%s)
    local total_duration=$((end_time - START_TIME))
    local pass_rate=$(safe_divide $PASSED_TEST_CASES $((PASSED_TEST_CASES + FAILED_TEST_CASES)) 1)

    echo ""
    echo -e "${BLUE}============================================================${NC}"
    echo -e "${GREEN}🎉 测试执行完成${NC}"
    echo -e "${BLUE}============================================================${NC}"
    echo -e "⏱️  总执行时间: ${total_duration}秒"
    echo -e "📊 测试结果:"
    echo -e "   📁 测试文件: $((PASSED_FILES + FAILED_FILES))个"
    echo -e "   📋 测试用例: $((PASSED_TEST_CASES + FAILED_TEST_CASES))个"
    echo -e "   ✅ 通过用例: $PASSED_TEST_CASES个"
    echo -e "   ❌ 失败用例: $FAILED_TEST_CASES个"
    echo -e "   📈 通过率: ${pass_rate}%"
    echo ""
    echo -e "📁 结果文件:"
    echo -e "   📄 主日志: $MAIN_LOG"
    if [[ "$SKIP_REPORTS" == "false" ]]; then
        echo -e "   📝 Markdown: $REPORTS_DIR/test_execution_report_${EXECUTION_ID}.md"
    fi
    echo ""

    if [[ $FAILED_TEST_CASES -gt 0 ]]; then
        log_warning "有 $FAILED_TEST_CASES 个测试用例失败，请查看详细日志进行分析"
        exit 1
    else
        log_success "所有测试用例通过！"
        exit 0
    fi
}

# 信号处理
trap 'log_error "测试执行被中断"; exit 1' INT TERM

# 执行主程序
main "$@"
