SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for db_taskcenter
-- ----------------------------
CREATE TABLE IF NOT EXISTS `db_taskcenter`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
  `service_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务名',
  `service_method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务方法名',
  `callback_service_method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '服务回调方法名',
  `service_param` json NULL COMMENT '服务参数',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '错误信息',
  `url` varchar(1000) NULL COMMENT '下载地址',
  `progress` int(3) NOT NULL DEFAULT 0 COMMENT '进度',
  `status` int(3) NOT NULL DEFAULT 1 COMMENT '状态1、进行中2、等待中3、错误4、成功',
  `finish_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `is_deleted` bit(1) NULL DEFAULT b'0' COMMENT '是否删除',
  `creator_id` int(11) NULL DEFAULT NULL COMMENT '创建人id',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `gmt_created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务中心' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
