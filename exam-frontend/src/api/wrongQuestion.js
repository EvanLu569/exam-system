// 错题本接口
import request from './request'

// 错题列表（分页）
export function listWrongQuestions(params) {
  return request({ url: '/wrong-questions', method: 'get', params })
}

// 今日待复习
export function getReviewToday() {
  return request({ url: '/wrong-questions/review-today', method: 'get' })
}

// 标记已掌握
export function masterWrongQuestion(id) {
  return request({ url: `/wrong-questions/${id}/master`, method: 'post' })
}

// 移除错题
export function removeWrongQuestion(id) {
  return request({ url: `/wrong-questions/${id}`, method: 'delete' })
}
