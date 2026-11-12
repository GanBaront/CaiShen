import { get, post } from './request'

/** 会员 / 单次「AI 新菜谱」生成（大模型创作完整菜谱，耗时较长，超时放宽到 120s） */
export const generateAiRecipe = (data: { ingredients: string[]; tastes?: string[] }) =>
  post('/ai/recipes/generate', data, { timeout: 120000 })

/** AI 新菜谱预览（示例） */
export const aiRecipePreview = () => get('/ai/recipes/preview')

/** 识别结果中匹配到的菜谱卡片（结构同后端 RecipeCardVO） */
export interface MatchedRecipe {
  id: number
  name: string
  cookTime?: number
  difficulty?: string
  taste?: string
  description?: string
  source?: string
  matchRate?: number
  missing?: string[]
  substitutable?: string[]
  totalCount?: number
  ownedCount?: number
}

/** AI 图片识别结果：生鲜食材图只出 ingredients，成品菜图还会给出菜名并匹配站内菜谱 */
export interface RecognitionResult {
  /** 识别出的食材（可直接加入食材篮） */
  ingredients: string[]
  /** 识别出的菜名（成品菜图才有） */
  dishes: string[]
  /** 上传后的图片地址（上传即识别时回填，用于回显） */
  imageUrl?: string
  /** 按菜名匹配到的站内菜谱，最多 4 条 */
  matchedRecipes?: MatchedRecipe[]
}

/** 对已上传的图片做 AI 识别（视觉模型，耗时较长，超时放宽到 90s） */
export const recognizeByUrl = (imageUrl: string) =>
  post<RecognitionResult>('/ai/recognize', { imageUrl }, { timeout: 90000 })

/** 上传图片并立即识别，一次请求完成（AI 识别页主用） */
export const recognizeByFile = (file: File) => {
  const fd = new FormData()
  fd.append('file', file)
  return post<RecognitionResult>('/ai/recognize/file', fd, { timeout: 120000 })
}

/** AI 能力就绪状态：未配置模型时前端给出提示，而不是让用户点了才报错 */
export interface AiStatus {
  textEnabled: boolean
  visionEnabled: boolean
  textModel?: string
  visionModel?: string
}

export const aiStatus = () => get<AiStatus>('/ai/status')
