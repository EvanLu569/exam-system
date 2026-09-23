<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <div class="brand-icon">📝</div>
        <h1 class="brand-title">在线考试系统</h1>
        <p class="brand-sub">Online Examination System</p>
      </div>

      <el-tabs v-model="tab" class="login-tabs">
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="学生注册" name="register" />
      </el-tabs>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" clearable>
            <template #prefix>👤</template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            @keyup.enter="onSubmit"
          >
            <template #prefix>🔒</template>
          </el-input>
        </el-form-item>

        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="onSubmit">
          {{ tab === 'login' ? '登 录' : '注 册' }}
        </el-button>
      </el-form>

      <div class="tips">
        <div>演示账号：管理员 <b>admin / 123456</b>，学生 <b>student / 123456</b></div>
        <el-button link type="primary" size="small" @click="resetDemo">重置演示数据</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { register } from '@/api/auth'
import { resetMockData, USE_MOCK } from '@/api/mock'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const tab = ref('login')
const loading = ref(false)
const formRef = ref()
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function goHome(role) {
  const redirect = route.query.redirect
  router.replace(redirect || (role === 'admin' ? '/admin/papers' : '/exams'))
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    if (tab.value === 'login') {
      const data = await store.login({ username: form.username, password: form.password })
      ElMessage.success(`欢迎，${data.username}`)
      goHome(data.role)
    } else {
      await register({ username: form.username, password: form.password, role: 'student' })
      ElMessage.success('注册成功，请登录')
      tab.value = 'login'
      form.password = ''
    }
  } catch {
    // 错误提示已由请求层统一处理
  } finally {
    loading.value = false
  }
}

function resetDemo() {
  if (USE_MOCK) {
    resetMockData()
    ElMessage.success('演示数据已重置')
    store.reset()
    router.replace('/login')
  } else {
    ElMessage.info('当前已连接真实后端，无需重置演示数据')
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f6feb 0%, #6a5cff 50%, #a855f7 100%);
}

.login-card {
  width: 400px;
  max-width: calc(100vw - 32px);
  background: #fff;
  border-radius: 14px;
  padding: 40px 36px 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.brand {
  text-align: center;
  margin-bottom: 8px;
}

.brand-icon {
  font-size: 44px;
}

.brand-title {
  margin: 8px 0 4px;
  font-size: 22px;
  color: #303133;
}

.brand-sub {
  margin: 0;
  color: #909399;
  font-size: 13px;
  letter-spacing: 1px;
}

.login-tabs {
  margin-top: 12px;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

.tips {
  margin-top: 20px;
  text-align: center;
  color: #909399;
  font-size: 13px;
  line-height: 1.8;
}
</style>
