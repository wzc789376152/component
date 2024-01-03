ALTER TABLE `db_taskcenter` MODIFY COLUMN  `creator_id` VARCHAR(255);
ALTER TABLE `db_taskcenter` ADD COLUMN `run_url` VARCHAR(255) DEFAULT NULL COMMENT '运行地址';