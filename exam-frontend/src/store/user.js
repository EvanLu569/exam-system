import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken, getUser, setUser, removeUser } from '@/utils/auth'
import { login as loginApi, getMe as getMeApi, logout as logoutApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const userInfo = ref(getUser() || null)

  // 登录：接口返回 { id, username, role, token }
  async function login(payload) {
    const data = await loginApi(payload)
    token.value = data.token
    userInfo.value = { id: data.id, username: data.username, role: data.role }
    setToken(data.token)
    setUser(userInfo.value)
    return data
  }

  async function fetchInfo() {
    const info = await getMeApi()
    userInfo.value = info
    setUser(info)
    return info
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 忽略登出接口异常，本地状态照常清理
    }
    reset()
  }

  function reset() {
    token.value = ''
    userInfo.value = null
    removeToken()
    removeUser()
  }

  const isAdmin = () => userInfo.value?.role === 'admin'

  return { token, userInfo, login, fetchInfo, logout, reset, isAdmin }
})
