import http from './http'
import type { OverviewStat, PendingItem, TrendPoint } from './types'

/** 后端统计卡结构 { value, deltaPercent, trend } */
interface StatCard {
  value: number
  deltaPercent: number
  trend: string
}

/** 指标卡：前端展示定义（label/格式）+ 后端字段名 */
const STAT_DEFS: { key: string; label: string; card: string; money?: boolean }[] = [
  { key: 'activeUsers', label: '用户总数', card: 'activeUsers' },
  { key: 'aiCount', label: 'AI 菜谱生成次数', card: 'aiGenerations' },
  { key: 'members', label: '会员订阅', card: 'memberSubscriptions' },
  { key: 'orderAmount', label: '订单金额', card: 'orderAmount', money: true },
]

/** 待处理审核：后端字段 -> 展示项 */
const PENDING_DEFS: { key: string; card: string; label: string; actionText: string; to: string }[] = [
  { key: 'merchants', card: 'merchantAudit', label: '商家入驻申请', actionText: '处理', to: '/admin/merchants' },
  { key: 'products', card: 'productAudit', label: '商品上架审核', actionText: '处理', to: '/admin/products' },
  { key: 'comments', card: 'commentAudit', label: '评论内容审核', actionText: '处理', to: '/admin/comments' },
  { key: 'feedback', card: 'feedback', label: '用户反馈', actionText: '查看', to: '/admin/feedback' },
]

function formatValue(value: number, money?: boolean) {
  if (money) {
    return `¥${value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
  }
  return value.toLocaleString('zh-CN')
}

export async function fetchOverview(range: '7' | '30'): Promise<OverviewStat[]> {
  const res = await http.get('/admin/dashboard/overview', { params: { range } })
  const data = (res.data ?? {}) as Record<string, StatCard>
  return STAT_DEFS.map(({ key, label, card, money }) => {
    const c = data[card]
    if (!c) return { key, label, value: '—', deltaText: '', deltaUp: true }
    const up = c.trend !== 'down'
    const pct = Math.abs(Number(c.deltaPercent) || 0)
    return {
      key,
      label,
      value: formatValue(Number(c.value), money),
      deltaText: `较上期 ${up ? '↑' : '↓'} ${pct}%`,
      deltaUp: up,
    }
  })
}

export async function fetchAiTrend(range: '7' | '30'): Promise<TrendPoint[]> {
  const res = await http.get('/admin/dashboard/ai-trend', { params: { range } })
  const data = (res.data ?? {}) as { labels?: unknown[]; values?: unknown[] }
  const labels = data.labels ?? []
  const values = data.values ?? []
  return labels.map((label, i) => ({
    label: String(label ?? ''),
    value: Number(values[i] ?? 0) || 0,
  }))
}

export async function fetchPending(): Promise<PendingItem[]> {
  const res = await http.get('/admin/dashboard/pending')
  const data = (res.data ?? {}) as Record<string, unknown>
  return PENDING_DEFS.map(({ key, card, label, actionText, to }) => ({
    key,
    label,
    actionText,
    to,
    count: Number(data[card] ?? 0) || 0,
  }))
}
