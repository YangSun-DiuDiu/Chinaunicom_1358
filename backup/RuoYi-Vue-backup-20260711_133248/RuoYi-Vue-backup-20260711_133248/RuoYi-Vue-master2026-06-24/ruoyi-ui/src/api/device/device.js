import request from '@/utils/request'

// 查询设备列表
export function listDevice(query) {
  return request({
    url: '/device/list',
    method: 'get',
    params: query
  })
}

// 查询设备详情
export function getDevice(deviceId) {
  return request({
    url: '/device/' + deviceId,
    method: 'get'
  })
}

// 新增设备
export function addDevice(data) {
  return request({
    url: '/device',
    method: 'post',
    data: data
  })
}

// 修改设备
export function updateDevice(data) {
  return request({
    url: '/device',
    method: 'put',
    data: data
  })
}

// 删除设备
export function delDevice(deviceIds) {
  return request({
    url: '/device/' + deviceIds,
    method: 'delete'
  })
}

// 导出设备
export function exportDevice(query) {
  return request({
    url: '/device/export',
    method: 'post',
    params: query
  })
}

// 报修-发送短信
export function repairDevice(deviceId) {
  return request({
    url: '/device/repair/' + deviceId,
    method: 'post'
  })
}

// 获取设备拓扑树
export function getDeviceTopology() {
  return request({
    url: '/device/topology',
    method: 'get'
  })
}

// ===== 设备端口 =====

// 查询设备端口列表
export function listDevicePort(query) {
  return request({
    url: '/device/port/list',
    method: 'get',
    params: query
  })
}

// 新增端口
export function addDevicePort(data) {
  return request({
    url: '/device/port',
    method: 'post',
    data: data
  })
}

// 修改端口
export function updateDevicePort(data) {
  return request({
    url: '/device/port',
    method: 'put',
    data: data
  })
}

// 删除端口
export function delDevicePort(portIds) {
  return request({
    url: '/device/port/' + portIds,
    method: 'delete'
  })
}

// 绑定端口
export function bindPorts(data) {
  return request({
    url: '/device/port/bind',
    method: 'post',
    data: data
  })
}

// ===== 设备状态日志 =====

export function listStatusLog(query) {
  return request({
    url: '/device/statuslog/list',
    method: 'get',
    params: query
  })
}

// ===== 心跳日志 =====

export function listHeartbeatLog(query) {
  return request({
    url: '/device/heartbeat/list',
    method: 'get',
    params: query
  })
}
