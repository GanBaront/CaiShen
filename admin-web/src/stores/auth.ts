import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout } from '@/api/auth'
import { clearAuth, getAdmin, getToken, setAdmin, setToken, type AdminInfo } from '@/api/token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const admin = ref<AdminInfo | null>(getAdmin())
  const isLoggedIn = computed(() => !!token.value)

  async function login(username: string, password: string) {
    const res = await apiLogin(username, password)
    if (!res.token) throw new Error('登录失败：未返回 token')
    token.value = res.token
    setToken(res.token)
    const info: AdminInfo = res.admin ?? { username: res.username || username, realName: '超级管理员' }
    admin.value = info
    setAdmin(info)
  }

  async function logout() {
    await apiLogout()
    token.value = ''
    admin.value = null
    clearAuth()
  }

  return { token, admin, isLoggedIn, login, logout }
})
