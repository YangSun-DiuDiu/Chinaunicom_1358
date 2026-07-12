import request from '@/utils/request'

// 查询网络运行报告列表
export function listReport(query) {
  return request({ url: '/report/list', method: 'get', params: query })
}
// 查询报告详情
export function getReport(reportId) {
  return request({ url: '/report/' + reportId, method: 'get' })
}
// 生成指定周期报告
export function generateReport(reportType) {
  return request({ url: '/report/generate/' + reportType, method: 'post' })
}
// 删除报告
export function delReport(reportId) {
  return request({ url: '/report/' + reportId, method: 'delete' })
}
