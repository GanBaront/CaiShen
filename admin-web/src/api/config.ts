import http from './http'
import type { SysConfig } from './types'

export async function fetchConfig(): Promise<SysConfig> {
  const res = await http.get('/admin/config')
  return normalize(res.data)
}

export async function saveConfig(config: SysConfig): Promise<void> {
  await http.put('/admin/config', config)
}

/** 单模型连通性测试结果 */
export interface AiProbeResult {
  configured: boolean
  ok: boolean
  model?: string
  baseUrl?: string
  imageMode?: string
  latencyMs?: number
  reply?: string
  message?: string
}

/** AI 连通性测试：分别探测文本模型与视觉模型（模型响应较慢，超时放宽到 90s） */
export async function testAi(): Promise<{ text: AiProbeResult; vision: AiProbeResult }> {
  const res = await http.post('/admin/config/ai-test', null, { timeout: 90000 })
  return res.data as { text: AiProbeResult; vision: AiProbeResult }
}

/** 兼容 { key: value } 与 [{ configKey, configValue }] 两种返回 */
function normalize(data: unknown): SysConfig {
  const out: SysConfig = {}
  if (Array.isArray(data)) {
    for (const item of data) {
      const it = item as { configKey?: string; config_key?: string; configValue?: string; config_value?: string }
      const key = it.configKey || it.config_key
      if (key) out[key] = it.configValue ?? it.config_value ?? ''
    }
    return out
  }
  if (data && typeof data === 'object') {
    for (const [k, v] of Object.entries(data as Record<string, unknown>)) {
      out[k] = v == null ? '' : String(v)
    }
  }
  return out
}
