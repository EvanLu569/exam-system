<template>
  <div class="page">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">我的考试记录</span>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="记录号" width="90" />
        <el-table-column prop="paperName" label="试卷名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="得分" width="100">
          <template #default="{ row }">
            <b :class="row.status === 1 ? 'score-ok' : 'muted'">
              {{ row.status === 1 ? row.totalScore : '-' }}
            </b>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="交卷时间" width="160">
          <template #default="{ row }">{{ row.submitTime ? formatDateTime(row.submitTime) : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
              {{ row.status === 1 ? '已交卷' : '进行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" link type="primary" @click="goDetail(row.id)">
              查看详情
            </el-button>
            <el-button v-else link type="warning" @click="goContinue(row.paperId)">继续考试</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="暂无考试记录" />

      <div v-if="total > size" class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyRecords } from '@/api/exam'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(true)
const list = ref([])
const page = ref(1)
const size = 10
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const data = await getMyRecords({ page: page.value, size })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(load)

function goDetail(id) {
  router.push({ name: 'student-record-detail', params: { id } })
}

function goContinue(paperId) {
  router.push({ name: 'exam-taking', params: { paperId } })
}
</script>

<style scoped>
.score-ok {
  color: #67c23a;
}

.muted {
  color: #c0c4cc;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
