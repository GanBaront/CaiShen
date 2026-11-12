<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const menus = [
  { label: '首页', to: '/' },
  { label: 'AI 识别', to: '/ai' },
  { label: '菜谱推荐', to: '/recipes' },
  { label: '购物清单', to: '/shopping-list' },
  { label: '会员', to: '/member' },
  { label: '我的订单', to: '/orders' },
  { label: '个人中心', to: '/profile' }
]

function isActive(to: string) {
  if (to === '/') return route.path === '/'
  if (to === '/recipes') return route.path.startsWith('/recipes')
  return route.path.startsWith(to)
}

const quotaText = computed(() => {
  if (user.isMember) return '会员 · 不限次'
  const base = `本周免费 ${user.quotaUsed}/${user.quotaTotal} 次`
  // 单次余额 = 总剩余 - 免费剩余，购买「单次体验」后在此展示
  const freeRemaining = Math.max(0, user.quotaTotal - user.quotaUsed)
  const single = Math.max(0, user.quotaRemaining - freeRemaining)
  return single > 0 ? `${base} · 单次可用 ${single} 次` : base
})
const rightLabel = computed(() => {
  if (!user.isLoggedIn) return '登录/注册'
  if (route.path.startsWith('/profile')) return '退出登录'
  return '我的'
})

function onRight() {
  if (!user.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (rightLabel.value === '退出登录') {
    logoutConfirm.value = true
    return
  }
  router.push('/profile')
}

// 退出登录确认弹窗
const logoutConfirm = ref(false)
const loggingOut = ref(false)

async function confirmLogout() {
  loggingOut.value = true
  try {
    await user.logout()
  } finally {
    loggingOut.value = false
    logoutConfirm.value = false
    router.push('/login')
  }
}

// 每次进入页面刷新登录态，保证会员身份、到期时间与免费次数准确
onMounted(() => {
  if (!user.isLoggedIn) return
  user.fetchMe()
  user.fetchQuota()
})
</script>

<template>
  <header class="nav">
    <div class="nav-left">
      <div class="nav-logo" @click="router.push('/')">
        <span class="mark">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M20 4c0 8-4 13-12 14 0-8 4-13 12-14z" fill="#fff" />
            <path d="M4 20c2-6 5-9 10-11" stroke="#1F4E3B" stroke-width="1.8" stroke-linecap="round" />
          </svg>
        </span>
        材神食验室
      </div>
      <nav class="nav-menu">
        <a
          v-for="m in menus"
          :key="m.to"
          :class="{ active: isActive(m.to) }"
          @click="router.push(m.to)"
          >{{ m.label }}</a
        >
      </nav>
    </div>
    <div class="nav-right">
      <div v-if="user.isLoggedIn" class="member-box">
        <template v-if="user.isMember">
          <span class="member-tag">会员用户</span>
          <span v-if="user.memberExpireText" class="member-exp">{{ user.memberExpireText }}</span>
        </template>
        <span v-else class="quota">{{ quotaText }}</span>
      </div>
      <button class="btn btn-sm" :class="user.isLoggedIn ? 'btn-ghost' : 'btn-outline'" @click="onRight">
        {{ rightLabel }}
      </button>
    </div>

    <!-- 退出登录确认弹窗 -->
    <div v-if="logoutConfirm" class="modal-mask" @click.self="logoutConfirm = false">
      <div class="modal">
        <h3>退出登录</h3>
        <p>确定要退出当前账号吗？</p>
        <div class="modal-acts">
          <button class="btn btn-ghost" @click="logoutConfirm = false">取消</button>
          <button class="btn btn-primary" :disabled="loggingOut" @click="confirmLogout">
            {{ loggingOut ? '退出中…' : '确认退出' }}
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
/* 登录态右侧：会员标识 + 到期小字 */
.member-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}
.member-tag {
  background: var(--primary);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 999px;
  line-height: 1.2;
}
.member-exp {
  color: var(--text3);
  font-size: 11px;
  line-height: 1.2;
}
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.modal {
  width: 340px;
  max-width: calc(100vw - 48px);
  background: #fff;
  border-radius: 14px;
  padding: 20px 22px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}
.modal h3 {
  margin: 0 0 8px;
  font-size: 16px;
}
.modal p {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--text2);
}
.modal-acts {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
