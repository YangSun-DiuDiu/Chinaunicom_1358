CREATE TABLE net_report (
  report_id     BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告ID',
  report_type   VARCHAR(20) NOT NULL COMMENT 'DAILY/WEEKLY/MONTHLY/QUARTERLY/HALF_YEAR/YEARLY',
  period_start  DATETIME NOT NULL COMMENT '统计开始',
  period_end    DATETIME NOT NULL COMMENT '统计结束',
  health_score  DECIMAL(5,2) COMMENT '总健康分',
  device_score  DECIMAL(5,2) COMMENT '设备维度分',
  line_score    DECIMAL(5,2) COMMENT '线路维度分',
  flow_score    DECIMAL(5,2) COMMENT '流量维度分',
  content_md    LONGTEXT COMMENT '大模型生成正文',
  raw_metrics   JSON COMMENT '原始指标快照',
  create_by     VARCHAR(64) DEFAULT 'system' COMMENT '创建者',
  create_time   DATETIME COMMENT '创建时间',
  del_flag      CHAR(1) DEFAULT '0' COMMENT '删除标志(0正常1删除)',
  INDEX idx_type_time (report_type, period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网络运行报告';
