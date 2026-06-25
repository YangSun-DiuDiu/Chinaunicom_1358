import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/* Layout */
import Layout from '@/layout'

/**
 * 全静态路由配置 - 废弃若依动态路由
 * 所有路由在配置文件中静态定义，根据用户权限动态渲染侧边栏
 */

// 静态路由（所有用户均可访问的基础路由）
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '/',
    component: Layout,
    redirect: 'index',
    children: [
      {
        path: 'index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/h5-register',
    component: () => import('@/views/visitor/h5/register'),
    hidden: true,
    meta: { title: '访客自助登记' }
  },
  {
    path: '/repair-complete',
    component: () => import('@/views/device/repair/complete'),
    hidden: true,
    meta: { title: '维修确认' }
  },
  {
    path: '/lock',
    component: () => import('@/views/lock'),
    hidden: true,
    meta: { title: '锁定屏幕' }
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
]

// 全静态业务路由 - 根据权限过滤侧边栏显示
export const staticRoutes = [
  // ==================== 人员管理 ====================
  {
    path: '/system',
    component: Layout,
    name: 'System',
    alwaysShow: true,
    meta: { title: '人员管理', icon: 'user' },
    children: [
      {
        path: 'user',
        component: () => import('@/views/system/user/index'),
        name: 'User',
        meta: { title: '用户管理', icon: 'people', permissions: ['system:user:list'] }
      },
      {
        path: 'dept',
        component: () => import('@/views/system/dept/index'),
        name: 'Dept',
        meta: { title: '部门管理', icon: 'tree', permissions: ['system:dept:list'] }
      },
      {
        path: 'role',
        component: () => import('@/views/system/role/index'),
        name: 'Role',
        meta: { title: '角色管理', icon: 'peoples', permissions: ['system:role:list'] }
      }
    ]
  },
  // ==================== 设备管理 ====================
  {
    path: '/device',
    component: Layout,
    name: 'Device',
    alwaysShow: true,
    meta: { title: '设备管理', icon: 'monitor' },
    children: [
      {
        path: 'list',
        component: () => import('@/views/device/list/index'),
        name: 'DeviceList',
        meta: { title: '设备列表', icon: 'monitor', permissions: ['device:list'] }
      },
      {
        path: 'topology',
        component: () => import('@/views/device/topology/index'),
        name: 'DeviceTopology',
        meta: { title: '设备拓扑', icon: 'tree', permissions: ['device:topology:view'] }
      },
      {
        path: 'heartbeat',
        component: () => import('@/views/device/heartbeat/index'),
        name: 'DeviceHeartbeat',
        meta: { title: '心跳日志', icon: 'log', permissions: ['device:heartbeat:list'] }
      },
      {
        path: 'fault',
        component: () => import('@/views/device/fault/index'),
        name: 'DeviceFault',
        meta: { title: '故障预警', icon: 'bell', permissions: ['device:fault:list'] }
      },
      {
        path: 'snmp',
        component: () => import('@/views/device/snmp/index'),
        name: 'DeviceSnmp',
        meta: { title: 'SNMP管理', icon: 'monitor', permissions: ['device:snmp:query'] }
      },
      {
        path: 'repair',
        component: () => import('@/views/device/repair/index'),
        name: 'DeviceRepair',
        meta: { title: '维修工单', icon: 'tool', permissions: ['device:repair:list'] }
      },
      {
        path: 'repair-workload',
        component: () => import('@/views/device/repair/workload'),
        name: 'DeviceRepairWorkload',
        hidden: true,
        meta: { title: '工作量统计', permissions: ['device:repair:list'] }
      },
      {
        path: 'parts',
        component: () => import('@/views/device/parts/index'),
        name: 'DeviceParts',
        meta: { title: '配件管理', icon: 'component', permissions: ['device:parts:list'] }
      }
    ]
  },
  // ==================== 访客管理 ====================
  {
    path: '/visitor',
    component: Layout,
    name: 'Visitor',
    alwaysShow: true,
    meta: { title: '访客管理', icon: 'people' },
    children: [
      {
        path: 'appointment',
        component: () => import('@/views/visitor/appointment/index'),
        name: 'VisitorAppointment',
        meta: { title: '访客预约', icon: 'edit', permissions: ['visitor:appointment:list'] }
      },
      {
        path: 'approval',
        component: () => import('@/views/visitor/approval/index'),
        name: 'VisitorApproval',
        meta: { title: '访客审批', icon: 'checkbox', permissions: ['visitor:appointment:pending'] }
      },
      {
        path: 'register',
        component: () => import('@/views/visitor/register/index'),
        name: 'VisitorRegister',
        meta: { title: '现场登记', icon: 'form', permissions: ['visitor:register:add'] }
      },
      {
        path: 'log',
        component: () => import('@/views/visitor/log/index'),
        name: 'VisitorLog',
        meta: { title: '来访记录', icon: 'list', permissions: ['visitor:log:list'] }
      }
    ]
  },
  // ==================== 日志管理 ====================
  {
    path: '/log',
    component: Layout,
    name: 'Log',
    meta: { title: '日志管理', icon: 'documentation' },
    children: [
      {
        path: 'operlog',
        component: () => import('@/views/monitor/operlog/index'),
        name: 'OperLog',
        meta: { title: '操作日志', icon: 'form', permissions: ['monitor:operlog:list'] }
      },
      {
        path: 'logininfor',
        component: () => import('@/views/monitor/logininfor/index'),
        name: 'LoginLog',
        meta: { title: '登录日志', icon: 'logininfor', permissions: ['monitor:logininfor:list'] }
      }
    ]
  },
  // ==================== 系统管理 ====================
  {
    path: '/settings',
    component: Layout,
    name: 'Settings',
    meta: { title: '系统管理', icon: 'system' },
    children: [
      {
        path: 'menu',
        component: () => import('@/views/system/menu/index'),
        name: 'Menu',
        meta: { title: '菜单管理', icon: 'tree-table', permissions: ['system:menu:list'] }
      }
    ]
  },
  // ==================== 隐藏路由(不在侧边栏显示) ====================
  {
    path: '/system',
    component: Layout,
    hidden: true,
    children: [
      {
        path: 'user-auth/role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/system/user', permissions: ['system:user:edit'] }
      },
      {
        path: 'role-auth/user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/system/role', permissions: ['system:role:edit'] }
      }
    ]
  },
  // ==================== 智能化管理 ====================
  {
    path: '/smart',
    component: Layout,
    name: 'Smart',
    alwaysShow: true,
    meta: { title: '智能化管理', icon: 'cpu' },
    children: [
      {
        path: 'person-access-device',
        component: () => import('@/views/smart/person-access/device/index'),
        name: 'PersonAccessDevice',
        meta: { title: '人员通行设备', icon: 'monitor', permissions: ['smart:personAccess:list'] }
      },
      {
        path: 'person-access-permission',
        component: () => import('@/views/smart/person-access/permission/index'),
        name: 'PersonAccessPerm',
        meta: { title: '通行权限管理', icon: 'lock', permissions: ['smart:personPerm:list'] }
      },
      {
        path: 'vehicle-access-device',
        component: () => import('@/views/smart/vehicle-access/device/index'),
        name: 'VehicleAccessDevice',
        meta: { title: '车辆通行设备', icon: 's-order', permissions: ['smart:vehicleAccess:list'] }
      },
      {
        path: 'vehicle-access-permission',
        component: () => import('@/views/smart/vehicle-access/permission/index'),
        name: 'VehicleAccessPerm',
        meta: { title: '车辆通行权限', icon: 'lock', permissions: ['smart:vehiclePerm:list'] }
      },
      {
        path: 'video-device',
        component: () => import('@/views/smart/video/device/index'),
        name: 'VideoDevice',
        meta: { title: '监控设备管理', icon: 'video-camera', permissions: ['smart:videoDevice:list'] }
      },
      {
        path: 'video-preview',
        component: () => import('@/views/smart/video/preview/index'),
        name: 'VideoPreview',
        meta: { title: '视频预览', icon: 'video-play', permissions: ['smart:videoPreview:list'] }
      },
      {
        path: 'video-map',
        component: () => import('@/views/smart/video/map/index'),
        name: 'VideoMap',
        meta: { title: '地图模式', icon: 'location', permissions: ['smart:videoMap:list'] }
      }
    ]
  },
  // ==================== 考勤管理 ====================
  {
    path: '/attendance',
    component: Layout,
    name: 'Attendance',
    alwaysShow: true,
    meta: { title: '考勤管理', icon: 'date' },
    children: [
      {
        path: 'import',
        component: () => import('@/views/attendance/import/index'),
        name: 'AttendanceImport',
        meta: { title: '数据对接', icon: 'upload', permissions: ['attendance:import:list'] }
      },
      {
        path: 'data',
        component: () => import('@/views/attendance/data/index'),
        name: 'AttendanceData',
        meta: { title: '考勤数据', icon: 's-data', permissions: ['attendance:data:list'] }
      }
    ]
  },
  // ==================== 短信管理 ====================
  {
    path: '/sms',
    component: Layout,
    name: 'Sms',
    alwaysShow: true,
    meta: { title: '短信管理', icon: 'message' },
    children: [
      {
        path: 'config',
        component: () => import('@/views/sms/config/index'),
        name: 'SmsConfig',
        meta: { title: '短信设置', icon: 'edit', permissions: ['sms:config:edit'] }
      },
      {
        path: 'log',
        component: () => import('@/views/sms/log/index'),
        name: 'SmsLogView',
        meta: { title: '发送记录', icon: 'list', permissions: ['sms:log:list'] }
      }
    ]
  }
]

// 防止连续点击多次路由报错
let routerPush = Router.prototype.push
let routerReplace = Router.prototype.replace
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch(err => err)
}
Router.prototype.replace = function push(location) {
  return routerReplace.call(this, location).catch(err => err)
}

export default new Router({
  mode: 'history',
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})
