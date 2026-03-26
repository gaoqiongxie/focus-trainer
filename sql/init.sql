-- =============================================
-- 儿童注意力训练APP 数据库初始化脚本
-- 创建时间: 2026-03-26
-- =============================================

CREATE DATABASE IF NOT EXISTS focus_trainer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE focus_trainer;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    user_id     BIGINT      NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    user_type   TINYINT     NOT NULL                COMMENT '用户类型(1:儿童 2:家长)',
    parent_id   BIGINT                              COMMENT '家长ID(儿童用户关联)',
    nickname    VARCHAR(50)                         COMMENT '昵称',
    avatar      VARCHAR(255)                        COMMENT '头像URL',
    phone       VARCHAR(11)                         COMMENT '手机号',
    password    VARCHAR(128)                        COMMENT '密码(BCrypt加密)',
    age         TINYINT                             COMMENT '年龄',
    gender      TINYINT     DEFAULT 0               COMMENT '性别(0:未知 1:男 2:女)',
    grade       TINYINT                             COMMENT '年级(1-6)',
    star_count  INT         DEFAULT 0               COMMENT '星星总数',
    status      TINYINT     NOT NULL DEFAULT 1      COMMENT '状态(0:禁用 1:正常)',
    deleted     TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除(0:正常 1:删除)',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_phone (phone),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 2. 训练配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS training_config (
    config_id       INT         NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    training_type   TINYINT     NOT NULL               COMMENT '训练类型(1:专注时长 2:视觉追踪 3:听觉专注 4:记忆训练)',
    training_name   VARCHAR(50) NOT NULL               COMMENT '训练名称',
    level           TINYINT     NOT NULL               COMMENT '难度等级(1-10)',
    duration        INT                                COMMENT '训练时长(秒)',
    config_json     TEXT                               COMMENT '详细配置(JSON)',
    icon_url        VARCHAR(255)                       COMMENT '训练图标',
    description     VARCHAR(255)                       COMMENT '训练描述',
    is_active       TINYINT     NOT NULL DEFAULT 1     COMMENT '是否启用(0:否 1:是)',
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (config_id),
    UNIQUE KEY uk_type_level (training_type, level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练配置表';

-- ----------------------------
-- 3. 训练记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS training_record (
    record_id        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    user_id          BIGINT      NOT NULL               COMMENT '用户ID',
    training_type    TINYINT     NOT NULL               COMMENT '训练类型',
    level            TINYINT     NOT NULL               COMMENT '难度等级',
    duration         INT         NOT NULL               COMMENT '计划时长(秒)',
    actual_duration  INT         NOT NULL DEFAULT 0     COMMENT '实际时长(秒)',
    status           TINYINT     NOT NULL DEFAULT 0     COMMENT '状态(0:进行中 1:完成 2:中断)',
    interrupt_count  INT                  DEFAULT 0     COMMENT '中断次数',
    accuracy         DECIMAL(5,2)                       COMMENT '正确率(%)',
    score            INT                  DEFAULT 0     COMMENT '得分',
    star_reward      INT                  DEFAULT 0     COMMENT '获得星星数',
    start_time       DATETIME    NOT NULL               COMMENT '开始时间',
    end_time         DATETIME                           COMMENT '结束时间',
    device_info      VARCHAR(255)                       COMMENT '设备信息',
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (record_id),
    KEY idx_user_time (user_id, start_time),
    KEY idx_type_date (training_type, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练记录表';

-- ----------------------------
-- 4. 用户能力评估表
-- ----------------------------
CREATE TABLE IF NOT EXISTS user_ability (
    ability_id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评估ID',
    user_id             BIGINT       NOT NULL               COMMENT '用户ID',
    attention_duration  DECIMAL(5,2) DEFAULT 0             COMMENT '专注时长得分(0-100)',
    visual_attention    DECIMAL(5,2) DEFAULT 0             COMMENT '视觉注意力得分(0-100)',
    auditory_attention  DECIMAL(5,2) DEFAULT 0             COMMENT '听觉注意力得分(0-100)',
    working_memory      DECIMAL(5,2) DEFAULT 0             COMMENT '工作记忆得分(0-100)',
    inhibitory_control  DECIMAL(5,2) DEFAULT 0             COMMENT '抑制控制得分(0-100)',
    total_score         DECIMAL(5,2) DEFAULT 0             COMMENT '综合得分(0-100)',
    ability_level       VARCHAR(10)                        COMMENT '能力等级(A/B/C/D/E)',
    evaluate_date       DATE         NOT NULL               COMMENT '评估日期',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (ability_id),
    UNIQUE KEY uk_user_date (user_id, evaluate_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户能力评估表';

-- ----------------------------
-- 5. 激励记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS reward_record (
    reward_id    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '奖励ID',
    user_id      BIGINT      NOT NULL               COMMENT '用户ID',
    reward_type  TINYINT     NOT NULL               COMMENT '奖励类型(1:星星 2:徽章 3:成就)',
    reward_value INT                                COMMENT '奖励值(星星数/徽章ID)',
    reward_name  VARCHAR(50)                        COMMENT '奖励名称',
    source_type  TINYINT                            COMMENT '来源(1:训练完成 2:签到 3:活动)',
    source_id    BIGINT                             COMMENT '来源ID',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (reward_id),
    KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='激励记录表';

-- ----------------------------
-- 6. 用户连续打卡表
-- ----------------------------
CREATE TABLE IF NOT EXISTS user_streak (
    streak_id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    user_id           BIGINT   NOT NULL UNIQUE         COMMENT '用户ID',
    current_streak    INT      DEFAULT 0               COMMENT '当前连续天数',
    max_streak        INT      DEFAULT 0               COMMENT '历史最大连续天数',
    last_train_date   DATE                             COMMENT '最后训练日期',
    update_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (streak_id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户连续打卡表';

-- ----------------------------
-- 7. 通知消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS notification (
    notification_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    user_id         BIGINT       NOT NULL               COMMENT '用户ID',
    type            TINYINT      NOT NULL               COMMENT '类型(1:训练提醒 2:奖励通知 3:专家建议)',
    title           VARCHAR(100) NOT NULL               COMMENT '标题',
    content         TEXT         NOT NULL               COMMENT '内容',
    is_read         TINYINT      DEFAULT 0              COMMENT '是否已读(0:未读 1:已读)',
    extra_data      TEXT                                COMMENT '扩展数据(JSON)',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (notification_id),
    KEY idx_user_read (user_id, is_read, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息表';

-- ----------------------------
-- 初始化训练配置数据
-- ----------------------------
INSERT INTO training_config (training_type, training_name, level, duration, description, is_active) VALUES
(1, '专注时长-入门', 1, 300,  '5分钟专注训练，适合初学者', 1),
(1, '专注时长-进阶', 2, 600,  '10分钟专注训练', 1),
(1, '专注时长-标准', 3, 900,  '15分钟专注训练', 1),
(1, '专注时长-强化', 4, 1200, '20分钟专注训练', 1),
(1, '专注时长-挑战', 5, 1800, '30分钟专注训练，挑战模式', 1),
(2, '视觉追踪-初级', 1, 300,  '3x3舒尔特方格，初级难度', 1),
(2, '视觉追踪-中级', 2, 300,  '4x4舒尔特方格，中级难度', 1),
(2, '视觉追踪-高级', 3, 300,  '5x5舒尔特方格，高级难度', 1),
(3, '听觉专注-初级', 1, 300,  '简单声音识别训练', 1),
(3, '听觉专注-中级', 2, 300,  '干扰环境声音识别', 1),
(4, '记忆训练-初级', 1, 300,  '数字记忆，序列长度3-5', 1),
(4, '记忆训练-中级', 2, 300,  '图形记忆，序列长度5-7', 1),
(4, '记忆训练-高级', 3, 300,  '混合记忆，序列长度7-10', 1);
