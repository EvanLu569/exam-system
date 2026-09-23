<template>
  <div class="page">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">试卷管理</span>
        <div>
          <el-button @click="$router.push('/admin/papers/generate')">智能组卷</el-button>
          <el-button type="primary" @click="openCreate">手动组卷</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="试卷名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="durationMinutes" label="时长(分)" width="90" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column prop="questionCount" label="题数" width="70" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="考试时间" min-width="200">
          <template #default="{ row }">
            <div v-if="row.startTime">{{ formatDateTime(row.startTime) }} ~ {{ formatDateTime(row.endTime) }}</div>
            <span v-else class="muted">不限时间</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">题目/编辑</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="handlePublish(row)">发布</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 手动组卷 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑试卷' : '手动组卷'" width="860px" top="4vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="paper-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="试卷名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入试卷名称" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="考试时长" prop="durationMinutes">
              <el-input-number v-model="form.durationMinutes" :min="1" :max="600" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="分钟">
              <span class="total-score">总分 {{ totalScore }} 分</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="可选" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止时间">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="可选" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-divider content-position="left">已选题目（{{ selected.length }}）</el-divider>
      <div class="selected-box">
        <el-empty v-if="!selected.length" description="尚未选题" :image-size="50" />
        <div v-for="(item, i) in selected" :key="item.questionId" class="selected-row">
          <span class="s-idx">{{ i + 1 }}.</span>
          <span class="s-content">{{ item.content }}</span>
          <el-tag size="small" :type="item.type === 1 ? 'primary' : item.type === 2 ? 'success' : 'warning'" class="s-tag">
            {{ typeLabel(item.type) }}
          </el-tag>
          <span class="s-score-label">分值</span>
          <el-input-number v-model="item.score" :min="1" :max="100" size="small" style="width: 90px" />
          <el-button link type="danger" @click="removeSelected(i)">移除</el-button>
        </div>
      </div>

      <el-divider content-position="left">从题库选题</el-divider>
      <el-input v-model="bankKeyword" placeholder="搜索题干关键词" clearable class="bank-search" />
      <div class="bank-box">
        <div v-for="q in bankFiltered" :key="q.id" class="bank-row">
          <span class="b-content">{{ q.content }}</span>
          <el-tag size="small" :type="q.type === 1 ? 'primary' : q.type === 2 ? 'success' : 'warning'">
            {{ typeLabel(q.type) }}
          </el-tag>
          <el-tag size="small" :type="difficultyType(q.difficulty)">{{ difficultyLabel(q.difficulty) }}</el-tag>
          <span class="b-score">{{ q.score }} 分</span>
          <el-button
            size="small"
            type="primary"
            plain
            :disabled="selected.some((s) => s.questionId === q.id)"
            @click="addSelected(q)"
          >
            {{ selected.some((s) => s.questionId === q.id) ? '已选' : '加入' }}
          </el-button>
        </div>
        <el-empty v-if="!bankFiltered.length" description="题库中没有匹配的题目" :image-size="50" />
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPapers, getPaper, createPaper, updatePaper, deletePaper, publishPaper } from '@/api/paper'
import { listQuestions } from '@/api/question'
import { typeLabel, difficultyLabel, difficultyType, statusLabel, statusType, formatDateTime } from '@/utils/format'

const loading = ref(true)
const list = ref([])
const page = ref(1)
const size = 10
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const editId = ref(null)
const formRef = ref()
const bank = ref([])
const bankKeyword = ref('')
const selected = ref([])

const form = reactive({
  name: '',
  durationMinutes: 60,
  startTime: null,
  endTime: null
})

const rules = {
  name: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  durationMinutes: [{ required: true, message: '请输入考试时长', trigger: 'change' }]
}

const totalScore = computed(() => selected.value.reduce((s, x) => s + (x.score || 0), 0))
const bankFiltered = computed(() => {
  const kw = bankKeyword.value.trim()
  if (!kw) return bank.value
  return bank.value.filter((q) => (q.content || '').includes(kw))
})

async function load() {
  loading.value = true
  try {
    const data = await listPapers({ page: page.value, size })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadBank() {
  const data = await listQuestions({ page: 1, size: 1000 })
  bank.value = data.list
}

onMounted(load)

function openCreate() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, { name: '', durationMinutes: 60, startTime: null, endTime: null })
  selected.value = []
  bankKeyword.value = ''
  formRef.value?.clearValidate()
  dialogVisible.value = true
  loadBank()
}

async function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  const detail = await getPaper(row.id)
  Object.assign(form, {
    name: detail.name,
    durationMinutes: detail.durationMinutes,
    startTime: detail.startTime || null,
    endTime: detail.endTime || null
  })
  selected.value = detail.questions.map((q) => ({
    questionId: q.id,
    content: q.content,
    type: q.type,
    score: q.score
  }))
  bankKeyword.value = ''
  formRef.value?.clearValidate()
  dialogVisible.value = true
  loadBank()
}

function addSelected(q) {
  selected.value.push({ questionId: q.id, content: q.content, type: q.type, score: q.score || 5 })
}

function removeSelected(i) {
  selected.value.splice(i, 1)
}

async function handleSave() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (!selected.value.length) {
    ElMessage.warning('请至少选择一道题目')
    return
  }

  const payload = {
    name: form.name,
    durationMinutes: form.durationMinutes,
    totalScore: totalScore.value,
    startTime: form.startTime,
    endTime: form.endTime,
    questions: selected.value.map((s, i) => ({ questionId: s.questionId, score: s.score, sortOrder: i + 1 }))
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await updatePaper(editId.value, payload)
      ElMessage.success('试卷已更新')
    } else {
      await createPaper(payload)
      ElMessage.success('试卷已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handlePublish(row) {
  try {
    await ElMessageBox.confirm(`确认发布试卷「${row.name}」？发布后学生即可参加。`, '发布确认', { type: 'warning' })
  } catch {
    return
  }
  await publishPaper(row.id)
  ElMessage.success('已发布')
  load()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除试卷「${row.name}」将同时删除其考试记录，确认删除？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  await deletePaper(row.id)
  ElMessage.success('已删除')
  load()
}
</script>

<style scoped>
.paper-form {
  margin-bottom: 4px;
}

.total-score {
  color: #f56c6c;
  font-weight: 600;
}

.selected-box,
.bank-box {
  max-height: 240px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px;
}

.selected-row,
.bank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 4px;
  border-bottom: 1px solid #f5f7fa;
}

.selected-row:last-child,
.bank-row:last-child {
  border-bottom: none;
}

.s-idx {
  color: #909399;
  width: 24px;
}

.s-content,
.b-content {
  flex: 1;
  color: #303133;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.s-tag {
  flex-shrink: 0;
}

.s-score-label {
  color: #909399;
  font-size: 12px;
}

.b-score {
  color: #909399;
  font-size: 12px;
  width: 44px;
}

.bank-search {
  width: 300px;
  margin-bottom: 8px;
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
