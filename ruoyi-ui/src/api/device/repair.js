import request from '@/utils/request'

// 查询维修工单列表
export function listRepair(query) {
  return request({ url: '/device/repair/list', method: 'get', params: query })
}
// 创建维修工单(替代旧的报修)
export function createRepair(deviceId) {
  return request({ url: '/device/repair/create/' + deviceId, method: 'post' })
}
// 转派
export function transferRepair(repairId, data) {
  return request({ url: '/device/repair/transfer/' + repairId, method: 'post', data })
}
// 接收
export function acceptRepair(repairId) {
  return request({ url: '/device/repair/accept/' + repairId, method: 'post' })
}
// 拒绝
export function rejectRepair(repairId, reason) {
  return request({ url: '/device/repair/reject/' + repairId, method: 'post', data: { reason } })
}
// 工作量统计
export function workloadStats() {
  return request({ url: '/device/repair/workload', method: 'get' })
}
