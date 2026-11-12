<template>
  <div class="stat-grid">
    <div v-for="s in statView" :key="s.key" class="stat-card">
      <div class="label">{{ s.label }}</div>
      <div class="value">{{ s.value }}</div>
      <div class="delta">
        {{ s.parts.prefix }}
        <span :class="s.deltaUp ? 'delta-up' : 'delta-down'">{{ s.parts.mark }}</span>
        {{ s.parts.suffix }}
      </div>
    </div>
  </div>

  <div class="chart-grid">
    <div class="panel">
      <div class="chart-head">
        <h3 class="panel-title">AI 菜谱生成趋势</h3>
        <span class="unit">单位：次</span>
      </div>
      <LineChart :points="trend" />
    </div>

    <div class="panel">
      <h3 class="panel-title">待处理审核</h3>
      <div v-if="pendingNotice" class="notice" style="margin-top: 12px">{{ pendingNotice }}</div>
      <div v-for="p in pendingItems" :key="p.key" class="pending-row">
        <span>{{ p.label }}</span>
        <div class="right">
          <span class="pending-count">{{ p.count }}</span>
          <span class="pending-action" @click="router.push(p.to)">{{ p.actionText }}</span>
        </div>
      </div>
    </div>
  </div>

  <ListPage
    ref="recipeListRef"
    title="菜谱管理"
    :rows="rows"
    :columns="columns"
    :form-fields="formFields"
    :row-actions="rowActionsOf"
    :loading="loading"
    :notice="recipeNotice"
    search-placeholder="搜索菜名"
    :search-keys="['name']"
    create-label="新增菜谱"
    create-title="新增菜谱"
    edit-title="编辑菜谱"
    @submit="submit"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import LineChart from '@/components/LineChart.vue'
import ListPage from '@/components/ListPage.vue'
import { useRecipes } from '@/composables/useRecipes'
import { fetchAiTrend, fetchOverview, fetchPending } from '@/api/dashboard'
import type { OverviewStat, PendingItem, TrendPoint } from '@/api/types'
import type { Row, RowAction } from '@/types/list'
import { useUiStore } from '@/stores/ui'

const router = useRouter()
const ui = useUiStore()

const {
  rows,
  loading,
  notice: recipeNotice,
  load: loadRecipes,
  columns,
  formFields,
  submit,
  rowActions,
} = useRecipes()

const recipeListRef = ref<InstanceType<typeof ListPage> | null>(null)

function rowActionsOf(row: Row): RowAction[] {
  return rowActions(row, openEdit)
}
function openEdit(row: Row) {
  recipeListRef.value?.openEdit(row)
}

const stats = ref<OverviewStat[]>([])
const trend = ref<TrendPoint[]>([])
const pendingItems = ref<PendingItem[]>([])
const pendingNotice = ref('')

const statView = computed(() => stats.value.map((s) => ({ ...s, parts: splitDelta(s.deltaText || '') })))

function splitDelta(text: string) {
  const m = text.match(/^([^↑↓]*)([↑↓]\s*\d+%)(.*)$/)
  if (!m) return { prefix: text, mark: '', suffix: '' }
  return { prefix: m[1], mark: m[2], suffix: m[3] }
}

async function loadDashboard() {
  try {
    const [o, t, p] = await Promise.all([fetchOverview(ui.range), fetchAiTrend(ui.range), fetchPending()])
    stats.value = o
    trend.value = t
    pendingItems.value = p
    pendingNotice.value = ''
  } catch {
    stats.value = []
    trend.value = []
    pendingItems.value = []
    pendingNotice.value = '数据加载失败，请检查后台服务后重试'
  }
}

watch(() => ui.range, loadDashboard)

onMounted(() => {
  loadDashboard()
  loadRecipes()
})
</script>
