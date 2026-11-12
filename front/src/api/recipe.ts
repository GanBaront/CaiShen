import { get, post, del } from './request'

export interface RecipeQuery {
  tastes?: string[]
  dietType?: string
  /** 与后端 RecipeQueryDTO.avoidItems 对应（此前误传 avoidFoods 导致忌口过滤失效） */
  avoidItems?: string[]
  difficulty?: string
  /** 与后端 RecipeQueryDTO.maxCookTime 对应（此前误传 maxTime 导致耗时筛选失效） */
  maxCookTime?: number
  servings?: number
  sort?: string
  page?: number
  size?: number
  /** 我的食材（用于匹配度计算与过滤） */
  ingredients?: string[]
}

/** 首页「生成菜谱」：后端已接入大模型排序（含 aiReason 推荐理由），AI 调用较慢，超时放宽到 90s */
export const generateRecipes = (data: {
  ingredients: string[]
  tastes?: string[]
  dietType?: string
  /** 与后端 RecipeGenerateDTO.avoidItems 对应 */
  avoidItems?: string[]
  servings?: number
}) => post('/recipes/generate', data, { timeout: 90000 })

export const listRecipes = (params: RecipeQuery) => get('/recipes', params as Record<string, any>)
export const todayRecipes = () => get('/recipes/today')
export const recipeDetail = (id: string | number) => get(`/recipes/${id}`)
export const similarRecipes = (id: string | number) => get(`/recipes/${id}/similar`)

export const favoriteRecipe = (id: string | number) => post(`/recipes/${id}/favorite`)
export const unfavoriteRecipe = (id: string | number) => del(`/recipes/${id}/favorite`)
export const addRecipeToShoppingList = (id: string | number) => post(`/recipes/${id}/shopping-list`)

/** 菜谱推荐 · 一键下单（按还缺的食材自动选最优商家） */
export const createRecipeOrder = (id: string | number) => post(`/recipes/${id}/order`)

/** 菜谱评价列表（仅管理端审核「已通过」的评论） */
export const recipeReviews = (id: string | number) => get(`/recipes/${id}/reviews`)

/** 提交菜谱评价（进入待审核，管理端审核通过后展示） */
export const submitRecipeReview = (id: string | number, data: { rating: number; content?: string }) =>
  post(`/recipes/${id}/review`, data)
