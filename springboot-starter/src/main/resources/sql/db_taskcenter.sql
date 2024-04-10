SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for db_taskcenter
-- ----------------------------
CREATE TABLE `db_taskcenter_temp`  (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `title` varchar(255) NOT NULL COMMENT '标题',
    `service_name` varchar(255) NOT NULL COMMENT '服务名',
    `service_method` varchar(255) NOT NULL COMMENT '服务方法名',
    `callback_service_method` varchar(255) DEFAULT NULL COMMENT '服务回调方法名',
    `error_msg` text DEFAULT NULL COMMENT '错误信息',
    `url` varchar(1000) DEFAULT NULL COMMENT '下载地址',
    `progress` int(11) NOT NULL DEFAULT '0' COMMENT '进度',
    `status` int(11) NOT NULL DEFAULT '1' COMMENT '状态1、进行中2、等待中3、错误4、成功',
    `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
    `is_deleted` bit(1) DEFAULT b'0' COMMENT '是否删除',
    `creator_id` varchar(255) DEFAULT NULL COMMENT '创建人id',
    `creator` varchar(255) DEFAULT NULL COMMENT '创建人',
    `gmt_created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `run_url` varchar(255) DEFAULT NULL COMMENT '运行地址',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务中心' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
