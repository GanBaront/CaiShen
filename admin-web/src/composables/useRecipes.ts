import { createRecipe, deleteRecipe, fetchRecipes, updateRecipe, updateRecipeStatus } from '@/api/content'
import type { Recipe } from '@/api/types'
import * as seed from '@/mock/seed'
import { useResource } from './useResource'
import type { Column, FormField, Row, RowAction } from '@/types/list'

/** 菜谱库逻辑（数据概览的「菜谱管理」与「菜谱库」页共用） */
export function useRecipes() {
  const { rows, loading, notice, load, OFFLINE_NOTICE } = useResource<Recipe>(
    () => fetchRecipes(),
    seed.recipes,
  )

  const columns: Column[] = [
    { key: 'name', label: '菜名' },
    { key: 'cuisine', label: '菜系' },
    { key: 'difficulty', label: '难度' },
    { key: 'source', label: '来源', pill: (v) => (v === 'AI生成' ? 'pill--dark' : 'pill--gray') },
    {
      key: 'status',
      label: '状态',
      pill: (v) => (v === '已上线' ? 'pill--green' : v === '待审核' ? 'pill--amber' : 'pill--gray'),
    },
  ]

  const formFields: FormField[] = [
    { key: 'name', label: '菜名' },
    { key: 'cuisine', label: '菜系', type: 'select', options: ['家常菜', '创意菜', '轻食'] },
    { key: 'difficulty', label: '难度', type: 'select', options: ['简单', '中等', '较难'] },
    { key: 'cookTime', label: '耗时（分钟）', type: 'number' },
    { key: 'taste', label: '口味' },
    { key: 'source', label: '来源', type: 'select', options: ['人工', 'AI生成'] },
    { key: 'status', label: '状态', type: 'select', options: ['已上线', '待审核', '已下架'] },
    { key: 'description', label: '描述', type: 'textarea' },
  ]

  async function submit(payload: { mode: 'create' | 'edit'; id: number | null; values: Row }) {
    const { mode, id, values } = payload
    const data = {
      ...values,
      cookTime: values.cookTime === '' ? undefined : Number(values.cookTime),
    } as Partial<Recipe>
    try {
      if (mode === 'create') await createRecipe(data)
      else if (id !== null) await updateRecipe(id, data)
      await load()
    } catch {
      // 后端不可用时本地更新，保证页面可用
      if (mode === 'create') {
        rows.value = [{ id: nextId(), ...data } as Recipe, ...rows.value]
      } else {
        const idx = rows.value.findIndex((r) => r.id === id)
        if (idx >= 0) rows.value[idx] = { ...rows.value[idx], ...data }
      }
      notice.value = OFFLINE_NOTICE
    }
  }

  async function setStatus(row: Row, status: string) {
    row.status = status
    try {
      await updateRecipeStatus(row.id as number, status)
    } catch {
      notice.value = OFFLINE_NOTICE
    }
  }

  async function remove(row: Row) {
    try {
      await deleteRecipe(row.id as number)
      await load()
    } catch {
      rows.value = rows.value.filter((r) => r.id !== row.id)
      notice.value = OFFLINE_NOTICE
    }
  }

  function rowActions(row: Row, onEdit: (row: Row) => void): RowAction[] {
    const actions: RowAction[] = [{ label: '编辑', kind: 'outline', onClick: () => onEdit(row) }]
    if (row.status === '待审核') actions.push({ label: '通过', kind: 'primary', onClick: () => setStatus(row, '已上线') })
    else if (row.status === '已上线') actions.push({ label: '下架', kind: 'outline', onClick: () => setStatus(row, '已下架') })
    else actions.push({ label: '上架', kind: 'outline', onClick: () => setStatus(row, '已上线') })
    return actions
  }

  function nextId() {
    return rows.value.reduce((max, r) => Math.max(max, Number(r.id) || 0), 0) + 1
  }

  return { rows, loading, notice, load, columns, formFields, submit, setStatus, remove, rowActions }
}
