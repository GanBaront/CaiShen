/** 后台业务实体（对齐 sql/一键建库建表sql.txt） */

export interface Recipe {
  id: number
  name: string
  cuisine: string
  difficulty: string
  taste?: string
  cookTime?: number
  source: string
  status: string
  description?: string
  [key: string]: unknown
}

export interface Ingredient {
  id: number
  name: string
  alias?: string
  category: string
  defaultUnit?: string
  status?: number
  [key: string]: unknown
}

export interface Nutrition {
  id: number
  ingredientId: number
  ingredientName?: string
  calories: number
  protein: number
  fat: number
  carbs: number
  unitDesc?: string
  [key: string]: unknown
}

export interface AllergenTag {
  id: number
  name: string
  remark?: string
  status?: number
  [key: string]: unknown
}

export interface AdminUser {
  id: number
  account: string
  nickname?: string
  phone?: string
  status: number
  createTime?: string
  [key: string]: unknown
}

export interface Member {
  id: number
  userId: number
  account?: string
  planType: string
  startTime?: string
  endTime?: string
  status?: number
  [key: string]: unknown
}

export interface Order {
  id: number
  orderNo: string
  merchantName?: string
  amount: number
  status: string
  deliveryDesc?: string
  createTime?: string
  [key: string]: unknown
}

export interface Merchant {
  id: number
  name: string
  rating: number
  distanceKm: number
  deliveryMinutes: number
  tags?: string
  stockStatus: string
  auditStatus: string
  [key: string]: unknown
}

export interface MerchantProduct {
  id: number
  merchantId: number
  merchantName?: string
  name: string
  price: number
  stock: number
  auditStatus: string
  [key: string]: unknown
}

export interface CommentItem {
  id: number
  userId: number
  recipeId: number
  recipeName?: string
  rating: number
  content: string
  status: string
  createTime?: string
  [key: string]: unknown
}

export interface Feedback {
  id: number
  userId?: number
  content: string
  contact?: string
  status: string
  createTime?: string
  [key: string]: unknown
}

export interface TrendPoint {
  label: string
  value: number
}

export interface OverviewStat {
  key: string
  label: string
  value: string
  deltaText: string
  deltaUp: boolean
}

export interface PendingItem {
  key: string
  label: string
  count: number
  actionText: string
  to: string
}

export type SysConfig = Record<string, string>
