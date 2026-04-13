-- =============================================
-- Focus Trainer 第4-7批扩展SQL
-- 创建时间: 2026-04-13
-- 包含: 称号表、通知表扩展、隐私配置表
-- =============================================

USE focus_trainer;

-- ----------------------------
-- 9. 用户称号表
-- ----------------------------
CREATE TABLE IF NOT EXISTS user_title (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    user_id         BIGINT       NOT NULL               COMMENT '用户ID',
    title_id        INT          NOT NULL               COMMENT '称号ID',
    title_key       VARCHAR(50)  NOT NULL               COMMENT '称号标识',
    title_name      VARCHAR(50)  NOT NULL               COMMENT '称号名称',
    unlocked_at     DATETIME     NOT NULL               COMMENT '解锁时间',
    is_active       TINYINT      NOT NULL DEFAULT 1     COMMENT '是否当前使用(0:否 1:是)',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_active (user_id, is_active),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户称号表';

-- ----------------------------
-- 10. 通知配置表（推送规则）
-- ----------------------------
CREATE TABLE IF NOT EXISTS notification_config (
    config_id       INT          NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    type            TINYINT      NOT NULL               COMMENT '类型(1:训练提醒 2:奖励通知 3:专家建议)',
    title           VARCHAR(100) NOT NULL               COMMENT '通知标题模板',
    content         VARCHAR(500) NOT NULL               COMMENT '通知内容模板',
    trigger_rule    VARCHAR(255)                        COMMENT '触发规则(JSON)',
    enabled         TINYINT      NOT NULL DEFAULT 1     COMMENT '是否启用',
    cron_expression VARCHAR(50)                          COMMENT '定时任务Cron表达式',
    priority        TINYINT      NOT NULL DEFAULT 5     COMMENT '优先级(1-10)',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知配置表';

-- ----------------------------
-- 11. 用户隐私配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS user_privacy (
    privacy_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    user_id         BIGINT       NOT NULL               COMMENT '用户ID',
    parent_id       BIGINT       NOT NULL               COMMENT '家长用户ID',
    data_retention  TINYINT      NOT NULL DEFAULT 365   COMMENT '数据保留天数',
    allow_analytics TINYINT      NOT NULL DEFAULT 1     COMMENT '允许数据分析(0:否 1:是)',
    allow_notifications TINYINT  NOT NULL DEFAULT 1     COMMENT '允许推送通知(0:否 1:是)',
    training_lock   TINYINT      NOT NULL DEFAULT 0     COMMENT '训练锁定模式(0:关闭 1:开启)',
    lock_duration   INT          NOT NULL DEFAULT 300   COMMENT '锁定时长(秒)',
    last_export     DATETIME                              COMMENT '最后导出时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (privacy_id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户隐私配置表';

-- ----------------------------
-- 12. 训练难度推荐记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS difficulty_recommendation (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    user_id         BIGINT       NOT NULL               COMMENT '用户ID',
    training_type   TINYINT      NOT NULL               COMMENT '训练类型',
    current_level   TINYINT      NOT NULL               COMMENT '当前难度',
    recommended_level TINYINT    NOT NULL               COMMENT '推荐难度',
    confidence      DECIMAL(5,2) DEFAULT 0             COMMENT '置信度(0-100)',
    reason          VARCHAR(255)                        COMMENT '推荐理由',
    status          TINYINT      NOT NULL DEFAULT 0     COMMENT '状态(0:推荐 1:已采纳 2:已忽略)',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_type (user_id, training_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练难度推荐记录表';

-- ----------------------------
-- 13. 数据导出记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS data_export_log (
    export_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '导出ID',
    user_id         BIGINT       NOT NULL               COMMENT '操作用户ID',
    target_user_id  BIGINT       NOT NULL               COMMENT '被导出用户ID',
    export_type     TINYINT      NOT NULL               COMMENT '导出类型(1:全部数据 2:训练记录 3:能力报告)',
    file_path       VARCHAR(255)                        COMMENT '文件路径',
    file_size       BIGINT       DEFAULT 0              COMMENT '文件大小(字节)',
    status          TINYINT      NOT NULL DEFAULT 0     COMMENT '状态(0:处理中 1:完成 2:失败)',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (export_id),
    KEY idx_user_id (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据导出记录表';

-- ----------------------------
-- 插入通知配置数据
-- ----------------------------
INSERT INTO notification_config (type, title, content, trigger_rule, enabled, cron_expression, priority) VALUES
-- 每日训练提醒（早8点）
(1, '🎯 今日训练提醒', '亲爱的{ nickname }，今天还没有训练哦~快来练一练，保持专注好习惯！', '{"type":"daily_reminder","streak_less_than":999}', 1, '0 0 8 * * ?', 5),
-- 连续打卡提醒（晚7点）
(1, '🔥 连续打卡第{ streak }天', '你已经坚持训练{ streak }天了！再坚持一下就能解锁连续打卡徽章啦~', '{"type":"streak_reminder","streak_mod":3}', 1, '0 0 19 * * ?', 6),
-- 奖励获得通知
(2, '⭐ 恭喜获得{ reward_name }！', '太棒了！{ nickname }在训练中表现优异，获得了 { reward_value } 奖励！', '{"type":"reward"}', 1, NULL, 8),
-- 专家建议通知
(3, '📊 能力提升建议', '根据你的训练数据，我们发现 { dimension } 还有提升空间，推荐尝试 { training_name }！', '{"type":"ability_suggestion","score_less_than":70}', 1, '0 0 10 ? * SUN', 4);

-- ----------------------------
-- 插入称号配置数据
-- ----------------------------
INSERT INTO `badge` (badge_key, name, description, icon, category, condition_type, condition_value, star_cost, is_active, display_order) VALUES
-- 等级称号
('title_beginner', '初学者', '刚刚开始训练的新手', '🌱', 'title', 'training_count', 1, 0, 1, 100),
('title_practitioner', '练习生', '已完成10次训练', '📚', 'title', 'training_count', 10, 0, 1, 101),
('title_able', '小能手', '已完成50次训练', '⭐', 'title', 'training_count', 50, 0, 1, 102),
('title_expert', '高手', '已完成100次训练', '🌟', 'title', 'training_count', 100, 0, 1, 103),
('title_master', '大师', '已完成300次训练', '🏆', 'title', 'training_count', 300, 0, 1, 104),
-- 专项称号
('title_visual_master', '视觉达人', '视觉注意力得分超过80分', '👁️', 'title', 'ability_visual', 80, 0, 1, 200),
('title_memory_master', '记忆大师', '工作记忆得分超过80分', '🧠', 'title', 'ability_memory', 80, 0, 1, 201),
('title_attention_star', '专注之星', '专注时长得分超过80分', '⏰', 'title', 'ability_attention', 80, 0, 1, 202),
('title_auditory_master', '听觉高手', '听觉注意力得分超过80分', '👂', 'title', 'ability_auditory', 80, 0, 1, 203),
('title_consistent', '持之以恒', '连续打卡超过30天', '🔥', 'title', 'streak', 30, 0, 1, 204),
('title_accurate', '精准达人', '单次训练正确率超过95%', '🎯', 'title', 'accuracy', 95, 0, 1, 205);

-- ----------------------------
-- 插入音效配置（作为JSON配置，存储在系统配置表或代码中）
-- ----------------------------
-- 动物音效映射
-- training_type=3 (声音序列) 使用的动物
-- 1=🐱猫, 2=🐕狗, 3=🐦鸟, 4=🐸青蛙, 5=🦁狮子, 6=🐘大象, 7=🦈鲨鱼, 8=🐴马
