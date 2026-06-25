import request from '@/utils/request'

// ===== 访客预约 =====

// 查询预约列表
export function listAppointment(query) {
  return request({
    url: '/visitor/appointment/list',
    method: 'get',
    params: query
  })
}

// 查询预约详情
export function getAppointment(appointmentId) {
  return request({
    url: '/visitor/appointment/' + appointmentId,
    method: 'get'
  })
}

// 新增预约
export function addAppointment(data) {
  return request({
    url: '/visitor/appointment',
    method: 'post',
    data: data
  })
}

// 修改预约
export function updateAppointment(data) {
  return request({
    url: '/visitor/appointment',
    method: 'put',
    data: data
  })
}

// 删除预约
export function delAppointment(appointmentIds) {
  return request({
    url: '/visitor/appointment/' + appointmentIds,
    method: 'delete'
  })
}

// 审批预约
export function approveAppointment(data) {
  return request({
    url: '/visitor/appointment/approve',
    method: 'put',
    data: data
  })
}

// 取消预约
export function cancelAppointment(appointmentId) {
  return request({
    url: '/visitor/appointment/cancel/' + appointmentId,
    method: 'put'
  })
}

// 完成来访
export function completeAppointment(appointmentId) {
  return request({
    url: '/visitor/appointment/complete/' + appointmentId,
    method: 'put'
  })
}

// 待审批列表
export function pendingAppointment() {
  return request({
    url: '/visitor/appointment/pending',
    method: 'get'
  })
}

// ===== 来访记录 =====

// 查询来访记录列表
export function listVisitorLog(query) {
  return request({
    url: '/visitor/log/list',
    method: 'get',
    params: query
  })
}

// 查询来访记录详情
export function getVisitorLog(logId) {
  return request({
    url: '/visitor/log/' + logId,
    method: 'get'
  })
}

// 现场登记
export function registerVisitor(data) {
  return request({
    url: '/visitor/log',
    method: 'post',
    data: data
  })
}

// 记录离开
export function visitorExit(logId) {
  return request({
    url: '/visitor/log/exit/' + logId,
    method: 'put'
  })
}

// 导出来访记录
export function exportVisitorLog(query) {
  return request({
    url: '/visitor/log/export',
    method: 'post',
    params: query
  })
}
