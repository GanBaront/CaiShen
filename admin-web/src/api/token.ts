const TOKEN_KEY = 'cs_admin_token'
const ADMIN_KEY = 'cs_admin_info'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}
export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}
export function getAdmin(): AdminInfo | null {
  const raw = localStorage.getItem(ADMIN_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AdminInfo
  } catch {
    return null
  }
}
export function setAdmin(info: AdminInfo | null) {
  if (info) localStorage.setItem(ADMIN_KEY, JSON.stringify(info))
  else localStorage.removeItem(ADMIN_KEY)
}
export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ADMIN_KEY)
}

export interface AdminInfo {
  id?: number
  username: string
  realName?: string
  real_name?: string
}

export interface LoginResult {
  token: string
  admin?: AdminInfo
  username?: string
}
