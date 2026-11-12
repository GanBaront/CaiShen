import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as ingredientApi from '@/api/ingredient'
import * as userApi from '@/api/user'
import { useUserStore } from '@/stores/user'

export interface IngredientItem {
  id: number | string
  name: string
}

export const useIngredientStore = defineStore('ingredient', () => {
  // 食材篮与常用食材均以接口为准，不再写死示例（游客 / 接口失败时为空）
  const items = ref<IngredientItem[]>([])
  const common = ref<string[]>([])
  const tastes = ref<string[]>(['清淡'])
  const dietType = ref<string>('不限')
  // 忌口/过敏源默认为空，登录后以 /user/preference 返回为准（不写死「花生/香菜」）
  const avoidFoods = ref<string[]>([])
  // 管理端「过敏源标签库」维护的标签（公开接口，游客可读）
  const allergenTags = ref<any[]>([])
  // fetchMine 请求期间用户是否有本地增删（避免响应返回后覆盖用户刚做的操作）
  let itemsDirty = false

  // ===== 忌口 / 过敏源与用户偏好（t_user_avoid_item）联动 =====
  // 后端分两类存储：忌口食材（type=1）与过敏源（type=2），avoidFoods 是两者合并后的展示视图
  const avoidItems = ref<string[]>([])
  const allergenItems = ref<string[]>([])
  let avoidDirty = false
  // 偏好里的默认用餐人数（保存忌口时需要回传，避免覆盖丢失）
  let cachedServings = 2

  function mergeAvoid() {
    avoidFoods.value = [...new Set([...avoidItems.value, ...allergenItems.value])]
  }

  /** 登录后拉取忌口/过敏源（未登录请求失败静默保持为空） */
  async function fetchAvoid() {
    avoidDirty = false
    // 游客不要请求账户接口：后端返回 code 401 会被全局拦截器强制跳登录页
    if (!useUserStore().isLoggedIn) {
      avoidItems.value = []
      allergenItems.value = []
      avoidFoods.value = []
      return
    }
    try {
      const res: any = await userApi.getPreference()
      if (avoidDirty) return
      avoidItems.value = Array.isArray(res?.avoidItems) ? res.avoidItems : []
      allergenItems.value = Array.isArray(res?.allergenItems) ? res.allergenItems : []
      if (res?.defaultServings) cachedServings = res.defaultServings
      mergeAvoid()
    } catch {
      /* 未登录或降级：保持为空 */
    }
  }

  /** 把忌口/过敏源全量保存到用户偏好（未登录静默跳过，本地仍然生效） */
  async function persistAvoid() {
    try {
      await userApi.updatePreference({
        tastes: tastes.value,
        dietType: dietType.value,
        defaultServings: cachedServings,
        avoidItems: avoidItems.value,
        allergenItems: allergenItems.value
      })
    } catch {
      /* 未登录：仅本地生效 */
    }
  }

  async function fetchMine() {
    itemsDirty = false
    // 忌口 / 标签库先就绪，保证页面首次拉取推荐时已带上用户忌口
    await Promise.all([fetchAvoid(), fetchTags()])
    // 游客不请求账户接口（否则 401 会被强制跳登录页）
    if (!useUserStore().isLoggedIn) {
      items.value = []
      return
    }
    try {
      const list: any = await ingredientApi.myIngredients()
      if (Array.isArray(list) && !itemsDirty) items.value = list
    } catch {
      /* 接口失败：食材篮保持为空 */
    }
  }

  /** 拉取管理端维护的过敏源标签库（启用中的） */
  async function fetchTags() {
    try {
      const list: any = await ingredientApi.allergenTags()
      if (Array.isArray(list)) allergenTags.value = list
    } catch {
      /* 降级：保持为空 */
    }
  }

  async function fetchCommon() {
    try {
      const list: any = await ingredientApi.commonIngredients()
      if (Array.isArray(list) && list.length) {
        // 后端返回 Ingredient 对象（{id,name,...}），只取名称，兼容纯字符串
        common.value = list
          .map((it: any) => (typeof it === 'string' ? it : String(it?.name ?? '')).trim())
          .filter(Boolean)
      }
    } catch {
      /* 降级 */
    }
  }

  function addLocal(name: string) {
    const n = name.trim()
    if (!n || items.value.some((i) => i.name === n)) return
    itemsDirty = true
    items.value.push({ id: `local-${Date.now()}-${n}`, name: n })
  }

  async function add(name: string, source: 'text' | 'image' | 'voice' = 'text') {
    const n = name.trim()
    if (!n) return
    itemsDirty = true
    try {
      const res: any = await ingredientApi.addIngredient({ name: n, source })
      if (res?.id) items.value.push({ id: res.id, name: res.name || n })
      else addLocal(n)
    } catch {
      addLocal(n)
    }
  }

  async function remove(id: number | string) {
    itemsDirty = true
    // 本地降级添加的食材（id 形如 local-xxx）没有后端记录，只做本地移除
    if (typeof id !== 'string' || !id.startsWith('local-')) {
      try {
        await ingredientApi.removeIngredient(id)
      } catch {
        /* 降级：仍然本地移除 */
      }
    }
    items.value = items.value.filter((i) => i.id !== id)
  }

  function toggleTaste(t: string) {
    const i = tastes.value.indexOf(t)
    if (i >= 0) tastes.value.splice(i, 1)
    else tastes.value.push(t)
  }

  /** 添加忌口/过敏源（allergen=true 表示来自过敏源标签库）并自动保存 */
  function addAvoid(name: string, allergen = false) {
    const n = name.trim()
    if (!n) return
    avoidDirty = true
    if (!avoidFoods.value.includes(n)) avoidFoods.value.push(n)
    const target = allergen ? allergenItems : avoidItems
    if (!target.value.includes(n)) target.value.push(n)
    persistAvoid()
  }

  /** 移除忌口（两类中同名都会移除）并自动保存 */
  function removeAvoid(name: string) {
    avoidDirty = true
    avoidFoods.value = avoidFoods.value.filter((n) => n !== name)
    avoidItems.value = avoidItems.value.filter((n) => n !== name)
    allergenItems.value = allergenItems.value.filter((n) => n !== name)
    persistAvoid()
  }

  /** 标签库点选切换：已选则移除，未选则加入（过敏源类） */
  function toggleAvoid(name: string, allergen = true) {
    if (avoidFoods.value.includes(name)) removeAvoid(name)
    else addAvoid(name, allergen)
  }

  return {
    items,
    common,
    tastes,
    dietType,
    avoidFoods,
    allergenTags,
    fetchMine,
    fetchCommon,
    fetchTags,
    fetchAvoid,
    add,
    remove,
    toggleTaste,
    addAvoid,
    removeAvoid,
    toggleAvoid
  }
})
