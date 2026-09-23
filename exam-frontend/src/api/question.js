// 题库管理接口（管理员）
import request from './request'

// 分页查询题目：page/size/type/difficulty/keyword
export function listQuestions(params) {
  return request({ url: '/questions', method: 'get', params })
}

export function getQuestion(id) {
  return request({ url: `/questions/${id}`, method: 'get' })
}

export function createQuestion(data) {
  return request({ url: '/questions', method: 'post', data })
}

export function updateQuestion(id, data) {
  return request({ url: `/questions/${id}`, method: 'put', data })
}

export function deleteQuestion(id) {
  return request({ url: `/questions/${id}`, method: 'delete' })
}
