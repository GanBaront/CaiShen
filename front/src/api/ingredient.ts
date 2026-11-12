import { get, post, del } from './request'

export const commonIngredients = () => get<string[]>('/ingredients/common')
export const searchIngredients = (keyword: string) => get('/ingredients/search', { keyword })

/** 管理端维护的过敏源标签库（启用中的），用户端忌口/过敏源选择面板数据源 */
export const allergenTags = () => get('/ingredients/allergen-tags')

/** 我的食材篮（文字 / 上传图片 / 语音结果统一入口） */
export const myIngredients = () => get('/user/ingredients')
export const addIngredient = (data: {
  name: string
  quantity?: string | number
  unit?: string
  source?: 'text' | 'image' | 'voice'
}) => post('/user/ingredients', data)
export const removeIngredient = (id: number | string) => del(`/user/ingredients/${id}`)

/** AI 识别图片返回：食材列表（菜品图会拆解出主要食材）+ 菜名 + 匹配到的站内菜谱 */
import type { RecognitionResult } from './ai'
export type { RecognitionResult }

/** 上传食材图片，返回 { fileName, url } */
export const uploadImage = (file: File) => {
  const fd = new FormData()
  fd.append('file', file)
  return post<{ fileName: string; url: string }>('/upload/image', fd)
}

/** AI 识别图片中的食材/菜品（视觉模型，耗时较长，超时放宽到 90s） */
export const recognizeIngredients = (imageUrl: string) =>
  post<RecognitionResult>('/upload/recognize-ingredients', { imageUrl }, { timeout: 90000 })
