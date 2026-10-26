/*
 Navicat Premium Data Transfer

 Source Server         : 备课
 Source Server Type    : MySQL
 Source Server Version : 80044
 Source Host           : 127.0.0.1:3306
 Source Schema         : product_db

 Target Server Type    : MySQL
 Target Server Version : 80044
 File Encoding         : 65001

 Date: 10/04/2026 15:48:59
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for banner
-- ----------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `location` varchar(128) DEFAULT NULL COMMENT '格式页面-位置-模块',
  `name` varchar(255) DEFAULT NULL COMMENT '中文名称',
  `img_url` varchar(2048) DEFAULT NULL COMMENT '图片列表，逗号分隔',
  `pc_link` varchar(2048) DEFAULT NULL COMMENT 'PC端跳转链接，逗号分隔',
  `text` varchar(2048) DEFAULT NULL COMMENT '文本',
  `status` varchar(16) DEFAULT NULL COMMENT '状态 ON在线，OFF下线',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of banner
-- ----------------------------
BEGIN;
INSERT INTO `banner` VALUES (8, 'BANNER', '轮播图1', 'https://file.xdclass.net/znoffer/banner/2025/12/04/37e15f35457548aab000da3b290fec70.png', 'znoffer.com', '简历综合多轮面试', 'ON', '2025-09-25 14:35:29', '2025-11-05 18:19:06');
INSERT INTO `banner` VALUES (9, 'BANNER', '轮播图4', 'https://file.xdclass.net/znoffer/banner/2025/12/04/ea2950fbdcb84644aa792281404f4d64.png', 'znoffer.com', '7*24小时最佳面试助手', 'ON', '2025-09-25 14:35:54', '2025-11-05 18:19:09');
COMMIT;

-- ----------------------------
-- Table structure for benefit
-- ----------------------------
DROP TABLE IF EXISTS `benefit`;
CREATE TABLE `benefit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权益ID',
  `name` varchar(100) NOT NULL COMMENT '权益名称',
  `benefit_code` varchar(255) DEFAULT NULL COMMENT '权益编码',
  `description` varchar(500) DEFAULT NULL COMMENT '权益描述',
  `unit_price` decimal(10,2) NOT NULL COMMENT '单次价格',
  `valid_days` int NOT NULL COMMENT '单次有效期(天)',
  `is_hot` tinyint NOT NULL DEFAULT '0' COMMENT '是否热门',
  `is_recommended` tinyint NOT NULL DEFAULT '0' COMMENT '是否推荐',
  `status` varchar(16) DEFAULT 'ON' COMMENT '状态',
  `sort` int DEFAULT '0' COMMENT '排序',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权益表';

-- ----------------------------
-- Records of benefit
-- ----------------------------
BEGIN;
INSERT INTO `benefit` VALUES (1, 'AI简历分析', 'RESUME_ANALYSE', 'AI系统分析简历优缺点，提供针对性优化建议', 29.00, 180, 0, 0, 'ON', 1, '2025-09-12 11:17:01', '2025-12-03 04:25:15');
INSERT INTO `benefit` VALUES (2, '专项面试', 'SPECIAL_INTERVIEW', '针对特定技术领域生成面试题并模拟面试', 39.00, 180, 0, 0, 'ON', 2, '2025-09-12 11:17:01', '2025-12-03 04:25:19');
INSERT INTO `benefit` VALUES (5, '简历综合面', 'COMMON_INTERVIEW', '基于简历直接进行面试，最高频题目', 49.00, 180, 1, 1, 'ON', 5, '2025-09-15 20:04:17', '2025-12-03 04:25:26');
INSERT INTO `benefit` VALUES (6, '面试题库', 'QUESTION_BANK', '互联网公司面试题库大集合', 599.00, 36500, 1, 1, 'ON', 0, '2025-11-11 23:03:14', '2025-12-03 04:13:01');
COMMIT;

-- ----------------------------
-- Table structure for package_benefit
-- ----------------------------
DROP TABLE IF EXISTS `package_benefit`;
CREATE TABLE `package_benefit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `product_package_id` bigint NOT NULL COMMENT '套餐ID',
  `benefit_code` varchar(64) NOT NULL COMMENT '权益编码',
  `count` int NOT NULL COMMENT '包含次数',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_package_benefit` (`product_package_id`,`benefit_code`),
  KEY `idx_benefit` (`benefit_code`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='套餐包含权益表';

-- ----------------------------
-- Records of package_benefit
-- ----------------------------
BEGIN;
INSERT INTO `package_benefit` VALUES (1, 1, 'RESUME_ANALYSE', 1, '2025-09-12 11:17:01');
INSERT INTO `package_benefit` VALUES (2, 1, 'SPECIAL_INTERVIEW', 2, '2025-09-12 11:17:01');
INSERT INTO `package_benefit` VALUES (3, 1, 'COMMON_INTERVIEW', 2, '2025-12-03 04:17:38');
INSERT INTO `package_benefit` VALUES (15, 2, 'RESUME_ANALYSE', 10, '2025-09-12 11:17:01');
INSERT INTO `package_benefit` VALUES (16, 2, 'SPECIAL_INTERVIEW', 10, '2025-09-12 11:17:01');
INSERT INTO `package_benefit` VALUES (19, 2, 'COMMON_INTERVIEW', 10, '2025-09-15 20:04:44');
INSERT INTO `package_benefit` VALUES (26, 3, 'COMMON_INTERVIEW', 30, '2025-09-15 20:04:59');
INSERT INTO `package_benefit` VALUES (27, 3, 'RESUME_ANALYSE', 30, '2025-09-12 11:17:01');
INSERT INTO `package_benefit` VALUES (29, 3, 'SPECIAL_INTERVIEW', 30, '2025-09-14 17:20:10');
INSERT INTO `package_benefit` VALUES (30, 3, 'QUESTION_BANK', 1, '2025-12-03 04:19:12');
INSERT INTO `package_benefit` VALUES (36, 4, 'COMMON_INTERVIEW', 15, '2025-09-15 20:05:16');
INSERT INTO `package_benefit` VALUES (37, 4, 'RESUME_ANALYSE', 15, '2025-09-12 11:17:01');
INSERT INTO `package_benefit` VALUES (38, 4, 'SPECIAL_INTERVIEW', 15, '2025-09-12 11:17:01');
INSERT INTO `package_benefit` VALUES (42, 4, 'QUESTION_BANK', 1, '2025-11-11 23:05:58');
COMMIT;

-- ----------------------------
-- Table structure for product_order
-- ----------------------------
DROP TABLE IF EXISTS `product_order`;
CREATE TABLE `product_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `account_id` bigint NOT NULL COMMENT '用户账号ID',
  `order_type` varchar(20) NOT NULL COMMENT '订单类型(package/benefit)',
  `title` varchar(255) DEFAULT NULL COMMENT '商品标题',
  `product_package_id` bigint DEFAULT NULL COMMENT '套餐ID',
  `benefit_id` bigint DEFAULT NULL COMMENT '权益ID',
  `purchase_count` int DEFAULT NULL COMMENT '购买次数',
  `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单次价格',
  `discount` decimal(5,2) NOT NULL DEFAULT '1.00' COMMENT '折扣',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `order_state` varchar(20) NOT NULL COMMENT '订单状态',
  `pay_type` varchar(20) DEFAULT NULL COMMENT '支付方式',
  `transaction_no` varchar(100) DEFAULT NULL COMMENT '第三方流水号',
  `out_trade_no` varchar(50) NOT NULL COMMENT '订单编号',
  `notify_time` datetime DEFAULT NULL COMMENT '支付回调时间',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购买订单表';

-- ----------------------------
-- Records of product_order
-- ----------------------------
BEGIN;
INSERT INTO `product_order` VALUES (1, 1, 'BENEFIT_ORDER', 'AI简历分析', NULL, 1, 2, 29.00, 1.00, 58.00, 'CANCEL', 'WECHAT_PAY', NULL, 'BFT1773812773079eILMKk', NULL, '2026-03-18 13:46:13', '2026-03-18 13:46:13');
INSERT INTO `product_order` VALUES (2, 1, 'PACKAGE_ORDER', '就业求职套餐-新人1元', 1, NULL, 1, 1.00, 1.00, 1.00, 'PAY', 'WECHAT_PAY', '4200003006202603180536667157', 'PKG17738128868881xgd7Z', '2026-03-18 13:58:08', '2026-03-18 13:48:07', '2026-03-18 13:58:07');
INSERT INTO `product_order` VALUES (3, 1, 'PACKAGE_ORDER', '就业求职套餐-新人1元', 1, NULL, 1, 1.00, 1.00, 1.00, 'PAY', 'WECHAT_PAY', '4200003091202603186788142201', 'PKG1773813036095irimXB', '2026-03-18 14:00:39', '2026-03-18 13:50:36', '2026-03-18 14:00:39');
INSERT INTO `product_order` VALUES (4, 1, 'PACKAGE_ORDER', '就业求职套餐-新人1元', 1, NULL, 1, 1.00, 1.00, 1.00, 'PAY', 'WECHAT_PAY', '4200003007202603183231905321', 'PKG17738131918181OQXw8', '2026-03-18 14:04:00', '2026-03-18 13:53:11', '2026-03-18 14:04:00');
INSERT INTO `product_order` VALUES (5, 1, 'PACKAGE_ORDER', '就业求职套餐-新人1元', 1, NULL, 1, 1.00, 1.00, 1.00, 'CANCEL', 'WECHAT_PAY', NULL, 'PKG1773813728448fhv05c', NULL, '2026-03-18 14:02:08', '2026-03-18 14:02:08');
INSERT INTO `product_order` VALUES (6, 1, 'PACKAGE_ORDER', '就业求职套餐-新人1元', 1, NULL, 1, 1.00, 1.00, 1.00, 'PAY', 'WECHAT_PAY', '4200003003202603184527812873', 'PKG1773813749466nFYqEI', '2026-03-18 14:02:59', '2026-03-18 14:02:29', '2026-03-18 14:02:59');
INSERT INTO `product_order` VALUES (7, 1, 'PACKAGE_ORDER', '就业求职套餐-新人1元', 1, NULL, 1, 1.00, 1.00, 1.00, 'PAY', 'WECHAT_PAY', '4200003054202603183381570670', 'PKG17738143040560lmbcc', '2026-03-18 14:12:13', '2026-03-18 14:11:44', '2026-03-18 14:12:12');
INSERT INTO `product_order` VALUES (8, 1, 'PACKAGE_ORDER', '就业求职套餐-新人1元', 1, NULL, 1, 1.00, 1.00, 1.00, 'PAY', 'WECHAT_PAY', '4200003069202603183322523822', 'PKG1773814548466zL7wQt', '2026-03-18 14:16:14', '2026-03-18 14:15:48', '2026-03-18 14:16:13');
COMMIT;

-- ----------------------------
-- Table structure for product_package
-- ----------------------------
DROP TABLE IF EXISTS `product_package`;
CREATE TABLE `product_package` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `name` varchar(100) NOT NULL COMMENT '套餐名称',
  `description` varchar(500) DEFAULT NULL COMMENT '套餐描述',
  `price` decimal(10,2) NOT NULL COMMENT '套餐价格',
  `valid_days` int NOT NULL COMMENT '有效期(天)',
  `is_recommended` tinyint DEFAULT '0' COMMENT '是否推荐',
  `status` varchar(16) DEFAULT 'ON' COMMENT '状态',
  `sort` int DEFAULT '0' COMMENT '排序',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='套餐表';

-- ----------------------------
-- Records of product_package
-- ----------------------------
BEGIN;
INSERT INTO `product_package` VALUES (1, '就业求职套餐-新人1元', '适合跳槽就业的面试准备，加客服领取-新人1元购买体验 ，请加下面微信 xdclass6', 1.00, 180, 1, 'ON', 1, '2025-09-12 11:17:01', '2026-03-18 13:47:52');
INSERT INTO `product_package` VALUES (2, '进阶跳槽套餐', '适合中级工程师的全面面试提升', 299.00, 180, 0, 'ON', 2, '2025-09-12 11:17:01', '2025-12-03 04:24:20');
INSERT INTO `product_package` VALUES (3, '资深专家套餐', '适合高级工程师的深度辅导', 999.00, 360, 0, 'ON', 3, '2025-09-12 11:17:01', '2025-12-03 04:14:54');
INSERT INTO `product_package` VALUES (4, '全能求职套餐', '适合各阶段工程师的全方位方案', 699.00, 360, 1, 'ON', 3, '2025-09-12 11:17:01', '2025-12-03 04:21:35');
INSERT INTO `product_package` VALUES (5, '大厂面试题库', '互联网公司跳槽社招和校招高频题库，包括编程语言、计算机基础、网络、AI大模型、算法等 几千道高频面试题+图文答案', 399.00, 36500, 0, 'ON', 0, '2025-11-11 23:05:35', '2025-12-10 07:26:56');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
