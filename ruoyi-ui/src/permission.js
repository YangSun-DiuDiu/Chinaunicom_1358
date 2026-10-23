import router, { staticRoutes } from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/h5/login', '/h5-register', '/h5/entry', '/pass', '/h5/appointments', '/h5/visitor/pass']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

// 动态路由（staticRoutes，含 H5 公开页）默认只在登录后注册；
// 白名单公开页（如「我的预约」「通行码出示」）在未登录时也需要被路由匹配到，
// 否则 addRoutes 从未执行 → 路由 matched 为空 → 白屏。
let dynamicRoutesAdded = false
function ensureStaticRoutes() {
  if (dynamicRoutesAdded) return
  router.addRoutes(staticRoutes)
  dynamicRoutesAdded = true
}

router.beforeEach((to, from, next) => {
  NProgress.start()
  // 白名单公开页（如「我的预约」「通行码出示」）：动态路由未注册时先注册，
  // 再强制重新导航以完成路由匹配（与登录后 GenerateRoutes 的 replace 策略一致）
  if (isWhiteList(to.path) && (!to.matched || to.matched.length === 0)) {
    ensureStaticRoutes()
    return next({ ...to, replace: true })
  }
  if (getToken()) {
    to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
    const isLock = store.getters.isLock
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else if (isWhiteList(to.path)) {
      next()
    } else if (isLock && to.path !== '/lock') {
      next({ path: '/lock' })
      NProgress.done()
    } else if (!isLock && to.path === '/lock') {
      next({ path: '/' })
      NProgress.done()
    } else {
      if (store.getters.permissions.length === 0) {
        isRelogin.show = true
        // 拉取用户信息和权限
        store.dispatch('GetInfo').then(() => {
          isRelogin.show = false
          // 基于用户权限生成静态路由
          store.dispatch('GenerateRoutes', store.getters.permissions).then(accessRoutes => {
            router.addRoutes(accessRoutes)
            next({ ...to, replace: true })
          })
        }).catch(err => {
          store.dispatch('LogOut').then(() => {
            Message.error(err)
            next({ path: '/' })
          })
        })
      } else {
        next()
      }
    }
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      ensureStaticRoutes()
      next()
    } else {
      const loginPath = to.path.startsWith('/h5') ? '/h5/login' : '/login'
      next(loginPath + '?redirect=' + encodeURIComponent(to.fullPath))
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
