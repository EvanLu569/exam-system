// 试卷管理接口
import request from './request'

// 试卷列表（分页）
export function listPapers(params) {
  return request({ url: '/papers', method: 'get', params })
}

// 试卷详情（含题目，管理员）
export function getPaper(id) {
  return request({ url: `/papers/${id}`, method: 'get' })
}

// 创建试卷（手动选题）
export function createPaper(data) {
  return request({ url: '/papers', method: 'post', data })
}

export function updatePaper(id, data) {
  return request({ url: `/papers/${id}`, method: 'put', data })
}

export function deletePaper(id) {
  return request({ url: `/papers/${id}`, method: 'delete' })
}

// 智能组卷
export function generatePaper(data) {
  return request({ url: '/papers/generate', method: 'post', data })
}

// 发布试卷
export function publishPaper(id) {
  return request({ url: `/papers/${id}/publish`, method: 'post' })
}
