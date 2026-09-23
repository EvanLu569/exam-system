import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { startExam, saveExam, submitExam } from '@/api/exam'
import { deserializeAnswer, serializeAnswer } from '@/utils/format'

// 当前考试状态：试卷、答题进度、剩余时间（接口文档 5.3）
export const useExamStore = defineStore('exam', () => {
  const recordId = ref(null)
  const paper = ref(null) // { id, name, durationMinutes, totalScore }
  const questions = ref([]) // 不含正确答案
  const answers = ref({}) // questionId -> UI 值（多选为数组，单选/判断为字符串）
  const remaining = ref(0) // 剩余秒数
  const startedAt = ref(null)

  const answeredCount = computed(
    () => questions.value.filter((q) => isAnsweredValue(answers.value[q.id])).length
  )

  function isAnsweredValue(v) {
    if (Array.isArray(v)) return v.length > 0
    return v !== undefined && v !== null && v !== ''
  }

  // 开始 / 继续考试
  async function start(paperId) {
    const res = await startExam(paperId)
    recordId.value = res.recordId
    paper.value = res.paper
    questions.value = res.paper.questions || []

    // 断线续考：优先用服务端返回的已保存答案，其次本地缓存
    const saved = res.savedAnswers || loadLocal()
    answers.value = {}
    questions.value.forEach((q) => {
      const raw = saved?.[q.id]
      answers.value[q.id] = deserializeAnswer(raw ?? '', q.type)
    })

    remaining.value = Math.max(0, Number(res.remainingSeconds) || 0)
    startedAt.value = Date.now()
    persist()
    return res
  }

  function setAnswer(questionId, value) {
    answers.value[questionId] = value
    persist()
  }

  function getAnswerPayload() {
    return questions.value.map((q) => ({
      questionId: q.id,
      userAnswer: serializeAnswer(answers.value[q.id], q.type)
    }))
  }

  // 异步保存进度（防丢失）
  async function save() {
    if (!recordId.value) return
    await saveExam(recordId.value, getAnswerPayload())
  }

  // 交卷
  async function submit() {
    const res = await submitExam(recordId.value, getAnswerPayload())
    clearLocal()
    return res
  }

  // 本地缓存（刷新恢复兜底）
  function cacheKey() {
    return `exam_answers_${recordId.value}`
  }
  function persist() {
    if (!recordId.value) return
    localStorage.setItem(cacheKey(), JSON.stringify(answers.value))
  }
  function loadLocal() {
    if (!recordId.value) return {}
    const raw = localStorage.getItem(cacheKey())
    if (!raw) return {}
    try {
      return JSON.parse(raw)
    } catch {
      return {}
    }
  }
  function clearLocal() {
    if (recordId.value) localStorage.removeItem(cacheKey())
  }

  function reset() {
    recordId.value = null
    paper.value = null
    questions.value = []
    answers.value = {}
    remaining.value = 0
    startedAt.value = null
  }

  return {
    recordId,
    paper,
    questions,
    answers,
    remaining,
    answeredCount,
    start,
    setAnswer,
    save,
    submit,
    reset
  }
})
