<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type Link = { label: string; to?: string; admin?: boolean }

const route = useRoute()
const router = useRouter()

/** 管理端地址：默认本地 5174，可用 VITE_ADMIN_URL 覆盖 */
const ADMIN_URL = (import.meta as any).env?.VITE_ADMIN_URL || 'http://localhost:5174'

// 页脚右侧链接按设计图逐页区分
const linkMap: Record<string, Link[]> = {
  home: [{ label: '意见反馈', to: '/profile' }, { label: '会员说明', to: '/member' }, { label: '商家/管理入口', admin: true }],
  recipes: [{ label: '意见反馈', to: '/profile' }, { label: '会员说明', to: '/member' }],
  'recipe-detail': [{ label: '意见反馈', to: '/profile' }, { label: '会员说明', to: '/member' }],
  'shopping-list': [{ label: '意见反馈', to: '/profile' }, { label: '商家/管理入口', admin: true }],
  member: [{ label: '意见反馈', to: '/profile' }, { label: '返回首页', to: '/' }],
  profile: [{ label: '会员说明', to: '/member' }, { label: '返回首页', to: '/' }]
}

const links = computed<Link[]>(() => linkMap[String(route.name)] || [{ label: '意见反馈', to: '/profile' }])

function onLink(l: Link) {
  if (l.admin) {
    window.open(ADMIN_URL, '_blank')
    return
  }
  if (l.to) router.push(l.to)
}
</script>

<template>
  <footer class="footer">
    <div>©材神食验室 · 让每一份食材都不被辜负</div>
    <div class="links">
      <a v-for="l in links" :key="l.label" @click="onLink(l)">{{ l.label }}</a>
    </div>
  </footer>
</template>
