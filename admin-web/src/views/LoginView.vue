<template>
  <div class="login">
    <div class="brand-side">
      <div class="logo">
        <span class="mark">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M20 4c0 8-4 13-12 14 0-8 4-13 12-14z" fill="#fff" />
            <path d="M4 20c2-6 5-9 10-11" stroke="#1F4E3B" stroke-width="1.8" stroke-linecap="round" />
          </svg>
        </span>
        材神食验室
      </div>
      <div class="mid">
        <h1>运营后台，<br>让每一份食材都不被辜负。</h1>
        <p class="sub">在这里管理菜谱与食材、审核商家与评论、查看经营数据与用户反馈。</p>
        <div class="tags">
          <span>内容管理</span><span>审核与反馈</span><span>用户与会员</span><span>经营数据</span>
        </div>
      </div>
      <div class="foot">材神食验室 · 运营后台</div>
    </div>

    <div class="form-side">
      <form class="form-card" @submit.prevent="onSubmit">
        <h1>材神食验室 · 运营后台</h1>
        <p class="sub">请使用管理员账号登录</p>
        <div v-if="error" class="err">{{ error }}</div>
        <div class="field">
          <label>账号</label>
          <input v-model="username" type="text" placeholder="请输入管理员账号" autocomplete="username" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <button class="btn btn-primary btn-block" style="height: 46px" :disabled="loading" type="submit">
          {{ loading ? '登录中…' : '登录' }}
        </button>
        <div class="hint">默认管理员账号：admin / admin123</div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function onSubmit() {
  if (!username.value || !password.value) {
    error.value = '请输入账号与密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    const redirect = (route.query.redirect as string) || '/admin/dashboard'
    router.push(redirect)
  } catch {
    error.value = '登录失败：账号或密码错误，或后端服务未启动'
  } finally {
    loading.value = false
  }
}
</script>
