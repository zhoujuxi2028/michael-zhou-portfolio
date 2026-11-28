# Bug 列表与回归测试

## 🐛 Bug #1: Newman HTMLExtra 报告缺失

### 问题
```bash
# 运行时显示警告
newman: could not find 'htmlextra' reporter
# 结果：没有生成 newman/api-report.html
```

### 原因
官方镜像 `postman/newman:6-alpine` 没有安装 htmlextra reporter

### 修复
创建 `Dockerfile.newman`:
```dockerfile
FROM postman/newman:6-alpine
RUN npm install -g newman-reporter-htmlextra
```

修改 `docker-compose.yml`:
```yaml
newman:
  build:
    context: .
    dockerfile: Dockerfile.newman
```

### 回归测试
```bash
cd test-project

# 1. 构建镜像
docker compose build newman

# 2. 清理旧报告
rm -f newman/api-report.html

# 3. 运行测试
docker compose up newman

# 4. 验证
ls -lh newman/api-report.html
# ✅ 应该存在，大小 > 200KB
```

**验证点**:
- [ ] 没有 "could not find" 警告
- [ ] 文件存在: `newman/api-report.html`
- [ ] 文件大小 > 200KB
- [ ] 可以在浏览器打开

---

## 🐛 Bug #2: Cypress 测试被中断

### 问题
```bash
# 使用旧命令
docker compose up --abort-on-container-exit

# 结果：
# - 只跑了 01-api-tests.cy.js (7个测试)
# - 02-ui-tests.cy.js 没运行 (9个测试)
# - Cypress exit code: 137 (被杀)
# - 只有1个视频文件
```

### 原因
- Newman 6.2秒完成退出
- `--abort-on-container-exit` 杀掉还在运行的 Cypress（需要14秒）
- Cypress 被 SIGKILL 中断

### 修复
使用后台运行 + 等待所有容器完成：
```bash
# 旧命令（有问题 - 会提前中断）
docker compose up --abort-on-container-exit

# 方法1：后台运行 + 等待完成（推荐）
docker compose up -d
docker compose wait cypress newman

# 方法2：使用npm脚本
npm run docker:test:detached
```

更新 `package.json`:
```json
"docker:test:detached": "docker compose up -d && docker compose wait cypress newman"
```

**为什么这样解决**:
- `-d` 后台运行，不会在任何容器退出时自动停止
- `wait` 命令等待指定的容器全部完成
- Newman (6秒) 和 Cypress (15秒) 都能完整执行

### 回归测试

#### 方法1: 使用带日志的测试脚本（推荐）
```bash
cd test-project

# 运行完整回归测试（生成详细日志）
./run-regression-test-with-logs.sh

# 查看测试总结
cat test-logs/test-summary-*.txt

# 查看详细日志
ls -lh test-logs/
```

#### 方法2: 手动测试
```bash
cd test-project

# 1. 清理旧数据
rm -rf cypress/videos/*

# 2. 后台运行测试
docker compose up -d

# 3. 等待完成
docker compose wait cypress newman

# 4. 检查视频数量
ls cypress/videos/*.mp4 | wc -l
# ✅ 应该是 2

# 5. 检查测试数量
docker compose logs cypress | grep "All specs"
# ✅ 应该显示 16 passing

# 6. 清理
docker compose down
```

#### 方法3: 使用npm脚本
```bash
npm run docker:test        # 运行测试
npm run docker:test:logs   # 运行测试并生成日志
```

**验证点**:
- [ ] Newman 完成后 Cypress 继续运行（不被杀）
- [ ] Cypress exit code = 0（不是137）
- [ ] 生成2个视频文件
- [ ] 执行16个测试（不是7个）

---

## 🐛 Bug #3: 回归测试脚本日志解析错误

### 问题
```bash
# 运行测试脚本时显示错误的统计数据
[2026-02-21 14:15:25] ⚠️  Cypress: 39/16 测试执行（预期 16）
./run-regression-test-with-logs.sh: line 183: [: assertions: integer expression expected
[2026-02-21 14:15:25] ⚠️  Newman: assertions 断言（预期 18）
```

### 原因
- **Cypress**: 正则表达式 `grep -o "[0-9]*$"` 提取了错误的数字
- **Newman**: `awk '{print $2}'` 提取了字段名 "assertions" 而不是数值 "18"
- **Shell 错误**: 在整数比较中使用了非整数字符串

### 修复
修改 `run-regression-test-with-logs.sh` (line 172-189):

```bash
# 提取 Cypress 测试数量（使用 awk 精确匹配）
CYPRESS_TESTS=$(docker compose logs cypress 2>/dev/null | \
    grep "All specs passed" | \
    awk '{for(i=1;i<=NF;i++) if($i~/^[0-9]+$/ && $(i+1)~/^[0-9]+$/) {print $(i+1); exit}}' | \
    head -1)

# 提取 Newman 断言数量（使用 Perl regex lookbehind）
NEWMAN_ASSERTIONS=$(docker compose logs newman 2>/dev/null | \
    grep "assertions" | \
    grep -oP 'assertions\s*│\s*\K\d+' | \
    head -1)

# 验证并设置默认值
CYPRESS_TESTS=${CYPRESS_TESTS:-0}
NEWMAN_ASSERTIONS=${NEWMAN_ASSERTIONS:-0}

# 确保是整数
if ! [[ "$CYPRESS_TESTS" =~ ^[0-9]+$ ]]; then
    CYPRESS_TESTS=0
fi

if ! [[ "$NEWMAN_ASSERTIONS" =~ ^[0-9]+$ ]]; then
    NEWMAN_ASSERTIONS=0
fi
```

### 回归测试
```bash
cd test-project

# 运行修复后的脚本
./run-regression-test-with-logs.sh

# 验证结果
cat test-logs/test-summary-*.txt | grep -E "Cypress:|Newman:"
# ✅ 应该显示: Cypress: 16/16
# ✅ 应该显示: Newman 断言: 18
```

**验证点**:
- [ ] 无 "39/16" 错误
- [ ] 无 "assertions" 字符串错误
- [ ] 无 "integer expression expected" 错误
- [ ] 正确显示 "Cypress: 16/16 测试通过"
- [ ] 正确显示 "Newman: 18 断言通过"

---

## 🐛 Bug #4: Deploy Pipeline 冒烟测试 `newman: command not found`

### 问题
```text
DVP / Staging Smoke Test → Run Newman smoke test
line 3: newman: command not found
##[error]Process completed with exit code 127.
```
main 上每次 `DVP / Deploy Pipeline` 都失败在此步（2026-11-19 起最近 8 次全部失败），staging 冒烟测试从未真正执行。

### 原因
| 项 | 说明 |
|----|------|
| 引入 | `42938b6`（REQ-DVP-001 迁移，#44） |
| 根因 | `Install Newman` 步骤为 `npm ci`，newman 只装到 `node_modules/.bin`（不在 `PATH`）；下一步直接调用 `newman` |
| 为何未被发现 | `dvp-deploy.yml` 仅在 push 到 main / 手动触发时运行，PR 门禁不跑；本机有全局 newman，直接调用可用 |

### 修复
`.github/workflows/dvp-deploy.yml`：
```diff
-          newman run postman/api-collection.json \
+          npx --no-install newman run postman/api-collection.json \
```

### 回归测试
```bash
# 1. 本地复现（去掉全局 newman，仅保留 node/npm/npx）
cd devops-platform-demo
env -i HOME="$HOME" PATH="<node-only-dir>:/usr/bin:/bin" bash -c 'newman --version'   # 期望 exit 127
env -i HOME="$HOME" PATH="<node-only-dir>:/usr/bin:/bin" bash -c \
  'npx --no-install newman run postman/api-collection.json -e postman/environment.json -r cli'  # 期望 0 failures

# 2. 合并前：修复分支手动触发（staging/production 均为 dry-run，不部署）
gh workflow run dvp-deploy.yml --ref fix/dvp-deploy-newman-smoke -f skip_production=true
gh run watch   # 期望 DVP / Staging Smoke Test ✅

# 3. 合并后：main 上 push 触发的 DVP / Deploy Pipeline 全绿
gh run list --workflow dvp-deploy.yml --branch main --limit 1
```

> **⚠️ 为何不能用文档 PR（如 #91）回归**：PR 的 CI 不运行 `dvp-deploy.yml`，PR 绿灯不能证明此 bug 已修复。

---

## ✅ 完整回归测试（一键执行）

```bash
#!/bin/bash
cd test-project

echo "=== 清理环境 ==="
rm -rf newman/api-report.html cypress/videos/*
docker compose down -v

echo ""
echo "=== Bug #1: 构建镜像 ==="
docker compose build newman

echo ""
echo "=== 运行所有测试 ==="
docker compose up --exit-code-from cypress

echo ""
echo "=== 验证结果 ==="
echo "Newman报告: $(ls -lh newman/api-report.html 2>/dev/null || echo '❌ 不存在')"
echo "视频数量: $(ls cypress/videos/*.mp4 2>/dev/null | wc -l)"
echo "退出码: $?"

# 快速检查
if [ -f newman/api-report.html ] && [ $(ls cypress/videos/*.mp4 2>/dev/null | wc -l) -eq 2 ]; then
  echo ""
  echo "✅ 所有Bug已修复！"
else
  echo ""
  echo "❌ 仍有问题，请检查上面的输出"
fi
```

**保存为**: `test-project/run-regression-test.sh`

**使用**:
```bash
chmod +x run-regression-test.sh
./run-regression-test.sh
```

---

## 📊 测试结果记录

| Bug | 描述 | 状态 | 备注 |
|-----|------|------|------|
| #1 | Newman HTMLExtra 缺失 | ✅ 通过 | 报告生成 235 KB（`newman/api-report.html`）；镜像断言 htmlextra 1.23.1 |
| #2 | Cypress 测试中断 | ✅ 通过 | 2 个 spec 均执行；CI E2E（Cypress 16.3.0）通过 |
| #3 | 回归脚本日志解析错误 | ✅ 通过 | 无 `39/16` 错误；当前套件 Cypress 17/17、Newman 18 断言 |
| #4 | Deploy 冒烟测试 newman 未找到 | ✅ 通过 | 本地 ✅；分支手动触发 run 45739365526 ✅（7 请求 / 18 断言 / 0 失败）；main push run 45749279264 ✅（#92 合并后全部 stage 通过；#91 先合并的 run 45749250835 仍失败，符合预期） |

**测试日期**: 2026-11-28
**测试人**: Michael Zhou

---

## 🎯 5分钟快速验证

```bash
cd test-project
docker compose build newman
rm -rf newman/api-report.html cypress/videos/*
docker compose up --exit-code-from cypress
ls -lh newman/api-report.html
ls cypress/videos/
```

**期望**:
- ✅ `newman/api-report.html` 存在
- ✅ `cypress/videos/` 有2个 .mp4 文件
- ✅ 退出码 = 0
