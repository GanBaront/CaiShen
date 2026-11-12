<template>
  <ListPage
    :rows="rows"
    :columns="columns"
    :row-actions="actionsFor"
    :loading="loading"
    :notice="notice"
    :filters="filters"
    :default-filters="{ status: '待审核' }"
    search-placeholder="搜索菜谱名 / 评论内容"
    :search-keys="['recipeName', 'content']"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ListPage from '@/components/ListPage.vue'
import * as seed from '@/mock/seed'
import { auditComment, fetchComments } from '@/api/audit'
import type { CommentItem } from '@/api/types'
import type { Column, FilterDef, Row, RowAction } from '@/types/list'

const OFFLINE_NOTICE = '接口未连接，当前展示示例数据'

const rows = ref<CommentItem[]>([])
const loading = ref(false)
const notice = ref('')

const columns: Column[] = [
  { key: 'recipeName', label: '菜谱' },
  { key: 'userId', label: '用户 ID', muted: true },
  { key: 'rating', label: '评分' },
  { key: 'content', label: '评论内容' },
  {
    key: 'status',
    label: '状态',
    pill: (v) => (v === '已通过' ? 'pill--green' : v === '待审核' ? 'pill--amber' : 'pill--red'),
  },
  { key: 'createTime', label: '提交时间', muted: true },
]

const filters: FilterDef[] = [{ key: 'status', label: '状态', options: ['待审核', '已通过', '已驳回'] }]

function actionsFor(row: Row): RowAction[] {
  if (row.status !== '待审核') return []
  return [
    { label: '通过', kind: 'primary', onClick: () => audit(row, '通过') },
    { label: '驳回', kind: 'outline', onClick: () => audit(row, '驳回') },
  ]
}

async function audit(row: Row, result: '通过' | '驳回') {
  const target = rows.value.find((c) => c.id === row.id)
  if (target) target.status = result === '通过' ? '已通过' : '已驳回'
  try {
    await auditComment(row.id as number, result)
  } catch {
    notice.value = OFFLINE_NOTICE
  }
}

async function load() {
  loading.value = true
  try {
    rows.value = await fetchComments()
    notice.value = ''
  } catch {
    rows.value = seed.comments.map((it) => ({ ...it }))
    notice.value = OFFLINE_NOTICE
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
