<template>
  <ListPage
    :rows="rows"
    :columns="columns"
    :row-actions="actionsFor"
    :loading="loading"
    :notice="notice"
    :filters="filters"
    :default-filters="{ status: '待处理' }"
    search-placeholder="搜索反馈内容"
    :search-keys="['content']"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ListPage from '@/components/ListPage.vue'
import * as seed from '@/mock/seed'
import { fetchFeedback, handleFeedback } from '@/api/audit'
import type { Feedback } from '@/api/types'
import type { Column, FilterDef, Row, RowAction } from '@/types/list'

const OFFLINE_NOTICE = '接口未连接，当前展示示例数据'

const rows = ref<Feedback[]>([])
const loading = ref(false)
const notice = ref('')

const columns: Column[] = [
  { key: 'content', label: '反馈内容' },
  { key: 'contact', label: '联系方式', muted: true },
  { key: 'statusText', label: '状态', pill: (v) => (v === '已处理' ? 'pill--green' : 'pill--amber') },
  { key: 'createTime', label: '提交时间', muted: true },
]

const filters: FilterDef[] = [{ key: 'statusText', label: '状态', options: ['待处理', '已处理'] }]

function withStatusText(list: Feedback[]): Feedback[] {
  return list.map((it) => ({ ...it, statusText: it.status }))
}

function actionsFor(row: Row): RowAction[] {
  if (row.statusText === '已处理') return []
  return [{ label: '标记已处理', kind: 'primary', onClick: () => markHandled(row) }]
}

async function markHandled(row: Row) {
  const target = rows.value.find((f) => f.id === row.id)
  if (target) {
    target.status = '已处理'
    target.statusText = '已处理'
  }
  try {
    await handleFeedback(row.id as number)
  } catch {
    notice.value = OFFLINE_NOTICE
  }
}

async function load() {
  loading.value = true
  try {
    rows.value = withStatusText(await fetchFeedback())
    notice.value = ''
  } catch {
    rows.value = withStatusText(seed.feedback)
    notice.value = OFFLINE_NOTICE
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
