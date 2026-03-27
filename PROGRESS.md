# Focus Trainer — 开发任务进度记录

> 项目：儿童专注力训练 APP（FocusKids）  
> 后端仓库：https://github.com/gaoqiongxie/focus-trainer  
> 前端仓库：https://github.com/gaoqiongxie/focus-trainer-mobile  
> 更新时间：2026-03-27

---

## 整体进度概览

| 阶段 | 内容 | 状态 | 完成时间 |
|------|------|------|---------|
| Phase 0 | 项目初始化 & 架构设计 | ✅ 完成 | 2026-03-25 |
| Phase 1 | P0 安全与编译问题修复 | ✅ 完成 | 2026-03-25 |
| Phase 2 | 4个训练游戏前端实现 | ✅ 完成 | 2026-03-26 |
| Phase 3 | 首页重构 & 游戏选择页 | ✅ 完成 | 2026-03-26 |
| Phase 4 | 家长端数据报告（前端+后端） | ✅ 完成 | 2026-03-26 |
| Phase 5 | 后端训练配置完善 | ✅ 完成 | 2026-03-26 |
| Phase 6 | 系统 UI 设计图 | ✅ 完成 | 2026-03-27 |
| Phase 7 | 自适应难度系统 | 🔜 待开发 | - |
| Phase 8 | 成就系统 & 勋章体系 | 🔜 待开发 | - |
| Phase 9 | 音效集成（audioplayers） | 🔜 待开发 | - |

---

## Phase 0 · 项目初始化

### 技术选型
- **后端**：Java 1.8 + Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 + Redis + JWT
- **前端**：Flutter 3.0+ + Provider + Dio + charts_flutter + audioplayers + lottie
- **数据库**：MySQL（focus_trainer）
- **包名**：`com.focuskids.trainer`

### 核心实体设计
| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表（儿童/家长账号） |
| `training_config` | 训练游戏配置（类型/难度/参数） |
| `training_record` | 训练记录（成绩/得分/时长） |
| `reward_record` | 奖励记录（星星） |

---

## Phase 1 · P0 安全与编译问题修复

### 修复清单（共11项）
| # | 问题 | 修复方案 |
|---|------|---------|
| 1 | IDOR 漏洞（越权查询他人记录） | 在 Service 层校验 userId == currentUserId |
| 2 | 字段白名单缺失 | 使用 VO 层过滤，不直接返回 Entity |
| 3 | JWT 密钥硬编码 | 迁移至 application.yml 外部配置 |
| 4 | 并发竞态（星星计数） | Redis INCR 原子操作替换 SELECT+UPDATE |
| 5 | TrainingRecord 缺少 userId 字段 | 添加字段并完善关联查询 |
| 6 | R 类无 fail() 方法 | 统一使用 `R.error()` |
| 7 | 注解冲突（@Autowired + 构造注入） | 统一改为构造注入 |
| 8 | SQL 注入风险 | 参数绑定，禁止字符串拼接 |
| 9 | 缺少全局异常处理 | 添加 GlobalExceptionHandler |
| 10 | 密码明文存储 | BCrypt 加密 |
| 11 | 编译错误（泛型警告等） | 修复类型声明 |

---

## Phase 2 · 4个训练游戏前端实现

### 游戏1：舒尔特方格（SchulteGridScreen）
- **文件**：`lib/screens/games/schulte_grid_screen.dart`
- **难度**：初级(3×3) / 中级(4×4) / 高级(5×5)
- **玩法**：随机乱序数字填满方格，按从小到大顺序依次点击
- **功能**：实时计时、错误统计、正确/错误颜色动画反馈、星级评价
- **计分**：按完成时间和错误次数综合评分，3/2/1星

### 游戏2：数字闪现（FlashNumberScreen）
- **文件**：`lib/screens/games/flash_number_screen.dart`
- **难度**：初级(3位,2s) / 中级(4位,1.5s) / 高级(5位,1s)
- **玩法**：屏幕短暂闪现数字，隐藏后用内置键盘输入
- **功能**：内置自定义数字键盘、逐轮成绩反馈、完整成绩单

### 游戏3：卡片配对（CardMatchScreen）
- **文件**：`lib/screens/games/card_match_screen.dart`
- **难度**：初级(6对) / 中级(8对) / 高级(10对)
- **玩法**：翻牌找到相同 emoji 图案的两张牌完成配对
- **功能**：翻转动画、步数统计、配对成功/失败视觉反馈、星级评价

### 游戏4：声音序列（SoundSequenceScreen）
- **文件**：`lib/screens/games/sound_sequence_screen.dart`
- **难度**：初级(3音) / 中级(5音) / 高级(7音)
- **玩法**：记住动物声音顺序，播放完成后按顺序回放
- **功能**：动物 emoji 代替音效（后续接入 audioplayers）、脉冲动画、轮次进度

---

## Phase 3 · 首页重构 & 游戏选择页

### HomeScreen（home_screen.dart）
- **头部**：渐变蓝紫 AppBar，显示用户名、累计星星、连续天数、本周次数
- **内容**：4个训练模块卡片（视觉/听觉/记忆/专注） + 快速开始按钮
- **底部导航**：首页 / 报告 / 我的（3个 Tab）

### GameSelectionScreen（game_selection_screen.dart）
- 按训练类型分组显示游戏列表
- 每个游戏展示难度按钮（初级绿/中级蓝/高级紫）
- 点击难度按钮直接进入对应游戏 Screen

---

## Phase 4 · 家长端数据报告

### 后端 API（5个接口）
| 接口 | 路径 | 说明 |
|------|------|------|
| 仪表板总览 | GET /api/parent/report/dashboard | 今日/本周训练次数、星星、综合评分 |
| 训练趋势 | GET /api/parent/report/trend | 最近N天每日训练次数折线图数据 |
| 能力分析 | GET /api/parent/report/ability | 四维能力雷达图数据 |
| 训练记录 | GET /api/parent/report/records | 分页训练记录明细 |
| 周报摘要 | GET /api/parent/report/weekly | 本周亮点+建议 |

- **文件**：`ParentReportController.java` / `ParentReportService.java` / `ParentReportServiceImpl.java`
- **权限**：JWT 认证，自动校验家长-儿童绑定关系

### 前端（ParentReportScreen）
- **文件**：`lib/screens/parent/parent_report_screen.dart`
- **Tab 1 总览**：4个统计数字卡片 + 4维能力进度条 + 训练洞察文字
- **Tab 2 趋势**：最近7天训练次数柱状图（charts_flutter）
- **Tab 3 记录**：训练记录列表（时间/游戏/得分/星级）

---

## Phase 5 · 后端训练配置完善

### init.sql 更新
为每种游戏类型添加详细 `config_json`：

```json
// 舒尔特方格-中级
{"game":"schulte","gridSize":4,"standardTime":40,"scoring":{"perfect":40,"good":20,"base":5}}

// 数字闪现-高级
{"game":"flash","digits":5,"displayMs":1000,"rounds":10,"scorePerRound":10}

// 卡片配对-初级
{"game":"card_match","pairs":6,"gridColumns":4,"scoreBase":100}

// 声音序列-中级
{"game":"sound_sequence","seqLength":5,"rounds":5,"animals":8,"scorePerRound":50}
```

### 训练类型扩展
| type | 说明 |
|------|------|
| 1 | 专注时长 |
| 2 | 视觉追踪（舒尔特方格） |
| 3 | 听觉专注（声音序列） |
| 4 | 记忆训练（卡片配对） |
| 21 | 数字闪现（视觉追踪子类） |

---

## Phase 6 · 系统 UI 设计图

### 设计产出
- **文件**：`ui/focus_trainer_ui.html`（前端仓库）
- **内容**：
  - 设计系统：色彩令牌8色、5级字体、8档间距
  - 核心组件库：按钮/标签/星级/进度条/输入框
  - 页面导航流程图
  - 9个页面原型图（手机框架展示）

### 设计规范
| 规范项 | 值 |
|--------|---|
| 主色 Primary | #4A90D9 蓝 |
| 辅色 Secondary | #6C63FF 紫 |
| 成功色 Success | #43C59E 绿 |
| 错误色 Error | #FF6B6B 红 |
| 星级金 Gold | #FBBF24 |
| 背景色 Background | #F0F2F8 |
| 基础间距 | 4px（4/8/12/16/24/32/48/64） |

---

## 待开发功能（Backlog）

### 高优先级
- [ ] **自适应难度系统**：根据历史成绩自动推荐下一难度
- [ ] **音效集成**：audioplayers 集成真实动物/乐器音效
- [ ] **离线缓存**：无网络时也可训练，联网后同步数据

### 中优先级
- [ ] **成就系统**：勋章/徽章/排行榜
- [ ] **家长推送通知**：每日训练提醒、周报推送
- [ ] **儿童多账号**：一个家长账号绑定多个孩子

### 低优先级
- [ ] **深色模式**
- [ ] **多语言（i18n）**
- [ ] **导出报告 PDF**

---

## Git 提交记录

### 后端（gaoqiongxie/focus-trainer）
| Commit | 说明 |
|--------|------|
| `ba6597b` | feat: 添加家长端数据报告API + 完善训练配置数据 |

### 前端（gaoqiongxie/focus-trainer-mobile）
| Commit | 说明 |
|--------|------|
| `ae45519` | feat: 补齐4个训练游戏 + 家长端数据报告页面 |
