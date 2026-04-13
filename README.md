# 🧠 Focus Trainer - 儿童注意力训练APP后端

> 通过科学有效的训练方法，帮助3-12岁儿童提升专注力和注意力。

## 📋 项目简介

Focus Trainer 后端服务，基于 Spring Boot + MyBatis-Plus 构建，为儿童注意力训练APP提供完整的API支持，包括用户管理、训练系统、能力评估、激励体系和消息通知。

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 1.8 | 开发语言 |
| Spring Boot | 2.7.18 | 应用框架 |
| MyBatis-Plus | 3.5.5 | ORM框架 |
| MySQL | 8.0 | 数据库 |
| Redis | - | 缓存/会话 |
| JWT | 0.9.1 | 认证鉴权 |
| Knife4j | 3.0.3 | API文档 |
| Hutool | 5.8.25 | 工具库 |

## 📁 项目结构

```
focus-trainer/
├── sql/                              # 数据库脚本
│   ├── init.sql                      # 建表 + 初始化数据
│   ├── badge.sql                     # 徽章表DDL + 种子数据（15个徽章）
│   └── seed.sql                      # 种子数据（测试数据）
├── src/main/
│   ├── java/com/focuskids/trainer/
│   │   ├── FocusTrainerApplication   # 启动类
│   │   ├── common/                   # 通用模块
│   │   │   ├── api/                  # 统一响应、错误码、异常处理
│   │   │   ├── base/                 # 基础类（BaseEntity、分页）
│   │   │   └── mybatis/              # 自动填充处理器
│   │   ├── config/                   # 配置类
│   │   │   ├── CorsConfig            # 跨域配置
│   │   │   ├── JacksonConfig         # JSON序列化配置
│   │   │   ├── JwtInterceptor        # JWT拦截器
│   │   │   ├── MybatisPlusConfig     # MP配置
│   │   │   └── WebMvcConfig          # Web MVC配置
│   │   ├── controller/               # 控制器层
│   │   │   ├── AuthController        # 认证（注册/登录）
│   │   │   ├── UserController        # 用户管理
│   │   │   ├── TrainingController    # 训练管理
│   │   │   ├── EvaluationController  # 能力评估
│   │   │   ├── RewardController      # 激励系统
│   │   │   ├── DailyTaskController   # 每日任务
│   │   │   └── NotificationController # 通知消息
│   │   ├── entity/                   # 实体类（10张表）
│   │   ├── mapper/                   # Mapper接口
│   │   ├── service/                  # 业务逻辑层
│   │   │   ├── AuthService           # 认证服务
│   │   │   ├── TrainingService       # 训练服务
│   │   │   ├── EvaluationService     # 评估服务
│   │   │   ├── RewardService         # 激励服务
│   │   │   ├── BadgeService          # 徽章服务
│   │   │   ├── BadgeRuleEngine       # 徽章规则引擎
│   │   │   ├── DailyTaskService      # 每日任务服务
│   │   │   └── NotificationService   # 通知服务
│   │   └── util/                     # 工具类
│   │       └── JwtUtil               # JWT工具
│   └── resources/
│       ├── application.yml           # 应用配置
│       └── mapper/                   # MyBatis XML映射
└── pom.xml                           # Maven依赖配置
```

## 🗄️ 数据库设计（10张核心表）

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表（家长+儿童） |
| `training_config` | 训练配置表（17条，含category分类） |
| `training_record` | 训练记录表 |
| `user_ability` | 用户能力评估表 |
| `reward_record` | 奖励记录表 |
| `user_streak` | 连续打卡记录表 |
| `daily_task` | 每日任务表 |
| `notification` | 消息通知表 |
| `badge` | 徽章定义表（15个预置徽章） |
| `user_badge` | 用户徽章领取记录表 |

## 🚀 快速开始

### 环境要求

- JDK 1.8+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+

### 1. 初始化数据库

```bash
# 第一次：建表 + 基础数据（训练配置）
mysql -u root -p < sql/init.sql

# 创建徽章表（必须在 seed.sql 之前）
mysql -u root -p focus_trainer < sql/badge.sql

# 导入测试数据（可选，用于开发/演示）
mysql -u root -p focus_trainer < sql/seed.sql
```

> `seed.sql` 包含1个测试家庭（家长+儿童），30天模拟训练记录、能力评估曲线、星星奖励、已解锁徽章、打卡记录等。

### 2. 修改配置

编辑 `src/main/resources/application.yml`，修改数据库和Redis连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/focus_trainer?...
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
```

### 3. 启动服务

```bash
# Maven方式
mvn clean install
mvn spring-boot:run

# 或直接运行jar
java -jar target/focus-trainer-0.1.0.jar
```

### 4. 访问API文档

启动成功后访问：`http://localhost:8080/api/v1/doc.html`

### 🧪 测试账号

导入 `seed.sql` 后可使用以下测试账号登录（密码统一为 `focuskids123`）：

| 角色 | 手机号 | 密码 | 说明 |
|------|--------|------|------|
| 家长 | 13800138001 | focuskids123 | 可查看儿童报告、绑定多个儿童 |
| 儿童 | 13800138002 | focuskids123 | 8岁/二年级/男，连续打卡15天，累计~370星，已解锁7个徽章 |

## 📡 API接口列表

### 认证模块 `/auth`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/register` | 用户注册 |
| POST | `/auth/login` | 用户登录 |
| POST | `/auth/refresh` | 刷新Token |
| POST | `/auth/bind-child` | 家长绑定儿童 |

### 用户模块 `/user`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/profile` | 获取个人信息 |
| PUT | `/user/profile` | 更新个人信息 |
| GET | `/user/children` | 获取儿童列表 |

### 训练模块 `/training`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/training/config` | 获取训练配置 |
| POST | `/training/start` | 开始训练 |
| POST | `/training/complete` | 完成训练 |
| POST | `/training/interrupt` | 中断训练 |
| GET | `/training/records` | 训练记录列表 |
| GET | `/training/statistics` | 训练统计数据 |

### 评估模块 `/evaluation`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/evaluation/ability` | 获取能力评估 |
| GET | `/evaluation/report` | 获取评估报告 |
| GET | `/evaluation/progress` | 进步趋势 |

### 激励模块 `/reward`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/reward/stars` | 星星数量 |
| GET | `/reward/badges` | 徽章列表（含15个预置徽章，已解锁/未解锁状态） |
| GET | `/reward/streak` | 连续打卡 |
| GET | `/reward/leaderboard` | 排行榜 |

### 每日任务模块 `/daily-task`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/daily-task/today` | 获取今日任务列表 |
| GET | `/daily-task/overview` | 获取今日任务概况 |
| POST | `/daily-task/complete` | 完成任务 |
| POST | `/daily-task/claim` | 领取任务奖励 |

### 通知模块 `/notification`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/notification/list` | 通知列表 |
| PUT | `/notification/read/{id}` | 标记已读 |
| PUT | `/notification/read-all` | 全部已读 |

## 🔐 认证说明

所有需要登录的接口请在请求头中携带JWT Token：

```
Authorization: Bearer <token>
```

## 📄 License

MIT
