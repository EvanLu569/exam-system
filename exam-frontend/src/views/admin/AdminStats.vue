<template>
  <div class="page">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">试卷正确率分析</span>
        <el-select v-model="paperId" placeholder="选择试卷" style="width: 280px" @change="loadAccuracy">
          <el-option v-for="p in papers" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </div>

      <el-empty v-if="!paperId" description="请选择一张试卷查看各题正确率" />
      <template v-else>
        <el-alert
          v-if="!loading && !accuracy.length"
          title="该试卷暂无答题记录"
          type="info"
          :closable="false"
          show-icon
        />
        <el-table v-loading="loading" :data="accuracy" stripe>
          <el-table-column prop="questionId" label="题号" width="70" />
          <el-table-column prop="content" label="题目内容" min-width="260" show-overflow-tooltip />
          <el-table-column prop="totalCount" label="作答人数" width="100" />
          <el-table-column prop="correctCount" label="答对人数" width="100" />
          <el-table-column label="正确率" min-width="200">
            <template #default="{ row }">
              <div class="acc-cell">
                <el-progress
                  :percentage="Math.round(row.accuracyRate * 100)"
                  :color="accuracyColor(row.accuracyRate)"
                  :stroke-width="14"
                  style="flex: 1"
                />
                <span class="acc-text">{{ Math.round(row.accuracyRate * 100) }}%</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listPapers } from '@/api/paper'
import { getPaperAccuracy } from '@/api/stats'

const papers = ref([])
const paperId = ref(null)
const accuracy = ref([])
const loading = ref(false)

function accuracyColor(rate) {
  if (rate >= 0.8) return '#67c23a'
  if (rate >= 0.6) return '#409eff'
  if (rate >= 0.4) return '#e6a23c'
  return '#f56c6c'
}

async function loadAccuracy() {
  if (!paperId.value) return
  loading.value = true
  try {
    accuracy.value = await getPaperAccuracy(paperId.value)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const data = await listPapers({ page: 1, size: 100 })
  papers.value = data.list
  if (papers.value.length) {
    paperId.value = papers.value[0].id
    loadAccuracy()
  }
})
</script>

<style scoped>
.acc-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.acc-text {
  color: #606266;
  font-size: 13px;
  width: 44px;
  text-align: right;
}
</style>
