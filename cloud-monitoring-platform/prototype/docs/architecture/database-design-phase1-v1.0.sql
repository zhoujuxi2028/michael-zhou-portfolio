/*
 * Phase 1 设备信息查询系统数据库设计
 * 版本: v1.0
 * 创建日期: 2024-12-08
 * 说明: 基于 vendor-b 项目的 t_device 表结构，简化并优化为符合 Phase 1 需求的数据库设计
 */

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `cloud_monitoring_phase1` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `cloud_monitoring_phase1`;

-- ===========================================
-- 1. 设备类型表 (t_device_type)
-- 用于设备分类管理，支持层级结构
-- ===========================================
CREATE TABLE `t_device_type` (
  `type_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '类型ID (主键)',
  `type_code` varchar(32) NOT NULL COMMENT '类型编码',
  `type_name` varchar(64) NOT NULL COMMENT '类型名称',
  `parent_type_id` bigint(20) NULL DEFAULT NULL COMMENT '父类型ID',
  `type_description` text NULL COMMENT '类型描述',
  `sort_order` int(11) NULL DEFAULT 0 COMMENT '排序权重',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用 1-启用 0-禁用',
  `created_by` varchar(32) NULL DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(32) NULL DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`type_id`),
  UNIQUE KEY `uk_type_code` (`type_code`),
  KEY `idx_parent_type_id` (`parent_type_id`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备类型表';

-- ===========================================
-- 2. 区域信息表 (t_region)
-- 用于设备地理位置分类管理
-- ===========================================
CREATE TABLE `t_region` (
  `region_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '区域ID (主键)',
  `region_code` varchar(32) NOT NULL COMMENT '区域编码',
  `region_name` varchar(100) NOT NULL COMMENT '区域名称',
  `parent_region_id` bigint(20) NULL DEFAULT NULL COMMENT '父区域ID',
  `region_level` int(11) NOT NULL DEFAULT 1 COMMENT '区域层级 1-省 2-市 3-区县 4-街道',
  `region_path` varchar(512) NULL DEFAULT NULL COMMENT '区域路径 逗号分隔的ID路径',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '中心点经度',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '中心点纬度',
  `sort_order` int(11) NULL DEFAULT 0 COMMENT '排序权重',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用 1-启用 0-禁用',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`region_id`),
  UNIQUE KEY `uk_region_code` (`region_code`),
  KEY `idx_parent_region_id` (`parent_region_id`),
  KEY `idx_region_level` (`region_level`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区域信息表';

-- ===========================================
-- 3. 设备信息主表 (t_device)
-- 存储设备核心信息，基于 vendor-b 表结构简化
-- ===========================================
CREATE TABLE `t_device` (
  `device_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '设备ID (主键)',
  `device_code` varchar(64) NOT NULL COMMENT '设备编号 (唯一标识)',
  `device_name` varchar(100) NOT NULL COMMENT '设备名称',
  `device_type_id` bigint(20) NOT NULL COMMENT '设备类型ID (外键)',
  `device_model` varchar(64) NULL DEFAULT NULL COMMENT '设备型号',
  `device_version` varchar(64) NULL DEFAULT NULL COMMENT '设备版本',

  -- 位置信息 (基于 vendor-b 的位置字段)
  `region_id` bigint(20) NULL DEFAULT NULL COMMENT '所属区域ID (外键)',
  `install_location` varchar(255) NULL DEFAULT NULL COMMENT '安装位置描述',
  `install_address` varchar(255) NULL DEFAULT NULL COMMENT '安装详细地址',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '纬度',

  -- 技术参数 (基于 vendor-b 的关键技术字段)
  `frequency_band` varchar(32) NULL DEFAULT NULL COMMENT '频段',
  `cell_id` varchar(30) NULL DEFAULT NULL COMMENT '设备Cell-ID',
  `pci` varchar(16) NULL DEFAULT NULL COMMENT 'PCI',
  `sim_number` varchar(32) NULL DEFAULT NULL COMMENT 'SIM卡号/CCID',
  `iot_device_id` varchar(64) NULL DEFAULT NULL COMMENT 'IoT平台设备ID',

  -- 管理信息
  `responsible_person` varchar(50) NULL DEFAULT NULL COMMENT '负责人',
  `contact_phone` varchar(20) NULL DEFAULT NULL COMMENT '联系电话',
  `contact_email` varchar(100) NULL DEFAULT NULL COMMENT '联系邮箱',
  `maintenance_cycle` int(11) NULL DEFAULT NULL COMMENT '维护周期(天)',
  `warranty_expire_date` date NULL DEFAULT NULL COMMENT '保修到期日期',

  -- 安装信息
  `install_date` date NULL DEFAULT NULL COMMENT '安装日期',
  `install_person` varchar(50) NULL DEFAULT NULL COMMENT '安装人员',
  `acceptance_person` varchar(50) NULL DEFAULT NULL COMMENT '验收人员',
  `acceptance_date` date NULL DEFAULT NULL COMMENT '验收日期',

  -- 状态信息
  `device_status` varchar(16) NOT NULL DEFAULT 'OFFLINE' COMMENT '设备状态 ONLINE-在线 OFFLINE-离线 MAINTENANCE-维修中 FAULT-故障 DEACTIVATED-已停用',
  `is_host` tinyint(1) NULL DEFAULT NULL COMMENT '是否主机 1-主机 0-从机',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用 1-启用 0-禁用',

  -- 业务信息
  `project_id` varchar(32) NULL DEFAULT NULL COMMENT '项目ID',
  `company_id` varchar(32) NULL DEFAULT NULL COMMENT '厂商ID',
  `owner_name` varchar(100) NULL DEFAULT NULL COMMENT '业主姓名',
  `owner_mobile` varchar(20) NULL DEFAULT NULL COMMENT '业主电话',

  -- 扩展信息
  `description` text NULL COMMENT '设备描述',
  `remarks` text NULL COMMENT '备注信息',

  -- 系统字段
  `created_by` varchar(32) NULL DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(32) NULL DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int(11) NOT NULL DEFAULT 1 COMMENT '版本号(乐观锁)',

  PRIMARY KEY (`device_id`),
  UNIQUE KEY `uk_device_code` (`device_code`),
  KEY `idx_device_type_id` (`device_type_id`),
  KEY `idx_region_id` (`region_id`),
  KEY `idx_device_status` (`device_status`),
  KEY `idx_install_date` (`install_date`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_responsible_person` (`responsible_person`),
  CONSTRAINT `fk_device_type` FOREIGN KEY (`device_type_id`) REFERENCES `t_device_type` (`type_id`),
  CONSTRAINT `fk_device_region` FOREIGN KEY (`region_id`) REFERENCES `t_region` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备信息主表';

-- ===========================================
-- 4. 用户表 (t_user)
-- 用于用户认证和权限管理
-- ===========================================
CREATE TABLE `t_user` (
  `user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID (主键)',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码(加密)',
  `salt` varchar(32) NULL DEFAULT NULL COMMENT '密码盐值',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `email` varchar(100) NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) NULL DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) NULL DEFAULT NULL COMMENT '头像URL',

  -- 角色权限
  `role` varchar(32) NOT NULL DEFAULT 'DEVICE_OPERATOR' COMMENT '角色 ADMIN-系统管理员 DEVICE_MANAGER-设备管理员 DEVICE_OPERATOR-设备操作员',
  `permissions` text NULL COMMENT '特殊权限配置(JSON格式)',

  -- 状态信息
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE-正常 LOCKED-锁定 DISABLED-禁用',
  `login_failure_count` int(11) NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
  `locked_until` datetime NULL DEFAULT NULL COMMENT '锁定截止时间',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) NULL DEFAULT NULL COMMENT '最后登录IP',
  `password_expire_time` datetime NULL DEFAULT NULL COMMENT '密码过期时间',

  -- 系统字段
  `created_by` varchar(32) NULL DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(32) NULL DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_real_name` (`real_name`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`),
  KEY `idx_last_login_time` (`last_login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ===========================================
-- 5. 设备维护记录表 (t_device_maintenance)
-- 记录设备维护历史
-- ===========================================
CREATE TABLE `t_device_maintenance` (
  `maintenance_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '维护记录ID (主键)',
  `device_id` bigint(20) NOT NULL COMMENT '设备ID (外键)',
  `maintenance_type` varchar(32) NOT NULL COMMENT '维护类型 ROUTINE-例行维护 REPAIR-故障维修 UPGRADE-升级改造 INSPECTION-巡检',
  `maintenance_title` varchar(200) NOT NULL COMMENT '维护标题',
  `maintenance_content` text NOT NULL COMMENT '维护内容',
  `maintenance_result` text NULL COMMENT '维护结果',

  -- 时间信息
  `planned_start_time` datetime NULL DEFAULT NULL COMMENT '计划开始时间',
  `planned_end_time` datetime NULL DEFAULT NULL COMMENT '计划结束时间',
  `actual_start_time` datetime NULL DEFAULT NULL COMMENT '实际开始时间',
  `actual_end_time` datetime NULL DEFAULT NULL COMMENT '实际结束时间',

  -- 人员信息
  `maintenance_person` varchar(100) NOT NULL COMMENT '维护人员',
  `maintenance_phone` varchar(20) NULL DEFAULT NULL COMMENT '维护人员电话',
  `confirm_person` varchar(50) NULL DEFAULT NULL COMMENT '确认人',

  -- 状态信息
  `maintenance_status` varchar(16) NOT NULL DEFAULT 'PLANNED' COMMENT '维护状态 PLANNED-计划中 IN_PROGRESS-进行中 COMPLETED-已完成 CANCELLED-已取消',
  `device_status_before` varchar(16) NULL DEFAULT NULL COMMENT '维护前设备状态',
  `device_status_after` varchar(16) NULL DEFAULT NULL COMMENT '维护后设备状态',

  -- 费用信息
  `estimated_cost` decimal(10,2) NULL DEFAULT NULL COMMENT '预计费用',
  `actual_cost` decimal(10,2) NULL DEFAULT NULL COMMENT '实际费用',

  -- 附件信息
  `attachments` text NULL COMMENT '相关附件(JSON格式存储文件信息)',
  `remarks` text NULL COMMENT '备注',

  -- 系统字段
  `created_by` varchar(32) NULL DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(32) NULL DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`maintenance_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_maintenance_type` (`maintenance_type`),
  KEY `idx_maintenance_status` (`maintenance_status`),
  KEY `idx_planned_start_time` (`planned_start_time`),
  KEY `idx_actual_start_time` (`actual_start_time`),
  KEY `idx_maintenance_person` (`maintenance_person`),
  CONSTRAINT `fk_maintenance_device` FOREIGN KEY (`device_id`) REFERENCES `t_device` (`device_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备维护记录表';

-- ===========================================
-- 6. 操作日志表 (t_operation_log)
-- 记录用户操作行为，用于审计
-- ===========================================
CREATE TABLE `t_operation_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID (主键)',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) NULL DEFAULT NULL COMMENT '操作用户名',
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型 LOGIN-登录 LOGOUT-登出 QUERY-查询 CREATE-新增 UPDATE-修改 DELETE-删除 EXPORT-导出',
  `operation_module` varchar(32) NOT NULL COMMENT '操作模块 USER-用户管理 DEVICE-设备管理 DEVICE_TYPE-设备类型 REGION-区域管理',
  `operation_description` varchar(500) NOT NULL COMMENT '操作描述',
  `request_url` varchar(255) NULL DEFAULT NULL COMMENT '请求URL',
  `request_method` varchar(10) NULL DEFAULT NULL COMMENT '请求方法',
  `request_params` text NULL COMMENT '请求参数',
  `response_result` varchar(16) NULL DEFAULT NULL COMMENT '响应结果 SUCCESS-成功 FAILED-失败',
  `error_message` text NULL COMMENT '错误信息',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `ip_address` varchar(50) NULL DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) NULL DEFAULT NULL COMMENT '用户代理',
  `execution_time` bigint(20) NULL DEFAULT NULL COMMENT '执行耗时(毫秒)',

  PRIMARY KEY (`log_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_operation_module` (`operation_module`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_response_result` (`response_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ===========================================
-- 7. 系统配置表 (t_system_config)
-- 存储系统配置参数
-- ===========================================
CREATE TABLE `t_system_config` (
  `config_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID (主键)',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text NOT NULL COMMENT '配置值',
  `config_type` varchar(32) NOT NULL DEFAULT 'STRING' COMMENT '配置类型 STRING-字符串 NUMBER-数字 BOOLEAN-布尔 JSON-JSON对象',
  `config_group` varchar(32) NOT NULL DEFAULT 'SYSTEM' COMMENT '配置分组',
  `config_description` varchar(255) NULL DEFAULT NULL COMMENT '配置描述',
  `is_public` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否公开 1-公开 0-私有',
  `is_editable` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可编辑 1-可编辑 0-只读',
  `sort_order` int(11) NULL DEFAULT 0 COMMENT '排序权重',

  -- 系统字段
  `created_by` varchar(32) NULL DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(32) NULL DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_config_group` (`config_group`),
  KEY `idx_is_public` (`is_public`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ===========================================
-- 数据初始化脚本
-- ===========================================

-- 初始化设备类型数据
INSERT INTO `t_device_type` (`type_code`, `type_name`, `parent_type_id`, `type_description`, `sort_order`, `is_active`) VALUES
('SIGNAL_COVERAGE', '信号覆盖设备', NULL, '信号覆盖相关设备', 1, 1),
('MICRO_DISTRIBUTE', '微分布设备', 1, '微分布信号覆盖设备', 1, 1),
('ELEVATOR', '电梯设备', 1, '电梯信号覆盖设备', 2, 1),
('IOT_SENSOR', 'IoT传感器', NULL, '物联网传感器设备', 2, 1),
('TEMPERATURE', '温度传感器', 4, '温度检测传感器', 1, 1),
('HUMIDITY', '湿度传感器', 4, '湿度检测传感器', 2, 1),
('MONITORING', '监控设备', NULL, '监控相关设备', 3, 1),
('CAMERA', '摄像头', 7, '监控摄像头设备', 1, 1);

-- 初始化区域数据(示例)
INSERT INTO `t_region` (`region_code`, `region_name`, `parent_region_id`, `region_level`, `longitude`, `latitude`, `sort_order`) VALUES
('CN', '中国', NULL, 1, 104.195397, 35.86166, 1),
('CN_BJ', '北京市', 1, 2, 116.405285, 39.904989, 1),
('CN_BJ_HD', '海淀区', 2, 3, 116.298056, 39.959912, 1),
('CN_BJ_CY', '朝阳区', 2, 3, 116.443108, 39.921489, 2),
('CN_SH', '上海市', 1, 2, 121.472644, 31.231706, 2),
('CN_SH_HP', '黄浦区', 5, 3, 121.484443, 31.231585, 1),
('CN_GD', '广东省', 1, 2, 113.280637, 23.125178, 3),
('CN_GD_GZ', '广州市', 7, 3, 113.280637, 23.125178, 1),
('CN_GD_SZ', '深圳市', 7, 3, 114.085947, 22.547, 2);

-- 更新区域路径
UPDATE `t_region` SET `region_path` = '1' WHERE `region_id` = 1;
UPDATE `t_region` SET `region_path` = '1,2' WHERE `region_id` = 2;
UPDATE `t_region` SET `region_path` = '1,2,3' WHERE `region_id` = 3;
UPDATE `t_region` SET `region_path` = '1,2,4' WHERE `region_id` = 4;
UPDATE `t_region` SET `region_path` = '1,5' WHERE `region_id` = 5;
UPDATE `t_region` SET `region_path` = '1,5,6' WHERE `region_id` = 6;
UPDATE `t_region` SET `region_path` = '1,7' WHERE `region_id` = 7;
UPDATE `t_region` SET `region_path` = '1,7,8' WHERE `region_id` = 8;
UPDATE `t_region` SET `region_path` = '1,7,9' WHERE `region_id` = 9;

-- 初始化管理员用户(密码: admin123)
INSERT INTO `t_user` (`username`, `password`, `salt`, `real_name`, `email`, `phone`, `role`, `status`) VALUES
('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'admin_salt', '系统管理员', 'admin@example.com', '13800000000', 'ADMIN', 'ACTIVE');

-- 初始化系统配置
INSERT INTO `t_system_config` (`config_key`, `config_value`, `config_type`, `config_group`, `config_description`, `is_public`, `is_editable`) VALUES
('system.name', 'Cloud Monitoring Platform - Phase 1', 'STRING', 'SYSTEM', '系统名称', 1, 1),
('system.version', '1.0.0', 'STRING', 'SYSTEM', '系统版本', 1, 0),
('page.default.size', '20', 'NUMBER', 'PAGE', '默认分页大小', 1, 1),
('page.max.size', '100', 'NUMBER', 'PAGE', '最大分页大小', 1, 1),
('session.timeout', '7200', 'NUMBER', 'SECURITY', '会话超时时间(秒)', 0, 1),
('login.failure.max', '5', 'NUMBER', 'SECURITY', '最大登录失败次数', 0, 1),
('login.lock.duration', '1800', 'NUMBER', 'SECURITY', '锁定时长(秒)', 0, 1),
('password.min.length', '6', 'NUMBER', 'SECURITY', '密码最小长度', 0, 1);