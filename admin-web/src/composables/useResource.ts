import { ref, type Ref } from 'vue'

const OFFLINE_NOTICE = '接口未连接，当前展示示例数据'

/**
 * 列表数据加载：接口可用时用接口数据，接口不可用时退回示例数据（不白屏）
 */
export function useResource<T extends object>(loader: () => Promise<T[]>, seed: T[]) {
  const rows = ref<T[]>([]) as Ref<T[]>
  const loading = ref(false)
  const notice = ref('')

  async function load() {
    loading.value = true
    try {
      rows.value = await loader()
      notice.value = ''
    } catch {
      rows.value = seed.map((it) => ({ ...it })) as T[]
      notice.value = OFFLINE_NOTICE
    } finally {
      loading.value = false
    }
  }

  return { rows, loading, notice, load, OFFLINE_NOTICE }
}
