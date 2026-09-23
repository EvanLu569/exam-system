CREATE DATABASE IF NOT EXISTS exam_system DEFAULT CHARACTER SET utf8mb4;
USE exam_system;

-- 1. 用户表
CREATE TABLE `user` (
                        `id` BIGINT NOT NULL AUTO_INCREMENT,
                        `username` VARCHAR(50) NOT NULL,
                        `password` VARCHAR(100) NOT NULL,
                        `role` VARCHAR(20) DEFAULT 'student',
                        `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB;

-- 2. 题库表
CREATE TABLE `question` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT,
                            `content` TEXT NOT NULL COMMENT '题干',
                            `options` JSON COMMENT '选项，JSON数组',
                            `correct_answer` VARCHAR(200) COMMENT '正确答案',
                            `score` INT DEFAULT 5 COMMENT '分值',
                            `type` INT DEFAULT 1 COMMENT '1单选 2多选 3判断',
                            `difficulty` INT DEFAULT 1 COMMENT '1简单 2中等 3困难',
                            `knowledge_point` VARCHAR(100) COMMENT '知识点',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (`id`),
                            KEY `idx_type` (`type`),
                            KEY `idx_knowledge` (`knowledge_point`)
) ENGINE=InnoDB;

-- 3. 试卷表
CREATE TABLE `exam_paper` (
                              `id` BIGINT NOT NULL AUTO_INCREMENT,
                              `name` VARCHAR(200) NOT NULL,
                              `duration_minutes` INT NOT NULL,
                              `total_score` INT DEFAULT 100,
                              `start_time` DATETIME,
                              `end_time` DATETIME,
                              `status` INT DEFAULT 0 COMMENT '0未发布 1已发布 2已结束',
                              `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB;

-- 4. 试卷-题目关联表
CREATE TABLE `paper_question` (
                                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                                  `paper_id` BIGINT NOT NULL,
                                  `question_id` BIGINT NOT NULL,
                                  `score` INT DEFAULT 5,
                                  `sort_order` INT DEFAULT 0,
                                  PRIMARY KEY (`id`),
                                  KEY `idx_paper` (`paper_id`)
) ENGINE=InnoDB;

-- 5. 考试记录表
CREATE TABLE `exam_record` (
                               `id` BIGINT NOT NULL AUTO_INCREMENT,
                               `paper_id` BIGINT NOT NULL,
                               `user_id` BIGINT NOT NULL,
                               `total_score` INT DEFAULT 0,
                               `start_time` DATETIME,
                               `submit_time` DATETIME,
                               `status` INT DEFAULT 0 COMMENT '0进行中 1已交卷',
                               PRIMARY KEY (`id`),
                               KEY `idx_paper_user` (`paper_id`, `user_id`)
) ENGINE=InnoDB;

-- 6. 答题明细表（新增）
CREATE TABLE `answer_record` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT,
                                 `record_id` BIGINT NOT NULL,
                                 `question_id` BIGINT NOT NULL,
                                 `user_answer` VARCHAR(500),
                                 `is_correct` TINYINT DEFAULT 0,
                                 `score` INT DEFAULT 0,
                                 PRIMARY KEY (`id`),
                                 KEY `idx_record` (`record_id`)
) ENGINE=InnoDB;

-- 7. 错题本（新增）
CREATE TABLE `wrong_question` (
                                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                                  `user_id` BIGINT NOT NULL,
                                  `question_id` BIGINT NOT NULL,
                                  `wrong_count` INT DEFAULT 1,
                                  `last_wrong_time` DATETIME,
                                  `next_review_time` DATETIME,
                                  `mastered` TINYINT DEFAULT 0,
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`)
) ENGINE=InnoDB;

-- ========== 测试数据 ==========

-- 用户
INSERT INTO `user` (`username`, `password`, `role`) VALUES ('admin', '123456', 'admin');
INSERT INTO `user` (`username`, `password`, `role`) VALUES ('student1', '123456', 'student');


-- 题目
INSERT INTO `question` (`content`, `options`, `correct_answer`, `score`, `type`, `difficulty`, `knowledge_point`) VALUES
                                                                                                                      ('以下哪个是 Java 集合接口？', '["List", "Thread", "Servlet", "Socket"]', 'A', 5, 1, 1, '集合'),
                                                                                                                      ('以下哪些是 Java 基本数据类型？', '["int", "String", "boolean", "double"]', 'ACD', 5, 2, 1, '基础语法'),
                                                                                                                      ('Java 中 String 是可变的。', '["正确", "错误"]', 'B', 5, 3, 1, '基础语法'),
                                                                                                                      ('HashMap 是线程安全的。', '["正确", "错误"]', 'B', 5, 3, 2, '集合'),
                                                                                                                      ('以下哪个不是 JVM 内存区域？', '["堆", "栈", "寄存器", "方法区"]', 'C', 5, 1, 3, 'JVM'),
                                                                                                                      ('ArrayList 和 LinkedList 的区别？', '["数组vs链表", "线程安全", "性能", "以上都是"]', 'A', 5, 1, 2, '集合'),
                                                                                                                      ('以下哪个是线程安全的集合？', '["ArrayList", "HashMap", "ConcurrentHashMap", "LinkedList"]', 'C', 5, 1, 2, '多线程'),
                                                                                                                      ('synchronized 和 Lock 的区别？', '["性能", "可中断", "公平性", "以上都是"]', 'D', 5, 1, 3, '多线程'),
                                                                                                                      ('JVM 垃圾回收算法有哪些？', '["标记清除", "复制", "标记整理", "以上都是"]', 'D', 5, 2, 3, 'JVM'),
                                                                                                                      ('以下哪些是 Java 8 新特性？', '["Lambda", "Stream", "Optional", "以上都是"]', 'D', 5, 2, 2, '基础语法'),
                                                                                                                      ('接口和抽象类的区别？', '["多继承", "构造方法", "成员变量", "以上都是"]', 'D', 5, 1, 2, '基础语法'),
                                                                                                                      ('Spring 的 IOC 是什么？', '["控制反转", "面向切面", "依赖注入", "以上都是"]', 'A', 5, 1, 2, 'Spring'),
                                                                                                                      ('Spring AOP 的实现原理？', '["动态代理", "反射", "字节码", "以上都是"]', 'D', 5, 1, 3, 'Spring'),
                                                                                                                      ('MySQL 索引的底层结构？', '["B+树", "红黑树", "哈希表", "链表"]', 'A', 5, 1, 2, 'MySQL'),
                                                                                                                      ('事务的四大特性是什么？', '["ACID", "BASE", "CAP", "以上都是"]', 'A', 5, 1, 2, 'MySQL');

-- 试卷
INSERT INTO `exam_paper` (`name`, `duration_minutes`, `total_score`, `status`)
VALUES ('测试考试', 30, 100, 1);

-- 试卷-题目关联（试卷1 关联 5 道题）
INSERT INTO `paper_question` (`paper_id`, `question_id`, `score`, `sort_order`) VALUES
                                                                                 (1, 1, 5, 1),
                                                                                 (1, 2, 5, 2),
                                                                                 (1, 3, 5, 3),
                                                                                 (1, 4, 5, 4),
                                                                                 (1, 5, 5, 5);

