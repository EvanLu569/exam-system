// 成绩分析接口
import request from './request'

// 个人成绩趋势
export function getMyScores() {
  return request({ url: '/stats/my-scores', method: 'get' })
}

// 知识点掌握度
export function getKnowledgeMastery() {
  return request({ url: '/stats/knowledge-mastery', method: 'get' })
}

// 试卷正确率（管理员）
export function getPaperAccuracy(paperId) {
  return request({ url: `/stats/paper/${paperId}/accuracy`, method: 'get' })
}
