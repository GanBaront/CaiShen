import http, { asList } from './http'
import type { AdminUser, Member, Order } from './types'

/* ---------------- 用户管理 ---------------- */
export async function fetchUsers(params: { keyword?: string } = {}): Promise<AdminUser[]> {
  const res = await http.get('/admin/users', { params: { ...params, size: 200 } })
  return asList<AdminUser>(res.data)
}
export async function updateUser(id: number, payload: Partial<AdminUser>): Promise<void> {
  await http.put(`/admin/users/${id}`, payload)
}

/* ---------------- 会员与订单 ---------------- */
export async function fetchMembers(): Promise<Member[]> {
  const res = await http.get('/admin/members', { params: { size: 200 } })
  return asList<Member>(res.data)
}
export async function fetchOrders(): Promise<Order[]> {
  const res = await http.get('/admin/orders', { params: { size: 200 } })
  return asList<Order>(res.data)
}
