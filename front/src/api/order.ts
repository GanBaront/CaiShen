import { get, post } from './request'

/** 全部加入购物车 → 下单 */
export const createOrder = (data: { merchantId: number | string; merchantName?: string; amount?: number }) =>
  post('/orders', data)

/** 我的订单（列表 + 状态计数） */
export const myOrders = () => get('/orders')

/** 订单详情（查看进度） */
export const orderDetail = (id: number | string) => get(`/orders/${id}`)

/** 确认收货（已支付 / 配送中 → 已完成） */
export const confirmReceive = (id: number | string) => post(`/orders/${id}/receive`)

/** 去评价 */
export const reviewOrder = (id: number | string, data: { rating: number; content?: string }) =>
  post(`/orders/${id}/review`, data)
