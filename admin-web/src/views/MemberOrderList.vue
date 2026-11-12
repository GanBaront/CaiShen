<template>
  <div class="panel" style="padding: 12px 16px">
    <div class="range-switch">
      <button :class="{ on: tab === 'members' }" @click="tab = 'members'">会员订阅</button>
      <button :class="{ on: tab === 'orders' }" @click="tab = 'orders'">订单</button>
    </div>
  </div>

  <ListPage
    v-if="tab === 'members'"
    :rows="members"
    :columns="memberColumns"
    :loading="membersLoading"
    :notice="membersNotice"
    :filters="memberFilters"
    search-placeholder="搜索账号"
    :search-keys="['account']"
  />
  <ListPage
    v-else
    :rows="orders"
    :columns="orderColumns"
    :loading="ordersLoading"
    :notice="ordersNotice"
    :filters="orderFilters"
    search-placeholder="搜索订单号 / 商家"
    :search-keys="['orderNo', 'merchantName']"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ListPage from '@/components/ListPage.vue'
import * as seed from '@/mock/seed'
import { fetchMembers, fetchOrders } from '@/api/user'
import type { Member, Order } from '@/api/types'
import type { Column, FilterDef } from '@/types/list'

const OFFLINE_NOTICE = '接口未连接，当前展示示例数据'

const tab = ref<'members' | 'orders'>('members')

const members = ref<Member[]>([])
const orders = ref<Order[]>([])
const membersLoading = ref(false)
const ordersLoading = ref(false)
const membersNotice = ref('')
const ordersNotice = ref('')

const memberColumns: Column[] = [
  { key: 'account', label: '用户账号' },
  { key: 'planType', label: '会员类型', pill: (v) => (v === '会员' ? 'pill--green' : 'pill--gray') },
  { key: 'startTime', label: '开通时间', muted: true },
  { key: 'endTime', label: '到期时间', muted: true },
  { key: 'statusText', label: '状态', pill: (v) => (v === '正常' ? 'pill--green' : 'pill--gray') },
]
const memberFilters: FilterDef[] = [{ key: 'planType', label: '会员类型', options: ['免费', '会员'] }]

const orderColumns: Column[] = [
  { key: 'orderNo', label: '订单号' },
  { key: 'merchantName', label: '商家' },
  { key: 'amount', label: '金额（元）' },
  {
    key: 'status',
    label: '状态',
    pill: (v) => (v === '已完成' ? 'pill--green' : v === '配送中' ? 'pill--amber' : 'pill--gray'),
  },
  { key: 'deliveryDesc', label: '配送说明', muted: true },
  { key: 'createTime', label: '下单时间', muted: true },
]
const orderFilters: FilterDef[] = [
  { key: 'status', label: '状态', options: ['待支付', '已支付', '配送中', '已完成'] },
]

async function loadMembers() {
  membersLoading.value = true
  try {
    members.value = (await fetchMembers()).map((it) => ({ ...it, statusText: it.status === 0 ? '已过期' : '正常' }))
    membersNotice.value = ''
  } catch {
    members.value = seed.members.map((it) => ({ ...it, statusText: it.status === 0 ? '已过期' : '正常' }))
    membersNotice.value = OFFLINE_NOTICE
  } finally {
    membersLoading.value = false
  }
}

async function loadOrders() {
  ordersLoading.value = true
  try {
    orders.value = await fetchOrders()
    ordersNotice.value = ''
  } catch {
    orders.value = seed.orders.map((it) => ({ ...it }))
    ordersNotice.value = OFFLINE_NOTICE
  } finally {
    ordersLoading.value = false
  }
}

onMounted(() => {
  loadMembers()
  loadOrders()
})
</script>
