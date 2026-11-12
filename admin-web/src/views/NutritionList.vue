<template>
  <ListPage
    ref="listRef"
    :rows="rows"
    :columns="columns"
    :form-fields="formFields"
    :row-actions="actionsFor"
    :loading="loading"
    :notice="notice"
    search-placeholder="搜索食材名"
    :search-keys="['ingredientName']"
    create-label="新增营养数据"
    create-title="新增营养数据"
    edit-title="编辑营养数据"
    @submit="submit"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ListPage from '@/components/ListPage.vue'
import * as seed from '@/mock/seed'
import { fetchNutrition, saveNutrition } from '@/api/content'
import type { Nutrition } from '@/api/types'
import type { Column, FormField, Row, RowAction } from '@/types/list'

const OFFLINE_NOTICE = '接口未连接，当前展示示例数据'

const rows = ref<Nutrition[]>([])
const loading = ref(false)
const notice = ref('')
const listRef = ref<InstanceType<typeof ListPage> | null>(null)

const columns: Column[] = [
  { key: 'ingredientName', label: '食材' },
  { key: 'calories', label: '热量（kcal）' },
  { key: 'protein', label: '蛋白质（g）' },
  { key: 'fat', label: '脂肪（g）' },
  { key: 'carbs', label: '碳水（g）' },
  { key: 'unitDesc', label: '单位', muted: true },
]

const formFields: FormField[] = [
  { key: 'ingredientName', label: '食材名' },
  { key: 'unitDesc', label: '计量单位说明' },
  { key: 'calories', label: '热量（kcal）', type: 'number' },
  { key: 'protein', label: '蛋白质（g）', type: 'number' },
  { key: 'fat', label: '脂肪（g）', type: 'number' },
  { key: 'carbs', label: '碳水（g）', type: 'number' },
]

async function load() {
  loading.value = true
  try {
    rows.value = await fetchNutrition()
    notice.value = ''
  } catch {
    rows.value = seed.nutrition.map((it) => ({ ...it }))
    notice.value = OFFLINE_NOTICE
  } finally {
    loading.value = false
  }
}

function openEdit(row: Row) {
  listRef.value?.openEdit(row)
}

function actionsFor(row: Row): RowAction[] {
  return [{ label: '编辑', kind: 'outline', onClick: () => openEdit(row) }]
}

function toNumber(v: unknown) {
  const n = Number(v)
  return Number.isNaN(n) ? 0 : n
}

async function submit(payload: { mode: 'create' | 'edit'; id: number | null; values: Row }) {
  const { mode, id, values } = payload
  const data = {
    ...values,
    calories: toNumber(values.calories),
    protein: toNumber(values.protein),
    fat: toNumber(values.fat),
    carbs: toNumber(values.carbs),
  } as Partial<Nutrition>
  try {
    // 营养数据接口为 GET/PUT（按食材 upsert）
    await saveNutrition(id !== null ? { id, ...data } : data)
    await load()
  } catch {
    if (mode === 'create') {
      rows.value = [{ id: nextId(), ingredientId: 0, ...data } as Nutrition, ...rows.value]
    } else {
      const idx = rows.value.findIndex((r) => r.id === id)
      if (idx >= 0) rows.value[idx] = { ...rows.value[idx], ...data }
    }
    notice.value = OFFLINE_NOTICE
  }
}

function nextId() {
  return rows.value.reduce((max, r) => Math.max(max, Number(r.id) || 0), 0) + 1
}

onMounted(load)
</script>
