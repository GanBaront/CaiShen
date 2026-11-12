<template>
  <div class="panel">
    <div class="toolbar">
      <h3 v-if="title" class="panel-title">{{ title }}</h3>
      <select v-for="f in filters" :key="f.key" v-model="filterValues[f.key]" class="select">
        <option value="">全部{{ f.label || '' }}</option>
        <option v-for="opt in f.options" :key="opt" :value="opt">{{ opt }}</option>
      </select>
      <span class="spacer" />
      <input v-if="searchPlaceholder" v-model="keyword" class="input" :placeholder="searchPlaceholder" />
      <button v-if="createLabel" class="btn btn-primary btn-sm" @click="openCreate">+ {{ createLabel }}</button>
    </div>

    <div v-if="notice" class="notice">{{ notice }}</div>

    <table class="table">
      <thead>
        <tr>
          <th v-for="c in columns" :key="c.key" :style="c.width ? { width: c.width } : undefined">{{ c.label }}</th>
          <th v-if="rowActions" class="col-actions">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in filtered" :key="rowKeyOf(row)">
          <td v-for="c in columns" :key="c.key" :class="{ muted: c.muted }" :style="c.width ? { width: c.width } : undefined">
            <span v-if="c.pill" class="pill" :class="c.pill(row[c.key], row)">{{ row[c.key] }}</span>
            <span v-else-if="c.chip" class="chip">{{ row[c.key] }}</span>
            <template v-else>{{ row[c.key] }}</template>
          </td>
          <td v-if="rowActions" class="col-actions">
            <div class="row-actions">
              <button
                v-for="a in rowActions(row)"
                :key="a.label"
                class="btn btn-sm"
                :class="a.kind === 'primary' ? 'btn-primary' : 'btn-outline'"
                @click="a.onClick()"
              >
                {{ a.label }}
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="!filtered.length" class="empty">{{ loading ? '加载中…' : '暂无数据' }}</div>

    <div v-if="modalVisible" class="modal-mask" @click.self="modalVisible = false">
      <div class="modal">
        <h3>{{ editingId === null ? createTitle || '新增' : editTitle || '编辑' }}</h3>
        <div class="form-grid">
          <div
            v-for="f in formFields"
            :key="f.key"
            class="field"
            :class="{ full: f.type === 'textarea' || f.full }"
          >
            <label>{{ f.label }}</label>
            <select v-if="f.type === 'select'" v-model="formValues[f.key]">
              <option v-for="opt in f.options || []" :key="opt" :value="opt">{{ opt }}</option>
            </select>
            <textarea v-else-if="f.type === 'textarea'" v-model="formValues[f.key]" />
            <input v-else :type="f.type === 'number' ? 'number' : 'text'" v-model="formValues[f.key]" />
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-ghost" @click="modalVisible = false">取消</button>
          <button class="btn btn-primary" @click="submit">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { Column, FilterDef, FormField, Row, RowAction } from '@/types/list'

const props = withDefaults(
  defineProps<{
    rows: Row[]
    columns: Column[]
    title?: string
    rowKey?: string
    searchPlaceholder?: string
    searchKeys?: string[]
    filters?: FilterDef[]
    /** 筛选器初始选中值，如 { auditStatus: '待审核' } */
    defaultFilters?: Record<string, string>
    createLabel?: string
    createTitle?: string
    editTitle?: string
    formFields?: FormField[]
    rowActions?: (row: Row) => RowAction[]
    loading?: boolean
    notice?: string
  }>(),
  { rowKey: 'id', searchKeys: () => [], filters: () => [], formFields: () => [], defaultFilters: () => ({}) },
)

const emit = defineEmits<{
  (e: 'submit', payload: { mode: 'create' | 'edit'; id: number | null; values: Row }): void
}>()

const keyword = ref('')
const filterValues = reactive<Record<string, string>>({})
props.filters.forEach((f) => (filterValues[f.key] = props.defaultFilters[f.key] ?? ''))

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return props.rows.filter((row) => {
    for (const f of props.filters) {
      const v = filterValues[f.key]
      if (v && String(row[f.key]) !== v) return false
    }
    if (!kw || !props.searchKeys.length) return true
    return props.searchKeys.some((k) => String(row[k] ?? '').toLowerCase().includes(kw))
  })
})

function rowKeyOf(row: Row): string | number {
  return row[props.rowKey] as string | number
}

/* ---------------- 新增 / 编辑弹窗 ---------------- */
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const formValues = reactive<Row>({})

function resetForm(source?: Row) {
  for (const key of Object.keys(formValues)) delete formValues[key]
  for (const f of props.formFields) {
    formValues[f.key] = source?.[f.key] ?? (f.type === 'select' ? (f.options?.[0] ?? '') : '')
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

watch(
  () => props.formFields,
  () => resetForm(),
)

defineExpose({
  openEdit(row: Row) {
    editingId.value = row[props.rowKey] as number
    resetForm(row)
    modalVisible.value = true
  },
})

function submit() {
  emit('submit', {
    mode: editingId.value === null ? 'create' : 'edit',
    id: editingId.value,
    values: { ...formValues },
  })
  modalVisible.value = false
}
</script>
