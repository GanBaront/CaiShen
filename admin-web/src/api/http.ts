import axios from 'axios'
import { clearAuth, getToken } from './token'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    // 兼容 { code, message, data } 与直接返回数据体两种契约
    if (body && typeof body === 'object' && 'data' in body && ('code' in body || 'success' in body)) {
      res.data = (body as { data: unknown }).data
    }
    return res
  },
  (err) => {
    if (err?.response?.status === 401) {
      clearAuth()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  },
)

/** 兼容分页返回：数组、{list/records/rows, total} 均可 */
export function asList<T>(data: unknown): T[] {
  if (Array.isArray(data)) return data as T[]
  if (data && typeof data === 'object') {
    const d = data as Record<string, unknown>
    for (const key of ['list', 'records', 'rows', 'items']) {
      if (Array.isArray(d[key])) return d[key] as T[]
    }
  }
  return []
}

export default http
