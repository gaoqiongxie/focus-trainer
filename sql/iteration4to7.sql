-- ================================================================
-- 第4-7批 DDL：新增表
-- ================================================================

-- ----------------------------
-- 1. 称号定义表（系统预置）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_title` (
    `title_id`      INT          NOT NULL AUTO_INCREMENT COMMENT '称号ID',
    `title_key`     VARCHAR(64)  NOT NULL UNIQUE COMMENT '称号唯一标识',
    `name`          VARCHAR(64)  NOT NULL COMMENT '称号名称',
    `description`   VARCHAR(255) NOT NULL COMMENT '称号描述',
    `icon`          VARCHAR(64)  NOT NULL DEFAULT '🏅' COMMENT '图标emoji',
    `category`      VARCHAR(32)  NOT NULL COMMENT '类别: level/special/achievement',
    `unlock_type`   VARCHAR(32)  NOT NULL COMMENT '解锁条件类型',
    `unlock_value`  INT          NOT NULL DEFAULT 0 COMMENT '解锁条件阈值',
    `display_order` INT          NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `is_active`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`title_id`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='称号定义表';

-- ----------------------------
-- 2. 用户称号记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_title_record` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT   NOT NULL COMMENT '用户ID',
    `title_id`     INT      NOT NULL COMMENT '称号ID',
    `is_equipped`  TINYINT  NOT NULL DEFAULT 0 COMMENT '是否装备中(0:否 1:是)',
    `unlocked_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '解锁时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_badge` (`user_id`, `title_id`),
    KEY `idx_user_equipped` (`user_id`, `is_equipped`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户称号记录表';

-- ----------------------------
-- 3. 音效配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sound_effect_config` (
    `sound_id`     INT          NOT NULL AUTO_INCREMENT COMMENT '音效ID',
    `sound_key`    VARCHAR(64)  NOT NULL UNIQUE COMMENT '音效唯一标识',
    `name`         VARCHAR(64)  NOT NULL COMMENT '音效名称',
    `category`     VARCHAR(32)  NOT NULL COMMENT '类别: reward/game/ui/animal',
    `file_name`    VARCHAR(128) NOT NULL COMMENT '音效文件名',
    `duration_ms`  INT          NOT NULL DEFAULT 0 COMMENT '时长(毫秒)',
    `description`  VARCHAR(255)          COMMENT '描述',
    `is_active`    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `display_order`INT          NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`sound_id`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='音效配置表';

-- ----------------------------
-- 4. 训练会话表（断点续练）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `training_session` (
    `session_id`    BIGINT   NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id`       BIGINT   NOT NULL COMMENT '用户ID',
    `record_id`     BIGINT   NOT NULL COMMENT '训练记录ID',
    `session_data`  TEXT     NOT NULL COMMENT '会话状态数据(JSON)',
    `status`        TINYINT  NOT NULL DEFAULT 1 COMMENT '状态(0:已结束 1:进行中 2:已暂停)',
    `pause_count`   INT      NOT NULL DEFAULT 0 COMMENT '暂停次数',
    `last_pause_at` DATETIME          COMMENT '最后暂停时间',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`session_id`),
    UNIQUE KEY `uk_record_id` (`record_id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练会话表';

-- ----------------------------
-- 5. 家长设置表（训练锁定等）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `parent_setting` (
    `setting_id`   BIGINT   NOT NULL AUTO_INCREMENT COMMENT '设置ID',
    `user_id`      BIGINT   NOT NULL COMMENT '用户ID(家长)',
    `child_id`     BIGINT   NOT NULL COMMENT '孩子ID',
    `training_lock`TINYINT  NOT NULL DEFAULT 0 COMMENT '训练锁定模式(0:关闭 1:开启)',
    `daily_limit_min` INT   NOT NULL DEFAULT 120 COMMENT '每日训练时长限制(分钟)',
    `remind_time`  VARCHAR(10)         DEFAULT '08:00' COMMENT '每日提醒时间(HH:mm)',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`setting_id`),
    UNIQUE KEY `uk_parent_child` (`user_id`, `child_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家长设置表';

-- ----------------------------
-- 6. 数据导出记录表（隐私合规）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `data_export_record` (
    `export_id`    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '导出ID',
    `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
    `file_path`    VARCHAR(255) NOT NULL COMMENT '导出文件路径',
    `file_size`    BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态(0:生成中 1:完成 2:失败)',
    `expire_time`  DATETIME              COMMENT '过期时间',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`export_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据导出记录表';

-- ================================================================
-- 称号种子数据（15个称号）
-- ================================================================

-- 等级称号（按综合评分解锁）
INSERT INTO `user_title` (`title_key`, `name`, `description`, `icon`, `category`, `unlock_type`, `unlock_value`, `display_order`) VALUES
('beginner',    '初学者',   '刚踏上训练之旅的小朋友',       '🌱', 'level',      'total_score',   20,  1),
('trainee',     '练习生',   '正在努力训练中',               '📝', 'level',      'total_score',   40,  2),
('expert',      '小能手',   '训练技巧越来越熟练',           '⭐', 'level',      'total_score',   60,  3),
('master',      '高手',     '已经是一名训练高手了',         '🏆', 'level',      'total_score',   80,  4),
('grandmaster', '大师',     '登峰造极，专注力大师',         '👑', 'level',      'total_score',   95,  5);

-- 专项称号
INSERT INTO `user_title` (`title_key`, `name`, `description`, `icon`, `category`, `unlock_type`, `unlock_value`, `display_order`) VALUES
('visual_expert',   '视觉达人',   '视觉注意力训练表现优异',     '👁️', 'special',    'visual_score',   80, 10),
('memory_master',   '记忆大师',   '记忆力训练表现优异',         '🧠', 'special',    'memory_score',   80, 11),
('focus_star',      '专注之星',   '专注时长训练表现优异',       '💫', 'special',    'focus_score',    80, 12),
('auditory_genius', '听觉天才',   '听觉注意力训练表现优异',     '👂', 'special',    'auditory_score', 80, 13),
('control_king',    '自控达人',   '抑制控制训练表现优异',       '🛡️', 'special',    'control_score',  80, 14),
('streak_legend',   '连续打卡王', '连续21天以上打卡',           '🔥', 'achievement','streak_days',   21, 20),
('training_fan',    '训练狂人',   '累计完成100次训练',          '💪', 'achievement','training_count', 100, 21),
('star_collector',  '星星收藏家', '累计获得500颗星星',          '💎', 'achievement','total_stars',   500, 22),
('all_rounder',     '全能选手',   '五维能力全部达到60分以上',   '🌈', 'special',    'all_dimensions', 60, 15);

-- ================================================================
-- 音效配置种子数据
-- ================================================================
INSERT INTO `sound_effect_config` (`sound_key`, `name`, `category`, `file_name`, `duration_ms`, `description`, `display_order`) VALUES
-- 奖励音效
('reward_star',      '获得星星',   'reward', 'reward_star.mp3',      1500, '训练完成获得星星', 1),
('reward_badge',     '获得徽章',   'reward', 'reward_badge.mp3',     2000, '解锁新徽章', 2),
('reward_levelup',   '等级提升',   'reward', 'reward_levelup.mp3',   2500, '称号/等级提升', 3),
('reward_perfect',   '完美通关',   'reward', 'reward_perfect.mp3',   3000, '满正确率通关', 4),
-- 游戏音效
('game_correct',     '答对',       'game',   'game_correct.mp3',     500,  '答对提示音', 10),
('game_wrong',       '答错',       'game',   'game_wrong.mp3',       500,  '答错提示音', 11),
('game_click',       '点击',       'game',   'game_click.mp3',       200,  '通用点击音', 12),
('game_countdown',   '倒计时',     'game',   'game_countdown.mp3',   1000, '倒计时提示', 13),
('game_start',       '开始训练',   'game',   'game_start.mp3',       1500, '训练开始', 14),
('game_complete',    '训练完成',   'game',   'game_complete.mp3',    2000, '训练结束', 15),
-- UI音效
('ui_tab',           '切换标签',   'ui',     'ui_tab.mp3',           150,  '底部导航切换', 20),
('ui_notification',  '通知',       'ui',     'ui_notification.mp3',  800,  '收到通知', 21),
-- 动物叫声（声音序列游戏用）
('animal_cat',       '小猫',       'animal', 'animal_cat.mp3',       800,  '小猫叫声', 30),
('animal_dog',       '小狗',       'animal', 'animal_dog.mp3',       800,  '小狗叫声', 31),
('animal_bird',      '小鸟',       'animal', 'animal_bird.mp3',      600,  '小鸟叫声', 32),
('animal_frog',      '青蛙',       'animal', 'animal_frog.mp3',      700,  '青蛙叫声', 33),
('animal_cow',       '奶牛',       'animal', 'animal_cow.mp3',       1000, '奶牛叫声', 34),
('animal_horse',     '小马',       'animal', 'animal_horse.mp3',     900,  '小马叫声', 35),
('animal_chicken',   '小鸡',       'animal', 'animal_chicken.mp3',   500,  '小鸡叫声', 36),
('animal_duck',      '小鸭',       'animal', 'animal_duck.mp3',      600,  '小鸭叫声', 37);
