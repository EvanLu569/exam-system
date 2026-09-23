<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <span class="logo-icon">📝</span>
        <span>在线考试系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#001529"
        text-color="rgba(255,255,255,0.68)"
        active-text-color="#fff"
        class="menu"
      >
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <span class="menu-icon">{{ m.icon }}</span>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ route.meta.title }}</div>
        <el-dropdown trigger="click" @command="onCommand">
          <div class="user">
            <el-avatar :size="30" class="avatar">{{ avatarText }}</el-avatar>
            <span class="name">{{ store.userInfo?.username }}</span>
            <el-tag size="small" :type="store.userInfo?.role === 'admin' ? 'warning' : 'success'">
              {{ store.userInfo?.role === 'admin' ? '管理员' : '学生' }}
            </el-tag>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const menus = computed(() => {
  if (store.userInfo?.role === 'admin') {
    return [
      { path: '/admin/papers', title: '试卷管理', icon: '📄' },
      { path: '/admin/questions', title: '题库管理', icon: '📝' },
      { path: '/admin/papers/generate', title: '智能组卷', icon: '🧩' },
      { path: '/admin/stats', title: '成绩分析', icon: '📊' }
    ]
  }
  return [
    { path: '/exams', title: '考试列表', icon: '📋' },
    { path: '/records', title: '我的记录', icon: '🏆' },
    { path: '/wrong-questions', title: '错题本', icon: '📕' },
    { path: '/stats', title: '成绩分析', icon: '📊' }
  ]
})

const activeMenu = computed(() => {
  // 记录详情页高亮「我的记录」
  if (route.path.startsWith('/records')) return '/records'
  if (route.path.startsWith('/admin/papers')) return '/admin/papers'
  return route.path
})

const avatarText = computed(() => {
  const name = store.userInfo?.username || '?'
  return name.charAt(0).toUpperCase()
})

async function onCommand(command) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    } catch {
      return
    }
    await store.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: #001529;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}

.logo-icon {
  font-size: 22px;
}

.menu {
  border-right: none;
  flex: 1;
}

.menu-icon {
  margin-right: 8px;
}

.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.avatar {
  background: #409eff;
  color: #fff;
}

.name {
  font-size: 14px;
  color: #303133;
}

.main {
  background: #f5f7fa;
  padding: 20px;
}
</style>
