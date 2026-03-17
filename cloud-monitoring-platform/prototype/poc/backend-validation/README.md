# Backend Technology Validation

## 概述

本目录包含后端技术栈的概念验证代码，用于验证 Phase 1 项目的技术选型可行性。

## 验证目标

1. **Spring Boot 框架验证** - 验证 Spring Boot 2.7.x 的基础功能
2. **MyBatis Plus 集成验证** - 验证 ORM 框架的性能和易用性
3. **JWT 认证机制验证** - 验证身份认证方案的安全性
4. **Redis 缓存验证** - 验证缓存机制的效果
5. **MySQL 数据库集成验证** - 验证数据库连接和操作

## 技术栈

- **框架**: Spring Boot 2.7.x
- **ORM**: MyBatis Plus 3.5.x
- **数据库**: MySQL 8.0
- **缓存**: Redis 6.x
- **认证**: JWT
- **构建工具**: Maven 3.8.x

## 目录结构

```
backend-validation/
├── README.md           # 本说明文档
├── pom.xml            # Maven 依赖配置（精简版）
├── application.yml    # 应用配置（验证环境）
└── src/
    └── main/java/
        └── validation/
            ├── Application.java      # 启动类
            ├── config/              # 配置验证
            ├── controller/          # API验证
            ├── service/             # 业务逻辑验证
            └── entity/              # 数据模型验证
```

## 验证要点

### 1. 框架启动验证
- [ ] Spring Boot 应用正常启动
- [ ] 健康检查端点响应正常
- [ ] 配置加载正确

### 2. 数据库集成验证
- [ ] MyBatis Plus 配置正确
- [ ] 数据库连接池正常工作
- [ ] 基础 CRUD 操作无误
- [ ] 分页查询性能达标

### 3. 缓存机制验证
- [ ] Redis 连接正常
- [ ] 缓存读写功能正常
- [ ] 缓存过期策略生效

### 4. 认证机制验证
- [ ] JWT 令牌生成和验证
- [ ] 权限控制拦截器
- [ ] 会话管理机制

### 5. 性能验证
- [ ] 并发请求处理能力
- [ ] 数据库查询性能
- [ ] 内存使用情况

## 运行说明

### 环境要求
- JDK 11+
- Maven 3.6+
- MySQL 8.0
- Redis 6.0+

### 快速启动
```bash
# 编译
mvn clean compile

# 运行
mvn spring-boot:run
```

### 验证检查点
```bash
# 健康检查
curl http://localhost:8080/actuator/health

# API测试
curl http://localhost:8080/api/test

# 数据库连接测试
curl http://localhost:8080/api/db-test
```

## 验证结果

### 性能基准
- **启动时间**: < 10秒
- **API响应时间**: < 100ms
- **数据库查询**: < 50ms
- **缓存响应**: < 10ms

### 兼容性
- **JDK版本**: 11, 17 兼容
- **数据库版本**: MySQL 8.0+
- **缓存版本**: Redis 6.0+

## 相关文档

- [Phase 1 架构设计](../../docs/architecture/architecture-design-phase1-v1.0.md)
- [数据库设计](../../docs/architecture/database-design-phase1-v1.0.sql)
- [项目主文档](../../CLAUDE.md)