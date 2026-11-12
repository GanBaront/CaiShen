import http from './http'
import type { AdminInfo, LoginResult } from './token'

export async function login(username: string, password: string): Promise<LoginResult> {
  const res = await http.post('/admin/login', { username, password })
  const data = res.data as Partial<LoginResult> & { admin?: AdminInfo }
  return {
    token: (data?.token as string) || '',
    admin: data?.admin,
    username: data?.username,
  }
}

export async function logout(): Promise<void> {
  try {
    await http.post('/admin/logout')
  } catch {
    // 退出失败也照常清理本地登录态
  }
}
