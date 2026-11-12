import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as listApi from '@/api/shoppingList'
import { useUserStore } from '@/stores/user'

export interface ListItem {
  id: number | string
  name: string
  amount: string
  category: string
  checked: boolean
  /** 商家售卖规格（购买单位） */
  sellUnit?: string
  /** 商家最低参考价 */
  bestPrice?: number | string
}

export const useShoppingListStore = defineStore('shoppingList', () => {
  // 清单完全以 /shopping-list 为准，不写死示例（新用户为空即显示空态）
  const items = ref<ListItem[]>([])
  const sourceRecipes = ref<string[]>([])

  const pendingCount = computed(() => items.value.filter((i) => !i.checked).length)
  const doneCount = computed(() => items.value.filter((i) => i.checked).length)

  /** 按分类分组，未勾选项优先，已购项归入「已购」组 */
  const groups = computed(() => {
    const order: string[] = []
    const map = new Map<string, ListItem[]>()
    for (const it of items.value) {
      const cat = it.checked ? '已购' : it.category
      if (!map.has(cat)) {
        map.set(cat, [])
        order.push(cat)
      }
      map.get(cat)!.push(it)
    }
    return order.map((cat) => ({ category: cat, items: map.get(cat)! }))
  })

  async function fetchList() {
    // 游客不请求账户接口（否则 401 会被强制跳登录页）
    if (!useUserStore().isLoggedIn) {
      items.value = []
      sourceRecipes.value = []
      return
    }
    try {
      const res: any = await listApi.getShoppingList()
      if (res) {
        items.value = Array.isArray(res.items) ? res.items : []
        // 后端 sourceRecipes 是逗号分隔的字符串；若直接赋值给数组，模板 v-for
        // 会按字符遍历，把「番茄炒蛋」拆成「番」「茄」「炒」「蛋」多个 chip
        const src = res.sourceRecipes
        if (Array.isArray(src)) {
          sourceRecipes.value = src.filter(Boolean)
        } else if (typeof src === 'string' && src.trim()) {
          sourceRecipes.value = src
            .split(/[,，]/)
            .map((s: string) => s.trim())
            .filter(Boolean)
        } else {
          sourceRecipes.value = []
        }
      }
    } catch {
      /* 接口失败：保持空清单，由页面空态提示 */
    }
  }

  async function addItem(name: string, amount = '适量') {
    const n = name.trim()
    if (!n) return
    try {
      const res: any = await listApi.addShoppingItem({ name: n, amount })
      items.value.push({
        id: res?.id ?? `local-${Date.now()}`,
        name: res?.name || n,
        amount: res?.amount || amount,
        category: res?.category || '其他',
        checked: false,
        sellUnit: res?.sellUnit,
        bestPrice: res?.bestPrice
      })
    } catch {
      items.value.push({ id: `local-${Date.now()}`, name: n, amount, category: '其他', checked: false })
    }
  }

  async function toggleChecked(id: number | string) {
    const it = items.value.find((i) => i.id === id)
    if (!it) return
    it.checked = !it.checked
    try {
      await listApi.updateShoppingItem(id, { checked: it.checked })
    } catch {
      /* 降级：本地状态已更新 */
    }
  }

  return { items, sourceRecipes, pendingCount, doneCount, groups, fetchList, addItem, toggleChecked }
})
