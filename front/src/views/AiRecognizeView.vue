<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Chip from '@/components/Chip.vue'
import RecipeCard from '@/components/RecipeCard.vue'
import { useIngredientStore } from '@/stores/ingredients'
import { useAiRecommendStore } from '@/stores/aiRecommend'
import { useRequireLogin } from '@/composables/useRequireLogin'
import * as aiApi from '@/api/ai'
import * as recipeApi from '@/api/recipe'

const router = useRouter()
const ingredient = useIngredientStore()
const aiRecommend = useAiRecommendStore()
const ensureLogin = useRequireLogin()

const fileInput = ref<HTMLInputElement | null>(null)
const cameraInput = ref<HTMLInputElement | null>(null)

/** 本地预览地址（objectURL，选图即显示，不等上传完成） */
const preview = ref('')
/** 识别阶段：idle 待上传 / uploading 上传中 / recognizing 识别中 */
const phase = ref<'idle' | 'uploading' | 'recognizing'>('idle')
const busy = computed(() => phase.value !== 'idle')
const statusText = computed(() =>
  phase.value === 'uploading' ? '上传中…' : phase.value === 'recognizing' ? 'AI 识别中…' : ''
)

const err = ref('')
const dishes = ref<string[]>([])
const names = ref<string[]>([])
const matched = ref<aiApi.MatchedRecipe[]>([])
const done = ref(false)
const manualInput = ref('')
const addMsg = ref('')
const recommendLoading = ref(false)

const hasResult = computed(() => dishes.value.length > 0 || names.value.length > 0)

/** AI 就绪状态：视觉未配置时直接提示，避免用户上传后才看到报错 */
const status = ref<aiApi.AiStatus | null>(null)
const visionReady = computed(() => status.value?.visionEnabled !== false) // 拿不到状态时不阻塞使用

onMounted(async () => {
  try {
    status.value = await aiApi.aiStatus()
  } catch {
    status.value = null
  }
})

function pick() {
  if (busy.value) return
  if (!visionReady.value) return
  if (!ensureLogin()) return
  fileInput.value?.click()
}

function shoot() {
  if (busy.value) return
  if (!visionReady.value) return
  if (!ensureLogin()) return
  cameraInput.value?.click()
}

function onFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files && input.files[0]
  input.value = ''
  if (file) recognize(file)
}

function onDrop(e: DragEvent) {
  if (busy.value) return
  if (!ensureLogin()) return
  const file = e.dataTransfer?.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    err.value = '请拖入图片文件（jpg / png / webp）'
    return
  }
  recognize(file)
}

/** 重置上一次识别结果，避免新旧结果混在一起 */
function resetResult() {
  err.value = ''
  addMsg.value = ''
  dishes.value = []
  names.value = []
  matched.value = []
  done.value = false
  manualInput.value = ''
}

async function recognize(file: File) {
  resetResult()
  if (preview.value) URL.revokeObjectURL(preview.value)
  preview.value = URL.createObjectURL(file)
  phase.value = 'uploading'
  try {
    // 上传与识别合并成一次请求（后端 /ai/recognize/file）
    phase.value = 'recognizing'
    const res = await aiApi.recognizeByFile(file)
    if (res?.imageUrl && !preview.value) preview.value = res.imageUrl
    dishes.value = Array.isArray(res?.dishes) ? res.dishes : []
    names.value = Array.isArray(res?.ingredients) ? res.ingredients : []
    matched.value = Array.isArray(res?.matchedRecipes) ? res.matchedRecipes : []
    done.value = true
    if (!hasResult.value) {
      err.value = '没认出食材，换一张更清晰、光线更好的照片试试'
    }
  } catch (e: any) {
    err.value = e?.message || '识别失败，请稍后重试'
  } finally {
    phase.value = 'idle'
  }
}

function removeName(n: string) {
  names.value = names.value.filter((x) => x !== n)
}

function addManual() {
  const raw = manualInput.value.trim()
  if (!raw) return
  raw
    .split(/[、,，\s]+/)
    .filter(Boolean)
    .forEach((n) => {
      if (!names.value.includes(n)) names.value.push(n)
    })
  manualInput.value = ''
}

/** 把识别出的食材写入「我的食材」（已存在的自动跳过） */
async function addToBasket() {
  if (!names.value.length) return
  if (!ensureLogin()) return
  const existing = new Set(ingredient.items.map((i) => i.name))
  for (const n of names.value) {
    if (!existing.has(n)) await ingredient.add(n, 'image')
  }
  addMsg.value = `已加入食材篮：${names.value.join('、')}`
}

/** 用识别出的食材走一次 AI 推荐（后端大模型排序 + 推荐理由），跳推荐页展示 */
async function recommend() {
  if (!names.value.length) {
    err.value = '还没有可用食材，先添加几样再推荐'
    return
  }
  if (!ensureLogin()) return
  err.value = ''
  recommendLoading.value = true
  try {
    const existing = new Set(ingredient.items.map((i) => i.name))
    for (const n of names.value) {
      if (!existing.has(n)) await ingredient.add(n, 'image')
    }
    const res: any = await recipeApi.generateRecipes({
      ingredients: names.value,
      tastes: ingredient.tastes,
      dietType: ingredient.dietType,
      avoidItems: ingredient.avoidFoods
    })
    aiRecommend.setResults(Array.isArray(res) ? res : [])
    router.push('/recipes')
  } catch (e: any) {
    err.value = e?.message || '推荐失败，请稍后重试'
  } finally {
    recommendLoading.value = false
  }
}

function openRecipe(id: number) {
  router.push(`/recipes/${id}`)
}

function mapCard(r: aiApi.MatchedRecipe) {
  return {
    ...r,
    time: r.cookTime != null ? `${r.cookTime} 分钟` : '',
    match: r.matchRate != null ? `${r.matchRate}%` : '',
    status:
      r.missing && r.missing.length
        ? `缺：${r.missing.join('、')}`
        : r.totalCount
          ? '食材齐全'
          : ''
  }
}
</script>

<template>
  <div class="ai">
    <div class="hd">
      <span class="badge">AI 视觉识别</span>
      <h1>拍一张，<br />告诉我这是什么</h1>
      <p class="lead">
        上传菜品或食材照片，视觉大模型会认出这是什么菜、用了哪些食材；
        认出菜品能直接给出做法，认出食材可以立刻拿去推荐今晚吃什么。
      </p>
      <div class="points">
        <div class="point"><span class="ok">1</span>成品菜 → 识别菜名 + 反推食材 + 匹配菜谱</div>
        <div class="point"><span class="ok">2</span>生鲜食材 / 冰箱同框 → 逐个列出可识别食材</div>
        <div class="point"><span class="ok">3</span>结果可编辑，确认后再入食材篮，避免误识别</div>
      </div>
    </div>

    <div class="panel">
      <!-- 上传区 -->
      <div class="uploader">
        <div
          class="drop"
          :class="{ filled: !!preview, busy, disabled: !visionReady }"
          @click="pick"
          @dragover.prevent
          @drop.prevent="onDrop"
        >
          <img v-if="preview" :src="preview" alt="待识别图片" />
          <template v-else>
            <div class="ico">📷</div>
            <div class="t">
              {{ visionReady ? '点击选择图片，或把图片拖到这里' : '视觉模型尚未配置' }}
            </div>
            <div class="d">
              {{
                visionReady
                  ? 'jpg / png / webp，单张不超过 5MB'
                  : '在后台「系统配置 → AI 服务」填写视觉模型 Key 后即可使用'
              }}
            </div>
          </template>
          <div v-if="busy" class="mask">
            <span class="spinner" />
            <span>{{ statusText }}</span>
          </div>
        </div>
        <input ref="fileInput" type="file" accept="image/*" hidden @change="onFile" />
        <input ref="cameraInput" type="file" accept="image/*" capture="environment" hidden @change="onFile" />
        <div class="acts">
          <button class="btn btn-ghost" :disabled="busy || !visionReady" @click="shoot">拍照识别</button>
          <button class="btn btn-ghost" :disabled="busy || !visionReady" @click="pick">选择图片</button>
        </div>
      </div>

      <!-- 结果区 -->
      <div class="result">
        <div v-if="!done && !busy" class="empty">
          <div class="empty-ico">🍳</div>
          <h3>识别结果会显示在这里</h3>
          <p>左边上传一张照片，AI 会告诉你这是哪道菜、用了哪些食材，并自动匹配站内做法。</p>
        </div>

        <template v-else>
          <div v-if="dishes.length" class="block">
            <div class="blk-t">识别到菜品</div>
            <div class="dish-row">
              <Chip v-for="d in dishes" :key="d" :label="d" variant="sel" />
            </div>
          </div>

          <div class="block">
            <div class="blk-t">
              识别出的食材
              <span class="blk-tip">可删除误识别项，或手动补充</span>
            </div>
            <div class="chips">
              <Chip
                v-for="n in names"
                :key="n"
                :label="n"
                closable
                @close="removeName(n)"
              />
              <span v-if="!names.length" class="chips-empty">未拆解出食材，可手动补充</span>
            </div>
            <div class="manual">
              <input
                v-model="manualInput"
                placeholder="补一个食材，如：青椒"
                @keyup.enter="addManual"
              />
              <button class="btn btn-ghost" @click="addManual">添加</button>
            </div>
          </div>

          <p v-if="err" class="err">⚠ {{ err }}</p>
          <p v-if="addMsg" class="ok">✓ {{ addMsg }}</p>

          <div class="res-acts">
            <button class="btn btn-ghost" :disabled="!names.length" @click="addToBasket">
              加入我的食材
            </button>
            <button
              class="btn btn-primary"
              :disabled="!names.length || recommendLoading"
              @click="recommend"
            >
              {{ recommendLoading ? 'AI 推荐中…' : '用这些食材推荐菜谱 →' }}
            </button>
          </div>
        </template>
      </div>
    </div>

    <!-- 匹配到的菜谱 -->
    <section v-if="matched.length" class="matched">
      <div class="sec-head">
        <h2>这道菜的做法</h2>
        <a @click="router.push('/recipes')">更多菜谱 →</a>
      </div>
      <div class="wall">
        <RecipeCard
          v-for="r in matched"
          :key="r.id"
          variant="wall"
          :name="r.name"
          :time="mapCard(r).time"
          :difficulty="r.difficulty"
          :taste="r.taste"
          :match="mapCard(r).match"
          :status="mapCard(r).status"
          @open="openRecipe(r.id)"
        />
      </div>
    </section>

    <section v-else-if="done && dishes.length" class="matched">
      <div class="no-match">
        菜谱库里还没有「{{ dishes.join('、') }}」这道菜。
        可以点上面的「用这些食材推荐菜谱」，让 AI 按你现有的食材推荐相近做法。
      </div>
    </section>

    <p class="foot-tip">
      识别由视觉大模型完成，光线不足或菜品摆盘遮挡时可能不准，请以识别结果为准人工确认。
    </p>
  </div>
</template>

<style scoped>
.ai {
  max-width: 1296px;
  margin: 0 auto;
  padding: 52px 72px 80px;
}
.hd {
  max-width: 640px;
}
.hd .badge {
  display: inline-block;
  background: var(--chip-bg);
  color: var(--chip-text);
  font-size: 13px;
  border-radius: 999px;
  padding: 5px 14px;
  margin-bottom: 20px;
}
.hd h1 {
  font-size: 40px;
  line-height: 1.24;
  margin: 0 0 18px;
  font-weight: 700;
}
.hd .lead {
  color: var(--text2);
  font-size: 15px;
  margin: 0 0 24px;
}
.points {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 32px;
}
.point {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text2);
  font-size: 14px;
}
.point .ok {
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

.panel {
  display: grid;
  grid-template-columns: 460px 1fr;
  gap: 22px;
  align-items: start;
}
.uploader,
.result {
  background: #fff;
  border-radius: 20px;
  box-shadow: var(--shadow);
  padding: 20px;
}
.result {
  min-height: 420px;
}
.drop {
  position: relative;
  border: 1.5px dashed var(--border);
  border-radius: 14px;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s;
  background: #fbfdfa;
}
.drop:hover {
  border-color: var(--primary);
}
.drop.disabled {
  cursor: not-allowed;
  background: #fafbf9;
}
.drop.disabled:hover {
  border-color: var(--border);
}
.drop.filled {
  border-style: solid;
  padding: 0;
}
.drop img {
  width: 100%;
  height: 300px;
  object-fit: cover;
  display: block;
}
.drop .ico {
  font-size: 34px;
}
.drop .t {
  font-size: 14px;
  color: var(--text);
}
.drop .d {
  font-size: 12px;
  color: var(--text3);
}
.drop .mask {
  position: absolute;
  inset: 0;
  background: rgba(31, 78, 59, 0.62);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 14px;
}
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.acts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 14px;
}

.empty {
  height: 100%;
  min-height: 380px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 6px;
}
.empty-ico {
  font-size: 40px;
}
.empty h3 {
  font-size: 16px;
  margin: 6px 0 0;
}
.empty p {
  color: var(--text2);
  font-size: 13px;
  max-width: 320px;
  line-height: 1.7;
  margin: 0;
}

.block {
  padding-bottom: 14px;
  margin-bottom: 14px;
  border-bottom: 1px solid #f0f3ee;
}
.block:last-of-type {
  border-bottom: none;
  margin-bottom: 0;
}
.blk-t {
  font-size: 13px;
  color: var(--text2);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.blk-tip {
  font-size: 12px;
  color: var(--text3);
}
.dish-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chips-empty {
  font-size: 12px;
  color: var(--text3);
  line-height: 28px;
}
.manual {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}
.manual input {
  flex: 1;
  height: 38px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0 12px;
  font-family: inherit;
  font-size: 13px;
  outline: none;
  color: var(--text);
}
.err {
  color: var(--danger);
  font-size: 12px;
  margin: 12px 0 0;
}
.ok {
  color: var(--success);
  font-size: 12px;
  margin: 12px 0 0;
}
.res-acts {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #f0f3ee;
}

.matched {
  margin-top: 42px;
}
.wall {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.no-match {
  background: #fff;
  border-radius: 16px;
  box-shadow: var(--shadow);
  padding: 28px;
  font-size: 13px;
  color: var(--text2);
  line-height: 1.8;
}
.foot-tip {
  margin-top: 26px;
  font-size: 12px;
  color: var(--text3);
  line-height: 1.7;
}

@media (max-width: 1080px) {
  .ai {
    padding: 36px 24px 60px;
  }
  .panel {
    grid-template-columns: 1fr;
  }
  .wall {
    grid-template-columns: repeat(2, 1fr);
  }
  .hd h1 {
    font-size: 32px;
  }
}
</style>
