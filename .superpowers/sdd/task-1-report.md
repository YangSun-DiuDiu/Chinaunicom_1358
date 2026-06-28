# Task 1 Report: 数据库五表 + 旧数据清理

> **Date:** 2026-06-29 | **Status:** COMPLETED | **DB:** ry101@1.94.26.126:3306

---

## 1. Summary

Created and executed `sql/sms_migration.sql` (206 lines). All 8 sections executed successfully against the production database.

## 2. Verification Results

### 2.1 Five tables exist

| # | Table | Rows | Engine |
|---|-------|------|--------|
| 1 | `sys_sms_channel` | 1 | InnoDB |
| 2 | `sys_sms_sign_template` | 1 | InnoDB |
| 3 | `sys_sms_biz` | 10 | InnoDB |
| 4 | `sys_sms_blacklist` | 0 | InnoDB |
| 5 | `sys_sms_log` (new schema) | 0 | InnoDB |

### 2.2 New sys_sms_log schema
- `send_mode` tinyint -- 1 instant / 2 scheduled
- `task_status` varchar(20) -- WAIT / SUCCESS / FAIL / CANCEL
- `retry_count` int -- retry counter
- No old columns (recipient, content, send_result, biz_type, biz_id, dept_id) remain

### 2.3 Aliyun channel
- channel_id=1, channel_type=ALIYUN, accessKeyId=LTAI5tAHTo19HSFGH2t28dUj, endpoint=dysmsapi.aliyuncs.com

### 2.4 Sign template
- st_id=1, channel_id=1, sign_name=智慧园区, template_code=SMS_123456789

### 2.5 10 biz_code records
device_repair, device_repair_transfer, device_offline_alert, smart_device_repair, device_fault_repair, device_repair_alert, device_online_notify, visitor_approval, sms_test, device_offline_old

### 2.6 Old configs deleted
0 rows remain matching `config_key LIKE 'sms.%'`

### 2.7 Old menus deleted
menu_id IN (2086, 2111, 2112) -- gone

### 2.8 New menus (parent=2132 短信中台)

| menu_id | menu_name | perms |
|---------|-----------|-------|
| 2132 | 短信中台 (M) | - |
| 2133 | 短信渠道管理 (C) | sms:channel:list |
| 2138 | 签名模板管理 (C) | sms:signtemplate:list |
| 2143 | 业务短信配置 (C) | sms:biz:list |
| 2148 | 短信黑名单 (C) | sms:blacklist:list |
| 2153 | 定时短信任务 (C) | sms:schedule:list |
| 2157 | 短信发送日志 (C) | sms:log:list |

Each C-level menu has F-button children (query/add/edit/remove or domain-specific).

---

## 3. Files Changed

- `sql/sms_migration.sql` -- NEW, 206 lines, complete self-contained migration
