/*
 Navicat Premium Data Transfer

 Source Server         : 备课
 Source Server Type    : MySQL
 Source Server Version : 80044
 Source Host           : 127.0.0.1:3306
 Source Schema         : account_db

 Target Server Type    : MySQL
 Target Server Version : 80044
 File Encoding         : 65001

 Date: 10/04/2026 15:48:14
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for account
-- ----------------------------
DROP TABLE IF EXISTS `account`;
CREATE TABLE `account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '电话号码',
  `head_img` varchar(524) DEFAULT NULL COMMENT '头像',
  `status` varchar(16) DEFAULT 'ON' COMMENT '状态：ON，OFF',
  `role` varchar(255) DEFAULT NULL COMMENT '角色：COMMON普通用户，ADMIN超级会员',
  `openid` varchar(64) DEFAULT NULL COMMENT '微信openid',
  `unionid` varchar(64) DEFAULT NULL COMMENT '微信unionid',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_unionid` (`unionid`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表，支持微信登录';

-- ----------------------------
-- Records of account
-- ----------------------------
BEGIN;
INSERT INTO `account` VALUES (1, 'user_13113777335', NULL, '13113777335', NULL, 'ON', 'COMMON', NULL, NULL, '2026-03-18 13:45:08', '2026-03-18 13:45:08');
COMMIT;

-- ----------------------------
-- Table structure for account_benefit
-- ----------------------------
DROP TABLE IF EXISTS `account_benefit`;
CREATE TABLE `account_benefit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `account_id` bigint NOT NULL COMMENT '⽤户账号ID',
  `benefit_code` varchar(255) DEFAULT NULL COMMENT '权益编码',
  `product_order_id` bigint DEFAULT NULL COMMENT '来源订单ID',
  `remaining_count` int NOT NULL DEFAULT '0' COMMENT '剩余次数',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '总次数',
  `start_time` datetime NOT NULL COMMENT '⽣效时间',
  `end_time` datetime NOT NULL COMMENT '到期时间',
  `benefit_id` bigint DEFAULT NULL COMMENT '权益ID',
  `order_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '来源类型(package_order:套餐订单,benefit_order:权益订单)',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='⽤户权益表';

-- ----------------------------
-- Records of account_benefit
-- ----------------------------
BEGIN;
INSERT INTO `account_benefit` VALUES (14, 1, 'COMMON_INTERVIEW', 8, 2, 2, '2026-03-18 14:16:14', '2027-03-13 14:16:14', 5, 'PACKAGE_ORDER', '2026-03-18 14:16:14', '2026-03-18 14:16:14');
INSERT INTO `account_benefit` VALUES (15, 1, 'RESUME_ANALYSE', 8, 1, 10, '2026-03-18 14:16:15', '2026-09-14 14:16:15', 1, 'PACKAGE_ORDER', '2026-03-18 14:16:14', '2026-03-20 16:47:53');
INSERT INTO `account_benefit` VALUES (16, 1, 'SPECIAL_INTERVIEW', 8, 2, 2, '2026-03-18 14:16:15', '2027-03-13 14:16:15', 2, 'PACKAGE_ORDER', '2026-03-18 14:16:14', '2026-03-18 14:16:14');
COMMIT;

-- ----------------------------
-- Table structure for benefit_task
-- ----------------------------
DROP TABLE IF EXISTS `benefit_task`;
CREATE TABLE `benefit_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_id` bigint NOT NULL COMMENT '用户ID',
  `account_benefit_id` bigint NOT NULL COMMENT '用户权益记录ID',
  `use_times` int DEFAULT '1' COMMENT '使用次数(扣减次数)',
  `business_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '关联业务ID(如面试ID)',
  `benefit_code` varchar(64) NOT NULL DEFAULT '' COMMENT '权益编码(业务类型)',
  `message_id` varchar(64) DEFAULT NULL COMMENT '消息唯一标识(UUID)',
  `lock_state` varchar(16) DEFAULT 'LOCK' COMMENT '锁定状态:LOCK-锁定, FINISH-完成, CANCEL-取消',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_id` (`message_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权益任务表(本地事务消息表)';

-- ----------------------------
-- Records of benefit_task
-- ----------------------------
BEGIN;
INSERT INTO `benefit_task` VALUES (28, 1, 15, 1, '28', 'RESUME_ANALYSE', '15c1d714-3389-494a-86b7-280599a5fa32', 'CANCEL', '2026-04-10 12:46:10', '2026-04-10 12:46:10');
INSERT INTO `benefit_task` VALUES (29, 1, 15, 1, '30', 'RESUME_ANALYSE', 'fc04a20f-c87c-42ea-8ac0-1cc06195d299', 'CANCEL', '2026-04-10 14:02:57', '2026-04-10 14:02:57');
INSERT INTO `benefit_task` VALUES (30, 1, 15, 1, '31', 'RESUME_ANALYSE', '017180d3-c4ee-4e3d-be43-916d146f9730', 'CANCEL', '2026-04-10 14:06:09', '2026-04-10 14:06:09');
INSERT INTO `benefit_task` VALUES (31, 1, 15, 1, '31', 'RESUME_ANALYSE', '1f456533-a051-4f88-a8f4-eeceac41dbff', 'LOCK', '2026-04-10 14:12:50', '2026-04-10 14:12:50');
INSERT INTO `benefit_task` VALUES (32, 1, 15, 1, '35', 'RESUME_ANALYSE', '360397b0d7c447069230e10052bbac6b', 'FINISH', '2026-04-10 15:35:10', '2026-04-10 15:35:10');
INSERT INTO `benefit_task` VALUES (33, 1, 15, 1, '36', 'RESUME_ANALYSE', '8e73e92c9aad4925bcd18c9f8ecde203', 'FINISH', '2026-04-10 15:36:00', '2026-04-10 15:36:00');
INSERT INTO `benefit_task` VALUES (34, 1, 15, 1, '37', 'RESUME_ANALYSE', '119fcefeeb2d45b08dab8498b3776a4c', 'FINISH', '2026-04-10 15:39:30', '2026-04-10 15:39:30');
COMMIT;

-- ----------------------------
-- Table structure for benefit_usage_log
-- ----------------------------
DROP TABLE IF EXISTS `benefit_usage_log`;
CREATE TABLE `benefit_usage_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `account_id` bigint NOT NULL COMMENT '用户账号ID（关联account表id）',
  `biz_id` bigint NOT NULL COMMENT '使用权益ID（关联interview，resume表id）',
  `benefit_code` varchar(64) NOT NULL COMMENT '权益编码',
  `account_benefit` bigint NOT NULL COMMENT '用户权益ID（关联account_benefit表id）',
  `usage_time` datetime NOT NULL COMMENT '使用时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '使用备注',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权益使用日志表';

-- ----------------------------
-- Records of benefit_usage_log
-- ----------------------------
BEGIN;
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
