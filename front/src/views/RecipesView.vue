<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Chip from '@/components/Chip.vue'
import RecipeCard from '@/components/RecipeCard.vue'
import { useIngredientStore } from '@/stores/ingredients'
import { useAiRecommendStore } from '@/stores/aiRecommend'
import { useRequireLogin } from '@/composables/useRequireLogin'
import * as recipeApi from '@/api/recipe'

const router = useRouter()
const ingredient = useIngredientStore()
const aiRecommend = useAiRecommendStore()
const ensureLogin = useRequireLogin()

const tasteOptions = ['酸', '甜', '辣', '咸', '清淡']
const dietOptions = ['不限', '素食', '生酮', '低卡']
const difficultyOptions = ['不限', '简单', '中等', '较难']
const timeOptions = ['≤ 15 分钟', '≤ 30 分钟', '不限']
const sortOptions = ['匹配度', '耗时', '难度'] as const

// 初始筛选跟随首页/store 的选择（首页点「生成菜谱」跳转过来时保持用户已选口味与饮食方式）
const tastes = ref<string[]>(
  ingredient.tastes.length ? [...ingredient.tastes] : ['清淡']
)
const dietType = ref(ingredient.dietType || '不限')
const difficulty = ref('不限')
const maxTime = ref('不限')
const servings = ref(2)
const sort = ref<(typeof sortOptions)[number]>('匹配度')
const avoidFoods = computed(() => ingredient.avoidFoods)

// 忌口/过敏源选择面板：数据源为管理端维护的过敏源标签库
const avoidPanelOpen = ref(false)

function openAvoidPanel() {
  if (!ensureLogin()) return
  avoidPanelOpen.value = !avoidPanelOpen.value
}

const hiddenCount = ref(0)
const resultCount = ref(0)
const loadError = ref('')
// 首屏是否展示「AI 智能推荐」结果（来自首页生成菜谱，10 分钟内有效）
const aiActive = ref(false)

const recipes = ref<any[]>([])

function toggleTaste(t: string) {
  const i = tastes.value.indexOf(t)
  if (i >= 0) tastes.value.splice(i, 1)
  else tastes.value.push(t)
}

const page = ref(1)
const hasMore = ref(false)
const loading = ref(false)

/**
 * 组装查询参数。
 * 注意：GET 场景下数组字段必须用逗号拼接（如 tastes=清淡,辣），
 * axios 默认会把数组序列化成 tastes[]=xx，Spring 无法绑定，导致筛选/食材匹配失效。
 * 另外：这里刻意不传 ingredients——后端传了会做「与我的食材有交集」的硬过滤，
 * 叠加口味/难度等筛选后各饮食方式结果趋同；不传时后端改用我的食材篮计算匹配度排序，
 * 列表全量展示且个性化排序仍在，切换饮食方式的差异也能直观看到。
 */
function buildParams() {
  const params: Record<string, any> = {
    dietType: dietType.value,
    servings: servings.value,
    sort: sort.value === '匹配度' ? '匹配度' : sort.value === '耗时' ? '耗时' : '难度',
    page: page.value,
    size: 12
  }
  if (difficulty.value !== '不限') params.difficulty = difficulty.value
  if (tastes.value.length) params.tastes = tastes.value.join(',')
  if (ingredient.avoidFoods.length) params.avoidItems = ingredient.avoidFoods.join(',')
  if (maxTime.value !== '不限') params.maxCookTime = Number(maxTime.value.replace(/\D/g, ''))
  return params
}

/** 后端行 -> 卡片展示字段（cookTime/matchRate/missing -> time/match/ingredientStatus） */
function mapRow(r: any) {
  const time = r.time || (r.cookTime != null ? `${r.cookTime} 分钟` : '')
  const match = r.match || (r.matchRate != null ? `${r.matchRate}%` : '')
  let ingredientStatus = r.ingredientStatus
  if (!ingredientStatus) {
    if (Array.isArray(r.missing) && r.missing.length) {
      ingredientStatus = `缺：${r.missing.join('、')}`
    } else if (Array.isArray(r.substitutable) && r.substitutable.length) {
      ingredientStatus = `缺：${r.substitutable.join('、')}（可替代）`
    } else if (r.totalCount) {
      ingredientStatus = '食材齐全'
    }
  }
  return { ...r, time, match, ingredientStatus }
}

async function loadRecipes(reset = true) {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  // 任何主动刷新（改筛选 / 排序 / 重置 / 重试）都退出 AI 推荐态，回到常规后端列表
  if (reset && aiActive.value) {
    aiActive.value = false
    aiRecommend.clear()
  }
  try {
    const res: any = await recipeApi.listRecipes(buildParams())
    const list = res?.list || res?.records || res
    const rows: any[] = (Array.isArray(list) ? list : []).map(mapRow)
    resultCount.value = Number(res?.total ?? rows.length) || 0
    hiddenCount.value = Number(res?.hiddenCount ?? 0) || 0
    hasMore.value = page.value < Number(res?.pages ?? 1)
    // 同步后端返回的收藏状态，已收藏的菜谱直接进入 favorited 集合
    const favoritedIds = rows.filter((r: any) => r.favorite).map((r: any) => r.id)
    if (reset) {
      recipes.value = rows
      favorited.value = favoritedIds
    } else {
      recipes.value = [...recipes.value, ...rows]
      for (const id of favoritedIds) {
        if (!favorited.value.includes(id)) favorited.value.push(id)
      }
    }
  } catch (e: any) {
    // 接口失败：如实提示，不再用写死的示例菜谱冒充推荐结果
    if (reset) {
      recipes.value = []
      resultCount.value = 0
      hiddenCount.value = 0
    }
    loadError.value = e?.message || '菜谱加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function loadMore() {
  page.value += 1
  loadRecipes(false)
}

function reload() {
  page.value = 1
  return loadRecipes(true)
}

// 忌口/过敏源选择后的手动刷新按钮（选择标签即出现）
const avoidHint = ref('')
const avoidRefreshing = ref(false)

async function applyAvoid() {
  if (loading.value || avoidRefreshing.value) return
  avoidRefreshing.value = true
  avoidHint.value = ''
  avoidPending.value = false
  await reload()
  avoidRefreshing.value = false
  avoidHint.value = `已刷新：${resultCount.value} 道推荐，隐藏 ${hiddenCount.value} 道`
}

function reset() {
  tastes.value = ['清淡']
  dietType.value = '不限'
  difficulty.value = '不限'
  maxTime.value = '不限'
  servings.value = 2
  avoidPending.value = false
  avoidHint.value = ''
  reload()
}

// 口味/饮食/难度/耗时/人数/排序变化自动刷新
watch(
  [tastes, dietType, difficulty, maxTime, servings, sort],
  () => {
    if (mounted) reload()
  },
  { deep: true }
)

// 忌口/过敏源变化不立即刷新：标记待刷新，由「刷新推荐」按钮应用，避免连续选多个标签时反复请求
const avoidPending = ref(false)
watch(
  avoidFoods,
  () => {
    if (mounted) avoidPending.value = true
  },
  { deep: true }
)

let mounted = false

function onUnlockAi() {
  if (!ensureLogin()) return
  router.push('/member')
}

const favorited = ref<(string | number)[]>([])
const favMsg = ref('')

async function onFavorite(r: any) {
  if (!ensureLogin()) return
  favMsg.value = ''
  const i = favorited.value.indexOf(r.id)
  const wasFavorited = i >= 0
  if (wasFavorited) favorited.value.splice(i, 1)
  else favorited.value.push(r.id)
  try {
    if (wasFavorited) await recipeApi.unfavoriteRecipe(r.id)
    else await recipeApi.favoriteRecipe(r.id)
  } catch (e: any) {
    // 接口失败回滚，避免「页面显示了但后端没存」
    if (wasFavorited) favorited.value.push(r.id)
    else favorited.value = favorited.value.filter((x) => x !== r.id)
    favMsg.value = `「${r.name}」${wasFavorited ? '取消收藏' : '收藏'}失败：${e?.message || '请稍后重试'}`
  }
}

function changeIngredients() {
  router.push('/')
}

/** 一键下单：按菜谱所缺食材自动选商家生成订单 */
const orderingId = ref<string | number | null>(null)
const orderMsg = ref<{ ok: boolean; text: string } | null>(null)

async function onOrder(r: any) {
  if (!ensureLogin()) return
  if (orderingId.value) return
  orderingId.value = r.id
  orderMsg.value = null
  try {
    const res: any = await recipeApi.createRecipeOrder(r.id)
    const merchant = res?.merchantName || '商家'
    orderMsg.value = {
      ok: true,
      text: `「${r.name}」下单成功：已向「${merchant}」下单所缺食材，订单号 ${res?.orderNo || ''}`
    }
    // 下单成功后回到「我的订单」，与用户点「一键下单」的预期一致（此前只留一句提示，容易以为没反应）
    window.setTimeout(() => {
      if (orderingId.value === null) router.push('/orders')
    }, 1200)
  } catch (e: any) {
    orderMsg.value = { ok: false, text: `「${r.name}」${e?.message || '一键下单失败，请稍后重试'}` }
  } finally {
    orderingId.value = null
  }
}

onMounted(async () => {
  await ingredient.fetchMine()
  mounted = true
  avoidPending.value = false
  avoidHint.value = ''
  // 首页刚「生成菜谱」：优先展示 AI 推荐结果（含推荐理由），不重复请求列表
  const freshAi = aiRecommend.fresh()
  if (freshAi.length) {
    const rows = freshAi.map(mapRow)
    recipes.value = rows
    resultCount.value = rows.length
    hiddenCount.value = 0
    hasMore.value = false
    favorited.value = rows.filter((r: any) => r.favorite).map((r: any) => r.id)
    aiActive.value = true
  } else {
    reload()
  }
})
</script>

<template>
  <div class="recipes">
    <div class="sec top">
      <!-- 你的食材 -->
      <div class="ing-bar card">
        <span class="lbl">你的食材</span>
        <div class="chips" style="margin: 0; gap: 10px">
          <Chip v-for="it in ingredient.items" :key="it.id" :label="it.name" />
        </div>
        <button class="btn btn-outline btn-sm" @click="changeIngredients">修改食材</button>
      </div>

      <div class="cols">
        <!-- 左栏：个性化筛选 -->
        <aside class="filter card">
          <div class="f-head">
            <h2>个性化筛选</h2>
            <a @click="reset">重置</a>
          </div>
          <div class="group">
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
          <div class="group">
            <div class="lbl">饮食方式</div>
            <div
              v-for="d in dietOptions"
              :key="d"
              class="radio"
              :class="{ on: dietType === d }"
              @click="dietType = d"
            >
              <span class="dot"></span>{{ d }}
            </div>
          </div>
          <div class="group">
            <div class="lbl">忌口 / 过敏源</div>
            <div class="chips" style="margin: 0">
              <Chip
                v-for="a in avoidFoods"
                :key="a"
                :label="a"
                closable
                @close="ingredient.removeAvoid(a)"
              />
              <Chip variant="add" label="+ 添加" @click="openAvoidPanel" />
            </div>
            <div v-if="avoidPanelOpen" class="avoid-panel">
              <div class="avoid-head">
                <span>从标签库选择（管理端维护）</span>
                <a @click="avoidPanelOpen = false">收起</a>
              </div>
              <div v-if="ingredient.allergenTags.length" class="chips" style="margin: 8px 0 0">
                <Chip
                  v-for="t in ingredient.allergenTags"
                  :key="t.id"
                  :variant="avoidFoods.includes(t.name) ? 'sel' : 'off'"
                  :label="t.name"
                  @click="ingredient.toggleAvoid(t.name, true)"
                />
              </div>
              <div v-else class="avoid-empty">管理端暂未配置启用的过敏源标签</div>
              <div class="avoid-tip">点击标签即可添加 / 移除，自动保存到你的偏好；选好后点「刷新推荐」应用过滤。</div>
            </div>
            <button
              v-if="avoidFoods.length"
              class="btn btn-outline btn-sm avoid-refresh"
              :class="{ pending: avoidPending }"
              :disabled="loading || avoidRefreshing"
              @click="applyAvoid"
            >
              {{
                loading || avoidRefreshing
                  ? '刷新中…'
                  : avoidPending
                    ? '↻ 刷新推荐（应用新忌口）'
                    : '↻ 刷新推荐'
              }}
            </button>
            <p v-if="avoidHint" class="avoid-hint">{{ avoidHint }}</p>
          </div>
          <div class="group">
            <div class="lbl">制作难度</div>
            <div class="chips" style="margin: 0">
              <Chip
                v-for="d in difficultyOptions"
                :key="d"
                :variant="difficulty === d ? 'sel' : 'off'"
                :label="d"
                @click="difficulty = d"
              />
            </div>
          </div>
          <div class="group">
            <div class="lbl">烹饪耗时</div>
            <div class="chips" style="margin: 0">
              <Chip
                v-for="t in timeOptions"
                :key="t"
                :variant="maxTime === t ? 'sel' : 'off'"
                :label="t"
                @click="maxTime = t"
              />
            </div>
          </div>
          <div class="group last">
            <div class="lbl">用餐人数</div>
            <div class="counter">
              <button class="btn-sq" @click="servings = Math.max(1, servings - 1)">−</button>
              <span class="num">{{ servings }} 人</span>
              <button class="btn-sq" @click="servings = servings + 1">+</button>
            </div>
          </div>
        </aside>

        <!-- 右栏 -->
        <section class="content">
          <div v-if="avoidFoods.length" class="alert">
            <span class="ico">⚠</span>
            已按你的忌口过滤含<b>{{ avoidFoods.join('、') }}</b>的菜谱，隐藏 {{ hiddenCount }} 道。
            <span v-if="avoidPending" class="alert-pending">忌口已更新，请点左侧「刷新推荐」应用</span>
          </div>

          <div v-if="aiActive" class="ai-banner">
            <span class="ico">✨</span>
            已根据你的食材由 <b>AI 智能排序</b>，推荐理由由大模型结合你的食材生成；调整筛选将退出 AI 排序。
          </div>

          <div class="result-head">
            <h2>{{ aiActive ? 'AI 为你推荐 ' + resultCount + ' 道菜谱' : '为你找到 ' + resultCount + ' 道菜谱' }}</h2>
            <div class="sort">
              <span class="sort-lbl">排序</span>
              <span class="sep"></span>
              <Chip
                v-for="s in sortOptions"
                :key="s"
                :variant="sort === s ? 'sel' : 'off'"
                :label="s"
                @click="sort = s"
              />
            </div>
          </div>

          <div v-if="orderMsg" class="order-msg" :class="orderMsg.ok ? 'ok' : 'err'">
            {{ orderMsg.text }}
            <a v-if="orderMsg.ok" @click="router.push('/orders')">查看订单 →</a>
          </div>

          <div v-if="loadError" class="order-msg err">
            ⚠ {{ loadError }}
            <a @click="reload">重试</a>
          </div>

          <div v-if="favMsg" class="order-msg err">
            ⚠ {{ favMsg }}
          </div>

          <div v-if="!loading && !recipes.length && !loadError" class="empty-state">
            <div class="empty-ico">🍽️</div>
            <h3>没有找到匹配的菜谱</h3>
            <p>
              当前筛选条件下暂无结果{{
                ingredient.items.length ? '，菜谱库还没有同时用到这些食材的菜' : ''
              }}。可以放宽筛选，或回首页换一批食材试试。
            </p>
            <div class="empty-acts">
              <button class="btn-ghost" @click="reset">重置筛选</button>
              <button class="btn-primary" @click="changeIngredients">去换食材</button>
            </div>
          </div>

          <div v-if="recipes.length" class="recipe-wall">
            <RecipeCard
              v-for="r in recipes"
              :key="r.id"
              variant="wall"
              show-heart
              show-order
              :name="r.name"
              :time="r.time"
              :difficulty="r.difficulty"
              :taste="r.taste"
              :match="r.match"
              :reason="r.aiReason"
              :ai-generated="!!r.aiGenerated"
              :status="r.ingredientStatus"
              :status-type="String(r.ingredientStatus || '').startsWith('缺') ? 'lack' : 'full'"
              :favorited="favorited.includes(r.id)"
              :ordering="orderingId === r.id"
              @open="router.push(`/recipes/${r.id}`)"
              @favorite="onFavorite(r)"
              @order="onOrder(r)"
            />
          </div>

          <div class="member-bar">
            <div>
              <span class="tag-amber">会员专享 · AI 新菜谱</span>
              <h3>想吃点不一样的？让 AI 用这 4 样食材创作一道新菜</h3>
              <p>会员 ¥19/月，或单次 ¥2.99</p>
            </div>
            <button class="btn" @click="onUnlockAi">解锁 AI 新菜谱</button>
          </div>

          <button v-if="hasMore" class="load-more" :disabled="loading" @click="loadMore">
            {{ loading ? '加载中…' : '加载更多' }}
          </button>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.top {
  padding-top: 26px;
  padding-bottom: 40px;
}
.ing-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 22px;
  margin-bottom: 22px;
}
.ing-bar .lbl {
  font-size: 13px;
  color: var(--text2);
}
.ing-bar .btn {
  margin-left: auto;
}
.cols {
  display: grid;
  grid-template-columns: 348px 1fr;
  gap: 28px;
  align-items: start;
}
.filter {
  padding: 20px 22px;
}
/* 忌口/过敏源标签库选择面板（数据来自管理端过敏源标签库） */
.avoid-panel {
  margin-top: 10px;
  border: 1px dashed var(--border);
  border-radius: 10px;
  background: var(--chip-bg, #f6f8f4);
  padding: 10px 12px;
}
.avoid-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text3);
}
.avoid-head a {
  color: var(--primary);
  cursor: pointer;
}
.avoid-empty {
  font-size: 12px;
  color: var(--text3);
  padding: 8px 0;
}
.avoid-tip {
  font-size: 11px;
  color: var(--text3);
  margin-top: 8px;
  line-height: 1.5;
}
.avoid-refresh {
  margin-top: 10px;
}
.avoid-refresh.pending {
  border-color: var(--amber, #e2b33a);
  color: var(--amber, #e2b33a);
}
.avoid-hint {
  font-size: 11px;
  color: #2f7d4f;
  margin: 8px 0 0;
}
.alert-pending {
  margin-left: auto;
  font-size: 11px;
  white-space: nowrap;
}
.f-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}
.f-head h2 {
  font-size: 18px;
  font-weight: 700;
}
.f-head a {
  font-size: 13px;
  color: var(--text3);
  cursor: pointer;
}
.group {
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
}
.group.last {
  border-bottom: none;
  padding-bottom: 0;
}
.group .lbl {
  font-size: 12px;
  color: var(--text2);
  margin-bottom: 10px;
}
.radio {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
  color: var(--text2);
  padding: 5px 0;
  cursor: pointer;
}
.radio .dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid #c9d6cc;
  background: #fff;
  flex: none;
}
.radio.on .dot {
  border: 5px solid var(--primary);
}
.counter {
  display: flex;
  align-items: center;
  gap: 14px;
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
.counter .num {
  font-size: 15px;
  font-weight: 600;
}
.alert {
  background: var(--danger-bg);
  color: var(--danger);
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 22px;
}
.ai-banner {
  background: var(--chip-bg);
  color: var(--chip-text);
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 22px;
  line-height: 1.5;
}
.ai-banner b {
  font-weight: 700;
}
.ai-banner .ico {
  font-size: 15px;
}
.alert b {
  font-weight: 700;
}
.alert .ico {
  flex: none;
}
.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.result-head h2 {
  font-size: 24px;
  font-weight: 700;
}
.sort {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sort-lbl {
  font-size: 13px;
  color: var(--text3);
}
.sep {
  width: 1px;
  height: 16px;
  background: var(--border);
  margin: 0 4px;
}
.recipe-wall {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.order-msg {
  margin-bottom: 16px;
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 13px;
}
.order-msg a {
  margin-left: 8px;
  color: var(--primary);
  font-weight: 600;
  cursor: pointer;
}
.order-msg.ok {
  background: rgba(47, 125, 68, 0.08);
  color: var(--primary);
}
.order-msg.err {
  background: var(--danger-bg);
  color: var(--danger);
}
.empty-state {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 56px 24px;
  text-align: center;
}
.empty-state .empty-ico {
  font-size: 40px;
  margin-bottom: 14px;
}
.empty-state h3 {
  font-size: 17px;
  margin: 0 0 8px;
}
.empty-state p {
  color: var(--text2);
  font-size: 13px;
  margin: 0 auto 20px;
  max-width: 420px;
  line-height: 1.7;
}
.empty-acts {
  display: flex;
  justify-content: center;
  gap: 14px;
}
.empty-acts button {
  height: 38px;
  padding: 0 22px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.empty-acts .btn-ghost {
  background: #fff;
  border: 1px solid var(--border);
  color: var(--text2);
}
.empty-acts .btn-primary {
  background: var(--primary);
  border: none;
  color: #fff;
}
.member-bar {
  margin-top: 22px;
  background: var(--primary);
  border-radius: 20px;
  padding: 24px 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  color: #fff;
}
.member-bar h3 {
  font-size: 18px;
  font-weight: 700;
  margin: 10px 0 6px;
}
.member-bar p {
  margin: 0;
  color: #cfe0d6;
  font-size: 13px;
}
.member-bar .btn {
  background: #fff;
  color: var(--primary);
  flex: none;
}
.load-more {
  display: block;
  margin: 26px auto 0;
  min-width: 220px;
  height: 44px;
  background: #fff;
  border: 1px solid var(--border);
  color: var(--text2);
  font-weight: 600;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
}
</style>
