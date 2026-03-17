# Phase 1 程序架构设计文档

## 文档信息
- **版本**: v1.0
- **创建日期**: 2024-12-08
- **更新日期**: 2024-12-08
- **负责人**: Claude Code
- **状态**: 设计中

## 项目概述

Phase 1 项目采用前后端分离架构，基于现代化的 Java Spring Boot 和 Vue.js 技术栈，实现设备信息查询系统的核心功能。

## 技术架构

### 整体架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端层 (Web)   │    │   API 网关层     │    │   后端服务层     │
│                │    │                │    │                │
│  Vue.js 3      │───▶│  Nginx         │───▶│  Spring Boot   │
│  ElementUI Plus│    │  反向代理       │    │  业务逻辑层     │
│  Vue Router    │    │  负载均衡       │    │                │
│  Pinia/Vuex    │    │                │    │                │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                              │
                                              ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │   缓存层         │    │   数据层         │
                       │                │    │                │
                       │  Redis         │    │  MySQL 8.0     │
                       │  会话缓存       │    │  数据持久化     │
                       │  查询缓存       │    │  事务处理       │
                       └─────────────────┘    └─────────────────┘
```

### 技术栈详情

#### 后端技术栈
- **框架**: Spring Boot 2.7.x
- **数据访问**: MyBatis Plus 3.5.x
- **数据库**: MySQL 8.0
- **缓存**: Redis 6.x
- **认证**: JWT + Spring Security
- **文档**: Swagger/OpenAPI 3.0
- **构建工具**: Maven 3.8.x
- **JDK版本**: OpenJDK 11

#### 前端技术栈
- **框架**: Vue.js 3.3.x
- **UI组件库**: ElementUI Plus 2.4.x
- **路由**: Vue Router 4.x
- **状态管理**: Pinia 2.x
- **HTTP客户端**: Axios 1.x
- **图表组件**: ECharts 5.x
- **构建工具**: Vite 4.x
- **包管理器**: pnpm 8.x

#### 开发工具
- **IDE**: IntelliJ IDEA / VSCode
- **API测试**: Postman / Apifox
- **数据库工具**: DataGrip / MySQL Workbench
- **版本控制**: Git

## 项目目录结构

```
phase1-project/
├── backend/                          # 后端项目
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/zct/monitoring/
│   │       │       ├── CloudMonitoringApplication.java
│   │       │       ├── config/          # 配置类
│   │       │       │   ├── SecurityConfig.java
│   │       │       │   ├── MyBatisPlusConfig.java
│   │       │       │   ├── RedisConfig.java
│   │       │       │   └── SwaggerConfig.java
│   │       │       ├── common/          # 公共模块
│   │       │       │   ├── base/        # 基础类
│   │       │       │   ├── constant/    # 常量
│   │       │       │   ├── exception/   # 异常处理
│   │       │       │   ├── result/      # 统一返回结果
│   │       │       │   └── util/        # 工具类
│   │       │       ├── modules/         # 业务模块
│   │       │       │   ├── auth/        # 认证模块
│   │       │       │   │   ├── controller/
│   │       │       │   │   ├── service/
│   │       │       │   │   ├── entity/
│   │       │       │   │   └── mapper/
│   │       │       │   ├── device/      # 设备管理模块
│   │       │       │   │   ├── controller/
│   │       │       │   │   ├── service/
│   │       │       │   │   ├── entity/
│   │       │       │   │   ├── mapper/
│   │       │       │   │   └── dto/
│   │       │       │   ├── system/      # 系统管理模块
│   │       │       │   │   ├── controller/
│   │       │       │   │   ├── service/
│   │       │       │   │   ├── entity/
│   │       │       │   │   └── mapper/
│   │       │       │   └── statistics/  # 统计模块
│   │       │       │       ├── controller/
│   │       │       │       ├── service/
│   │       │       │       └── dto/
│   │       │       └── security/        # 安全模块
│   │       │           ├── jwt/
│   │       │           ├── handler/
│   │       │           └── filter/
│   │       └── resources/
│   │           ├── application.yml      # 主配置文件
│   │           ├── application-dev.yml  # 开发环境配置
│   │           ├── application-prod.yml # 生产环境配置
│   │           ├── mapper/              # MyBatis映射文件
│   │           ├── static/              # 静态资源
│   │           └── sql/                 # SQL脚本
│   ├── pom.xml                         # Maven配置
│   └── README.md                       # 后端说明文档
├── frontend/                           # 前端项目
│   ├── src/
│   │   ├── api/                        # API接口
│   │   │   ├── auth.js
│   │   │   ├── device.js
│   │   │   ├── system.js
│   │   │   └── statistics.js
│   │   ├── assets/                     # 静态资源
│   │   │   ├── images/
│   │   │   ├── icons/
│   │   │   └── styles/
│   │   ├── components/                 # 公共组件
│   │   │   ├── layout/
│   │   │   ├── common/
│   │   │   └── charts/
│   │   ├── router/                     # 路由配置
│   │   │   ├── index.js
│   │   │   └── modules/
│   │   ├── stores/                     # 状态管理
│   │   │   ├── index.js
│   │   │   ├── auth.js
│   │   │   ├── device.js
│   │   │   └── system.js
│   │   ├── utils/                      # 工具类
│   │   │   ├── request.js
│   │   │   ├── auth.js
│   │   │   ├── validation.js
│   │   │   └── common.js
│   │   ├── views/                      # 页面组件
│   │   │   ├── login/
│   │   │   ├── dashboard/
│   │   │   ├── device/
│   │   │   │   ├── DeviceList.vue
│   │   │   │   ├── DeviceDetail.vue
│   │   │   │   └── DeviceEdit.vue
│   │   │   ├── statistics/
│   │   │   │   └── Dashboard.vue
│   │   │   └── system/
│   │   │       ├── UserManagement.vue
│   │   │       └── DeviceTypeManagement.vue
│   │   ├── App.vue                     # 根组件
│   │   └── main.js                     # 入口文件
│   ├── package.json                    # 依赖配置
│   ├── vite.config.js                  # Vite配置
│   └── README.md                       # 前端说明文档
├── docs/                               # 项目文档
│   ├── api/                           # API文档
│   ├── database/                      # 数据库文档
│   ├── deployment/                    # 部署文档
│   └── development/                   # 开发文档
├── scripts/                           # 脚本文件
│   ├── init-database.sh              # 数据库初始化
│   ├── start-dev.sh                  # 开发环境启动
│   └── build.sh                      # 构建脚本
└── README.md                         # 项目说明文档
```

## 核心模块设计

### 1. 认证模块 (auth)

#### 功能职责
- 用户登录/登出
- JWT令牌生成与验证
- 用户权限控制
- 密码加密与验证

#### 核心类设计
```java
// 用户实体
@Entity
public class User {
    private Long userId;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private UserRole role;
    private UserStatus status;
    // ... getter/setter
}

// 认证服务
@Service
public class AuthService {
    public AuthResult login(LoginRequest request);
    public void logout(String token);
    public boolean validateToken(String token);
    public UserInfo getCurrentUser();
}

// 认证控制器
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/login")
    public Result<AuthResult> login(@RequestBody LoginRequest request);

    @PostMapping("/logout")
    public Result<Void> logout();

    @GetMapping("/profile")
    public Result<UserInfo> getProfile();
}
```

### 2. 设备管理模块 (device)

#### 功能职责
- 设备信息查询
- 设备详情展示
- 设备信息编辑
- 设备统计分析

#### 核心类设计
```java
// 设备实体
@Entity
public class Device {
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Long deviceTypeId;
    private String deviceModel;
    private Long regionId;
    private String installLocation;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private DeviceStatus deviceStatus;
    // ... 其他字段
}

// 设备服务
@Service
public class DeviceService {
    public PageResult<DeviceVO> queryDevices(DeviceQueryRequest request);
    public DeviceDetailVO getDeviceDetail(Long deviceId);
    public void updateDevice(Long deviceId, DeviceUpdateRequest request);
    public DeviceStatisticsVO getStatistics();
}

// 设备控制器
@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    @GetMapping
    public Result<PageResult<DeviceVO>> queryDevices(@Valid DeviceQueryRequest request);

    @GetMapping("/{deviceId}")
    public Result<DeviceDetailVO> getDeviceDetail(@PathVariable Long deviceId);

    @PutMapping("/{deviceId}")
    public Result<Void> updateDevice(@PathVariable Long deviceId, @RequestBody @Valid DeviceUpdateRequest request);

    @GetMapping("/statistics")
    public Result<DeviceStatisticsVO> getStatistics();
}
```

### 3. 系统管理模块 (system)

#### 功能职责
- 用户管理
- 设备类型管理
- 区域管理
- 系统配置

#### 核心类设计
```java
// 设备类型服务
@Service
public class DeviceTypeService {
    public List<DeviceTypeTreeVO> getDeviceTypeTree();
    public void createDeviceType(DeviceTypeCreateRequest request);
    public void updateDeviceType(Long typeId, DeviceTypeUpdateRequest request);
    public void deleteDeviceType(Long typeId);
}

// 区域服务
@Service
public class RegionService {
    public List<RegionTreeVO> getRegionTree();
    public List<RegionVO> getRegionsByLevel(Integer level);
}
```

## API 接口设计

### 统一响应格式
```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2024-12-08T10:00:00Z"
}
```

### 核心接口列表

#### 1. 认证接口
```
POST   /api/auth/login          # 用户登录
POST   /api/auth/logout         # 用户登出
GET    /api/auth/profile        # 获取用户信息
POST   /api/auth/refresh-token  # 刷新令牌
```

#### 2. 设备管理接口
```
GET    /api/devices             # 获取设备列表
GET    /api/devices/{id}        # 获取设备详情
PUT    /api/devices/{id}        # 更新设备信息
GET    /api/devices/statistics  # 获取设备统计信息
GET    /api/devices/export      # 导出设备数据
```

#### 3. 设备类型接口
```
GET    /api/device-types        # 获取设备类型列表
GET    /api/device-types/tree   # 获取设备类型树
POST   /api/device-types        # 创建设备类型
PUT    /api/device-types/{id}   # 更新设备类型
DELETE /api/device-types/{id}   # 删除设备类型
```

#### 4. 区域管理接口
```
GET    /api/regions             # 获取区域列表
GET    /api/regions/tree        # 获取区域树
GET    /api/regions/{level}     # 按层级获取区域
```

#### 5. 系统管理接口
```
GET    /api/users               # 获取用户列表
POST   /api/users               # 创建用户
PUT    /api/users/{id}          # 更新用户
DELETE /api/users/{id}          # 删除用户
GET    /api/system/configs      # 获取系统配置
PUT    /api/system/configs      # 更新系统配置
```

## 前端架构设计

### 页面结构
```
主布局 (Layout)
├── 顶部导航栏 (Header)
│   ├── Logo
│   ├── 面包屑导航
│   └── 用户信息
├── 侧边栏 (Sidebar)
│   ├── 仪表板
│   ├── 设备管理
│   │   ├── 设备列表
│   │   └── 设备类型
│   ├── 统计分析
│   └── 系统管理
│       ├── 用户管理
│       └── 区域管理
└── 内容区域 (Main Content)
    ├── 页面标题
    ├── 操作按钮
    └── 数据展示区
```

### 状态管理设计
```javascript
// 用户状态
const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    userInfo: null,
    permissions: []
  }),
  actions: {
    async login(credentials),
    async logout(),
    async getUserInfo()
  }
});

// 设备状态
const useDeviceStore = defineStore('device', {
  state: () => ({
    deviceList: [],
    deviceTypes: [],
    regions: [],
    currentDevice: null
  }),
  actions: {
    async fetchDevices(params),
    async fetchDeviceDetail(deviceId),
    async updateDevice(deviceId, data)
  }
});
```

### 组件设计原则
1. **单一职责**: 每个组件只负责一个功能
2. **可复用性**: 公共组件要具有良好的通用性
3. **数据流向**: 使用 Props Down, Events Up 模式
4. **状态管理**: 复杂状态使用 Pinia 集中管理

## 安全设计

### 1. 认证安全
- JWT令牌认证
- 令牌自动刷新机制
- 会话超时控制
- 密码强度验证

### 2. 授权安全
- 基于角色的访问控制(RBAC)
- 接口权限拦截
- 前端路由权限控制
- 按钮级别的权限控制

### 3. 数据安全
- SQL注入防护(MyBatis参数化查询)
- XSS攻击防护(输入验证和输出编码)
- CSRF攻击防护(token验证)
- 敏感数据加密存储

### 4. 通信安全
- HTTPS传输加密
- API接口签名验证
- 请求频率限制
- 输入参数验证

## 性能优化

### 1. 后端优化
- 数据库索引优化
- SQL查询优化
- Redis缓存策略
- 连接池配置
- 异步处理

### 2. 前端优化
- 组件懒加载
- 路由懒加载
- 静态资源压缩
- CDN加速
- 虚拟滚动(大数据列表)

### 3. 数据库优化
- 合理的表结构设计
- 高效的索引策略
- 查询分页限制
- 读写分离(后期扩展)

## 部署架构

### 开发环境
```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  Frontend   │  │  Backend    │  │  Database   │
│  Vite Dev   │  │  Spring     │  │  MySQL      │
│  :3000      │  │  Boot :8080 │  │  :3306      │
└─────────────┘  └─────────────┘  └─────────────┘
```

### 生产环境
```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   Nginx     │  │  Spring     │  │  MySQL      │
│   :80/443   │  │  Boot :8080 │  │  :3306      │
└─────────────┘  └─────────────┘  └─────────────┘
       │              │              │
       └──────────────┴──────────────┘
                      │
              ┌─────────────┐
              │   Redis     │
              │   :6379     │
              └─────────────┘
```

## 开发规范

### 1. 代码规范
- **后端**: 遵循阿里巴巴Java开发规范
- **前端**: 遵循Vue.js官方风格指南
- **数据库**: 遵循MySQL命名规范
- **接口**: 遵循RESTful设计规范

### 2. Git提交规范
```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式化
refactor: 重构
test: 测试
chore: 构建工具或辅助工具的变动
```

### 3. 分支管理
- `main`: 主分支，生产环境代码
- `develop`: 开发分支，集成测试
- `feature/*`: 功能分支
- `hotfix/*`: 紧急修复分支

## 测试策略

### 1. 单元测试
- **后端**: JUnit 5 + Mockito
- **前端**: Jest + Vue Test Utils
- **覆盖率**: 目标80%以上

### 2. 集成测试
- API接口测试
- 数据库集成测试
- 前后端联调测试

### 3. 性能测试
- 接口性能测试
- 数据库查询性能测试
- 前端渲染性能测试

## 项目里程碑

### Phase 1.1: 项目框架搭建 (1周)
- [ ] 后端Spring Boot项目初始化
- [ ] 前端Vue.js项目初始化
- [ ] 数据库创建和表结构初始化
- [ ] 基础配置和开发环境搭建

### Phase 1.2: 认证模块开发 (1周)
- [ ] 用户认证功能
- [ ] JWT令牌管理
- [ ] 前端登录页面
- [ ] 权限控制基础框架

### Phase 1.3: 设备查询功能 (2周)
- [ ] 设备列表查询API
- [ ] 设备详情查询API
- [ ] 前端设备管理页面
- [ ] 查询条件和分页功能

### Phase 1.4: 统计和管理功能 (1.5周)
- [ ] 设备统计API开发
- [ ] 统计图表前端实现
- [ ] 设备类型管理
- [ ] 系统配置管理

### Phase 1.5: 测试和优化 (1周)
- [ ] 单元测试编写
- [ ] 集成测试执行
- [ ] 性能优化
- [ ] Bug修复

### Phase 1.6: 部署和文档 (0.5周)
- [ ] 生产环境部署
- [ ] API文档完善
- [ ] 用户手册编写
- [ ] 项目交付

## 风险评估与应对

### 1. 技术风险
**风险**: 技术选型不当导致性能问题
**应对**: 充分的技术调研和原型验证

### 2. 进度风险
**风险**: 开发进度滞后
**应对**: 合理的任务拆分和优先级管理

### 3. 质量风险
**风险**: 代码质量不达标
**应对**: 代码审查机制和自动化测试

## 后续扩展规划

Phase 1 完成后，为后续阶段准备：
1. **微服务化改造** - 支持更大规模的系统
2. **实时监控功能** - WebSocket实时数据推送
3. **移动端应用** - 基于当前API开发移动应用
4. **大数据分析** - 集成大数据处理能力
5. **AI智能分析** - 设备故障预测和智能运维

## 相关文档

- [数据库设计文档](./database-design-phase1-v1.0.sql)
- [Phase 1 需求规格说明书](./requirements-phase1-device-query-v1.0.md)
- [项目总体说明](../CLAUDE.md)
- [API接口文档](./api-design-phase1-v1.0.md) (待创建)
- [部署指南](./deployment-guide-phase1-v1.0.md) (待创建)