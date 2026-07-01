import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/h5/login', '/h5-register', '/pass', '/repair-complete']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

router.beforeEach((to, from, next) => {
  NProgress.start()
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
