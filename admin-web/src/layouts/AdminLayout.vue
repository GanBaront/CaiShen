<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="mark">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M20 4c0 8-4 13-12 14 0-8 4-13 12-14z" fill="#fff" />
            <path d="M4 20c2-6 5-9 10-11" stroke="#1F4E3B" stroke-width="1.8" stroke-linecap="round" />
          </svg>
        </span>
        <div>
          <div class="name">材神食验室</div>
          <div class="sub">运营后台</div>
        </div>
      </div>

      <template v-for="group in menu" :key="group.title || 'main'">
        <div v-if="group.title" class="nav-group-title">{{ group.title }}</div>
        <router-link
          v-for="item in group.items"
          :key="item.path"
          class="nav-item"
          :class="{ active: route.path === item.path }"
          :to="item.path"
        >
          {{ item.label }}
        </router-link>
      </template>
    </aside>

    <div class="main">
      <header class="topbar">
        <h2>{{ pageTitle }}</h2>
        <div class="topbar-right">
          <div class="range-switch">
            <button :class="{ on: ui.range === '7' }" @click="ui.setRange('7')">近 7 天</button>
            <button :class="{ on: ui.range === '30' }" @click="ui.setRange('30')">近 30 天</button>
          </div>
          <span class="avatar" :title="adminName">管</span>
          <button class="btn btn-ghost btn-sm" @click="logoutConfirm = true">退出</button>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>

    <!-- 退出登录确认弹窗 -->
    <div v-if="logoutConfirm" class="modal-mask" @click.self="logoutConfirm = false">
      <div class="modal modal-sm">
        <h3>退出登录</h3>
        <p class="modal-tip">确定要退出管理后台吗？</p>
        <div class="modal-foot">
          <button class="btn btn-ghost" @click="logoutConfirm = false">取消</button>
          <button class="btn btn-primary" :disabled="loggingOut" @click="onLogout">
            {{ loggingOut ? '退出中…' : '确认退出' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const ui = useUiStore()

const menu = [
  {
    title: '',
    items: [{ path: '/admin/dashboard', label: '数据概览' }],
  },
  {
    title: '内容管理',
    items: [
      { path: '/admin/recipes', label: '菜谱库' },
      { path: '/admin/ingredients', label: '食材库' },
      { path: '/admin/nutrition', label: '营养数据' },
      { path: '/admin/allergen-tags', label: '过敏源标签' },
    ],
  },
  {
    title: '用户与交易',
    items: [
      { path: '/admin/users', label: '用户管理' },
      { path: '/admin/members-orders', label: '会员与订单' },
      { path: '/admin/merchants', label: '商家审核' },
      { path: '/admin/products', label: '商品审核' },
    ],
  },
  {
    title: '审核与反馈',
    items: [
      { path: '/admin/comments', label: '评论审核' },
      { path: '/admin/feedback', label: '用户反馈' },
    ],
  },
  {
    title: '系统',
    items: [{ path: '/admin/config', label: '系统配置' }],
  },
]

const pageTitle = computed(() => (route.meta.title as string) || '数据概览')
const adminName = computed(() => auth.admin?.realName || auth.admin?.real_name || auth.admin?.username || '管理员')

const logoutConfirm = ref(false)
const loggingOut = ref(false)

async function onLogout() {
  loggingOut.value = true
  try {
    await auth.logout()
  } finally {
    loggingOut.value = false
    logoutConfirm.value = false
    router.push('/login')
  }
}
</script>

<style scoped>
.modal-sm {
  width: 360px;
}
.modal-tip {
  margin: 0;
  font-size: 13px;
  color: #667;
}
</style>
