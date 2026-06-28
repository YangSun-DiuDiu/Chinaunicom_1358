# Task 1 Report: 租客管理 -- 数据库建表 + 菜单 SQL

**Status:** Completed
**Date:** 2026-06-28

---

## Files Modified

| File | Description |
|------|-------------|
| `sql/hostel_management.sql` | Appended tenant_info CREATE TABLE + 3 条菜单 INSERT |

## Database Verification

**Server:** 1.94.26.126:3306/ry101

### Tables

| Table | Columns | Notes |
|-------|---------|-------|
| `tenant_info` | 19 columns | PK tenant_id, FK room_id -> room_info |

### Columns

| Column | Type | Null | Default | Comment |
|--------|------|------|---------|---------|
| tenant_id | bigint | NO | auto_increment | PK |
| tenant_name | varchar(64) | NO | NULL | 姓名 |
| id_card | varchar(18) | YES | NULL | 身份证号 |
| phone | varchar(20) | YES | NULL | 手机号 |
| gender | char(1) | YES | M | M/F |
| room_id | bigint | YES | NULL | FK->room_info |
| check_in_date | date | YES | NULL | 入住日期 |
| check_out_date | date | YES | NULL | 退租日期 |
| rent_start | date | YES | NULL | 租期开始 |
| rent_end | date | YES | NULL | 租期结束 |
| status | varchar(20) | YES | NORMAL | NORMAL/CHECKED_OUT |
| openid | varchar(64) | YES | NULL | 微信openid |
| push_enabled | char(1) | YES | 1 | 推送开关 |
| dept_id | bigint | YES | NULL | |
| create_by | varchar(64) | YES | NULL | |
| create_time | datetime | YES | NULL | |
| update_by | varchar(64) | YES | NULL | |
| update_time | datetime | YES | NULL | |
| remark | varchar(500) | YES | NULL | |

### Menus

| menu_id | menu_name | parent_id | order_num | path | perms |
|---------|-----------|-----------|-----------|------|-------|
| 2125 | 租客管理 | 0 | 11 | tenant | NULL |
| 2126 | 租客档案 | 2125 | 1 | list | hostel:tenant:list |
| 2127 | 入住办理 | 2125 | 2 | checkin | hostel:tenant:checkin |

---

## Summary

- 1 table created (`tenant_info`) with 19 columns, PK, and defaults
- 3 menu entries created (1 parent directory + 2 child pages)
- SQL appended to `sql/hostel_management.sql`, committed as `ce1e4e8`
