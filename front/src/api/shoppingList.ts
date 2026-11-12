import { get, post, put, del } from './request'

export const getShoppingList = () => get('/shopping-list')
export const addShoppingItem = (data: {
  name: string
  amount?: string
  unit?: string
  category?: string
}) => post('/shopping-list/items', data)
export const updateShoppingItem = (id: number | string, data: { checked?: boolean; amount?: string }) =>
  put(`/shopping-list/items/${id}`, data)
export const removeShoppingItem = (id: number | string) => del(`/shopping-list/items/${id}`)
export const shareShoppingList = () => post('/shopping-list/share')
/** 导出清单：后端返回 txt 附件流，前端按 Blob 接收并触发浏览器下载 */
export const exportShoppingList = () =>
  get<Blob>('/shopping-list/export', undefined, { responseType: 'blob' })
