import { get } from './request'

export type MerchantSort = '综合' | '价格最低' | '好评率' | '距离最近'

/** 平台内商家比价 */
export const compareMerchants = (sort: MerchantSort) => get('/merchants/compare', { sort })

/** 查看某个商家的报价明细 */
export const merchantQuote = (id: number | string) => get(`/merchants/${id}/quote`)

/** 商家可购商品总清单（平台商家一共可购买的东西） */
export const merchantProducts = () => get('/merchants/products')
