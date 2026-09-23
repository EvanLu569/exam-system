<template>
  <div class="page">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">错题本</span>
      </div>

      <el-tabs v-model="tab" @tab-change="onTabChange">
        <el-tab-pane label="全部错题" name="all" />
        <el-tab-pane :label="`今日待复习 (${reviewToday.length})`" name="today" />
      </el-tabs>

      <div v-loading="loading">
        <el-empty v-if="!loading && currentList.length === 0" description="暂无错题" />

        <el-card
          v-for="w in currentList"
          :key="w.id"
          class="wrong-item"
          shadow="never"
        >
          <div class="wrong-head">
            <el-tag size="small" :type="typeTag(w.type)">{{ typeLabel(w.type) }}</el-tag>
            <span class="wrong-count">答错 {{ w.wrongCount }} 次</span>
            <span v-if="w.nextReviewTime" class="review-time">下次复习：{{ formatDateTime(w.nextReviewTime) }}</span>
          </div>

          <div class="wrong-content">{{ w.content }}</div>

          <div v-if="w.options && w.options.length" class="wrong-options">
            <div v-for="(opt, oi) in w.options" :key="oi" class="wrong-opt">
              <span class="opt-letter">{{ optionLetter(oi) }}.</span> {{ opt }}
            </div>
          </div>

          <div class="wrong-answer">
            正确答案：<b class="ok">{{ answerText(w.correctAnswer, w.type) }}</b>
          </div>

          <div class="wrong-actions">
            <el-button size="small" type="success" plain @click="handleMaster(w)">已掌握</el-button>
            <el-button size="small" type="danger" plain @click="handleRemove(w)">移除</el-button>
          </div>
        </el-card>
      </div>

      <div v-if="tab === 'all' && total > size" class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="loadAll"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listWrongQuestions, getReviewToday, masterWrongQuestion, removeWrongQuestion } from '@/api/wrongQuestion'
import { typeLabel, optionLetter, answerText, formatDateTime } from '@/utils/format'

const tab = ref('all')
const loading = ref(true)
const all = ref([])
const reviewToday = ref([])
const page = ref(1)
const size = 10
const total = ref(0)

const currentList = computed(() => (tab.value === 'all' ? all.value : reviewToday.value))

function typeTag(type) {
  return type === 1 ? 'primary' : type === 2 ? 'success' : 'warning'
}

async function loadAll() {
  loading.value = true
  try {
    const data = await listWrongQuestions({ page: page.value, size })
    all.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadToday() {
  reviewToday.value = await getReviewToday()
}

function onTabChange() {
  if (tab.value === 'all') loadAll()
  else loadToday()
}

onMounted(loadAll)

async function handleMaster(w) {
  await masterWrongQuestion(w.id)
  ElMessage.success('已标记掌握，移出错题本')
  refresh()
}

async function handleRemove(w) {
  try {
    await ElMessageBox.confirm('确认移除该错题？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await removeWrongQuestion(w.id)
  ElMessage.success('已移除')
  refresh()
}

function refresh() {
  if (tab.value === 'all') loadAll()
  else {
    loadToday()
    loadAll()
  }
}
</script>

<style scoped>
.wrong-item {
  margin-bottom: 12px;
}

.wrong-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.wrong-count {
  color: #f56c6c;
  font-size: 13px;
}

.review-time {
  color: #909399;
  font-size: 13px;
}

.wrong-content {
  font-size: 15px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 8px;
}

.wrong-options {
  margin-bottom: 8px;
}

.wrong-opt {
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
}

.opt-letter {
  font-weight: 600;
  color: #409eff;
}

.wrong-answer {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.ok {
  color: #67c23a;
}

.wrong-actions {
  display: flex;
  gap: 8px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
