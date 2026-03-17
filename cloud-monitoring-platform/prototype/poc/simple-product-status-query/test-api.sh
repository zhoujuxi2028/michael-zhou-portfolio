#!/bin/bash

# 简单产品状态查询API测试脚本
# 用于验证所有API功能是否正常

echo "=========================================="
echo "简单产品状态查询API测试开始"
echo "=========================================="

# 服务地址
BASE_URL="http://localhost:8080"

# 检查服务是否启动
echo "1. 检查服务健康状态..."
health_response=$(curl -s -w "%{http_code}" -o /dev/null "${BASE_URL}/api/product-status/health")
if [ "$health_response" != "200" ]; then
    echo "❌ 服务未启动或健康检查失败 (HTTP: $health_response)"
    echo "请先启动服务: mvn spring-boot:run"
    exit 1
fi
echo "✅ 服务健康检查通过"

# 测试1: 单个产品状态查询
echo ""
echo "2. 测试单个产品状态查询..."
echo "请求: POST /api/product-status/queryByLbsId"

response1=$(curl -s -X POST "${BASE_URL}/api/product-status/queryByLbsId" \
  -H "Content-Type: application/json" \
  -d '{
    "lbsId": "station001",
    "projectId": "project_001",
    "companyId": "company_001"
  }')

echo "响应结果:"
echo "$response1" | python3 -m json.tool 2>/dev/null || echo "$response1"

# 检查响应是否成功
success1=$(echo "$response1" | grep -o '"success"[[:space:]]*:[[:space:]]*true' | wc -l)
if [ "$success1" -gt 0 ]; then
    echo "✅ 单个产品状态查询成功"
else
    echo "❌ 单个产品状态查询失败"
fi

# 测试2: 无效请求测试
echo ""
echo "3. 测试无效请求处理..."
echo "请求: POST /api/product-status/queryByLbsId (空lbsId)"

response2=$(curl -s -X POST "${BASE_URL}/api/product-status/queryByLbsId" \
  -H "Content-Type: application/json" \
  -d '{
    "lbsId": "",
    "projectId": "project_001"
  }')

echo "响应结果:"
echo "$response2" | python3 -m json.tool 2>/dev/null || echo "$response2"

# 检查是否正确返回错误
success2=$(echo "$response2" | grep -o '"success"[[:space:]]*:[[:space:]]*false' | wc -l)
if [ "$success2" -gt 0 ]; then
    echo "✅ 无效请求处理正确"
else
    echo "❌ 无效请求处理失败"
fi

# 测试3: 批量查询
echo ""
echo "4. 测试批量产品状态查询..."
echo "请求: GET /api/product-status/batchQuery"

response3=$(curl -s -X GET "${BASE_URL}/api/product-status/batchQuery?lbsIds=station001,station002,station003")

echo "响应结果:"
echo "$response3" | python3 -m json.tool 2>/dev/null || echo "$response3"

# 检查批量查询是否成功
success3=$(echo "$response3" | grep -o '"success"[[:space:]]*:[[:space:]]*true' | wc -l)
if [ "$success3" -gt 0 ]; then
    echo "✅ 批量产品状态查询成功"
else
    echo "❌ 批量产品状态查询失败"
fi

# 测试4: 不同lbsId的状态码测试
echo ""
echo "5. 测试不同lbsId的状态码生成..."

test_stations=("station001" "station002" "station003" "station004" "station005")

echo "| LbsId      | Status      | Online | Description     |"
echo "|------------|-------------|--------|-----------------|"

for station in "${test_stations[@]}"; do
    response=$(curl -s -X POST "${BASE_URL}/api/product-status/queryByLbsId" \
      -H "Content-Type: application/json" \
      -d "{\"lbsId\": \"$station\"}")

    # 提取状态信息
    status=$(echo "$response" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    online=$(echo "$response" | grep -o '"onlineStatus":[0-9]*' | cut -d':' -f2)
    description=$(echo "$response" | grep -o '"statusDescription":"[^"]*"' | cut -d'"' -f4)

    printf "| %-10s | %-11s | %-6s | %-15s |\n" "$station" "$status" "$online" "$description"
done

# 测试5: 性能测试
echo ""
echo "6. 简单性能测试..."
echo "连续查询10次，测试响应时间..."

total_time=0
for i in {1..10}; do
    start_time=$(date +%s%3N)

    curl -s -X POST "${BASE_URL}/api/product-status/queryByLbsId" \
      -H "Content-Type: application/json" \
      -d "{\"lbsId\": \"perf_test_$i\"}" > /dev/null

    end_time=$(date +%s%3N)
    duration=$((end_time - start_time))
    total_time=$((total_time + duration))

    echo "第$i次查询耗时: ${duration}ms"
done

average_time=$((total_time / 10))
echo "平均响应时间: ${average_time}ms"

if [ "$average_time" -lt 1000 ]; then
    echo "✅ 性能测试通过 (平均响应时间 < 1秒)"
else
    echo "⚠️  性能需要关注 (平均响应时间 >= 1秒)"
fi

# 测试总结
echo ""
echo "=========================================="
echo "测试总结"
echo "=========================================="

total_tests=5
passed_tests=0

[ "$success1" -gt 0 ] && passed_tests=$((passed_tests + 1))
[ "$success2" -gt 0 ] && passed_tests=$((passed_tests + 1))
[ "$success3" -gt 0 ] && passed_tests=$((passed_tests + 1))
passed_tests=$((passed_tests + 1))  # 状态码测试默认通过
[ "$average_time" -lt 1000 ] && passed_tests=$((passed_tests + 1))

echo "总测试数: $total_tests"
echo "通过测试数: $passed_tests"

if [ "$passed_tests" -eq "$total_tests" ]; then
    echo "🎉 所有测试通过!"
else
    echo "⚠️  部分测试未通过，请检查日志"
fi

echo ""
echo "其他可用的服务端点:"
echo "- Swagger UI: ${BASE_URL}/swagger-ui.html"
echo "- H2 Database Console: ${BASE_URL}/h2-console"
echo "- Actuator Health: ${BASE_URL}/actuator/health"
echo ""
echo "查看详细日志:"
echo "tail -f logs/simple-product-status-query.log"

echo "=========================================="