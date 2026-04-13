-- ================================================================
-- 徽章体系 DDL
-- ================================================================

-- 徽章定义表（系统预置，不可修改）
CREATE TABLE IF NOT EXISTS `badge` (
    `badge_id`       INT          NOT NULL AUTO_INCREMENT COMMENT '徽章ID',
    `badge_key`      VARCHAR(64)  NOT NULL UNIQUE COMMENT '徽章唯一标识',
    `name`           VARCHAR(64)  NOT NULL COMMENT '徽章名称',
    `description`   VARCHAR(255) NOT NULL COMMENT '徽章描述',
    `icon`           VARCHAR(64)  NOT NULL DEFAULT '🏅' COMMENT '图标emoji',
    `category`       VARCHAR(32)  NOT NULL COMMENT '类别: streak/completion/accuracy/stars/special',
    `condition_type` VARCHAR(32)  NOT NULL COMMENT '条件类型',
    `condition_value`INT          NOT NULL COMMENT '条件阈值',
    `star_cost`      INT          NOT NULL DEFAULT 0 COMMENT '解锁所需星星（0=免费）',
    `is_active`      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `display_order`  INT          NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`badge_id`),
    KEY `idx_category`   (`category`),
    KEY `idx_condition_type` (`condition_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='徽章定义表';

-- 用户徽章领取记录表
CREATE TABLE IF NOT EXISTS `user_badge` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT      NOT NULL COMMENT '用户ID',
    `badge_id`     INT         NOT NULL COMMENT '徽章ID',
    `earned_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    `source_type`  INT         NOT NULL DEFAULT 1 COMMENT '来源: 1=训练 2=打卡 3=购买 4=系统',
    `source_id`    BIGINT      COMMENT '来源ID（训练记录ID等）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_badge` (`user_id`, `badge_id`),
    KEY `idx_user_id`   (`user_id`),
    KEY `idx_badge_id`  (`badge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户徽章领取记录表';

-- ================================================================
-- 徽章种子数据（15个徽章）
-- ================================================================
INSERT INTO `badge` (`badge_key`, `name`, `description`, `icon`, `category`, `condition_type`, `condition_value`, `display_order`) VALUES
-- ===== 打卡类 =====
('first_train',    '初出茅庐',  '完成第一次训练',                         '🌱', 'completion',  'training_count',    1,   1),
('train_10',       '训练达人',  '累计完成10次训练',                       '💪', 'completion',  'training_count',   10,   2),
('train_50',       '训练狂热',  '累计完成50次训练',                       '🔥', 'completion',  'training_count',   50,   3),
('train_100',      '训练传奇',  '累计完成100次训练',                      '👑', 'completion',  'training_count',  100,   4),

-- ===== 连续打卡类 =====
('streak_3',       '坚持之星',  '连续3天打卡',                           '⭐', 'streak',       'streak_days',       3,  10),
('streak_7',       '连续7天',   '连续7天打卡',                           '🌟', 'streak',       'streak_days',       7,  11),
('streak_14',      '连续14天',  '连续14天打卡',                          '✨', 'streak',       'streak_days',      14,  12),
('streak_21',      '连续21天',  '连续21天打卡，养成习惯！',              '💫', 'streak',       'streak_days',      21,  13),
('streak_30',      '连续30天',  '连续30天打卡，专注王者！',              '🌈', 'streak',       'streak_days',      30,  14),

-- ===== 正确率类 =====
('perfect_100',    '满分通关',  '任意训练达到100%正确率',                '💯', 'accuracy',     'perfect_score',      1,  20),
('accuracy_90_5',  '精准王',    '任意训练5次达到90%以上正确率',          '🎯', 'accuracy',     'accuracy_90_count', 5,  21),

-- ===== 星星收集类 =====
('stars_50',       '小试牛刀',  '累计获得50颗星星',                       '🌙', 'stars',        'total_stars',      50,  30),
('stars_100',      '百星少年',  '累计获得100颗星星',                     '☀️', 'stars',        'total_stars',     100,  31),
('stars_500',      '五星上将',  '累计获得500颗星星',                     '🌟', 'stars',        'total_stars',     500,  32),
('stars_1000',     '千星传奇',  '累计获得1000颗星星',                    '💎', 'stars',        'total_stars',    1000,  33);
