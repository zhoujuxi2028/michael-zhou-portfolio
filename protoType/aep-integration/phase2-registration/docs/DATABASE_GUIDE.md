# AEP Integration 数据库使用指南

## 📋 文档信息
- **版本**: v1.0
- **创建日期**: 2026-01-25
- **更新日期**: 2026-01-25
- **适用环境**: MySQL 8.0+
- **状态**: 生产就绪

## 🎯 概述

AEP Integration项目采用MySQL作为主数据库，用于存储产品注册信息、操作日志、配置管理和统计数据。本指南提供完整的数据库设置、使用和维护说明。

## 🏗️ 数据库架构

### 核心表结构

```
aep_integration/
├── aep_products              # 产品注册信息表
├── aep_product_configs       # 产品配置表
├── aep_operation_logs        # 操作审计日志表
├── aep_system_logs          # 系统日志表
├── aep_app_configs          # 应用配置表
├── aep_environments         # 环境配置表
├── aep_operation_stats      # 操作统计表（日汇总）
└── aep_error_stats          # 错误统计表
```

### 关键设计特性

- **UTF8MB4字符集** - 支持完整的Unicode字符
- **复合索引优化** - 针对查询模式优化的索引设计
- **分区支持** - 大表按日期分区，提升查询性能
- **敏感数据加密** - AEP密钥等敏感信息加密存储
- **审计日志完整** - 所有操作记录可追溯

## 🚀 快速开始

### 1. 环境准备

**系统要求：**
- MySQL 8.0+
- Java 17+
- 网络连通性到远程MySQL服务器

**创建环境配置：**

```bash
# 复制配置模板
cp .env.database.template .env.database

# 编辑配置文件
vim .env.database
```

**必须配置的参数：**

```bash
# MySQL服务器连接信息
DB_HOST=your-mysql-server.com
DB_PORT=3306
DB_NAME=aep_integration
DB_USERNAME=aep_user
DB_PASSWORD=your_secure_password

# 管理员账户（仅用于初始化）
DB_ADMIN_USER=root
DB_ADMIN_PASSWORD=admin_password
```

### 2. 数据库初始化

**自动化设置：**

```bash
# 开发环境
./scripts/setup_database.sh development

# 测试环境
./scripts/setup_database.sh test

# 生产环境
./scripts/setup_database.sh production
```

**验证安装：**

```bash
# 测试数据库连接
./scripts/test_database.sh

# 检查表结构
mysql -h $DB_HOST -u $DB_USERNAME -p $DB_NAME -e "SHOW TABLES;"
```

### 3. 应用配置

**Maven依赖已添加：**
- mysql-connector-java 8.0.33
- HikariCP 5.0.1
- MyBatis 3.5.13
- Flyway 9.22.0

**Java代码集成：**

```java
// 获取数据库服务实例
DatabaseService dbService = DatabaseService.getInstance();

// 检查健康状态
if (dbService.isHealthy()) {
    System.out.println("数据库连接正常");
}

// 保存产品信息
AepProduct product = AepProduct.builder()
    .productName("智能传感器")
    .deviceType("SENSOR")
    .dataFormat(1)
    .build();

boolean success = dbService.saveProduct(product);
```

## 📊 数据表详解

### 1. aep_products - 产品注册表

**主要字段：**
- `product_id` - AEP平台产品ID（业务主键）
- `product_name` - 产品名称（唯一）
- `device_type` - 设备类型（SENSOR/GATEWAY/DEVICE/TERMINAL/MODULE）
- `network_type` - 网络类型（NB-IOT/2G/3G/4G/5G/WIFI/ETHERNET/LORA）
- `master_key` - AEP产品主密钥（加密存储）
- `status` - 状态（ACTIVE/INACTIVE/DELETED）

**常用查询：**

```sql
-- 查询所有活跃产品
SELECT * FROM aep_products WHERE status = 'ACTIVE' ORDER BY created_at DESC;

-- 按设备类型统计
SELECT device_type, COUNT(*) as count FROM aep_products
WHERE status = 'ACTIVE' GROUP BY device_type;

-- 查找重复产品名称
SELECT product_name, COUNT(*) as count FROM aep_products
GROUP BY product_name HAVING count > 1;
```

### 2. aep_operation_logs - 操作审计表

**主要字段：**
- `operation_id` - 操作唯一标识（UUID）
- `operation_type` - 操作类型（CREATE/UPDATE/DELETE/QUERY）
- `resource_type` - 资源类型（PRODUCT/DEVICE/SUBSCRIPTION）
- `operation_status` - 操作状态（SUCCESS/FAILED/PENDING）
- `execution_time_ms` - 执行时长（毫秒）

**常用查询：**

```sql
-- 查询最近失败的操作
SELECT * FROM aep_operation_logs
WHERE operation_status = 'FAILED'
ORDER BY start_time DESC LIMIT 20;

-- 统计今日操作情况
SELECT operation_type, operation_status, COUNT(*) as count,
       AVG(execution_time_ms) as avg_time
FROM aep_operation_logs
WHERE DATE(start_time) = CURDATE()
GROUP BY operation_type, operation_status;

-- 查询慢操作（>5秒）
SELECT * FROM aep_operation_logs
WHERE execution_time_ms > 5000
ORDER BY execution_time_ms DESC;
```

### 3. aep_app_configs - 应用配置表

**配置分组：**
- `database` - 数据库配置
- `aep_api` - AEP API配置
- `retry_policy` - 重试策略配置
- `security` - 安全配置
- `monitoring` - 监控配置

**常用操作：**

```sql
-- 查看所有配置
SELECT config_group, config_key, config_value, description
FROM aep_app_configs ORDER BY config_group, config_key;

-- 更新配置
UPDATE aep_app_configs
SET config_value = '10'
WHERE config_group = 'database' AND config_key = 'connection_pool_size';

-- 添加新配置
INSERT INTO aep_app_configs (config_group, config_key, config_value, description)
VALUES ('aep_api', 'request_rate_limit', '100', 'API请求速率限制（每分钟）');
```

## 🔧 高级功能

### 1. 性能监控

**查看连接池状态：**

```java
DatabaseService dbService = DatabaseService.getInstance();
DatabaseConfig.DatabaseStats stats = dbService.getDatabaseStats();
System.out.println("连接池状态: " + stats.toString());
```

**查看操作统计：**

```java
DatabaseService.OperationStats opStats = dbService.getOperationStats();
System.out.println("操作统计: " + opStats.toString());
System.out.println("成功率: " + opStats.getSuccessRate() + "%");
```

### 2. 数据维护

**清理过期日志：**

```java
// 清理30天前的操作日志
int deletedCount = dbService.cleanupOldLogs(30);
System.out.println("已清理日志记录数: " + deletedCount);
```

**手动清理SQL：**

```sql
-- 清理90天前的操作日志
DELETE FROM aep_operation_logs
WHERE start_time < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- 清理旧的错误统计
DELETE FROM aep_error_stats
WHERE stat_date < DATE_SUB(CURDATE(), INTERVAL 365 DAY);
```

### 3. 备份与恢复

**数据备份：**

```bash
# 全量备份
mysqldump -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME > backup_$(date +%Y%m%d_%H%M%S).sql

# 仅备份架构
mysqldump -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD --no-data $DB_NAME > schema_backup.sql

# 仅备份数据
mysqldump -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD --no-create-info $DB_NAME > data_backup.sql
```

**数据恢复：**

```bash
# 恢复完整备份
mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME < backup_20260125_143022.sql

# 恢复特定表
mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME -e "DROP TABLE IF EXISTS aep_products;"
mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME < products_backup.sql
```

## 🛡️ 安全最佳实践

### 1. 连接安全

**SSL配置：**

```properties
# 启用SSL连接
DB_USE_SSL=true
DB_SSL_MODE=REQUIRED

# 客户端证书（如需要）
DB_SSL_CERT_PATH=/path/to/client-cert.pem
DB_SSL_KEY_PATH=/path/to/client-key.pem
DB_SSL_CA_PATH=/path/to/ca-cert.pem
```

**防火墙设置：**

```bash
# 仅允许应用服务器IP连接MySQL
# 在MySQL服务器上执行：
ufw allow from 192.168.1.100 to any port 3306
ufw deny 3306
```

### 2. 用户权限管理

**创建只读用户：**

```sql
-- 创建只读用户用于报表查询
CREATE USER 'aep_readonly'@'%' IDENTIFIED BY 'readonly_password';
GRANT SELECT ON aep_integration.* TO 'aep_readonly'@'%';
FLUSH PRIVILEGES;
```

**创建备份用户：**

```sql
-- 创建专用备份用户
CREATE USER 'aep_backup'@'localhost' IDENTIFIED BY 'backup_password';
GRANT SELECT, LOCK TABLES, SHOW VIEW, EVENT, TRIGGER ON aep_integration.* TO 'aep_backup'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 敏感数据处理

**数据加密：**

```java
// 敏感数据加密存储示例
public class SecurityUtils {
    private static final String ALGORITHM = "AES-256-GCM";

    public static String encryptSensitiveData(String data) {
        // 实现加密逻辑
        return encryptedData;
    }

    public static String decryptSensitiveData(String encryptedData) {
        // 实现解密逻辑
        return originalData;
    }
}
```

**数据脱敏：**

```sql
-- 查看脱敏后的敏感信息
SELECT
    id, product_name, device_type,
    CONCAT(LEFT(master_key, 4), '****', RIGHT(master_key, 4)) as masked_key,
    status, created_at
FROM aep_products;
```

## 📈 监控与告警

### 1. 性能监控SQL

```sql
-- 数据库性能概览
SELECT
    'Products' as table_name, COUNT(*) as record_count,
    ROUND(AVG(LENGTH(master_key))) as avg_key_length
FROM aep_products
UNION ALL
SELECT
    'Operation Logs' as table_name, COUNT(*) as record_count,
    ROUND(AVG(execution_time_ms)) as avg_execution_time
FROM aep_operation_logs;

-- 慢查询分析
SELECT
    operation_type,
    AVG(execution_time_ms) as avg_time,
    MAX(execution_time_ms) as max_time,
    COUNT(*) as count
FROM aep_operation_logs
WHERE start_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY operation_type
HAVING avg_time > 1000;
```

### 2. 告警规则

**建议设置的告警：**
- 数据库连接数 > 80%
- 慢查询数量 > 10/分钟
- 失败操作率 > 5%
- 磁盘空间使用率 > 85%
- 日志表大小 > 1GB

### 3. 健康检查脚本

```bash
#!/bin/bash
# health_check.sh

# 检查数据库连接
if ! mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD -e "SELECT 1;" >/dev/null 2>&1; then
    echo "CRITICAL: Database connection failed"
    exit 2
fi

# 检查表空间
TABLE_SIZE=$(mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME \
    -e "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024) AS size_mb
        FROM information_schema.tables
        WHERE table_schema = '$DB_NAME';" | tail -1)

if [[ $TABLE_SIZE -gt 1000 ]]; then
    echo "WARNING: Database size is ${TABLE_SIZE}MB"
    exit 1
fi

echo "OK: Database is healthy"
exit 0
```

## 🔄 数据迁移

### 1. Flyway迁移

**迁移文件命名：**
```
src/main/resources/db/migration/
├── V1__Initial_schema.sql
├── V2__Add_product_indexes.sql
├── V3__Update_operation_logs.sql
└── V4__Add_statistics_tables.sql
```

**迁移示例：**

```sql
-- V2__Add_product_indexes.sql
-- 添加产品表性能优化索引

CREATE INDEX idx_products_device_network
ON aep_products (device_type, network_type, status);

CREATE INDEX idx_products_created_at
ON aep_products (created_at DESC);

-- 添加操作日志复合索引
CREATE INDEX idx_operation_logs_complex
ON aep_operation_logs (operation_type, resource_type, start_time DESC);
```

### 2. 数据版本控制

**检查迁移状态：**

```bash
# 查看迁移历史
mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME \
    -e "SELECT * FROM flyway_schema_history ORDER BY installed_on;"

# 手动执行迁移（如果自动迁移被禁用）
java -cp "target/*" org.flywaydb.commandline.Main migrate \
    -url=jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME \
    -user=$DB_USERNAME -password=$DB_PASSWORD
```

## 🚨 故障排查

### 1. 常见问题

**连接超时：**
```
问题：Connection timeout
解决：
1. 检查网络连通性
2. 增加连接超时设置
3. 检查MySQL服务器负载
```

**字符编码问题：**
```
问题：中文字符显示异常
解决：
1. 确认数据库字符集为utf8mb4
2. 检查连接URL中的字符集参数
3. 验证客户端字符集配置
```

**权限拒绝：**
```
问题：Access denied for user
解决：
1. 验证用户名密码
2. 检查用户权限
3. 确认用户可以从当前IP连接
```

### 2. 调试技巧

**启用详细日志：**

```properties
# 启用MyBatis SQL日志
logging.level.com.aep.registration.database.mapper=DEBUG

# 启用HikariCP连接池日志
logging.level.com.zaxxer.hikari=DEBUG

# 启用数据库迁移日志
logging.level.org.flywaydb=INFO
```

**SQL执行跟踪：**

```java
// 在代码中添加执行时间跟踪
long startTime = System.currentTimeMillis();
try {
    // 数据库操作
    result = dbService.saveProduct(product);
} finally {
    long executionTime = System.currentTimeMillis() - startTime;
    logger.info("SQL执行时间: " + executionTime + "ms");
}
```

## 📚 参考资源

### 相关文档
- [MySQL 8.0 Reference Manual](https://dev.mysql.com/doc/refman/8.0/en/)
- [MyBatis User Guide](https://mybatis.org/mybatis-3/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP)
- [Flyway Documentation](https://flywaydb.org/documentation/)

### 项目相关
- **架构设计**: `docs/architecture-design.md`
- **API文档**: `docs/api-reference.md`
- **部署指南**: `docs/deployment-guide.md`

---

**维护信息**：
- **维护团队**: 云监控平台技术团队
- **更新频率**: 按需更新
- **文档状态**: 🟢 最新版本

如有疑问或建议，请联系技术团队或提交Issue。