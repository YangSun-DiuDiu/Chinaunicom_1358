import Layout from '@/layout'

const h5Routes = [
  {
    path: '/h5',
    component: Layout,
    hidden: true,
    children: [
      { path: 'login',    component: () => import('@/views/h5/login.vue'),    name: 'H5Login',    meta: { title: 'H5登录' } },
      { path: '',         component: () => import('@/views/h5/index.vue'),    name: 'H5Index',    meta: { title: '园区服务' } },
      { path: 'visitor',  component: () => import('@/views/h5/visitor.vue'), name: 'H5Visitor',  meta: { title: '访客登记' } },
      { path: 'visitor/pass', component: () => import('@/views/h5/visitor-pass.vue'), name: 'H5VisitorPass', meta: { title: '通行码查询' } },
      { path: 'guard',    component: () => import('@/views/h5/guard.vue'),   name: 'H5Guard',    meta: { title: '保安核验', requireAuth: true, permission: 'visitor:guard:verify' } },
      { path: 'meeting',  component: () => import('@/views/h5/meeting.vue'),  name: 'H5Meeting',  meta: { title: '会议室' } },
      { path: 'meeting/book', component: () => import('@/views/h5/meeting-book.vue'), name: 'H5MeetingBook', meta: { title: '预约会议' } },
      { path: 'device',   component: () => import('@/views/h5/device.vue'),  name: 'H5Device',   meta: { title: '设备状态' } },
    ]
  }
]

export default h5Routes
