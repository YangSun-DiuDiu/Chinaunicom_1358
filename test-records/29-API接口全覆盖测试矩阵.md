# 29-API接口全覆盖测试矩阵

## 说明

本矩阵覆盖项目全部 REST API 接口（100+），按模块分组。测试时请使用 Postman / Swagger UI (`/swagger-ui.html`) 或浏览器 F12 Network 逐一验证。

测试状态标记：✅通过 ❌失败 ⚠️跳过

---

## 1. 认证模块（公开接口）

| # | 方法 | 路径 | 正常测试 | 参数校验 | 异常测试 | 状态 |
|---|------|------|----------|----------|----------|------|
| 1 | POST | `/login` | 正确凭据登录返回token | 空参数/错误凭据 | 不存在用户/停用用户 | ☐ |
| 2 | GET | `/captchaImage` | 返回验证码图片和uuid | - | - | ☐ |
| 3 | POST | `/logout` | 登出成功 | 无token | token过期 | ☐ |
| 4 | GET | `/getInfo` | 返回用户+角色+权限 | 无token | token过期 | ☐ |
| 5 | GET | `/getRouters` | 返回菜单路由树 | 无token | 无权限用户返回空 | ☐ |
| 6 | GET | `/getConfig` | 返回系统配置 | 无token | - | ☐ |
| 7 | POST | `/unlockscreen` | 正确密码解锁 | 错误密码 | - | ☐ |

## 2. 用户管理 `/system/user/*`

| # | 方法 | 路径 | 权限 | 正常测试 | 筛选测试 | 校验 | 状态 |
|---|------|------|------|----------|----------|------|------|
| 8 | GET | `/system/user/list` | `system:user:list` | 分页返回用户 | 名称/手机/状态/时间组合 | - | ☐ |
| 9 | GET | `/system/user/{userId}` | `system:user:query` | 返回详情+角色+岗位 | - | 不存在的ID | ☐ |
| 10 | GET | `/system/user/` | `system:user:query` | 返回角色和岗位列表 | - | - | ☐ |
| 11 | GET | `/system/user/deptTree` | `system:user:list` | 返回部门树 | - | - | ☐ |
| 12 | GET | `/system/user/authRole/{userId}` | `system:user:query` | 返回已授权角色 | - | 无权用户 | ☐ |
| 13 | POST | `/system/user` | `system:user:add` | 新增用户成功 | - | 用户名重复/必填缺失 | ☐ |
| 14 | POST | `/system/user/export` | `system:user:export` | 下载Excel | 按条件导出 | 无数据导出 | ☐ |
| 15 | POST | `/system/user/importData` | `system:user:import` | 导入成功 | - | 错误格式/必填缺失 | ☐ |
| 16 | POST | `/system/user/importTemplate` | 公开 | 下载模板 | - | - | ☐ |
| 17 | PUT | `/system/user` | `system:user:edit` | 修改成功 | - | 唯一约束冲突 | ☐ |
| 18 | PUT | `/system/user/resetPwd` | `system:user:resetPwd` | 密码重置成功 | - | - | ☐ |
| 19 | PUT | `/system/user/changeStatus` | `system:user:edit` | 状态切换成功 | - | 停用admin | ☐ |
| 20 | PUT | `/system/user/authRole` | `system:user:edit` | 授权角色成功 | - | - | ☐ |
| 21 | DELETE | `/system/user/{userIds}` | `system:user:remove` | 删除成功 | - | 删除自己/admin | ☐ |

## 3. 角色管理 `/system/role/*`

| # | 方法 | 路径 | 权限 | 正常测试 | 状态 |
|---|------|------|------|----------|------|
| 22 | GET | `/system/role/list` | `system:role:list` | 分页返回角色 | ☐ |
| 23 | GET | `/system/role/{roleId}` | `system:role:query` | 返回角色详情 | ☐ |
| 24 | GET | `/system/role/deptTree/{roleId}` | `system:role:query` | 返回部门树（数据权限） | ☐ |
| 25 | GET | `/system/role/authUser/allocatedList` | `system:role:list` | 返回已分配用户 | ☐ |
| 26 | GET | `/system/role/authUser/unallocatedList` | `system:role:list` | 返回未分配用户 | ☐ |
| 27 | POST | `/system/role` | `system:role:add` | 新增角色 | ☐ |
| 28 | POST | `/system/role/export` | `system:role:export` | 导出Excel | ☐ |
| 29 | PUT | `/system/role` | `system:role:edit` | 修改角色 | ☐ |
| 30 | PUT | `/system/role/dataScope` | `system:role:edit` | 设置数据权限 | ☐ |
| 31 | PUT | `/system/role/changeStatus` | `system:role:edit` | 状态切换 | ☐ |
| 32 | PUT | `/system/role/authUser/cancel` | `system:role:edit` | 取消单个授权 | ☐ |
| 33 | PUT | `/system/role/authUser/cancelAll` | `system:role:edit` | 批量取消授权 | ☐ |
| 34 | PUT | `/system/role/authUser/selectAll` | `system:role:edit` | 批量授权 | ☐ |
| 35 | DELETE | `/system/role/{roleIds}` | `system:role:remove` | 删除角色 | ☐ |

## 4. 部门管理 `/system/dept/*`

| # | 方法 | 路径 | 权限 | 正常测试 | 状态 |
|---|------|------|------|----------|------|
| 36 | GET | `/system/dept/list` | `system:dept:list` | 返回部门树 | ☐ |
| 37 | GET | `/system/dept/list/exclude/{deptId}` | `system:dept:list` | 排除子节点 | ☐ |
| 38 | GET | `/system/dept/{deptId}` | `system:dept:query` | 返回部门详情 | ☐ |
| 39 | POST | `/system/dept` | `system:dept:add` | 新增部门 | ☐ |
| 40 | PUT | `/system/dept` | `system:dept:edit` | 修改部门 | ☐ |
| 41 | PUT | `/system/dept/updateSort` | `system:dept:edit` | 批量更新排序 | ☐ |
| 42 | DELETE | `/system/dept/{deptId}` | `system:dept:remove` | 删除部门 | ☐ |

## 5. 菜单管理 `/system/menu/*`

| # | 方法 | 路径 | 权限 | 正常测试 | 状态 |
|---|------|------|------|----------|------|
| 43 | GET | `/system/menu/list` | `system:menu:list` | 返回菜单树 | ☐ |
| 44 | GET | `/system/menu/{menuId}` | `system:menu:query` | 返回菜单详情 | ☐ |
| 45 | GET | `/system/menu/treeselect` | 用户级 | 返回菜单下拉树 | ☐ |
| 46 | GET | `/system/menu/roleMenuTreeselect/{roleId}` | 用户级 | 返回角色菜单树 | ☐ |
| 47 | POST | `/system/menu` | `system:menu:add` | 新增菜单 | ☐ |
| 48 | PUT | `/system/menu` | `system:menu:edit` | 修改菜单 | ☐ |
| 49 | PUT | `/system/menu/updateSort` | `system:menu:edit` | 更新排序 | ☐ |
| 50 | DELETE | `/system/menu/{menuId}` | `system:menu:remove` | 删除菜单 | ☐ |

## 6. 参数配置 `/system/config/*`

| # | 方法 | 路径 | 权限 | 正常测试 | 状态 |
|---|------|------|------|----------|------|
| 51 | GET | `/system/config/list` | `system:config:list` | 参数列表 | ☐ |
| 52 | GET | `/system/config/{configId}` | `system:config:query` | 参数详情 | ☐ |
| 53 | GET | `/system/config/configKey/{key}` | 需登录 | 按key取值 | ☐ |
| 54 | POST | `/system/config` | `system:config:add` | 新增参数 | ☐ |
| 55 | PUT | `/system/config` | `system:config:edit` | 修改参数 | ☐ |
| 56 | DELETE | `/system/config/{configIds}` | `system:config:remove` | 删除参数 | ☐ |
| 57 | DELETE | `/system/config/refreshCache` | `system:config:remove` | 刷新缓存 | ☐ |

## 7. 通知公告 `/system/notice/*`

| # | 方法 | 路径 | 权限 | 正常测试 | 状态 |
|---|------|------|------|----------|------|
| 58 | GET | `/system/notice/listTop` | 需登录 | 首页公告(top5) | ☐ |
| 59 | GET | `/system/notice/list` | `system:notice:list` | 公告列表 | ☐ |
| 60 | GET | `/system/notice/{noticeId}` | `system:notice:query` | 公告详情 | ☐ |
| 61 | POST | `/system/notice` | `system:notice:add` | 新增公告 | ☐ |
| 62 | PUT | `/system/notice` | `system:notice:edit` | 修改公告 | ☐ |
| 63 | DELETE | `/system/notice/{noticeIds}` | `system:notice:remove` | 删除公告 | ☐ |
| 64 | POST | `/system/notice/markRead` | 需登录 | 标记单条已读 | ☐ |
| 65 | POST | `/system/notice/markReadAll` | 需登录 | 全部标记已读 | ☐ |
| 66 | GET | `/system/notice/readUsers/list` | 需登录 | 已读用户列表 | ☐ |

## 8. 岗位管理 `/system/post/*`

| # | 方法 | 路径 | 权限 | 状态 |
|---|------|------|------|------|
| 67 | GET | `/system/post/list` | `system:post:list` | ☐ |
| 68 | GET | `/system/post/{postId}` | `system:post:query` | ☐ |
| 69 | POST | `/system/post` | `system:post:add` | ☐ |
| 70 | PUT | `/system/post` | `system:post:edit` | ☐ |
| 71 | DELETE | `/system/post/{postIds}` | `system:post:remove` | ☐ |

## 9. 字典管理 `/system/dict/type/*` + `/system/dict/data/*`

| # | 方法 | 路径 | 权限 | 状态 |
|---|------|------|------|------|
| 72 | GET | `/system/dict/type/list` | `system:dict:list` | ☐ |
| 73 | GET | `/system/dict/type/{dictId}` | `system:dict:query` | ☐ |
| 74 | POST | `/system/dict/type` | `system:dict:add` | ☐ |
| 75 | PUT | `/system/dict/type` | `system:dict:edit` | ☐ |
| 76 | DELETE | `/system/dict/type/{dictIds}` | `system:dict:remove` | ☐ |
| 77 | DELETE | `/system/dict/type/refreshCache` | 需登录 | ☐ |
| 78 | GET | `/system/dict/data/list` | `system:dict:list` | ☐ |
| 79 | GET | `/system/dict/data/{dictCode}` | `system:dict:query` | ☐ |
| 80 | GET | `/system/dict/data/type/{dictType}` | 需登录 | ☐ |
| 81 | POST | `/system/dict/data` | `system:dict:add` | ☐ |
| 82 | PUT | `/system/dict/data` | `system:dict:edit` | ☐ |
| 83 | DELETE | `/system/dict/data/{dictCodes}` | `system:dict:remove` | ☐ |

## 10. 操作日志 `/monitor/operlog/*`

| # | 方法 | 路径 | 权限 | 状态 |
|---|------|------|------|------|
| 84 | GET | `/monitor/operlog/list` | `monitor:operlog:list` | ☐ |
| 85 | POST | `/monitor/operlog/export` | `monitor:operlog:export` | ☐ |
| 86 | DELETE | `/monitor/operlog/{operIds}` | `monitor:operlog:remove` | ☐ |
| 87 | DELETE | `/monitor/operlog/clean` | `monitor:operlog:remove` | ☐ |

## 11. 登录日志 `/monitor/logininfor/*`

| # | 方法 | 路径 | 权限 | 状态 |
|---|------|------|------|------|
| 88 | GET | `/monitor/logininfor/list` | `monitor:logininfor:list` | ☐ |
| 89 | POST | `/monitor/logininfor/export` | `monitor:logininfor:export` | ☐ |
| 90 | DELETE | `/monitor/logininfor/{infoIds}` | `monitor:logininfor:remove` | ☐ |
| 91 | DELETE | `/monitor/logininfor/clean` | `monitor:logininfor:remove` | ☐ |
| 92 | GET | `/monitor/logininfor/unlock/{userName}` | `monitor:logininfor:unlock` | ☐ |

## 12. 设备管理 `/device/*` `/device/port/*` `/device/statuslog/*` `/device/heartbeat/*` `/device/fault/*` `/device/snmp/*`

| # | 方法 | 路径 | 权限 | 状态 |
|---|------|------|------|------|
| 93 | GET | `/device/list` | `device:list` | ☐ |
| 94 | GET | `/device/onlineCount` | `device:query` | ☐ |
| 95 | GET | `/device/offlineCount` | `device:query` | ☐ |
| 96 | GET | `/device/deviceTypeStats` | `device:query` | ☐ |
| 97 | GET | `/device/{deviceId}` | `device:query` | ☐ |
| 98 | GET | `/device/topology` | `device:topology:query` | ☐ |
| 99 | POST | `/device` | `device:add` | ☐ |
| 100 | PUT | `/device` | `device:edit` | ☐ |
| 101 | DELETE | `/device/{deviceIds}` | `device:remove` | ☐ |
| 102 | POST | `/device/export` | `device:export` | ☐ |
| 103 | POST | `/device/repair/{deviceId}` | `device:repair` | ☐ |
| 104 | GET | `/device/port/list` | `device:port:list` | ☐ |
| 105 | GET | `/device/port/{portId}` | `device:port:query` | ☐ |
| 106 | POST | `/device/port` | `device:port:add` | ☐ |
| 107 | PUT | `/device/port` | `device:port:edit` | ☐ |
| 108 | DELETE | `/device/port/{portIds}` | `device:port:remove` | ☐ |
| 109 | POST | `/device/port/bind` | `device:port:edit` | ☐ |
| 110 | GET | `/device/statuslog/list` | `device:statuslog:list` | ☐ |
| 111 | GET | `/device/statuslog/{logId}` | `device:statuslog:query` | ☐ |
| 112 | GET | `/device/heartbeat/list` | `device:heartbeat:list` | ☐ |
| 113 | POST | `/device/heartbeat/export` | `device:heartbeat:export` | ☐ |
| 114 | GET | `/device/fault/list` | `device:fault:list` | ☐ |
| 115 | GET | `/device/fault/stats` | `device:fault:list` | ☐ |
| 116 | POST | `/device/fault/repair/{deviceId}` | `device:fault:repair` | ☐ |
| 117 | GET | `/device/snmp/info/{deviceId}` | `device:snmp:query` | ☐ |
| 118 | GET | `/device/snmp/ports/{deviceId}` | `device:snmp:query` | ☐ |
| 119 | GET | `/device/snmp/nvrStorage/{deviceId}` | `device:snmp:query` | ☐ |
| 120 | GET | `/device/snmp/get/{deviceId}` | `device:snmp:query` | ☐ |
| 121 | GET | `/device/snmp/walk/{deviceId}` | `device:snmp:query` | ☐ |

## 13. 访客管理 `/visitor/appointment/*` `/visitor/log/*` `/pass/*`

| # | 方法 | 路径 | 权限 | 状态 |
|---|------|------|------|------|
| 122 | GET | `/visitor/appointment/list` | `visitor:appointment:list` | ☐ |
| 123 | GET | `/visitor/appointment/pending` | `visitor:appointment:pending` | ☐ |
| 124 | GET | `/visitor/appointment/{appointmentId}` | `visitor:appointment:query` | ☐ |
| 125 | POST | `/visitor/appointment` | `visitor:appointment:add` | ☐ |
| 126 | PUT | `/visitor/appointment` | `visitor:appointment:edit` | ☐ |
| 127 | DELETE | `/visitor/appointment/{appointmentIds}` | `visitor:appointment:remove` | ☐ |
| 128 | PUT | `/visitor/appointment/approve` | `visitor:appointment:approve` | ☐ |
| 129 | PUT | `/visitor/appointment/cancel/{appointmentId}` | `visitor:appointment:cancel` | ☐ |
| 130 | PUT | `/visitor/appointment/complete/{appointmentId}` | `visitor:appointment:complete` | ☐ |
| 131 | POST | `/visitor/appointment/export` | `visitor:appointment:export` | ☐ |
| 132 | GET | `/visitor/log/list` | `visitor:log:list` | ☐ |
| 133 | GET | `/visitor/log/{logId}` | `visitor:log:query` | ☐ |
| 134 | POST | `/visitor/log` | `visitor:log:add` | ☐ |
| 135 | PUT | `/visitor/log/exit/{logId}` | `visitor:log:edit` | ☐ |
| 136 | GET | `/visitor/log/export` | `visitor:log:export` | ☐ |
| 137 | GET | `/pass/{passCode}` | 公开 | ☐ |

## 14. 短信管理 `/sms/log/*`

| # | 方法 | 路径 | 权限 | 状态 |
|---|------|------|------|------|
| 138 | GET | `/sms/log/list` | `sms:log:list` | ☐ |
| 139 | GET | `/sms/log/{smsId}` | `sms:log:query` | ☐ |

## 15. 仪表盘 `/dashboard/*`

| # | 方法 | 路径 | 权限 | 状态 |
|---|------|------|------|------|
| 140 | GET | `/dashboard/device` | `dashboard:device:query` | ☐ |
| 141 | GET | `/dashboard/visitor` | `dashboard:visitor:query` | ☐ |

## 16. 通用接口 `/common/*`

| # | 方法 | 路径 | 权限 | 状态 |
|---|------|------|------|------|
| 142 | GET | `/common/download` | 需登录 | ☐ |
| 143 | GET | `/common/download/resource` | 需登录 | ☐ |
| 144 | POST | `/common/upload` | 需登录 | ☐ |
| 145 | POST | `/common/uploads` | 需登录 | ☐ |

## 17. 个人中心 `/system/user/profile/*`

| # | 方法 | 路径 | 权限 | 状态 |
|---|------|------|------|------|
| 146 | GET | `/system/user/profile` | 需登录 | ☐ |
| 147 | PUT | `/system/user/profile` | 需登录 | ☐ |
| 148 | PUT | `/system/user/profile/updatePwd` | 需登录 | ☐ |
| 149 | POST | `/system/user/profile/avatar` | 需登录 | ☐ |

---

## 测试统计

| 分类 | 接口数 | 已测 | 通过 | 失败 | 跳过 |
|------|--------|------|------|------|------|
| 认证模块 | 7 | 0 | 0 | 0 | 0 |
| 用户管理 | 14 | 0 | 0 | 0 | 0 |
| 角色管理 | 14 | 0 | 0 | 0 | 0 |
| 部门管理 | 7 | 0 | 0 | 0 | 0 |
| 菜单管理 | 8 | 0 | 0 | 0 | 0 |
| 参数配置 | 7 | 0 | 0 | 0 | 0 |
| 通知公告 | 9 | 0 | 0 | 0 | 0 |
| 岗位管理 | 5 | 0 | 0 | 0 | 0 |
| 字典管理 | 12 | 0 | 0 | 0 | 0 |
| 监控-操作日志 | 4 | 0 | 0 | 0 | 0 |
| 监控-登录日志 | 5 | 0 | 0 | 0 | 0 |
| 设备管理 | 29 | 0 | 0 | 0 | 0 |
| 访客管理 | 16 | 0 | 0 | 0 | 0 |
| 短信管理 | 2 | 0 | 0 | 0 | 0 |
| 仪表盘 | 2 | 0 | 0 | 0 | 0 |
| 通用接口 | 4 | 0 | 0 | 0 | 0 |
| 个人中心 | 4 | 0 | 0 | 0 | 0 |
| **总计** | **149** | **0** | **0** | **0** | **0** |

---

> 📝 每个接口需验证：正常请求返回200 + 参数校验 + 权限校验（403）+ 异常场景（404/500）
