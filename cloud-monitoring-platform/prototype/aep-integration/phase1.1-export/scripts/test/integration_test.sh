#!/bin/bash

# IMP-005: 集成测试脚本 - 覆盖完整的导出流程
# 测试query.sh的各种使用场景，确保功能正常

set -e

# 测试配置
TEST_START_TIME=$(date "+%Y%m%d_%H%M%S")
TEST_REPORT_DIR="reports/integration_test_${TEST_START_TIME}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# 进入项目根目录
cd "$PROJECT_ROOT"

echo "🧪 === AEP数据导出工具集成测试 ==="
echo "测试时间: $(date)"
echo "项目根目录: $PROJECT_ROOT"
echo "测试报告: $TEST_REPORT_DIR"
echo ""

# 创建测试报告目录
mkdir -p "$TEST_REPORT_DIR"

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
run_test() {
    local test_name="$1"
    local test_command="$2"
    local expected_files="$3"

    echo "📋 测试: $test_name"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    # 清理之前的输出
    rm -f output/*.csv output/*.json 2>/dev/null || true

    # 执行测试命令
    echo "   命令: $test_command"
    if eval "$test_command" > "$TEST_REPORT_DIR/${test_name}.log" 2>&1; then
        # 检查预期文件是否存在
        local files_exist=true
        for file in $expected_files; do
            if [ ! -f "$file" ]; then
                echo "   ❌ 失败: 预期文件 $file 不存在"
                files_exist=false
                break
            fi
        done

        if $files_exist; then
            echo "   ✅ 通过"
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            echo "   ❌ 失败: 输出文件缺失"
            FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
    else
        echo "   ❌ 失败: 命令执行错误"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo ""
}

# 测试用例1: 默认模式（最重要）
echo "🎯 === 核心功能测试 ==="
run_test "01_default_mode" "./query.sh" "output/products.csv output/devices.csv"

# 测试用例2: 明确参数CSV格式
run_test "02_explicit_csv" "./query.sh --export-all --format csv" "output/products.csv output/devices.csv"

# 测试用例3: JSON格式导出
run_test "03_json_format" "./query.sh --export-all --format json" "output/products.json output/devices.json"

# 测试用例4: 仅导出产品
run_test "04_products_only" "./query.sh --export-products --format csv" "output/products.csv"

echo "🔍 === 边界条件测试 ==="

# 测试用例5: 帮助信息
run_test "05_help_display" "./query.sh --help" ""

# 测试用例6: 版本信息
run_test "06_version_display" "./query.sh --version" ""

echo "⚡ === 性能和稳定性测试 ==="

# 测试用例7: 连续执行（稳定性）
echo "📋 测试: 07_consecutive_runs"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
consecutive_success=true

for i in {1..3}; do
    echo "   第${i}次执行..."
    rm -f output/*.csv 2>/dev/null || true
    if ./query.sh > "$TEST_REPORT_DIR/07_consecutive_${i}.log" 2>&1; then
        if [ -f "output/products.csv" ] && [ -f "output/devices.csv" ]; then
            echo "   ✅ 第${i}次成功"
        else
            echo "   ❌ 第${i}次失败: 输出文件缺失"
            consecutive_success=false
            break
        fi
    else
        echo "   ❌ 第${i}次失败: 命令执行错误"
        consecutive_success=false
        break
    fi
done

if $consecutive_success; then
    echo "   ✅ 通过: 连续执行稳定"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo "   ❌ 失败: 连续执行不稳定"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

echo "📊 === 数据验证测试 ==="

# 测试用例8: 数据完整性验证
echo "📋 测试: 08_data_integrity"
TOTAL_TESTS=$((TOTAL_TESTS + 1))

./query.sh > "$TEST_REPORT_DIR/08_data_integrity.log" 2>&1

if [ -f "output/products.csv" ] && [ -f "output/devices.csv" ]; then
    # 检查产品文件
    product_lines=$(wc -l < output/products.csv)
    device_lines=$(wc -l < output/devices.csv)

    # 验证文件不为空且有头部
    if [ "$product_lines" -ge 2 ] && [ "$device_lines" -ge 2 ]; then
        echo "   ✅ 通过: 数据完整性验证"
        echo "   📊 产品记录: $((product_lines - 1)) 行"
        echo "   📊 设备记录: $((device_lines - 1)) 行"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo "   ❌ 失败: 数据文件为空或缺少头部"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
else
    echo "   ❌ 失败: 输出文件不存在"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

echo "🏁 === 测试总结 ==="
echo "📊 测试统计:"
echo "   总测试数: $TOTAL_TESTS"
echo "   通过: $PASSED_TESTS"
echo "   失败: $FAILED_TESTS"
echo "   成功率: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%"
echo ""

# 生成测试报告
cat > "$TEST_REPORT_DIR/summary.md" << EOF
# AEP数据导出工具 - 集成测试报告

**测试时间**: $(date)
**测试版本**: v1.1.0
**测试环境**: $(uname -s) $(uname -r)

## 📊 测试结果统计

| 指标 | 数值 |
|------|------|
| 总测试数 | $TOTAL_TESTS |
| 通过测试 | $PASSED_TESTS |
| 失败测试 | $FAILED_TESTS |
| 成功率 | $(( PASSED_TESTS * 100 / TOTAL_TESTS ))% |

## 🧪 测试用例列表

1. **默认模式测试** - 验证无参数默认行为
2. **明确参数CSV** - 验证显式CSV格式导出
3. **JSON格式测试** - 验证JSON格式导出
4. **产品导出测试** - 验证仅导出产品功能
5. **帮助信息测试** - 验证--help参数
6. **版本信息测试** - 验证--version参数
7. **连续执行测试** - 验证工具稳定性
8. **数据完整性测试** - 验证导出数据质量

## 📁 测试文件

详细日志请查看:
- \`01_default_mode.log\` - 默认模式执行日志
- \`02_explicit_csv.log\` - CSV格式测试日志
- \`03_json_format.log\` - JSON格式测试日志
- \`04_products_only.log\` - 产品导出测试日志
- \`05_help_display.log\` - 帮助信息日志
- \`06_version_display.log\` - 版本信息日志
- \`07_consecutive_*.log\` - 连续执行测试日志
- \`08_data_integrity.log\` - 数据完整性测试日志

## 🎯 测试结论

$(if [ $FAILED_TESTS -eq 0 ]; then
    echo "✅ **所有测试通过** - 工具功能正常，可以安全使用"
else
    echo "⚠️ **存在失败测试** - 请检查失败的测试用例，修复问题后重新测试"
fi)

---
*自动生成于: $(date)*
EOF

echo "📁 测试报告已保存到: $TEST_REPORT_DIR/"
echo ""

# 根据测试结果返回退出代码
if [ $FAILED_TESTS -eq 0 ]; then
    echo "🎉 所有测试通过！工具功能正常。"
    exit 0
else
    echo "⚠️  有 $FAILED_TESTS 个测试失败，请检查详细日志。"
    exit 1
fi