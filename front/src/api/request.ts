import axios, { type AxiosRequestConfig } from 'axios'

export const TOKEN_KEY = 'cs_token'
export const USER_KEY = 'cs_user'

/** 后端未启动 / 接口异常时统一抛出，由调用方降级到示例数据 */
export class ApiError extends Error {
  status?: number
  constructor(message: string, status?: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

const http = axios.create({
  baseURL: '/api',
  timeout: 8000
})

// 请求拦截器：携带 JWT
http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers = config.headers ?? {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一解包 Result { code, message, data }；未登录（HTTP 401 或 body code 401）清理登录态并跳登录页
http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      const code = (body as any).code
      // 后端字段为 message（历史代码误读 msg，导致错误提示与次数弹窗判断失效）
      const msg = (body as any).message || (body as any).msg || '请求失败'
      if (code === 200 || code === 0) return (body as any).data
      // 后端未登录返回的是 HTTP 200 + body code 401，必须在这里识别，
      // 否则游客的登录态永远不被清理、也永远不会被引导去登录
      if (code === 401) {
        handleUnauthorized()
        return Promise.reject(new ApiError(msg === '请求失败' ? '未登录或登录已过期' : msg, 401))
      }
      return Promise.reject(new ApiError(msg, response.status))
    }
    return body
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      handleUnauthorized()
    }
    const msg = error?.response?.data?.message || error?.response?.data?.msg || error.message || '网络异常'
    return Promise.reject(new ApiError(msg, status))
  }
)

/** 未登录 / 登录过期：清理本地登录态，跳转登录页并携带回跳地址 */
function handleUnauthorized() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  const current = window.location.pathname + window.location.search
  if (window.location.pathname !== '/login') {
    window.location.assign(`/login?redirect=${encodeURIComponent(current)}`)
  }
}

export function get<T = any>(
  url: string,
  params?: Record<string, any>,
  config?: AxiosRequestConfig
): Promise<T> {
  return http.get(url, { params, ...(config || {}) }) as unknown as Promise<T>
}
export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return http.post(url, data, config) as unknown as Promise<T>
}
export function put<T = any>(url: string, data?: any): Promise<T> {
  return http.put(url, data) as unknown as Promise<T>
}
export function del<T = any>(url: string, params?: Record<string, any>): Promise<T> {
  return http.delete(url, { params }) as unknown as Promise<T>
}

export default http
