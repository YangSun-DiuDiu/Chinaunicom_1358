import Vue from "vue";
import Router from "vue-router";

Vue.use(Router);

/* Layout */
import Layout from "@/layout";

/**
 * 全静态路由配置 - 废弃若依动态路由
 * 所有路由在配置文件中静态定义，根据用户权限动态渲染侧边栏
 */

// 静态路由（所有用户均可访问的基础路由）
export const constantRoutes = [
  {
    path: "/redirect",
    component: Layout,
    hidden: true,
    children: [
      {
        path: "/redirect/:path(.*)",
        component: () => import("@/views/redirect"),
      },
    ],
  },
  {
    path: "/login",
    component: () => import("@/views/login"),
    hidden: true,
  },
  {
    path: "/404",
    component: () => import("@/views/error/404"),
    hidden: true,
  },
  {
    path: "/401",
    component: () => import("@/views/error/401"),
    hidden: true,
  },
  {
    path: "/",
    component: Layout,
    redirect: "index",
    children: [
      {
        path: "index",
        component: () => import("@/views/index"),
        name: "Index",
        meta: { title: "首页", icon: "home", affix: true },
      },
    ],
  },
  {
    path: "/h5-register",
    component: () => import("@/views/visitor/h5/register"),
    hidden: true,
    meta: { title: "访客自助登记" },
  },
  {
    path: "/lock",
    component: () => import("@/views/lock"),
    hidden: true,
    meta: { title: "锁定屏幕" },
  },
  {
    path: "/user",
    component: Layout,
    hidden: true,
    redirect: "noredirect",
    children: [
      {
        path: "profile",
        component: () => import("@/views/system/user/profile/index"),
        name: "Profile",
        meta: { title: "个人中心", icon: "user" },
      },
    ],
  },
  // ==================== H5 登录（白名单） ====================
  {
    path: "/h5/login",
    component: () => import("@/views/h5/login"),
    hidden: true,
    meta: { title: "H5登录" },
  },
];

// 全静态业务路由 - 根据权限过滤侧边栏显示
export const staticRoutes = [
  // ==================== H5 功能页（需登录+权限） ====================
  {
    path: "/h5",
    component: () => import("@/views/h5/index"),
    hidden: true,
    meta: { title: "园区服务" },
  },
  {
    path: "/h5/visitor",
    component: () => import("@/views/h5/visitor"),
    hidden: true,
    meta: { title: "访客登记", permissions: ["visitor:appointment:add"] },
  },
  {
    path: "/h5/visitor/pass",
    component: () => import("@/views/h5/visitor-pass"),
    hidden: true,
    meta: { title: "通行码查询", permissions: ["visitor:appointment:list"] },
  },
  {
    path: "/h5/appointments",
    component: () => import("@/views/h5/appointments"),
    hidden: true,
    meta: { title: "我的预约" },
  },
  {
    path: "/h5/guard",
    component: () => import("@/views/h5/guard"),
    hidden: true,
    meta: { title: "保安核验", permissions: ["visitor:guard:verify"] },
  },
  // ==================== 系统管理 ====================
  {
    path: "/system",
    component: Layout,
    name: "System",
    alwaysShow: true,
    meta: { title: "系统管理", icon: "system", parentMenuId: 1 },
    children: [
      {
        path: "user",
        component: () => import("@/views/system/user/index"),
        name: "User",
        meta: {
          title: "用户管理",
          icon: "people",
          permissions: ["system:user:list"],
        },
      },
      {
        path: "dept",
        component: () => import("@/views/system/dept/index"),
        name: "Dept",
        meta: {
          title: "部门管理",
          icon: "tree",
          permissions: ["system:dept:list"],
        },
      },
      {
        path: "role",
        component: () => import("@/views/system/role/index"),
        name: "Role",
        meta: {
          title: "角色管理",
          icon: "peoples",
          permissions: ["system:role:list"],
        },
      },
      {
        path: "menu",
        component: () => import("@/views/system/menu/index"),
        name: "Menu",
        meta: {
          title: "菜单管理",
          icon: "tree-table",
          permissions: ["system:menu:list"],
        },
      },
    ],
  },
  // ==================== 访客管理 ====================
  {
    path: "/visitor",
    component: Layout,
    name: "Visitor",
    alwaysShow: true,
    meta: { title: "访客管理", icon: "people", parentMenuId: 2004 },
    children: [
      {
        path: "appointment",
        component: () => import("@/views/visitor/appointment/index"),
        name: "VisitorAppointment",
        meta: {
          title: "访客预约",
          icon: "edit",
          permissions: ["visitor:appointment:list"],
        },
      },
      {
        path: "approval",
        component: () => import("@/views/visitor/approval/index"),
        name: "VisitorApproval",
        meta: {
          title: "访客审批",
          icon: "checkbox",
          permissions: ["visitor:approval:list"],
        },
      },
      {
        path: "register",
        component: () => import("@/views/visitor/register/index"),
        name: "VisitorRegister",
        meta: {
          title: "现场登记",
          icon: "form",
          permissions: ["visitor:register:add"],
        },
      },
      {
        path: "log",
        component: () => import("@/views/visitor/log/index"),
        name: "VisitorLog",
        meta: {
          title: "来访记录",
          icon: "list",
          permissions: ["visitor:log:list"],
        },
      },
      {
        path: "deptQrcode",
        component: () => import("@/views/visitor/qrcode/index"),
        name: "VisitorDeptQrcode",
        meta: {
          title: "二维码",
          icon: "qr-code",
          permissions: ["visitor:qrcode:list"],
        },
      },
    ],
  },
  // ==================== 系统管理隐藏路由(分配角色/用户) ====================
  {
    path: "/system",
    component: Layout,
    hidden: true,
    children: [
      {
        path: "user-auth/role/:userId(\\d+)",
        component: () => import("@/views/system/user/authRole"),
        name: "AuthRole",
        meta: {
          title: "分配角色",
          activeMenu: "/system/user",
          permissions: ["system:user:edit"],
        },
      },
      {
        path: "role-auth/user/:roleId(\\d+)",
        component: () => import("@/views/system/role/authUser"),
        name: "AuthUser",
        meta: {
          title: "分配用户",
          activeMenu: "/system/role",
          permissions: ["system:role:edit"],
        },
      },
    ],
  },
];


// 防止连续点击多次路由报错
let routerPush = Router.prototype.push;
let routerReplace = Router.prototype.replace;
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch((err) => err);
};
Router.prototype.replace = function push(location) {
  return routerReplace.call(this, location).catch((err) => err);
};

export default new Router({
  mode: "history",
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes,
});
