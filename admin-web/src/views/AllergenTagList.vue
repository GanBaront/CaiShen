<template>
  <ListPage
    ref="listRef"
    :rows="rows"
    :columns="columns"
    :form-fields="formFields"
    :row-actions="actionsFor"
    :loading="loading"
    :notice="notice"
    search-placeholder="搜索标签名"
    :search-keys="['name']"
    create-label="新增标签"
    create-title="新增过敏源标签"
    edit-title="编辑过敏源标签"
    @submit="submit"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ListPage from '@/components/ListPage.vue'
import * as seed from '@/mock/seed'
import { createAllergenTag, deleteAllergenTag, fetchAllergenTags, updateAllergenTag } from '@/api/content'
import type { AllergenTag } from '@/api/types'
import type { Column, FormField, Row, RowAction } from '@/types/list'

const OFFLINE_NOTICE = '接口未连接，当前展示示例数据'

const rows = ref<AllergenTag[]>([])
const loading = ref(false)
const notice = ref('')
const listRef = ref<InstanceType<typeof ListPage> | null>(null)

const columns: Column[] = [
  { key: 'name', label: '标签名' },
  { key: 'remark', label: '说明', muted: true },
  { key: 'statusText', label: '状态', pill: (v) => (v === '启用' ? 'pill--green' : 'pill--gray') },
]

const formFields: FormField[] = [
  { key: 'name', label: '标签名' },
  { key: 'remark', label: '说明' },
  { key: 'statusText', label: '状态', type: 'select', options: ['启用', '停用'] },
]

function withStatusText(list: AllergenTag[]): AllergenTag[] {
  return list.map((it) => ({ ...it, statusText: it.status === 0 ? '停用' : '启用' }))
}

async function load() {
  loading.value = true
  try {
    rows.value = withStatusText(await fetchAllergenTags())
    notice.value = ''
  } catch {
    rows.value = withStatusText(seed.allergenTags)
    notice.value = OFFLINE_NOTICE
  } finally {
    loading.value = false
  }
}

function openEdit(row: Row) {
  listRef.value?.openEdit(row)
}

function actionsFor(row: Row): RowAction[] {
  return [
    { label: '编辑', kind: 'outline', onClick: () => openEdit(row) },
    { label: '删除', kind: 'outline', onClick: () => remove(row) },
  ]
}

async function submit(payload: { mode: 'create' | 'edit'; id: number | null; values: Row }) {
  const { mode, id, values } = payload
  const data = {
    name: values.name,
    remark: values.remark,
    status: values.statusText === '停用' ? 0 : 1,
  } as Partial<AllergenTag>
  try {
    if (mode === 'create') await createAllergenTag(data)
    else if (id !== null) await updateAllergenTag(id, data)
    await load()
  } catch {
    if (mode === 'create') {
      rows.value = [{ id: nextId(), statusText: '启用', ...data } as AllergenTag, ...rows.value]
    } else {
      const idx = rows.value.findIndex((r) => r.id === id)
      if (idx >= 0) rows.value[idx] = { ...rows.value[idx], statusText: values.statusText, ...data }
    }
    notice.value = OFFLINE_NOTICE
  }
}

async function remove(row: Row) {
  try {
    await deleteAllergenTag(row.id as number)
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
