import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 写操作（保存偏好 / 收藏 / 下单 / 评价 / AI 生成 等）前的登录校验。
 * 未登录时跳转到登录页并带回跳地址，返回 false。
 */
export function useRequireLogin() {
  const router = useRouter()
  const route = useRoute()
  const user = useUserStore()

  return function ensureLogin(): boolean {
    if (user.isLoggedIn) return true
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return false
  }
}
