import { defineStore } from 'pinia'
import { ref } from 'vue'

export type RangeKey = '7' | '30'

/** 顶栏「近 7 天 / 近 30 天」时间范围（仅数据概览使用） */
export const useUiStore = defineStore('ui', () => {
  const range = ref<RangeKey>('7')
  function setRange(r: RangeKey) {
    range.value = r
  }
  return { range, setRange }
})
