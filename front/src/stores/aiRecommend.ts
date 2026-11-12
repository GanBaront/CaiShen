import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 首页「生成菜谱」AI 结果在页面间的传递（首页 → 菜谱推荐页）。
 * 后端 /recipes/generate 已接入大模型排序并返回 aiReason 推荐理由，
 * 结果在本 store 暂存 10 分钟，推荐页首屏直接展示，调整筛选后回退常规列表。
 */
export const useAiRecommendStore = defineStore('aiRecommend', () => {
  const results = ref<any[]>([])
  const generatedAt = ref(0)

  function setResults(list: any[]) {
    results.value = Array.isArray(list) ? list : []
    generatedAt.value = Date.now()
  }

  function clear() {
    results.value = []
    generatedAt.value = 0
  }

  /** 10 分钟内的结果视为有效，返回副本；过期返回空数组 */
  function fresh(): any[] {
    if (generatedAt.value && Date.now() - generatedAt.value < 10 * 60 * 1000) {
      return [...results.value]
    }
    return []
  }

  return { results, generatedAt, setResults, clear, fresh }
})
