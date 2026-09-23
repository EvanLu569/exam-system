// Mock 请求引擎：模拟后端 REST 接口，接口路径与接口文档 v1.0 完全一致（前缀 /api）。
// 通过环境变量 VITE_USE_MOCK 控制是否启用（默认启用）。设为 false 即切换到真实 axios 请求。
// 各处理函数 resolve 到「统一返回格式」中的 data；错误通过携带 code 的 Error 抛出。

import { getDB, saveDB, nextId, resetDB } from './db'
import { getToken } from '@/utils/auth'

export const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

const sleep = (ms = 180) => new Promise((r) => setTimeout(r, ms))

// ISO 时间 "YYYY-MM-DDTHH:mm:ss"
function nowISO() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function parseDT(s) {
  if (!s) return null
  const m = String(s).match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/)
  if (!m) return null
  return new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +(m[6] || 0))
}

function err(code, message) {
  const e = new Error(message)
  e.code = code
  return e
}

function safeUser(u) {
  return { id: u.id, username: u.username, role: u.role, createTime: u.createTime }
}

function currentUser() {
  const token = getToken()
  const m = token && token.match(/^mock-token-(\d+)$/)
  if (!m) return null
  return getDB().users.find((u) => u.id === Number(m[1])) || null
}

function requireAuth() {
  const u = currentUser()
  if (!u) throw err(401, '登录已过期，请重新登录')
  return u
}

function requireAdmin() {
  const u = requireAuth()
  if (u.role !== 'admin') throw err(403, '无权限访问')
  return u
}

function parseUrl(url) {
  const [path, qs = ''] = url.split('?')
  const query = {}
  qs.split('&')
    .filter(Boolean)
    .forEach((kv) => {
      const i = kv.indexOf('=')
      const k = i >= 0 ? kv.slice(0, i) : kv
      const v = i >= 0 ? kv.slice(i + 1) : ''
      query[decodeURIComponent(k)] = decodeURIComponent(v)
    })
  return { path, query }
}

function matchPattern(pattern, path) {
  const pp = pattern.split('/').filter(Boolean)
  const ap = path.split('/').filter(Boolean)
  if (pp.length !== ap.length) return null
  const params = {}
  for (let i = 0; i < pp.length; i++) {
    if (pp[i][0] === ':') params[pp[i].slice(1)] = decodeURIComponent(ap[i])
    else if (pp[i] !== ap[i]) return null
  }
  return params
}

function paginate(list, page, size) {
  const start = (page - 1) * size
  return { total: list.length, list: list.slice(start, start + size) }
}

// 答案拆分为字母集合（去重、大写、排序）
function letters(ans) {
  return [...new Set(String(ans ?? '').split(/[,，\s]+/).filter(Boolean).flatMap((s) => s.toUpperCase().split('')))]
    .filter(Boolean)
    .sort()
}

function normalize(ans) {
  return letters(ans).join('')
}

// 判卷：单选/判断全对得分；多选「少选得部分分，错选不得分」
function grade(q, userAnswer, score) {
  if (q.type === 2) {
    const correct = letters(q.correctAnswer)
    const user = letters(userAnswer)
    if (user.length === 0) return { score: 0, isCorrect: false }
    const hasWrong = user.some((l) => !correct.includes(l))
    if (hasWrong) return { score: 0, isCorrect: false }
    const sc = Math.round(score * (user.length / correct.length) * 10) / 10
    return { score: sc, isCorrect: user.length === correct.length }
  }
  const ok = normalize(userAnswer) === normalize(q.correctAnswer)
  return { score: ok ? score : 0, isCorrect: ok }
}

function sumScore(db, paperId) {
  return db.paperQuestions
    .filter((pp) => pp.paperId === paperId)
    .reduce((s, pp) => s + (pp.score || 0), 0)
}

function paperQuestionsOf(db, paperId) {
  return db.paperQuestions.filter((pp) => pp.paperId === paperId).sort((a, b) => a.sortOrder - b.sortOrder)
}

function paperSummary(db, p) {
  return {
    id: p.id,
    name: p.name,
    durationMinutes: p.durationMinutes,
    totalScore: sumScore(db, p.id),
    status: p.status,
    startTime: p.startTime,
    endTime: p.endTime,
    questionCount: paperQuestionsOf(db, p.id).length,
    createTime: p.createTime
  }
}

// 艾宾浩斯复习间隔（天）：1 / 2 / 4 / 7 / 15
function reviewAfter(wrongCount) {
  const intervals = [1, 2, 4, 7, 15]
  const days = intervals[Math.min(wrongCount - 1, intervals.length - 1)]
  const d = new Date(Date.now() + days * 24 * 3600 * 1000)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T00:00:00`
}

function upsertWrongQuestion(db, userId, questionId) {
  const w = db.wrongQuestions.find((x) => x.userId === userId && x.questionId === questionId)
  if (w) {
    w.wrongCount += 1
    w.lastWrongTime = nowISO()
    w.nextReviewTime = reviewAfter(w.wrongCount)
    w.mastered = 0
  } else {
    db.wrongQuestions.push({
      id: nextId(db.wrongQuestions),
      userId,
      questionId,
      wrongCount: 1,
      lastWrongTime: nowISO(),
      nextReviewTime: reviewAfter(1),
      mastered: 0
    })
  }
}

function wrongToVO(db, w) {
  const q = db.questions.find((x) => x.id === w.questionId)
  if (!q) return null
  return {
    id: w.id,
    questionId: q.id,
    content: q.content,
    options: q.options,
    type: q.type,
    correctAnswer: q.correctAnswer,
    wrongCount: w.wrongCount,
    lastWrongTime: w.lastWrongTime,
    nextReviewTime: w.nextReviewTime,
    mastered: w.mastered
  }
}

// ---------- 认证 ----------
function handleLogin(body) {
  const db = getDB()
  const user = db.users.find((u) => u.username === body.username && u.password === body.password)
  if (!user) throw err(1001, '用户名或密码错误')
  return { id: user.id, username: user.username, role: user.role, token: `mock-token-${user.id}` }
}

function handleRegister(body) {
  const db = getDB()
  if (!body.username || !body.password) throw err(400, '用户名和密码不能为空')
  if (db.users.some((u) => u.username === body.username)) throw err(1002, '用户名已存在')
  const user = {
    id: nextId(db.users),
    username: body.username,
    password: body.password,
    role: 'student',
    createTime: nowISO()
  }
  db.users.push(user)
  saveDB(db)
  return null
}

// ---------- 题库（管理员） ----------
function handleQuestionList(q) {
  const db = getDB()
  const page = Number(q.page) || 1
  const size = Number(q.size) || 10
  let list = [...db.questions]
  if (q.type) list = list.filter((x) => x.type === Number(q.type))
  if (q.difficulty) list = list.filter((x) => x.difficulty === Number(q.difficulty))
  if (q.keyword) list = list.filter((x) => (x.content || '').includes(q.keyword))
  list.sort((a, b) => b.id - a.id)
  return paginate(list, page, size)
}

function handleQuestionCreate(body) {
  const db = getDB()
  const q = {
    id: nextId(db.questions),
    content: body.content,
    options: body.options || null,
    correctAnswer: body.correctAnswer,
    score: body.score ?? 5,
    type: body.type ?? 1,
    difficulty: body.difficulty ?? 1,
    knowledgePoint: body.knowledgePoint || '',
    createTime: nowISO()
  }
  db.questions.push(q)
  saveDB(db)
  return { id: q.id }
}

function handleQuestionItem(method, id, body) {
  const db = getDB()
  const idx = db.questions.findIndex((x) => x.id === id)
  if (idx < 0) throw err(404, '题目不存在')
  if (method === 'get') return db.questions[idx]
  if (method === 'put') {
    db.questions[idx] = { ...db.questions[idx], ...body, id }
    saveDB(db)
    return db.questions[idx]
  }
  if (method === 'delete') {
    db.questions.splice(idx, 1)
    db.paperQuestions = db.paperQuestions.filter((pp) => pp.questionId !== id)
    db.answerRecords = db.answerRecords.filter((a) => a.questionId !== id)
    db.wrongQuestions = db.wrongQuestions.filter((w) => w.questionId !== id)
    saveDB(db)
    return null
  }
  throw err(405, '不支持的请求方法')
}

// ---------- 试卷 ----------
function handlePaperList(q) {
  const db = getDB()
  const page = Number(q.page) || 1
  const size = Number(q.size) || 10
  const list = db.papers.map((p) => paperSummary(db, p)).sort((a, b) => b.id - a.id)
  return paginate(list, page, size)
}

function handlePaperDetail(id) {
  const db = getDB()
  const p = db.papers.find((x) => x.id === id)
  if (!p) throw err(404, '试卷不存在')
  const questions = paperQuestionsOf(db, id)
    .map((pp) => {
      const q = db.questions.find((x) => x.id === pp.questionId)
      if (!q) return null
      return {
        id: q.id,
        content: q.content,
        options: q.options,
        correctAnswer: q.correctAnswer,
        score: pp.score,
        type: q.type,
        difficulty: q.difficulty,
        knowledgePoint: q.knowledgePoint,
        sortOrder: pp.sortOrder
      }
    })
    .filter(Boolean)
  return {
    id: p.id,
    name: p.name,
    durationMinutes: p.durationMinutes,
    totalScore: sumScore(db, id),
    startTime: p.startTime,
    endTime: p.endTime,
    status: p.status,
    questions
  }
}

function handlePaperCreate(body) {
  const db = getDB()
  const paper = {
    id: nextId(db.papers),
    name: body.name,
    durationMinutes: body.durationMinutes,
    totalScore: body.totalScore ?? 100,
    startTime: body.startTime || null,
    endTime: body.endTime || null,
    status: body.status ?? 0,
    createTime: nowISO()
  }
  db.papers.push(paper)
  ;(body.questions || []).forEach((item, i) => {
    db.paperQuestions.push({
      id: nextId(db.paperQuestions),
      paperId: paper.id,
      questionId: item.questionId,
      score: item.score ?? 5,
      sortOrder: item.sortOrder ?? i + 1
    })
  })
  saveDB(db)
  return { id: paper.id }
}

function handlePaperUpdate(id, body) {
  const db = getDB()
  const idx = db.papers.findIndex((x) => x.id === id)
  if (idx < 0) throw err(404, '试卷不存在')
  const p = db.papers[idx]
  db.papers[idx] = {
    ...p,
    name: body.name ?? p.name,
    durationMinutes: body.durationMinutes ?? p.durationMinutes,
    startTime: body.startTime !== undefined ? body.startTime : p.startTime,
    endTime: body.endTime !== undefined ? body.endTime : p.endTime,
    status: body.status ?? p.status,
    id
  }
  // 若提交了 questions，则重建题目关联
  if (body.questions) {
    db.paperQuestions = db.paperQuestions.filter((pp) => pp.paperId !== id)
    body.questions.forEach((item, i) => {
      db.paperQuestions.push({
        id: nextId(db.paperQuestions),
        paperId: id,
        questionId: item.questionId,
        score: item.score ?? 5,
        sortOrder: item.sortOrder ?? i + 1
      })
    })
  }
  saveDB(db)
  return db.papers[idx]
}

function handlePaperDelete(id) {
  const db = getDB()
  const idx = db.papers.findIndex((x) => x.id === id)
  if (idx < 0) throw err(404, '试卷不存在')
  const recordIds = new Set(db.records.filter((r) => r.paperId === id).map((r) => r.id))
  db.papers.splice(idx, 1)
  db.paperQuestions = db.paperQuestions.filter((pp) => pp.paperId !== id)
  db.records = db.records.filter((r) => r.paperId !== id)
  db.answerRecords = db.answerRecords.filter((a) => !recordIds.has(a.recordId))
  saveDB(db)
  return null
}

// 智能组卷：按题型 / 难度 / 知识点多维约束抽题
function pickByDifficulty(candidates, need, diffDist) {
  const selected = []
  const used = new Set()
  Object.entries(diffDist || {})
    .sort((a, b) => Number(b[1]) - Number(a[1]))
    .forEach(([dStr, cnt]) => {
      const d = Number(dStr)
      const want = Number(cnt)
      const pool = candidates.filter((q) => q.difficulty === d && !used.has(q.id))
      for (let i = 0; i < want && i < pool.length; i++) {
        selected.push(pool[i])
        used.add(pool[i].id)
      }
    })
  if (selected.length < need) {
    const rest = candidates.filter((q) => !used.has(q.id))
    for (let i = 0; selected.length < need && i < rest.length; i++) {
      selected.push(rest[i])
      used.add(rest[i].id)
    }
  }
  return selected
}

function handlePaperGenerate(body) {
  const db = getDB()
  const rules = body.rules || {}
  const typeDist = rules.typeDistribution || {}
  const diffDist = rules.difficultyDistribution || {}
  const kps = rules.knowledgePoints || []

  const pool = db.questions.filter((q) => (kps.length ? kps.includes(q.knowledgePoint) : true))

  const picked = []
  const used = new Set()
  for (const [typeStr, count] of Object.entries(typeDist)) {
    const type = Number(typeStr)
    const need = Number(count)
    const candidates = pool.filter((q) => q.type === type)
    const sel = pickByDifficulty(candidates, need, diffDist)
    sel.forEach((q) => {
      if (!used.has(q.id)) {
        picked.push(q)
        used.add(q.id)
      }
    })
  }
  // 未指定题型分布时，按难度分布抽取
  if (!Object.keys(typeDist).length && Object.keys(diffDist).length) {
    const need = Object.values(diffDist).reduce((s, n) => s + Number(n), 0)
    pickByDifficulty(pool, need, diffDist).forEach((q) => {
      if (!used.has(q.id)) {
        picked.push(q)
        used.add(q.id)
      }
    })
  }

  if (!picked.length) throw err(400, '题库题目不足，无法组卷')

  const paper = {
    id: nextId(db.papers),
    name: body.name,
    durationMinutes: body.durationMinutes,
    totalScore: body.totalScore ?? picked.reduce((s, q) => s + q.score, 0),
    startTime: body.startTime || null,
    endTime: body.endTime || null,
    status: 0,
    createTime: nowISO()
  }
  db.papers.push(paper)
  picked.forEach((q, i) => {
    db.paperQuestions.push({
      id: nextId(db.paperQuestions),
      paperId: paper.id,
      questionId: q.id,
      score: q.score,
      sortOrder: i + 1
    })
  })
  saveDB(db)

  return {
    id: paper.id,
    name: paper.name,
    questions: picked.map((q) => ({
      id: q.id,
      content: q.content,
      type: q.type,
      difficulty: q.difficulty,
      knowledgePoint: q.knowledgePoint,
      score: q.score
    }))
  }
}

function handlePaperPublish(id) {
  const db = getDB()
  const p = db.papers.find((x) => x.id === id)
  if (!p) throw err(404, '试卷不存在')
  if (!paperQuestionsOf(db, id).length) throw err(400, '试卷暂无题目，无法发布')
  p.status = 1
  saveDB(db)
  return null
}

// ---------- 考试 ----------
function handleAvailable(me) {
  const db = getDB()
  const nowMs = Date.now()
  return db.papers
    .filter((p) => {
      if (p.status !== 1) return false
      if (p.startTime && parseDT(p.startTime).getTime() > nowMs) return false
      if (p.endTime && parseDT(p.endTime).getTime() < nowMs) return false
      return true
    })
    .map((p) => {
      const my = db.records.filter((r) => r.paperId === p.id && r.userId === me.id)
      const submitted = my.some((r) => r.status === 1)
      const inProgress = my.some((r) => r.status === 0)
      return {
        id: p.id,
        name: p.name,
        durationMinutes: p.durationMinutes,
        totalScore: sumScore(db, p.id),
        startTime: p.startTime,
        endTime: p.endTime,
        status: p.status,
        joined: submitted,
        submitted,
        inProgress
      }
    })
    .sort((a, b) => a.id - b.id)
}

function loadSavedAnswers(record) {
  return record.answers || {}
}

function handleStart(me, paperId) {
  const db = getDB()
  const paper = db.papers.find((p) => p.id === paperId)
  if (!paper) throw err(404, '试卷不存在')
  if (paper.status !== 1) throw err(paper.status === 0 ? 2001 : 2002, paper.status === 0 ? '考试未开始' : '考试已结束')

  const nowMs = Date.now()
  if (paper.startTime && parseDT(paper.startTime).getTime() > nowMs) throw err(2001, '考试未开始')
  if (paper.endTime && parseDT(paper.endTime).getTime() < nowMs) throw err(2002, '考试已结束')

  // 断线续考：复用进行中的记录
  let record = db.records.find((r) => r.paperId === paperId && r.userId === me.id && r.status === 0)
  if (!record) {
    if (db.records.some((r) => r.paperId === paperId && r.userId === me.id && r.status === 1)) {
      throw err(2003, '已交卷，不能重复交')
    }
    record = { id: nextId(db.records), paperId, userId: me.id, totalScore: 0, startTime: nowISO(), submitTime: null, status: 0, answers: {} }
    db.records.push(record)
    saveDB(db)
  }

  const questions = paperQuestionsOf(db, paperId)
    .map((pp) => {
      const q = db.questions.find((x) => x.id === pp.questionId)
      if (!q) return null
      return { id: q.id, content: q.content, options: q.options, score: pp.score, type: q.type, sortOrder: pp.sortOrder }
    })
    .filter(Boolean)

  // 剩余秒数：受时长与试卷截止时间双重约束
  const elapsedMs = nowMs - parseDT(record.startTime).getTime()
  const durationMs = paper.durationMinutes * 60 * 1000
  let remaining = Math.max(0, Math.floor((durationMs - elapsedMs) / 1000))
  if (paper.endTime) {
    remaining = Math.min(remaining, Math.max(0, Math.floor((parseDT(paper.endTime).getTime() - nowMs) / 1000)))
  }

  return {
    recordId: record.id,
    paper: {
      id: paper.id,
      name: paper.name,
      durationMinutes: paper.durationMinutes,
      totalScore: sumScore(db, paper.id),
      questions
    },
    startTime: record.startTime,
    remainingSeconds: remaining,
    savedAnswers: loadSavedAnswers(record)
  }
}

function findOwnRecord(me, recordId) {
  const db = getDB()
  const record = db.records.find((r) => r.id === recordId && r.userId === me.id)
  if (!record) throw err(404, '考试记录不存在')
  return { db, record }
}

function handleSave(me, recordId, body) {
  const { db, record } = findOwnRecord(me, recordId)
  if (record.status === 1) throw err(2003, '已交卷，不能重复交')
  const map = {}
  ;(body.answers || []).forEach((a) => {
    if (a.questionId != null) map[a.questionId] = a.userAnswer ?? ''
  })
  record.answers = map
  saveDB(db)
  return null
}

function handleSubmit(me, recordId, body) {
  const { db, record } = findOwnRecord(me, recordId)
  if (record.status === 1) throw err(2003, '已交卷，不能重复交')

  const answerMap = {}
  ;(body.answers || []).forEach((a) => {
    answerMap[a.questionId] = a.userAnswer ?? ''
  })

  let total = 0
  let correctCount = 0
  const details = []
  const wrongs = []

  paperQuestionsOf(db, record.paperId).forEach((pp) => {
    const q = db.questions.find((x) => x.id === pp.questionId)
    if (!q) return
    const userAnswer = answerMap[q.id] ?? ''
    const { score, isCorrect } = grade(q, userAnswer, pp.score)
    total += score
    if (isCorrect) correctCount += 1
    else if (normalize(userAnswer) !== '') wrongs.push(q.id)
    details.push({
      questionId: q.id,
      content: q.content,
      options: q.options,
      type: q.type,
      userAnswer,
      correctAnswer: q.correctAnswer,
      isCorrect,
      score
    })
  })

  record.status = 1
  record.submitTime = nowISO()
  record.totalScore = Math.round(total * 10) / 10
  record.answers = answerMap
  saveDB(db)

  // 写入答题明细
  db.answerRecords = db.answerRecords.filter((a) => a.recordId !== recordId)
  details.forEach((d) => {
    db.answerRecords.push({
      id: nextId(db.answerRecords),
      recordId,
      questionId: d.questionId,
      userAnswer: d.userAnswer,
      isCorrect: d.isCorrect ? 1 : 0,
      score: d.score
    })
  })

  // 错题本自动收录
  wrongs.forEach((qid) => upsertWrongQuestion(db, me.id, qid))
  saveDB(db)

  return {
    recordId,
    totalScore: record.totalScore,
    correctCount,
    totalCount: details.length,
    details
  }
}

function handleMyRecords(me, q) {
  const db = getDB()
  const page = Number(q.page) || 1
  const size = Number(q.size) || 10
  const list = db.records
    .filter((r) => r.userId === me.id)
    .map((r) => ({
      id: r.id,
      paperId: r.paperId,
      paperName: db.papers.find((p) => p.id === r.paperId)?.name || '已删除试卷',
      totalScore: r.totalScore,
      startTime: r.startTime,
      submitTime: r.submitTime,
      status: r.status
    }))
    .sort((a, b) => b.id - a.id)
  return paginate(list, page, size)
}

function handleRecordDetail(me, id) {
  const db = getDB()
  const record = db.records.find((r) => r.id === id)
  if (!record) throw err(404, '记录不存在')
  if (me.role !== 'admin' && record.userId !== me.id) throw err(403, '无权限')
  const paper = db.papers.find((p) => p.id === record.paperId)
  const answers = db.answerRecords.filter((a) => a.recordId === id)
  const questions = answers
    .map((a) => {
      const q = db.questions.find((x) => x.id === a.questionId)
      if (!q) return null
      return {
        questionId: q.id,
        content: q.content,
        options: q.options,
        type: q.type,
        userAnswer: a.userAnswer,
        correctAnswer: q.correctAnswer,
        isCorrect: !!a.isCorrect,
        score: a.score
      }
    })
    .filter(Boolean)
  return {
    id: record.id,
    paperId: record.paperId,
    paperName: paper?.name || '已删除试卷',
    paperTotalScore: paper ? sumScore(db, paper.id) : 0,
    totalScore: record.totalScore,
    correctCount: questions.filter((x) => x.isCorrect).length,
    totalCount: questions.length,
    startTime: record.startTime,
    submitTime: record.submitTime,
    status: record.status,
    questions
  }
}

// ---------- 错题本 ----------
function handleWrongList(me, q) {
  const db = getDB()
  const page = Number(q.page) || 1
  const size = Number(q.size) || 10
  const list = db.wrongQuestions
    .filter((w) => w.userId === me.id && !w.mastered)
    .map((w) => wrongToVO(db, w))
    .filter(Boolean)
    .sort((a, b) => b.wrongCount - a.wrongCount || (a.nextReviewTime < b.nextReviewTime ? -1 : 1))
  return paginate(list, page, size)
}

function handleReviewToday(me) {
  const db = getDB()
  const nowMs = Date.now()
  return db.wrongQuestions
    .filter((w) => w.userId === me.id && !w.mastered && w.nextReviewTime && parseDT(w.nextReviewTime).getTime() <= nowMs)
    .map((w) => wrongToVO(db, w))
    .filter(Boolean)
}

function handleWrongMaster(me, id) {
  const db = getDB()
  const w = db.wrongQuestions.find((x) => x.id === id && x.userId === me.id)
  if (!w) throw err(404, '错题不存在')
  w.mastered = 1
  saveDB(db)
  return null
}

function handleWrongDelete(me, id) {
  const db = getDB()
  const idx = db.wrongQuestions.findIndex((x) => x.id === id && x.userId === me.id)
  if (idx < 0) throw err(404, '错题不存在')
  db.wrongQuestions.splice(idx, 1)
  saveDB(db)
  return null
}

// ---------- 统计 ----------
function handleMyScores(me) {
  const db = getDB()
  return db.records
    .filter((r) => r.userId === me.id && r.status === 1)
    .map((r) => {
      const p = db.papers.find((x) => x.id === r.paperId)
      return {
        paperName: p?.name || '已删除试卷',
        score: r.totalScore,
        date: (r.submitTime || r.startTime || '').slice(0, 10)
      }
    })
    .sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : 0))
}

function handleKnowledgeMastery(me) {
  const db = getDB()
  const myRecordIds = db.records.filter((r) => r.userId === me.id && r.status === 1).map((r) => r.id)
  const byKp = {}
  db.answerRecords
    .filter((a) => myRecordIds.includes(a.recordId))
    .forEach((a) => {
      const q = db.questions.find((x) => x.id === a.questionId)
      if (!q || !q.knowledgePoint) return
      if (!byKp[q.knowledgePoint]) byKp[q.knowledgePoint] = { total: 0, correct: 0 }
      byKp[q.knowledgePoint].total += 1
      if (a.isCorrect) byKp[q.knowledgePoint].correct += 1
    })
  return Object.entries(byKp).map(([knowledgePoint, v]) => ({
    knowledgePoint,
    total: v.total,
    correct: v.correct,
    masteryRate: v.total ? Math.round((v.correct / v.total) * 100) / 100 : 0
  }))
}

function handlePaperAccuracy(paperId) {
  const db = getDB()
  const recordIds = db.records.filter((r) => r.paperId === paperId && r.status === 1).map((r) => r.id)
  return paperQuestionsOf(db, paperId)
    .map((pp) => {
      const q = db.questions.find((x) => x.id === pp.questionId)
      if (!q) return null
      const relevant = db.answerRecords.filter((a) => recordIds.includes(a.recordId) && a.questionId === q.id)
      const totalCount = relevant.length
      const correctCount = relevant.filter((a) => a.isCorrect).length
      return {
        questionId: q.id,
        content: q.content,
        totalCount,
        correctCount,
        accuracyRate: totalCount ? Math.round((correctCount / totalCount) * 100) / 100 : 0
      }
    })
    .filter(Boolean)
}

// ---------- 主分发 ----------
export async function mockRequest({ method = 'get', url = '', data, params }) {
  await sleep()
  method = method.toLowerCase()
  const { path, query } = parseUrl(url)
  const q = params && Object.keys(params).length ? params : query
  const db = getDB()
  let m

  // 认证
  if (method === 'post' && path === '/api/auth/login') return handleLogin(data)
  if (method === 'post' && path === '/api/auth/register') return handleRegister(data)
  if (method === 'get' && path === '/api/auth/me') return safeUser(requireAuth())
  if (method === 'post' && path === '/api/auth/logout') return null

  // 题库（管理员）
  if (method === 'get' && path === '/api/questions') return handleQuestionList(q, requireAdmin())
  if (method === 'post' && path === '/api/questions') return handleQuestionCreate(data, requireAdmin())
  if ((m = matchPattern('/api/questions/:id', path))) {
    return handleQuestionItem(method, Number(m.id), data, requireAdmin())
  }

  // 试卷
  if (method === 'get' && path === '/api/papers') return handlePaperList(q, requireAuth())
  if (method === 'post' && path === '/api/papers/generate') return handlePaperGenerate(data, requireAdmin())
  if (method === 'post' && path === '/api/papers') return handlePaperCreate(data, requireAdmin())
  if ((m = matchPattern('/api/papers/:id/publish', path)) && method === 'post') {
    return handlePaperPublish(Number(m.id), requireAdmin())
  }
  if ((m = matchPattern('/api/papers/:id', path))) {
    const id = Number(m.id)
    if (method === 'get') return handlePaperDetail(id, requireAdmin())
    if (method === 'put') return handlePaperUpdate(id, data, requireAdmin())
    if (method === 'delete') return handlePaperDelete(id, requireAdmin())
  }

  // 考试
  if (method === 'get' && path === '/api/exams/available') return handleAvailable(requireAuth())
  if (method === 'get' && path === '/api/exams/records') return handleMyRecords(requireAuth(), q)
  if ((m = matchPattern('/api/exams/records/:id', path)) && method === 'get') {
    return handleRecordDetail(requireAuth(), Number(m.id))
  }
  if ((m = matchPattern('/api/exams/:paperId/start', path)) && method === 'post') {
    return handleStart(requireAuth(), Number(m.paperId))
  }
  if ((m = matchPattern('/api/exams/:recordId/save', path)) && method === 'post') {
    return handleSave(requireAuth(), Number(m.recordId), data)
  }
  if ((m = matchPattern('/api/exams/:recordId/submit', path)) && method === 'post') {
    return handleSubmit(requireAuth(), Number(m.recordId), data)
  }

  // 错题本
  if (method === 'get' && path === '/api/wrong-questions/review-today') return handleReviewToday(requireAuth())
  if (method === 'get' && path === '/api/wrong-questions') return handleWrongList(requireAuth(), q)
  if ((m = matchPattern('/api/wrong-questions/:id/master', path)) && method === 'post') {
    return handleWrongMaster(requireAuth(), Number(m.id))
  }
  if ((m = matchPattern('/api/wrong-questions/:id', path)) && method === 'delete') {
    return handleWrongDelete(requireAuth(), Number(m.id))
  }

  // 统计
  if (method === 'get' && path === '/api/stats/my-scores') return handleMyScores(requireAuth())
  if (method === 'get' && path === '/api/stats/knowledge-mastery') return handleKnowledgeMastery(requireAuth())
  if ((m = matchPattern('/api/stats/paper/:paperId/accuracy', path)) && method === 'get') {
    return handlePaperAccuracy(Number(m.paperId), requireAdmin())
  }

  const e = err(404, `Mock 接口未实现: ${method.toUpperCase()} ${path}`)
  throw e
}

export function resetMockData() {
  resetDB()
}
