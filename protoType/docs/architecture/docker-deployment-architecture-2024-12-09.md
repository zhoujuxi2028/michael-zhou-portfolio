# Cloud Monitoring Platform - Docker部署指南

## 文档信息
- **版本**: v1.0
- **创建日期**: 2024-12-09
- **更新日期**: 2024-12-09
- **状态**: 已完成

## 概述

本指南说明如何使用Docker和Docker Compose部署云监控平台设备信息查询系统。

> **⚠️ 重要说明**: Docker Compose配置文件已移动到 `poc/` 目录中，作为概念验证的一部分。详见 [`poc/README.md`](../../poc/README.md)。

## 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Spring Boot   │    │      MySQL      │    │      Redis      │
│    应用服务     │◄──►│     数据库      │    │    缓存服务     │
│   Port: 8080    │    │   Port: 3306    │    │   Port: 6379    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少2GB可用内存
- 至少5GB可用磁盘空间

## 快速启动

### 1. 克隆项目
```bash
cd /path/to/Cloud-Monitoring-Platform/protoType
```

### 2. 启动服务
```bash
# 启动所有服务（后台运行）
docker-compose up -d

# 查看启动日志
docker-compose logs -f
```

### 3. 验证部署
```bash
# 检查服务状态
docker-compose ps

# 测试API
curl http://localhost:8080/api/health/check
```

### 4. 访问服务
- **API根路径**: http://localhost:8080/api
- **API文档**: http://localhost:8080/api/swagger-ui/
- **数据库监控**: http://localhost:8080/api/druid/
- **健康检查**: http://localhost:8080/api/health/check

## 详细部署步骤

### 第一步：准备数据库初始化脚本

确保数据库初始化脚本存在：
```bash
ls -la docs/database-design-phase1-v1.0.sql
```

如果文件不存在，请从项目中复制或创建。

### 第二步：配置环境变量（可选）

创建`.env`文件自定义配置：
```bash
# 数据库配置
DB_HOST=mysql
DB_PORT=3306
DB_NAME=cloud_monitoring_phase1
DB_USERNAME=cloudapp
DB_PASSWORD=your_secure_password

# Redis配置
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# 应用配置
SPRING_PROFILES_ACTIVE=docker
```

### 第三步：构建和启动

```bash
# 拉取镜像
docker-compose pull

# 构建应用镜像
docker-compose build

# 启动所有服务
docker-compose up -d

# 等待服务就绪（约60-90秒）
docker-compose logs -f app
```

### 第四步：验证部署

```bash
# 检查所有容器状态
docker-compose ps

# 应该看到类似输出：
# NAME                     COMMAND                  SERVICE   STATUS
# cloud-monitoring-app     "sh -c 'java $JAVA_O…"   app      Up (healthy)
# cloud-monitoring-mysql   "docker-entrypoint.s…"   mysql    Up
# cloud-monitoring-redis   "docker-entrypoint.s…"   redis    Up

# 测试API响应
curl -s http://localhost:8080/api/health/check | jq
```

## 服务管理

### 启动服务
```bash
docker-compose up -d
```

### 停止服务
```bash
docker-compose down
```

### 重启服务
```bash
docker-compose restart
```

### 查看日志
```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs app
docker-compose logs mysql
docker-compose logs redis

# 实时跟踪日志
docker-compose logs -f app
```

### 扩容应用
```bash
# 运行多个应用实例
docker-compose up -d --scale app=3
```

## 数据持久化

系统使用Docker volumes持久化数据：

- `cloud-monitoring-mysql-data`: MySQL数据文件
- `cloud-monitoring-redis-data`: Redis数据文件
- `cloud-monitoring-app-logs`: 应用日志文件
- `cloud-monitoring-app-uploads`: 上传文件

### 备份数据
```bash
# 备份MySQL数据
docker-compose exec mysql mysqldump -u root -p123456 cloud_monitoring_phase1 > backup.sql

# 备份Docker volumes
docker run --rm -v cloud-monitoring-mysql-data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-backup.tar.gz -C /data .
```

### 恢复数据
```bash
# 恢复MySQL数据
docker-compose exec -T mysql mysql -u root -p123456 cloud_monitoring_phase1 < backup.sql
```

## 配置说明

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `DB_HOST` | mysql | 数据库主机名 |
| `DB_PORT` | 3306 | 数据库端口 |
| `DB_NAME` | cloud_monitoring_phase1 | 数据库名 |
| `DB_USERNAME` | cloudapp | 数据库用户名 |
| `DB_PASSWORD` | cloudapp123 | 数据库密码 |
| `REDIS_HOST` | redis | Redis主机名 |
| `REDIS_PORT` | 6379 | Redis端口 |
| `REDIS_PASSWORD` | redis123 | Redis密码 |
| `SPRING_PROFILES_ACTIVE` | docker | Spring配置文件 |

### 端口映射

| 服务 | 内部端口 | 外部端口 |
|------|----------|----------|
| Spring Boot | 8080 | 8080 |
| MySQL | 3306 | 3306 |
| Redis | 6379 | 6379 |

## 故障排查

### 应用启动失败
```bash
# 查看应用日志
docker-compose logs app

# 检查数据库连接
docker-compose exec app ping mysql

# 进入容器调试
docker-compose exec app sh
```

### 数据库连接问题
```bash
# 检查MySQL是否就绪
docker-compose exec mysql mysql -u root -p123456 -e "SHOW DATABASES;"

# 检查网络连通性
docker-compose exec app nslookup mysql
```

### 性能问题
```bash
# 查看容器资源使用
docker stats

# 查看应用JVM状态
docker-compose exec app jstat -gc 1
```

## 生产环境部署建议

### 安全配置
1. 修改默认密码
2. 使用加密的环境变量
3. 限制容器网络访问
4. 启用HTTPS

### 监控配置
```yaml
# 添加到docker-compose.yml
services:
  app:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health/check"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### 日志管理
```yaml
# 配置日志驱动
services:
  app:
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
```

### 资源限制
```yaml
# 添加资源限制
services:
  app:
    deploy:
      resources:
        limits:
          cpus: "1.0"
          memory: 512M
        reservations:
          cpus: "0.5"
          memory: 256M
```

## 维护操作

### 更新应用
```bash
# 拉取最新代码
git pull

# 重新构建应用镜像
docker-compose build app

# 滚动更新（零宕机）
docker-compose up -d app
```

### 清理资源
```bash
# 清理未使用的镜像
docker image prune

# 清理未使用的容器
docker container prune

# 清理未使用的volumes（注意：会删除数据）
docker volume prune
```

## 联系支持

如有问题，请联系：
- 开发团队：dev@zct.com
- 技术支持：support@zct.com
- 项目地址：[GitHub Repository URL]