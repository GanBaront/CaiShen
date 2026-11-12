import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import * as userApi from '@/api/user'
import { TOKEN_KEY, USER_KEY } from '@/api/request'

function safeParse(raw: string | null): any {
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<any>(safeParse(localStorage.getItem(USER_KEY)))

  // 免费次数：接口未返回前不展示写死数值（quotaLoaded=false 时页面显示加载中）
  const quotaUsed = ref<number>(0)
  const quotaTotal = ref<number>(0)
  /** 剩余可用次数（每周免费剩余 + 未使用的单次余额） */
  const quotaRemaining = ref<number>(0)
  const quotaLoaded = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const nickname = computed(() => user.value?.nickname || '')
  const isMember = computed(() => user.value?.member === true)
  const memberLabel = computed(() => (isMember.value ? '会员用户' : '免费用户'))
  /** 会员到期时间原始值（后端 LocalDateTime） */
  const memberEndTime = computed<string>(() => user.value?.memberEndTime || '')
  /** 会员到期文案：2026年11月18日到期 */
  const memberExpireText = computed(() => {
    const raw = memberEndTime.value
    if (!raw) return ''
    const d = new Date(String(raw).replace(' ', 'T'))
    if (isNaN(d.getTime())) return ''
    return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日到期`
  })

  function setSession(t: string, u: any) {
    token.value = t
    user.value = u || null
    localStorage.setItem(TOKEN_KEY, t)
    if (u) localStorage.setItem(USER_KEY, JSON.stringify(u))
    else localStorage.removeItem(USER_KEY)
  }

  function clearSession() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  async function login(account: string, password: string, captchaId?: string, captchaCode?: string) {
    const res: any = await authApi.login({ account, password, captchaId: captchaId || '', captchaCode: captchaCode || '' })
    setSession(res?.token || '', res?.user)
    return res
  }

  async function register(account: string, password: string, captchaId?: string, captchaCode?: string) {
    const res: any = await authApi.register({ account, password, captchaId: captchaId || '', captchaCode: captchaCode || '' })
    if (res?.token) setSession(res.token, res.user)
    return res
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      /* 后端不可用时忽略 */
    }
    clearSession()
  }

  async function fetchMe() {
    if (!isLoggedIn.value) return
    try {
      const me = await authApi.me()
      if (me) {
        user.value = me
        localStorage.setItem(USER_KEY, JSON.stringify(me))
      }
    } catch {
      /* 降级：沿用本地缓存 */
    }
  }

  async function fetchQuota() {
    if (!isLoggedIn.value) return
    try {
      const q: any = await userApi.getFreeQuota()
      if (q && typeof q.used === 'number') {
        // 后端字段：used=本周已用，limit=每周上限，remaining=剩余（免费剩余+单次余额）
        quotaUsed.value = q.used
        quotaTotal.value = typeof q.limit === 'number' ? q.limit : quotaTotal.value
        quotaRemaining.value =
          typeof q.remaining === 'number' && q.remaining !== Number.MAX_VALUE
            ? q.remaining
            : Math.max(0, quotaTotal.value - quotaUsed.value)
        quotaLoaded.value = true
      }
    } catch {
      /* 接口失败：保持未加载状态，页面显示加载中而不是假的 3/5 */
    }
  }

  return {
    token,
    user,
    quotaUsed,
    quotaTotal,
    quotaRemaining,
    quotaLoaded,
    isLoggedIn,
    nickname,
    isMember,
    memberLabel,
    memberEndTime,
    memberExpireText,
    login,
    register,
    logout,
    fetchMe,
    fetchQuota
  }
})
