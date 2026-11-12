<template>
  <svg class="line-chart" :viewBox="`0 0 ${W} ${H}`" preserveAspectRatio="none">
    <line v-for="g in gridYs" :key="`g${g.y}`" :x1="padLeft" :y1="g.y" :x2="W - padRight" :y2="g.y" stroke="#E3E8E2" stroke-width="1" />
    <text v-for="g in gridYs" :key="`t${g.y}`" :x="padLeft - 10" :y="g.y + 4" text-anchor="end" fill="#9AA79E" font-size="12">
      {{ g.value }}
    </text>
    <polyline :points="polyline" fill="none" stroke="#3E9A6B" stroke-width="2" stroke-linejoin="round" />
    <circle v-for="(p, i) in normalized" :key="`p${i}`" :cx="p.x" :cy="p.y" r="4.5" fill="#fff" stroke="#3E9A6B" stroke-width="2" />
    <text
      v-for="p in xLabels"
      :key="`x${p.i}`"
      :x="p.x"
      :y="H - 6"
      text-anchor="middle"
      fill="#6B7A70"
      font-size="12"
    >
      {{ p.label }}
    </text>
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TrendPoint } from '@/api/types'

const props = withDefaults(
  defineProps<{
    points: TrendPoint[]
    max?: number
  }>(),
  {},
)

const W = 620
const H = 230
const padLeft = 44
const padRight = 16
const padTop = 14
const padBottom = 30

const gridCount = 4

/** Y 轴最大值：未传 max 时按数据自动取整（1/2/5 × 10^n） */
const maxValue = computed(() => {
  if (props.max && props.max > 0) return props.max
  const raw = props.points.reduce((m, p) => Math.max(m, p.value), 0)
  if (raw <= 0) return 10
  const base = Math.pow(10, Math.floor(Math.log10(raw)))
  for (const k of [1, 2, 5, 10]) {
    if (raw <= k * base) return k * base
  }
  return 10 * base
})

const gridYs = computed(() =>
  Array.from({ length: gridCount + 1 }, (_, i) => {
    const value = Math.round((maxValue.value / gridCount) * i)
    return { value, y: H - padBottom - ((H - padTop - padBottom) * i) / gridCount }
  }),
)

const normalized = computed(() => {
  const list = props.points
  const innerW = W - padLeft - padRight
  const step = list.length > 1 ? innerW / (list.length - 1) : 0
  return list.map((p, i) => ({
    x: padLeft + step * i,
    y: H - padBottom - ((H - padTop - padBottom) * Math.max(0, Math.min(p.value, maxValue.value))) / maxValue.value,
  }))
})

const polyline = computed(() => normalized.value.map((p) => `${p.x},${p.y}`).join(' '))

/** x 轴标签：点太多时抽样显示，保证首尾都有 */
const xLabels = computed(() => {
  const list = normalized.value
  const every = Math.max(1, Math.ceil(list.length / 10))
  return list
    .map((p, i) => ({ ...p, i, label: props.points[i]?.label ?? '' }))
    .filter((p, idx, arr) => p.i % every === 0 || idx === arr.length - 1)
})
</script>
