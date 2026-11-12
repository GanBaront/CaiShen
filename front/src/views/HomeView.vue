<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Chip from '@/components/Chip.vue'
import RecipeCard from '@/components/RecipeCard.vue'
import { useIngredientStore } from '@/stores/ingredients'
import { useUserStore } from '@/stores/user'
import { useAiRecommendStore } from '@/stores/aiRecommend'
import { useRequireLogin } from '@/composables/useRequireLogin'
import * as recipeApi from '@/api/recipe'
import * as ingredientApi from '@/api/ingredient'

const router = useRouter()
const ingredient = useIngredientStore()
const user = useUserStore()
const aiRecommend = useAiRecommendStore()
const ensureLogin = useRequireLogin()

const inputText = ref('')
const tasteOptions = ['酸', '甜', '辣', '咸', '清淡']
const dietOptions = ['不限', '素食', '生酮', '低卡']

const imageInput = ref<HTMLInputElement | null>(null)
const imageStatus = ref('')
const imageBusy = ref(false)
// 识别到成品菜且站内有同款菜谱时，给出直达链接（避免「识别出来却不知道下一步」）
const imageHit = ref<{ id: number; name: string } | null>(null)

const listening = ref(false)
const voiceStatus = ref('')
// 浏览器语音识别实例（webkitSpeechRecognition，TS 无内置类型，用 any）
let recognition: any = null

// ===== 上传图片识别食材 =====
function pickImage() {
  if (imageBusy.value) return
  if (!ensureLogin()) return
  imageInput.value?.click()
}

async function onImageChosen(e: Event) {
  const files = (e.target as HTMLInputElement).files
  const file = files && files[0]
  ;(e.target as HTMLInputElement).value = ''
  if (!file) return
  imageStatus.value = '图片上传中…'
  imageBusy.value = true
  imageHit.value = null
  try {
    const res: any = await ingredientApi.uploadImage(file)
    const url = res?.url
    if (!url) {
      imageStatus.value = '上传失败，请重试'
      return
    }
    imageStatus.value = 'AI 识别中…'
    const rec: any = await ingredientApi.recognizeIngredients(url)
    // 新版返回 { ingredients, dishes, matchedRecipes }；兼容旧版纯数组
    const names: string[] = Array.isArray(rec) ? rec : (rec?.ingredients ?? [])
    const dishes: string[] = Array.isArray(rec?.dishes) ? rec.dishes : []
    const hit = Array.isArray(rec?.matchedRecipes) ? rec.matchedRecipes[0] : null
    if (hit?.id) imageHit.value = { id: hit.id, name: hit.name }
    if (names.length) {
      for (const n of names) await ingredient.add(n, 'image')
      imageStatus.value = dishes.length
        ? `识别到菜品：${dishes.join('、')}｜已添加食材：${names.join('、')}`
        : `识别成功：${names.join('、')}`
    } else if (dishes.length) {
      imageStatus.value = `识别到菜品：${dishes.join('、')}，未拆解出食材，请手动补充`
    } else {
      imageStatus.value = '未识别出食材，请换一张更清晰的图片'
    }
  } catch (err: any) {
    imageStatus.value = err?.message || '图片识别失败，请稍后重试'
  } finally {
    imageBusy.value = false
  }
}

// ===== 按住说话（浏览器 Web Speech API） =====
function startVoice(e: Event) {
  if (!ensureLogin()) return
  const SR: any =
    (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (!SR) {
    voiceStatus.value = '当前浏览器不支持语音识别，请用 Chrome / Edge 试试'
    return
  }
  if (listening.value) return
  recognition = new SR()
  recognition.lang = 'zh-CN'
  recognition.interimResults = false
  recognition.maxAlternatives = 1
  recognition.continuous = false
  let gotResult = false

  recognition.onstart = () => {
    listening.value = true
    voiceStatus.value = '正在聆听，说完成后松开鼠标…'
  }
  recognition.onerror = (ev: any) => {
    // 手动停止时浏览器会抛 aborted，静默忽略
    if (ev?.error === 'aborted') return
    listening.value = false
    voiceStatus.value =
      ev?.error === 'not-allowed' ? '麦克风权限被拒绝，请在浏览器设置中允许麦克风'
        : ev?.error === 'no-speech' ? '没有听清，请再按住说一次'
        : '语音识别失败，请重试'
  }
  recognition.onend = () => {
    listening.value = false
    if (!gotResult && voiceStatus.value.startsWith('正在聆听')) {
      voiceStatus.value = '没有听清，请再按住说一次'
    }
  }
  recognition.onresult = (ev: any) => {
    gotResult = true
    const text = String(ev?.results?.[0]?.[0]?.transcript || '').trim()
    if (!text) return
    // 识别结果填入「你有哪些食材」输入框，由用户确认后点「添加」
    inputText.value = text
    voiceStatus.value = `已识别：“${text}”，已填入食材输入框`
  }
  try {
    recognition.start()
    // 兜底：鼠标在卡片外松开也能结束识别
    document.addEventListener('mouseup', stopVoice)
  } catch {
    voiceStatus.value = '语音识别启动失败，请重试'
  }
}

function stopVoice() {
  document.removeEventListener('mouseup', stopVoice)
  if (!listening.value && !recognition) return
  try {
    recognition?.stop()
  } catch {
    /* 忽略 */
  }
}

// ===== 页内添加忌口弹窗（替代浏览器 prompt） =====
const avoidModal = ref(false)
const avoidModalName = ref('')

function openAvoidModal() {
  avoidModalName.value = ''
  avoidModal.value = true
}

function confirmAvoid() {
  const n = avoidModalName.value.trim()
  if (n) ingredient.addAvoid(n)
  avoidModal.value = false
}

// 今日推荐完全以 /recipes/today 为准，不写死示例
const todayRecipes = ref<any[]>([])
const todayErr = ref('')

function addInput() {
  const raw = inputText.value.trim()
  if (!raw) return
  raw
    .split(/[、,，\s]+/)
    .filter(Boolean)
    .forEach((n) => ingredient.add(n))
  inputText.value = ''
}

const genErr = ref('')
// AI 生成中状态（大模型排序需要数秒到数十秒）
const generating = ref(false)
// 次数用尽弹窗
const quotaModal = ref(false)
// 非会员剩余次数（每周免费剩余 + 未使用的单次余额）用尽：禁用生成按钮，不再发请求
const quotaExhausted = computed(
  () => user.isLoggedIn && user.quotaLoaded && !user.isMember && user.quotaRemaining <= 0
)
// 单次余额 = 总剩余 - 免费剩余，购买「单次体验」后展示
const singleBalance = computed(() => {
  const freeRemaining = Math.max(0, user.quotaTotal - user.quotaUsed)
  return Math.max(0, user.quotaRemaining - freeRemaining)
})

async function generate() {
  genErr.value = ''
  // 核心功能必须先登录：未登录直接跳登录页，不发请求
  if (!ensureLogin()) return
  if (quotaExhausted.value) {
    quotaModal.value = true
    return
  }
  if (!ingredient.items.length) {
    genErr.value = '请先添加食材，再点击生成菜谱'
    return
  }
  generating.value = true
  try {
    const res: any = await recipeApi.generateRecipes({
      ingredients: ingredient.items.map((i) => i.name),
      tastes: ingredient.tastes,
      dietType: ingredient.dietType,
      avoidItems: ingredient.avoidFoods
    })
    // 保存 AI 推荐结果（含推荐理由），推荐页首屏直接展示
    aiRecommend.setResults(Array.isArray(res) ? res : [])
    await user.fetchQuota()
    router.push('/recipes')
  } catch (e: any) {
    const msg = e?.message || '生成失败，请稍后重试'
    // 次数用尽 / 会员权限类错误：弹窗拦截，不再放行
    if (msg.includes('次数') || msg.includes('会员')) {
      quotaModal.value = true
    } else {
      // 失败只在当前页提示，绝不跳转（此前失败也 push('/recipes')，
      // 会让游客"看起来能用"，属于漏洞的一部分）
      genErr.value = msg
    }
  } finally {
    generating.value = false
  }
}

function onMember() {
  router.push('/member')
}

onMounted(async () => {
  ingredient.fetchCommon()
  // 登录用户的食材篮以数据库为准（未登录 / 接口失败则为空，由空态提示）
  ingredient.fetchMine()
  try {
    const list: any = await recipeApi.todayRecipes()
    // 后端返回 cookTime/matchRate，映射为卡片所需的 time/match 展示字段
    todayRecipes.value = Array.isArray(list)
      ? list.map((r: any) => ({
          ...r,
          time: r.time || (r.cookTime != null ? `${r.cookTime} 分钟` : ''),
          match: r.match || (r.matchRate != null ? `${r.matchRate}%` : '')
        }))
      : []
  } catch (e: any) {
    todayRecipes.value = []
    todayErr.value = e?.message || '今日推荐加载失败'
  }
  user.fetchQuota()
})
</script>

<template>
  <div class="home">
    <!-- Hero -->
    <div class="hero">
      <div>
        <span class="badge">AI 智能食验助手</span>
        <h1>让每一份食材<br />都不被辜负</h1>
        <p class="lead">
          告诉我冰箱里有什么，AI 马上推荐今晚能做的菜，还能避开你的忌口，缺什么料也有办法。
        </p>
        <div class="points">
          <div class="point"><span class="ok">✓</span>文字、语音、图片，三种方式录入食材</div>
          <div class="point"><span class="ok">✓</span>忌口、过敏源自动过滤，吃得放心</div>
          <div class="point"><span class="ok">✓</span>缺料给替代方案，也能一键买菜</div>
        </div>
      </div>

      <div class="tool">
        <label>你有哪些食材？</label>
        <div class="row-input">
          <input
            v-model="inputText"
            placeholder="例如：番茄、鸡蛋，用顿号或逗号分隔"
            @keyup.enter="addInput"
          />
          <button class="btn btn-primary" @click="addInput">添加</button>
        </div>
        <div class="chips">
          <Chip
            v-for="it in ingredient.items"
            :key="it.id"
            :label="it.name"
            closable
            @close="ingredient.remove(it.id)"
          />
          <span v-if="!ingredient.items.length" class="chips-empty">
            还没有食材，输入或点下方常用快速添加
          </span>
        </div>
        <div v-if="ingredient.common.length" class="common">
          常用
          <Chip
            v-for="c in ingredient.common"
            :key="c"
            variant="add"
            :label="`+ ${c}`"
            @click="ingredient.add(c)"
          />
        </div>
        <div class="two">
          <input
            ref="imageInput"
            type="file"
            accept="image/*"
            style="display: none"
            @change="onImageChosen"
          />
          <div class="dashed" :class="{ busy: imageBusy }" @click="pickImage">
            <div class="t">{{ imageBusy ? '处理中…' : '上传食材图片' }}</div>
            <div class="d">{{ imageStatus || '点击选择图片，AI 自动识别食材' }}</div>
          </div>
          <div
            class="dashed"
            :class="{ busy: listening }"
            @mousedown.prevent="startVoice"
            @mouseup="stopVoice"
            @mouseleave="stopVoice"
            @touchstart.prevent="startVoice"
            @touchend.prevent="stopVoice"
            @touchcancel="stopVoice"
          >
            <div class="t">{{ listening ? '正在聆听…（松开鼠标结束）' : '按住说话' }}</div>
            <div class="d">{{ voiceStatus || '按住开始，说出手头的食材' }}</div>
          </div>
        </div>
        <div v-if="imageHit" class="ai-hit">
          <span>站内已有「{{ imageHit.name }}」的做法</span>
          <a @click="router.push(`/recipes/${imageHit.id}`)">查看菜谱 →</a>
        </div>
        <div class="ai-entry">
          <span>想直接拍整道菜？AI 识菜可反推食材并给做法</span>
          <a @click="router.push('/ai')">试试 AI 识别 →</a>
        </div>
        <label>口味偏好</label>
        <div class="chips" style="margin-top: 0">
          <Chip
            v-for="t in tasteOptions"
            :key="t"
            :variant="ingredient.tastes.includes(t) ? 'sel' : 'off'"
            :label="t"
            @click="ingredient.toggleTaste(t)"
          />
        </div>
        <div class="prefs" style="margin-top: 14px">
          <div>
            <label style="margin-top: 0">饮食方式</label>
            <div class="select-wrap">
              <select v-model="ingredient.dietType" class="select">
                <option v-for="d in dietOptions" :key="d" :value="d">{{ d }}</option>
              </select>
              <span class="caret">▾</span>
            </div>
          </div>
          <div>
            <label style="margin-top: 0">忌口 / 过敏源</label>
            <div class="chips" style="margin-top: 0">
              <Chip
                v-for="a in ingredient.avoidFoods"
                :key="a"
                :label="a"
                closable
                @close="ingredient.removeAvoid(a)"
              />
              <Chip variant="add" label="+ 添加" @click="openAvoidModal" />
            </div>
          </div>
        </div>
        <p v-if="genErr" class="gen-err">⚠ {{ genErr }}</p>
        <div class="tool-foot">
          <span v-if="user.isLoggedIn && user.isMember" class="free">
            会员用户<template v-if="user.memberExpireText"> · {{ user.memberExpireText }}</template>
          </span>
          <span v-else-if="user.isLoggedIn && !user.quotaLoaded" class="free">免费次数加载中…</span>
          <span v-else-if="user.isLoggedIn && singleBalance > 0" class="free">
            本周免费次数 <b>{{ user.quotaUsed }}/{{ user.quotaTotal }}</b> · 单次可用 <b>{{ singleBalance }}</b> 次
          </span>
          <span v-else-if="user.isLoggedIn" class="free">本周免费次数 <b>{{ user.quotaUsed }}/{{ user.quotaTotal }}</b> · 开通会员不限次</span>
          <span v-else class="free link" @click="router.push('/login')">登录后可享每周免费生成次数</span>
          <button
            class="btn btn-primary"
            :disabled="quotaExhausted || generating"
            :title="quotaExhausted ? '本周免费次数已用完' : ''"
            @click="generate"
          >
            {{ generating ? 'AI 智能推荐中…' : quotaExhausted ? '本周次数已用完' : '生成菜谱 →' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 四步说明 -->
    <div class="sec" style="margin-top: 140px">
      <div class="sec-head"><h2>从食材到餐桌，一站走完</h2></div>
      <div class="steps">
        <div class="step">
          <div class="n">01</div>
          <h3>识别食材</h3>
          <p>文字、语音、图片，任选一种，识别结果可修改。</p>
        </div>
        <div class="step">
          <div class="n">02</div>
          <h3>推荐菜谱</h3>
          <p>按匹配度排序，并过滤你的忌口与过敏源。</p>
        </div>
        <div class="step">
          <div class="n">03</div>
          <h3>分步烹饪</h3>
          <p>图文教学，带步骤指引与关键提示。</p>
        </div>
        <div class="step">
          <div class="n">04</div>
          <h3>采购与反馈</h3>
          <p>缺料生成清单、多商家比价；餐后评分优化推荐。</p>
        </div>
      </div>
    </div>

    <!-- 今日推荐 -->
    <div class="sec" style="margin-top: 92px">
      <div class="sec-head">
        <h2>今日推荐</h2>
        <a @click="router.push('/recipes')">查看全部 →</a>
      </div>
      <div class="recs">
        <RecipeCard
          v-for="r in todayRecipes"
          :key="r.id"
          :name="r.name"
          :time="r.time"
          :difficulty="r.difficulty"
          :match="r.match"
          @open="router.push(`/recipes/${r.id}`)"
        />
        <div v-if="!todayRecipes.length" class="recs-empty">
          {{ todayErr || '暂无推荐菜谱，管理端上架后这里会自动展示' }}
        </div>
      </div>
    </div>

    <!-- 添加忌口弹窗 -->
    <div v-if="avoidModal" class="modal-mask" @click.self="avoidModal = false">
      <div class="modal">
        <div class="modal-head">
          <h3>添加忌口 / 过敏源</h3>
          <span class="modal-close" @click="avoidModal = false">✕</span>
        </div>
        <div class="modal-body">
          <input
            v-model="avoidModalName"
            class="modal-input"
            placeholder="例如：香菜 / 花生"
            maxlength="20"
            @keyup.enter="confirmAvoid"
          />
          <div class="modal-acts">
            <button class="btn btn-ghost" @click="avoidModal = false">取消</button>
            <button class="btn btn-primary" @click="confirmAvoid">提交</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 次数用尽弹窗 -->
    <div v-if="quotaModal" class="modal-mask" @click.self="quotaModal = false">
      <div class="modal">
        <div class="modal-head">
          <h3>本周免费次数已用完</h3>
          <span class="modal-close" @click="quotaModal = false">✕</span>
        </div>
        <div class="modal-body">
          <p class="modal-p">
            非会员每周可免费生成 <b>5</b> 次菜谱，额度已用完，暂时无法继续生成。
          </p>
          <p class="modal-p muted">
            开通会员可不限次使用；也可购买<b>单次体验 ¥2.99</b>，购买后立即可用。免费额度将于 <b>7 天后（下周一 0 点）</b>自动重置。
          </p>
          <div class="modal-acts">
            <button class="btn btn-ghost" @click="quotaModal = false; router.push('/member')">
              购买单次体验
            </button>
            <button class="btn btn-primary" @click="quotaModal = false; router.push('/member')">
              开通会员，不限次
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 会员推广条 -->
    <div class="member" style="margin-top: 88px">
      <div class="inner">
        <div>
          <span class="tag-amber">会员专享</span>
          <h3>让 AI 为你创作一道新菜</h3>
          <p>基础功能每周免费 5 次。会员 ¥19/月，解锁 AI 生成的新颖菜谱；也可单次 ¥2.99 体验。</p>
        </div>
        <button class="btn" @click="onMember">了解会员</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hero {
  max-width: 1296px;
  margin: 0 auto;
  padding: 52px 72px 0;
  display: grid;
  grid-template-columns: 1fr 660px;
  gap: 56px;
  align-items: start;
}
.hero .badge {
  display: inline-block;
  background: var(--chip-bg);
  color: var(--chip-text);
  font-size: 13px;
  border-radius: 999px;
  padding: 5px 14px;
  margin-bottom: 20px;
}
.hero h1 {
  font-size: 44px;
  line-height: 1.22;
  margin: 0 0 20px;
  font-weight: 700;
}
.hero .lead {
  color: var(--text2);
  font-size: 15px;
  margin: 0 0 26px;
  max-width: 430px;
}
.hero .points {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.hero .point {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text2);
  font-size: 14px;
}
.hero .point .ok {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--chip-bg);
  color: var(--chip-text);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  flex: none;
}

.tool {
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 6px 24px rgba(31, 78, 59, 0.08);
  padding: 22px 22px 18px;
}
.tool label {
  font-size: 13px;
  color: var(--text2);
  display: block;
  margin: 14px 0 8px;
}
.row-input {
  display: flex;
  gap: 10px;
}
.row-input input {
  flex: 1;
  height: 42px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0 12px;
  font-family: inherit;
  font-size: 13px;
  outline: none;
  color: var(--text);
}
.chips {
  margin-top: 12px;
}
.chips-empty {
  font-size: 12px;
  color: var(--text3);
  line-height: 28px;
}
.common {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 13px;
  color: var(--text2);
  flex-wrap: wrap;
}
.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 12px;
}
.two .dashed {
  cursor: pointer;
  user-select: none;
  transition: opacity 0.2s;
}
.two .dashed:hover {
  border-color: var(--primary);
}
/* 只降透明度做视觉反馈，绝不能加 pointer-events:none，
   否则按住说话时元素收不到 mouseup，识别永远停不下来 */
.two .dashed.busy {
  opacity: 0.65;
}
.ai-hit,
.ai-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 12px;
}
.ai-hit {
  background: var(--chip-bg);
  color: var(--chip-text);
}
.ai-entry {
  background: #f7f9f5;
  color: var(--text2);
}
.ai-hit a,
.ai-entry a {
  color: var(--primary);
  cursor: pointer;
  font-weight: 600;
  flex: none;
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
  width: 380px;
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
.modal-acts {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 14px;
}
.modal-p {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--text);
  line-height: 1.7;
}
.modal-p.muted {
  color: var(--text2);
}
.prefs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  align-items: end;
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
.tool-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #f0f3ee;
}
.tool-foot .free {
  font-size: 12px;
  color: var(--text3);
}
.tool-foot .free.link {
  cursor: pointer;
  text-decoration: underline;
}
.tool-foot .free b {
  color: var(--amber);
}
.gen-err {
  margin: 10px 0 -6px;
  font-size: 12px;
  color: var(--danger);
}

.steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.step {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: var(--shadow);
}
.step .n {
  font-size: 30px;
  font-weight: 700;
  color: #b9cdbf;
  margin-bottom: 14px;
}
.step h3 {
  font-size: 16px;
  margin: 0 0 8px;
}
.step p {
  color: var(--text2);
  font-size: 13px;
  margin: 0;
}

.recs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.recs-empty {
  grid-column: 1 / -1;
  padding: 40px 16px;
  text-align: center;
  color: var(--text2);
  font-size: 13px;
  background: #fff;
  border-radius: 16px;
  box-shadow: var(--shadow);
}

.member {
  max-width: 1296px;
  margin: 0 auto;
  padding: 0 72px;
  width: 100%;
}
.member .inner {
  background: var(--primary);
  border-radius: 20px;
  padding: 34px 36px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
}
.member .inner h3 {
  font-size: 20px;
  margin: 12px 0 8px;
  font-weight: 700;
}
.member .inner p {
  margin: 0;
  color: #cfe0d6;
  font-size: 13px;
}
.member .btn {
  background: #fff;
  color: var(--primary);
}
</style>
