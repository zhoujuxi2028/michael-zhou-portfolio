# AEP Integration 数据库部署指南

## 📋 文档信息
- **版本**: v1.0
- **创建日期**: 2026-01-25
- **更新日期**: 2026-01-25
- **适用环境**: 本地开发、测试、生产环境
- **状态**: 就绪

## 🎯 快速部署

### 前置条件
- MySQL 8.0+ 服务器 (**推荐 MySQL 9.6** - 支持最新优化特性)
- MySQL 客户端工具
- 网络连通性

### 🆕 MySQL 9.6 特别说明
本项目已针对 MySQL 9.6 进行特别优化，包括：
- **utf8mb4_0900_ai_ci** 字符集优化
- **JSON 功能增强** - 支持更高效的JSON索引和查询
- **微秒精度时间戳** - 提供更精确的操作监控
- **窗口函数** - 支持高级分析查询

详细特性说明请参考：`docs/MYSQL_96_FEATURES.md`

### 一键部署
```bash
# 1. 克隆或确认项目目录
cd /Users/michael_zhou/Documents/ZCT/github/Cloud-Monitoring-Platform/protoType/aep-integration/phase2-registration

# 2. 配置数据库连接（编辑 .env.database）
vim .env.database

# 3. 执行数据库初始化
./scripts/setup_database.sh development

# 4. 验证部署结果
./scripts/test_database.sh
```

## 🛠️ 详细部署步骤

### 1. MySQL 安装

#### macOS (Homebrew)
```bash
# 安装MySQL
brew install mysql

# 启动MySQL服务
brew services start mysql

# 安全配置
mysql_secure_installation

# 设置root密码
mysql -u root -p
ALTER USER 'root'@'localhost' IDENTIFIED BY 'your_root_password';
```

#### Ubuntu/Debian
```bash
# 更新包列表
sudo apt update

# 安装MySQL
sudo apt install mysql-server

# 启动服务
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

#### CentOS/RHEL
```bash
# 安装MySQL仓库
sudo yum install mysql-community-release

# 安装MySQL
sudo yum install mysql-community-server

# 启动服务
sudo systemctl start mysqld
sudo systemctl enable mysqld

# 获取临时密码
sudo grep 'temporary password' /var/log/mysqld.log
```

#### Windows
1. 下载MySQL安装包：https://dev.mysql.com/downloads/mysql/
2. 运行安装程序
3. 选择"Full"安装类型
4. 设置root密码
5. 启动MySQL服务

#### Docker 部署
```bash
# 创建MySQL容器
docker run --name aep-mysql \
  -e MYSQL_ROOT_PASSWORD=root_password \
  -e MYSQL_DATABASE=aep_integration_db \
  -e MYSQL_USER=aep_user \
  -e MYSQL_PASSWORD=aep_password_2026 \
  -p 3306:3306 \
  -d mysql:8.0

# 检查容器状态
docker ps | grep aep-mysql

# 连接测试
docker exec -it aep-mysql mysql -u root -p
```

### 2. 环境配置

#### 本地开发环境
```bash
# 已创建的配置文件内容
cat .env.database
```

配置说明：
- `DB_HOST=localhost` - 本地MySQL服务器
- `DB_NAME=aep_integration_db` - 数据库名称
- `DB_USERNAME=aep_user` - 应用数据库用户
- `DB_PASSWORD=aep_password_2026` - 应用用户密码
- `DB_ADMIN_USER=root` - 管理员用户（仅初始化时使用）

#### 远程服务器环境
```bash
# 复制模板并编辑
cp .env.database.template .env.database.remote
vim .env.database.remote

# 关键配置项
DB_HOST=your-mysql-server.com
DB_PORT=3306
DB_USE_SSL=true
DB_SSL_MODE=REQUIRED
```

### 3. 数据库初始化

#### 执行初始化脚本
```bash
# 开发环境
./scripts/setup_database.sh development

# 测试环境
./scripts/setup_database.sh test

# 生产环境
./scripts/setup_database.sh production
```

#### 脚本执行过程
1. **检查必要文件** - 验证架构文件和配置存在
2. **加载环境配置** - 读取 .env.database 文件
3. **测试数据库连接** - 验证管理员账户连接
4. **创建数据库和用户** - 自动创建数据库及应用用户
5. **执行架构脚本** - 创建表、索引、视图、存储过程
6. **验证设置** - 检查表创建结果
7. **应用环境配置** - 根据环境应用特定配置

#### 手动执行（如脚本失败）
```bash
# 1. 连接MySQL管理员账户
mysql -u root -p

# 2. 创建数据库
CREATE DATABASE aep_integration_db CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;

# 3. 创建用户
CREATE USER 'aep_user'@'%' IDENTIFIED BY 'aep_password_2026';
CREATE USER 'aep_user'@'localhost' IDENTIFIED BY 'aep_password_2026';

# 4. 授权
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER ON aep_integration_db.* TO 'aep_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER ON aep_integration_db.* TO 'aep_user'@'localhost';
FLUSH PRIVILEGES;

# 5. 退出管理员会话
EXIT;

# 6. 导入架构
mysql -u aep_user -p aep_integration_db < db/schema_v2.sql
```

### 4. 验证部署

#### 自动化测试
```bash
# 运行测试脚本
./scripts/test_database.sh

# 预期输出示例：
# 测试数据库连接...
# 主机: localhost:3306
# 数据库: aep_integration_db
# 用户: aep_user
# ✓ 数据库连接成功
#
# 数据库统计:
# +----------+-----------+---------+
# | products | platforms | configs |
# +----------+-----------+---------+
# |        0 |         2 |       8 |
# +----------+-----------+---------+
```

#### 手动验证
```bash
# 连接数据库
mysql -h localhost -u aep_user -p aep_integration_db

# 检查表结构
SHOW TABLES;

# 验证数据
SELECT * FROM t_aep_platforms;
SELECT * FROM t_aep_configs LIMIT 5;

# 检查视图
SELECT * FROM v_aep_product_overview LIMIT 5;
```

## 🔧 故障排查

### 常见问题及解决方案

#### 1. 连接被拒绝
```
错误: Can't connect to MySQL server on 'localhost' (61)
解决:
1. 确认MySQL服务已启动: brew services list | grep mysql
2. 检查端口占用: lsof -i :3306
3. 验证防火墙设置
```

#### 2. 认证失败
```
错误: Access denied for user 'aep_user'@'localhost'
解决:
1. 验证用户密码: mysql -u root -p -e "SELECT user, host FROM mysql.user WHERE user='aep_user';"
2. 重新创建用户: DROP USER 'aep_user'@'localhost'; CREATE USER ...
3. 检查权限: SHOW GRANTS FOR 'aep_user'@'localhost';
```

#### 3. 数据库不存在
```
错误: Unknown database 'aep_integration_db'
解决:
1. 手动创建数据库: CREATE DATABASE aep_integration_db;
2. 检查字符集: SHOW CREATE DATABASE aep_integration_db;
```

#### 4. 表创建失败
```
错误: Table 'xxx' already exists 或 Syntax error
解决:
1. 检查MySQL版本: SELECT VERSION();
2. 清空数据库重新创建: DROP DATABASE aep_integration_db; CREATE DATABASE ...
3. 逐句执行SQL确认错误位置
```

### 日志和诊断

#### 查看MySQL错误日志
```bash
# macOS (Homebrew)
tail -f /opt/homebrew/var/mysql/*.err

# Ubuntu/Debian
sudo tail -f /var/log/mysql/error.log

# CentOS/RHEL
sudo tail -f /var/log/mysqld.log
```

#### 启用查询日志
```sql
-- 临时启用慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;
SET GLOBAL slow_query_log_file = '/tmp/mysql_slow.log';
```

## 🚀 性能优化

### MySQL 配置优化 (my.cnf)
```ini
[mysqld]
# 基础配置
character-set-server = utf8mb3
collation-server = utf8mb3_general_ci
default-storage-engine = INNODB

# 内存优化
innodb_buffer_pool_size = 2G
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2
max_connections = 200

# 查询缓存 (MySQL 8.0已移除，使用Query Cache替代)
query_cache_size = 0
query_cache_type = 0

# 慢查询日志
slow_query_log = ON
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
```

### 索引优化检查
```sql
-- 检查表大小
SELECT
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size MB'
FROM information_schema.TABLES
WHERE table_schema = 'aep_integration_db'
ORDER BY (data_length + index_length) DESC;

-- 分析表统计信息
ANALYZE TABLE t_aep_products;
ANALYZE TABLE t_aep_operations;

-- 检查索引使用情况
SHOW INDEX FROM t_aep_operations;
```

## 🔒 生产环境安全

### 1. 用户权限最小化
```sql
-- 创建只读用户
CREATE USER 'aep_readonly'@'%' IDENTIFIED BY 'readonly_password';
GRANT SELECT ON aep_integration_db.* TO 'aep_readonly'@'%';

-- 创建备份用户
CREATE USER 'aep_backup'@'localhost' IDENTIFIED BY 'backup_password';
GRANT SELECT, LOCK TABLES, SHOW VIEW ON aep_integration_db.* TO 'aep_backup'@'localhost';

FLUSH PRIVILEGES;
```

### 2. SSL配置
```bash
# 检查SSL支持
mysql -u root -p -e "SHOW VARIABLES LIKE 'have_ssl';"

# 强制SSL连接
mysql -u aep_user -p --ssl-mode=REQUIRED aep_integration_db
```

### 3. 防火墙配置
```bash
# Ubuntu/Debian
sudo ufw allow from YOUR_APP_SERVER_IP to any port 3306
sudo ufw deny 3306

# CentOS/RHEL
sudo firewall-cmd --add-rich-rule="rule family=ipv4 source address=YOUR_APP_SERVER_IP/32 port protocol=tcp port=3306 accept"
sudo firewall-cmd --remove-service=mysql
```

## 📊 监控和维护

### 健康检查脚本
```bash
#!/bin/bash
# health_check.sh

# 检查MySQL服务状态
if ! pgrep -x mysqld > /dev/null; then
    echo "ERROR: MySQL service is not running"
    exit 2
fi

# 检查数据库连接
if ! ./scripts/test_database.sh > /dev/null 2>&1; then
    echo "ERROR: Database connection failed"
    exit 2
fi

# 检查表空间大小
TABLE_SIZE=$(mysql -u aep_user -p$DB_PASSWORD aep_integration_db \
    -e "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024) AS size_mb
        FROM information_schema.tables
        WHERE table_schema = 'aep_integration_db';" | tail -1)

if [[ $TABLE_SIZE -gt 1000 ]]; then
    echo "WARNING: Database size is ${TABLE_SIZE}MB"
    exit 1
fi

echo "OK: Database is healthy"
```

### 自动备份
```bash
#!/bin/bash
# backup_database.sh

BACKUP_DIR="/path/to/backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="aep_integration_db_backup_$DATE.sql"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
mysqldump -u aep_backup -p$BACKUP_PASSWORD \
    --single-transaction \
    --routines \
    --triggers \
    aep_integration_db > $BACKUP_DIR/$BACKUP_FILE

# 压缩备份文件
gzip $BACKUP_DIR/$BACKUP_FILE

# 清理旧备份（保留7天）
find $BACKUP_DIR -name "aep_integration_db_backup_*.sql.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_FILE.gz"
```

## 📚 相关文档

- **数据库设计文档**: `docs/DATABASE_DESIGN.md`
- **应用使用指南**: `docs/DATABASE_GUIDE.md`
- **架构设计**: `docs/architecture-design.md`

---

**部署清单 ✅**
- [ ] MySQL 8.0+ 已安装并启动
- [ ] 环境变量文件已配置 (.env.database)
- [ ] 数据库初始化脚本已执行成功
- [ ] 连接测试通过
- [ ] 表结构验证完成
- [ ] 安全配置已应用
- [ ] 监控脚本已部署

**维护团队**: AEP Integration 技术团队
**更新频率**: 跟随版本发布
**文档状态**: 🟢 生产就绪