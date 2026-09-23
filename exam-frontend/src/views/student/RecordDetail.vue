<template>
  <div class="page">
    <div v-if="loading" class="loading-box">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else-if="record">
      <el-page-header @back="$router.push('/records')" content="记录详情" class="back" />

      <el-card class="score-card" shadow="never">
        <div class="score-wrap">
          <div class="score-big">{{ record.totalScore }}</div>
          <div class="score-meta">
            <h2 class="paper-name">{{ record.paperName }}</h2>
            <div class="score-line">
              满分 {{ record.paperTotalScore ?? '-' }} 分
              <el-tag :type="passed ? 'success' : 'danger'" size="small" class="pass-tag">
                {{ passed ? '及格' : '不及格' }}
              </el-tag>
            </div>
            <div class="score-line muted">交卷时间：{{ formatDateTime(record.submitTime) }}</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never">
        <div class="detail-head">
          答题详情
          <span class="detail-stat">答对 <b class="ok">{{ record.correctCount }}</b> / {{ record.totalCount }} 题</span>
        </div>

        <div v-for="(d, i) in record.questions" :key="d.questionId" class="detail-item">
          <div class="detail-q">
            <el-tag size="small" :type="d.isCorrect ? 'success' : 'danger'">
              {{ d.isCorrect ? '正确' : '错误' }}
            </el-tag>
            <span class="d-no">{{ i + 1 }}.</span>
            <span class="d-content">{{ d.content }}</span>
            <span class="d-score">{{ d.score }} 分</span>
          </div>
          <div class="d-answer">
            <div>你的答案：<span :class="d.isCorrect ? 'ok' : 'bad'">{{ answerText(d.userAnswer, d.type) || '未作答' }}</span></div>
            <div v-if="!d.isCorrect">正确答案：<span class="ok">{{ answerText(d.correctAnswer, d.type) }}</span></div>
          </div>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getRecordDetail } from '@/api/exam'
import { answerText, formatDateTime } from '@/utils/format'

const route = useRoute()
const recordId = Number(route.params.id)
const loading = ref(true)
const record = ref(null)

const passed = computed(() => {
  const total = record.value?.paperTotalScore || 100
  return (record.value?.totalScore ?? 0) >= total * 0.6
})

onMounted(async () => {
  try {
    record.value = await getRecordDetail(recordId)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.loading-box {
  padding: 20px;
}

.back {
  margin-bottom: 16px;
}

.score-card {
  margin-bottom: 16px;
}

.score-wrap {
  display: flex;
  align-items: center;
  gap: 24px;
}

.score-big {
  font-size: 48px;
  font-weight: 700;
  color: #409eff;
  line-height: 1;
}

.score-meta {
  flex: 1;
}

.paper-name {
  margin: 0 0 6px;
  font-size: 18px;
  color: #303133;
}

.score-line {
  color: #606266;
  font-size: 14px;
  margin-top: 4px;
}

.score-line.muted {
  color: #909399;
  font-size: 13px;
}

.pass-tag {
  margin-left: 8px;
}

.detail-head {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.detail-stat {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
}

.detail-stat .ok {
  color: #67c23a;
}

.detail-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0f2f5;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-q {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 6px;
}

.d-no {
  font-weight: 600;
  color: #303133;
}

.d-content {
  flex: 1;
  color: #303133;
  line-height: 1.5;
}

.d-score {
  color: #909399;
  font-size: 13px;
  white-space: nowrap;
}

.d-answer {
  padding-left: 44px;
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
}

.ok {
  color: #67c23a;
  font-weight: 600;
}

.bad {
  color: #f56c6c;
  font-weight: 600;
}
</style>
