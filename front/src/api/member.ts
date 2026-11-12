import { get, post } from './request'

export const memberStatus = () => get('/member/status')
export const memberPlans = () => get('/member/plans')

/** 模拟支付（不接真实商户）：购买记录、商城订单，或直接按类型支付（支付成功才生成记录） */
export const mockPay = (data: {
  billingRecordId?: number | string
  orderId?: number | string
  type?: string
  amount?: number
}) => post('/pay/mock', data)

export const billingRecords = () => get('/billing/records')
