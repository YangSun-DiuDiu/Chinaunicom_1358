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

// 获取会议仪表盘数据
export function getMeetingDashboard() {
  return request({
    url: '/dashboard/meeting',
    method: 'get'
  })
}

// 获取公寓仪表盘数据
export function getApartmentDashboard() {
  return request({
    url: '/dashboard/apartment',
    method: 'get'
  })
}
