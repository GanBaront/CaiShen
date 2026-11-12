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
    search-placeholder="搜索菜名"
    :search-keys="['name']"
    create-label="新增菜谱"
    create-title="新增菜谱"
    edit-title="编辑菜谱"
    @submit="submit"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ListPage from '@/components/ListPage.vue'
import { useRecipes } from '@/composables/useRecipes'
import type { FilterDef, Row, RowAction } from '@/types/list'

const { rows, loading, notice, load, columns, formFields, submit, rowActions, remove } = useRecipes()
const listRef = ref<InstanceType<typeof ListPage> | null>(null)

const filters: FilterDef[] = [
  { key: 'cuisine', label: '菜系', options: ['家常菜', '创意菜', '轻食'] },
  { key: 'status', label: '状态', options: ['已上线', '待审核', '已下架'] },
]

function openEdit(row: Row) {
  listRef.value?.openEdit(row)
}

function actionsFor(row: Row): RowAction[] {
  return [...rowActions(row, openEdit), { label: '删除', kind: 'outline', onClick: () => remove(row) }]
}

onMounted(load)
</script>
