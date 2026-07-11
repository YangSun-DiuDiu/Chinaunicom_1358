import auth from '@/plugins/auth'
import router, { constantRoutes, staticRoutes } from '@/router'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'

const permission = {
  state: {
    routes: [],
    addRoutes: [],
    defaultRoutes: [],
    topbarRouters: [],
    sidebarRouters: []
  },
  mutations: {
    SET_ROUTES: (state, routes) => {
      state.addRoutes = routes
      state.routes = constantRoutes.concat(routes)
    },
    SET_DEFAULT_ROUTES: (state, routes) => {
      state.defaultRoutes = constantRoutes.concat(routes)
    },
    SET_TOPBAR_ROUTES: (state, routes) => {
      state.topbarRouters = routes
    },
    SET_SIDEBAR_ROUTERS: (state, routes) => {
      state.sidebarRouters = routes
    },
  },
  actions: {
    // 静态路由生成 - 基于用户权限过滤
    GenerateRoutes({ commit }, permissions) {
      return new Promise(resolve => {
        // 先深拷贝路由配置（保留 component 函数引用），避免污染原始 staticRoutes
        const routesClone = cloneRoutes(staticRoutes)
        // 深度过滤静态路由，只保留用户有权限的路由
        const filteredRoutes = filterStaticRoutes(routesClone, permissions)
        const sidebarRoutes = JSON.parse(JSON.stringify(filteredRoutes))

        // 处理组件映射（使用保留组件引用的 filteredRoutes，而非 JSON 深拷贝）
        const asyncRoutes = filterAsyncRouter(filteredRoutes)
        asyncRoutes.push({ path: '*', redirect: '/404', hidden: true })

        router.addRoutes(asyncRoutes)
        commit('SET_ROUTES', asyncRoutes)
        commit('SET_SIDEBAR_ROUTERS', constantRoutes.concat(sidebarRoutes))
        commit('SET_DEFAULT_ROUTES', sidebarRoutes)
        commit('SET_TOPBAR_ROUTES', sidebarRoutes)
        resolve(asyncRoutes)
      })
    }
  }
}

// 根据用户权限过滤静态路由
function filterStaticRoutes(routes, permissions) {
  return routes.filter(route => {
    if (route.children) {
      route.children = filterStaticRoutes(route.children, permissions)
      // 如果子路由全部被过滤，则隐藏父路由
      if (route.children.length === 0) {
        return false
      }
    }
    // 检查权限
    if (route.meta && route.meta.permissions) {
      const perms = Array.isArray(route.meta.permissions) ? route.meta.permissions : [route.meta.permissions]
      return auth.hasPermiOr(perms)
    }
    return true
  })
}

// 遍历路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  var children = []
  childrenMap.forEach(el => {
    el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path
    if (el.children && el.children.length && el.component === 'ParentView') {
      children = children.concat(filterChildren(el.children, el))
    } else {
      children.push(el)
    }
  })
  return children
}

// 递归深拷贝路由配置，保留 component 函数引用
function cloneRoutes(routes) {
  return routes.map(route => {
    const cloned = { ...route }
    if (route.children) {
      cloned.children = cloneRoutes(route.children)
    }
    return cloned
  })
}

export const loadView = (view) => {
  if (process.env.NODE_ENV === 'development') {
    return (resolve) => require([`@/views/${view}`], resolve)
  } else {
    return () => import(`@/views/${view}`)
  }
}

export default permission
