-- =================================================================
-- AEP Integration Database Schema v2.0
-- 基于现有系统设计规范，专为AEP平台集成优化
--
-- 设计原则：
-- 1. 与现有系统命名规范保持一致
-- 2. 采用相同的数据类型和字符集
-- 3. 支持与现有设备管理系统的关联
-- 4. 提供完整的AEP操作审计能力
-- 5. 支持多环境和多租户部署
--
-- 创建日期: 2026-01-25
-- 目标系统: Vendor C云监控平台
-- =================================================================

SET NAMES utf8mb3;
SET FOREIGN_KEY_CHECKS = 0;

-- =================================================================
-- 1. AEP产品管理表
-- =================================================================

DROP TABLE IF EXISTS `t_aep_products`;
CREATE TABLE `t_aep_products` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `aep_product_id` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'AEP平台产品ID',
  `product_name` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '产品名称',
  `device_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '设备类型：SENSOR/GATEWAY/DEVICE/TERMINAL/MODULE',
  `network_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '网络类型：NB-IOT/2G/3G/4G/5G/WIFI/ETHERNET/LORA',
  `data_format` tinyint NOT NULL DEFAULT 1 COMMENT '数据格式：1=JSON, 2=二进制',
  `industry_id` int NULL DEFAULT NULL COMMENT '行业ID',
  `protocol_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '协议类型：CoAP/MQTT/LWM2M',
  `manufacturer_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '厂商ID',
  `manufacturer_name` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '厂商名称',
  `device_model` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '设备型号',
  `max_device_count` int NULL DEFAULT NULL COMMENT '最大设备数量',
  `current_device_count` int NULL DEFAULT 0 COMMENT '当前设备数量',
  `master_key` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'AEP产品主密钥（加密存储）',
  `product_secret` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '产品密钥（加密存储）',
  `enable_security` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用安全认证：0=否, 1=是',
  `auto_create_device` tinyint(1) NULL DEFAULT 0 COMMENT '是否自动创建设备：0=否, 1=是',
  `description` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '产品描述',
  `platform_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '关联AEP平台配置ID',
  `local_prod_module` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '本地对应的产品型号',
  `status` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE/DELETED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人ID',
  `sort` tinyint NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_aep_product_id` (`aep_product_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_product_name_status` (`product_name` ASC, `status` ASC) USING BTREE,
  INDEX `idx_device_type` (`device_type` ASC) USING BTREE,
  INDEX `idx_network_type` (`network_type` ASC) USING BTREE,
  INDEX `idx_platform_id` (`platform_id` ASC) USING BTREE,
  INDEX `idx_status` (`status` ASC) USING BTREE,
  INDEX `idx_create_time` (`create_time` ASC) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='AEP产品信息管理表' ROW_FORMAT=DYNAMIC;

-- =================================================================
-- 2. AEP设备映射表
-- =================================================================

DROP TABLE IF EXISTS `t_aep_devices`;
CREATE TABLE `t_aep_devices` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `aep_device_id` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'AEP平台设备ID',
  `aep_product_id` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'AEP产品ID',
  `local_device_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '本地设备ID（关联t_device表）',
  `local_lbs_id` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '本地设备编码（关联t_device.lbs_id）',
  `device_name` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '设备名称',
  `node_id` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '设备节点ID（IMEI/MAC/Serial）',
  `imsi` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'NB-IoT终端IMSI',
  `psk` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '设备PSK密钥（加密）',
  `verify_code` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '设备验证码',
  `device_status` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'OFFLINE' COMMENT '设备状态：ONLINE/OFFLINE/ABNORMAL',
  `aep_status` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'AEP平台设备状态',
  `sync_status` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'PENDING' COMMENT '同步状态：PENDING/SUCCESS/FAILED',
  `last_sync_time` datetime NULL DEFAULT NULL COMMENT '最后同步时间',
  `last_online_time` datetime NULL DEFAULT NULL COMMENT '最后上线时间',
  `last_offline_time` datetime NULL DEFAULT NULL COMMENT '最后离线时间',
  `device_lng` float(20,5) NULL DEFAULT NULL COMMENT '设备经度',
  `device_lat` float(20,5) NULL DEFAULT NULL COMMENT '设备纬度',
  `device_address` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '设备地址',
  `platform_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'AEP平台配置ID',
  `is_secure` tinyint(1) NULL DEFAULT 0 COMMENT '是否安全设备',
  `timeout` int NULL DEFAULT 0 COMMENT '验证码超时时间（秒）',
  `mute` tinyint(1) NULL DEFAULT 0 COMMENT '是否冻结状态',
  `organization` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '设备所属组织',
  `description` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '设备描述',
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
) ENGINE=InnoDB CHARACTER SET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='AEP设备与本地设备映射表' ROW_FORMAT=DYNAMIC;

-- =================================================================
-- 3. AEP操作记录表
-- =================================================================

DROP TABLE IF EXISTS `t_aep_operations`;
CREATE TABLE `t_aep_operations` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `operation_id` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '操作唯一标识',
  `operation_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/QUERY/REGISTER/COMMAND',
  `resource_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '资源类型：PRODUCT/DEVICE/SUBSCRIPTION/DATA',
  `resource_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '资源ID',
  `resource_name` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '资源名称',
  `method` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '请求方法：GET/POST/PUT/DELETE',
  `api_url` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'API请求地址',
  `request_data` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '请求数据（脱敏后）',
  `response_data` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '响应数据（脱敏后）',
  `response_code` int NULL DEFAULT NULL COMMENT '响应状态码',
  `operation_status` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '操作状态：PENDING/SUCCESS/FAILED',
  `error_code` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'AEP错误码',
  `error_message` varchar(1024) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '错误信息',
  `execution_time` int NULL DEFAULT NULL COMMENT '执行时长（毫秒）',
  `start_time` datetime(3) NOT NULL COMMENT '开始时间（毫秒精度）',
  `end_time` datetime(3) NULL DEFAULT NULL COMMENT '结束时间（毫秒精度）',
  `operator_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `client_ip` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户代理',
  `platform_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'AEP平台配置ID',
  `retry_count` int NULL DEFAULT 0 COMMENT '重试次数',
  `trace_id` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '链路跟踪ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_operation_id` (`operation_id` ASC) USING BTREE,
  INDEX `idx_operation_type` (`operation_type` ASC) USING BTREE,
  INDEX `idx_resource_type` (`resource_type` ASC) USING BTREE,
  INDEX `idx_operation_status` (`operation_status` ASC) USING BTREE,
  INDEX `idx_start_time` (`start_time` ASC) USING BTREE,
  INDEX `idx_operator_id` (`operator_id` ASC) USING BTREE,
  INDEX `idx_platform_id` (`platform_id` ASC) USING BTREE,
  INDEX `idx_complex_query` (`operation_type` ASC, `resource_type` ASC, `operation_status` ASC, `start_time` ASC) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='AEP API操作记录表' ROW_FORMAT=DYNAMIC;

-- =================================================================
-- 4. AEP平台配置表
-- =================================================================

DROP TABLE IF EXISTS `t_aep_platforms`;
CREATE TABLE `t_aep_platforms` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `platform_name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '平台名称',
  `platform_alias` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '平台别名',
  `api_host` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'AEP API主机地址',
  `app_id` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'AEP应用ID',
  `app_key` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'AEP应用密钥（加密存储）',
  `app_secret` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'AEP应用密码（加密存储）',
  `environment` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'production' COMMENT '环境：development/test/staging/production',
  `tenant_id` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '租户ID',
  `api_version` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'v1.0' COMMENT 'API版本',
  `max_retries` int NULL DEFAULT 3 COMMENT '最大重试次数',
  `timeout_ms` int NULL DEFAULT 30000 COMMENT '请求超时时间（毫秒）',
  `rate_limit` int NULL DEFAULT 100 COMMENT '速率限制（每分钟）',
  `enable_ssl` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用SSL：0=否, 1=是',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否激活：0=否, 1=是',
  `is_default` tinyint(1) NULL DEFAULT 0 COMMENT '是否默认平台：0=否, 1=是',
  `description` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '平台描述',
  `ext_config` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '扩展配置（JSON格式）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人ID',
  `sort` tinyint NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_name_env` (`platform_name` ASC, `environment` ASC) USING BTREE,
  UNIQUE INDEX `uk_app_id_env` (`app_id` ASC, `environment` ASC) USING BTREE,
  INDEX `idx_environment` (`environment` ASC) USING BTREE,
  INDEX `idx_is_active` (`is_active` ASC) USING BTREE,
  INDEX `idx_is_default` (`is_default` ASC) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='AEP平台配置管理表' ROW_FORMAT=DYNAMIC;

-- =================================================================
-- 5. AEP数据同步状态表
-- =================================================================

DROP TABLE IF EXISTS `t_aep_sync_status`;
CREATE TABLE `t_aep_sync_status` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `sync_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '同步类型：FULL/INCREMENTAL/REALTIME',
  `resource_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '资源类型：PRODUCT/DEVICE/DATA/ALARM',
  `resource_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '资源ID',
  `direction` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '同步方向：UP/DOWN/BOTH',
  `sync_status` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '同步状态：PENDING/RUNNING/SUCCESS/FAILED/PARTIAL',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `last_sync_time` datetime NULL DEFAULT NULL COMMENT '最后成功同步时间',
  `next_sync_time` datetime NULL DEFAULT NULL COMMENT '下次同步时间',
  `total_records` int NULL DEFAULT 0 COMMENT '总记录数',
  `success_records` int NULL DEFAULT 0 COMMENT '成功记录数',
  `failed_records` int NULL DEFAULT 0 COMMENT '失败记录数',
  `sync_progress` decimal(5,2) NULL DEFAULT 0.00 COMMENT '同步进度（百分比）',
  `error_message` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '错误信息',
  `error_details` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '详细错误信息',
  `retry_count` int NULL DEFAULT 0 COMMENT '重试次数',
  `max_retries` int NULL DEFAULT 3 COMMENT '最大重试次数',
  `platform_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'AEP平台配置ID',
  `operator_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '操作人ID',
  `sync_config` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '同步配置（JSON）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sync_type` (`sync_type` ASC) USING BTREE,
  INDEX `idx_resource_type` (`resource_type` ASC) USING BTREE,
  INDEX `idx_sync_status` (`sync_status` ASC) USING BTREE,
  INDEX `idx_last_sync_time` (`last_sync_time` ASC) USING BTREE,
  INDEX `idx_next_sync_time` (`next_sync_time` ASC) USING BTREE,
  INDEX `idx_platform_id` (`platform_id` ASC) USING BTREE,
  INDEX `idx_complex_sync` (`sync_type` ASC, `resource_type` ASC, `sync_status` ASC) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='AEP数据同步状态追踪表' ROW_FORMAT=DYNAMIC;

-- =================================================================
-- 6. AEP统计数据表
-- =================================================================

DROP TABLE IF EXISTS `t_aep_statistics`;
CREATE TABLE `t_aep_statistics` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `stat_hour` tinyint NULL DEFAULT NULL COMMENT '统计小时（0-23），空值表示日统计',
  `category` tinyint NOT NULL COMMENT '统计类别：0=设备数量 1=操作统计 2=同步统计 3=告警统计',
  `metric_name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '指标名称',
  `metric_value` bigint NOT NULL DEFAULT 0 COMMENT '指标值',
  `metric_unit` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'COUNT' COMMENT '指标单位',
  `dimension1` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '维度1（如设备类型）',
  `dimension2` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '维度2（如网络类型）',
  `dimension3` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '维度3（如状态）',
  `additional_data` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '附加数据（JSON格式）',
  `platform_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'AEP平台配置ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stat_unique` (`stat_date` ASC, `stat_hour` ASC, `category` ASC, `metric_name` ASC, `dimension1` ASC, `dimension2` ASC, `platform_id` ASC) USING BTREE,
  INDEX `idx_stat_date` (`stat_date` ASC) USING BTREE,
  INDEX `idx_category` (`category` ASC) USING BTREE,
  INDEX `idx_metric_name` (`metric_name` ASC) USING BTREE,
  INDEX `idx_platform_id` (`platform_id` ASC) USING BTREE,
  INDEX `idx_complex_stat` (`stat_date` ASC, `category` ASC, `dimension1` ASC) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='AEP统计数据表' ROW_FORMAT=DYNAMIC;

-- =================================================================
-- 7. AEP配置管理表
-- =================================================================

DROP TABLE IF EXISTS `t_aep_configs`;
CREATE TABLE `t_aep_configs` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `config_group` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '配置组：api/sync/retry/security/monitoring',
  `config_key` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '配置值',
  `config_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'STRING' COMMENT '配置类型：STRING/INTEGER/BOOLEAN/JSON/ENCRYPTED',
  `is_encrypted` tinyint(1) NULL DEFAULT 0 COMMENT '是否加密存储：0=否, 1=是',
  `is_required` tinyint(1) NULL DEFAULT 0 COMMENT '是否必需配置：0=否, 1=是',
  `default_value` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '默认值',
  `validation_rule` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '验证规则（正则表达式）',
  `description` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '配置描述',
  `environment` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'ALL' COMMENT '适用环境：ALL/development/test/production',
  `platform_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'AEP平台配置ID（空值表示全局配置）',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否生效：0=否, 1=是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_unique` (`config_group` ASC, `config_key` ASC, `environment` ASC, `platform_id` ASC) USING BTREE,
  INDEX `idx_config_group` (`config_group` ASC) USING BTREE,
  INDEX `idx_environment` (`environment` ASC) USING BTREE,
  INDEX `idx_platform_id` (`platform_id` ASC) USING BTREE,
  INDEX `idx_is_active` (`is_active` ASC) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='AEP配置管理表' ROW_FORMAT=DYNAMIC;

-- =================================================================
-- 8. AEP设备数据表
-- =================================================================

DROP TABLE IF EXISTS `t_aep_device_data`;
CREATE TABLE `t_aep_device_data` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `aep_device_id` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'AEP设备ID',
  `local_device_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '本地设备ID',
  `message_id` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '消息ID',
  `data_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '数据类型：TELEMETRY/COMMAND/ALARM/STATUS',
  `data_format` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '数据格式：JSON/BINARY/HEX',
  `raw_data` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '原始数据',
  `parsed_data` json NULL COMMENT '解析后数据（JSON格式）',
  `data_size` int NULL DEFAULT NULL COMMENT '数据大小（字节）',
  `signal_strength` int NULL DEFAULT NULL COMMENT '信号强度',
  `battery_level` int NULL DEFAULT NULL COMMENT '电池电量（百分比）',
  `device_lng` float(20,5) NULL DEFAULT NULL COMMENT '设备经度',
  `device_lat` float(20,5) NULL DEFAULT NULL COMMENT '设备纬度',
  `receive_time` datetime(3) NOT NULL COMMENT '接收时间',
  `device_time` datetime NULL DEFAULT NULL COMMENT '设备时间',
  `process_status` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSED/FAILED',
  `process_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `error_message` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '处理错误信息',
  `platform_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'AEP平台配置ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_aep_device_id` (`aep_device_id` ASC) USING BTREE,
  INDEX `idx_local_device_id` (`local_device_id` ASC) USING BTREE,
  INDEX `idx_data_type` (`data_type` ASC) USING BTREE,
  INDEX `idx_receive_time` (`receive_time` ASC) USING BTREE,
  INDEX `idx_process_status` (`process_status` ASC) USING BTREE,
  INDEX `idx_device_time` (`device_time` ASC) USING BTREE,
  INDEX `idx_platform_id` (`platform_id` ASC) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='AEP设备数据接收表' ROW_FORMAT=DYNAMIC;

-- =================================================================
-- 9. 初始化默认数据
-- =================================================================

-- 插入默认平台配置
INSERT INTO `t_aep_platforms` (`id`, `platform_name`, `platform_alias`, `api_host`, `app_id`, `app_key`, `app_secret`, `environment`, `description`, `is_default`) VALUES
('aep_platform_dev_001', 'AEP开发环境', 'AEP-DEV', 'your_tenant.api.ctwing.cn', '267848', 'ENCRYPTED_KEY_PLACEHOLDER', 'ENCRYPTED_SECRET_PLACEHOLDER', 'development', 'AEP平台开发环境配置', 1),
('aep_platform_prod_001', 'AEP生产环境', 'AEP-PROD', 'your_tenant.api.ctwing.cn', '267848', 'ENCRYPTED_KEY_PLACEHOLDER', 'ENCRYPTED_SECRET_PLACEHOLDER', 'production', 'AEP平台生产环境配置', 0);

-- 插入默认配置项
INSERT INTO `t_aep_configs` (`id`, `config_group`, `config_key`, `config_value`, `config_type`, `description`, `is_required`) VALUES
-- API配置
('cfg_api_001', 'api', 'default_timeout_ms', '30000', 'INTEGER', '默认API请求超时时间（毫秒）', 1),
('cfg_api_002', 'api', 'max_retry_count', '3', 'INTEGER', '最大重试次数', 1),
('cfg_api_003', 'api', 'retry_interval_ms', '1000', 'INTEGER', '重试间隔时间（毫秒）', 1),
('cfg_api_004', 'api', 'enable_request_logging', 'true', 'BOOLEAN', '是否启用请求日志', 0),

-- 同步配置
('cfg_sync_001', 'sync', 'auto_sync_enabled', 'true', 'BOOLEAN', '是否启用自动同步', 1),
('cfg_sync_002', 'sync', 'sync_interval_minutes', '30', 'INTEGER', '同步间隔（分钟）', 1),
('cfg_sync_003', 'sync', 'full_sync_hour', '2', 'INTEGER', '每日全量同步时间（24小时制）', 1),
('cfg_sync_004', 'sync', 'max_sync_records', '1000', 'INTEGER', '单次最大同步记录数', 1),

-- 监控配置
('cfg_monitor_001', 'monitoring', 'enable_performance_monitoring', 'true', 'BOOLEAN', '启用性能监控', 0),
('cfg_monitor_002', 'monitoring', 'stats_retention_days', '90', 'INTEGER', '统计数据保留天数', 1),
('cfg_monitor_003', 'monitoring', 'log_retention_days', '30', 'INTEGER', '操作日志保留天数', 1),

-- 安全配置
('cfg_security_001', 'security', 'encryption_algorithm', 'AES-256-GCM', 'STRING', '数据加密算法', 1),
('cfg_security_002', 'security', 'key_rotation_days', '90', 'INTEGER', '密钥轮换周期（天）', 1),
('cfg_security_003', 'security', 'enable_data_masking', 'true', 'BOOLEAN', '启用数据脱敏', 1);

-- =================================================================
-- 10. 创建视图（便于查询）
-- =================================================================

-- 产品概览视图
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
    END as device_usage_percentage
FROM t_aep_products p
LEFT JOIN t_aep_platforms plt ON p.platform_id = plt.id
WHERE p.status = 'ACTIVE';

-- 设备状态概览视图
CREATE VIEW v_aep_device_status AS
SELECT
    d.id,
    d.aep_device_id,
    d.device_name,
    d.device_status,
    d.sync_status,
    d.last_sync_time,
    d.last_online_time,
    p.product_name,
    p.device_type,
    plt.platform_name
FROM t_aep_devices d
LEFT JOIN t_aep_products p ON d.aep_product_id = p.aep_product_id
LEFT JOIN t_aep_platforms plt ON d.platform_id = plt.id;

-- 操作统计视图
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
    MIN(execution_time) as min_execution_time
FROM t_aep_operations
WHERE start_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY DATE(start_time), operation_type, resource_type
ORDER BY operation_date DESC, operation_type;

-- =================================================================
-- 11. 创建存储过程
-- =================================================================

DELIMITER $$

-- 清理过期数据存储过程
CREATE PROCEDURE sp_aep_cleanup_expired_data(IN retention_days INT)
BEGIN
    DECLARE done INT DEFAULT FALSE;

    START TRANSACTION;

    -- 清理过期操作日志
    DELETE FROM t_aep_operations
    WHERE start_time < DATE_SUB(CURRENT_DATE, INTERVAL retention_days DAY);

    -- 清理过期设备数据
    DELETE FROM t_aep_device_data
    WHERE receive_time < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL retention_days DAY);

    -- 清理过期同步状态记录
    DELETE FROM t_aep_sync_status
    WHERE create_time < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL (retention_days * 2) DAY)
    AND sync_status IN ('SUCCESS', 'FAILED');

    COMMIT;

    SELECT 'AEP data cleanup completed' as result;
END$$

-- 统计数据聚合存储过程
CREATE PROCEDURE sp_aep_aggregate_daily_stats(IN stat_date DATE)
BEGIN
    -- 删除当天已有统计
    DELETE FROM t_aep_statistics WHERE stat_date = stat_date AND stat_hour IS NULL;

    -- 设备数量统计
    INSERT INTO t_aep_statistics (id, stat_date, category, metric_name, metric_value, dimension1, platform_id)
    SELECT
        CONCAT('stat_', UUID_SHORT()),
        stat_date,
        0,
        'device_count',
        COUNT(*),
        device_status,
        platform_id
    FROM t_aep_devices
    WHERE DATE(create_time) <= stat_date
    GROUP BY device_status, platform_id;

    -- 操作统计
    INSERT INTO t_aep_statistics (id, stat_date, category, metric_name, metric_value, dimension1, dimension2, platform_id)
    SELECT
        CONCAT('stat_', UUID_SHORT()),
        stat_date,
        1,
        'operation_count',
        COUNT(*),
        operation_type,
        operation_status,
        platform_id
    FROM t_aep_operations
    WHERE DATE(start_time) = stat_date
    GROUP BY operation_type, operation_status, platform_id;

    SELECT CONCAT('Daily stats aggregated for ', stat_date) as result;
END$$

DELIMITER ;

-- =================================================================
-- 12. 创建触发器
-- =================================================================

-- 产品设备数量自动更新触发器
DELIMITER $$

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
END$$

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
END$$

DELIMITER ;

SET FOREIGN_KEY_CHECKS = 1;

-- =================================================================
-- Schema创建完成
-- =================================================================

SELECT 'AEP Integration Database Schema v2.0 created successfully!' as message,
       'Compatible with existing system design patterns' as compatibility,
       'Optimized for AEP platform integration' as optimization;