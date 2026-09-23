<template>
  <div class="exam-page">
    <div v-if="loading" class="loading-box">
      <el-skeleton :rows="8" animated />
    </div>

    <template v-else>
      <!-- 顶部栏：倒计时 -->
      <div class="exam-top">
        <div class="exam-title">{{ store.paper?.name }}</div>
        <div class="exam-info">
          <el-tag v-if="wsState === 'reconnecting'" type="warning" size="small">连接重试中…</el-tag>
          <span class="timer" :class="{ danger: store.remaining < 300 }">
            ⏱ 剩余 {{ formatSeconds(store.remaining) }}
          </span>
          <span class="answered">已答 {{ store.answeredCount }} / {{ store.questions.length }}</span>
        </div>
      </div>

      <div class="exam-body">
        <!-- 答题区 -->
        <div class="question-area">
          <el-card
            v-for="(q, i) in store.questions"
            :key="q.id"
            class="question-card"
            shadow="never"
            :ref="(el) => setAnchor(i, el)"
          >
            <div class="q-head">
              <el-tag size="small" :type="q.type === 1 ? 'primary' : q.type === 2 ? 'success' : 'warning'">
                {{ typeLabel(q.type) }}
              </el-tag>
              <span class="q-score">{{ q.score }} 分</span>
            </div>

            <div class="q-content">
              <span class="q-no">{{ i + 1 }}.</span> {{ q.content }}
            </div>

            <!-- 单选题 -->
            <el-radio-group v-if="q.type === 1" v-model="store.answers[q.id]" class="opt-list" @change="onAnswerChange">
              <el-radio v-for="(opt, oi) in q.options" :key="oi" :value="optionLetter(oi)" class="opt-item">
                <span class="opt-letter">{{ optionLetter(oi) }}.</span> {{ opt }}
              </el-radio>
            </el-radio-group>

            <!-- 多选题 -->
            <el-checkbox-group v-else-if="q.type === 2" v-model="store.answers[q.id]" class="opt-list" @change="onAnswerChange">
              <el-checkbox v-for="(opt, oi) in q.options" :key="oi" :value="optionLetter(oi)" class="opt-item">
                <span class="opt-letter">{{ optionLetter(oi) }}.</span> {{ opt }}
              </el-checkbox>
            </el-checkbox-group>

            <!-- 判断题 -->
            <el-radio-group v-else v-model="store.answers[q.id]" class="opt-list" @change="onAnswerChange">
              <el-radio value="T" class="opt-item">正确</el-radio>
              <el-radio value="F" class="opt-item">错误</el-radio>
            </el-radio-group>
          </el-card>
        </div>

        <!-- 答题卡 -->
        <div class="sheet">
          <el-card shadow="never">
            <div class="sheet-title">答题卡</div>
            <div class="sheet-grid">
              <button
                v-for="(q, i) in store.questions"
                :key="q.id"
                class="sheet-btn"
                :class="{
                  answered: isAnswered(store.answers[q.id]),
                  current: currentIndex === i
                }"
                @click="scrollToCurrent(i)"
              >
                {{ i + 1 }}
              </button>
            </div>
            <div class="sheet-legend">
              <span><i class="dot answered" /> 已答</span>
              <span><i class="dot" /> 未答</span>
            </div>
            <el-button type="primary" class="submit-btn" :loading="submitting" @click="handleSubmit()">
              交 卷
            </el-button>
          </el-card>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useExamStore } from '@/store/exam'
import { useUserStore } from '@/store/user'
import { typeLabel, optionLetter, isAnswered, formatSeconds } from '@/utils/format'
import { createExamSocket } from '@/utils/ws'
import { USE_MOCK } from '@/api/mock'

const route = useRoute()
const router = useRouter()
const store = useExamStore()
const userStore = useUserStore()

const paperId = Number(route.params.paperId)
const loading = ref(true)
const submitting = ref(false)
const currentIndex = ref(0)
const wsState = ref('closed')
const anchors = []

let timer = null
let saveTimer = null
let saveDebounce = null
let wsSocket = null

function setAnchor(i, el) {
  anchors[i] = el
}

function scrollToCurrent(i) {
  currentIndex.value = i
  nextTick(() => {
    anchors[i]?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function onAnswerChange() {
  // 立即落本地缓存，并防抖异步保存到服务端
  clearTimeout(saveDebounce)
  saveDebounce = setTimeout(() => {
    store.save().catch(() => {})
  }, 800)
}

function startCountdown() {
  clearInterval(timer)
  timer = setInterval(() => {
    if (store.remaining > 0) {
      store.remaining -= 1
    }
    if (store.remaining <= 0) {
      clearInterval(timer)
      autoSubmit('考试时间已到')
    }
  }, 1000)
}

function connectWs() {
  if (USE_MOCK || wsSocket) return
  wsSocket = createExamSocket({
    paperId,
    token: userStore.token,
    onCountdown: (seconds) => {
      // 服务端权威时间校准
      store.remaining = Math.max(0, Number(seconds) || 0)
    },
    onEnded: () => autoSubmit('考试已结束'),
    onForceSubmit: (reason) => autoSubmit(reason || '时间到'),
    onStatus: (s) => {
      wsState.value = s
    }
  })
}

function warnLeave(e) {
  e.preventDefault()
  e.returnValue = ''
}

onMounted(async () => {
  try {
    await store.start(paperId)
    startCountdown()
    connectWs()
    // 周期自动保存，防止意外关闭丢失
    saveTimer = setInterval(() => {
      store.save().catch(() => {})
    }, 30000)
    window.addEventListener('beforeunload', warnLeave)
  } catch {
    // 错误提示已由请求层统一处理
    router.replace('/exams')
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  clearInterval(timer)
  clearInterval(saveTimer)
  clearTimeout(saveDebounce)
  wsSocket?.close()
  window.removeEventListener('beforeunload', warnLeave)
})

async function handleSubmit(auto = false) {
  if (submitting.value) return
  if (!auto) {
    const unanswered = store.questions.length - store.answeredCount
    try {
      await ElMessageBox.confirm(
        unanswered > 0 ? `还有 ${unanswered} 题未作答，确定交卷吗？` : '确认提交试卷吗？',
        '交卷确认',
        { type: 'warning', confirmButtonText: '交卷', cancelButtonText: '继续答题' }
      )
    } catch {
      return
    }
  }

  submitting.value = true
  try {
    const res = await store.submit()
    ElMessage.success(`交卷成功，得分 ${res.totalScore} 分`)
    router.replace({ name: 'exam-result', params: { recordId: res.recordId } })
  } catch {
    // 错误提示已由请求层统一处理
  } finally {
    submitting.value = false
  }
}

function autoSubmit(reason) {
  ElMessage.warning(reason + '，系统将自动交卷')
  handleSubmit(true)
}
</script>

<style scoped>
.exam-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 16px;
}

.loading-box {
  padding: 20px;
}

.exam-top {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.exam-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.exam-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.timer {
  font-weight: 600;
  color: #409eff;
}

.timer.danger {
  color: #f56c6c;
}

.answered {
  color: #606266;
  font-size: 14px;
}

.exam-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.question-area {
  flex: 1;
  min-width: 0;
}

.question-card {
  margin-bottom: 16px;
}

.q-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.q-score {
  color: #909399;
  font-size: 13px;
}

.q-content {
  font-size: 15px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 14px;
}

.q-no {
  font-weight: 700;
}

.opt-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.opt-item {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  transition: border-color 0.2s;
  width: 100%;
  margin: 0;
  height: auto;
}

.opt-item:hover {
  border-color: #c6e2ff;
}

.opt-letter {
  font-weight: 600;
  margin-right: 8px;
  color: #409eff;
}

.sheet {
  width: 180px;
  flex-shrink: 0;
  position: sticky;
  top: 76px;
}

.sheet-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.sheet-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.sheet-btn {
  height: 30px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  color: #606266;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.sheet-btn.answered {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.sheet-btn.current {
  box-shadow: 0 0 0 2px #c6e2ff;
}

.sheet-legend {
  display: flex;
  gap: 12px;
  margin: 14px 0;
  font-size: 12px;
  color: #909399;
}

.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 2px;
  margin-right: 4px;
  vertical-align: -1px;
}

.dot.answered {
  background: #409eff;
  border-color: #409eff;
}

.submit-btn {
  width: 100%;
}
</style>
