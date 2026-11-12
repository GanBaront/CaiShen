<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useIngredientStore } from '@/stores/ingredients'
import { useRequireLogin } from '@/composables/useRequireLogin'
import * as memberApi from '@/api/member'
import * as aiApi from '@/api/ai'

const router = useRouter()
const user = useUserStore()
const ingredient = useIngredientStore()
const ensureLogin = useRequireLogin()

const plans = ref<any[]>([
  {
    type: 'free',
    name: '免费',
    price: '¥0',
    unit: '',
    hot: false,
    btn: '前往使用',
    btnClass: 'btn-outline',
    features: [
      '文字 / 语音 / 图片上传识别食材',
      '个性化筛选与食材替代',
      '分步烹饪教学',
      '购物清单',
      '每周 5 次免费使用'
    ]
  },
  {
    type: 'single',
    name: '单次体验',
    price: '¥2.99',
    unit: '/ 次',
    hot: false,
    btn: '购买 1 次',
    btnClass: 'btn-outline',
    features: ['生成 1 道 AI 新菜谱', '无需订阅，用完即止']
  },
  {
    type: 'member',
    name: '会员',
    price: '¥19',
    unit: '/ 月',
    hot: true,
    btn: '立即开通',
    btnClass: 'btn-primary',
    features: ['包含免费方案全部功能', 'AI 生成的新颖菜谱', '更多高级功能', '到期提醒，支持续费']
  }
])

// AI 新菜谱预览：完全以接口返回的库中 AI 菜谱为准，没有则不显示示例
const preview = ref<any>(null)
const previewTip = ref('')

// ===== AI 新菜谱生成（真实调用大模型，会员不限次 / 非会员消耗单次余额） =====
const aiGenerating = ref(false)
const aiGenMsg = ref('')

async function createAiRecipe() {
  if (!ensureLogin()) return
  if (aiGenerating.value) return
  const names = ingredient.items.map((i) => i.name)
  if (!names.length) {
    aiGenMsg.value = '请先在首页添加食材，再来让 AI 创作新菜'
    return
  }
  aiGenerating.value = true
  aiGenMsg.value = ''
  try {
    const res: any = await aiApi.generateAiRecipe({
      ingredients: names,
      tastes: ingredient.tastes
    })
    // 生成成功直接进入新菜谱详情页
    router.push(`/recipes/${res?.id}`)
  } catch (e: any) {
    aiGenMsg.value = e?.message || 'AI 生成失败，请稍后重试'
  } finally {
    aiGenerating.value = false
  }
}

const orderPlan = ref({ name: '会员 · 1 个月', amount: '¥19.00' })

// 购买记录完全以 /billing/records 为准，不写死示例（新用户为空即显示空态）
const records = ref<any[]>([])

function fmtAmount(v: any) {
  const n = Number(v)
  return Number.isFinite(n) ? `¥${n.toFixed(2)}` : String(v ?? '')
}

/** 后端 PlanVO -> 套餐卡展示字段（code/recommended/buttonText 与前端模板对齐） */
function mapPlan(p: any) {
  const code = p.code || p.type || ''
  const priceNum = Number(p.price)
  const isFree = code === '免费' || priceNum === 0
  return {
    ...p,
    type: code === '会员' ? 'member' : code === '单次' ? 'single' : isFree ? 'free' : code,
    rawPrice: p.price,
    price: isFree ? '¥0' : Number.isFinite(priceNum) ? `¥${priceNum}` : String(p.price ?? ''),
    hot: !!p.recommended || !!p.hot,
    btn: p.buttonText || p.btn || (isFree ? '前往使用' : code === '会员' ? '立即开通' : '购买 1 次'),
    btnClass: p.recommended || p.hot ? 'btn-primary' : 'btn-outline'
  }
}

function mapRecord(r: any) {
  return {
    ...r,
    amount: typeof r.amount === 'number' ? fmtAmount(r.amount) : r.amount,
    time: r.createTime || r.time
  }
}

const quotaPercent = computed(() =>
  user.quotaTotal ? Math.round((user.quotaUsed / user.quotaTotal) * 100) : 0
)

// 当前选中的套餐类型（支付成功时才创建购买记录）
const selectedType = ref('')
const orderMsg = ref('')
const orderMsgOk = ref(true)
const paying = ref(false)

// 支付确认 / 成功弹窗
const payConfirm = ref(false)
const paySuccess = ref(false)

function showOrderMsg(msg: string, ok = true) {
  orderMsg.value = msg
  orderMsgOk.value = ok
  setTimeout(() => (orderMsg.value = ''), 3000)
}

/** 套餐本地展示与后端类型的映射 */
const PLAN_TYPE_MAP: Record<string, string> = {
  member: '会员·1个月',
  single: '单次AI菜谱'
}

function planAmount(p: any): string {
  const n = Number(p.rawPrice ?? p.price)
  return Number.isFinite(n) ? `¥${n.toFixed(2)}` : String(p.price)
}

/** 后端 LocalDateTime ISO 串 → yyyy-MM-dd HH:mm:ss */
function formatTime(t?: string): string {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

async function choosePlan(p: any) {
  orderMsg.value = ''
  // 免费方案：引导去首页使用基础功能（兼容本地 type=free / 后端 code=免费 / price=0）
  const isFree =
    p.type === 'free' || p.code === '免费' || p.price === 0 || p.price === '¥0'
  if (isFree) {
    router.push('/')
    return
  }
  if (!ensureLogin()) return
  const backendType = PLAN_TYPE_MAP[p.type] || (String(p.name).includes('会员') ? '会员·1个月' : '单次AI菜谱')
  selectedType.value = backendType
  orderPlan.value = { name: backendType, amount: planAmount(p) }
  showOrderMsg(`已选择「${p.name}」，请点击下方模拟支付完成开通`)
}

/** 点击支付按钮：先校验，再弹确认窗口 */
function openPayConfirm() {
  if (!ensureLogin()) return
  if (!selectedType.value) {
    showOrderMsg('请先选择要开通的套餐', false)
    return
  }
  payConfirm.value = true
}

async function confirmPay() {
  if (!selectedType.value) {
    payConfirm.value = false
    return
  }
  paying.value = true
  try {
    // 支付成功时后端才生成购买记录，不再提前落库
    await memberApi.mockPay({ type: selectedType.value })
    selectedType.value = ''
    payConfirm.value = false
    paySuccess.value = true
    // 刷新登录态（会员身份）与免费次数、购买记录
    await user.fetchMe()
    await user.fetchQuota()
    try {
      const res: any = await memberApi.billingRecords()
      if (Array.isArray(res)) records.value = res.map(mapRecord)
    } catch {
      /* 忽略 */
    }
  } catch (e: any) {
    payConfirm.value = false
    showOrderMsg(e?.message || '支付失败，请稍后重试', false)
  } finally {
    paying.value = false
  }
}

onMounted(async () => {
  user.fetchMe()
  user.fetchQuota()
  // 食材篮用于「AI 创作新菜」的输入（登录用户以数据库为准）
  ingredient.fetchMine()
  try {
    const res: any = await memberApi.memberPlans()
    if (Array.isArray(res) && res.length) plans.value = res.map(mapPlan)
  } catch {
    /* 降级：保留设计图套餐 */
  }
  try {
    const res: any = await aiApi.aiRecipePreview()
    if (res?.name) {
      // 后端字段为 cookTime/difficulty/taste/description，这里映射为卡片展示字段
      const meta = [res.cookTime ? `${res.cookTime} 分钟` : '', res.difficulty, res.taste]
        .filter(Boolean)
        .join(' · ')
      preview.value = {
        name: res.name,
        meta,
        desc: res.description || '',
        ingredients: Array.isArray(res.ingredients) ? res.ingredients : []
      }
    } else {
      previewTip.value = res?.tip || '平台还没有 AI 生成的菜谱，开通会员后即可创作'
    }
  } catch {
    /* 接口失败：不展示示例，给出提示 */
    previewTip.value = '暂时获取不到 AI 菜谱示例，请稍后重试'
  }
  // 购买记录属于账户数据：游客不请求，避免 401 被强制跳登录
  if (user.isLoggedIn) {
    try {
      const res: any = await memberApi.billingRecords()
      if (Array.isArray(res)) records.value = res.map(mapRecord)
    } catch {
      /* 降级：记录为空 */
    }
  }
})
</script>

<template>
  <div class="member-page sec">
    <div class="hero">
      <span class="badge">会员中心</span>
      <h1>让 AI 为你创作新菜谱</h1>
      <p>基础功能每周免费 5 次。想要更多新鲜点子，开通会员或按次体验。</p>
    </div>

    <!-- 状态卡：会员显示到期日，非会员显示今日免费次数 -->
    <div class="status">
      <div>
        <div class="k">当前身份</div>
        <div class="v">{{ user.memberLabel }}</div>
      </div>
      <div v-if="user.isMember">
        <div class="k">会员到期</div>
        <div class="v exp">{{ user.memberExpireText || '—' }}</div>
      </div>
      <div v-else>
        <div class="k">本周剩余</div>
        <div class="v">
          <template v-if="user.quotaLoaded">{{ user.quotaUsed }}/{{ user.quotaTotal }} <small>次</small></template>
          <template v-else>—</template>
        </div>
      </div>
      <template v-if="user.isMember">
        <div class="member-perk">会员有效期内可享受全部会员权益</div>
      </template>
      <template v-else>
        <div class="bar"><i :style="{ width: quotaPercent + '%' }"></i></div>
        <div class="reset">次数每周一 0 点重置</div>
      </template>
    </div>

    <!-- 套餐 -->
    <div class="plans">
      <div v-for="p in plans" :key="p.type || p.name" class="plan" :class="{ hot: p.hot }">
        <span v-if="p.hot" class="hot-tag">推荐</span>
        <h3>{{ p.name }}</h3>
        <div class="price">{{ p.price }}<small>{{ p.unit }}</small></div>
        <ul>
          <li v-for="f in p.features" :key="f"><span class="ok">✓</span>{{ f }}</li>
        </ul>
        <button class="btn" :class="p.btnClass || (p.hot ? 'btn-primary' : 'btn-outline')" @click="choosePlan(p)">
          {{ p.btn || p.buttonText || '立即开通' }}
        </button>
      </div>
    </div>

    <div class="two">
      <!-- AI 新菜谱预览 -->
      <div class="blk">
        <div class="bh"><h2>AI 新菜谱预览</h2><span class="tag-amber">示例</span></div>
        <p class="sub">用你现有的食材，组合出不同寻常但可行的做法。</p>
        <div v-if="preview" class="preview">
          <div class="pic img-ph"><span class="ai">AI 创作</span>AI 菜品图</div>
          <div>
            <h3>{{ preview.name }}</h3>
            <div class="m">{{ preview.meta }}</div>
            <p class="d">{{ preview.desc }}</p>
            <div class="chips" style="margin: 0">
              <span v-for="i in preview.ingredients" :key="i" class="chip">{{ i }}</span>
            </div>
          </div>
        </div>
        <div v-else class="preview-empty">{{ previewTip || '暂无 AI 菜谱示例' }}</div>
        <button class="btn btn-primary btn-block ai-gen-btn" :disabled="aiGenerating" @click="createAiRecipe">
          {{ aiGenerating ? 'AI 创作中…（约需半分钟）' : '用我的食材创作一道 AI 新菜' }}
        </button>
        <p v-if="aiGenMsg" class="ai-gen-msg">⚠ {{ aiGenMsg }}</p>
      </div>

      <!-- 确认订单 -->
      <div class="blk order">
        <div class="bh"><h2>确认订单</h2></div>
        <div class="row"><span class="nm">{{ orderPlan.name }}</span><span>{{ orderPlan.amount }}</span></div>
        <div class="pay">
          <span class="radio"></span>微信支付
          <span class="sim">模拟支付，不产生真实扣款</span>
        </div>
        <div class="due"><span class="lbl">应付</span><span class="amt">{{ orderPlan.amount }}</span></div>
        <p v-if="orderMsg" class="order-msg" :class="{ err: !orderMsgOk }">{{ orderMsg }}</p>
        <button class="btn btn-primary btn-block pay-btn" :disabled="paying" @click="openPayConfirm">
          {{ paying ? '支付中…' : '模拟支付开通' }}
        </button>
        <div class="note">到期前会通过站内通知提醒你，可随时续费。</div>
      </div>
    </div>

    <!-- 确认支付弹窗 -->
    <div v-if="payConfirm" class="modal-mask" @click.self="payConfirm = false">
      <div class="modal">
        <h3>确认支付</h3>
        <div class="pay-item">
          <span class="pay-nm">{{ orderPlan.name }}</span>
          <span class="pay-amt">{{ orderPlan.amount }}</span>
        </div>
        <p class="pay-tip">模拟支付，不会产生真实扣款。确认完成本次支付吗？</p>
        <div class="pay-acts">
          <button class="btn btn-ghost" @click="payConfirm = false">取消</button>
          <button class="btn btn-primary" :disabled="paying" @click="confirmPay">
            {{ paying ? '支付中…' : '确认支付' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 支付成功弹窗 -->
    <div v-if="paySuccess" class="modal-mask">
      <div class="modal success">
        <div class="ok-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path d="M5 12.5 10 17.5 19 7" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <h3>模拟支付成功</h3>
        <p class="pay-tip">已为你开通，未产生真实扣款。</p>
        <div class="pay-acts center">
          <button class="btn btn-primary" @click="paySuccess = false">完成</button>
        </div>
      </div>
    </div>

    <!-- 订阅与购买记录 -->
    <div class="table">
      <h2>订阅与购买记录</h2>
      <table>
        <thead>
          <tr><th>类型</th><th>金额</th><th>状态</th><th>时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="(r, i) in records" :key="i">
            <td>{{ r.type }}</td>
            <td>{{ r.amount }}</td>
            <td>
              <span :class="r.status === '已支付' ? 'tag-green' : 'tag-amber'">{{ r.status }}</span>
            </td>
            <td>{{ formatTime(r.createTime || r.time) }}</td>
          </tr>
          <tr v-if="!records.length">
            <td colspan="4" class="record-empty">
              {{ user.isLoggedIn ? '暂无购买记录，选择套餐完成支付后会显示在这里' : '登录后可查看购买记录' }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="foot-space"></div>
  </div>
</template>

<style scoped>
.member-page {
  padding-bottom: 40px;
}
.hero {
  text-align: center;
  padding: 44px 0 30px;
}
.hero .badge {
  display: inline-block;
  background: var(--chip-bg);
  color: var(--chip-text);
  font-size: 13px;
  border-radius: 999px;
  padding: 5px 16px;
  margin-bottom: 16px;
}
.hero h1 {
  font-size: 34px;
  font-weight: 700;
  margin-bottom: 12px;
}
.hero p {
  color: var(--text2);
  font-size: 14px;
  margin: 0;
}
.status {
  background: #fff;
  border-radius: 16px;
  box-shadow: var(--shadow);
  padding: 20px 26px;
  display: grid;
  grid-template-columns: 150px 150px 1fr 130px;
  align-items: center;
  gap: 20px;
  margin-bottom: 26px;
}
.status .k {
  color: var(--text3);
  font-size: 12px;
}
.status .v {
  font-size: 20px;
  font-weight: 700;
  margin-top: 4px;
}
.status .v small {
  font-size: 14px;
  font-weight: 600;
}
.status .v.exp {
  font-size: 16px;
}
.member-perk {
  grid-column: span 2;
  color: var(--primary);
  font-size: 13px;
  font-weight: 600;
}
.status .bar {
  height: 10px;
  border-radius: 999px;
  background: #eff3ee;
  overflow: hidden;
}
.status .bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--primary);
}
.status .reset {
  color: var(--text3);
  font-size: 12px;
  text-align: right;
}
.plans {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 26px;
}
.plan {
  background: #fff;
  border-radius: 16px;
  box-shadow: var(--shadow);
  padding: 26px;
  position: relative;
  border: 1px solid transparent;
}
.plan.hot {
  border-color: var(--primary);
  box-shadow: 0 6px 20px rgba(31, 78, 59, 0.14);
}
.plan .hot-tag {
  position: absolute;
  top: -11px;
  right: 20px;
  background: var(--primary);
  color: #fff;
  font-size: 12px;
  border-radius: 999px;
  padding: 3px 12px;
  font-weight: 600;
}
.plan h3 {
  font-size: 16px;
  margin-bottom: 14px;
}
.plan .price {
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
}
.plan .price small {
  font-size: 14px;
  color: var(--text2);
  font-weight: 400;
  margin-left: 4px;
}
.plan ul {
  list-style: none;
  padding: 0;
  margin: 20px 0 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.plan li {
  display: flex;
  gap: 9px;
  color: var(--text2);
  font-size: 13px;
}
.plan li .ok {
  color: var(--success);
  flex: none;
}
.plan .btn {
  width: 100%;
  height: 42px;
}
.two {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 24px;
  margin-bottom: 30px;
  align-items: start;
}
.blk .bh {
  margin-bottom: 8px;
}
.preview {
  display: grid;
  grid-template-columns: 210px 1fr;
  gap: 18px;
}
.preview-empty {
  padding: 40px 16px;
  text-align: center;
  color: var(--text2);
  font-size: 13px;
  background: var(--chip-bg, #f4f7f3);
  border-radius: 12px;
}
.record-empty {
  text-align: center;
  color: var(--text2);
  font-size: 13px;
  padding: 18px 0;
}
.preview .pic {
  height: 190px;
  border-radius: 12px;
  position: relative;
}
.preview .pic .ai {
  position: absolute;
  left: 10px;
  top: 10px;
  background: var(--primary);
  color: #fff;
  font-size: 11px;
  border-radius: 6px;
  padding: 3px 8px;
}
.preview h3 {
  font-size: 17px;
  margin: 4px 0 8px;
}
.preview .m {
  color: var(--text3);
  font-size: 13px;
  margin-bottom: 10px;
}
.preview .d {
  color: var(--text2);
  font-size: 13px;
  margin: 0 0 14px;
}
.order .row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  font-size: 14px;
}
.order .row .nm {
  color: var(--text2);
}
.order .pay {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 12px 0;
  font-size: 13px;
}
.order .pay .radio {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 5px solid var(--primary);
  background: #fff;
  flex: none;
}
.order .pay .sim {
  color: var(--text3);
  font-size: 12px;
  margin-left: auto;
}
.order .due {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 14px 0 16px;
}
.order .due .lbl {
  color: var(--text2);
  font-size: 13px;
}
.order .due .amt {
  font-size: 26px;
  font-weight: 700;
}
.order .pay-btn {
  height: 44px;
}
.order-msg {
  font-size: 12px;
  color: var(--success, var(--primary));
  margin: 8px 0;
}
.order-msg.err {
  color: var(--danger);
}
.order .note {
  color: var(--text3);
  font-size: 12px;
  margin-top: 12px;
  text-align: center;
}
.foot-space {
  height: 10px;
}

/* 支付确认 / 成功弹窗 */
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
  padding: 22px 24px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  text-align: center;
}
.modal h3 {
  margin: 0 0 10px;
  font-size: 16px;
  text-align: left;
}
.modal.success h3 {
  text-align: center;
}
.pay-item {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  background: #f6f8f5;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.pay-item .pay-nm {
  font-size: 14px;
  color: var(--text2);
}
.pay-item .pay-amt {
  font-size: 20px;
  font-weight: 700;
}
.pay-tip {
  margin: 0 0 18px;
  font-size: 13px;
  color: var(--text2);
  text-align: left;
}
.modal.success .pay-tip {
  text-align: center;
}
.pay-acts {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.pay-acts.center {
  justify-content: center;
}
.ok-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--success, #2f9e5f);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 2px auto 12px;
}
</style>
