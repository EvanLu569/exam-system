<template>
  <div class="page">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">题库管理</span>
        <el-button type="primary" @click="openCreate">新增题目</el-button>
      </div>

      <!-- 筛选 -->
      <div class="filters">
        <el-select v-model="query.type" placeholder="题型" clearable style="width: 130px" @change="reload">
          <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-select v-model="query.difficulty" placeholder="难度" clearable style="width: 130px" @change="reload">
          <el-option v-for="d in DIFFICULTIES" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="题干关键词"
          clearable
          style="width: 220px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-button type="primary" plain @click="reload">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="content" label="题目内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 1 ? 'primary' : row.type === 2 ? 'success' : 'warning'">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="difficultyType(row.difficulty)">{{ difficultyLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="knowledgePoint" label="知识点" width="110">
          <template #default="{ row }">{{ row.knowledgePoint || '-' }}</template>
        </el-table-column>
        <el-table-column label="正确答案" width="110">
          <template #default="{ row }">
            <b class="answer">{{ answerText(row.correctAnswer, row.type) }}</b>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="70" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > query.size" class="pager">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="load"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑题目' : '新增题目'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="题型" prop="type">
          <el-radio-group v-model="form.type" @change="onTypeChange">
            <el-radio-button :value="1">单选题</el-radio-button>
            <el-radio-button :value="2">多选题</el-radio-button>
            <el-radio-button :value="3">判断题</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题目内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="2" placeholder="请输入题目内容" />
        </el-form-item>

        <el-form-item v-if="form.type !== 3" label="选项">
          <div class="option-editor">
            <div v-for="(opt, i) in form.options" :key="i" class="option-row">
              <span class="opt-letter">{{ optionLetter(i) }}.</span>
              <el-input v-model="form.options[i]" :placeholder="`选项 ${optionLetter(i)} 内容`" />
              <el-button type="danger" link :disabled="form.options.length <= 2" @click="removeOption(i)">
                删除
              </el-button>
            </div>
            <el-button type="primary" link @click="addOption">+ 添加选项</el-button>
          </div>
        </el-form-item>

        <el-form-item v-if="form.type === 1" label="正确答案" prop="correctAnswer">
          <el-radio-group v-model="form.correctAnswer">
            <el-radio v-for="(opt, i) in form.options" :key="i" :value="optionLetter(i)">
              {{ optionLetter(i) }}
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.type === 2" label="正确答案" prop="multiAnswers">
          <el-checkbox-group v-model="multiAnswers">
            <el-checkbox v-for="(opt, i) in form.options" :key="i" :value="optionLetter(i)">
              {{ optionLetter(i) }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item v-if="form.type === 3" label="正确答案" prop="correctAnswer">
          <el-radio-group v-model="form.correctAnswer">
            <el-radio value="T">正确</el-radio>
            <el-radio value="F">错误</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="难度" prop="difficulty">
          <el-radio-group v-model="form.difficulty">
            <el-radio-button :value="1">简单</el-radio-button>
            <el-radio-button :value="2">中等</el-radio-button>
            <el-radio-button :value="3">困难</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="知识点">
          <el-input v-model="form.knowledgePoint" placeholder="如：集合 / 多线程 / JVM" />
        </el-form-item>

        <el-form-item label="分值" prop="score">
          <el-input-number v-model="form.score" :min="1" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listQuestions, createQuestion, updateQuestion, deleteQuestion } from '@/api/question'
import {
  QUESTION_TYPES,
  DIFFICULTIES,
  typeLabel,
  difficultyLabel,
  difficultyType,
  optionLetter,
  answerText,
  toLetters,
  serializeAnswer
} from '@/utils/format'

const loading = ref(true)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, type: null, difficulty: null, keyword: '' })

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const editId = ref(null)
const formRef = ref()
const multiAnswers = ref([])

const form = reactive({
  type: 1,
  content: '',
  options: ['', ''],
  correctAnswer: '',
  score: 5,
  difficulty: 1,
  knowledgePoint: ''
})

const rules = {
  content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.type) params.type = query.type
    if (query.difficulty) params.difficulty = query.difficulty
    if (query.keyword) params.keyword = query.keyword
    const data = await listQuestions(params)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function reload() {
  query.page = 1
  load()
}

onMounted(load)

function resetForm() {
  Object.assign(form, { type: 1, content: '', options: ['', ''], correctAnswer: '', score: 5, difficulty: 1, knowledgePoint: '' })
  multiAnswers.value = []
  formRef.value?.clearValidate()
}

function onTypeChange() {
  form.correctAnswer = ''
  multiAnswers.value = []
  if (form.type !== 3 && (!form.options || form.options.length === 0)) {
    form.options = ['', '']
  }
}

function addOption() {
  form.options.push('')
}

function removeOption(i) {
  form.options.splice(i, 1)
  form.correctAnswer = ''
  multiAnswers.value = []
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    type: row.type,
    content: row.content,
    options: row.options ? [...row.options] : ['', ''],
    correctAnswer: row.correctAnswer,
    score: row.score,
    difficulty: row.difficulty ?? 1,
    knowledgePoint: row.knowledgePoint || ''
  })
  multiAnswers.value = row.type === 2 ? toLetters(row.correctAnswer) : []
  dialogVisible.value = true
}

async function handleSave() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  if (form.type !== 3) {
    const filled = form.options.filter((o) => o && o.trim())
    if (filled.length < 2) {
      ElMessage.warning('请至少填写两个选项')
      return
    }
  }

  const correctAnswer = form.type === 2 ? serializeAnswer(multiAnswers.value, 2) : form.correctAnswer
  if (!correctAnswer) {
    ElMessage.warning('请设置正确答案')
    return
  }

  const payload = {
    type: form.type,
    content: form.content,
    score: form.score,
    difficulty: form.difficulty,
    knowledgePoint: form.knowledgePoint,
    options: form.type === 3 ? null : form.options.map((o) => o.trim()).filter(Boolean),
    correctAnswer
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await updateQuestion(editId.value, payload)
      ElMessage.success('题目已更新')
    } else {
      await createQuestion(payload)
      ElMessage.success('题目已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该题目？已加入试卷的题目也会一并移除。', '删除确认', { type: 'warning' })
  } catch {
    return
  }
  await deleteQuestion(row.id)
  ElMessage.success('已删除')
  load()
}
</script>

<style scoped>
.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.option-editor {
  width: 100%;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.opt-letter {
  font-weight: 600;
  color: #409eff;
  width: 20px;
}

.answer {
  color: #67c23a;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
