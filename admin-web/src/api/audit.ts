import http, { asList } from './http'
import type { CommentItem, Feedback, Merchant, MerchantProduct } from './types'

/* ---------------- 商家审核 ---------------- */
export async function fetchMerchants(): Promise<Merchant[]> {
  const res = await http.get('/admin/merchants', { params: { size: 200 } })
  return asList<Merchant>(res.data)
}
export async function auditMerchant(id: number, result: '通过' | '驳回'): Promise<void> {
  await http.put(`/admin/merchants/${id}/audit`, { result, auditStatus: result === '通过' ? '已通过' : '已驳回' })
}

/* ---------------- 商品审核 ---------------- */
export async function fetchProducts(): Promise<MerchantProduct[]> {
  const res = await http.get('/admin/products', { params: { size: 200 } })
  return asList<MerchantProduct>(res.data)
}
export async function auditProduct(id: number, result: '通过' | '驳回'): Promise<void> {
  await http.put(`/admin/products/${id}/audit`, { result, auditStatus: result === '通过' ? '已通过' : '已驳回' })
}

/* ---------------- 评论审核 ---------------- */
export async function fetchComments(): Promise<CommentItem[]> {
  const res = await http.get('/admin/comments', { params: { size: 200 } })
  return asList<CommentItem>(res.data)
}
export async function auditComment(id: number, result: '通过' | '驳回'): Promise<void> {
  await http.put(`/admin/comments/${id}/audit`, { result, status: result === '通过' ? '已通过' : '已驳回' })
}

/* ---------------- 用户反馈 ---------------- */
export async function fetchFeedback(): Promise<Feedback[]> {
  const res = await http.get('/admin/feedback', { params: { size: 200 } })
  return asList<Feedback>(res.data)
}
export async function handleFeedback(id: number): Promise<void> {
  await http.put(`/admin/feedback/${id}/handle`, { status: '已处理' })
}
