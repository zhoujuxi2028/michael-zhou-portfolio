-- AEP Integration Database Schema
-- 设计版本: v1.0
-- 创建日期: 2026-01-25
-- 目标数据库: MySQL 8.0+

-- 创建数据库
CREATE DATABASE IF NOT EXISTS aep_integration
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE aep_integration;

-- =================================================================
-- 1. 产品管理表
-- =================================================================

-- 1.1 产品注册表
DROP TABLE IF EXISTS aep_products;
CREATE TABLE aep_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT 'AEP平台产品ID',
    product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
    device_type VARCHAR(32) NOT NULL COMMENT '设备类型：SENSOR/GATEWAY/DEVICE等',
    network_type VARCHAR(32) COMMENT '网络类型：NB-IOT/2G/3G/4G/5G等',
    data_format INTEGER NOT NULL DEFAULT 1 COMMENT '数据格式：1=JSON, 2=二进制',
    industry_id INTEGER COMMENT '行业ID',
    description TEXT COMMENT '产品描述',
    device_model VARCHAR(128) COMMENT '设备型号',
    manufacturer VARCHAR(128) COMMENT '制造商',
    protocol_type VARCHAR(32) COMMENT '协议类型：CoAP/MQTT等',
    max_device_count INTEGER COMMENT '最大设备数量',
    enable_security BOOLEAN DEFAULT FALSE COMMENT '是否启用安全认证',
    auto_create_device BOOLEAN DEFAULT FALSE COMMENT '是否自动创建设备',
    master_key VARCHAR(256) COMMENT 'AEP产品主密钥(加密存储)',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE/DELETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(64) COMMENT '创建者',
    UNIQUE KEY uk_product_id (product_id),
    UNIQUE KEY uk_product_name (product_name, status),
    INDEX idx_device_type (device_type),
    INDEX idx_network_type (network_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='AEP产品注册信息表';

-- 1.2 产品配置表
DROP TABLE IF EXISTS aep_product_configs;
CREATE TABLE aep_product_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '关联aep_products.product_id',
    config_key VARCHAR(128) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT '配置类型：STRING/JSON/INTEGER/BOOLEAN',
    description TEXT COMMENT '配置描述',
    is_encrypted BOOLEAN DEFAULT FALSE COMMENT '是否加密存储',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_config (product_id, config_key),
    FOREIGN KEY (product_id) REFERENCES aep_products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='AEP产品配置表';

-- =================================================================
-- 2. 操作审计表
-- =================================================================

-- 2.1 API操作记录表
DROP TABLE IF EXISTS aep_operation_logs;
CREATE TABLE aep_operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_id VARCHAR(64) NOT NULL COMMENT '操作唯一标识',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/QUERY',
    resource_type VARCHAR(32) NOT NULL COMMENT '资源类型：PRODUCT/DEVICE/SUBSCRIPTION',
    resource_id VARCHAR(128) COMMENT '资源ID',
    resource_name VARCHAR(256) COMMENT '资源名称',
    operation_status VARCHAR(16) NOT NULL COMMENT '操作状态：SUCCESS/FAILED/PENDING',
    error_code VARCHAR(32) COMMENT '错误码',
    error_message TEXT COMMENT '错误信息',
    request_params JSON COMMENT '请求参数(脱敏)',
    response_data JSON COMMENT '响应数据(脱敏)',
    execution_time_ms INTEGER COMMENT '执行时长(毫秒)',
    start_time TIMESTAMP(3) NOT NULL COMMENT '开始时间',
    end_time TIMESTAMP(3) COMMENT '结束时间',
    operator VARCHAR(64) COMMENT '操作者',
    client_ip VARCHAR(64) COMMENT '客户端IP',
    user_agent VARCHAR(512) COMMENT '用户代理',
    INDEX idx_operation_type (operation_type),
    INDEX idx_resource_type (resource_type),
    INDEX idx_operation_status (operation_status),
    INDEX idx_start_time (start_time),
    INDEX idx_operator (operator),
    UNIQUE KEY uk_operation_id (operation_id)
) ENGINE=InnoDB COMMENT='AEP API操作审计日志';

-- 2.2 系统事件日志表
DROP TABLE IF EXISTS aep_system_logs;
CREATE TABLE aep_system_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    log_level VARCHAR(16) NOT NULL COMMENT '日志级别：DEBUG/INFO/WARN/ERROR',
    logger_name VARCHAR(256) COMMENT '日志记录器名称',
    message TEXT NOT NULL COMMENT '日志消息',
    exception_info TEXT COMMENT '异常信息',
    thread_name VARCHAR(128) COMMENT '线程名称',
    timestamp TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_log_level (log_level),
    INDEX idx_timestamp (timestamp),
    INDEX idx_logger_name (logger_name)
) ENGINE=InnoDB COMMENT='系统日志表';

-- =================================================================
-- 3. 配置管理表
-- =================================================================

-- 3.1 应用配置表
DROP TABLE IF EXISTS aep_app_configs;
CREATE TABLE aep_app_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_group VARCHAR(64) NOT NULL COMMENT '配置组：database/aep_api/retry_policy等',
    config_key VARCHAR(128) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT '配置类型',
    description TEXT COMMENT '配置描述',
    is_encrypted BOOLEAN DEFAULT FALSE COMMENT '是否加密',
    is_required BOOLEAN DEFAULT FALSE COMMENT '是否必需配置',
    default_value TEXT COMMENT '默认值',
    validation_rule VARCHAR(512) COMMENT '验证规则(正则表达式)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_config_group_key (config_group, config_key),
    INDEX idx_config_group (config_group)
) ENGINE=InnoDB COMMENT='应用配置管理表';

-- 3.2 环境配置表
DROP TABLE IF EXISTS aep_environments;
CREATE TABLE aep_environments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    env_name VARCHAR(32) NOT NULL COMMENT '环境名称：dev/test/staging/prod',
    env_alias VARCHAR(64) COMMENT '环境别名',
    aep_api_host VARCHAR(128) NOT NULL COMMENT 'AEP API主机地址',
    aep_app_id VARCHAR(64) NOT NULL COMMENT 'AEP应用ID',
    aep_app_key_encrypted TEXT NOT NULL COMMENT 'AEP应用KEY(加密)',
    aep_app_secret_encrypted TEXT NOT NULL COMMENT 'AEP应用SECRET(加密)',
    max_retries INTEGER DEFAULT 3 COMMENT '最大重试次数',
    timeout_ms INTEGER DEFAULT 30000 COMMENT '请求超时时间(毫秒)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    description TEXT COMMENT '环境描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_env_name (env_name)
) ENGINE=InnoDB COMMENT='AEP环境配置表';

-- =================================================================
-- 4. 统计监控表
-- =================================================================

-- 4.1 操作统计表(按天聚合)
DROP TABLE IF EXISTS aep_operation_stats;
CREATE TABLE aep_operation_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date DATE NOT NULL COMMENT '统计日期',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型',
    resource_type VARCHAR(32) NOT NULL COMMENT '资源类型',
    total_count INTEGER NOT NULL DEFAULT 0 COMMENT '总操作数',
    success_count INTEGER NOT NULL DEFAULT 0 COMMENT '成功操作数',
    failed_count INTEGER NOT NULL DEFAULT 0 COMMENT '失败操作数',
    avg_execution_time_ms INTEGER COMMENT '平均执行时长',
    max_execution_time_ms INTEGER COMMENT '最大执行时长',
    min_execution_time_ms INTEGER COMMENT '最小执行时长',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stat_date_operation_resource (stat_date, operation_type, resource_type),
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB COMMENT='AEP操作统计表';

-- 4.2 错误统计表
DROP TABLE IF EXISTS aep_error_stats;
CREATE TABLE aep_error_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date DATE NOT NULL COMMENT '统计日期',
    error_code VARCHAR(32) NOT NULL COMMENT '错误码',
    operation_type VARCHAR(32) COMMENT '操作类型',
    error_count INTEGER NOT NULL DEFAULT 0 COMMENT '错误次数',
    first_occurrence TIMESTAMP COMMENT '首次出现时间',
    last_occurrence TIMESTAMP COMMENT '最后出现时间',
    sample_error_message TEXT COMMENT '错误消息样例',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stat_date_error_operation (stat_date, error_code, operation_type),
    INDEX idx_stat_date (stat_date),
    INDEX idx_error_code (error_code)
) ENGINE=InnoDB COMMENT='AEP错误统计表';

-- =================================================================
-- 5. 初始化数据
-- =================================================================

-- 5.1 插入默认环境配置（需要手动更新实际值）
INSERT INTO aep_environments (env_name, env_alias, aep_api_host, aep_app_id, aep_app_key_encrypted, aep_app_secret_encrypted, description) VALUES
('dev', '开发环境', 'your_tenant.api.ctwing.cn', '267848', 'ENCRYPTED_KEY_PLACEHOLDER', 'ENCRYPTED_SECRET_PLACEHOLDER', 'AEP开发环境配置'),
('test', '测试环境', 'your_tenant.api.ctwing.cn', '267848', 'ENCRYPTED_KEY_PLACEHOLDER', 'ENCRYPTED_SECRET_PLACEHOLDER', 'AEP测试环境配置'),
('prod', '生产环境', 'your_tenant.api.ctwing.cn', '267848', 'ENCRYPTED_KEY_PLACEHOLDER', 'ENCRYPTED_SECRET_PLACEHOLDER', 'AEP生产环境配置');

-- 5.2 插入默认应用配置
INSERT INTO aep_app_configs (config_group, config_key, config_value, config_type, description, is_required) VALUES
-- 数据库配置
('database', 'connection_pool_size', '10', 'INTEGER', '数据库连接池大小', TRUE),
('database', 'connection_timeout_ms', '30000', 'INTEGER', '数据库连接超时时间', TRUE),
('database', 'query_timeout_ms', '60000', 'INTEGER', '数据库查询超时时间', TRUE),

-- AEP API配置
('aep_api', 'default_retry_count', '3', 'INTEGER', '默认重试次数', TRUE),
('aep_api', 'default_timeout_ms', '30000', 'INTEGER', '默认请求超时时间', TRUE),
('aep_api', 'enable_request_logging', 'true', 'BOOLEAN', '启用请求日志记录', FALSE),
('aep_api', 'enable_response_logging', 'true', 'BOOLEAN', '启用响应日志记录', FALSE),

-- 重试策略配置
('retry_policy', 'exponential_backoff_base', '1000', 'INTEGER', '指数退避基础时间(毫秒)', TRUE),
('retry_policy', 'exponential_backoff_max', '30000', 'INTEGER', '指数退避最大时间(毫秒)', TRUE),
('retry_policy', 'max_retry_attempts', '5', 'INTEGER', '最大重试次数', TRUE),

-- 安全配置
('security', 'encryption_algorithm', 'AES-256-GCM', 'STRING', '加密算法', TRUE),
('security', 'sensitive_data_retention_days', '30', 'INTEGER', '敏感数据保留天数', TRUE),

-- 监控配置
('monitoring', 'stats_aggregation_interval_hours', '1', 'INTEGER', '统计聚合间隔(小时)', TRUE),
('monitoring', 'log_retention_days', '90', 'INTEGER', '日志保留天数', TRUE),
('monitoring', 'enable_performance_monitoring', 'true', 'BOOLEAN', '启用性能监控', FALSE);

-- =================================================================
-- 6. 创建数据库用户和权限（可选）
-- =================================================================

-- 创建应用专用数据库用户
-- CREATE USER 'aep_user'@'localhost' IDENTIFIED BY 'secure_password_here';
-- CREATE USER 'aep_user'@'%' IDENTIFIED BY 'secure_password_here';

-- 授予权限
-- GRANT SELECT, INSERT, UPDATE, DELETE ON aep_integration.* TO 'aep_user'@'localhost';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON aep_integration.* TO 'aep_user'@'%';

-- 刷新权限
-- FLUSH PRIVILEGES;

-- =================================================================
-- 7. 创建视图（便于查询）
-- =================================================================

-- 7.1 产品概览视图
CREATE VIEW v_product_overview AS
SELECT
    p.id,
    p.product_id,
    p.product_name,
    p.device_type,
    p.network_type,
    p.status,
    p.created_at,
    p.updated_at,
    COALESCE(device_count.count, 0) as current_device_count,
    p.max_device_count,
    CASE
        WHEN p.max_device_count > 0 THEN ROUND((COALESCE(device_count.count, 0) * 100.0 / p.max_device_count), 2)
        ELSE 0
    END as device_usage_percentage
FROM aep_products p
LEFT JOIN (
    -- 这里假设后续会有设备表来统计设备数量
    SELECT product_id, 0 as count FROM aep_products WHERE 1=0
) device_count ON p.product_id = device_count.product_id
WHERE p.status = 'ACTIVE';

-- 7.2 操作统计视图
CREATE VIEW v_operation_summary AS
SELECT
    DATE(start_time) as operation_date,
    operation_type,
    resource_type,
    COUNT(*) as total_operations,
    SUM(CASE WHEN operation_status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count,
    SUM(CASE WHEN operation_status = 'FAILED' THEN 1 ELSE 0 END) as failed_count,
    ROUND(AVG(execution_time_ms), 2) as avg_execution_time,
    MAX(execution_time_ms) as max_execution_time,
    MIN(execution_time_ms) as min_execution_time
FROM aep_operation_logs
WHERE start_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY DATE(start_time), operation_type, resource_type
ORDER BY operation_date DESC, operation_type;

-- =================================================================
-- 8. 创建存储过程（数据维护）
-- =================================================================

-- 8.1 清理过期日志存储过程
DELIMITER $$
CREATE PROCEDURE sp_cleanup_expired_logs(IN retention_days INTEGER)
BEGIN
    DECLARE done INT DEFAULT FALSE;

    START TRANSACTION;

    -- 清理过期操作日志
    DELETE FROM aep_operation_logs
    WHERE start_time < DATE_SUB(CURRENT_DATE, INTERVAL retention_days DAY);

    -- 清理过期系统日志
    DELETE FROM aep_system_logs
    WHERE timestamp < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL retention_days DAY);

    -- 清理过期错误统计（保留更长时间）
    DELETE FROM aep_error_stats
    WHERE stat_date < DATE_SUB(CURRENT_DATE, INTERVAL (retention_days * 3) DAY);

    COMMIT;

    SELECT 'Log cleanup completed' as result;
END$$
DELIMITER ;

-- 8.2 聚合统计数据存储过程
DELIMITER $$
CREATE PROCEDURE sp_aggregate_daily_stats(IN stat_date DATE)
BEGIN
    -- 删除当天已有统计
    DELETE FROM aep_operation_stats WHERE stat_date = stat_date;

    -- 重新计算并插入统计数据
    INSERT INTO aep_operation_stats (
        stat_date, operation_type, resource_type, total_count, success_count,
        failed_count, avg_execution_time_ms, max_execution_time_ms, min_execution_time_ms
    )
    SELECT
        stat_date,
        operation_type,
        resource_type,
        COUNT(*) as total_count,
        SUM(CASE WHEN operation_status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count,
        SUM(CASE WHEN operation_status = 'FAILED' THEN 1 ELSE 0 END) as failed_count,
        ROUND(AVG(execution_time_ms)) as avg_execution_time_ms,
        MAX(execution_time_ms) as max_execution_time_ms,
        MIN(execution_time_ms) as min_execution_time_ms
    FROM aep_operation_logs
    WHERE DATE(start_time) = stat_date
    GROUP BY operation_type, resource_type;

    SELECT CONCAT('Daily stats aggregated for ', stat_date) as result;
END$$
DELIMITER ;

-- =================================================================
-- 9. 创建索引优化
-- =================================================================

-- 为经常查询的字段创建复合索引
CREATE INDEX idx_operation_logs_complex ON aep_operation_logs (operation_type, resource_type, operation_status, start_time);
CREATE INDEX idx_products_complex ON aep_products (device_type, network_type, status, created_at);

-- =================================================================
-- 完成数据库schema创建
-- =================================================================

SELECT 'AEP Integration Database Schema created successfully!' as message;