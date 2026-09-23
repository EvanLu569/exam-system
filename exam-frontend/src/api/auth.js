// 认证模块接口
import request from './request'

// 登录：返回 { id, username, role, token }
export function login(data) {
  return request({ url: '/auth/login', method: 'post', data })
}

// 注册：返回 null（成功后需重新登录）
export function register(data) {
  return request({ url: '/auth/register', method: 'post', data })
}

// 获取当前用户：返回 { id, username, role }
export function getMe() {
  return request({ url: '/auth/me', method: 'get' })
}

export function logout() {
  return request({ url: '/auth/logout', method: 'post' })
}
