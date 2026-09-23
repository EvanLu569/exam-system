<template>
  <div class="page">
    <el-card shadow="never" class="generate-card">
      <el-page-header @back="$router.push('/admin/papers')" content="智能组卷" class="back" />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-divider content-position="left">试卷信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="试卷名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入试卷名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试时长" prop="durationMinutes">
              <el-input-number v-model="form.durationMinutes" :min="1" :max="600" style="width: 160px" />
              <span class="unit">分钟</span>
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

        <el-divider content-position="left">组卷规则</el-divider>
        <el-form-item label="题型分布">
          <div class="dist-row">
            <span v-for="t in QUESTION_TYPES" :key="t.value" class="dist-item">
              <span class="dist-label">{{ t.label }}</span>
              <el-input-number v-model="typeDist[t.value]" :min="0" :max="200" size="small" style="width: 90px" />
              <span class="dist-unit">题</span>
            </span>
          </div>
        </el-form-item>

        <el-form-item label="难度分布">
          <div class="dist-row">
            <span v-for="d in DIFFICULTIES" :key="d.value" class="dist-item">
              <span class="dist-label">{{ d.label }}</span>
              <el-input-number v-model="difficultyDist[d.value]" :min="0" :max="200" size="small" style="width: 90px" />
              <span class="dist-unit">题</span>
            </span>
          </div>
        </el-form-item>

        <el-form-item label="知识点覆盖">
          <el-select
            v-model="form.knowledgePoints"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入知识点（可留空）"
            style="width: 100%"
          >
            <el-option v-for="kp in SUGGEST_KP" :key="kp" :label="kp" :value="kp" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="generating" @click="handleGenerate">开始组卷</el-button>
          <el-button @click="$router.push('/admin/papers')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { generatePaper } from '@/api/paper'
import { QUESTION_TYPES, DIFFICULTIES } from '@/utils/format'

const SUGGEST_KP = ['集合', '多线程', 'JVM', '基础语法', '数据结构']

const router = useRouter()
const formRef = ref()
const generating = ref(false)

const form = reactive({
  name: '',
  durationMinutes: 60,
  startTime: null,
  endTime: null,
  knowledgePoints: []
})

const typeDist = reactive({ 1: 0, 2: 0, 3: 0 })
const difficultyDist = reactive({ 1: 0, 2: 0, 3: 0 })

const rules = {
  name: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  durationMinutes: [{ required: true, message: '请输入考试时长', trigger: 'change' }]
}

async function handleGenerate() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  const totalByType = Object.values(typeDist).reduce((s, n) => s + (n || 0), 0)
  if (totalByType === 0) {
    ElMessage.warning('请设置题型分布（至少选择一道题）')
    return
  }

  const payload = {
    name: form.name,
    durationMinutes: form.durationMinutes,
    totalScore: 100,
    startTime: form.startTime,
    endTime: form.endTime,
    rules: {
      typeDistribution: Object.fromEntries(Object.entries(typeDist).filter(([, v]) => v > 0).map(([k, v]) => [k, v])),
      difficultyDistribution: Object.fromEntries(Object.entries(difficultyDist).filter(([, v]) => v > 0).map(([k, v]) => [k, v])),
      knowledgePoints: form.knowledgePoints
    }
  }

  generating.value = true
  try {
    const res = await generatePaper(payload)
    ElMessage.success(`组卷成功，共抽取 ${res.questions.length} 道题`)
    router.push('/admin/papers')
  } catch {
    // 错误提示已由请求层统一处理
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.generate-card {
  max-width: 860px;
  margin: 0 auto;
}

.back {
  margin-bottom: 16px;
}

.unit {
  margin-left: 8px;
  color: #909399;
}

.dist-row {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

.dist-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dist-label {
  color: #606266;
  font-size: 14px;
  width: 40px;
}

.dist-unit {
  color: #909399;
}
</style>
