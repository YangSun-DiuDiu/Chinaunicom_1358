import request from '@/utils/request'

// 设备列表
export function getDeviceList() {
  return request({ url: '/h5/device/list', method: 'get' })
}

// 会议室列表+当日安排
export function getMeetingRooms() {
  return request({ url: '/h5/meeting/rooms', method: 'get' })
}

// 首页动态聚合
export function getNews() {
  return request({ url: '/h5/news', method: 'get' })
}
