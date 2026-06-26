# Task 1 Report: 数据库菜单 + 权限调整

## Status: COMPLETED

## What was done

1. Deleted old menu 2033 (会议预约) from `sys_role_menu` and `sys_menu`
2. Inserted new 会议预约 menu (id=2035) at order_num=2 with perms `meeting:booking:list`
3. Inserted new 会议审批 menu (id=2036) at order_num=3 with perms `meeting:approval:list`
4. Assigned both new menus (2035, 2036) to role_id=1 (admin)

## Verification results

```text
sys_menu under parent 2031:
  2032  会议室管理  order=1  meeting:room:list
  2035  会议预约    order=2  meeting:booking:list
  2034  会议管理    order=3  meeting:booking:list
  2036  会议审批    order=3  meeting:approval:list

sys_role_menu:
  role=1 -> menu 2035 (会议预约, meeting:booking:list)
  role=1 -> menu 2036 (会议审批, meeting:approval:list)
```

## Database

- Host: 1.94.26.126:3306
- Database: ry101
- User: sadmin
