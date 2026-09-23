// 学生端考试接口
import request from './request'

// 可参加的考试列表
export function getAvailableExams() {
  return request({ url: '/exams/available', method: 'get' })
}

// 开始 / 继续考试：返回 { recordId, paper, startTime, remainingSeconds, savedAnswers }
export function startExam(paperId) {
  return request({ url: `/exams/${paperId}/start`, method: 'post' })
}

// 保存答题进度
export function saveExam(recordId, answers) {
  return request({ url: `/exams/${recordId}/save`, method: 'post', data: { answers } })
}

// 交卷：返回 { recordId, totalScore, correctCount, totalCount, details }
export function submitExam(recordId, answers) {
  return request({ url: `/exams/${recordId}/submit`, method: 'post', data: { answers } })
}

// 我的考试记录（分页）
export function getMyRecords(params) {
  return request({ url: '/exams/records', method: 'get', params })
}

// 考试记录详情
export function getRecordDetail(id) {
  return request({ url: `/exams/records/${id}`, method: 'get' })
}
