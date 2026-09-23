<template>
  <div class="page">
    <div v-if="loading" class="loading-box">
      <el-skeleton :rows="4" animated />
    </div>

    <el-empty v-else-if="!exams.length" description="暂无正在进行的考试" />

    <div v-else class="paper-grid">
      <el-card v-for="p in exams" :key="p.id" class="paper-card" shadow="hover">
        <div class="paper-head">
          <h3 class="paper-name">{{ p.name }}</h3>
          <el-tag v-if="p.submitted" type="info" size="small">已交卷</el-tag>
          <el-tag v-else-if="p.inProgress" type="warning" size="small">进行中</el-tag>
        </div>

        <div class="paper-meta">
          <span>⏱ {{ p.durationMinutes }} 分钟</span>
          <span>💯 满分 {{ p.totalScore }} 分</span>
        </div>

        <div class="paper-time">
          <span v-if="p.startTime">开始：{{ formatDateTime(p.startTime) }}</span>
          <span v-if="p.endTime">截止：{{ formatDateTime(p.endTime) }}</span>
        </div>

        <div class="paper-actions">
          <el-button v-if="p.inProgress" type="warning" @click="goTaking(p.id)">继续考试</el-button>
          <el-button v-else-if="p.submitted" type="info" plain disabled>已交卷</el-button>
          <el-button v-else type="primary" @click="goTaking(p.id)">开始考试</el-button>
          <el-button v-if="p.submitted" type="primary" plain @click="$router.push('/records')">
            查看记录
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAvailableExams } from '@/api/exam'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(true)
const exams = ref([])

onMounted(async () => {
  try {
    exams.value = await getAvailableExams()
  } finally {
    loading.value = false
  }
})

function goTaking(id) {
  router.push({ name: 'exam-taking', params: { paperId: id } })
}
</script>

<style scoped>
.loading-box {
  padding: 20px;
}

.paper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.paper-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.paper-name {
  margin: 0;
  font-size: 16px;
  color: #303133;
  line-height: 1.4;
}

.paper-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin: 14px 0 6px;
  color: #606266;
  font-size: 13px;
}

.paper-time {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 14px;
  color: #909399;
  font-size: 12px;
}

.paper-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
