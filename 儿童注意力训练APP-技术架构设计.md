# 儿童注意力训练APP - 技术架构设计

## 文档信息
- **文档版本**: v1.0
- **创建日期**: 2026-03-25
- **技术架构**: 移动端 + 后端 + 数据库 + 云服务

---

## 一、技术选型

### 1.1 整体架构
采用前后端分离的移动应用架构，支持多端扩展。

```
┌─────────────────────────────────────────────────────────┐
│                     用户层                              │
├──────────────────┬──────────────────────────────────────┤
│   iOS Client     │      Android Client                  │
│   (Flutter)      │      (Flutter)                       │
└──────────────────┴──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                    网关层                               │
│              (Nginx + HTTPS + 负载均衡)                   │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   应用层                                │
├──────────────────┬──────────────────────────────────────┤
│   用户服务       │      训练服务                        │
│   (用户认证/管理) │      (训练记录/统计)                │
├──────────────────┼──────────────────────────────────────┤
│   评估服务       │      推荐服务                        │
│   (能力评估/分析) │      (个性化推荐)                    │
├──────────────────┼──────────────────────────────────────┤
│   通知服务       │      文件服务                        │
│   (消息推送)     │      (图片/音频存储)                 │
└──────────────────┴──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   数据层                                │
├──────────────────┬──────────────────────────────────────┤
│   MySQL          │      Redis                          │
│   (业务数据)     │      (缓存/会话)                     │
├──────────────────┼──────────────────────────────────────┤
│   MongoDB        │      InfluxDB                       │
│   (日志数据)     │      (时序数据)                      │
└──────────────────┴──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   云服务层                              │
│              (腾讯云: COS/CDN/监控/告警)                 │
└─────────────────────────────────────────────────────────┘
```

### 1.2 移动端技术栈
| 组件 | 技术选择 | 说明 |
|------|---------|------|
| 开发框架 | Flutter 3.0+ | 跨平台开发，iOS和Android统一代码 |
| 状态管理 | Provider | 轻量级状态管理 |
| 网络请求 | Dio | 支持拦截器、缓存、超时设置 |
| 本地存储 | Hive + SharedPreferences | Hive存储复杂数据，SP存储简单配置 |
| 路由管理 | fluro | 灵活的路由跳转 |
| UI组件 | Material Design | 符合儿童友好的设计规范 |
| 音频处理 | audioplayers | 训练音频播放 |
| 视频处理 | video_player | 训练视频播放 |
| 图像处理 | cached_network_image | 图片缓存 |
| 动画效果 | Lottie | 流畅的动画效果 |

### 1.3 后端技术栈
| 组件 | 技术选择 | 说明 |
|------|---------|------|
| 开发框架 | Spring Boot 2.7+ | 成熟稳定的Java企业级框架 |
| ORM框架 | MyBatis-Plus | 增强MyBatis，简化开发 |
| 数据库 | MySQL 8.0 | 关系型数据库，存储核心业务数据 |
| 缓存 | Redis 6.0+ | 缓存热点数据、分布式锁、会话管理 |
| 消息队列 | RabbitMQ | 异步任务、解耦系统 |
| 对象存储 | 腾讯云COS | 图片、音频、视频文件存储 |
| CDN | 腾讯云CDN | 加速静态资源访问 |
| 搜索引擎 | Elasticsearch | 日志检索、用户行为分析 |
| 监控告警 | Prometheus + Grafana | 系统监控、性能分析 |
| 日志收集 | ELK Stack | 日志收集、存储、分析 |
| API文档 | Swagger | 自动生成API文档 |
| 容器化 | Docker + K8s | 服务部署、扩缩容 |

---

## 二、数据库设计

### 2.1 核心表结构

#### 2.1.1 用户表 (sys_user)
```sql
CREATE TABLE sys_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    user_type TINYINT NOT NULL COMMENT '用户类型(1:儿童 2:家长)',
    parent_id BIGINT COMMENT '家长ID(儿童用户关联)',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    phone VARCHAR(11) COMMENT '手机号',
    password VARCHAR(128) COMMENT '密码(BCrypt加密)',
    age TINYINT COMMENT '年龄',
    gender TINYINT COMMENT '性别(0:未知 1:男 2:女)',
    grade TINYINT COMMENT '年级(1-6)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0:禁用 1:正常)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_phone (phone),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

#### 2.1.2 训练配置表 (training_config)
```sql
CREATE TABLE training_config (
    config_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    training_type TINYINT NOT NULL COMMENT '训练类型(1:专注时长 2:视觉追踪 3:听觉专注 4:记忆训练)',
    level TINYINT NOT NULL COMMENT '难度等级(1-10)',
    duration INT COMMENT '训练时长(秒)',
    config_json TEXT COMMENT '详细配置(JSON)',
    is_active TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_type_level (training_type, level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练配置表';
```

#### 2.1.3 训练记录表 (training_record)
```sql
CREATE TABLE training_record (
    record_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    training_type TINYINT NOT NULL COMMENT '训练类型',
    level TINYINT NOT NULL COMMENT '难度等级',
    duration INT NOT NULL COMMENT '计划时长(秒)',
    actual_duration INT NOT NULL COMMENT '实际时长(秒)',
    status TINYINT NOT NULL COMMENT '状态(0:未完成 1:完成 2:中断)',
    interrupt_count INT DEFAULT 0 COMMENT '中断次数',
    accuracy DECIMAL(5,2) COMMENT '正确率(%)',
    score INT COMMENT '得分',
    star_reward INT COMMENT '获得星星数',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    device_info VARCHAR(255) COMMENT '设备信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_time (user_id, start_time),
    INDEX idx_type_date (training_type, DATE(start_time))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练记录表';
```

#### 2.1.4 用户能力评估表 (user_ability)
```sql
CREATE TABLE user_ability (
    ability_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评估ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    attention_duration DECIMAL(5,2) COMMENT '专注时长得分',
    visual_attention DECIMAL(5,2) COMMENT '视觉注意力得分',
    auditory_attention DECIMAL(5,2) COMMENT '听觉注意力得分',
    working_memory DECIMAL(5,2) COMMENT '工作记忆得分',
    inhibitory_control DECIMAL(5,2) COMMENT '抑制控制得分',
    total_score DECIMAL(5,2) COMMENT '总分',
    level VARCHAR(20) COMMENT '能力等级(A/B/C/D/E)',
    evaluate_date DATE NOT NULL COMMENT '评估日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_date (user_id, evaluate_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户能力评估表';
```

#### 2.1.5 激励记录表 (reward_record)
```sql
CREATE TABLE reward_record (
    reward_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '奖励ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    reward_type TINYINT NOT NULL COMMENT '奖励类型(1:星星 2:徽章 3:成就)',
    reward_value INT COMMENT '奖励值(星星数量/徽章ID)',
    reward_name VARCHAR(50) COMMENT '奖励名称',
    source_type TINYINT COMMENT '来源类型(1:训练 2:签到 3:活动)',
    source_id BIGINT COMMENT '来源ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='激励记录表';
```

#### 2.1.6 用户连续记录表 (user_streak)
```sql
CREATE TABLE user_streak (
    streak_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL UNIQUE KEY COMMENT '用户ID',
    current_streak INT DEFAULT 0 COMMENT '当前连续天数',
    max_streak INT DEFAULT 0 COMMENT '最大连续天数',
    last_train_date DATE COMMENT '最后训练日期',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户连续记录表';
```

#### 2.1.7 通知消息表 (notification)
```sql
CREATE TABLE notification (
    notification_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type TINYINT NOT NULL COMMENT '通知类型(1:训练提醒 2:奖励 3:建议)',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读(0:未读 1:已读)',
    extra_data TEXT COMMENT '扩展数据(JSON)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_read (user_id, is_read, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息表';
```

### 2.2 分表策略
- training_record表按月分表：training_record_202603, training_record_202604...
- user_behavior日志表按日分表

---

## 三、后端API设计

### 3.1 API设计规范
- RESTful API风格
- 统一响应格式
- JWT token认证
- 接口版本号：/api/v1/

### 3.2 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1711353600000
}
```

### 3.3 核心API接口

#### 3.3.1 用户认证模块
```
POST   /api/v1/auth/register           # 用户注册
POST   /api/v1/auth/login              # 用户登录
POST   /api/v1/auth/logout             # 用户登出
POST   /api/v1/auth/refresh-token      # 刷新token
POST   /api/v1/auth/child-bind         # 家长绑定儿童
```

#### 3.3.2 用户信息模块
```
GET    /api/v1/user/profile            # 获取用户信息
PUT    /api/v1/user/profile            # 更新用户信息
GET    /api/v1/user/children           # 获取儿童列表（家长）
GET    /api/v1/user/parent             # 获取家长信息（儿童）
```

#### 3.3.3 训练模块
```
GET    /api/v1/training/config        # 获取训练配置
POST   /api/v1/training/start          # 开始训练
POST   /api/v1/training/complete       # 完成训练
POST   /api/v1/training/interrupt      # 中断训练
GET    /api/v1/training/records        # 获取训练记录
GET    /api/v1/training/statistics     # 获取训练统计
```

#### 3.3.4 评估模块
```
POST   /api/v1/evaluation/initialize   # 初始化能力评估
POST   /api/v1/evaluation/submit       # 提交评估
GET    /api/v1/evaluation/result       # 获取评估结果
GET    /api/v1/evaluation/history      # 获取评估历史
```

#### 3.3.5 激励模块
```
GET    /api/v1/reward/stars            # 获取星星数量
GET    /api/v1/reward/badges           # 获取徽章列表
GET    /api/v1/reward/achievements     # 获取成就列表
GET    /api/v1/reward/streak           # 获取连续记录
```

#### 3.3.6 通知模块
```
GET    /api/v1/notification/list       # 获取通知列表
POST   /api/v1/notification/read       # 标记已读
POST   /api/v1/notification/settings   # 更新通知设置
```

---

## 四、移动端架构

### 4.1 项目结构
```
lib/
├── main.dart                    # 应用入口
├── config/                      # 配置文件
│   ├── app_config.dart
│   └── env_config.dart
├── constants/                   # 常量定义
│   ├── constants.dart
│   └── strings.dart
├── models/                      # 数据模型
│   ├── user_model.dart
│   ├── training_model.dart
│   └── reward_model.dart
├── providers/                   # 状态管理
│   ├── user_provider.dart
│   ├── training_provider.dart
│   └── reward_provider.dart
├── services/                    # 服务层
│   ├── api_service.dart
│   ├── storage_service.dart
│   └── audio_service.dart
├── pages/                       # 页面
│   ├── home/                    # 首页
│   ├── training/                # 训练页
│   ├── profile/                 # 个人中心
│   └── parent/                  # 家长端
├── widgets/                     # 通用组件
│   ├── common_widgets.dart
│   ├── training_widgets.dart
│   └── reward_widgets.dart
├── utils/                       # 工具类
│   ├── http_util.dart
│   ├── date_util.dart
│   └── validator.dart
└── l10n/                        # 国际化
    └── messages_zh.dart
```

### 4.2 核心页面

#### 4.2.1 儿童端主页
- 显示今日任务
- 显示星星数量
- 显示连续训练天数
- 快速进入训练

#### 4.2.2 训练选择页
- 列出所有训练模块
- 显示训练难度
- 显示训练进度
- 开始训练按钮

#### 4.2.3 训练进行页
- 倒计时显示
- 训练任务展示
- 退出/暂停按钮
- 防止切换应用

#### 4.2.4 训练结果页
- 显示成绩
- 显示获得星星
- 鼓励动画
- 查看详情/再训练一次

#### 4.2.5 家长端主页
- 孩子训练统计
- 今日训练总结
- 数据趋势图
- 推荐建议

### 4.3 状态管理
使用Provider进行状态管理，主要Provider：
- UserProvider：用户信息管理
- TrainingProvider：训练状态管理
- RewardProvider：奖励状态管理
- ParentProvider：家长端状态管理

---

## 五、核心算法设计

### 5.1 个性化难度调整算法

#### 算法思路
1. 收集最近N次训练数据
2. 计算平均正确率和平均完成时间
3. 根据表现调整难度

#### 伪代码
```python
def adjust_difficulty(user_id, training_type):
    # 获取最近10次训练数据
    records = get_recent_records(user_id, training_type, 10)
    
    # 计算指标
    avg_accuracy = sum(r.accuracy for r in records) / len(records)
    avg_time = sum(r.actual_duration for r in records) / len(records)
    standard_time = get_standard_time(records[0].level)
    
    # 难度调整规则
    current_level = records[0].level
    
    # 表现优秀：正确率>85%且时间<标准时间
    if avg_accuracy > 85 and avg_time < standard_time:
        new_level = min(current_level + 1, 10)
    
    # 表现一般：正确率60-85%
    elif 60 <= avg_accuracy <= 85:
        new_level = current_level
    
    # 表现较差：正确率<60%
    else:
        new_level = max(current_level - 1, 1)
    
    return new_level
```

### 5.2 能力评估算法

#### 评估维度
- 专注时长：最近7天平均训练时长
- 视觉注意力：视觉训练正确率
- 听觉注意力：听觉训练正确率
- 工作记忆：记忆训练正确率
- 抑制控制：任务中断率

#### 评分公式
```
总分 = 专注时长×20% + 视觉注意力×20% + 听觉注意力×20% +
       工作记忆×20% + 抑制控制×20%

等级划分：
A: 90-100分 优秀
B: 80-89分 良好
C: 70-79分 中等
D: 60-69分 及格
E: <60分 待提升
```

---

## 六、性能优化

### 6.1 移动端优化
- 图片懒加载
- 列表分页加载
- 本地缓存策略
- 首页预加载
- 动画优化

### 6.2 后端优化
- Redis缓存热点数据
- 数据库索引优化
- SQL查询优化
- 接口响应压缩
- CDN加速静态资源

### 6.3 数据库优化
- 读写分离
- 分表分库
- 索引优化
- 慢查询监控

---

## 七、安全设计

### 7.1 认证与授权
- JWT token认证
- token过期机制
- 刷新token机制
- 权限分级管理

### 7.2 数据安全
- HTTPS传输
- 密码BCrypt加密
- 敏感数据脱敏
- 日志脱敏

### 7.3 儿童隐私保护
- 符合《儿童个人信息网络保护规定》
- 最小化数据收集
- 家长同意机制
- 数据删除权

---

## 八、监控与运维

### 8.1 系统监控
- 服务器资源监控（CPU、内存、磁盘、网络）
- 应用性能监控（APM）
- 接口响应时间监控
- 错误日志监控

### 8.2 业务监控
- 用户注册数
- 日活/月活用户
- 训练完成率
- 付费转化率

### 8.3 告警机制
- 系统异常告警
- 接口超时告警
- 错误率超阈值告警
- 资源使用率告警

---

## 九、部署方案

### 9.1 开发环境
- 本地开发环境
- 测试服务器
- 模拟数据

### 9.2 生产环境
- 腾讯云服务器
- MySQL主从架构
- Redis集群
- Nginx负载均衡
- Docker容器化部署
- K8s容器编排

### 9.3 CI/CD流程
1. 代码提交
2. 自动化测试
3. 构建Docker镜像
4. 推送镜像仓库
5. 自动部署到测试环境
6. 测试通过后部署到生产环境

---

## 十、AI辅助开发计划

### 10.1 AI代码生成
- 使用AI生成基础代码框架
- 自动生成Entity、Mapper、Service层代码
- 自动生成API接口文档
- 自动生成单元测试

### 10.2 AI辅助设计
- AI生成UI原型
- AI生成数据库设计
- AI生成测试用例
- AI生成代码注释

### 10.3 AI辅助测试
- 自动生成测试数据
- 自动化测试执行
- 测试报告生成
- Bug自动分析

---

**文档结束**
