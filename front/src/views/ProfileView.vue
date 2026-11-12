<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Chip from '@/components/Chip.vue'
import { useUserStore } from '@/stores/user'
import { useIngredientStore } from '@/stores/ingredients'
import { useRequireLogin } from '@/composables/useRequireLogin'
import * as userApi from '@/api/user'
import * as feedbackApi from '@/api/feedback'
import * as ingredientApi from '@/api/ingredient'
import * as recipeApi from '@/api/recipe'

const router = useRouter()
const user = useUserStore()
const ingredientStore = useIngredientStore()
const ensureLogin = useRequireLogin()

const menus = [
  { key: 'pref', label: '资料与饮食偏好' },
  { key: 'fav', label: '我的收藏' },
  { key: 'history', label: '历史记录' },
  { key: 'feedback', label: '意见反馈' }
]
const active = ref('pref')

const nickname = ref('')
const phone = ref('')
const avatarUrl = ref('')
const avatarInput = ref<HTMLInputElement | null>(null)
const avatarBusy = ref(false)

function pickAvatar() {
  if (avatarBusy.value) return
  if (!ensureLogin()) return
  avatarInput.value?.click()
}

async function onAvatarChosen(e: Event) {
  const files = (e.target as HTMLInputElement).files
  const file = files && files[0]
  ;(e.target as HTMLInputElement).value = ''
  if (!file) return
  avatarBusy.value = true
  try {
    const res: any = await ingredientApi.uploadImage(file)
    const url = res?.url
    if (!url) throw new Error('上传失败，请重试')
    await userApi.updateProfile({ avatar: url })
    avatarUrl.value = url
    // 同步本地登录态缓存
    if (user.user) {
      user.user.avatar = url
      localStorage.setItem('cs_user', JSON.stringify(user.user))
    }
    showPrefResult('头像已更新', true)
  } catch (err: any) {
    showPrefResult(err?.message || '头像更新失败，请稍后重试', false)
  } finally {
    avatarBusy.value = false
  }
}

const tasteOptions = ['酸', '甜', '辣', '咸', '清淡']
const dietOptions = ['不限', '素食', '生酮', '低卡']

const tastes = ref<string[]>(['清淡'])
const dietType = ref('不限')
const servings = ref(2)
const avoidFoods = ref<string[]>([])
const allergens = ref<string[]>([])

// 我的收藏完全以 /user/favorites 为准，不写死示例（新用户为空即显示空态）
const favorites = ref<any[]>([])

/** 取消收藏：从我的收藏中移除 */
async function removeFavorite(id: number | string) {
  if (!ensureLogin()) return
  const prev = favorites.value
  favorites.value = prev.filter((f) => f.id !== id)
  try {
    await recipeApi.unfavoriteRecipe(id)
  } catch (e: any) {
    /* 失败回滚，保持原收藏列表 */
    favorites.value = prev
    showPrefResult(e?.message || '取消收藏失败，请稍后重试', false)
  }
}

// 浏览历史
const histories = ref<any[]>([])

/** 后端 LocalDateTime ISO 串 → yyyy-MM-dd HH:mm */
function fmtTime(t?: string): string {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

function toggleTaste(t: string) {
  const i = tastes.value.indexOf(t)
  if (i >= 0) tastes.value.splice(i, 1)
  else tastes.value.push(t)
}

function onMenu(m: any) {
  if (m.to) {
    router.push(m.to)
    return
  }
  active.value = m.key
  const el = document.querySelector('.' + m.key + '-blk')
  el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function loadProfile() {
  try {
    const res: any = await userApi.getProfile()
    if (res) {
      nickname.value = res.nickname || ''
      phone.value = res.phone || ''
      avatarUrl.value = res.avatar || ''
    }
  } catch {
    /* 降级：沿用本地缓存 */
  }
  if (!nickname.value) nickname.value = user.user?.nickname || ''
  if (!avatarUrl.value) avatarUrl.value = user.user?.avatar || ''
}

const phoneError = ref('')
const basicSaving = ref(false)

/** 手机号格式校验：允许留空，填写则必须是 11 位大陆手机号 */
function validatePhone(p: string): string {
  const v = p.trim()
  if (!v) return ''
  return /^1[3-9]\d{9}$/.test(v) ? '' : '请输入正确的 11 位手机号（1 开头）'
}

async function saveBasic() {
  if (!ensureLogin()) return
  const err = validatePhone(phone.value)
  phoneError.value = err
  if (err) {
    showPrefResult(err, false)
    return
  }
  basicSaving.value = true
  try {
    const name = nickname.value.trim()
    const tel = phone.value.trim()
    await userApi.updateProfile({ nickname: name, phone: tel })
    nickname.value = name
    phone.value = tel
    if (user.user) {
      user.user.nickname = name
      user.user.phone = tel
      localStorage.setItem('cs_user', JSON.stringify(user.user))
    }
    showPrefResult('基本资料已保存', true)
  } catch (e: any) {
    showPrefResult(e?.message || '保存失败，请稍后重试', false)
  } finally {
    basicSaving.value = false
  }
}

async function loadPreference() {
  try {
    const res: any = await userApi.getPreference()
    if (res) {
      if (Array.isArray(res.tastes)) tastes.value = res.tastes
      if (res.dietType) dietType.value = res.dietType
      if (res.defaultServings) servings.value = res.defaultServings
      // 忌口 / 过敏源以服务端为准：即使为空也覆盖本地默认值，
      // 否则用户删除后的空列表会被设计图默认的「香菜/花生」顶回去
      if (Array.isArray(res.avoidItems)) avoidFoods.value = res.avoidItems
      if (Array.isArray(res.allergenItems)) allergens.value = res.allergenItems
    }
  } catch {
    /* 降级：保留当前本地偏好 */
  }
}

// 页内添加弹窗（替代浏览器 prompt）
const addModal = ref<{ type: 'avoid' | 'allergen'; title: string } | null>(null)
const addModalName = ref('')

function openAdd(type: 'avoid' | 'allergen') {
  addModal.value = { type, title: type === 'avoid' ? '添加忌口食材' : '添加过敏源' }
  addModalName.value = ''
}

function confirmAdd() {
  if (!addModal.value) return
  const n = addModalName.value.trim()
  if (!n) return
  if (addModal.value.type === 'avoid') {
    if (!avoidFoods.value.includes(n)) avoidFoods.value.push(n)
  } else if (!allergens.value.includes(n)) {
    allergens.value.push(n)
  }
  addModal.value = null
}

// 保存偏好结果弹窗（成功 / 失败）
const prefResult = ref<{ ok: boolean; msg: string } | null>(null)

function showPrefResult(msg: string, ok: boolean) {
  prefResult.value = { ok, msg }
}

async function savePreference() {
  if (!ensureLogin()) return
  try {
    await userApi.updatePreference({
      tastes: tastes.value,
      dietType: dietType.value,
      defaultServings: servings.value,
      avoidItems: avoidFoods.value,
      allergenItems: allergens.value
    })
    // 同步忌口到 ingredient store，推荐/详情页的忌口过滤立即用最新数据
    ingredientStore.fetchAvoid()
    showPrefResult('你的饮食偏好已保存', true)
  } catch (e: any) {
    showPrefResult(e?.message || '保存失败，请稍后重试', false)
  }
}

function cancelPreference() {
  loadPreference()
}

// 意见反馈
const feedbackContent = ref('')
const feedbackContact = ref('')
const feedbackList = ref<any[]>([])
const feedbackSubmitting = ref(false)
const feedbackMsg = ref('')

async function submitFeedback() {
  feedbackMsg.value = ''
  if (!feedbackContent.value.trim()) {
    feedbackMsg.value = '请填写反馈内容'
    return
  }
  if (!ensureLogin()) return
  feedbackSubmitting.value = true
  try {
    await feedbackApi.submitFeedback({
      content: feedbackContent.value.trim(),
      contact: feedbackContact.value.trim() || undefined
    })
    feedbackContent.value = ''
    feedbackContact.value = ''
    feedbackMsg.value = '提交成功，感谢您的反馈！'
    await loadFeedback()
  } catch (e: any) {
    feedbackMsg.value = e?.message || '提交失败，请稍后重试'
  } finally {
    feedbackSubmitting.value = false
  }
}

async function loadFeedback() {
  try {
    const res: any = await feedbackApi.myFeedback()
    const list = res?.records || res
    if (Array.isArray(list)) feedbackList.value = list
  } catch {
    /* 降级：忽略 */
  }
}

async function loadHistory() {
  try {
    const res: any = await userApi.getHistory()
    const list = res?.records || res
    if (Array.isArray(list)) {
      histories.value = list.map((r: any, i: number) => ({
        id: r.id ?? i,
        name: r.name,
        meta: `${r.cookTime ? r.cookTime + ' 分钟' : ''}${r.difficulty ? ' · ' + r.difficulty : ''}`
      }))
    }
  } catch {
    /* 降级：历史为空即可 */
  }
}

onMounted(async () => {
  await user.fetchMe()
  // 后端不可用时用缓存资料兜底，保持与设计图一致的展示
  loadProfile()
  loadPreference()
  loadFeedback()
  loadHistory()
  try {
    const res: any = await userApi.getFavorites()
    const list = res?.records || res
    if (Array.isArray(list)) {
      favorites.value = list.map((f: any, i: number) => ({
        id: f.id ?? i,
        thumb: '图',
        name: f.name,
        meta: `${f.cookTime ? f.cookTime + ' 分钟' : ''}${f.difficulty ? ' · ' + f.difficulty : ''}`
      }))
    }
  } catch {
    /* 接口失败：收藏保持为空，由空态提示 */
  }
})
</script>

<template>
  <div class="profile sec">
    <!-- 左栏 -->
    <aside class="side">
      <div class="me">
        <div class="avatar-wrap" title="点击更换头像" @click="pickAvatar">
          <img v-if="avatarUrl" :src="avatarUrl" class="avatar avatar-img" alt="头像" />
          <div v-else class="avatar img-ph"></div>
          <span class="avatar-edit">更换</span>
        </div>
        <input ref="avatarInput" type="file" accept="image/*" style="display: none" @change="onAvatarChosen" />
        <div class="nick">{{ nickname || '[昵称]' }}</div>
        <div class="role">{{ user.memberLabel }}</div>
        <div v-if="user.isMember && user.memberExpireText" class="role-exp">{{ user.memberExpireText }}</div>
        <button class="btn btn-primary btn-sm btn-block" @click="router.push('/member')">
          {{ user.isMember ? '续费' : '开通会员' }}
        </button>
      </div>
      <div class="menu">
        <a
          v-for="m in menus"
          :key="m.key"
          :class="{ on: active === m.key }"
          @click="onMenu(m)"
          >{{ m.label }}</a
        >
      </div>
    </aside>

    <!-- 右主区 -->
    <div class="stack">
      <!-- 基本资料 -->
      <div class="blk pref-blk">
        <div class="bh"><h2>基本资料</h2><span class="r">昵称与手机号用于展示和联系</span></div>
        <div class="grid2">
          <div class="fld">
            <label>昵称</label>
            <input v-model="nickname" placeholder="请输入昵称" maxlength="20" />
          </div>
          <div class="fld">
            <label>手机号</label>
            <input
              v-model="phone"
              class="tel-input"
              :class="{ invalid: !!phoneError }"
              placeholder="请输入 11 位手机号"
              maxlength="11"
              inputmode="numeric"
              @input="phoneError = validatePhone(phone)"
            />
            <p v-if="phoneError" class="fld-err">{{ phoneError }}</p>
          </div>
        </div>
        <div class="pref-acts">
          <button class="btn btn-primary" :disabled="basicSaving" @click="saveBasic">
            {{ basicSaving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>

      <!-- 饮食偏好 -->
      <div class="blk pref-blk">
        <div class="bh"><h2>饮食偏好</h2><span class="r">推荐与筛选时会自动应用</span></div>
        <div class="pref-row">
          <div class="lbl">口味</div>
          <div class="chips" style="margin: 0">
            <Chip
              v-for="t in tasteOptions"
              :key="t"
              :variant="tastes.includes(t) ? 'sel' : 'off'"
              :label="t"
              @click="toggleTaste(t)"
            />
          </div>
        </div>
        <div class="two-col">
          <div class="pref-row" style="margin-bottom: 0">
            <div class="lbl">饮食方式</div>
            <div class="select-wrap">
              <select v-model="dietType" class="select">
                <option v-for="d in dietOptions" :key="d" :value="d">{{ d }}</option>
              </select>
              <span class="caret">▾</span>
            </div>
          </div>
          <div class="pref-row" style="margin-bottom: 0">
            <div class="lbl">默认用餐人数</div>
            <div class="count">
              <button class="btn-sq" @click="servings = Math.max(1, servings - 1)">−</button>
              <span class="num">{{ servings }} 人</span>
              <button class="btn-sq" @click="servings = servings + 1">+</button>
            </div>
          </div>
        </div>
        <div class="pref-row" style="margin-top: 16px">
          <div class="lbl">忌口食材</div>
          <div class="chips" style="margin: 0">
            <Chip
              v-for="a in avoidFoods"
              :key="a"
              :label="a"
              closable
              @close="avoidFoods = avoidFoods.filter((x) => x !== a)"
            />
            <Chip variant="add" label="+ 添加" @click="openAdd('avoid')" />
          </div>
        </div>
        <div class="pref-row" style="margin-bottom: 0">
          <div class="lbl">过敏源</div>
          <div class="chips" style="margin: 0">
            <Chip
              v-for="a in allergens"
              :key="a"
              :label="a"
              closable
              @close="allergens = allergens.filter((x) => x !== a)"
            />
            <Chip variant="add" label="+ 添加" @click="openAdd('allergen')" />
          </div>
        </div>
        <div class="pref-acts">
          <button class="btn btn-ghost" @click="cancelPreference">取消</button>
          <button class="btn btn-primary" @click="savePreference">保存偏好</button>
        </div>
      </div>

      <!-- 我的收藏 -->
      <div class="blk fav-blk">
        <div class="bh">
          <h2>我的收藏</h2>
          <a @click="router.push('/recipes')">查看全部 →</a>
        </div>
        <div class="favs">
          <div v-for="f in favorites" :key="f.id" class="fav">
            <div class="fav-inner" @click="router.push(`/recipes/${f.id}`)">
              <div class="thumb img-ph">{{ f.thumb }}</div>
              <div class="t">{{ f.name }}</div>
              <div class="m">{{ f.meta }}</div>
            </div>
            <button class="fav-cancel" title="取消收藏" @click.stop="removeFavorite(f.id)">
              ♥ 取消收藏
            </button>
          </div>
        </div>
        <div v-if="!favorites.length" class="empty-line">
          还没有收藏的菜谱，<a @click="router.push('/recipes')">去菜谱推荐看看 →</a>
        </div>
      </div>

      <!-- 历史记录 -->
      <div class="blk history-blk">
        <div class="bh">
          <h2>历史记录</h2>
          <span class="r">最近浏览过的菜谱</span>
        </div>
        <div v-if="!histories.length" class="empty-line">
          还没有浏览记录，<a @click="router.push('/recipes')">去看看菜谱 →</a>
        </div>
        <div v-else class="hist-list">
          <div
            v-for="h in histories"
            :key="h.id"
            class="hist-item"
            @click="router.push(`/recipes/${h.id}`)"
          >
            <span class="hist-nm">{{ h.name }}</span>
            <span class="hist-meta">{{ h.meta || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- 意见反馈 -->
      <div class="blk feedback-blk">
        <div class="bh">
          <h2>意见反馈</h2>
          <span class="r">我们会尽快处理你的建议</span>
        </div>
        <div class="fb-form">
          <textarea
            v-model="feedbackContent"
            class="fb-text"
            rows="4"
            placeholder="请描述你遇到的问题或建议，例如功能不好用、菜谱不准确等"
          ></textarea>
          <div class="fb-foot">
            <input v-model="feedbackContact" class="fb-contact" placeholder="联系方式（选填，便于我们回复你）" />
            <button class="btn btn-primary" :disabled="feedbackSubmitting" @click="submitFeedback">
              {{ feedbackSubmitting ? '提交中…' : '提交反馈' }}
            </button>
          </div>
          <p v-if="feedbackMsg" class="fb-msg">{{ feedbackMsg }}</p>
        </div>
        <div class="fb-list">
          <div class="fb-title">我的反馈</div>
          <div v-if="!feedbackList.length" class="empty-line">还没有提交过反馈</div>
          <div v-for="(f, i) in feedbackList" :key="f.id || i" class="fb-item">
            <div class="fb-content">{{ f.content }}</div>
            <div class="fb-meta">
              <span>{{ fmtTime(f.createTime) }}</span>
              <span>{{ f.status || '待处理' }}</span>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- 添加忌口 / 过敏源弹窗 -->
    <div v-if="addModal" class="modal-mask" @click.self="addModal = null">
      <div class="modal modal-add">
        <div class="modal-head">
          <h3>{{ addModal.title }}</h3>
          <span class="modal-close" @click="addModal = null">×</span>
        </div>
        <div class="modal-body">
          <input
            v-model="addModalName"
            class="modal-input"
            :placeholder="addModal.type === 'avoid' ? '输入要忌口的食材，如：香菜' : '输入过敏源，如：花生'"
            @keyup.enter="confirmAdd"
          />
        </div>
        <div class="modal-acts">
          <button class="btn btn-ghost" @click="addModal = null">取消</button>
          <button class="btn btn-primary" @click="confirmAdd">添加</button>
        </div>
      </div>
    </div>

    <!-- 保存偏好 / 头像更新结果弹窗 -->
    <div v-if="prefResult" class="modal-mask" @click.self="prefResult = null">
      <div class="modal modal-result">
        <div class="res-icon" :class="prefResult.ok ? 'ok' : 'err'">
          <svg v-if="prefResult.ok" width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M5 12.5 10 17.5 19 7" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path d="M6 6l12 12M18 6L6 18" stroke="#fff" stroke-width="2.4" stroke-linecap="round" />
          </svg>
        </div>
        <h3>{{ prefResult.ok ? '保存成功' : '操作失败' }}</h3>
        <p class="res-msg">{{ prefResult.msg }}</p>
        <button class="btn btn-primary btn-block" @click="prefResult = null">确定</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile {
  padding-top: 26px;
  padding-bottom: 40px;
  display: grid;
  grid-template-columns: 210px 1fr;
  gap: 28px;
  align-items: start;
}
.side .me {
  background: #fff;
  border-radius: 16px;
  box-shadow: var(--shadow);
  padding: 22px;
  text-align: center;
}
.side .avatar {
  width: 66px;
  height: 66px;
  border-radius: 50%;
  margin: 0 auto 12px;
}
.avatar-wrap {
  position: relative;
  width: 66px;
  margin: 0 auto 12px;
  cursor: pointer;
}
.avatar-wrap .avatar-img {
  width: 66px;
  height: 66px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}
.avatar-wrap .avatar-edit {
  position: absolute;
  left: 50%;
  bottom: -6px;
  transform: translateX(-50%);
  background: var(--primary);
  color: #fff;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 999px;
  white-space: nowrap;
  opacity: 0.9;
}
.side .nick {
  font-weight: 700;
  font-size: 15px;
}
.side .role {
  color: var(--text3);
  font-size: 12px;
  margin: 4px 0 14px;
}
.side .role-exp {
  color: var(--text3);
  font-size: 11px;
  margin: -10px 0 12px;
}
.side .menu {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.side .menu a {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--text2);
  cursor: pointer;
}
.side .menu a.on {
  background: var(--chip-bg);
  color: var(--chip-text);
  font-weight: 600;
}
.stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.blk .bh a {
  color: var(--primary);
  font-size: 13px;
  cursor: pointer;
}
.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.fld label {
  display: block;
  font-size: 12px;
  color: var(--text2);
  margin-bottom: 7px;
}
.fld input {
  width: 100%;
  height: 42px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0 12px;
  font-size: 13px;
  color: var(--text);
  outline: none;
  background: #fff;
}
.fld input::placeholder {
  color: var(--text3);
}
.fld input.tel-input {
  letter-spacing: 0.5px;
}
.fld input.invalid {
  border-color: var(--danger, #d64545);
}
.fld-err {
  color: var(--danger, #d64545);
  font-size: 12px;
  margin: 6px 0 0;
}
.pref-row {
  margin-bottom: 16px;
}
.pref-row .lbl {
  font-size: 12px;
  color: var(--text2);
  margin-bottom: 9px;
}
.select-wrap {
  position: relative;
}
.select-wrap select {
  width: 100%;
  appearance: none;
  -webkit-appearance: none;
  outline: none;
}
.select-wrap .caret {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text2);
  font-size: 12px;
  pointer-events: none;
}
.two-col {
  display: grid;
  grid-template-columns: 1fr 260px;
  gap: 24px;
  align-items: end;
}
.count {
  display: flex;
  align-items: center;
  gap: 14px;
  justify-content: flex-start;
}
.btn-sq {
  width: 34px;
  height: 34px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: var(--text2);
  cursor: pointer;
}
.count .num {
  font-size: 15px;
  font-weight: 600;
}
.pref-acts {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}
.favs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.fav {
  cursor: default;
}
.fav-inner {
  cursor: pointer;
}
.fav-inner:hover .t {
  color: var(--primary);
}
.fav .thumb {
  height: 104px;
  border-radius: 12px;
  margin-bottom: 12px;
}
.fav .t {
  font-size: 14px;
  font-weight: 600;
}
.fav .m {
  color: var(--text3);
  font-size: 12px;
  margin-top: 5px;
}
.fav-cancel {
  margin-top: 10px;
  width: 100%;
  height: 32px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  color: var(--danger, #d64545);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}
.fav-cancel:hover {
  border-color: var(--danger, #d64545);
  background: var(--danger-bg, #fdeeee);
}
.fb-text {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--text);
  outline: none;
  resize: vertical;
  font-family: inherit;
}
.fb-foot {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}
.fb-contact {
  flex: 1;
  height: 40px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0 12px;
  font-size: 13px;
  color: var(--text);
  outline: none;
}
.fb-msg {
  color: var(--text2);
  font-size: 12px;
  margin: 10px 0 0;
}
.fb-list {
  margin-top: 18px;
  border-top: 1px dashed var(--border);
  padding-top: 14px;
}
.fb-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}
.fb-item {
  padding: 10px 0;
  border-bottom: 1px dashed var(--border);
}
.fb-content {
  font-size: 13px;
  color: var(--text);
  white-space: pre-wrap;
}
.fb-meta {
  display: flex;
  justify-content: space-between;
  color: var(--text3);
  font-size: 12px;
  margin-top: 6px;
}
.empty-line {
  color: var(--text2);
  font-size: 13px;
  padding: 8px 0;
}
.empty-line a {
  color: var(--primary);
  cursor: pointer;
}

/* 历史记录 */
.hist-list {
  display: flex;
  flex-direction: column;
}
.hist-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 2px;
  border-bottom: 1px dashed var(--border);
  cursor: pointer;
}
.hist-item:last-child {
  border-bottom: none;
}
.hist-item:hover .hist-nm {
  color: var(--primary);
}
.hist-nm {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}
.hist-meta {
  font-size: 12px;
  color: var(--text3);
}

/* 意见反馈 */
.fb-form {
  margin-top: 4px;
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
  width: 420px;
  max-width: calc(100vw - 48px);
  background: #fff;
  border-radius: 14px;
  padding: 20px 22px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.modal-head h3 {
  font-size: 16px;
  margin: 0;
}
.modal-close {
  cursor: pointer;
  color: var(--text3);
  font-size: 14px;
}
.modal-body {
  font-size: 13px;
  color: var(--text);
}
.kv {
  display: flex;
  justify-content: space-between;
  padding: 7px 0;
}
.kv span {
  color: var(--text2);
}
.progress-track {
  height: 8px;
  background: var(--chip-bg, #eef2ee);
  border-radius: 999px;
  overflow: hidden;
  margin: 14px 0 8px;
}
.progress-bar {
  height: 100%;
  background: var(--primary);
  border-radius: 999px;
  transition: width 0.3s;
}
.progress-desc {
  color: var(--text2);
  font-size: 12px;
}
.stars {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
}
.star {
  font-size: 24px;
  color: #d8ddd9;
  cursor: pointer;
  line-height: 1;
}
.star.on {
  color: #f5a623;
}
.modal-input {
  width: 100%;
  height: 42px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0 12px;
  font-size: 13px;
  color: var(--text);
  outline: none;
}
.toast {
  position: fixed;
  top: 78px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 200;
  padding: 10px 22px;
  border-radius: 999px;
  font-size: 13px;
  color: #fff;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}
.toast-ok {
  background: var(--primary);
}
.toast-err {
  background: var(--danger);
}

/* 添加忌口 / 过敏源弹窗 */
.modal-add {
  width: 360px;
}
.modal-acts {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 18px;
}

/* 保存结果弹窗 */
.modal-result {
  width: 320px;
  text-align: center;
}
.modal-result h3 {
  font-size: 16px;
  margin: 0 0 6px;
}
.res-msg {
  font-size: 13px;
  color: var(--text2);
  margin: 0 0 16px;
}
.res-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 2px auto 12px;
}
.res-icon.ok {
  background: var(--success, #2f9e5f);
}
.res-icon.err {
  background: var(--danger, #d64545);
}
</style>
