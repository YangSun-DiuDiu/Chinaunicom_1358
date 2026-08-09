import request from '@/utils/request'

// 获取文档目录树
export function getDocTree() {
  return request({
    url: '/doc/tree',
    method: 'get'
  })
}

// 获取文档内容
export function getDocContent(path) {
  return request({
    url: '/doc/content',
    method: 'get',
    params: { path }
  })
}

// 获取同步状态
export function getDocStatus() {
  return request({
    url: '/doc/status',
    method: 'get'
  })
}

// 手动触发同步
export function syncDoc() {
  return request({
    url: '/doc/sync',
    method: 'post'
  })
}

// 搜索文档
export function searchDoc(keyword) {
  return request({
    url: '/doc/search',
    method: 'get',
    params: { keyword }
  })
}
