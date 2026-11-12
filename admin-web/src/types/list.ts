/** 通用列表管理页（ListPage 组件）对外类型 */
export type Row = Record<string, any>

export interface Column {
  key: string
  label: string
  width?: string
  muted?: boolean
  /** 返回 pill 修饰类，如 'pill--green' */
  pill?: (value: unknown, row: Row) => string
  /** 以浅绿 chip 呈现 */
  chip?: boolean
}

export interface FormField {
  key: string
  label: string
  type?: 'text' | 'number' | 'select' | 'textarea'
  options?: string[]
  full?: boolean
}

export interface RowAction {
  label: string
  kind?: 'primary' | 'outline'
  onClick: () => void
}

export interface FilterDef {
  key: string
  label?: string
  options: string[]
}
