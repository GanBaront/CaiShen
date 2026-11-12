<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import * as authApi from '@/api/auth'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const tab = ref<'login' | 'register'>('login')
const account = ref('')
const password = ref('')
const confirmPassword = ref('')
const captchaId = ref('')
const captchaImage = ref('')
const captchaCode = ref('')
const agree = ref(true)
const loading = ref(false)
const error = ref('')
const passwordVisible = ref(false)
const confirmVisible = ref(false)

const buttonText = computed(() => (tab.value === 'login' ? '登录' : '注册'))

// 后台是独立工程（admin-web，dev 端口 5174），
// 按当前访问主机推导地址，避免写死 localhost 导致换机器/局域网访问失效
const adminUrl = `${window.location.protocol}//${window.location.hostname}:5174`

function switchTab(t: 'login' | 'register') {
  if (tab.value === t) return
  tab.value = t
  error.value = ''
  confirmPassword.value = ''
  captchaCode.value = ''
}

async function refreshCaptcha() {
  captchaCode.value = ''
  try {
    const c = await authApi.getCaptcha()
    captchaId.value = c?.captchaId || ''
    captchaImage.value = c?.image || ''
  } catch {
    captchaImage.value = ''
  }
}

onMounted(refreshCaptcha)

async function onSubmit() {
  error.value = ''
  if (!account.value.trim() || !password.value) {
    error.value = '请输入账号和密码'
    return
  }
  if (tab.value === 'register' && confirmPassword.value !== password.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  if (!captchaCode.value.trim()) {
    error.value = '请输入图形验证码'
    return
  }
  if (!agree.value) {
    error.value = '请先阅读并同意《用户协议》和《隐私政策》'
    return
  }
  loading.value = true
  try {
    if (tab.value === 'login')
      await user.login(account.value.trim(), password.value, captchaId.value, captchaCode.value.trim())
    else await user.register(account.value.trim(), password.value, captchaId.value, captchaCode.value.trim())
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: any) {
    error.value = e?.message || '操作失败，请稍后重试'
    // 验证码一次性使用，失败后刷新换新
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page login-page">
    <div class="login">
      <!-- 左侧品牌区 -->
      <div class="brand">
        <div class="logo">
          <span class="mark">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M20 4c0 8-4 13-12 14 0-8 4-13 12-14z" fill="#fff" />
              <path
                d="M4 20c2-6 5-9 10-11"
                stroke="#1F4E3B"
                stroke-width="1.8"
                stroke-linecap="round"
              />
            </svg>
          </span>
          材神食验室
        </div>
        <div class="mid">
          <h1>告诉我冰箱里有什么，<br />今晚吃什么我来想。</h1>
          <p class="sub">登录后可保存口味偏好、忌口与过敏源，收藏菜谱、同步购物清单。</p>
          <div class="tags">
            <span>上传图片识食材</span><span>忌口自动过滤</span><span>分步烹饪教学</span
            ><span>多商家比价买菜</span>
          </div>
        </div>
        <div class="foot">让每一份食材都不被辜负</div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-side">
        <div class="form-card">
          <h1>欢迎回来</h1>
          <p class="sub">
            没有账号？试试<span class="hl" @click="switchTab('register')">注册</span>吧。
          </p>
          <div class="roles">
            <div class="role" :class="{ on: tab === 'login' }" @click="switchTab('login')">登录</div>
            <div class="role" :class="{ on: tab === 'register' }" @click="switchTab('register')">注册</div>
          </div>
          <div class="field">
            <label>账号</label>
            <input v-model="account" type="text" placeholder="请输入账号 / 手机号" />
          </div>
          <div class="field">
            <label>密码</label>
            <div class="pwd-wrap">
              <input
                v-model="password"
                :type="passwordVisible ? 'text' : 'password'"
                placeholder="请输入密码"
                @keyup.enter="onSubmit"
              />
              <span class="eye" :title="passwordVisible ? '隐藏密码' : '显示密码'" @click="passwordVisible = !passwordVisible">
                <!-- 睁眼 / 闭眼 -->
                <svg v-if="passwordVisible" width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z" stroke="currentColor" stroke-width="1.7" />
                  <circle cx="12" cy="12" r="2.6" stroke="currentColor" stroke-width="1.7" />
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z" stroke="currentColor" stroke-width="1.7" />
                  <circle cx="12" cy="12" r="2.6" stroke="currentColor" stroke-width="1.7" />
                  <path d="M4 4l16 16" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                </svg>
              </span>
            </div>
          </div>
          <div v-if="tab === 'register'" class="field">
            <label>确认密码</label>
            <div class="pwd-wrap">
              <input
                v-model="confirmPassword"
                :type="confirmVisible ? 'text' : 'password'"
                placeholder="请再次输入密码"
                @keyup.enter="onSubmit"
              />
              <span class="eye" :title="confirmVisible ? '隐藏密码' : '显示密码'" @click="confirmVisible = !confirmVisible">
                <svg v-if="confirmVisible" width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z" stroke="currentColor" stroke-width="1.7" />
                  <circle cx="12" cy="12" r="2.6" stroke="currentColor" stroke-width="1.7" />
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z" stroke="currentColor" stroke-width="1.7" />
                  <circle cx="12" cy="12" r="2.6" stroke="currentColor" stroke-width="1.7" />
                  <path d="M4 4l16 16" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                </svg>
              </span>
            </div>
          </div>
          <div class="field">
            <label>图形验证码</label>
            <div class="captcha-row">
              <input
                v-model="captchaCode"
                type="text"
                maxlength="4"
                placeholder="请输入右侧验证码"
                @keyup.enter="onSubmit"
              />
              <img
                v-if="captchaImage"
                :src="captchaImage"
                class="captcha-img"
                title="点击刷新验证码"
                alt="图形验证码"
                @click="refreshCaptcha"
              />
              <span v-else class="captcha-img captcha-fallback" @click="refreshCaptcha">点击刷新</span>
            </div>
          </div>
          <div class="agree" @click="agree = !agree">
            <span class="box" :class="{ off: !agree }">{{ agree ? '✓' : '' }}</span>
            我已阅读并同意 <a>《用户协议》</a> 和 <a>《隐私政策》</a>
          </div>
          <p v-if="error" class="err">{{ error }}</p>
          <button class="btn btn-primary btn-block submit" :disabled="loading" @click="onSubmit">
            {{ loading ? '处理中…' : buttonText }}
          </button>
        </div>
        <div class="admin-entry">
          管理员请从<a class="hl" :href="adminUrl" target="_blank">后台入口</a>登录
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  background: #fff;
  display: block;
}
.login {
  display: flex;
  width: 1440px;
  min-height: 900px;
}
.brand {
  width: 50%;
  background: var(--primary);
  color: #fff;
  padding: 56px 64px;
  display: flex;
  flex-direction: column;
}
.brand .logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 17px;
}
.brand .logo .mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand .mid {
  margin-top: 180px;
}
.brand h1 {
  font-size: 40px;
  line-height: 1.28;
  margin: 0 0 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.brand .sub {
  color: #cfe0d6;
  font-size: 15px;
  margin: 0 0 34px;
  max-width: 520px;
}
.brand .tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.brand .tags span {
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: 999px;
  padding: 7px 16px;
  font-size: 13px;
  color: #e4efe8;
}
.brand .foot {
  margin-top: auto;
  color: #b9d2c4;
  font-size: 13px;
}
.form-side {
  width: 50%;
  background: var(--cream);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
}
.form-card {
  width: 392px;
}
.form-card h1 {
  font-size: 30px;
  margin: 0 0 10px;
  font-weight: 700;
}
.form-card .sub {
  color: var(--text2);
  font-size: 13px;
  margin: 0 0 30px;
}
.hl {
  color: var(--primary);
  font-weight: 600;
  cursor: pointer;
}
.field {
  margin-bottom: 18px;
}
.field label {
  display: block;
  font-size: 13px;
  color: var(--text2);
  margin-bottom: 8px;
}
.field input {
  width: 100%;
  height: 46px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  padding: 0 14px;
  font-size: 14px;
  color: var(--text);
  outline: none;
}
.field input::placeholder {
  color: var(--text3);
}
.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.captcha-row input {
  flex: 1;
  height: 46px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  padding: 0 14px;
  font-size: 14px;
  color: var(--text);
  outline: none;
}
.captcha-row input::placeholder {
  color: var(--text3);
}
.captcha-img {
  height: 46px;
  width: 122px;
  border-radius: 8px;
  border: 1px solid var(--border);
  cursor: pointer;
  flex: none;
  object-fit: cover;
}
.captcha-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text2);
  background: #fff;
}
.pwd-wrap {
  position: relative;
}
.pwd-wrap input {
  padding-right: 42px;
}
.pwd-wrap .eye {
  position: absolute;
  right: 13px;
  top: 50%;
  transform: translateY(-50%);
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text3);
  cursor: pointer;
  user-select: none;
}
.pwd-wrap .eye:hover {
  color: var(--text2);
}
.agree {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
  color: var(--text2);
  margin: 6px 0 20px;
  cursor: pointer;
}
.agree .box {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  flex: none;
}
.agree .box.off {
  background: #fff;
  border: 1px solid var(--border);
}
.agree a {
  color: var(--primary);
  font-weight: 600;
}
.err {
  color: var(--danger);
  font-size: 12px;
  margin: -8px 0 12px;
}
.submit {
  height: 46px;
}
.admin-entry {
  position: absolute;
  bottom: 56px;
  left: 0;
  width: 100%;
  text-align: center;
  color: var(--text2);
  font-size: 13px;
}
.roles {
  display: flex;
  border-bottom: 1px solid var(--border);
  margin-bottom: 26px;
}
.role {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  font-size: 14px;
  color: var(--text2);
  position: relative;
  cursor: pointer;
}
.role.on {
  color: var(--primary);
  font-weight: 600;
}
.role.on:after {
  content: "";
  position: absolute;
  left: 22%;
  right: 22%;
  bottom: -1px;
  height: 2px;
  background: var(--primary);
  border-radius: 2px;
}
</style>
