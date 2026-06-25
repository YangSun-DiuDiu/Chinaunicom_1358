import request from '@/utils/request'

/**
 * 短信日志 API
 */

// 查询短信日志列表
export function listSmsLog(query) {
  return request({
    url: '/sms/log/list',
    method: 'get',
    params: query
  })
}

// 查询短信日志详情
export function getSmsLog(smsId) {
  return request({
    url: '/sms/log/' + smsId,
    method: 'get'
  })
}
