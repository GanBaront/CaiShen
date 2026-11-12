<template>
  <ListPage
    ref="listRef"
    :rows="rows"
    :columns="columns"
    :form-fields="formFields"
    :row-actions="actionsFor"
    :loading="loading"
    :notice="notice"
    :filters="filters"
    search-placeholder="搜索食材名"
    :search-keys="['name', 'alias']"
    create-label="新增食材"
    create-title="新增食材"
    edit-title="编辑食材"
    @submit="submit"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ListPage from '@/components/ListPage.vue'
import * as seed from '@/mock/seed'
import { createIngredient, deleteIngredient, fetchIngredients, updateIngredient } from '@/api/content'
import type { Ingredient } from '@/api/types'
import type { Column, FilterDef, FormField, Row, RowAction } from '@/types/list'

const OFFLINE_NOTICE = '接口未连接，当前展示示例数据'

const rows = ref<Ingredient[]>([])
const loading = ref(false)
const notice = ref('')
const listRef = ref<InstanceType<typeof ListPage> | null>(null)

const columns: Column[] = [
  { key: 'name', label: '食材名' },
  { key: 'alias', label: '别名', muted: true },
  { key: 'category', label: '分类' },
  { key: 'defaultUnit', label: '默认单位' },
  { key: 'statusText', label: '状态', pill: (v) => (v === '正常' ? 'pill--green' : 'pill--gray') },
]

const filters: FilterDef[] = [
  { key: 'category', label: '分类', options: ['蔬菜', '肉类', '蛋类', '调味', '主食', '其他'] },
]

const formFields: FormField[] = [
  { key: 'name', label: '食材名' },
  { key: 'alias', label: '别名（逗号分隔）' },
  { key: 'category', label: '分类', type: 'select', options: ['蔬菜', '肉类', '蛋类', '调味', '主食', '其他'] },
  { key: 'defaultUnit', label: '默认单位' },
]

function withStatusText(list: Ingredient[]): Ingredient[] {
  return list.map((it) => ({ ...it, statusText: it.status === 0 ? '停用' : '正常' }))
}

async function load() {
  loading.value = true
  try {
    rows.value = withStatusText(await fetchIngredients())
    notice.value = ''
  } catch {
    rows.value = withStatusText(seed.ingredients)
    notice.value = OFFLINE_NOTICE
  } finally {
    loading.value = false
  }
}

function openEdit(row: Row) {
  listRef.value?.openEdit({ ...row, statusText: row.status === 0 ? '停用' : '正常' })
}

function actionsFor(row: Row): RowAction[] {
  return [
    { label: '编辑', kind: 'outline', onClick: () => openEdit(row) },
    { label: '删除', kind: 'outline', onClick: () => remove(row) },
  ]
}

async function submit(payload: { mode: 'create' | 'edit'; id: number | null; values: Row }) {
  const { mode, id, values } = payload
  try {
    if (mode === 'create') await createIngredient(values as Partial<Ingredient>)
    else if (id !== null) await updateIngredient(id, values as Partial<Ingredient>)
    await load()
  } catch {
    if (mode === 'create') {
      rows.value = [
        { id: nextId(), status: 1, ...values, statusText: '正常' } as unknown as Ingredient,
        ...rows.value,
      ]
    } else {
      const idx = rows.value.findIndex((r) => r.id === id)
      if (idx >= 0) rows.value[idx] = { ...rows.value[idx], ...values }
    }
    notice.value = OFFLINE_NOTICE
  }
}

async function remove(row: Row) {
  try {
    await deleteIngredient(row.id as number)
    await load()
  } catch {
    rows.value = rows.value.filter((r) => r.id !== row.id)
    notice.value = OFFLINE_NOTICE
  }
}

function nextId() {
  return rows.value.reduce((max, r) => Math.max(max, Number(r.id) || 0), 0) + 1
}

onMounted(load)
</script>
