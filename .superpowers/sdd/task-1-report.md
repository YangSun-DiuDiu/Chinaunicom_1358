# Task 1 Report: 公寓房源管理 -- 数据库建表 + 菜单 SQL

**Status:** Completed
**Date:** 2026-06-28

---

## Files Created

| File | Description |
|------|-------------|
| `sql/hostel_management.sql` | 建表 SQL (apartment_info, room_info) + 4 条菜单 INSERT |

## Database Verification

**Server:** 1.94.26.126:3306/ry101

### Tables

| Table | Rows | Indexes |
|-------|------|---------|
| `apartment_info` | 11 columns | `idx_apt_dept` (dept_id) |
| `room_info` | 17 columns | `idx_room_apt` (apartment_id), `idx_room_status` (status), `idx_room_dept` (dept_id) |

### Menus

| menu_id | menu_name | parent_id | path | perms |
|---------|-----------|-----------|------|-------|
| 2113 | 公寓房源管理 | 0 | hostel | NULL |
| 2114 | 公寓信息 | 2113 | apartment | hostel:apartment:list |
| 2115 | 房间管理-表格 | 2113 | room | hostel:room:list |
| 2116 | 房间卡片视图 | 2113 | room/card | hostel:room:card |

---

## Summary

- 2 tables created (`apartment_info`, `room_info`) with all columns, indexes, and defaults
- 4 menu entries created (1 parent directory + 3 child pages)
- SQL file committed as `sql/hostel_management.sql`
