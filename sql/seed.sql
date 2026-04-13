-- =============================================
-- Focus Trainer 种子数据
-- 用于本地开发/演示环境初始化
-- 注意：运行前请确保数据库已创建（init.sql）
-- 使用方式：mysql -uroot -p focus_trainer < seed.sql
-- 或在 DBeaver 中执行
-- =============================================

USE focus_trainer;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 测试用户
-- 密码说明: 统一使用 'focuskids123'
-- BCrypt 哈希值: $2a$10$8qRXwkIEqKn8h.3Lv9w4X.lC5g0xN8wQJyPqG5dH7XzAJBcKqGmZe
-- 测试账号：
--   家长: 13800138001 / focuskids123
--   儿童: 13800138002 / focuskids123
-- ----------------------------
INSERT INTO sys_user (user_type, parent_id, nickname, phone, password, age, gender, grade, star_count, status, deleted, create_time) VALUES
-- 家长账号
(2, NULL, '小明家长', '13800138001', '$2a$10$8qRXwkIEqKn8h.3Lv9w4X.lC5g0xN8wQJyPqG5dH7XzAJBcKqGmZe', NULL, 1, NULL, 0, 1, 0, '2026-03-01 10:00:00'),
-- 儿童账号（关联家长）
(1, 1, '小明', '13800138002', '$2a$10$8qRXwkIEqKn8h.3Lv9w4X.lC5g0xN8wQJyPqG5dH7XzAJBcKqGmZe', 8, 1, 2, 0, 1, 0, '2026-03-01 10:05:00');

-- ----------------------------
-- 2. 打卡记录（连续15天）
-- last_train_date = 今天-1天（昨天）
-- ----------------------------
INSERT INTO user_streak (user_id, current_streak, max_streak, last_train_date) VALUES
(2, 15, 15, DATE_SUB(CURDATE(), INTERVAL 1 DAY));

-- ----------------------------
-- 3. 过去30天训练记录（~90条）
-- 分布策略：
--   - 专注时长(1): 约25次，duration=300-600秒
--   - 舒尔特方格(2): 约25次，level=1-3
--   - 数字闪现(21): 约15次，level=1-3
--   - 声音序列(3): 约15次，level=1-3
--   - 卡片配对(4): 约10次，level=1-3
-- 正确率趋势：早期50-70%，近期80-95%（模拟进步）
-- 星星规则：accuracy<50%→1星，50-80%→2星，>80%→3星
-- 训练配置ID：
--   1-5: type=1 (专注时长), 6-8: type=2 (舒尔特), 9-11: type=21 (数字闪现), 12-14: type=3 (声音), 15-17: type=4 (卡片)
-- ----------------------------

-- 【第1周：3月10日-3月16日，学习起步期，accuracy 50-65%，每天2次】

-- 3月10日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 1, 300, 295, 1, 0, 55.00, 295, 1, '2026-03-10 16:30:00', '2026-03-10 16:34:55', 'iPhone 14', '2026-03-10 16:30:00'),
(2, 2, 1, 300, 210, 1, 1, 60.00, 180, 2, '2026-03-10 19:00:00', '2026-03-10 19:03:30', 'iPhone 14', '2026-03-10 19:00:00');

-- 3月11日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 1, 300, 280, 1, 0, 58.00, 280, 2, '2026-03-11 16:00:00', '2026-03-11 16:04:40', 'iPad Pro', '2026-03-11 16:00:00'),
(2, 3, 1, 300, 185, 1, 0, 52.00, 156, 1, '2026-03-11 19:00:00', '2026-03-11 19:03:05', 'iPad Pro', '2026-03-11 19:00:00');

-- 3月12日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 2, 1, 300, 195, 1, 0, 62.00, 195, 2, '2026-03-12 17:00:00', '2026-03-12 17:03:15', 'iPhone 14', '2026-03-12 17:00:00'),
(2, 4, 1, 300, 230, 1, 2, 55.00, 230, 1, '2026-03-12 20:00:00', '2026-03-12 20:03:50', 'iPhone 14', '2026-03-12 20:00:00');

-- 3月13日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 1, 300, 290, 1, 0, 60.00, 290, 2, '2026-03-13 16:30:00', '2026-03-13 16:35:20', 'iPad Pro', '2026-03-13 16:30:00'),
(2, 21, 1, 300, 168, 1, 0, 58.00, 168, 1, '2026-03-13 19:00:00', '2026-03-13 19:02:48', 'iPad Pro', '2026-03-13 19:00:00');

-- 3月14日（周末）
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 2, 1, 300, 188, 1, 0, 63.00, 188, 2, '2026-03-14 10:00:00', '2026-03-14 10:03:08', 'iPad Pro', '2026-03-14 10:00:00'),
(2, 3, 1, 300, 178, 1, 1, 60.00, 178, 2, '2026-03-14 15:00:00', '2026-03-14 15:02:58', 'iPad Pro', '2026-03-14 15:00:00');

-- 3月15日（周末）
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 2, 600, 580, 1, 0, 62.00, 580, 2, '2026-03-15 09:00:00', '2026-03-15 09:09:40', 'iPad Pro', '2026-03-15 09:00:00'),
(2, 2, 2, 300, 240, 1, 0, 58.00, 240, 1, '2026-03-15 14:00:00', '2026-03-15 14:04:00', 'iPad Pro', '2026-03-15 14:00:00');

-- 3月16日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 21, 1, 300, 162, 1, 0, 64.00, 162, 2, '2026-03-16 17:00:00', '2026-03-16 17:02:42', 'iPhone 14', '2026-03-16 17:00:00'),
(2, 4, 1, 300, 220, 1, 0, 65.00, 220, 2, '2026-03-16 20:00:00', '2026-03-16 20:03:40', 'iPhone 14', '2026-03-16 20:00:00');

-- 【第2周：3月17日-3月23日，提升期，accuracy 65-75%，每天2-3次】

-- 3月17日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 1, 300, 298, 1, 0, 68.00, 298, 2, '2026-03-17 16:00:00', '2026-03-17 16:04:58', 'iPad Pro', '2026-03-17 16:00:00'),
(2, 2, 1, 300, 180, 1, 0, 72.00, 180, 2, '2026-03-17 19:00:00', '2026-03-17 19:03:00', 'iPad Pro', '2026-03-17 19:00:00'),
(2, 3, 1, 300, 170, 1, 0, 68.00, 170, 2, '2026-03-17 20:30:00', '2026-03-17 20:02:50', 'iPad Pro', '2026-03-17 20:30:00');

-- 3月18日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 2, 2, 300, 235, 1, 1, 70.00, 235, 2, '2026-03-18 16:30:00', '2026-03-18 16:34:55', 'iPhone 14', '2026-03-18 16:30:00'),
(2, 21, 2, 300, 155, 1, 0, 65.00, 155, 2, '2026-03-18 19:30:00', '2026-03-18 19:02:35', 'iPhone 14', '2026-03-18 19:30:00');

-- 3月19日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 2, 600, 590, 1, 0, 70.00, 590, 2, '2026-03-19 17:00:00', '2026-03-19 17:09:50', 'iPad Pro', '2026-03-19 17:00:00'),
(2, 4, 1, 300, 205, 1, 0, 73.00, 205, 2, '2026-03-19 20:00:00', '2026-03-19 20:03:25', 'iPad Pro', '2026-03-19 20:00:00');

-- 3月20日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 2, 1, 300, 175, 1, 0, 75.00, 175, 2, '2026-03-20 16:00:00', '2026-03-20 16:02:55', 'iPhone 14', '2026-03-20 16:00:00'),
(2, 3, 2, 300, 165, 1, 0, 68.00, 165, 2, '2026-03-20 19:00:00', '2026-03-20 19:02:45', 'iPhone 14', '2026-03-20 19:00:00');

-- 3月21日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 1, 300, 295, 1, 0, 72.00, 295, 2, '2026-03-21 10:00:00', '2026-03-21 10:04:55', 'iPad Pro', '2026-03-21 10:00:00'),
(2, 2, 2, 300, 228, 1, 0, 74.00, 228, 2, '2026-03-21 14:00:00', '2026-03-21 14:03:48', 'iPad Pro', '2026-03-21 14:00:00');

-- 3月22日（周末）
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 21, 2, 300, 148, 1, 0, 72.00, 148, 2, '2026-03-22 10:30:00', '2026-03-22 10:02:28', 'iPad Pro', '2026-03-22 10:30:00'),
(2, 3, 2, 300, 160, 1, 0, 70.00, 160, 2, '2026-03-22 15:00:00', '2026-03-22 15:02:40', 'iPad Pro', '2026-03-22 15:00:00'),
(2, 4, 2, 300, 195, 1, 0, 72.00, 195, 2, '2026-03-22 19:00:00', '2026-03-22 19:03:15', 'iPad Pro', '2026-03-22 19:00:00');

-- 3月23日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 2, 600, 595, 1, 0, 75.00, 595, 2, '2026-03-23 17:00:00', '2026-03-23 17:09:55', 'iPhone 14', '2026-03-23 17:00:00'),
(2, 2, 2, 300, 220, 1, 1, 76.00, 220, 2, '2026-03-23 20:00:00', '2026-03-23 20:03:40', 'iPhone 14', '2026-03-23 20:00:00');

-- 【第3周：3月24日-3月30日，突破期，accuracy 75-88%，每天2-3次】

-- 3月24日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 2, 2, 300, 215, 1, 0, 80.00, 215, 3, '2026-03-24 16:00:00', '2026-03-24 16:03:35', 'iPad Pro', '2026-03-24 16:00:00'),
(2, 3, 2, 300, 155, 1, 0, 78.00, 155, 2, '2026-03-24 19:00:00', '2026-03-24 19:02:35', 'iPad Pro', '2026-03-24 19:00:00');

-- 3月25日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 2, 600, 598, 1, 0, 80.00, 598, 3, '2026-03-25 17:00:00', '2026-03-25 17:09:58', 'iPhone 14', '2026-03-25 17:00:00'),
(2, 21, 3, 300, 138, 1, 0, 76.00, 138, 2, '2026-03-25 20:00:00', '2026-03-25 20:02:18', 'iPhone 14', '2026-03-25 20:00:00');

-- 3月26日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 2, 2, 300, 208, 1, 0, 82.00, 208, 3, '2026-03-26 16:30:00', '2026-03-26 16:03:58', 'iPad Pro', '2026-03-26 16:30:00'),
(2, 4, 2, 300, 188, 1, 0, 80.00, 188, 3, '2026-03-26 19:30:00', '2026-03-26 19:03:08', 'iPad Pro', '2026-03-26 19:30:00');

-- 3月27日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 3, 3, 300, 148, 1, 0, 80.00, 148, 3, '2026-03-27 17:00:00', '2026-03-27 17:02:28', 'iPhone 14', '2026-03-27 17:00:00'),
(2, 1, 2, 600, 590, 1, 0, 82.00, 590, 3, '2026-03-27 20:00:00', '2026-03-27 20:09:50', 'iPhone 14', '2026-03-27 20:00:00');

-- 3月28日（周末）
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 2, 3, 300, 195, 1, 0, 78.00, 195, 2, '2026-03-28 10:00:00', '2026-03-28 10:03:15', 'iPad Pro', '2026-03-28 10:00:00'),
(2, 21, 3, 300, 132, 1, 0, 82.00, 132, 3, '2026-03-28 14:00:00', '2026-03-28 14:02:12', 'iPad Pro', '2026-03-28 14:00:00'),
(2, 3, 3, 300, 142, 1, 0, 84.00, 142, 3, '2026-03-28 16:00:00', '2026-03-28 16:02:22', 'iPad Pro', '2026-03-28 16:00:00');

-- 3月29日（周末）
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 3, 900, 880, 1, 0, 84.00, 880, 3, '2026-03-29 10:00:00', '2026-03-29 10:14:40', 'iPad Pro', '2026-03-29 10:00:00'),
(2, 4, 2, 300, 180, 1, 0, 85.00, 180, 3, '2026-03-29 15:00:00', '2026-03-29 15:03:00', 'iPad Pro', '2026-03-29 15:00:00');

-- 3月30日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 2, 3, 300, 188, 1, 0, 85.00, 188, 3, '2026-03-30 17:00:00', '2026-03-30 17:03:08', 'iPhone 14', '2026-03-30 17:00:00'),
(2, 21, 3, 300, 128, 1, 0, 86.00, 128, 3, '2026-03-30 20:00:00', '2026-03-30 20:02:08', 'iPhone 14', '2026-03-30 20:00:00');

-- 【第4周：3月31日-4月8日，稳定期，accuracy 85-95%，每天2-3次】

-- 3月31日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 3, 3, 300, 138, 1, 0, 88.00, 138, 3, '2026-03-31 17:00:00', '2026-03-31 17:02:18', 'iPad Pro', '2026-03-31 17:00:00'),
(2, 4, 2, 300, 175, 1, 0, 88.00, 175, 3, '2026-03-31 20:00:00', '2026-03-31 20:02:55', 'iPad Pro', '2026-03-31 20:00:00');

-- 4月1日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 2, 600, 596, 1, 0, 86.00, 596, 3, '2026-04-01 17:00:00', '2026-04-01 17:09:56', 'iPhone 14', '2026-04-01 17:00:00'),
(2, 2, 3, 300, 182, 1, 0, 86.00, 182, 3, '2026-04-01 20:00:00', '2026-04-01 20:03:02', 'iPhone 14', '2026-04-01 20:00:00');

-- 4月2日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 21, 3, 300, 125, 1, 0, 90.00, 125, 3, '2026-04-02 16:00:00', '2026-04-02 16:02:05', 'iPad Pro', '2026-04-02 16:00:00'),
(2, 3, 3, 300, 135, 1, 0, 88.00, 135, 3, '2026-04-02 19:00:00', '2026-04-02 19:02:15', 'iPad Pro', '2026-04-02 19:00:00');

-- 4月3日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 2, 3, 300, 178, 1, 0, 90.00, 178, 3, '2026-04-03 17:00:00', '2026-04-03 17:02:58', 'iPhone 14', '2026-04-03 17:00:00'),
(2, 4, 3, 300, 165, 1, 0, 88.00, 165, 3, '2026-04-03 20:00:00', '2026-04-03 20:02:45', 'iPhone 14', '2026-04-03 20:00:00');

-- 4月4日（清明假期）
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 3, 900, 895, 1, 0, 90.00, 895, 3, '2026-04-04 10:00:00', '2026-04-04 10:14:55', 'iPad Pro', '2026-04-04 10:00:00'),
(2, 3, 3, 300, 130, 1, 0, 92.00, 130, 3, '2026-04-04 14:00:00', '2026-04-04 14:02:10', 'iPad Pro', '2026-04-04 14:00:00');

-- 4月5日（清明假期）
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 2, 3, 300, 172, 1, 0, 92.00, 172, 3, '2026-04-05 10:30:00', '2026-04-05 10:02:52', 'iPad Pro', '2026-04-05 10:30:00'),
(2, 21, 3, 300, 120, 1, 0, 94.00, 120, 3, '2026-04-05 15:00:00', '2026-04-05 15:02:00', 'iPad Pro', '2026-04-05 15:00:00'),
(2, 4, 3, 300, 158, 1, 0, 90.00, 158, 3, '2026-04-05 18:00:00', '2026-04-05 18:02:38', 'iPad Pro', '2026-04-05 18:00:00');

-- 4月6日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 2, 600, 595, 1, 0, 88.00, 595, 3, '2026-04-06 17:00:00', '2026-04-06 17:09:55', 'iPhone 14', '2026-04-06 17:00:00'),
(2, 2, 3, 300, 175, 1, 0, 91.00, 175, 3, '2026-04-06 20:00:00', '2026-04-06 20:02:55', 'iPhone 14', '2026-04-06 20:00:00');

-- 4月7日
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 3, 3, 300, 128, 1, 0, 93.00, 128, 3, '2026-04-07 17:00:00', '2026-04-07 17:02:08', 'iPad Pro', '2026-04-07 17:00:00'),
(2, 4, 3, 300, 160, 1, 0, 92.00, 160, 3, '2026-04-07 20:00:00', '2026-04-07 20:02:40', 'iPad Pro', '2026-04-07 20:00:00');

-- 4月8日（昨天）
INSERT INTO training_record (user_id, training_type, level, duration, actual_duration, status, interrupt_count, accuracy, score, star_reward, start_time, end_time, device_info, create_time) VALUES
(2, 1, 3, 900, 890, 1, 0, 93.00, 890, 3, '2026-04-08 17:00:00', '2026-04-08 17:14:50', 'iPhone 14', '2026-04-08 17:00:00'),
(2, 2, 3, 300, 168, 1, 0, 95.00, 168, 3, '2026-04-08 20:00:00', '2026-04-08 20:02:48', 'iPhone 14', '2026-04-08 20:00:00');

-- ----------------------------
-- 4. 能力评估记录（每月1次，共4次，展示进步曲线）
-- 第1次（3月10日）：初始评估，分数偏低，E级
-- 第2次（3月17日）：略有提升，E级
-- 第3次（3月24日）：明显进步，D级
-- 第4次（4月8日）：良好，D级
-- 能力维度：专注时长、视觉注意力、听觉注意力、工作记忆、抑制控制
-- ----------------------------
INSERT INTO user_ability (user_id, attention_duration, visual_attention, auditory_attention, working_memory, inhibitory_control, total_score, ability_level, evaluate_date, create_time) VALUES
(2, 48.50, 52.00, 45.00, 50.00, 47.00, 48.50, 'E', '2026-03-10', '2026-03-10 21:00:00'),
(2, 58.00, 62.00, 55.00, 60.00, 56.00, 58.20, 'E', '2026-03-17', '2026-03-17 21:00:00'),
(2, 68.00, 72.00, 65.00, 70.00, 67.00, 68.40, 'D', '2026-03-24', '2026-03-24 21:00:00'),
(2, 78.00, 82.00, 75.00, 80.00, 77.00, 78.40, 'D', '2026-04-08', '2026-04-08 21:00:00');

-- ----------------------------
-- 5. 奖励记录
-- 5A: 星星收入记录（按每条训练记录生成奖励）
-- 5B: 打卡奖励（每签到1次奖励）
-- ----------------------------

-- 5A: 训练完成星星奖励（按 training_record 的 star_reward 汇总，source_type=1）
INSERT INTO reward_record (user_id, reward_type, reward_value, reward_name, source_type, source_id, create_time)
SELECT user_id, 1, star_reward,
       CASE
         WHEN training_type = 1 THEN '专注时长训练奖励'
         WHEN training_type = 2 THEN '舒尔特方格训练奖励'
         WHEN training_type = 21 THEN '数字闪现训练奖励'
         WHEN training_type = 3 THEN '声音序列训练奖励'
         WHEN training_type = 4 THEN '卡片配对训练奖励'
         ELSE '训练奖励'
       END,
       1, record_id, end_time
FROM training_record
WHERE status = 1 AND user_id = 2;

-- 5B: 连续打卡星星奖励（每天打卡奖励3星，source_type=2）
-- 生成过去15天每天的打卡奖励（对应 user_streak 的连续天数）
INSERT INTO reward_record (user_id, reward_type, reward_value, reward_name, source_type, source_id, create_time) VALUES
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 15 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 14 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 13 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 12 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 11 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 10 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 9 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 8 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 7 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '7天连续打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 7 DAY) + INTERVAL 18 HOUR),  -- 第7天额外奖励
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 6 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 5 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 4 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 3 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 2 DAY) + INTERVAL 18 HOUR),
(2, 1, 3, '14天连续打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 2 DAY) + INTERVAL 18 HOUR),  -- 第14天额外奖励
(2, 1, 3, '每日打卡奖励', 2, NULL, DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 18 HOUR);

-- 5C: 更新用户星星总数（从 reward_record 汇总）
UPDATE sys_user SET star_count = (
    SELECT COALESCE(SUM(reward_value), 0) FROM reward_record WHERE user_id = 2 AND reward_type = 1
) WHERE user_id = 2;

-- ----------------------------
-- 6. 通知消息
-- ----------------------------
INSERT INTO notification (user_id, type, title, content, is_read, create_time) VALUES
(2, 1, '今日训练提醒', '小明，今天还没完成训练哦！快来挑战一下舒尔特方格吧～坚持训练可以更快提升专注力！', 0, DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 9 HOUR),
(2, 2, '🎉 恭喜获得新成就！', '小明连续训练14天啦！你的专注力正在稳步提升，继续加油！', 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY) + INTERVAL 20 HOUR),
(2, 2, '⭐ 训练小达人', '小明今天表现很棒！正确率达到92%，获得3颗星星奖励，继续保持！', 1, '2026-04-07 20:30:00'),
(2, 3, '专注力训练建议', '根据小明的训练数据分析，建议近期可以尝试挑战舒尔特方格5×5高级难度，对提升视觉追踪能力很有帮助哦！', 1, '2026-04-05 10:00:00'),
(2, 1, '周末训练提醒', '周末来啦！小明可以利用周末时间多做几次训练，每天坚持效果更好哦～', 1, DATE_SUB(CURDATE(), INTERVAL 4 DAY) + INTERVAL 9 HOUR);

-- ----------------------------
-- 7. 徽章数据（预置用户已解锁的徽章）
-- 儿童用户(user_id=2)数据：
--   累计训练: ~78次 → first_train(1), train_10(2)
--   连续打卡: 15天   → streak_3(5), streak_7(6), streak_14(7)
--   累计星星: ~370   → stars_50(11), stars_100(12)
-- 注意：运行前需确保 badge 表已创建（先执行 badge.sql）
-- ----------------------------
INSERT INTO user_badge (user_id, badge_id, earned_at, source_type, source_id) VALUES
-- 训练类徽章（source_type=1，source_id指向任意训练记录）
(2, 1, '2026-03-10 16:35:00', 1, 1),   -- 初出茅庐：第1次训练
(2, 2, '2026-03-20 16:03:00', 1, 30),  -- 训练达人：第10次训练
-- 打卡类徽章（source_type=2，无source_id）
(2, 5, '2026-03-13 18:00:00', 2, NULL),  -- 坚持之星：第3天
(2, 6, '2026-03-17 18:00:00', 2, NULL),  -- 连续7天
(2, 7, '2026-03-24 18:00:00', 2, NULL),  -- 连续14天
-- 星星类徽章（source_type=4=系统）
(2, 11, '2026-03-25 20:00:00', 4, NULL),  -- 小试牛刀：50星
(2, 12, '2026-04-05 20:00:00', 4, NULL);  -- 百星少年：100星

-- ----------------------------
-- 8. 今日每日任务（4个，默认状态）
-- 基于昨天（最后训练日期）的任务快照
-- ----------------------------
INSERT INTO daily_task (user_id, task_date, task_type, title, description, target_value, progress_value, star_reward, status, create_time) VALUES
(2, CURDATE(), 1, '完成训练任务', '今日完成任意训练游戏', 1, 0, 5, 0, NOW()),
(2, CURDATE(), 2, '正确率达标', '今日训练正确率达到80%以上', 80, 0, 5, 0, NOW()),
(2, CURDATE(), 3, '坚持打卡', '保持连续打卡习惯', 1, 0, 3, 0, NOW()),
(2, CURDATE(), 4, '多维训练', '今日完成2种不同类型的训练', 2, 0, 7, 0, NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 种子数据执行完毕
-- 统计：
--   用户: 2条（家长+儿童）
--   训练记录: ~78条（30天跨度，正确率逐步提升）
--   能力评估: 4条（每周1次，进步曲线清晰）
--   星星奖励: ~78条训练奖励 + ~17条打卡奖励
--   累计星星: ~370颗（儿童）
--   通知: 5条
--   每日任务: 4条（今日）
--   打卡记录: 1条（连续15天）
-- =============================================
