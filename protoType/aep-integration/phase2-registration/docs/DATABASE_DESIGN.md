# AEP Integration 数据库设计文档

## 📋 文档信息
- **版本**: v2.0
- **创建日期**: 2026-01-25
- **更新日期**: 2026-01-25
- **设计者**: 云监控平台技术团队
- **状态**: 设计完成，待部署
- **兼容性**: 基于众成通信现有系统架构

## 🎯 设计概述

AEP Integration数据库设计基于现有众成通信云监控系统的数据库架构，完全采用现有系统的设计规范和命名约定，确保与现有70+张表的无缝集成。本设计专门针对中国电信AEP物联网平台集成优化，提供产品管理、设备映射、操作审计、数据同步等核心功能。

## 🏗️ 设计原则

### 1. 兼容性原则
- **命名规范**: 采用 `t_` 表名前缀，与现有系统保持一致
- **数据类型**: 使用 `varchar(32)` 作为主键，沿用现有规范
- **字符集**: 使用 `utf8mb3` 字符集和 `utf8mb3_general_ci` 排序规则
- **字段设计**: 保持与现有表相似的字段长度和类型定义

### 2. 专业性原则
- **业务聚焦**: 专门为AEP平台集成设计，避免功能冗余
- **数据分离**: 与现有业务数据物理分离，逻辑关联
- **扩展性**: 预留扩展字段和配置项，支持未来功能演进

### 3. 安全性原则
- **敏感数据加密**: AEP密钥、证书等敏感信息加密存储
- **操作审计**: 完整记录所有AEP API操作，支持安全审计
- **权限控制**: 支持基于角色的数据访问控制

## 📊 数据库架构

### 总体架构图

```
AEP Integration Database Architecture
├── 核心业务表 (4张)
│   ├── t_aep_products          # AEP产品管理表
│   ├── t_aep_devices          # AEP设备映射表
│   ├── t_aep_operations       # API操作记录表
│   └── t_aep_platforms        # 平台配置表
├── 数据管理表 (2张)
│   ├── t_aep_sync_status      # 同步状态表
│   └── t_aep_device_data      # 设备数据表
├── 系统配置表 (2张)
│   ├── t_aep_statistics       # 统计数据表
│   └── t_aep_configs          # 配置管理表
├── 智能视图 (3个)
│   ├── v_aep_product_overview
│   ├── v_aep_device_status
│   └── v_aep_operation_summary
└── 自动化组件
    ├── 存储过程 (2个)
    └── 触发器 (2个)
```

## 📋 核心表设计

### 1. t_aep_products - AEP产品管理表

**设计目标**: 管理AEP平台的产品信息，与本地产品型号建立映射关系

**核心字段**:
```sql
aep_product_id varchar(64)      -- AEP平台产品ID（唯一）
product_name varchar(128)       -- 产品名称
device_type varchar(32)         -- 设备类型（SENSOR/GATEWAY/DEVICE等）
network_type varchar(32)        -- 网络类型（NB-IOT/4G/5G/WIFI等）
master_key varchar(512)         -- AEP产品主密钥（加密存储）
local_prod_module varchar(64)   -- 本地对应产品型号（关联t_prod_module）
current_device_count int        -- 当前设备数量（自动统计）
max_device_count int           -- 最大设备数量限制
platform_id varchar(32)        -- 关联AEP平台配置
status varchar(16)             -- 产品状态（ACTIVE/INACTIVE/DELETED）
```

**业务规则**:
- `aep_product_id` 全局唯一，对应AEP平台的ProductID
- `product_name` 在同一状态下唯一
- `current_device_count` 通过触发器自动维护
- `master_key` 使用AES-256-GCM加密存储

**关联关系**:
- `local_prod_module` → `t_prod_module.prod_module` (现有系统产品型号)
- `platform_id` → `t_aep_platforms.id` (AEP平台配置)

### 2. t_aep_devices - AEP设备映射表

**设计目标**: 建立AEP设备与本地设备的双向映射关系，支持数据同步

**核心字段**:
```sql
aep_device_id varchar(64)       -- AEP平台设备ID（唯一）
aep_product_id varchar(64)      -- 关联AEP产品
local_device_id varchar(32)     -- 本地设备ID（关联t_device.id）
local_lbs_id varchar(256)       -- 本地设备编码（关联t_device.lbs_id）
node_id varchar(256)           -- 设备节点ID（IMEI/MAC/Serial）
device_status varchar(16)      -- 设备状态（ONLINE/OFFLINE/ABNORMAL）
sync_status varchar(16)        -- 同步状态（PENDING/SUCCESS/FAILED）
last_sync_time datetime        -- 最后同步时间
device_lng float(20,5)         -- 设备经度（与现有系统格式一致）
device_lat float(20,5)         -- 设备纬度（与现有系统格式一致）
```

**业务规则**:
- `aep_device_id` 全局唯一，对应AEP平台的DeviceID
- 支持一对一和一对多映射关系
- 地理坐标字段与现有系统保持精度一致
- `sync_status` 追踪设备数据同步状态

**关联关系**:
- `local_device_id` → `t_device.id` (现有系统设备表)
- `local_lbs_id` → `t_device.lbs_id` (现有系统设备编码)
- `aep_product_id` → `t_aep_products.aep_product_id` (AEP产品)

### 3. t_aep_operations - AEP API操作记录表

**设计目标**: 完整记录所有AEP API操作，支持操作审计和性能监控

**核心字段**:
```sql
operation_id varchar(64)        -- 操作唯一标识
operation_type varchar(32)      -- 操作类型（CREATE/UPDATE/DELETE等）
resource_type varchar(32)       -- 资源类型（PRODUCT/DEVICE/SUBSCRIPTION）
api_url varchar(512)           -- API请求地址
request_data text              -- 请求数据（脱敏后）
response_data text             -- 响应数据（脱敏后）
operation_status varchar(16)   -- 操作状态（PENDING/SUCCESS/FAILED）
execution_time int             -- 执行时长（毫秒）
start_time datetime(3)         -- 开始时间（毫秒精度）
operator_id varchar(32)        -- 操作人ID（关联t_sys_user）
trace_id varchar(64)          -- 链路跟踪ID
```

**业务规则**:
- `operation_id` 全局唯一，支持幂等性操作
- 时间字段使用毫秒精度，支持高精度性能分析
- 敏感数据自动脱敏后存储
- 支持分布式链路追踪

**关联关系**:
- `operator_id` → `t_sys_user.user_id` (现有系统用户表)
- `platform_id` → `t_aep_platforms.id` (AEP平台配置)

### 4. t_aep_platforms - AEP平台配置表

**设计目标**: 支持多环境AEP平台配置管理，实现环境隔离

**核心字段**:
```sql
platform_name varchar(64)      -- 平台名称
api_host varchar(128)          -- AEP API主机地址
app_id varchar(64)             -- AEP应用ID
app_key varchar(512)           -- AEP应用密钥（加密存储）
app_secret varchar(512)        -- AEP应用密码（加密存储）
environment varchar(32)        -- 环境标识（dev/test/staging/prod）
max_retries int                -- 最大重试次数
timeout_ms int                 -- 请求超时时间（毫秒）
is_active tinyint(1)           -- 是否激活
is_default tinyint(1)          -- 是否默认平台
```

**业务规则**:
- 支持同一平台的多环境配置
- 密钥信息使用AES-256-GCM加密存储
- 每个环境只能有一个默认平台
- 支持动态配置热更新

### 5. t_aep_sync_status - 数据同步状态表

**设计目标**: 追踪AEP与本地系统之间的数据同步状态

**核心字段**:
```sql
sync_type varchar(32)          -- 同步类型（FULL/INCREMENTAL/REALTIME）
resource_type varchar(32)      -- 资源类型（PRODUCT/device/DATA/ALARM）
direction varchar(16)          -- 同步方向（UP/DOWN/BOTH）
sync_status varchar(16)        -- 同步状态（PENDING/RUNNING/SUCCESS/FAILED）
sync_progress decimal(5,2)     -- 同步进度（百分比）
total_records int              -- 总记录数
success_records int            -- 成功记录数
failed_records int             -- 失败记录数
next_sync_time datetime        -- 下次同步时间
```

**业务规则**:
- 支持全量、增量、实时三种同步模式
- 详细记录同步进度和状态
- 支持同步任务的暂停和恢复
- 提供同步失败的详细错误信息

### 6. t_aep_device_data - AEP设备数据表

**设计目标**: 存储从AEP平台接收的设备数据

**核心字段**:
```sql
aep_device_id varchar(64)      -- AEP设备ID
data_type varchar(32)          -- 数据类型（TELEMETRY/COMMAND/ALARM/STATUS）
data_format varchar(16)        -- 数据格式（JSON/BINARY/HEX）
raw_data text                  -- 原始数据
parsed_data json              -- 解析后数据（JSON格式）
receive_time datetime(3)       -- 接收时间（毫秒精度）
process_status varchar(16)     -- 处理状态（PENDING/PROCESSED/FAILED）
```

**业务规则**:
- 支持多种数据格式的存储和解析
- 保留原始数据用于故障排查
- 使用JSON字段存储结构化数据
- 支持数据处理状态追踪

### 7. t_aep_statistics - AEP统计数据表

**设计目标**: 存储各类统计指标，支持监控和分析

**核心字段**:
```sql
stat_date date                 -- 统计日期
stat_hour tinyint              -- 统计小时（空值表示日统计）
category tinyint               -- 统计类别（0=设备 1=操作 2=同步 3=告警）
metric_name varchar(64)        -- 指标名称
metric_value bigint            -- 指标值
dimension1 varchar(64)         -- 维度1（设备类型等）
dimension2 varchar(64)         -- 维度2（网络类型等）
dimension3 varchar(64)         -- 维度3（状态等）
```

**业务规则**:
- 支持按小时和按天两种粒度统计
- 多维度统计分析
- 历史数据自动归档
- 支持实时和批量统计

### 8. t_aep_configs - AEP配置管理表

**设计目标**: 统一管理AEP集成相关的各种配置项

**核心字段**:
```sql
config_group varchar(64)       -- 配置组（api/sync/retry/security/monitoring）
config_key varchar(128)        -- 配置键
config_value text              -- 配置值
config_type varchar(32)        -- 配置类型（STRING/INTEGER/BOOLEAN/JSON/ENCRYPTED）
is_encrypted tinyint(1)        -- 是否加密存储
environment varchar(32)        -- 适用环境（ALL/dev/test/prod）
validation_rule varchar(512)   -- 验证规则（正则表达式）
```

**业务规则**:
- 支持分组和分环境的配置管理
- 敏感配置自动加密存储
- 配置值类型验证
- 支持配置的热更新

## 🔗 系统集成关系

### 与现有系统的关联

```sql
-- 1. 设备关联（双向映射）
t_aep_devices.local_device_id → t_device.id
t_aep_devices.local_lbs_id → t_device.lbs_id

-- 2. 产品型号关联
t_aep_products.local_prod_module → t_prod_module.prod_module

-- 3. 用户系统关联
t_aep_operations.operator_id → t_sys_user.user_id
t_aep_platforms.created_by → t_sys_user.user_id

-- 4. 公司厂商关联
t_aep_products.manufacturer_id → t_company.id

-- 5. 地理位置关联
t_aep_devices.device_lng/device_lat → 与现有系统经纬度格式一致
```

### 数据流向关系

```
AEP平台 ←→ t_aep_products/t_aep_devices ←→ 现有设备管理系统
    ↓
t_aep_operations (操作审计)
    ↓
t_aep_statistics (统计分析)
```

## 📊 智能视图系统

### 1. v_aep_product_overview - 产品概览视图

**功能**: 产品使用情况综合展示

```sql
CREATE VIEW v_aep_product_overview AS
SELECT
    p.id,
    p.aep_product_id,
    p.product_name,
    p.device_type,
    p.current_device_count,
    p.max_device_count,
    -- 设备使用率计算
    CASE
        WHEN p.max_device_count > 0
        THEN ROUND((p.current_device_count * 100.0 / p.max_device_count), 2)
        ELSE 0
    END as device_usage_percentage,
    plt.platform_name,
    plt.environment
FROM t_aep_products p
LEFT JOIN t_aep_platforms plt ON p.platform_id = plt.id
WHERE p.status = 'ACTIVE';
```

**应用场景**:
- 产品管理界面数据展示
- 设备容量监控告警
- 资源使用率分析

### 2. v_aep_device_status - 设备状态视图

**功能**: 设备状态综合监控

```sql
CREATE VIEW v_aep_device_status AS
SELECT
    d.aep_device_id,
    d.device_name,
    d.device_status,
    d.sync_status,
    d.last_sync_time,
    p.product_name,
    p.device_type,
    plt.platform_name
FROM t_aep_devices d
LEFT JOIN t_aep_products p ON d.aep_product_id = p.aep_product_id
LEFT JOIN t_aep_platforms plt ON d.platform_id = plt.id;
```

**应用场景**:
- 设备监控大屏
- 设备状态告警
- 同步状态追踪

### 3. v_aep_operation_summary - 操作统计视图

**功能**: API操作性能分析

```sql
CREATE VIEW v_aep_operation_summary AS
SELECT
    DATE(start_time) as operation_date,
    operation_type,
    resource_type,
    COUNT(*) as total_operations,
    SUM(CASE WHEN operation_status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count,
    ROUND(AVG(execution_time), 2) as avg_execution_time,
    MAX(execution_time) as max_execution_time
FROM t_aep_operations
WHERE start_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY DATE(start_time), operation_type, resource_type
ORDER BY operation_date DESC;
```

**应用场景**:
- API性能监控
- 成功率统计分析
- 系统健康度评估

## 🤖 自动化组件

### 存储过程

#### 1. sp_aep_cleanup_expired_data - 数据清理

**功能**: 自动清理过期的操作日志和设备数据

```sql
CREATE PROCEDURE sp_aep_cleanup_expired_data(IN retention_days INT)
BEGIN
    START TRANSACTION;

    -- 清理过期操作日志
    DELETE FROM t_aep_operations
    WHERE start_time < DATE_SUB(CURRENT_DATE, INTERVAL retention_days DAY);

    -- 清理过期设备数据
    DELETE FROM t_aep_device_data
    WHERE receive_time < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL retention_days DAY);

    COMMIT;
END
```

**调用方式**:
```sql
-- 清理30天前的数据
CALL sp_aep_cleanup_expired_data(30);
```

#### 2. sp_aep_aggregate_daily_stats - 统计聚合

**功能**: 每日统计数据聚合计算

```sql
CREATE PROCEDURE sp_aep_aggregate_daily_stats(IN stat_date DATE)
BEGIN
    -- 设备数量统计
    INSERT INTO t_aep_statistics (...)
    SELECT ... FROM t_aep_devices ...;

    -- 操作统计
    INSERT INTO t_aep_statistics (...)
    SELECT ... FROM t_aep_operations ...;
END
```

### 触发器

#### 1. 设备数量自动统计

```sql
-- 设备新增时自动更新产品设备数量
CREATE TRIGGER tr_aep_device_count_insert
AFTER INSERT ON t_aep_devices
FOR EACH ROW
BEGIN
    UPDATE t_aep_products
    SET current_device_count = (
        SELECT COUNT(*) FROM t_aep_devices
        WHERE aep_product_id = NEW.aep_product_id
    )
    WHERE aep_product_id = NEW.aep_product_id;
END

-- 设备删除时自动更新产品设备数量
CREATE TRIGGER tr_aep_device_count_delete
AFTER DELETE ON t_aep_devices
FOR EACH ROW
BEGIN
    UPDATE t_aep_products
    SET current_device_count = (
        SELECT COUNT(*) FROM t_aep_devices
        WHERE aep_product_id = OLD.aep_product_id
    )
    WHERE aep_product_id = OLD.aep_product_id;
END
```

## 🔒 安全设计

### 1. 敏感数据保护

**加密字段**:
- `t_aep_products.master_key` - 产品主密钥
- `t_aep_platforms.app_key` - AEP应用密钥
- `t_aep_platforms.app_secret` - AEP应用密码
- `t_aep_devices.psk` - 设备PSK密钥
- `t_aep_configs.config_value` (当is_encrypted=1时)

**加密算法**: AES-256-GCM

**密钥管理**:
- 使用专用的密钥管理服务
- 支持密钥轮换
- 密钥分环境隔离

### 2. 数据脱敏

**操作日志脱敏**:
- API请求/响应中的密钥信息自动脱敏
- 个人敏感信息马赛克处理
- 保留数据格式，隐藏具体值

**脱敏规则**:
```sql
-- 密钥脱敏：显示前4位和后4位，中间用****替代
SELECT CONCAT(LEFT(master_key, 4), '****', RIGHT(master_key, 4)) as masked_key

-- 设备ID部分脱敏
SELECT CONCAT(LEFT(aep_device_id, 8), '***', RIGHT(aep_device_id, 4)) as masked_device_id
```

### 3. 访问控制

**权限设计**:
- 基于角色的数据访问控制（RBAC）
- 支持行级安全（Row Level Security）
- 操作审计完整记录

**权限级别**:
- **管理员**: 所有表的完整访问权限
- **运维人员**: 读取权限 + 配置管理权限
- **开发人员**: 读取权限 + 测试环境写入权限
- **业务用户**: 基于视图的只读权限

## 📊 索引策略

### 1. 主键索引

**设计原则**:
- 所有表使用 `varchar(32)` 作为主键
- 主键值使用UUID格式，确保全局唯一
- 支持分布式环境下的ID生成

### 2. 唯一索引

```sql
-- 业务唯一性约束
t_aep_products: uk_aep_product_id (aep_product_id)
t_aep_devices: uk_aep_device_id (aep_device_id)
t_aep_operations: uk_operation_id (operation_id)
t_aep_platforms: uk_platform_name_env (platform_name, environment)

-- 业务组合唯一约束
t_aep_products: uk_product_name_status (product_name, status)
t_aep_statistics: uk_stat_unique (stat_date, stat_hour, category, metric_name, ...)
```

### 3. 查询优化索引

**高频查询索引**:
```sql
-- 设备状态查询
t_aep_devices: idx_device_status, idx_sync_status

-- 时间范围查询
t_aep_operations: idx_start_time
t_aep_device_data: idx_receive_time

-- 关联查询优化
t_aep_devices: idx_aep_product_id, idx_local_device_id
t_aep_operations: idx_operator_id, idx_platform_id

-- 复合查询索引
t_aep_operations: idx_complex_query (operation_type, resource_type, operation_status, start_time)
```

### 4. 分区策略

**时间分区**:
```sql
-- 操作日志按月分区
t_aep_operations: PARTITION BY RANGE (YEAR(start_time)*100 + MONTH(start_time))

-- 设备数据按周分区
t_aep_device_data: PARTITION BY RANGE (TO_DAYS(receive_time))

-- 统计数据按年分区
t_aep_statistics: PARTITION BY RANGE (YEAR(stat_date))
```

## 📈 监控指标

### 1. 业务监控

**关键指标**:
- 产品注册成功率
- 设备在线率
- 数据同步成功率
- API调用成功率

**监控SQL**:
```sql
-- API成功率（最近1小时）
SELECT
    operation_type,
    COUNT(*) as total,
    SUM(CASE WHEN operation_status = 'SUCCESS' THEN 1 ELSE 0 END) as success,
    ROUND(SUM(CASE WHEN operation_status = 'SUCCESS' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as success_rate
FROM t_aep_operations
WHERE start_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY operation_type;

-- 设备在线率
SELECT
    COUNT(*) as total_devices,
    SUM(CASE WHEN device_status = 'ONLINE' THEN 1 ELSE 0 END) as online_devices,
    ROUND(SUM(CASE WHEN device_status = 'ONLINE' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as online_rate
FROM t_aep_devices;
```

### 2. 性能监控

**关键指标**:
- API平均响应时间
- 数据库查询性能
- 存储空间使用率
- 并发连接数

**监控阈值**:
- API响应时间 > 5秒: 告警
- 数据库连接数 > 80%: 告警
- 磁盘使用率 > 85%: 告警
- 错误率 > 5%: 告警

## 🔄 数据生命周期

### 1. 数据分类

**热数据** (0-30天):
- `t_aep_operations` - 最近30天的操作记录
- `t_aep_device_data` - 最近30天的设备数据
- `t_aep_sync_status` - 活跃的同步任务

**温数据** (30-90天):
- 历史操作记录
- 设备历史数据
- 完成的同步任务记录

**冷数据** (90天+):
- 归档的操作日志
- 历史统计数据
- 已删除的资源记录

### 2. 数据归档策略

**自动归档**:
```sql
-- 每日凌晨2点执行数据归档
-- 将90天前的热数据迁移到归档表
CREATE EVENT ev_daily_archive
ON SCHEDULE EVERY 1 DAY
STARTS '2026-01-26 02:00:00'
DO
  CALL sp_archive_old_data(90);
```

**归档表结构**:
```sql
-- 归档表命名规则：原表名 + _archive_YYYYMM
t_aep_operations_archive_202601
t_aep_device_data_archive_202601
```

### 3. 数据清理策略

**清理规则**:
- 操作日志：保留180天
- 设备数据：保留365天
- 统计数据：永久保留
- 同步状态：成功记录保留30天，失败记录保留90天

**自动清理**:
```sql
-- 每周日凌晨执行数据清理
CREATE EVENT ev_weekly_cleanup
ON SCHEDULE EVERY 1 WEEK
STARTS '2026-01-26 03:00:00'
DO
  CALL sp_aep_cleanup_expired_data(180);
```

## 🚀 部署指南

### 1. 环境要求

**数据库版本**: MySQL 8.0+
**字符集**: utf8mb3
**存储引擎**: InnoDB
**最小硬件配置**:
- CPU: 4核心
- 内存: 8GB
- 存储: 100GB SSD

### 2. 部署步骤

```bash
# 1. 创建数据库
mysql -u root -p
CREATE DATABASE aep_integration_db CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;

# 2. 创建用户并授权
CREATE USER 'aep_user'@'%' IDENTIFIED BY 'secure_password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, INDEX, ALTER ON aep_integration_db.* TO 'aep_user'@'%';
FLUSH PRIVILEGES;

# 3. 执行建表脚本
mysql -u aep_user -p aep_integration_db < schema_v2.sql

# 4. 验证部署
mysql -u aep_user -p aep_integration_db -e "SHOW TABLES;"
```

### 3. 配置优化

**MySQL配置优化**:
```ini
# my.cnf 优化配置
[mysqld]
innodb_buffer_pool_size = 4G
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2
max_connections = 500
query_cache_size = 256M
slow_query_log = ON
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
```

**索引优化**:
```sql
-- 分析表统计信息
ANALYZE TABLE t_aep_operations;
ANALYZE TABLE t_aep_devices;
ANALYZE TABLE t_aep_device_data;

-- 检查索引使用情况
SHOW INDEX FROM t_aep_operations;
SHOW INDEX FROM t_aep_devices;
```

## 📊 性能测试

### 1. 基准测试

**测试场景**:
- 1000个产品，每个产品1000台设备
- 每秒1000次API调用
- 每秒10000条设备数据写入

**性能目标**:
- API响应时间 < 100ms (95%)
- 数据插入TPS > 50000
- 查询响应时间 < 50ms
- 并发连接数 > 1000

### 2. 压力测试

**测试工具**: sysbench, JMeter
**测试内容**:
```bash
# 数据库连接压力测试
sysbench /usr/share/sysbench/oltp_read_write.lua \
  --db-driver=mysql \
  --mysql-host=localhost \
  --mysql-user=aep_user \
  --mysql-password=password \
  --mysql-db=aep_integration_db \
  --tables=10 \
  --table-size=100000 \
  --threads=100 \
  --time=300 \
  run
```

## 🔍 故障排查

### 1. 常见问题

**连接问题**:
```sql
-- 检查连接数
SHOW STATUS LIKE 'Threads_connected';
SHOW VARIABLES LIKE 'max_connections';

-- 检查慢查询
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;
```

**性能问题**:
```sql
-- 检查锁等待
SHOW ENGINE INNODB STATUS;

-- 检查表大小
SELECT
  table_name,
  ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size MB'
FROM information_schema.TABLES
WHERE table_schema = 'aep_integration_db'
ORDER BY (data_length + index_length) DESC;
```

### 2. 监控脚本

**健康检查脚本**:
```bash
#!/bin/bash
# health_check.sh

# 检查数据库连接
mysql -u aep_user -p$DB_PASSWORD -h $DB_HOST -e "SELECT 1;" > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "ERROR: Cannot connect to database"
    exit 1
fi

# 检查表大小
TABLE_SIZE=$(mysql -u aep_user -p$DB_PASSWORD $DB_NAME -e "
SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024) AS size_mb
FROM information_schema.tables
WHERE table_schema = '$DB_NAME';" | tail -1)

if [ $TABLE_SIZE -gt 10000 ]; then
    echo "WARNING: Database size is ${TABLE_SIZE}MB"
fi

echo "OK: Database is healthy"
```

## 📚 参考资料

### 1. 相关文档
- [AEP平台API文档](../../reference/267848_sdk/doc/)
- [现有系统数据库设计](../../../../toyou/zc_backend/db_control_sys.sql)
- [项目架构设计](./architecture-design.md)

### 2. 技术规范
- [MySQL 8.0 Reference Manual](https://dev.mysql.com/doc/refman/8.0/en/)
- [数据库设计规范](https://github.com/alibaba/p3c)
- [安全设计指南](https://owasp.org/www-project-database-security/)

### 3. 运维文档
- [数据库运维手册](./DATABASE_GUIDE.md)
- [监控配置指南](./monitoring-guide.md)
- [备份恢复流程](./backup-recovery-guide.md)

---

**文档维护**：
- **更新频率**: 跟随版本发布更新
- **维护团队**: 云监控平台技术团队
- **文档状态**: 🟢 当前版本 | ⚡ 生产就绪

**版本历史**：
- v2.0 (2026-01-25): 基于现有系统架构的完整设计
- v1.0 (2026-01-24): 初始独立设计方案