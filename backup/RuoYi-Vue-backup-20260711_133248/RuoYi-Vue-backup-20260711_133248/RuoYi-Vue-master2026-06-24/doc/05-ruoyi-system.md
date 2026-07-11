# 05 — ruoyi-system 业务系统模块

## 模块定位

**业务系统层**，包含所有业务域实体、Service 接口与实现、Mapper 接口和 MyBatis XML 映射文件。是整个项目的业务逻辑核心。

**Maven 坐标**: `com.ruoyi:ruoyi-system:3.9.2` | 依赖: ruoyi-common

---

## 1. 整体包结构

```
com.ruoyi.system
 ├── domain/           — 24 个实体类 (domain directly + smart/ + vo/)
 ├── mapper/           — 24 个 Mapper 接口
 ├── service/          — 30+ Service 接口
 └── service/impl/     — 30+ ServiceImpl 实现

resources/mapper/system/ — 24 个 MyBatis XML 映射文件
```

---

## 2. RBAC 权限体系

### 2.1 权限模型

```
sys_user ──M:N── sys_user_role ──M:N── sys_role
    │                                      │
    │ 1:N                                  ├──M:N── sys_role_menu ──M:N── sys_menu
    ↓                                      │
sys_dept                              sys_role_dept ──M:N── sys_dept
    │
    └── sys_user_post ──M:N── sys_post
```

### 2.2 核心表

| 表 | 作用 |
|----|------|
| `sys_user` | 用户信息 |
| `sys_dept` | 部门/组织架构（树形） |
| `sys_role` | 角色定义 + 数据权限范围 |
| `sys_menu` | 菜单/权限（树形：目录M、菜单C、按钮F） |
| `sys_post` | 岗位 |
| `sys_user_role` | 用户-角色关联 |
| `sys_role_menu` | 角色-菜单/权限关联 |
| `sys_role_dept` | 角色-部门关联（数据权限） |
| `sys_user_post` | 用户-岗位关联 |

### 2.3 角色数据权限（data_scope）

| 值 | 常量 | 含义 |
|----|------|------|
| 1 | DATA_SCOPE_ALL | 全部数据 |
| 2 | DATA_SCOPE_CUSTOM | 自定义部门 |
| 3 | DATA_SCOPE_DEPT | 本部门 |
| 4 | DATA_SCOPE_DEPT_AND_CHILD | 本部门及子部门 |
| 5 | DATA_SCOPE_SELF | 仅本人 |

---

## 3. 业务模块全景

### 3.1 系统管理（标准若依）

| 功能 | Entity | Service | Mapper XML | Controller (在 admin) |
|------|--------|---------|------------|----------------------|
| 用户管理 | SysUser | ISysUserService | SysUserMapper.xml | SysUserController |
| 部门管理 | SysDept | ISysDeptService | SysDeptMapper.xml | SysDeptController |
| 角色管理 | SysRole | ISysRoleService | SysRoleMapper.xml | SysRoleController |
| 菜单管理 | SysMenu | ISysMenuService | SysMenuMapper.xml | SysMenuController |
| 岗位管理 | SysPost | ISysPostService | SysPostMapper.xml | - |
| 字典管理 | SysDictType/SysDictData | ISysDictTypeService/ISysDictDataService | 2 XML | - (由 common 提供) |
| 参数管理 | SysConfig | ISysConfigService | SysConfigMapper.xml | SysConfigController |
| 通知公告 | SysNotice/SysNoticeRead | ISysNoticeService/ISysNoticeReadService | 2 XML | SysNoticeController |
| 操作日志 | SysOperLog | ISysOperLogService | SysOperLogMapper.xml | SysOperlogController |
| 登录日志 | SysLogininfor | ISysLogininforService | SysLogininforMapper.xml | SysLogininforController |
| 在线用户 | SysUserOnline | ISysUserOnlineService | (无DB表—Redis查) | - |

### 3.2 IoT 设备管理

| 功能 | Entity | Service | 说明 |
|------|--------|---------|------|
| 设备管理 | Device | IDeviceService | CRUD + 统计(在线数/类型统计) |
| 心跳日志 | DeviceHeartbeatLog | IDeviceHeartbeatLogService | Ping 日志记录 |
| 设备端口 | DevicePort | IDevicePortService | 端口配置管理 |
| 故障管理 | (在 controller 无独立service) | - | 设备故障告警 |
| 维修工单 | DeviceRepair | IDeviceRepairService | 创建→转派→接单→拒绝→完成 |
| 状态变更 | DeviceStatusLog | IDeviceStatusLogService | 设备状态变化日志 |
| 备件管理 | (in controller) | - | 替换备件管理 |

### 3.3 访客管理

| 功能 | Entity | Service | 说明 |
|------|--------|---------|------|
| 访客预约 | VisitorAppointment | IVisitorAppointmentService | 预约→审批→取消→完成 |
| 访客记录 | VisitorLog | IVisitorLogService | 进出记录(H5自助+现场登记) |

### 3.4 智能化管理

| 功能 | Entity | 说明 |
|------|--------|------|
| 人车出入设备 | PersonAccessDevice | 闸机/门禁管理 |
| 出入权限 | PersonAccessPermission | 时间段+设备范围授权 |
| 视频监控 | (controller) | WebSocket 实时视频流 |

### 3.5 考勤管理

| 功能 | Entity | 说明 |
|------|--------|------|
| 考勤回调日志 | AttendanceCallbackLog | 钉钉/企业微信打卡数据 |
| 考勤同步 | DingTalkApiService / WeComApiService | Open API 对接 |

### 3.6 短信管理

| 功能 | Entity | 说明 |
|------|--------|------|
| 短信日志 | SmsLog | 发送记录 |
| 短信配置 | (读取 SysConfig) | 阿里云 AccessKey + 模板 |

### 3.7 仪表盘

| 功能 | Service | 说明 |
|------|---------|------|
| 设备仪表盘 | IDashboardService | 设备统计面板 |
| 访客仪表盘 | IDashboardService | 访客统计面板 |

---

## 4. Mapper 开发模式

所有 Mapper 遵循统一 MyBatis XML 模式：

```xml
<mapper namespace="com.ruoyi.system.mapper.XxxMapper">
  <resultMap type="Xxx" id="XxxResult">...</resultMap>
  <sql id="selectXxxVo">SELECT ... FROM xxx_table</sql>
  
  <select id="selectXxxList" parameterType="Xxx" resultMap="XxxResult">
  <!-- 动态条件查询 + ${params.dataScope} 数据权限 -->
  </select>
  
  <insert id="insertXxx" parameterType="Xxx" useGeneratedKeys="true" keyProperty="id">
  <!-- 动态插入 <if test="..."> -->
  </insert>
  
  <update id="updateXxx" parameterType="Xxx">
  <!-- 动态更新 + update_time = sysdate() -->
  </update>
</mapper>
```

## 5. Service 统一模式

```java
public interface IXxxService {
    List<Xxx> selectXxxList(Xxx xxx);      // 分页列表
    Xxx selectXxxById(Long id);            // 单条查询
    int insertXxx(Xxx xxx);                // 新增
    int updateXxx(Xxx xxx);                // 修改
    int deleteXxxByIds(Long[] ids);        // 批量删除
    // ... 业务特有方法
}
```

实现类添加 `@Service` + `@Transactional`，注入对应 Mapper。
