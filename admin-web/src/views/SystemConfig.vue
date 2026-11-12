<template>
  <div class="panel">
    <div class="chart-head">
      <h3 class="panel-title">系统配置</h3>
      <div class="head-acts">
        <button class="btn btn-sm" :disabled="testing" @click="onTest">
          {{ testing ? '检测中…' : '测试 AI 连接' }}
        </button>
        <button class="btn btn-primary btn-sm" :disabled="saving" @click="onSave">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </div>

    <div v-if="notice" class="notice">{{ notice }}</div>

    <div v-for="section in sections" :key="section.title" style="margin-bottom: 22px">
      <div class="nav-group-title" style="color: var(--text2); padding: 0 0 10px">{{ section.title }}</div>
      <div class="form-grid">
        <div v-for="f in section.fields" :key="f.key" class="field" :class="{ full: f.full }">
          <label>{{ f.label }}</label>
          <input
            v-if="f.secret"
            v-model="values[f.key]"
            type="password"
            :placeholder="f.placeholder || ''"
            autocomplete="new-password"
          />
          <input v-else v-model="values[f.key]" type="text" :placeholder="f.placeholder || ''" />
          <div v-if="f.hint" style="color: var(--text3); font-size: 12px; margin-top: 6px">{{ f.hint }}</div>
        </div>
      </div>
    </div>

    <!-- AI 连通性测试结果 -->
    <div v-if="probes.length" class="probe">
      <div class="nav-group-title" style="color: var(--text2); padding: 0 0 10px">AI 连接状态</div>
      <div class="probe-grid">
        <div v-for="p in probes" :key="p.label" class="probe-card" :class="{ ok: p.ok, bad: !p.ok }">
          <div class="probe-head">
            <span class="dot" />
            <b>{{ p.label }}</b>
            <span class="probe-model">{{ p.model || '—' }}</span>
          </div>
          <div class="probe-line">{{ p.ok ? `连接正常 · ${p.latencyMs}ms` : p.message || '未通过' }}</div>
          <div v-if="p.ok && p.reply" class="probe-sub">模型回复：{{ p.reply }}</div>
          <div v-if="!p.ok && p.baseUrl" class="probe-sub">{{ p.baseUrl }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import * as seed from '@/mock/seed'
import { fetchConfig, saveConfig, testAi, type AiProbeResult } from '@/api/config'
import type { SysConfig } from '@/api/types'

interface ConfigField {
  key: string
  label: string
  full?: boolean
  secret?: boolean
  placeholder?: string
  hint?: string
}

const OFFLINE_NOTICE = '接口未连接，已按本地示例数据编辑（保存仅在后台服务可用时生效）'

const sections: { title: string; fields: ConfigField[] }[] = [
  {
    title: '会员与次数',
    fields: [
      { key: 'free.daily.limit', label: '每日免费生成次数', placeholder: '5' },
      { key: 'member.month.price', label: '会员月价（元）', placeholder: '19' },
      { key: 'ai.single.price', label: '单次 AI 菜谱费用（元）', placeholder: '2.99' },
    ],
  },
  {
    title: 'AI 服务 · 文本模型（菜谱推荐 / AI 创作）',
    fields: [
      { key: 'ai.provider', label: '服务商', placeholder: 'openai-compatible' },
      { key: 'ai.model', label: '模型名称', placeholder: 'deepseek-chat' },
      {
        key: 'ai.base-url',
        label: '接口地址',
        full: true,
        placeholder: 'https://api.deepseek.com/v1',
        hint: 'OpenAI 兼容地址，末尾无需 /chat/completions',
      },
      {
        key: 'ai.api-key',
        label: 'API Key',
        full: true,
        secret: true,
        hint: '环境变量 AI_API_KEY 优先于此处；此处留空即可，密钥不建议落库',
      },
    ],
  },
  {
    title: 'AI 服务 · 视觉模型（菜品 / 食材图片识别）',
    fields: [
      { key: 'ai.vision-model', label: '模型名称', placeholder: 'glm-4v-flash' },
      {
        key: 'ai.vision-image-mode',
        label: '图片入参格式',
        placeholder: 'auto',
        hint: 'auto 自动按服务商判断；智谱需裸 base64，OpenAI 系用 data-uri',
      },
      {
        key: 'ai.vision-base-url',
        label: '接口地址',
        full: true,
        placeholder: 'https://open.bigmodel.cn/api/paas/v4',
        hint: '留空则复用文本模型的接口地址（同一服务商只需配一套）',
      },
      {
        key: 'ai.vision-api-key',
        label: 'API Key',
        full: true,
        secret: true,
        hint: '环境变量 AI_VISION_API_KEY 优先；留空则复用文本模型的 Key（DeepSeek 无视觉能力，识别需单独配）',
      },
    ],
  },
  {
    title: '第三方生鲜',
    fields: [
      { key: 'third.meituan.url', label: '第三方生鲜（美团买菜）跳转地址', full: true },
      { key: 'third.duoduo.url', label: '第三方生鲜（多多买菜）跳转地址', full: true },
    ],
  },
]

const values = reactive<SysConfig>({ ...seed.config })
const saving = ref(false)
const testing = ref(false)
const notice = ref('')

interface ProbeView extends AiProbeResult {
  label: string
}
const probes = ref<ProbeView[]>([])

async function load() {
  try {
    const remote = await fetchConfig()
    for (const key of Object.keys(values)) {
      if (remote[key] !== undefined) values[key] = remote[key]
    }
    notice.value = ''
  } catch {
    notice.value = OFFLINE_NOTICE
  }
}

async function onSave() {
  saving.value = true
  try {
    await saveConfig({ ...values })
    notice.value = '配置已保存'
  } catch {
    notice.value = OFFLINE_NOTICE
  } finally {
    saving.value = false
  }
}

/**
 * 测试 AI 连接：建议先「保存」再测试，因为后端读取的是已落库的配置。
 * 文本与视觉分别探测，便于定位是哪个服务商的 Key / 地址有问题。
 */
async function onTest() {
  testing.value = true
  notice.value = ''
  try {
    const res = await testAi()
    probes.value = [
      { label: '文本模型', ...res.text },
      { label: '视觉模型', ...res.vision }
    ]
  } catch (e: any) {
    probes.value = []
    notice.value = e?.message || 'AI 连接测试失败，请确认后台服务已启动'
  } finally {
    testing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head-acts {
  display: flex;
  align-items: center;
  gap: 10px;
}
.probe {
  margin-top: 8px;
}
.probe-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
}
.probe-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
}
.probe-card.ok {
  border-color: rgba(62, 154, 107, 0.45);
  background: rgba(62, 154, 107, 0.06);
}
.probe-card.bad {
  border-color: rgba(217, 80, 63, 0.4);
  background: var(--danger-bg);
}
.probe-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text);
}
.probe-head .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text3);
  flex: none;
}
.probe-card.ok .dot {
  background: var(--success);
}
.probe-card.bad .dot {
  background: var(--danger);
}
.probe-model {
  margin-left: auto;
  font-size: 12px;
  color: var(--text2);
}
.probe-line {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text2);
  line-height: 1.6;
  word-break: break-all;
}
.probe-card.bad .probe-line {
  color: var(--danger);
}
.probe-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text3);
  word-break: break-all;
}
</style>
