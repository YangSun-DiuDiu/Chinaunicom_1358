-- ============================================================
-- 文档中心模块 — 建表 + 菜单 + 定时任务
-- ============================================================

-- 1. 同步日志表
DROP TABLE IF EXISTS `sys_doc_sync_log`;
CREATE TABLE `sys_doc_sync_log` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sync_time` DATETIME NOT NULL COMMENT '同步时间',
    `commit_hash` VARCHAR(64) DEFAULT '' COMMENT '触发同步的Git commit',
    `modules_total` INT(11) DEFAULT 0 COMMENT '模块总数',
    `files_added` INT(11) DEFAULT 0 COMMENT '新增文件数',
    `files_orphaned` INT(11) DEFAULT 0 COMMENT '过时文件数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '同步状态(SUCCESS/FAILED)',
    `error_msg` VARCHAR(2000) DEFAULT '' COMMENT '失败原因',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='文档同步日志表';

-- 2. 文档中心菜单 (parentMenuId: 2150)
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('文档中心', 0, 9, 'doc', NULL, 1, 0, 'M', '0', '0', NULL, 'documentation', 'admin', NOW(), '', NULL, '文档中心目录');

SET @doc_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('文档浏览', @doc_menu_id, 1, '', 'doc/index', 1, 0, 'C', '0', '0', 'doc:view', '#', 'admin', NOW(), '', NULL, '文档中心浏览页面');

SET @doc_view_id = LAST_INSERT_ID();

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('手动同步', @doc_view_id, 1, '', '', 1, 0, 'F', '0', '0', 'doc:sync', '#', 'admin', NOW(), '', NULL, '文档中心手动同步');

-- 3. 定时任务 (每分钟检查 Git 变化)
INSERT INTO `sys_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('文档同步任务', 'SYSTEM', 'docSyncTask.checkAndSync()', '0 * * * * ?', '1', '1', '0', 'admin', NOW(), '', NULL, '每分钟检查Git commit变化并同步文档骨架');
