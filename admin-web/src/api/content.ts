import http, { asList } from './http'
import type { AllergenTag, Ingredient, Nutrition, Recipe } from './types'

/* ---------------- 菜谱库 ---------------- */
export async function fetchRecipes(params: { keyword?: string } = {}): Promise<Recipe[]> {
  // 拉全量（列表页分页/筛选在前端做），避免后端默认分页只给前 10 条导致部分菜谱无法管理
  const res = await http.get('/admin/recipes', { params: { ...params, size: 200 } })
  return asList<Recipe>(res.data)
}
export async function createRecipe(payload: Partial<Recipe>): Promise<void> {
  await http.post('/admin/recipes', payload)
}
export async function updateRecipe(id: number, payload: Partial<Recipe>): Promise<void> {
  await http.put(`/admin/recipes/${id}`, payload)
}
export async function deleteRecipe(id: number): Promise<void> {
  await http.delete(`/admin/recipes/${id}`)
}
export async function updateRecipeStatus(id: number, status: string): Promise<void> {
  await http.put(`/admin/recipes/${id}/status`, { status })
}

/* ---------------- 食材库 ---------------- */
export async function fetchIngredients(params: { keyword?: string } = {}): Promise<Ingredient[]> {
  const res = await http.get('/admin/ingredients', { params: { ...params, size: 200 } })
  return asList<Ingredient>(res.data)
}
export async function createIngredient(payload: Partial<Ingredient>): Promise<void> {
  await http.post('/admin/ingredients', payload)
}
export async function updateIngredient(id: number, payload: Partial<Ingredient>): Promise<void> {
  await http.put(`/admin/ingredients/${id}`, payload)
}
export async function deleteIngredient(id: number): Promise<void> {
  await http.delete(`/admin/ingredients/${id}`)
}

/* ---------------- 营养数据 ---------------- */
export async function fetchNutrition(): Promise<Nutrition[]> {
  const res = await http.get('/admin/nutrition', { params: { size: 200 } })
  return asList<Nutrition>(res.data)
}
export async function saveNutrition(payload: Partial<Nutrition>): Promise<void> {
  await http.put('/admin/nutrition', payload)
}

/* ---------------- 过敏源标签 ---------------- */
export async function fetchAllergenTags(): Promise<AllergenTag[]> {
  const res = await http.get('/admin/allergen-tags')
  return asList<AllergenTag>(res.data)
}
export async function createAllergenTag(payload: Partial<AllergenTag>): Promise<void> {
  await http.post('/admin/allergen-tags', payload)
}
export async function updateAllergenTag(id: number, payload: Partial<AllergenTag>): Promise<void> {
  await http.put(`/admin/allergen-tags/${id}`, payload)
}
export async function deleteAllergenTag(id: number): Promise<void> {
  await http.delete(`/admin/allergen-tags/${id}`)
}
