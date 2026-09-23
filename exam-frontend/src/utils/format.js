// 题型 / 难度 / 试卷状态 常量与格式化工具

export const QUESTION_TYPES = [
  { value: 1, label: '单选题' },
  { value: 2, label: '多选题' },
  { value: 3, label: '判断题' }
]

export const DIFFICULTIES = [
  { value: 1, label: '简单' },
  { value: 2, label: '中等' },
  { value: 3, label: '困难' }
]

export const PAPER_STATUS = [
  { value: 0, label: '未发布' },
  { value: 1, label: '已发布' },
  { value: 2, label: '已结束' }
]

// 选项字母：0 -> A, 1 -> B ...
export function optionLetter(index) {
  return String.fromCharCode(65 + index)
}

export function typeLabel(type) {
  return QUESTION_TYPES.find((t) => t.value === type)?.label || '未知'
}

export function difficultyLabel(d) {
  return DIFFICULTIES.find((x) => x.value === d)?.label || '未知'
}

export function difficultyType(d) {
  return ['', 'success', 'warning', 'danger'][d] || 'info'
}

export function statusLabel(status) {
  return PAPER_STATUS.find((s) => s.value === status)?.label || '未知'
}

export function statusType(status) {
  return ['info', 'success', 'danger'][status] || 'info'
}

// 将答案（字符串 "ABC" / "A,B,C" / 数组）拆成字母数组
export function toLetters(answer) {
  if (Array.isArray(answer)) {
    return answer.map((x) => String(x).trim().toUpperCase()).filter(Boolean)
  }
  return String(answer ?? '')
    .split(/[,，\s]+/)
    .filter(Boolean)
    .flatMap((s) => s.toUpperCase().split(''))
    .filter(Boolean)
}

// 答案展示：单选 "A"、多选 "A、B、C"、判断 T/F -> 正确/错误
export function answerText(answer, type) {
  if (answer === null || answer === undefined || answer === '') return ''
  if (type === 3) {
    const a = String(answer).toUpperCase()
    return a === 'T' ? '正确' : a === 'F' ? '错误' : String(answer)
  }
  return toLetters(answer).join('、')
}

// 提交到后端：多选去重排序拼接成 "ABC"，单选/判断原样返回字符串
export function serializeAnswer(answer, type) {
  if (type === 2) {
    return [...new Set(toLetters(answer))].sort().join('')
  }
  return answer === null || answer === undefined ? '' : String(answer)
}

// 从后端还原到 UI：多选转为数组，单选/判断返回字符串
export function deserializeAnswer(raw, type) {
  if (type === 2) return toLetters(raw)
  return raw === null || raw === undefined ? '' : String(raw)
}

// 判断答案是否为空
export function isAnswered(value) {
  if (Array.isArray(value)) return value.length > 0
  return value !== undefined && value !== null && value !== ''
}

// 解析 "YYYY-MM-DDTHH:mm:ss" 或 "YYYY-MM-DD HH:mm:ss"，避免浏览器时区差异
export function parseDateTime(s) {
  if (!s) return null
  if (s instanceof Date) return s
  const m = String(s).trim().match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/)
  if (!m) return null
  return new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +(m[6] || 0))
}

// 格式化为 "YYYY-MM-DD HH:mm"
export function formatDateTime(s) {
  const d = parseDateTime(s)
  if (!d) return s ? String(s) : '-'
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

// 秒数 -> "HH:mm:ss"
export function formatSeconds(sec) {
  const s = Math.max(0, Math.floor(sec))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const r = s % 60
  const p = (n) => String(n).padStart(2, '0')
  return `${p(h)}:${p(m)}:${p(r)}`
}
