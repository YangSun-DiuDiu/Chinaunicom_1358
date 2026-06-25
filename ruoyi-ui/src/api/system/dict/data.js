import request from '@/utils/request'

// 字典数据已被精简，保留空实现以兼容历史引用
export function getDicts(dictType) {
  return new Promise(resolve => {
    resolve({ code: 200, data: [] })
  })
}

// 根据字典类型查询字典数据信息
export function getDictsByType(dictType) {
  return getDicts(dictType)
}
