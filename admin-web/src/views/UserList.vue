<template>
  <ListPage
    ref="listRef"
    :rows="rows"
    :columns="columns"
    :form-fields="formFields"
    :row-actions="actionsFor"
    :loading="loading"
    :notice="notice"
    edit-title="编辑用户"
    @submit="submit"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ListPage from '@/components/ListPage.vue'
import * as seed from '@/mock/seed'
import { fetchUsers, updateUser } from '@/api/user'
import type { AdminUser } from '@/api/types'
import type { Column, FormField, Row, RowAction } from '@/types/list'

const OFFLINE_NOTICE = '接口未连接，当前展示示例数据'

const rows = ref<AdminUser[]>([])
const loading = ref(false)
const notice = ref('')
const listRef = ref<InstanceType<typeof ListPage> | null>(null)

const columns: Column[] = [
  { key: 'account', label: '账号' },
  { key: 'nickname', label: '昵称' },
  { key: 'phone', label: '手机号', muted: true },
  { key: 'statusText', label: '状态', pill: (v) => (v === '正常' ? 'pill--green' : 'pill--gray') },
  { key: 'createTime', label: '注册时间', muted: true },
]

const formFields: FormField[] = [
  { key: 'nickname', label: '昵称' },
  { key: 'phone', label: '手机号' },
  { key: 'statusText', label: '状态', type: 'select', options: ['正常', '停用'] },
]

function withStatusText(list: AdminUser[]): AdminUser[] {
  return list.map((it) => ({ ...it, statusText: it.status === 0 ? '停用' : '正常' }))
}

async function load() {
  loading.value = true
  try {
    rows.value = withStatusText(await fetchUsers())
    notice.value = ''
  } catch {
    rows.value = withStatusText(seed.users)
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

async function submit(payload: { mode: 'create' | 'edit'; id: number | null; values: Row }) {
  const { id, values } = payload
  const data = {
    nickname: values.nickname,
    phone: values.phone,
    status: values.statusText === '停用' ? 0 : 1,
  } as Partial<AdminUser>
  try {
    if (id !== null) await updateUser(id, data)
    await load()
  } catch {
    const idx = rows.value.findIndex((r) => r.id === id)
    if (idx >= 0) rows.value[idx] = { ...rows.value[idx], ...data, statusText: values.statusText }
    notice.value = OFFLINE_NOTICE
  }
}

onMounted(load)
</script>
