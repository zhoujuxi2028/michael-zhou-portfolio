# MySQL 9.6 特性和优化说明

## 📋 文档信息
- **版本**: v1.0
- **创建日期**: 2026-01-26
- **MySQL版本**: 9.6.0+
- **状态**: 生产就绪

## 🎯 概述

AEP Integration 项目已针对 MySQL 9.6 进行优化，充分利用最新版本的特性和改进，提供更好的性能、安全性和开发体验。

## 🆕 MySQL 9.6 新特性应用

### 1. 字符集和排序规则优化

#### 默认使用 utf8mb4_0900_ai_ci
```sql
-- 数据库级别
CREATE DATABASE aep_integration_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

-- 表级别
CREATE TABLE t_aep_products (
    product_name varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
) CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

**优势**：
- **更好的Unicode支持**: 完整的UTF-8 4字节字符支持
- **改进的排序算法**: 0900排序规则提供更精确的语言排序
- **AI优化**: AI (Accent Insensitive) 和 CI (Case Insensitive) 提供更智能的搜索

### 2. JSON功能增强

#### JSON Schema验证
```sql
-- 平台扩展配置支持自动JSON验证
ALTER TABLE t_aep_platforms
ADD CONSTRAINT chk_ext_config_format
CHECK (JSON_VALID(ext_config));
```

#### 改进的JSON索引
```sql
-- 为JSON字段中的特定键创建索引
CREATE INDEX idx_json_error_code ON t_aep_operations
    ((CAST(response_data->>'$.error_code' AS CHAR(32))));
```

#### JSON聚合函数
```sql
-- 在视图中使用MySQL 9.6的JSON聚合
SELECT
    operation_date,
    JSON_ARRAYAGG(
        CASE WHEN operation_status = 'FAILED'
        THEN JSON_OBJECT('error_code', error_code, 'time', start_time)
        ELSE NULL END
    ) as error_details
FROM t_aep_operations;
```

### 3. 时间精度提升

#### 微秒级时间戳
```sql
-- 使用 datetime(6) 获得微秒精度
CREATE TABLE t_aep_operations (
    start_time datetime(6) NOT NULL COMMENT '开始时间（微秒精度）',
    end_time datetime(6) NULL DEFAULT NULL COMMENT '结束时间（微秒精度）'
);
```

**应用场景**：
- 高精度性能监控
- 精确的操作时序分析
- 微服务间的精确时间关联

### 4. 窗口函数应用

#### 排名和分析
```sql
-- 产品设备使用率排名
SELECT
    product_name,
    current_device_count,
    ROW_NUMBER() OVER (
        PARTITION BY device_type
        ORDER BY current_device_count DESC
    ) as device_rank_in_type
FROM t_aep_products;
```

### 5. 虚拟列和生成列

#### JSON字段虚拟列
```sql
-- 为JSON字段创建虚拟列以提升查询性能
ALTER TABLE t_aep_operations
ADD COLUMN response_error_code VARCHAR(32)
GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(response_data, '$.error_code'))) VIRTUAL;

CREATE INDEX idx_response_error_code ON t_aep_operations(response_error_code);
```

**优势**：
- JSON字段查询性能提升
- 索引支持JSON内部数据
- 保持数据一致性

### 6. 增强的异常处理

#### 存储过程错误处理
```sql
DELIMITER $$
CREATE PROCEDURE sp_aep_cleanup_expired_data_v96(IN retention_days INT)
BEGIN
    -- MySQL 9.6 改进的异常处理
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;  -- 重新抛出异常，保持错误信息
    END;

    -- 使用GET DIAGNOSTICS获取详细信息
    DELETE FROM t_aep_operations WHERE start_time < DATE_SUB(CURRENT_DATE, INTERVAL retention_days DAY);
    GET DIAGNOSTICS @deleted_count = ROW_COUNT;

    -- 使用JSON格式返回结果
    SELECT JSON_OBJECT(
        'deleted_count', @deleted_count,
        'cleanup_time', NOW()
    ) as result;
END$$
DELIMITER ;
```

## 📊 性能优化策略

### 1. 索引策略优化

#### 复合索引设计
```sql
-- 利用MySQL 9.6的索引优化
CREATE INDEX idx_complex_query ON t_aep_operations (
    operation_type ASC,
    resource_type ASC,
    operation_status ASC,
    start_time ASC
) USING BTREE;
```

#### 前缀索引优化
```sql
-- 对长字符串字段使用前缀索引
CREATE INDEX idx_api_url_prefix ON t_aep_operations (api_url(100));
```

### 2. 查询优化

#### 利用CTE (Common Table Expressions)
```sql
-- 使用CTE提高查询可读性和性能
WITH operation_stats AS (
    SELECT
        DATE(start_time) as op_date,
        operation_type,
        COUNT(*) as total,
        AVG(execution_time) as avg_time
    FROM t_aep_operations
    WHERE start_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
    GROUP BY DATE(start_time), operation_type
)
SELECT * FROM operation_stats WHERE avg_time > 1000;
```

### 3. 分区表策略

#### 时间分区优化
```sql
-- 按月分区的操作日志表
ALTER TABLE t_aep_operations
PARTITION BY RANGE (YEAR(start_time)*100 + MONTH(start_time)) (
    PARTITION p202601 VALUES LESS THAN (202602),
    PARTITION p202602 VALUES LESS THAN (202603),
    PARTITION p202603 VALUES LESS THAN (202604),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

## 🔧 配置优化建议

### 1. MySQL 9.6 配置文件

#### my.cnf 优化配置
```ini
[mysqld]
# 字符集配置
character-set-server = utf8mb4
collation-server = utf8mb4_0900_ai_ci

# 内存分配优化
innodb_buffer_pool_size = 4G
innodb_log_file_size = 1G
innodb_log_buffer_size = 64M

# JSON优化
innodb_sort_buffer_size = 2M

# 并发优化
innodb_thread_concurrency = 0
innodb_read_io_threads = 8
innodb_write_io_threads = 8

# MySQL 9.6 新特性
# 启用并行查询
innodb_parallel_read_threads = 4

# 优化JSON处理
optimizer_switch = 'prefer_ordering_index=off'
```

### 2. 连接池配置

#### HikariCP 配置优化
```properties
# .env.database 中的MySQL 9.6优化配置
DB_CHARACTER_ENCODING=utf8mb4
DB_COLLATION=utf8mb4_0900_ai_ci

# 连接池优化
DB_POOL_SIZE=20
DB_MIN_IDLE=5
DB_MAX_LIFETIME=1800000

# MySQL 9.6 连接参数
DB_USE_SERVER_PREP_STMTS=true
DB_CACHE_PREP_STMTS=true
DB_PREP_STMT_CACHE_SIZE=500

# JSON优化
DB_USE_UNICODE=true
DB_CHARACTER_ENCODING_RESULTS=utf8mb4
```

## 🚀 迁移指南

### 从旧版本MySQL迁移

#### 1. 字符集迁移
```sql
-- 检查当前字符集
SELECT table_schema, table_name, table_collation
FROM information_schema.tables
WHERE table_schema = 'aep_integration_db';

-- 转换为utf8mb4_0900_ai_ci
ALTER TABLE t_aep_products CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

#### 2. JSON数据验证
```sql
-- 验证现有JSON数据
SELECT id, ext_config
FROM t_aep_platforms
WHERE NOT JSON_VALID(ext_config);
```

#### 3. 索引重建
```sql
-- 重建索引以利用新特性
DROP INDEX idx_old_index ON t_aep_operations;
CREATE INDEX idx_new_optimized ON t_aep_operations (operation_type, start_time DESC);
```

## 📈 监控和维护

### 1. 性能监控查询

#### JSON字段使用统计
```sql
SELECT
    table_name,
    column_name,
    AVG(JSON_LENGTH(column_name)) as avg_json_size
FROM information_schema.columns c
JOIN your_table t ON c.table_name = t.table_name
WHERE c.data_type = 'json'
GROUP BY table_name, column_name;
```

#### 索引效率分析
```sql
SELECT
    object_name,
    index_name,
    count_read,
    count_write,
    sum_timer_read/count_read as avg_read_time
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE object_schema = 'aep_integration_db'
ORDER BY avg_read_time DESC;
```

### 2. 自动化维护

#### 定期统计信息更新
```sql
-- 创建定时事件
CREATE EVENT ev_update_table_stats
ON SCHEDULE EVERY 1 DAY
STARTS CURDATE() + INTERVAL 1 DAY
DO
BEGIN
    ANALYZE TABLE t_aep_operations;
    ANALYZE TABLE t_aep_products;
    ANALYZE TABLE t_aep_devices;
END;
```

## 🔍 故障排查

### 1. JSON相关问题

#### 常见错误
```bash
# 错误: Invalid JSON text
# 解决: 验证JSON格式
SELECT id FROM t_aep_platforms WHERE NOT JSON_VALID(ext_config);
```

#### 性能问题
```sql
-- 检查JSON字段查询性能
EXPLAIN FORMAT=JSON
SELECT * FROM t_aep_operations
WHERE JSON_EXTRACT(response_data, '$.error_code') = 'AUTH_FAILED';
```

### 2. 字符集问题

#### 字符显示异常
```sql
-- 检查字符集配置
SHOW VARIABLES LIKE 'character_set%';
SHOW VARIABLES LIKE 'collation%';
```

## 📚 参考资源

### 官方文档
- [MySQL 9.6 新特性](https://dev.mysql.com/doc/relnotes/mysql/9.6/en/)
- [JSON 函数参考](https://dev.mysql.com/doc/refman/9.6/en/json-functions.html)
- [utf8mb4_0900_ai_ci 排序规则](https://dev.mysql.com/doc/refman/9.6/en/charset-unicode-sets.html)

### 项目文档
- **数据库设计**: `DATABASE_DESIGN.md`
- **部署指南**: `DATABASE_DEPLOYMENT_GUIDE.md`
- **使用指南**: `DATABASE_GUIDE.md`

---

**MySQL 9.6 特性总结**：
✅ utf8mb4_0900_ai_ci 字符集优化
✅ JSON 功能增强和索引优化
✅ 微秒精度时间戳
✅ 窗口函数应用
✅ 虚拟列性能优化
✅ 改进的异常处理机制

**维护团队**: AEP Integration 技术团队
**文档状态**: 🟢 MySQL 9.6 优化完成