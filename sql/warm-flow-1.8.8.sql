-- ============================================================
-- warm-flow 1.8.8 工作流引擎建表脚本（访客预约审批替换自研状态机）
-- 依据：warm-flow 1.8.8 实体字段（反编译）+ 官方文档 table.html 类型口径
-- 执行范围：仅测试库（192.168.1.55，库名以实测为准）；禁止在生产库执行
-- 说明：1.8.8 无自动建表，必须手动执行；id 由引擎生成（SnowId19），主键不设自增
-- ============================================================

CREATE TABLE IF NOT EXISTS `flow_definition` (
  `id`              bigint       NOT NULL COMMENT '主键id',
  `flow_code`       varchar(40)  NOT NULL COMMENT '流程编码',
  `flow_name`       varchar(100) NOT NULL COMMENT '流程名称',
  `model_value`     varchar(40)  NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
  `category`        varchar(100) DEFAULT NULL COMMENT '流程类别',
  `version`         varchar(20)  NOT NULL COMMENT '流程版本',
  `is_publish`      tinyint(1)   NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
  `form_custom`     char(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path`       varchar(100) DEFAULT NULL COMMENT '审批表单路径',
  `activity_status` tinyint(1)   NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
  `listener_type`   varchar(100) DEFAULT NULL COMMENT '监听器类型',
  `listener_path`   varchar(400) DEFAULT NULL COMMENT '监听器路径',
  `ext`             varchar(500) DEFAULT NULL COMMENT '业务详情',
  `create_time`     datetime     DEFAULT NULL COMMENT '创建时间',
  `update_time`     datetime     DEFAULT NULL COMMENT '更新时间',
  `create_by`       varchar(64)  DEFAULT NULL COMMENT '创建者',
  `update_by`       varchar(64)  DEFAULT NULL COMMENT '更新者',
  `del_flag`        char(1)      DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `tenant_id`       varchar(40)  DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`),
  KEY `idx_fd_flow_code` (`flow_code`),
  KEY `idx_fd_publish` (`is_publish`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

CREATE TABLE IF NOT EXISTS `flow_node` (
  `id`              bigint        NOT NULL COMMENT '主键id',
  `node_type`       tinyint(1)    NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `definition_id`   bigint        NOT NULL COMMENT '流程定义id',
  `node_code`       varchar(100)  NOT NULL COMMENT '流程节点编码',
  `node_name`       varchar(100)  DEFAULT NULL COMMENT '流程节点名称',
  `permission_flag` varchar(200)  DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开）',
  `node_ratio`      decimal(6,3)  DEFAULT NULL COMMENT '流程签署比例值',
  `coordinate`      varchar(100)  DEFAULT NULL COMMENT '坐标',
  `any_node_skip`   varchar(100)  DEFAULT NULL COMMENT '任意结点跳转',
  `listener_type`   varchar(100)  DEFAULT NULL COMMENT '监听器类型',
  `listener_path`   varchar(400)  DEFAULT NULL COMMENT '监听器路径',
  `form_custom`     char(1)       DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path`       varchar(100)  DEFAULT NULL COMMENT '审批表单路径',
  `version`         varchar(20)   NOT NULL COMMENT '版本',
  `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
  `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
  `create_by`       varchar(64)   DEFAULT NULL COMMENT '创建者',
  `update_by`       varchar(64)   DEFAULT NULL COMMENT '更新者',
  `ext`             text          COMMENT '节点扩展属性',
  `del_flag`        char(1)       DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `tenant_id`       varchar(40)   DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`),
  KEY `idx_fn_definition` (`definition_id`),
  KEY `idx_fn_node_code` (`node_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点表';

CREATE TABLE IF NOT EXISTS `flow_skip` (
  `id`              bigint       NOT NULL COMMENT '主键id',
  `definition_id`   bigint       NOT NULL COMMENT '流程定义id',
  `node_id`         bigint       DEFAULT NULL COMMENT '节点id',
  `now_node_code`   varchar(100) NOT NULL COMMENT '当前流程节点的编码',
  `now_node_type`   tinyint(1)   DEFAULT NULL COMMENT '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `next_node_code`  varchar(100) NOT NULL COMMENT '下一个流程节点的编码',
  `next_node_type`  tinyint(1)   DEFAULT NULL COMMENT '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `skip_name`       varchar(100) DEFAULT NULL COMMENT '跳转名称',
  `skip_type`       varchar(40)  DEFAULT NULL COMMENT '跳转类型（PASS审批通过 REJECT退回）',
  `skip_condition`  varchar(200) DEFAULT NULL COMMENT '跳转条件',
  `coordinate`      varchar(100) DEFAULT NULL COMMENT '坐标',
  `create_time`     datetime     DEFAULT NULL COMMENT '创建时间',
  `update_time`     datetime     DEFAULT NULL COMMENT '更新时间',
  `create_by`       varchar(64)  DEFAULT NULL COMMENT '创建者',
  `update_by`       varchar(64)  DEFAULT NULL COMMENT '更新者',
  `del_flag`        char(1)      DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `tenant_id`       varchar(40)  DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`),
  KEY `idx_fs_definition` (`definition_id`),
  KEY `idx_fs_now_node` (`now_node_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点跳转关联表';

CREATE TABLE IF NOT EXISTS `flow_instance` (
  `id`              bigint       NOT NULL COMMENT '主键id',
  `definition_id`   bigint       NOT NULL COMMENT '流程定义id',
  `flow_name`       varchar(100) DEFAULT NULL COMMENT '流程名称',
  `business_id`     varchar(40)  NOT NULL COMMENT '业务id',
  `node_type`       tinyint(1)   NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `node_code`       varchar(40)  NOT NULL COMMENT '流程节点编码',
  `node_name`       varchar(100) DEFAULT NULL COMMENT '流程节点名称',
  `variable`        text         COMMENT '任务变量',
  `flow_status`     varchar(20)  NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
  `activity_status` tinyint(1)   NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
  `def_json`        text         COMMENT '流程定义json',
  `create_by`       varchar(64)  DEFAULT NULL COMMENT '创建者',
  `create_time`     datetime     DEFAULT NULL COMMENT '创建时间',
  `update_time`     datetime     DEFAULT NULL COMMENT '更新时间',
  `update_by`       varchar(64)  DEFAULT NULL COMMENT '更新者',
  `ext`             varchar(500) DEFAULT NULL COMMENT '扩展字段，预留给业务系统使用',
  `del_flag`        char(1)      DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `tenant_id`       varchar(40)  DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`),
  KEY `idx_fi_business` (`business_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';

CREATE TABLE IF NOT EXISTS `flow_task` (
  `id`            bigint       NOT NULL COMMENT '主键id',
  `definition_id` bigint       NOT NULL COMMENT '流程定义id',
  `instance_id`   bigint       NOT NULL COMMENT '流程实例id',
  `flow_name`     varchar(100) DEFAULT NULL COMMENT '流程名称',
  `business_id`   varchar(40)  DEFAULT NULL COMMENT '业务id',
  `node_code`     varchar(100) NOT NULL COMMENT '节点编码',
  `node_name`     varchar(100) DEFAULT NULL COMMENT '节点名称',
  `node_type`     tinyint(1)   NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `flow_status`   varchar(20)  NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
  `form_custom`   char(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path`     varchar(100) DEFAULT NULL COMMENT '审批表单路径',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
  `create_by`     varchar(64)  DEFAULT NULL COMMENT '创建者',
  `update_by`     varchar(64)  DEFAULT NULL COMMENT '更新者',
  `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `tenant_id`     varchar(40)  DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`),
  KEY `idx_ft_instance` (`instance_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办任务表';

CREATE TABLE IF NOT EXISTS `flow_his_task` (
  `id`                bigint       NOT NULL COMMENT '主键id',
  `definition_id`     bigint       NOT NULL COMMENT '流程定义id',
  `flow_name`         varchar(100) DEFAULT NULL COMMENT '流程名称',
  `instance_id`       bigint       NOT NULL COMMENT '流程实例id',
  `task_id`           bigint       NOT NULL COMMENT '任务id',
  `cooperate_type`    tinyint(1)   NOT NULL DEFAULT '0' COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
  `business_id`       varchar(40)  DEFAULT NULL COMMENT '业务id',
  `node_code`         varchar(100) DEFAULT NULL COMMENT '开始节点编码',
  `node_name`         varchar(100) DEFAULT NULL COMMENT '开始节点名称',
  `node_type`         tinyint(1)   DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `target_node_code`  varchar(200) DEFAULT NULL COMMENT '目标节点编码',
  `target_node_name`  varchar(200) DEFAULT NULL COMMENT '目标节点名称',
  `approver`          varchar(40)  DEFAULT NULL COMMENT '审批者',
  `collaborator`      varchar(40)  DEFAULT NULL COMMENT '协作人',
  `skip_type`         varchar(10)  NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
  `flow_status`       varchar(20)  NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
  `message`           varchar(500) DEFAULT NULL COMMENT '审批意见',
  `variable`          text         COMMENT '任务变量',
  `ext`               text         COMMENT '业务详情',
  `form_custom`       char(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path`         varchar(100) DEFAULT NULL COMMENT '审批表单路径',
  `create_time`       datetime     DEFAULT NULL COMMENT '任务开始时间',
  `update_time`       datetime     DEFAULT NULL COMMENT '审批完成时间',
  `del_flag`          char(1)      DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `tenant_id`         varchar(40)  DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`),
  KEY `idx_fht_instance` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史任务记录表';

CREATE TABLE IF NOT EXISTS `flow_user` (
  `id`           bigint      NOT NULL COMMENT '主键id',
  `type`         char(1)     NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
  `processed_by` varchar(80) DEFAULT NULL COMMENT '权限人',
  `associated`   bigint      NOT NULL COMMENT '任务表id',
  `create_time`  datetime    DEFAULT NULL COMMENT '创建时间',
  `create_by`    varchar(80) DEFAULT NULL COMMENT '创建人',
  `update_time`  datetime    DEFAULT NULL COMMENT '更新时间',
  `update_by`    varchar(64) DEFAULT NULL COMMENT '更新人',
  `del_flag`     char(1)     DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `tenant_id`    varchar(40) DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`),
  KEY `idx_fu_associated` (`associated`, `type`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程用户表';

-- ============================================================
-- 业务表关联列（测试库执行；生产库如需上线另行评估）
-- 预约表新增 warm-flow 流程实例ID：发起审批时回填，审批/取消时定位引擎实例
-- ============================================================
ALTER TABLE `visitor_appointment` ADD COLUMN `flow_instance_id` bigint DEFAULT NULL COMMENT 'warm-flow流程实例ID' AFTER `pass_code`;

-- ============================================================
-- 实测补丁（2026-10-31，依据 1.55 测试库实建结构 + jar 内 Flow*Mapper.xml insert 列清单）
-- 教训：官方 table.html 的 7 表结构不完整——缺 flow_form 表，且 flow_node/flow_skip
--       缺 create_by/update_by、flow_instance 缺 update_by（引擎 insert 会报 Unknown column）
-- 新库直接执行上方建表语句即可（已并入）；已按旧版建表的库执行以下补丁：
-- ============================================================
ALTER TABLE `flow_node`     ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT '创建者' AFTER `update_time`,
                            ADD COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT '更新者' AFTER `create_by`;
ALTER TABLE `flow_skip`     ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT '创建者' AFTER `update_time`,
                            ADD COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT '更新者' AFTER `create_by`;
ALTER TABLE `flow_instance` ADD COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT '更新者' AFTER `update_time`;

CREATE TABLE IF NOT EXISTS `flow_form` (
  `id`           bigint       NOT NULL COMMENT '主键id',
  `form_code`    varchar(40)  DEFAULT NULL COMMENT '表单编码',
  `form_name`    varchar(100) DEFAULT NULL COMMENT '表单名称',
  `version`      varchar(20)  DEFAULT NULL COMMENT '表单版本',
  `is_publish`   tinyint(1)   NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
  `form_type`    varchar(40)  DEFAULT NULL COMMENT '表单类型',
  `form_path`    varchar(100) DEFAULT NULL COMMENT '表单路径',
  `form_content` text         COMMENT '表单内容',
  `ext`          varchar(500) DEFAULT NULL COMMENT '业务详情',
  `create_time`  datetime     DEFAULT NULL COMMENT '创建时间',
  `update_time`  datetime     DEFAULT NULL COMMENT '更新时间',
  `del_flag`     char(1)      DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `tenant_id`    varchar(40)  DEFAULT NULL COMMENT '租户id',
  `create_by`    varchar(64)  DEFAULT NULL COMMENT '创建者',
  `update_by`    varchar(64)  DEFAULT NULL COMMENT '更新者',
  PRIMARY KEY (`id`),
  KEY `idx_ff_code` (`form_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程表单表';

