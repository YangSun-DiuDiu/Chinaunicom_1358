import request from '@/utils/request'

// 获取设备驾驶舱数据
export function getDeviceDashboard() {
  return request({
    url: '/dashboard/device',
    method: 'get'
  })
}

// 获取访客驾驶舱数据
export function getVisitorDashboard() {
  return request({
    url: '/dashboard/visitor',
    method: 'get'
  })
}
