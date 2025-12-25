/* =========================================================
   SSRMS 初始化数据库脚本（合并版：ssrms.sql + feedback.sql）
   - 3校区 × 4楼宇 × 25房间 = 300 room
   - 每房间 80 seat => 24000 seat
   - 可反复执行：会 drop 旧表再重建
   ========================================================= */

CREATE DATABASE IF NOT EXISTS `ssrms`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `ssrms`;
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `quote`;
DROP TABLE IF EXISTS `announcement`;
DROP TABLE IF EXISTS `notice`;
DROP TABLE IF EXISTS `feedback`;
DROP TABLE IF EXISTS `violation`;
DROP TABLE IF EXISTS `reservation`;
DROP TABLE IF EXISTS `seat`;
DROP TABLE IF EXISTS `room`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;


/* =========================
   1) user
   ========================= */
CREATE TABLE `user` (
                        `id`             INT          NOT NULL AUTO_INCREMENT,
                        `account`        VARCHAR(20)  NOT NULL COMMENT '账号（登录用）',
                        `name`           VARCHAR(100) NOT NULL COMMENT '姓名',
                        `student_no`     VARCHAR(50)  NULL COMMENT '学号',
                        `password`       VARCHAR(100) NOT NULL COMMENT '密码（示例中为明文）',
                        `age`            INT          NULL,
                        `sex`            TINYINT(1)   NULL COMMENT '0 女 1 男',
                        `phone`          VARCHAR(20)  NULL,
                        `email`          VARCHAR(100) NULL COMMENT '邮箱',
                        `role_id`        INT          NOT NULL DEFAULT 1 COMMENT '0 管理员 1 学生',
                        `isValid`        VARCHAR(4)   NOT NULL DEFAULT 'Y' COMMENT 'Y 有效 N 无效',

                        `college`        VARCHAR(100) NULL COMMENT '学院',
                        `grade_class`    VARCHAR(100) NULL COMMENT '年级班级',
                        `common_campus`  VARCHAR(50)  NULL COMMENT '常用校区',
                        `profile_remark` VARCHAR(255) NULL COMMENT '个人中心备注信息',

                        `credit_score`   INT          NOT NULL DEFAULT 100 COMMENT '信用分，0-100',
                        `blacklist_flag` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否在黑名单 0-否 1-是',


                        `last_login_ip`   VARCHAR(64)  NULL COMMENT '最近登录IP',
                        `last_login_time` DATETIME     NULL COMMENT '最近登录时间',

                        `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_user_account` (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';


/* =========================
   2) room
   ========================= */
CREATE TABLE `room` (
                        `id`           INT          NOT NULL AUTO_INCREMENT,
                        `campus`       VARCHAR(50)  NOT NULL COMMENT '校区',
                        `building`     VARCHAR(50)  NOT NULL COMMENT '教学楼/图书馆',
                        `room_name`    VARCHAR(50)  NOT NULL COMMENT '房间名，如 301/401',
                        `total_seats`  INT          NOT NULL COMMENT '总座位数',
                        `open_seats`   INT          NOT NULL COMMENT '开放座位数',
                        `status`       VARCHAR(20)  NOT NULL DEFAULT 'open' COMMENT 'open/closed/maintain',
                        `remark`       VARCHAR(255) NULL,
                        `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室信息表';


/* =========================
   3) seat
   ========================= */
CREATE TABLE `seat` (
                        `id`      INT          NOT NULL AUTO_INCREMENT,
                        `room_id` INT          NOT NULL,
                        `seat_no` VARCHAR(20)  NOT NULL COMMENT '座位编号，如 01~80',
                        `status`  VARCHAR(20)  NOT NULL DEFAULT 'enabled' COMMENT 'enabled/disabled',
                        `remark`  VARCHAR(255) NULL,

                        PRIMARY KEY (`id`),
                        KEY `idx_seat_room_id` (`room_id`),
                        UNIQUE KEY `uk_seat_room_no` (`room_id`, `seat_no`),

                        CONSTRAINT `fk_seat_room`
                            FOREIGN KEY (`room_id`) REFERENCES `room`(`id`)
                                ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室座位表';


/* =========================
   4) reservation
   ========================= */
CREATE TABLE `reservation` (
                               `id`             BIGINT       NOT NULL AUTO_INCREMENT,
                               `reservation_no` VARCHAR(64)  NOT NULL COMMENT '预约编号',
                               `user_id`        INT          NOT NULL,
                               `room_id`        INT          NOT NULL,
                               `seat_id`        INT          NULL COMMENT '精确到座位则不为空',
                               `date`           DATE         NOT NULL COMMENT '预约日期',
                               `start_time`     TIME         NOT NULL COMMENT '开始时间',
                               `end_time`       TIME         NOT NULL COMMENT '结束时间',

                               `status`         VARCHAR(20)  NOT NULL DEFAULT 'reserved'
                                   COMMENT 'reserved/checked_in/late/no_show/cancelled/cancel_overdue',

                               `checkin_time`   DATETIME     NULL COMMENT '签到时间',
                               `checkout_time`  DATETIME     NULL COMMENT '签退时间',

                               `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_reservation_no` (`reservation_no`),
                               KEY `idx_resv_user_id` (`user_id`),
                               KEY `idx_resv_room_id` (`room_id`),
                               KEY `idx_resv_seat_id` (`seat_id`),
                               KEY `idx_resv_date` (`date`),

                               CONSTRAINT `fk_resv_user`
                                   FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
                                       ON DELETE CASCADE ON UPDATE CASCADE,

                               CONSTRAINT `fk_resv_room`
                                   FOREIGN KEY (`room_id`) REFERENCES `room`(`id`)
                                       ON DELETE CASCADE ON UPDATE CASCADE,

                               CONSTRAINT `fk_resv_seat`
                                   FOREIGN KEY (`seat_id`) REFERENCES `seat`(`id`)
                                       ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位预约记录表';


/* =========================
   5) violation
   ========================= */
CREATE TABLE `violation` (
                             `id`             BIGINT       NOT NULL AUTO_INCREMENT,
                             `user_id`        INT          NOT NULL,
                             `reservation_id` BIGINT       NULL COMMENT '关联预约记录，可选',
                             `v_type`         VARCHAR(30)  NOT NULL COMMENT 'no_show/late/occupy_overtime/other',
                             `v_date`         DATE         NOT NULL COMMENT '违规日期',
                             `room_id`        INT          NULL,
                             `description`    VARCHAR(255) NULL COMMENT '违规说明',
                             `penalty_score`  INT          NOT NULL DEFAULT 0 COMMENT '扣除信用分',
                             `handled`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已处理',
                             `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             PRIMARY KEY (`id`),
                             KEY `idx_violation_user_id` (`user_id`),

                             CONSTRAINT `fk_violation_user`
                                 FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
                                     ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规记录表';


/* =========================
   6) feedback（评价与投诉 / 建议）
   - category：env/service/suggestion/complaint/appeal/other
   - status：pending/processing/resolved
   ========================= */
CREATE TABLE `feedback` (
                            `id`              BIGINT       NOT NULL AUTO_INCREMENT,
                            `user_id`         INT          NOT NULL COMMENT '用户ID',
                            `room_id`         INT          NULL COMMENT '自习室ID（可选）',
                            `reservation_id`  BIGINT       NULL COMMENT '预约ID（可选）',
                            `category`        VARCHAR(30)  NOT NULL COMMENT 'env/service/suggestion/complaint/appeal/other',
                            `rating`          INT          NULL COMMENT '评分 1~5（可选）',
                            `content`         TEXT         NOT NULL COMMENT '反馈内容',
                            `status`          VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/resolved',
                            `reply`           TEXT         NULL COMMENT '管理员回复',
                            `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                            PRIMARY KEY (`id`),
                            KEY `idx_feedback_user` (`user_id`),
                            KEY `idx_feedback_status` (`status`),

                            CONSTRAINT `fk_feedback_user`
                                FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
                                    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评价与投诉表';



/* =========================
   7) notice（已弃用）
   - 旧版本使用 notice 表存公告
   - 当前版本统一使用 announcement 表，notice 已移除
   ========================= */

/* =========================
   8) quote
   ========================= */
CREATE TABLE `quote` (
                         `id`          INT NOT NULL AUTO_INCREMENT,
                         `content`     VARCHAR(255) NOT NULL COMMENT '提示语/金句',
                         `author`      VARCHAR(100) NULL,
                         `enabled`     TINYINT(1) NOT NULL DEFAULT 1,
                         `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页提示/金句';


/* =========================================================
   初始化数据
   ========================================================= */

/* 1) 用户：1个管理员 + 多个学生 */
INSERT INTO `user`
(`account`,`name`,`student_no`,`password`,`age`,`sex`,`phone`,`email`,
 `role_id`,`isValid`,`college`,`grade_class`,`common_campus`,`profile_remark`,
 `credit_score`,`blacklist_flag`)
VALUES
    ('eden','超级管理员',NULL,'123456',20,1,'15869052922',NULL,0,'Y','信息工程学院','计科一班','本部校区','管理员账号',100,0),

    ('xiaoming','小明','236002922','123456',18,1,'13456789222','xiaoming@demo.com',1,'Y','法学院','计科一班','本部校区','',100,0),
    ('caixy','蔡徐','236002937','123456',18,1,'13456789001','caixy@demo.com',1,'Y','信息工程学院','计科一班','本部校区','',100,0),
    ('kunkun','坤坤','236002938','123456',18,1,'13456789002','kunkun@demo.com',1,'Y','信息工程学院','计科一班','本部校区','',100,0),
    ('yuechenxi','月之晨曦','236002939','123456',19,0,'13456789003','yuechenxi@demo.com',1,'Y','信息工程学院','计科一班','本部校区','喜欢安静靠窗位',100,0),
    ('sleepy','sleepy','236002999','123456',18,1,'13456789004','sleepy@demo.com',1,'Y','信息工程学院','计科二班','东校区','午后容易犯困',100,0),
    ('liuyi','liuyi','236000000','123456',18,1,'13456789005','liuyi@demo.com',1,'Y','信息工程学院','计科二班','梅山校区','',100,0),

    ('alice','Alice','236003001','123456',18,0,'13456789006','alice@demo.com',1,'Y','外国语学院','英专二班','东校区','',100,0),
    ('bob','Bob','236003002','123456',19,1,'13456789007','bob@demo.com',1,'Y','管理学院','信管一班','本部校区','',100,0),
    ('charlie','Charlie','236003003','123456',20,1,'13456789008','charlie@demo.com',1,'Y','信息工程学院','软工一班','梅山校区','',100,0);


/* 2) quote（你后续想多加再追加 insert 就行） */
INSERT INTO `quote` (`content`,`author`,`enabled`,`create_time`) VALUES
                                                                     ('代码写完要多测试，bug 总会在你最不想看到它的时候出现。', NULL, 1, NOW()),
                                                                     ('专注一小时，胜过边刷手机边学习三小时。', NULL, 1, NOW()),
                                                                     ('复习最大的骗局，是“我好像都看过”；真正有用的是“这题我能当场写出来”。', NULL, 1, NOW()),
                                                                     ('不想学的时候，先坐下来学五分钟，很多坚持都是从这五分钟开始的。', NULL, 1, NOW()),
                                                                     ('今天偷的懒，都会在考试周加倍还回来。', NULL, 1, NOW()),
                                                                     ('把手机调成静音，让知识当主讲。', NULL, 1, NOW()),
                                                                     ('翻书请温柔一点，纸张也怕社死。', NULL, 1, NOW()),
                                                                     ('走路轻一点，这里连灵感都在午睡。', NULL, 1, NOW()),
                                                                     ('自习室不是外卖取餐区，香味会“作弊”。', NULL, 1, NOW()),
                                                                     ('你按下的不是键盘，是未来的加速键。', NULL, 1, NOW()),
                                                                     ('占座一分钟，心虚两小时；带走物品，安心学习。', NULL, 1, NOW()),
                                                                     ('耳机音量小一点，别让隔壁也被迫跟读。', NULL, 1, NOW()),
                                                                     ('垃圾请带走，别给保洁阿姨加难度。', NULL, 1, NOW()),
                                                                     ('喝水可以，开瓶请低调；咔一声，全场都醒。', NULL, 1, NOW()),
                                                                     ('书页不是草稿纸，别在上面留下“青春签名”。', NULL, 1, NOW()),
                                                                     ('自习最怕的不是难题，是被打断的节奏。', NULL, 1, NOW()),
                                                                     ('困了就起身走走，别把座位睡成床位。', NULL, 1, NOW()),
                                                                     ('把今天拆成一小步，明天就会多一大截。', NULL, 1, NOW()),
                                                                     ('电脑可以发热，脑子也要同步升温。', NULL, 1, NOW()),
                                                                     ('你来图书馆不是为了熬夜，是为了早点自由。', NULL, 1, NOW()),
                                                                     ('别急着刷题，先把错题“驯服”。', NULL, 1, NOW()),
                                                                     ('先读十页再玩手机：你会感谢这个决定。', NULL, 1, NOW()),
                                                                     ('安静不是规则，是对别人专注的尊重。', NULL, 1, NOW()),
                                                                     ('背公式像练肌肉：慢慢来，但别停。', NULL, 1, NOW()),
                                                                     ('复习不是重复，是把知识重新排序。', NULL, 1, NOW()),
                                                                     ('带走随身物品，也带走焦虑；回来继续就好。', NULL, 1, NOW()),
                                                                     ('别把书摊到别人的空间里，知识也要讲礼貌。', NULL, 1, NOW()),
                                                                     ('讨论请去讨论区，灵感需要房间，也需要边界。', NULL, 1, NOW()),
                                                                     ('你现在的自律，未来会以惊喜的方式回礼。', NULL, 1, NOW()),
                                                                     ('今天做完这一题，明天就少慌一点点。', NULL, 1, NOW()),
                                                                     ('借书像借时间：记得按时归还。', NULL, 1, NOW()),
                                                                     ('书本最喜欢的归宿：书架；不是你的包底。', NULL, 1, NOW()),
                                                                     ('别怕慢，怕的是停在“我等会儿再学”。', NULL, 1, NOW()),
                                                                     ('把目标写下来，它就从愿望变成计划。', NULL, 1, NOW()),
                                                                     ('翻页的声音小一点，别把安静翻成暴风雨。', NULL, 1, NOW()),
                                                                     ('别把插座当战利品，用完记得让位。', NULL, 1, NOW()),
                                                                     ('学习像存档：多保存几次，别等崩溃重来。', NULL, 1, NOW()),
                                                                     ('喝咖啡可以，别把杯子放在书上“盖章”。', NULL, 1, NOW()),
                                                                     ('真正的效率：专注 25 分钟，也不刷一次消息。', NULL, 1, NOW()),
                                                                     ('遇到不会的，先标记再回头；别原地打转。', NULL, 1, NOW()),
                                                                     ('自习不是比谁坐得久，是比谁走得稳。', NULL, 1, NOW()),
                                                                     ('别让“再玩五分钟”变成“五天”。', NULL, 1, NOW()),
                                                                     ('今天的你配得上一个认真结尾。', NULL, 1, NOW()),
                                                                     ('把书合上之前，给自己一句总结。', NULL, 1, NOW()),
                                                                     ('离馆请轻声，别把胜利结算成大喇叭。', NULL, 1, NOW()),
                                                                     ('把手机倒扣一小时，你会突然变强。', NULL, 1, NOW()),
                                                                     ('安静一点，隔壁的灵感正在加载。', NULL, 1, NOW()),
                                                                     ('别急着求快，先把基础打牢。', NULL, 1, NOW()),
                                                                     ('自习不是硬扛，是稳定输出。', NULL, 1, NOW()),
                                                                     ('把任务写成清单，焦虑就少一半。', NULL, 1, NOW()),
                                                                     ('先做最难的那一题，后面会顺很多。', NULL, 1, NOW()),
                                                                     ('不想学的时候，先学五分钟再决定。', NULL, 1, NOW()),
                                                                     ('别把座位留给空气，把机会留给自己。', NULL, 1, NOW()),
                                                                     ('小声一点，大家都在和题目摔跤。', NULL, 1, NOW()),
                                                                     ('翻书别用力，知识不需要“撕开”。', NULL, 1, NOW()),
                                                                     ('离开座位记得带走贵重物品。', NULL, 1, NOW()),
                                                                     ('喝水可以，咕咚请低调。', NULL, 1, NOW()),
                                                                     ('键盘声太响时，试试轻敲模式。', NULL, 1, NOW()),
                                                                     ('别让“等会儿”把今天偷走。', NULL, 1, NOW()),
                                                                     ('复习要回头看，才知道哪里没懂。', NULL, 1, NOW()),
                                                                     ('错题不是黑历史，是升级路线图。', NULL, 1, NOW()),
                                                                     ('把重点标出来，别让眼睛白跑。', NULL, 1, NOW()),
                                                                     ('学习最怕断档，哪怕每天一点点。', NULL, 1, NOW()),
                                                                     ('别在自习区开会，讨论请去讨论区。', NULL, 1, NOW()),
                                                                     ('把耳机音量调低，别共享歌单。', NULL, 1, NOW()),
                                                                     ('困了就站起来走两圈，比硬撑更有效。', NULL, 1, NOW()),
                                                                     ('别把咖啡当魔法，专注才是。', NULL, 1, NOW()),
                                                                     ('把今天的收获写一句，明天更清晰。', NULL, 1, NOW()),
                                                                     ('把目标切小，行动就不难。', NULL, 1, NOW()),
                                                                     ('你不需要完美开始，只需要开始。', NULL, 1, NOW()),
                                                                     ('先做能做的，再攻克不会的。', NULL, 1, NOW()),
                                                                     ('书包占座不算预约，别让规则尴尬。', NULL, 1, NOW()),
                                                                     ('自习区的香味会跑得很远。', NULL, 1, NOW()),
                                                                     ('垃圾请带走，保持桌面清爽。', NULL, 1, NOW()),
                                                                     ('别让通知弹窗决定你的节奏。', NULL, 1, NOW()),
                                                                     ('把消息免打扰，效率会很礼貌。', NULL, 1, NOW()),
                                                                     ('今天的努力，会在成绩单上露面。', NULL, 1, NOW()),
                                                                     ('慢慢来，但每天都来。', NULL, 1, NOW()),
                                                                     ('做题先理解题意，别急着写。', NULL, 1, NOW()),
                                                                     ('遇到卡点，换一种思路再试。', NULL, 1, NOW()),
                                                                     ('复盘比刷题更能提分。', NULL, 1, NOW()),
                                                                     ('别用“我差不多懂了”骗自己。', NULL, 1, NOW()),
                                                                     ('把知识讲给自己听，才算真的会。', NULL, 1, NOW()),
                                                                     ('不怕题多，就怕不总结。', NULL, 1, NOW()),
                                                                     ('学习也讲节奏：专注—休息—再专注。', NULL, 1, NOW()),
                                                                     ('把难题拆成小步，答案就不远。', NULL, 1, NOW()),
                                                                     ('别和别人比进度，和自己比坚持。', NULL, 1, NOW()),
                                                                     ('你坐下的这一刻，已经赢过拖延。', NULL, 1, NOW()),
                                                                     ('不要追求长时间，追求高质量。', NULL, 1, NOW()),
                                                                     ('把页面合上前，记得做一次总结。', NULL, 1, NOW()),
                                                                     ('书架是书的家，读完记得归位。', NULL, 1, NOW()),
                                                                     ('别在桌面堆满“以后再看”。', NULL, 1, NOW()),
                                                                     ('专注不是天赋，是选择。', NULL, 1, NOW()),
                                                                     ('今天学到的，会在某天救你一命。', NULL, 1, NOW()),
                                                                     ('不要把难题留到最后，最后更难。', NULL, 1, NOW()),
                                                                     ('宁可少做一点，也要做对一遍。', NULL, 1, NOW()),
                                                                     ('把错误记下来，比把答案背下来更重要。', NULL, 1, NOW()),
                                                                     ('今天多专注十分钟，明天少慌一整天。', NULL, 1, NOW()),
                                                                     ('离馆轻声，别把胜利播报全场。', NULL, 1, NOW()),
                                                                     ('Ciallo～(∠・ω< )⌒★', NULL, 1, NOW());


/* 3) 生成 300 个 room（每个房间 80 座） */
INSERT INTO `room`
(`campus`, `building`, `room_name`, `total_seats`, `open_seats`, `status`, `remark`)
SELECT
    c.campus,
    b.building,
    r.room_name,
    80 AS total_seats,
    80 AS open_seats,
    'open' AS status,
    NULL AS remark
FROM
    (SELECT '本部校区' AS campus UNION ALL SELECT '东校区' UNION ALL SELECT '梅山校区') c
        CROSS JOIN
    (SELECT '图书馆' AS building UNION ALL SELECT '1号教学楼' UNION ALL SELECT '2号教学楼' UNION ALL SELECT '3号教学楼') b
        CROSS JOIN
    (
        SELECT CONCAT(f.floor, LPAD(n.num, 2, '0')) AS room_name
        FROM (SELECT 1 AS floor UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) f
                 CROSS JOIN (SELECT 1 AS num UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) n
    ) r;


/* 4) 生成 seat：每个 room 80 个座位 => 300×80 = 24000 */
INSERT INTO `seat` (`room_id`, `seat_no`, `status`, `remark`)
SELECT
    rm.id AS room_id,
    LPAD(nums.n, 2, '0') AS seat_no,   -- 01~80
    'enabled' AS status,
    NULL AS remark
FROM `room` rm
         CROSS JOIN (
    SELECT (t.d * 10 + o.d + 1) AS n
    FROM
        (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
         UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7) t
            CROSS JOIN
        (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
         UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) o
    WHERE (t.d * 10 + o.d) < 80
) nums;


/* 5) 校验：room=300, seat=24000 */
SELECT COUNT(*) AS room_cnt FROM room;
SELECT COUNT(*) AS seat_cnt FROM seat;



CREATE TABLE `announcement` (
                                `id` INT NOT NULL AUTO_INCREMENT,
                                `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
                                `content` TEXT NOT NULL COMMENT '公告内容（支持多行文本）',
                                `type` VARCHAR(32) NOT NULL DEFAULT 'OTHER' COMMENT 'RULE/ADJUSTMENT/EMERGENCY/MAINTENANCE/EXAM/OTHER',
                                `level` VARCHAR(16) NOT NULL DEFAULT 'INFO' COMMENT 'IMPORTANT/WARNING/INFO',
                                `target_role` TINYINT NOT NULL DEFAULT 2 COMMENT '0管理员 1学生 2全部',
                                `target_text` VARCHAR(64) DEFAULT '全体学生' COMMENT '面向对象文案',
                                `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0否 1是',
                                `is_published` TINYINT NOT NULL DEFAULT 1 COMMENT '是否发布：0否 1是',
                                `publish_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
                                `expire_time` DATETIME NULL COMMENT '过期时间（可为空）',
                                `created_by` INT NULL COMMENT '创建人 user.id（可为空）',
                                `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                KEY `idx_announcement_pub` (`is_published`, `publish_time`),
                                KEY `idx_announcement_target` (`target_role`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =========================
-- 示例数据（你可以删掉）
-- =========================
INSERT INTO `announcement`
(`title`, `content`, `type`, `level`, `target_role`, `target_text`, `is_top`, `is_published`, `publish_time`)
VALUES
    ('【开放时间调整】本部图书馆 301 自习室本周末延长开放至 22:30',
     '本周末（周六、周日）本部图书馆 301 自习室开放时间延长至 22:30。
    请同学们合理安排学习时间，离开时带走个人物品并保持安静。',
     'ADJUSTMENT', 'IMPORTANT', 1, '全体学生', 1, 1, NOW()),
    ('【规则】预约后请按时签到，累计多次将影响信用分',
     '请在预约开始后 15 分钟内完成签到；超过 15 分钟视为迟到。
    如连续出现未签到/迟到等情况，系统将记录违规并扣减信用分。',
     'RULE', 'INFO', 1, '全体学生', 0, 1, NOW()),
    ('【设备维护】东校区 3 楼自习室 4 月 3 日 9:00–12:00 暂停开放',
     '因设备维护，东校区 3 楼自习室将于 4 月 3 日 9:00–12:00 暂停开放。
    给您带来不便，敬请谅解。',
     'MAINTENANCE', 'WARNING', 1, '东校区学生', 0, 1, NOW());

INSERT INTO `announcement`
(`title`, `content`, `type`, `level`, `target_role`, `target_text`, `is_top`, `is_published`, `publish_time`, `expire_time`)
VALUES
    ('【规则】预约开始后 15 分钟内需签到，否则计为迟到', '请在预约开始后 15 分钟内完成签到。
超过 15 分钟未签到将计为迟到；超过 30 分钟未签到将自动取消并记一次未签到。
请合理安排到达时间，避免影响信用分。', 'RULE', 'INFO', 1, '全体学生', 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
    ('【规则】同一账号每日最多可预约 2 次', '为保障座位资源公平使用，同一账号每日最多可创建 2 次预约。
如确需长时段学习，请优先选择连续时段或在到期后续约。', 'RULE', 'INFO', 1, '全体学生', 0, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
    ('【调整】期末周部分自习室提前开放', '期末周（12/23—1/05）部分自习室将提前至 07:30 开放。
具体房间与开放安排以“自习室开放情况”页面为准。', 'ADJUSTMENT', 'IMPORTANT', 2, '全体学生', 1, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY)),
    ('【考试】四级模拟考试占用提示（图书馆 401）', '本周六 14:00—17:00 图书馆 401 将用于四级模拟考试。
该时段该房间将暂停对外预约。', 'EXAM', 'WARNING', 1, '全体学生', 0, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY)),
    ('【维护】空调检修：本部图书馆 2F', '因空调检修，本部图书馆二楼部分区域将出现短时噪音。
预计 09:00—11:00 完成，给您带来不便敬请谅解。', 'MAINTENANCE', 'INFO', 1, '本部学生', 0, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL),
    ('【突发】网络波动：在线签到可能延迟', '受网络波动影响，签到/签退状态可能出现 1—2 分钟延迟。
如遇到显示异常，请稍后刷新页面或重新进入。', 'EMERGENCY', 'WARNING', 2, '全体用户', 1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY)),
    ('【规则】请勿占座：离开超过 30 分钟将被记录', '为维护公平使用，离座超过 30 分钟且未保持有效签到状态，系统可能记录一次占座违规。
如需短时离开，请尽快返回并保持安静。', 'RULE', 'WARNING', 1, '全体学生', 0, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), NULL),
    ('【调整】新增“安静区/讨论区”分区标识', '为提升学习体验，部分自习室新增“安静区/讨论区”分区标识。
请同学们按区域规则使用座位，避免影响他人。', 'ADJUSTMENT', 'INFO', 1, '全体学生', 0, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), NULL),
    ('【其他】失物招领：图书馆 301 发现耳机', '图书馆 301 自习室发现一副黑色耳机。
请失主携带学生证前往服务台认领。', 'OTHER', 'INFO', 2, '全体用户', 0, 1, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY)),
    ('【维护】座椅更换：东校区 3 楼 302', '东校区 3 楼 302 自习室将进行座椅更换。
施工期间该房间部分座位暂不可预约。', 'MAINTENANCE', 'WARNING', 1, '东校区学生', 0, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY)),
    ('【规则】请保持环境整洁，离开带走垃圾', '请自觉保持自习室整洁，不在桌面粘贴小广告、张贴便签。
离开时请带走垃圾并归位椅子。', 'RULE', 'INFO', 2, '全体用户', 0, 1, DATE_SUB(NOW(), INTERVAL 11 DAY), NULL),
    ('【调整】部分房间照明升级，晚间亮度提升', '本周完成部分自习室照明升级，晚间亮度将有所提升。
如有灯光异常请通过“评价与投诉”反馈。', 'ADJUSTMENT', 'INFO', 2, '全体用户', 0, 1, DATE_SUB(NOW(), INTERVAL 12 DAY), NULL),
    ('【考试】研究生复试自习区临时安排', '研究生复试期间，部分房间将作为候考与安静复习区。
具体安排以当日公告为准。', 'EXAM', 'IMPORTANT', 1, '全体学生', 0, 1, DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY)),
    ('【突发】临时停电演练：15:00—15:10', '今日 15:00—15:10 将进行临时停电演练。
演练期间系统可能无法提交签到/签退，请提前安排。', 'EMERGENCY', 'IMPORTANT', 2, '全体用户', 1, 1, DATE_SUB(NOW(), INTERVAL 0 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY)),
    ('【其他】欢迎使用“座位偏好”设置', '你可以在“个人中心”设置常用校区、自习偏好等。
系统将优先推荐符合偏好的房间与座位。', 'OTHER', 'INFO', 1, '全体学生', 0, 1, DATE_SUB(NOW(), INTERVAL 14 DAY), NULL);

-- 1) 新增 status 字段（若已存在请跳过本步）
ALTER TABLE `user`
  ADD COLUMN `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '信用状态 0正常 1预警 2黑名单' AFTER `credit_score`;

-- 2) 旧黑名单迁移（黑名单优先）
UPDATE `user`
SET `status` = 2
WHERE `blacklist_flag` = 1;

-- 3) 非黑名单用户：按信用分自动迁移为预警/正常
UPDATE `user`
SET `status` = CASE WHEN `credit_score` < 80 THEN 1 ELSE 0 END
WHERE (`blacklist_flag` IS NULL OR `blacklist_flag` = 0);

-- 4) 删除旧字段 blacklist_flag（若你的 MySQL 版本不支持 IF EXISTS，请手动确认后执行）
ALTER TABLE `user`
  DROP COLUMN `blacklist_flag`;