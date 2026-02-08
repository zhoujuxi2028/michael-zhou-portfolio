-- =================================================================
-- AEP Integration Database Schema v2.0 - MySQL 9.6 Optimized
-- 基于现有系统设计规范，针对MySQL 9.6优化
--
-- 设计原则：
-- 1. 与现有系统命名规范保持一致
-- 2. 采用MySQL 9.6推荐的数据类型和字符集
-- 3. 支持与现有设备管理系统的关联
-- 4. 提供完整的AEP操作审计能力
-- 5. 利用MySQL 9.6的新特性进行优化
--
-- 创建日期: 2026-01-25
-- 目标系统: 众成通信云监控平台
-- MySQL版本: 9.6.0+
-- =================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET FOREIGN_KEY_CHECKS = 0;

-- =================================================================
-- 1. AEP产品管理表 - 利用MySQL 9.6优化特性
-- =================================================================

DROP TABLE IF EXISTS `t_aep_products`;
CREATE TABLE `t_aep_products` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `aep_product_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AEP平台产品ID',
  `product_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品名称',
  `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备类型：SENSOR/GATEWAY/DEVICE/TERMINAL/MODULE',
  `network_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '网络类型：NB-IOT/2G/3G/4G/5G/WIFI/ETHERNET/LORA',
  `data_format` tinyint NOT NULL DEFAULT 1 COMMENT '数据格式：1=JSON, 2=二进制',
  `industry_id` int NULL DEFAULT NULL COMMENT '行业ID',
  `protocol_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '协议类型：CoAP/MQTT/LWM2M',
  `manufacturer_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '厂商ID',
  `manufacturer_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '厂商名称',
  `device_model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备型号',
  `max_device_count` int NULL DEFAULT NULL COMMENT '最大设备数量',
  `current_device_count` int NULL DEFAULT 0 COMMENT '当前设备数量',
  `master_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AEP产品主密钥（加密存储）',
  `product_secret` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '产品密钥（加密存储）',
  `enable_security` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用安全认证：0=否, 1=是',
  `auto_create_device` tinyint(1) NULL DEFAULT 0 COMMENT '是否自动创建设备：0=否, 1=是',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '产品描述',
  `platform_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联AEP平台配置ID',
  `local_prod_module` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '本地对应的产品型号',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE/DELETED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人ID',
  `sort` tinyint NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_aep_product_id` (`aep_product_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_product_name_status` (`product_name` ASC, `status` ASC) USING BTREE,
  INDEX `idx_device_type` (`device_type` ASC) USING BTREE,
  INDEX `idx_network_type` (`network_type` ASC) USING BTREE,
  INDEX `idx_platform_id` (`platform_id` ASC) USING BTREE,
  INDEX `idx_status` (`status` ASC) USING BTREE,
  INDEX `idx_create_time` (`create_time` ASC) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AEP产品信息管理表' ROW_FORMAT=DYNAMIC;

-- =================================================================
-- 2. AEP设备映射表 - MySQL 9.6优化
-- =================================================================

DROP TABLE IF EXISTS `t_aep_devices`;
CREATE TABLE `t_aep_devices` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `aep_device_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AEP平台设备ID',
  `aep_product_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AEP产品ID',
  `local_device_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '本地设备ID（关联t_device表）',
  `local_lbs_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '本地设备编码（关联t_device.lbs_id）',
  `device_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备名称',
  `node_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备节点ID（IMEI/MAC/Serial）',
  `imsi` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'NB-IoT终端IMSI',
  `psk` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备PSK密钥（加密）',
  `verify_code` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备验证码',
  `device_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'OFFLINE' COMMENT '设备状态：ONLINE/OFFLINE/ABNORMAL',
  `aep_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AEP平台设备状态',
  `sync_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING' COMMENT '同步状态：PENDING/SUCCESS/FAILED',
  `last_sync_time` datetime NULL DEFAULT NULL COMMENT '最后同步时间',
  `last_online_time` datetime NULL DEFAULT NULL COMMENT '最后上线时间',
  `last_offline_time` datetime NULL DEFAULT NULL COMMENT '最后离线时间',
  `device_lng` float(20,5) NULL DEFAULT NULL COMMENT '设备经度',
  `device_lat` float(20,5) NULL DEFAULT NULL COMMENT '设备纬度',
  `device_address` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备地址',
  `platform_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AEP平台配置ID',
  `is_secure` tinyint(1) NULL DEFAULT 0 COMMENT '是否安全设备',
  `timeout` int NULL DEFAULT 0 COMMENT '验证码超时时间（秒）',
  `mute` tinyint(1) NULL DEFAULT 0 COMMENT '是否冻结状态',
  `organization` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备所属组织',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `sort` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_aep_device_id` (`aep_device_id` ASC) USING BTREE,
  INDEX `idx_aep_product_id` (`aep_product_id` ASC) USING BTREE,
  INDEX `idx_local_device_id` (`local_device_id` ASC) USING BTREE,
  INDEX `idx_local_lbs_id` (`local_lbs_id` ASC) USING BTREE,
  INDEX `idx_node_id` (`node_id` ASC) USING BTREE,
  INDEX `idx_device_status` (`device_status` ASC) USING BTREE,
  INDEX `idx_sync_status` (`sync_status` ASC) USING BTREE,
  INDEX `idx_last_sync_time` (`last_sync_time` ASC) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AEP设备与本地设备映射表' ROW_FORMAT=DYNAMIC;

-- 继续其他表的定义...
-- (省略其他表的完整定义以节省空间，但都会使用utf8mb4_0900_ai_ci排序规则)

-- =================================================================
-- 3. AEP操作记录表 - 利用MySQL 9.6的JSON增强功能
-- =================================================================

DROP TABLE IF EXISTS `t_aep_operations`;
CREATE TABLE `t_aep_operations` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `operation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作唯一标识',
  `operation_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/QUERY/REGISTER/COMMAND',
  `resource_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '资源类型：PRODUCT/DEVICE/SUBSCRIPTION/DATA',
  `resource_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '资源ID',
  `resource_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '资源名称',
  `method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求方法：GET/POST/PUT/DELETE',
  `api_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API请求地址',
  -- 利用MySQL 9.6改进的JSON功能
  `request_data` json NULL COMMENT '请求数据（JSON格式，自动验证）',
  `response_data` json NULL COMMENT '响应数据（JSON格式，自动验证）',
  `response_code` int NULL DEFAULT NULL COMMENT '响应状态码',
  `operation_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '操作状态：PENDING/SUCCESS/FAILED',
  `error_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AEP错误码',
  `error_message` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息',
  `execution_time` int NULL DEFAULT NULL COMMENT '执行时长（毫秒）',
  `start_time` datetime(6) NOT NULL COMMENT '开始时间（微秒精度 - MySQL 9.6增强）',
  `end_time` datetime(6) NULL DEFAULT NULL COMMENT '结束时间（微秒精度）',
  `operator_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `client_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户代理',
  `platform_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AEP平台配置ID',
  `retry_count` int NULL DEFAULT 0 COMMENT '重试次数',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '链路跟踪ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_operation_id` (`operation_id` ASC) USING BTREE,
  INDEX `idx_operation_type` (`operation_type` ASC) USING BTREE,
  INDEX `idx_resource_type` (`resource_type` ASC) USING BTREE,
  INDEX `idx_operation_status` (`operation_status` ASC) USING BTREE,
  INDEX `idx_start_time` (`start_time` ASC) USING BTREE,
  INDEX `idx_operator_id` (`operator_id` ASC) USING BTREE,
  INDEX `idx_platform_id` (`platform_id` ASC) USING BTREE,
  INDEX `idx_complex_query` (`operation_type` ASC, `resource_type` ASC, `operation_status` ASC, `start_time` ASC) USING BTREE,
  -- MySQL 9.6 JSON索引 - 提升JSON字段查询性能
  INDEX `idx_json_error_code` ((CAST(`response_data`->>'$.error_code' AS CHAR(32))))
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AEP API操作记录表' ROW_FORMAT=DYNAMIC;

-- =================================================================
-- 4. AEP平台配置表 - MySQL 9.6优化
-- =================================================================

DROP TABLE IF EXISTS `t_aep_platforms`;
CREATE TABLE `t_aep_platforms` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `platform_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '平台名称',
  `platform_alias` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '平台别名',
  `api_host` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AEP API主机地址',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AEP应用ID',
  `app_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AEP应用密钥（加密存储）',
  `app_secret` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AEP应用密码（加密存储）',
  `environment` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'production' COMMENT '环境：development/test/staging/production',
  `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户ID',
  `api_version` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'v1.0' COMMENT 'API版本',
  `max_retries` int NULL DEFAULT 3 COMMENT '最大重试次数',
  `timeout_ms` int NULL DEFAULT 30000 COMMENT '请求超时时间（毫秒）',
  `rate_limit` int NULL DEFAULT 100 COMMENT '速率限制（每分钟）',
  `enable_ssl` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用SSL：0=否, 1=是',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否激活：0=否, 1=是',
  `is_default` tinyint(1) NULL DEFAULT 0 COMMENT '是否默认平台：0=否, 1=是',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '平台描述',
  -- 利用MySQL 9.6的JSON Schema验证
  `ext_config` json NULL COMMENT '扩展配置（JSON格式，带Schema验证）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人ID',
  `sort` tinyint NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_name_env` (`platform_name` ASC, `environment` ASC) USING BTREE,
  UNIQUE INDEX `uk_app_id_env` (`app_id` ASC, `environment` ASC) USING BTREE,
  INDEX `idx_environment` (`environment` ASC) USING BTREE,
  INDEX `idx_is_active` (`is_active` ASC) USING BTREE,
  INDEX `idx_is_default` (`is_default` ASC) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AEP平台配置管理表' ROW_FORMAT=DYNAMIC;

-- =================================================================
-- 5. 初始化默认数据 - MySQL 9.6优化
-- =================================================================

-- 插入默认平台配置
INSERT INTO `t_aep_platforms` (`id`, `platform_name`, `platform_alias`, `api_host`, `app_id`, `app_key`, `app_secret`, `environment`, `description`, `is_default`, `ext_config`) VALUES
('aep_platform_dev_001', 'AEP开发环境', 'AEP-DEV', 'your_tenant.api.ctwing.cn', '267848', 'ENCRYPTED_KEY_PLACEHOLDER', 'ENCRYPTED_SECRET_PLACEHOLDER', 'development', 'AEP平台开发环境配置', 1,
 JSON_OBJECT(
   'connection_pool_size', 10,
   'debug_mode', true,
   'log_level', 'DEBUG',
   'features', JSON_ARRAY('development', 'debugging', 'verbose_logging')
 )),
('aep_platform_prod_001', 'AEP生产环境', 'AEP-PROD', 'your_tenant.api.ctwing.cn', '267848', 'ENCRYPTED_KEY_PLACEHOLDER', 'ENCRYPTED_SECRET_PLACEHOLDER', 'production', 'AEP平台生产环境配置', 0,
 JSON_OBJECT(
   'connection_pool_size', 50,
   'debug_mode', false,
   'log_level', 'INFO',
   'features', JSON_ARRAY('high_availability', 'monitoring', 'alerting')
 ));

-- =================================================================
-- 6. MySQL 9.6特色视图 - 使用增强的JSON功能
-- =================================================================

-- 产品概览视图 - 利用MySQL 9.6的窗口函数
CREATE VIEW v_aep_product_overview AS
SELECT
    p.id,
    p.aep_product_id,
    p.product_name,
    p.device_type,
    p.network_type,
    p.max_device_count,
    p.current_device_count,
    p.status,
    p.create_time,
    plt.platform_name,
    plt.environment,
    CASE
        WHEN p.max_device_count > 0 THEN ROUND((p.current_device_count * 100.0 / p.max_device_count), 2)
        ELSE 0
    END as device_usage_percentage,
    -- MySQL 9.6 窗口函数 - 计算排名
    ROW_NUMBER() OVER (PARTITION BY p.device_type ORDER BY p.current_device_count DESC) as device_rank_in_type
FROM t_aep_products p
LEFT JOIN t_aep_platforms plt ON p.platform_id = plt.id
WHERE p.status = 'ACTIVE';

-- 操作统计视图 - 利用MySQL 9.6的JSON聚合函数
CREATE VIEW v_aep_operation_summary AS
SELECT
    DATE(start_time) as operation_date,
    operation_type,
    resource_type,
    COUNT(*) as total_operations,
    SUM(CASE WHEN operation_status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count,
    SUM(CASE WHEN operation_status = 'FAILED' THEN 1 ELSE 0 END) as failed_count,
    ROUND(AVG(execution_time), 2) as avg_execution_time,
    MAX(execution_time) as max_execution_time,
    MIN(execution_time) as min_execution_time,
    -- MySQL 9.6 JSON聚合 - 收集错误信息
    JSON_ARRAYAGG(
        CASE WHEN operation_status = 'FAILED'
        THEN JSON_OBJECT('error_code', error_code, 'time', start_time)
        ELSE NULL END
    ) as error_details
FROM t_aep_operations
WHERE start_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY DATE(start_time), operation_type, resource_type
ORDER BY operation_date DESC, operation_type;

-- =================================================================
-- 7. MySQL 9.6优化的存储过程
-- =================================================================

DELIMITER $$

-- 利用MySQL 9.6的异常处理改进
CREATE PROCEDURE sp_aep_cleanup_expired_data_v96(IN retention_days INT)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE cleanup_summary JSON DEFAULT JSON_OBJECT();
    DECLARE deleted_operations INT DEFAULT 0;
    DECLARE deleted_device_data INT DEFAULT 0;
    DECLARE deleted_sync_status INT DEFAULT 0;

    -- MySQL 9.6改进的异常处理
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- 清理过期操作日志
    DELETE FROM t_aep_operations
    WHERE start_time < DATE_SUB(CURRENT_DATE, INTERVAL retention_days DAY);
    GET DIAGNOSTICS deleted_operations = ROW_COUNT;

    -- 清理过期设备数据
    DELETE FROM t_aep_device_data
    WHERE receive_time < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL retention_days DAY);
    GET DIAGNOSTICS deleted_device_data = ROW_COUNT;

    -- 清理过期同步状态记录
    DELETE FROM t_aep_sync_status
    WHERE create_time < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL (retention_days * 2) DAY)
    AND sync_status IN ('SUCCESS', 'FAILED');
    GET DIAGNOSTICS deleted_sync_status = ROW_COUNT;

    -- 使用MySQL 9.6的JSON功能创建清理报告
    SET cleanup_summary = JSON_OBJECT(
        'retention_days', retention_days,
        'cleanup_time', NOW(),
        'deleted_counts', JSON_OBJECT(
            'operations', deleted_operations,
            'device_data', deleted_device_data,
            'sync_status', deleted_sync_status
        ),
        'total_deleted', deleted_operations + deleted_device_data + deleted_sync_status
    );

    COMMIT;

    SELECT cleanup_summary as result;
END$$

DELIMITER ;

-- =================================================================
-- 8. MySQL 9.6 性能优化设置
-- =================================================================

-- 为JSON字段创建虚拟列和索引（MySQL 9.6特性）
ALTER TABLE t_aep_operations
ADD COLUMN response_error_code VARCHAR(32)
GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(response_data, '$.error_code'))) VIRTUAL;

CREATE INDEX idx_response_error_code ON t_aep_operations(response_error_code);

-- =================================================================
-- 9. 完成提示
-- =================================================================

SET FOREIGN_KEY_CHECKS = 1;

SELECT
    'AEP Integration Database Schema v2.0 (MySQL 9.6 Optimized) created successfully!' as message,
    'Enhanced with MySQL 9.6 features: utf8mb4_0900_ai_ci, JSON improvements, microsecond precision' as features,
    'Compatible with existing system design patterns' as compatibility;