import request from '@/utils/request'

// 查询会议预约列表
export function getList(query) {
  return request({
    url: '/meeting/booking/list',
    method: 'get',
    params: query
  })
}

// 查询会议预约详情
export function getBooking(bookingId) {
  return request({
    url: '/meeting/booking/' + bookingId,
    method: 'get'
  })
}

// 新增会议预约
export function addBooking(data) {
  return request({
    url: '/meeting/booking',
    method: 'post',
    data: data
  })
}

// 修改会议预约
export function updateBooking(data) {
  return request({
    url: '/meeting/booking',
    method: 'put',
    data: data
  })
}

// 删除会议预约
export function delBooking(bookingIds) {
  return request({
    url: '/meeting/booking/' + bookingIds,
    method: 'delete'
  })
}

// 审批通过
export function approveBooking(bookingId) {
  return request({
    url: '/meeting/booking/approve/' + bookingId,
    method: 'put'
  })
}

// 审批拒绝
export function rejectBooking(bookingId) {
  return request({
    url: '/meeting/booking/reject/' + bookingId,
    method: 'put'
  })
}
