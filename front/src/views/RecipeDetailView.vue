<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRequireLogin } from '@/composables/useRequireLogin'
import { useIngredientStore } from '@/stores/ingredients'
import * as recipeApi from '@/api/recipe'

const route = useRoute()
const router = useRouter()
const ensureLogin = useRequireLogin()
const ingredient = useIngredientStore()

const id = computed(() => String(route.params.id || '1'))

const favorited = ref(false)
const rating = ref(4)
const reviewContent = ref('')
const currentStep = ref(1)

// 详情全部来自后端（管理端编辑后实时体现），不再使用写死的示例数据兜底
const detail = ref<any>(null)
const similar = ref<any[]>([])
const reviews = ref<any[]>([])
const loadError = ref('')
const reviewMsg = ref<{ ok: boolean; text: string } | null>(null)

const missing = computed(() =>
  detail.value ? detail.value.ingredients.filter((i: any) => !i.owned) : []
)
const stepCount = computed(() => (detail.value ? detail.value.steps.length : 0))
const currentStepData = computed(
  () => detail.value?.steps[currentStep.value - 1] || { title: '', desc: '', tip: '' }
)

/** 管理端过敏源标签 -> 常见食材关键词（与后端 ALLERGEN_KEYWORDS 保持一致） */
const ALLERGEN_KEYWORDS: Record<string, string[]> = {
  蛋类: ['蛋'],
  乳制品: ['牛奶', '奶油', '黄油', '芝士', '奶酪', '酸奶', '炼乳', '奶粉'],
  麸质: ['面粉', '小麦', '面包', '面条', '馒头', '饺子皮', '面筋', '意面'],
  坚果: ['花生', '核桃', '杏仁', '腰果', '榛子', '开心果', '松子'],
  海鲜: ['虾', '蟹', '贝', '鱼', '蛤', '鱿鱼', '章鱼', '海参', '生蚝', '蚝油'],
  花生: ['花生']
}

/** 忌口命中：具体食材全名匹配 + 过敏源标签关键词包含匹配（「蛋类」命中「鸡蛋」） */
function matchesAvoid(ingredientName: string, avoids: string[]): boolean {
  const name = String(ingredientName || '')
  for (const item of avoids) {
    const term = String(item || '').trim()
    if (!term) continue
    if (name === term) return true
    for (const kw of ALLERGEN_KEYWORDS[term] || []) {
      if (kw && name.includes(kw)) return true
    }
  }
  return false
}

/** 「适合」：按当前用户的忌口与食材实时计算 */
const suitable = computed(() => {
  if (!detail.value) return ''
  const avoids = ingredient.avoidFoods || []
  if (!avoids.length) return '无忌口冲突'
  const hit = detail.value.ingredients
    .filter((i: any) => matchesAvoid(i.name, avoids))
    .map((i: any) => i.name)
  return hit.length ? `含忌口：${hit.join('、')}` : '无忌口冲突'
})

function goStep(n: number) {
  currentStep.value = Math.min(Math.max(1, n), stepCount.value)
}

function scrollToAlt() {
  document.querySelector('.alt')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function startCook() {
  currentStep.value = 1
  document.querySelector('.teach')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

// 收藏 / 加入购物清单等操作的页面反馈
const actMsg = ref<{ ok: boolean; text: string } | null>(null)

async function toggleFavorite() {
  if (!ensureLogin()) return
  actMsg.value = null
  const next = !favorited.value
  favorited.value = next
  try {
    if (next) await recipeApi.favoriteRecipe(id.value)
    else await recipeApi.unfavoriteRecipe(id.value)
    actMsg.value = { ok: true, text: next ? '已加入收藏' : '已取消收藏' }
  } catch (e: any) {
    // 失败回滚本地状态，避免「页面显示了但后端没存」
    favorited.value = !next
    actMsg.value = {
      ok: false,
      text: `${next ? '收藏' : '取消收藏'}失败：${e?.message || '请稍后重试'}`
    }
  }
}

async function addToShoppingList() {
  if (!ensureLogin()) return
  actMsg.value = null
  try {
    // 后端已把菜谱里「没有的食材」全部写入购物清单（含去重），
    // 这里不能再本地重复添加，否则同名食材会出现两条
    await recipeApi.addRecipeToShoppingList(id.value)
  } catch (e: any) {
    actMsg.value = { ok: false, text: `加入购物清单失败：${e?.message || '请稍后重试'}` }
    return
  }
  router.push('/shopping-list')
}

// 一键下单：按还缺的食材自动选最优商家，直接生成订单
const ordering = ref(false)
const orderMsg = ref<{ ok: boolean; text: string } | null>(null)

async function oneClickOrder() {
  if (!ensureLogin()) return
  if (ordering.value) return
  ordering.value = true
  orderMsg.value = null
  try {
    const res: any = await recipeApi.createRecipeOrder(id.value)
    const merchant = res?.merchantName || '商家'
    orderMsg.value = {
      ok: true,
      text: `下单成功：已向「${merchant}」下单所缺食材，订单号 ${res?.orderNo || ''}`
    }
  } catch (e: any) {
    orderMsg.value = { ok: false, text: e?.message || '一键下单失败，请稍后重试' }
  } finally {
    ordering.value = false
  }
}

function goOrders() {
  orderMsg.value = null
  router.push('/orders')
}

/** 后端营养对象 {calories, protein, fat, carbs} -> 进度条数组 */
function buildNutrition(n: any) {
  if (!n) return []
  const num = (v: any) => (v == null ? 0 : Number(v))
  return [
    { name: '热量', value: `约 ${num(n.calories)} kcal`, pct: num(n.calories) / 600, color: '#E2703A' },
    { name: '蛋白质', value: `约 ${num(n.protein)} g`, pct: num(n.protein) / 40, color: '#5C8D5A' },
    { name: '脂肪', value: `约 ${num(n.fat)} g`, pct: num(n.fat) / 50, color: '#E2B33A' },
    { name: '碳水', value: `约 ${num(n.carbs)} g`, pct: num(n.carbs) / 80, color: '#3E9A6B' }
  ].map((it) => ({ ...it, width: `${Math.max(6, Math.min(100, Math.round(it.pct * 100)))}%` }))
}

/** 后端详情字段 -> 页面字段（description/cookTime/matchRate/步骤/营养/替代方案真实映射） */
function mapDetail(res: any) {
  const ingredients: any[] = res.ingredients || []
  const steps = (res.steps || []).map((s: any) => ({
    title: s.title,
    desc: s.description,
    tip: s.tips
  }))
  // 替代方案挂在所缺食材下，这里汇总成列表；没有替代方案则不展示该区块
  const substitutes = ingredients
    .filter((i: any) => !i.owned && Array.isArray(i.substitutes) && i.substitutes.length)
    .flatMap((i: any) =>
      i.substitutes.map((s: any) => ({
        forName: i.name,
        name: s.name,
        ratio: s.ratioDesc,
        note: s.note
      }))
    )
  return {
    ...res,
    desc: res.description || '',
    time: res.cookTime != null ? `${res.cookTime} 分钟` : '',
    match: res.matchRate != null ? `${res.matchRate}%` : '',
    servings: res.servings != null ? `${res.servings} 人份` : '',
    ingredients,
    substitutes,
    steps,
    nutrition: buildNutrition(res.nutrition)
  }
}

async function loadDetail() {
  loadError.value = ''
  try {
    const res: any = await recipeApi.recipeDetail(id.value)
    detail.value = mapDetail(res)
    if (res.favorite !== undefined) favorited.value = !!res.favorite
    currentStep.value = 1
    similar.value = (res.similar || []).map((s: any) => ({
      id: s.id,
      name: s.name,
      meta: [s.cookTime != null ? `${s.cookTime} 分钟` : '', s.difficulty].filter(Boolean).join(' · ')
    }))
  } catch (e: any) {
    detail.value = null
    similar.value = []
    loadError.value = e?.message || '菜谱加载失败，请稍后重试'
  }
}

/** 已通过的评价列表（管理端审核后展示） */
async function loadReviews() {
  try {
    const list: any = await recipeApi.recipeReviews(id.value)
    reviews.value = Array.isArray(list) ? list : []
  } catch {
    reviews.value = []
  }
}

/** 提交评价：进入待审核，管理端审核通过后对所有人展示 */
async function submitReview() {
  if (!ensureLogin()) return
  reviewMsg.value = null
  try {
    await recipeApi.submitRecipeReview(id.value, {
      rating: rating.value,
      content: reviewContent.value || undefined
    })
    reviewMsg.value = { ok: true, text: '评价已提交，审核通过后将对所有人展示' }
    reviewContent.value = ''
  } catch (e: any) {
    reviewMsg.value = { ok: false, text: e?.message || '提交失败，请稍后重试' }
  }
}

function fmtTime(t: any) {
  return String(t || '').replace('T', ' ').slice(0, 16)
}

onMounted(async () => {
  await ingredient.fetchMine()
  await loadDetail()
  await loadReviews()
})
</script>

<template>
  <div class="detail sec">
    <!-- 加载失败：如实提示，可重试 -->
    <div v-if="loadError" class="order-msg err" style="margin: 30px 0">
      ⚠ {{ loadError }}
      <a @click="loadDetail">重试</a>
    </div>
    <div v-else-if="!detail" class="order-msg" style="margin: 30px 0">菜谱加载中…</div>

    <template v-else>
      <div class="crumb">首页 / 菜谱推荐 / <b>{{ detail.name }}</b></div>

      <!-- 头部 -->
      <div class="head">
        <div class="big img-ph">菜品大图</div>
        <div>
          <div class="badges">
            <span class="tag-green">匹配 {{ detail.match || '—' }}</span>
            <span class="tag-gray">匹配度 {{ detail.match || '—' }}</span>
            <span>{{ detail.cuisine }}</span>
            <span>{{ detail.taste }}</span>
          </div>
          <h1>{{ detail.name }}</h1>
          <p v-if="detail.source === 'AI生成' && detail.status === '待审核'" class="ai-draft-tip">
            🤖 这道菜由 AI 用你现有的食材现场创作，尚未人工审核，用量与火候仅供参考。
          </p>
          <p class="desc">{{ detail.desc || '暂无简介' }}</p>
          <div class="meta">
            <div class="it"><div class="k">耗时</div><div class="v">{{ detail.time || '—' }}</div></div>
            <div class="it"><div class="k">难度</div><div class="v">{{ detail.difficulty || '—' }}</div></div>
            <div class="it"><div class="k">分量</div><div class="v">{{ detail.servings || '—' }}</div></div>
            <div class="it"><div class="k">适合</div><div class="v">{{ suitable }}</div></div>
          </div>
          <div class="acts">
            <button class="btn btn-primary" @click="startCook">开始烹饪</button>
            <button class="btn btn-outline" @click="toggleFavorite">
              {{ favorited ? '♥ 取消收藏' : '♡ 收藏' }}
            </button>
            <button class="btn btn-outline" @click="addToShoppingList">加入购物清单</button>
            <button class="btn btn-primary" :disabled="ordering" @click="oneClickOrder">
              {{ ordering ? '下单中…' : '一键下单' }}
            </button>
          </div>
          <p v-if="actMsg" class="act-msg" :class="{ err: !actMsg.ok }">{{ actMsg.text }}</p>
        </div>
      </div>

      <div class="cols">
        <!-- 左列 -->
        <div class="stack">
          <div class="blk">
            <div class="bh"><h2>所需食材</h2><span class="r">按 {{ detail.servings || '—' }}</span></div>
            <div
              v-for="(ing, i) in detail.ingredients"
              :key="i"
              class="ing"
            >
              <span class="nm">{{ ing.name }}</span>
              <span class="am">{{ ing.amount }}{{ ing.unit || '' }}</span>
              <span class="st">
                <span v-if="ing.owned" class="tag-green">已有</span>
                <template v-else>
                  <span class="tag-red">缺</span>
                  <a v-if="ing.substitutes && ing.substitutes.length" class="alt-link" @click="scrollToAlt">查看替代</a>
                </template>
              </span>
            </div>
          </div>

          <div v-if="detail.substitutes.length" class="alt">
            <div class="t"><span class="tag-red-bg">缺</span>食材不够？可以这样选</div>
            <div class="cards">
              <div v-for="s in detail.substitutes" :key="s.name" class="c">
                <div class="nm">{{ s.name }}<span class="for">替代 {{ s.forName }}</span></div>
                <div class="ln">替代比例：<b>{{ s.ratio || '—' }}</b></div>
                <div class="ln">口味衔接：{{ s.note || '—' }}</div>
              </div>
            </div>
          </div>

          <div class="blk">
            <div class="bh">
              <h2>分步教学</h2><span class="r">第 {{ currentStep }} 步 / 共 {{ stepCount }} 步</span>
            </div>
            <div class="teach">
              <div class="slist">
                <div
                  v-for="(s, i) in detail.steps"
                  :key="i"
                  class="s"
                  :class="{ on: currentStep === i + 1 }"
                  @click="goStep(i + 1)"
                >
                  <span class="n">{{ i + 1 }}</span>{{ s.title }}
                </div>
              </div>
              <div class="stage">
                <div class="pic img-ph">步骤示意图</div>
                <h3>步骤 {{ currentStep }} · {{ currentStepData.title }}</h3>
                <p>{{ currentStepData.desc }}</p>
                <div v-if="currentStepData.tip" class="tip"><b>关键提示：</b>{{ currentStepData.tip }}</div>
                <div class="ctrl">
                  <button class="btn btn-ghost" :disabled="currentStep <= 1" @click="goStep(currentStep - 1)">
                    上一步
                  </button>
                  <button
                    class="btn btn-primary"
                    :disabled="currentStep >= stepCount"
                    @click="goStep(currentStep + 1)"
                  >
                    下一步
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="blk">
            <div class="bh"><h2>营养信息</h2><span class="r">每份</span></div>
            <div class="nut">
              <div v-for="n in detail.nutrition" :key="n.name" class="r">
                <span>{{ n.name }}</span>
                <span class="bar"><i :style="{ width: n.width, background: n.color }"></i></span>
                <span class="v">{{ n.value }}</span>
              </div>
            </div>
          </div>

          <!-- 评价列表：仅展示管理端审核「已通过」的评论 -->
          <div class="blk">
            <div class="bh"><h2>大家怎么说（{{ reviews.length }}）</h2></div>
            <div v-if="!reviews.length" class="empty-reviews">暂无评价，做完了来抢沙发吧。</div>
            <div v-for="r in reviews" :key="r.id" class="rv">
              <div class="rv-h">
                <b>{{ r.nickname || '美食爱好者' }}</b>
                <span class="rv-stars">{{ '★'.repeat(r.rating || 0) }}{{ '☆'.repeat(5 - (r.rating || 0)) }}</span>
              </div>
              <p v-if="r.content" class="rv-c">{{ r.content }}</p>
              <div class="rv-t">{{ fmtTime(r.createTime) }}</div>
            </div>
          </div>
        </div>

        <!-- 右列 -->
        <div class="side">
          <div class="blk">
            <template v-if="missing.length">
              <div class="bh"><h2>还缺 {{ missing.length }} 样食材</h2></div>
              <div class="sub">一键加入购物清单比价，或直接一键下单。</div>
              <div v-for="(m, i) in missing" :key="m.name" class="short" :style="i ? 'border-top:1px solid #F0F3EE' : ''">
                <span>{{ m.name }}</span><span>{{ m.amount }}{{ m.unit || '' }}</span>
              </div>
              <button class="btn btn-outline btn-block" style="margin-top: 14px" @click="addToShoppingList">
                加入清单比价
              </button>
              <button
                class="btn btn-primary btn-block"
                style="margin-top: 10px"
                :disabled="ordering"
                @click="oneClickOrder"
              >
                {{ ordering ? '下单中…' : '一键下单（自动选商家）' }}
              </button>
            </template>
            <template v-else>
              <div class="bh"><h2>食材已齐备</h2></div>
              <div class="sub">这道菜的食材你的食材篮里都有，直接开始做吧。</div>
            </template>
            <p v-if="orderMsg" class="order-msg" :class="orderMsg.ok ? 'ok' : 'err'">
              {{ orderMsg.text }}
              <a v-if="orderMsg.ok" @click="goOrders">查看订单 →</a>
            </p>
          </div>

          <div class="blk">
            <div class="bh"><h2>做完了？说说感受</h2></div>
            <div class="stars">
              <span
                v-for="i in 5"
                :key="i"
                :class="{ on: i <= rating }"
                @click="rating = i"
                >{{ i <= rating ? '★' : '☆' }}</span
              >
            </div>
            <span class="rate-tag">最后成品（选填）</span>
            <textarea v-model="reviewContent" class="ta" placeholder="咸淡、火候、会不会再做…"></textarea>
            <button class="btn btn-primary btn-block" style="margin-top: 12px" @click="submitReview">
              提交评价
            </button>
            <p v-if="reviewMsg" class="order-msg" :class="reviewMsg.ok ? 'ok' : 'err'">{{ reviewMsg.text }}</p>
            <div class="sub" style="margin-top: 8px">评价提交后进入待审核，审核通过后展示给所有人。</div>
          </div>

          <div class="blk">
            <div class="bh"><h2>相似菜谱</h2></div>
            <div v-if="!similar.length" class="sub">暂无相似菜谱。</div>
            <div
              v-for="(s, i) in similar"
              :key="s.id"
              class="sim"
              :style="i ? 'border-top:1px solid #F0F3EE' : ''"
              @click="router.push(`/recipes/${s.id}`)"
            >
              <div class="p img-ph"></div>
              <div><div class="t">{{ s.name }}</div><div class="m">{{ s.meta }}</div></div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail {
  padding-bottom: 40px;
}
.crumb {
  color: var(--text3);
  font-size: 13px;
  padding: 18px 0 16px;
}
.crumb b {
  color: var(--text);
  font-weight: 600;
}
.head {
  display: grid;
  grid-template-columns: 470px 1fr;
  gap: 40px;
  align-items: center;
}
.head .big {
  height: 310px;
  border-radius: 14px;
}
.head .badges {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--text2);
}
.head h1 {
  font-size: 30px;
  font-weight: 700;
  margin: 14px 0 12px;
}
.head .desc {
  color: var(--text2);
  font-size: 14px;
  margin: 0 0 22px;
}
.head .ai-draft-tip {
  display: inline-block;
  background: var(--amber-bg);
  color: var(--amber);
  font-size: 12px;
  line-height: 1.6;
  border-radius: 8px;
  padding: 7px 12px;
  margin: 0 0 12px;
}
.meta {
  display: flex;
  gap: 38px;
  margin-bottom: 24px;
}
.meta .it .k {
  color: var(--text3);
  font-size: 12px;
}
.meta .it .v {
  font-size: 15px;
  font-weight: 600;
  margin-top: 4px;
}
.head .acts {
  display: flex;
  gap: 12px;
}
.act-msg {
  margin: 12px 0 0;
  font-size: 12px;
  color: #2f7d4f;
}
.act-msg.err {
  color: var(--danger, #c0392b);
}
.cols {
  display: grid;
  grid-template-columns: 1fr 372px;
  gap: 32px;
  margin-top: 36px;
  align-items: start;
}
.blk .bh {
  margin-bottom: 14px;
  align-items: baseline;
}
.ing {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 0;
  border-bottom: 1px solid #f0f3ee;
  font-size: 14px;
}
.ing:last-child {
  border-bottom: none;
}
.ing .nm {
  width: 150px;
}
.ing .am {
  color: var(--text2);
  flex: 1;
}
.ing .st {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}
.alt-link {
  color: var(--primary);
  cursor: pointer;
}
.alt {
  background: #edf5ee;
  border-radius: 16px;
  padding: 20px 22px;
}
.alt .t {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 14px;
}
.alt .cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.alt .c {
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
}
.alt .c .nm {
  font-weight: 700;
  font-size: 14px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.alt .c .nm .for {
  font-weight: 400;
  font-size: 11px;
  color: var(--text3);
  background: var(--chip-bg);
  border-radius: 999px;
  padding: 2px 8px;
}
.alt .c .ln {
  color: var(--text2);
  font-size: 12px;
  margin-bottom: 6px;
}
.alt .c .ln b {
  color: var(--text);
  font-weight: 600;
}
.teach {
  display: grid;
  grid-template-columns: 190px 1fr;
  gap: 20px;
}
.slist {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.slist .s {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 10px;
  font-size: 13px;
  color: var(--text2);
  cursor: pointer;
}
.slist .s .n {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #edf1ec;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--text2);
  flex: none;
}
.slist .s.on {
  background: #edf5ee;
  color: var(--chip-text);
  font-weight: 600;
}
.slist .s.on .n {
  background: var(--primary);
  color: #fff;
}
.stage .pic {
  height: 210px;
  border-radius: 12px;
  margin-bottom: 16px;
}
.stage h3 {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 10px;
}
.stage p {
  color: var(--text2);
  font-size: 13px;
  margin: 0 0 14px;
}
.tip {
  background: var(--amber-bg);
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 13px;
  color: #8a6a1c;
}
.tip b {
  color: #6e5312;
}
.stage .ctrl {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
.nut {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.nut .r {
  display: grid;
  grid-template-columns: 64px 1fr 88px;
  align-items: center;
  gap: 14px;
  font-size: 13px;
}
.nut .bar {
  height: 9px;
  border-radius: 999px;
  background: #eff3ee;
  overflow: hidden;
}
.nut .bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
}
.nut .v {
  text-align: right;
  color: var(--text2);
}
.side {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.side .sub {
  color: var(--text2);
  font-size: 12px;
  margin: 6px 0 12px;
}
.short {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  padding: 8px 0;
  color: var(--text2);
}
.order-msg {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.6;
}
.order-msg.ok {
  color: var(--success, #2f9e5f);
}
.order-msg.err {
  color: var(--danger, #d64545);
}
.order-msg a {
  color: var(--primary);
  cursor: pointer;
  margin-left: 4px;
}
.stars {
  color: #e6e9e6;
  font-size: 20px;
  letter-spacing: 3px;
  margin: 10px 0;
}
.stars .on {
  color: #f0c24b;
}
.stars span {
  cursor: pointer;
}
.rate-tag {
  display: inline-block;
  background: var(--chip-bg);
  color: var(--chip-text);
  font-size: 12px;
  border-radius: 999px;
  padding: 3px 10px;
  margin-bottom: 10px;
}
.ta {
  width: 100%;
  height: 76px;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
  font-family: inherit;
  font-size: 13px;
  outline: none;
  resize: none;
}
.sim {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  cursor: pointer;
}
.sim .p {
  width: 58px;
  height: 58px;
  border-radius: 10px;
  flex: none;
}
.sim .t {
  font-size: 13px;
  font-weight: 600;
}
.sim .m {
  color: var(--text3);
  font-size: 12px;
}
.empty-reviews {
  color: var(--text3);
  font-size: 13px;
  padding: 6px 0;
}
.rv {
  padding: 12px 0;
  border-bottom: 1px solid #f0f3ee;
}
.rv:last-child {
  border-bottom: none;
}
.rv-h {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}
.rv-stars {
  color: #f0c24b;
  letter-spacing: 2px;
}
.rv-c {
  color: var(--text2);
  font-size: 13px;
  line-height: 1.7;
  margin: 8px 0 6px;
}
.rv-t {
  color: var(--text3);
  font-size: 12px;
}
</style>
