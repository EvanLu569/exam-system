import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  // 考试答题页（全屏，无侧边栏）
  {
    path: '/exam/:paperId',
    name: 'exam-taking',
    component: () => import('@/views/student/ExamTaking.vue'),
    meta: { title: '考试答题', role: 'student', fullscreen: true }
  },
  // 交卷成绩页（全屏）
  {
    path: '/exam/result/:recordId',
    name: 'exam-result',
    component: () => import('@/views/student/Result.vue'),
    meta: { title: '考试成绩', role: 'student', fullscreen: true }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/exams',
    children: [
      // 学生端
      { path: 'exams', name: 'student-exams', component: () => import('@/views/student/ExamList.vue'), meta: { title: '考试列表', role: 'student' } },
      { path: 'records', name: 'student-records', component: () => import('@/views/student/MyRecords.vue'), meta: { title: '我的记录', role: 'student' } },
      { path: 'records/:id', name: 'student-record-detail', component: () => import('@/views/student/RecordDetail.vue'), meta: { title: '记录详情', role: 'student' } },
      { path: 'wrong-questions', name: 'student-wrong', component: () => import('@/views/student/WrongQuestions.vue'), meta: { title: '错题本', role: 'student' } },
      { path: 'stats', name: 'student-stats', component: () => import('@/views/student/Stats.vue'), meta: { title: '成绩分析', role: 'student' } },
      // 管理员端
      { path: 'admin/papers', name: 'admin-papers', component: () => import('@/views/admin/PaperManage.vue'), meta: { title: '试卷管理', role: 'admin' } },
      { path: 'admin/papers/generate', name: 'admin-paper-generate', component: () => import('@/views/admin/PaperGenerate.vue'), meta: { title: '智能组卷', role: 'admin' } },
      { path: 'admin/questions', name: 'admin-questions', component: () => import('@/views/admin/QuestionManage.vue'), meta: { title: '题库管理', role: 'admin' } },
      { path: 'admin/stats', name: 'admin-stats', component: () => import('@/views/admin/AdminStats.vue'), meta: { title: '成绩分析', role: 'admin' } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFound.vue'),
    meta: { public: true, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

function homeOf(role) {
  return role === 'admin' ? '/admin/papers' : '/exams'
}

router.beforeEach(async (to) => {
  document.title = (to.meta.title ? `${to.meta.title} - ` : '') + '在线考试系统'

  const token = getToken()
  const store = useUserStore()

  if (to.meta.public) {
    if (to.name === 'login' && token && store.userInfo) {
      return homeOf(store.userInfo.role)
    }
    return true
  }

  if (!token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (!store.userInfo) {
    try {
      await store.fetchInfo()
    } catch {
      store.reset()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  const role = store.userInfo?.role
  if (to.meta.role && to.meta.role !== role) {
    return homeOf(role)
  }
  return true
})

export default router
