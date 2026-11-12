import { get, put } from './request'

export interface Preference {
  tastes: string[]
  dietType: string
  defaultServings: number
  /** 忌口食材（后端字段名 avoidItems） */
  avoidItems: string[]
  /** 过敏源（后端字段名 allergenItems） */
  allergenItems: string[]
}

export const getProfile = () => get('/user/profile')
export const updateProfile = (data: { nickname?: string; phone?: string; avatar?: string }) =>
  put('/user/profile', data)

export const getPreference = () => get<Preference>('/user/preference')
export const updatePreference = (data: Preference) => put('/user/preference', data)

export const getHistory = () => get('/user/history')
export const getFavorites = () => get('/user/favorites')

export interface FreeQuota {
  used: number
  /** 后端字段名为 limit（每日上限） */
  limit: number
  remaining: number
  member?: boolean
}
export const getFreeQuota = () => get<FreeQuota>('/user/free-quota')
