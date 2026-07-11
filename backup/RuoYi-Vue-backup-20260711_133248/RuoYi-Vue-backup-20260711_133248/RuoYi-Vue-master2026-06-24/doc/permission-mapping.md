# 权限与后端方法对照表

> 日期：2026-07-02 | 版本：v1.0

## 访客管理

| 权限 | Controller | 方法 | 作用 |
|------|-----------|------|------|
| `visitor:appointment:list` | VisitorAppointmentController | `list()` | 分页查询预约列表 |
| `visitor:appointment:query` | VisitorAppointmentController | `getInfo()` | 查单条预约详情 |
| `visitor:appointment:add` | VisitorAppointmentController | `add()` | 新增预约 + 自动匹配审批人 |
| `visitor:appointment:edit` | VisitorAppointmentController | `edit()` | 修改预约 |
| `visitor:appointment:remove` | VisitorAppointmentController | `remove()` | 批量删除（逻辑删） |
| `visitor:appointment:approve` | VisitorAppointmentController | `approve()` | 审批通过/拒绝 → 创建log + 发短信 |
| `visitor:appointment:cancel` | VisitorAppointmentController | `cancel()` | 取消预约 |
| `visitor:appointment:complete` | VisitorAppointmentController | `complete()` | 完成来访 → 记录离开时间 |
| `visitor:appointment:export` | VisitorAppointmentController | `export()` | 导出 Excel |
| `visitor:approval:list` | VisitorAppointmentController | `pending()` | 待审批列表（非admin按审批人过滤） |
| `visitor:log:list` | VisitorLogController | `list()` | 分页查看来访记录 |
| `visitor:log:query` | VisitorLogController | `getInfo()` | 查单条来访记录 |
| `visitor:register:add` 或 `visitor:log:add` | VisitorLogController | `add()` | 现场登记 → 创建预约+记录 |
| `visitor:log:edit` | VisitorLogController | `exit()` | 记录离开时间 |
| `visitor:log:export` | VisitorLogController | `export()` | 导出 Excel |

## 会议管理

| 权限 | Controller | 方法 | 作用 |
|------|-----------|------|------|
| `meeting:booking:list` | MeetingBookingController | `list()` | 分页查询预约列表 |
| `meeting:booking:query` | MeetingBookingController | `getInfo()` | 查单条预约详情 |
| `meeting:booking:add` | MeetingBookingController | `add()` | 新增预约 + 通知审批人短信 |
| `meeting:booking:edit` | MeetingBookingController | `edit()` | 修改预约 |
| `meeting:booking:remove` | MeetingBookingController | `remove()` | 删除预约 |
| `meeting:booking:approve` | MeetingBookingController | `approve()` | 审批通过 → 通知主持人短信 |
| `meeting:booking:reject` | MeetingBookingController | `reject()` | 审批拒绝 → 通知主持人短信 |
| `meeting:booking:cancel` | MeetingBookingController | `cancel()` | 取消预约 |
| `meeting:room:list` | MeetingRoomController | `list()` | 分页查询会议室 |
| `meeting:room:query` | MeetingRoomController | `getInfo()` | 查单个会议室 |
| `meeting:room:add` | MeetingRoomController | `add()` | 新增会议室 |
| `meeting:room:edit` | MeetingRoomController | `edit()` | 修改会议室 |
| `meeting:room:remove` | MeetingRoomController | `remove()` | 删除会议室 |

## IoT 设备管理

| 权限 | Controller | 方法 | 作用 |
|------|-----------|------|------|
| `device:list` | IotDeviceController | `list()` | 设备分页列表 |
| `device:query` | IotDeviceController | `getInfo()` | 设备详情 |
| `device:add` | IotDeviceController | `add()` | 新增设备 |
| `device:edit` | IotDeviceController | `edit()` | 修改设备 |
| `device:remove` | IotDeviceController | `remove()` | 删除设备 |
| `device:export` | IotDeviceController | `export()` | 导出 Excel |
| `device:repair` | IotDeviceController | `repair()` | 设备报修 |
| `device:heartbeat:list` | DeviceHeartbeatController | `list()` | 心跳日志分页 |
| `device:heartbeat:query` | DeviceHeartbeatController | `getInfo()` | 心跳日志详情 |
| `device:heartbeat:export` | DeviceHeartbeatController | `export()` | 导出心跳日志 |
| `device:statuslog:list` | DeviceStatusController | `list()` | 设备状态变更日志 |
| `device:statuslog:query` | DeviceStatusController | `getInfo()` | 状态日志详情 |
| `device:fault:list` | DeviceFaultController | `list()` | 故障预警列表 |
| `device:fault:repair` | DeviceFaultController | `repair()` | 故障报修 |
| `device:repair:list` | DeviceRepairController | `list()` | 维修工单列表 |
| `device:repair:query` | DeviceRepairController | `getInfo()` | 工单详情 |
| `device:repair:accept` | DeviceRepairController | `accept()` | 接单 |
| `device:repair:reject` | DeviceRepairController | `reject()` | 拒绝工单 |
| `device:repair:transfer` | DeviceRepairController | `transfer()` | 工单转派 |
| `device:repair:remove` | DeviceRepairController | `remove()` | 删除工单 |
| `device:parts:list` | DevicePartsController | `list()` | 配件管理列表 |
| `device:parts:query` | DevicePartsController | `getInfo()` | 配件详情 |
| `device:parts:add` | DevicePartsController | `add()` | 新增配件 |
| `device:parts:edit` | DevicePartsController | `edit()` | 修改配件 |
| `device:parts:remove` | DevicePartsController | `remove()` | 删除配件 |
| `device:port:list` | DevicePortController | `list()` | 端口管理列表 |
| `device:port:query` | DevicePortController | `getInfo()` | 端口详情 |
| `device:port:add` | DevicePortController | `add()` | 新增端口 |
| `device:port:edit` | DevicePortController | `edit()` | 修改端口 |
| `device:port:remove` | DevicePortController | `remove()` | 删除端口 |
| `device:snmp:query` | DeviceSnmpController | `query()` | SNMP 查询 |
| `device:topology:query` | DeviceTopologyController | `query()` | 拓扑查询 |

## 驾驶舱

| 权限 | Controller | 方法 | 作用 |
|------|-----------|------|------|
| `dashboard:device:query` | DashboardController | `device()` | 设备仪表盘数据 |
| `dashboard:visitor:query` | DashboardController | `visitor()` | 访客仪表盘数据 |
| `dashboard:meeting:query` | DashboardController | `meeting()` | 会议仪表盘数据 |
| `dashboard:apartment:query` | DashboardController | `apartment()` | 公寓仪表盘数据 |

## H5 轻应用

| 权限 | Controller | 方法 | 作用 |
|------|-----------|------|------|
| `device:list` | H5Controller | `deviceList()` | H5 设备状态列表 |
| `meeting:booking:list` | H5Controller | `meetingRooms()` | H5 会议室+当日安排 |
| `dashboard:device:query` | H5Controller | `news()` | H5 首页动态聚合 |

## 公寓管理

| 权限 | Controller | 方法 | 作用 |
|------|-----------|------|------|
| `hostel:apartment:list` | ApartmentInfoController | `list()` | 公寓分页列表 |
| `hostel:apartment:query` | ApartmentInfoController | `getInfo()` | 公寓详情 |
| `hostel:apartment:add` | ApartmentInfoController | `add()` | 新增公寓 |
| `hostel:apartment:edit` | ApartmentInfoController | `edit()` | 修改公寓 |
| `hostel:apartment:remove` | ApartmentInfoController | `remove()` | 删除公寓 |
| `hostel:room:list` | RoomInfoController | `list()` | 房源列表 |
| `hostel:room:query` | RoomInfoController | `getInfo()` | 房源详情 |
| `hostel:room:card` | RoomInfoController | `cardList()` | 房源卡片视图 |
| `hostel:room:add` | RoomInfoController | `add()` | 新增房源 |
| `hostel:room:edit` | RoomInfoController | `edit()` | 修改房源 |
| `hostel:room:remove` | RoomInfoController | `remove()` | 删除房源 |
| `hostel:tenant:list` | TenantInfoController | `list()` | 租客列表 |
| `hostel:tenant:query` | TenantInfoController | `getInfo()` | 租客详情 |
| `hostel:tenant:add` | TenantInfoController | `add()` | 新增租客 |
| `hostel:tenant:edit` | TenantInfoController | `edit()` | 修改租客 |
| `hostel:tenant:remove` | TenantInfoController | `remove()` | 删除租客 |
| `hostel:tenant:checkin` | TenantInfoController | `checkin()` | 入住/退租/续费 |

## 短信中台

| 权限 | Controller | 方法 | 作用 |
|------|-----------|------|------|
| `sms:channel:list` | SmsChannelController | `list()` | 渠道列表 |
| `sms:channel:query` | SmsChannelController | `getInfo()` | 渠道详情 |
| `sms:channel:add` | SmsChannelController | `add()` | 新增渠道 |
| `sms:channel:edit` | SmsChannelController | `edit()` | 修改渠道 |
| `sms:channel:remove` | SmsChannelController | `remove()` | 删除渠道 |
| `sms:channel:test` | SmsChannelController | `test()` | 渠道测试 |
| `sms:config:edit` | SmsConfigController | `edit()` | 短信配置修改 |
| `sms:biz:list` | SmsBizController | `list()` | 业务配置列表 |
| `sms:biz:query` | SmsBizController | `getInfo()` | 业务配置详情 |
| `sms:biz:add` | SmsBizController | `add()` | 新增业务 |
| `sms:biz:edit` | SmsBizController | `edit()` | 修改业务 |
| `sms:biz:remove` | SmsBizController | `remove()` | 删除业务 |
| `sms:signtemplate:list` | SmsSignTemplateController | `list()` | 签名模板列表 |
| `sms:signtemplate:query` | SmsSignTemplateController | `getInfo()` | 签名模板详情 |
| `sms:signtemplate:add` | SmsSignTemplateController | `add()` | 新增模板 |
| `sms:signtemplate:edit` | SmsSignTemplateController | `edit()` | 修改模板 |
| `sms:signtemplate:remove` | SmsSignTemplateController | `remove()` | 删除模板 |
| `sms:log:list` | SmsLogController | `list()` | 发送日志列表 |
| `sms:log:query` | SmsLogController | `getInfo()` | 日志详情 |
| `sms:log:send` | SmsLogController | `send()` | 发送测试短信 |
| `sms:log:retry` | SmsLogController | `retry()` | 重发 |
| `sms:log:cancel` | SmsLogController | `cancel()` | 撤销任务 |
| `sms:blacklist:list` | SmsBlacklistController | `list()` | 黑名单列表 |
| `sms:blacklist:query` | SmsBlacklistController | `getInfo()` | 黑名单详情 |
| `sms:blacklist:add` | SmsBlacklistController | `add()` | 新增黑名单 |
| `sms:blacklist:edit` | SmsBlacklistController | `edit()` | 修改黑名单 |
| `sms:blacklist:remove` | SmsBlacklistController | `remove()` | 删除黑名单 |

## 系统管理

| 权限 | Controller | 方法 | 作用 |
|------|-----------|------|------|
| `system:user:list` | SysUserController | `list()` | 用户列表 |
| `system:user:query` | SysUserController | `getInfo()` | 用户详情 |
| `system:user:add` | SysUserController | `add()` | 新增用户 |
| `system:user:edit` | SysUserController | `edit()` | 修改用户 |
| `system:user:remove` | SysUserController | `remove()` | 删除用户 |
| `system:user:export` | SysUserController | `export()` | 导出用户 |
| `system:user:import` | SysUserController | `importData()` | 导入用户 |
| `system:user:resetPwd` | SysUserController | `resetPwd()` | 重置密码 |
| `system:role:list` | SysRoleController | `list()` | 角色列表 |
| `system:role:query` | SysRoleController | `getInfo()` | 角色详情 |
| `system:role:add` | SysRoleController | `add()` | 新增角色 |
| `system:role:edit` | SysRoleController | `edit()` | 修改角色 |
| `system:role:remove` | SysRoleController | `remove()` | 删除角色 |
| `system:role:export` | SysRoleController | `export()` | 导出角色 |
| `system:dept:list` | SysDeptController | `list()` | 部门列表 |
| `system:dept:query` | SysDeptController | `getInfo()` | 部门详情 |
| `system:dept:add` | SysDeptController | `add()` | 新增部门 |
| `system:dept:edit` | SysDeptController | `edit()` | 修改部门 |
| `system:dept:remove` | SysDeptController | `remove()` | 删除部门 |
| `system:menu:list` | SysMenuController | `list()` | 菜单列表 |
| `system:menu:query` | SysMenuController | `getInfo()` | 菜单详情 |
| `system:menu:add` | SysMenuController | `add()` | 新增菜单 |
| `system:menu:edit` | SysMenuController | `edit()` | 修改菜单 |
| `system:menu:remove` | SysMenuController | `remove()` | 删除菜单 |
| `system:config:list` | SysConfigController | `list()` | 参数设置列表 |
| `system:config:query` | SysConfigController | `getInfo()` | 参数详情 |
| `system:config:add` | SysConfigController | `add()` | 新增参数 |
| `system:config:edit` | SysConfigController | `edit()` | 修改参数 |
| `system:config:remove` | SysConfigController | `remove()` | 删除参数 |
| `system:notice:list` | SysNoticeController | `list()` | 通知公告列表 |
| `system:notice:query` | SysNoticeController | `getInfo()` | 公告详情 |
| `system:notice:add` | SysNoticeController | `add()` | 新增公告 |
| `system:notice:edit` | SysNoticeController | `edit()` | 修改公告 |
| `system:notice:remove` | SysNoticeController | `remove()` | 删除公告 |

## 智能化管理

| 权限 | Controller | 方法 | 作用 |
|------|-----------|------|------|
| `smart:personAccess:list` | SmartAccessController | `list()/person()` | 人员通行设备列表 |
| `smart:personAccess:query` | SmartAccessController | `getInfo()` | 人员设备详情 |
| `smart:personAccess:add` | SmartAccessController | `add()` | 新增人员设备 |
| `smart:personAccess:edit` | SmartAccessController | `edit()` | 修改人员设备 |
| `smart:personAccess:remove` | SmartAccessController | `remove()` | 删除人员设备 |
| `smart:vehicleAccess:list` | SmartAccessController | `list()/vehicle()` | 车辆通行设备列表 |
| `smart:vehicleAccess:query` | SmartAccessController | `getInfo()` | 车辆设备详情 |
| `smart:vehicleAccess:add` | SmartAccessController | `add()` | 新增车辆设备 |
| `smart:vehicleAccess:edit` | SmartAccessController | `edit()` | 修改车辆设备 |
| `smart:vehicleAccess:remove` | SmartAccessController | `remove()` | 删除车辆设备 |
| `smart:personPerm:list` | SmartAccessController | `listPerm()` | 人员通行权限 |
| `smart:personPerm:add` | SmartAccessController | `addPerm()` | 新增人员权限 |
| `smart:personPerm:remove` | SmartAccessController | `removePerm()` | 删除人员权限 |
| `smart:vehiclePerm:list` | SmartAccessController | `listPerm()` | 车辆通行权限 |
| `smart:vehiclePerm:add` | SmartAccessController | `addPerm()` | 新增车辆权限 |
| `smart:vehiclePerm:remove` | SmartAccessController | `removePerm()` | 删除车辆权限 |
| `smart:videoDevice:list` | SmartVideoDeviceController | `list()` | 监控设备列表 |
| `smart:videoDevice:query` | SmartVideoDeviceController | `getInfo()` | 监控设备详情 |
| `smart:videoDevice:add` | SmartVideoDeviceController | `add()` | 新增监控设备 |
| `smart:videoDevice:edit` | SmartVideoDeviceController | `edit()` | 修改监控设备 |
| `smart:videoDevice:remove` | SmartVideoDeviceController | `remove()` | 删除监控设备 |
| `smart:videoMap:list` | SmartVideoMapController | `list()` | 地图模式列表 |
| `smart:videoMap:query` | SmartVideoMapController | `getInfo()` | 地图详情 |
| `smart:videoMap:add` | SmartVideoMapController | `add()` | 新增地图 |
| `smart:videoMap:edit` | SmartVideoMapController | `edit()` | 修改地图 |
| `smart:videoMap:remove` | SmartVideoMapController | `remove()` | 删除地图 |
| `smart:videoPreview:list` | SmartVideoPreviewController | `list()` | 视频预览 |

## 考勤管理

| 权限 | Controller | 方法 | 作用 |
|------|-----------|------|------|
| `attendance:data:list` | AttendanceController | `list()` | 考勤数据列表 |
| `attendance:data:add` | AttendanceController | `add()` | 新增考勤记录 |
| `attendance:data:remove` | AttendanceController | `delete()` | 删除考勤记录 |
| `attendance:import:list` | AttendanceAppController | `list()` | 数据对接配置列表 |

## 日志监控

| 权限 | Controller | 方法 | 作用 |
|------|-----------|------|------|
| `monitor:logininfor:list` | SysLogininforController | `list()` | 登录日志列表 |
| `monitor:logininfor:remove` | SysLogininforController | `remove()` | 删除登录日志 |
| `monitor:logininfor:export` | SysLogininforController | `export()` | 导出登录日志 |
| `monitor:logininfor:unlock` | SysLogininforController | `unlock()` | 账户解锁 |
| `monitor:operlog:list` | SysOperlogController | `list()` | 操作日志列表 |
| `monitor:operlog:remove` | SysOperlogController | `remove()` | 删除操作日志 |
| `monitor:operlog:export` | SysOperlogController | `export()` | 导出操作日志 |
