// 统一请求封装：
//  - 请求拦截：自动携带 JWT（Authorization: Bearer {token}）
//  - 响应拦截：处理统一返回格式 { code, message, data }
//  - 默认走 Mock（VITE_USE_MOCK=true），设为 false 时走真实 axios（/api 经 vite 代理转发到后端）
// 所有 API 模块统一 resolve 到 data，失败以带 code 的 Error 抛出。

import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken, removeUser } from '@/utils/auth'
import { mockRequest, USE_MOCK } from './mock'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

service.interceptors.response.use(
  (res) => {
    const body = res.data
    // 统一返回格式 { code, message, data }
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) return body.data
      return Promise.reject(makeError(body.code, body.message))
    }
    // 兼容后端直接返回 data 的情况
    return body
  },
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.message || err.message || '请求失败，请稍后重试'
    return Promise.reject(makeError(status || 500, msg))
  }
)

function makeError(code, message) {
  const e = new Error(message || '请求失败')
  e.code = code
  return e
}

function handleError(e) {
  const code = e?.code || e?.status
  if (code === 401) {
    removeToken()
    removeUser()
    if (!location.pathname.startsWith('/login')) {
      location.href = '/login'
    }
    return
  }
  ElMessage.error(e?.message || '请求失败，请稍后重试')
}

export default async function request(config) {
  try {
    if (USE_MOCK) {
      // Mock 引擎按「接口文档」路径匹配，要求带 /api 前缀；而 api 模块的 url 依赖 axios 的 baseURL 补齐前缀。
      // 这里手动补上，保证 mock 与真实后端请求路径一致。
      const url = config.url.startsWith('/api') ? config.url : `/api${config.url}`
      return await mockRequest({ ...config, url })
    }
    return await service(config)
  } catch (e) {
    handleError(e)
    throw e
  }
}
