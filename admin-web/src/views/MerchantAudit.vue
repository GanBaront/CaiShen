<template>
  <ListPage
    :rows="rows"
    :columns="columns"
    :row-actions="actionsFor"
    :loading="loading"
    :notice="notice"
    :filters="filters"
    :default-filters="{ auditStatus: '待审核' }"
    search-placeholder="搜索商家名"
    :search-keys="['name']"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ListPage from '@/components/ListPage.vue'
import * as seed from '@/mock/seed'
import { auditMerchant, fetchMerchants } from '@/api/audit'
import type { Merchant } from '@/api/types'
import type { Column, FilterDef, Row, RowAction } from '@/types/list'

const OFFLINE_NOTICE = '接口未连接，当前展示示例数据'

const rows = ref<Merchant[]>([])
const loading = ref(false)
const notice = ref('')

const columns: Column[] = [
  { key: 'name', label: '商家名' },
  { key: 'rating', label: '好评率（%）' },
  { key: 'distanceKm', label: '距离（km）', muted: true },
  { key: 'deliveryMinutes', label: '约送达（分钟）', muted: true },
  { key: 'stockStatus', label: '库存状态' },
  {
    key: 'auditStatus',
    label: '审核状态',
    pill: (v) => (v === '已通过' ? 'pill--green' : v === '待审核' ? 'pill--amber' : 'pill--red'),
  },
]

const filters: FilterDef[] = [
  { key: 'auditStatus', label: '审核状态', options: ['待审核', '已通过', '已驳回'] },
]

function actionsFor(row: Row): RowAction[] {
  if (row.auditStatus !== '待审核') return []
  return [
    { label: '通过', kind: 'primary', onClick: () => audit(row, '通过') },
    { label: '驳回', kind: 'outline', onClick: () => audit(row, '驳回') },
  ]
}

async function audit(row: Row, result: '通过' | '驳回') {
  const target = rows.value.find((m) => m.id === row.id)
  if (target) target.auditStatus = result === '通过' ? '已通过' : '已驳回'
  try {
    await auditMerchant(row.id as number, result)
  } catch {
    notice.value = OFFLINE_NOTICE
  }
}

async function load() {
  loading.value = true
  try {
    rows.value = await fetchMerchants()
    notice.value = ''
  } catch {
    rows.value = seed.merchants.map((it) => ({ ...it }))
    notice.value = OFFLINE_NOTICE
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
