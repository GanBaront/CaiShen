<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Chip from '@/components/Chip.vue'
import { useShoppingListStore } from '@/stores/shoppingList'
import { useRequireLogin } from '@/composables/useRequireLogin'
import * as merchantApi from '@/api/merchant'
import * as listApi from '@/api/shoppingList'
import * as orderApi from '@/api/order'

const listStore = useShoppingListStore()
const ensureLogin = useRequireLogin()

const activeTab = ref<'platform' | 'third' | 'catalog'>('platform')
const sortOptions = ['综合', '价格最低', '好评率', '距离最近'] as const
const sort = ref<(typeof sortOptions)[number]>('综合')
const otherInput = ref('')

/** 商家可购商品总清单 */
const catalog = ref<any[]>([])
const expandedCatalog = ref<string | null>(null)

async function loadCatalog() {
  if (catalog.value.length) return
  try {
    const res: any = await merchantApi.merchantProducts()
    if (Array.isArray(res)) catalog.value = res
  } catch {
    /* 降级：保留空清单 */
  }
}

function switchTab(tab: 'platform' | 'third' | 'catalog') {
  activeTab.value = tab
  if (tab === 'catalog') loadCatalog()
}

function toggleOffers(name: string) {
  expandedCatalog.value = expandedCatalog.value === name ? null : name
}

function fmtPrice(p: any) {
  const n = Number(p)
  return Number.isFinite(n) ? n.toFixed(2) : p
}

const thirdParty = [
  { name: '去美团买菜', url: 'https://i.meituan.com/' },
  { name: '去多多买菜', url: 'https://mobile.yangkeduo.com/' }
]

// 商家列表完全以接口为准，不再使用写死商家兜底（假 id 会被真实下单）
const merchants = ref<any[]>([])
const merchantErr = ref('')

// 最近订单统计与最新一条，均来自 /orders，不写死示例
const orderStats = ref<{ label: string; count: number }[]>([])
const latestOrder = ref('')

/** 页面级操作反馈（分享 / 导出） */
const pageMsg = ref<{ ok: boolean; text: string } | null>(null)

const STATUS_ORDER = ['待支付', '已支付', '配送中', '已完成']

async function loadOrders() {
  try {
    const res: any = await orderApi.myOrders()
    if (res?.statusCount) {
      orderStats.value = STATUS_ORDER.map((k) => ({
        label: k,
        count: Number(res.statusCount[k] || 0)
      }))
    } else {
      orderStats.value = []
    }
    const list = res?.list || res?.records || []
    latestOrder.value = list.length
      ? `订单 · ${list[0].merchantName || '商家'} · ${list[0].status}`
      : '暂无订单'
  } catch {
    orderStats.value = []
    latestOrder.value = '暂无订单'
  }
}

function pickSort(s: (typeof sortOptions)[number]) {
  sort.value = s
  loadMerchants()
}

/** 后端 MerchantCompareVO -> 卡片展示字段（rating/distanceKm/totalPrice 等映射为文案） */
function mapMerchant(r: any) {
  return {
    ...r,
    tag: r.tags || '',
    rating: r.rating != null ? `好评率 ${r.rating}%` : '',
    distance: r.distanceKm != null ? `${r.distanceKm}km` : '',
    duration: r.deliveryMinutes != null ? `约 ${r.deliveryMinutes} 分钟` : '',
    price: r.totalPrice != null ? `¥${fmtPrice(r.totalPrice)}` : '',
    stock: r.stockStatus || '充足',
    cover:
      r.totalCount
        ? `覆盖清单 ${r.coveredCount}/${r.totalCount} 项${
            r.fullCoverage ? '' : `（缺 ${(r.missingNames || []).join('、')}）`
          }`
        : ''
  }
}

async function loadMerchants() {
  merchantErr.value = ''
  try {
    const res: any = await merchantApi.compareMerchants(sort.value)
    merchants.value = Array.isArray(res) ? res.map(mapMerchant) : []
  } catch (e: any) {
    merchants.value = []
    merchantErr.value = e?.message || '商家比价加载失败，请稍后重试'
  }
}

function addOther() {
  const n = otherInput.value.trim()
  if (!n) return
  listStore.addItem(n)
  otherInput.value = ''
}

// ===== 查看明细：弹窗展示该商家对购物清单的报价单 =====
const detailModal = ref<{ merchant: any; raw: any } | null>(null)
const detailLoadingId = ref<number | string | null>(null)

async function onDetail(m: any) {
  detailLoadingId.value = m.id
  try {
    const res: any = await merchantApi.merchantQuote(m.id)
    detailModal.value = { merchant: mapMerchant(res), raw: res }
  } catch (e: any) {
    cartMsg.value = { ok: false, text: `获取「${m.name}」报价明细失败：${e?.message || '请稍后重试'}` }
  } finally {
    detailLoadingId.value = null
  }
}

// ===== 全部加入购物车：向该商家一键下单（清单未购项） =====
const orderingId = ref<number | string | null>(null)
const cartMsg = ref<{ ok: boolean; text: string } | null>(null)

async function onAddCart(m: any) {
  if (!ensureLogin()) return
  if (orderingId.value) return
  orderingId.value = m.id
  cartMsg.value = null
  try {
    const res: any = await orderApi.createOrder({ merchantId: m.id })
    cartMsg.value = {
      ok: true,
      text: `已向「${m.name}」下单${res?.amount != null ? `，合计 ¥${fmtPrice(res.amount)}` : ''}，订单号 ${
        res?.orderNo || ''
      }，可在订单页支付`
    }
    loadOrders()
  } catch (e: any) {
    cartMsg.value = { ok: false, text: `向「${m.name}」下单失败：${e?.message || '请稍后重试'}` }
  } finally {
    orderingId.value = null
  }
}

/** 复制文本：优先 Clipboard API，失败降级 textarea + execCommand，仍失败返回 false */
async function copyText(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    /* 继续走降级方案 */
  }
  try {
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(ta)
    return ok
  } catch {
    return false
  }
}

// 浏览器禁止自动复制时，弹窗展示分享文本供手动复制
const shareModal = ref<{ text: string } | null>(null)
const shareCopied = ref('')

async function onShare() {
  pageMsg.value = null
  shareCopied.value = ''
  try {
    const res: any = await listApi.shareShoppingList()
    const text = res?.shareText || ''
    if (!text || !text.includes('：')) throw new Error('购物清单为空，暂无可分享内容')
    const ok = await copyText(text)
    if (ok) {
      pageMsg.value = { ok: true, text: '清单已复制到剪贴板，可直接粘贴发给朋友' }
    } else {
      shareModal.value = { text }
    }
  } catch (e: any) {
    pageMsg.value = { ok: false, text: e?.message || '分享失败，请稍后重试' }
  }
}

async function copyShareText() {
  if (!shareModal.value) return
  const ok = await copyText(shareModal.value.text)
  shareCopied.value = ok ? '已复制' : '复制失败，请手动全选复制'
}

async function onExport() {
  pageMsg.value = null
  try {
    const blob: any = await listApi.exportShoppingList()
    const file = blob instanceof Blob ? blob : new Blob([blob], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(file)
    const a = document.createElement('a')
    a.href = url
    a.download = 'shopping-list.txt'
    a.rel = 'noopener'
    a.style.display = 'none'
    document.body.appendChild(a)
    a.click()
    setTimeout(() => {
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    }, 2000)
    pageMsg.value = { ok: true, text: '清单已导出为 shopping-list.txt' }
  } catch (e: any) {
    pageMsg.value = { ok: false, text: e?.message || '导出失败，请稍后重试' }
  }
}

function goThird(url: string) {
  window.open(url, '_blank')
}

onMounted(async () => {
  listStore.fetchList()
  loadMerchants()
  loadOrders()
})
</script>

<template>
  <div class="shopping sec">
    <!-- 标题 -->
    <div class="title-row">
      <div>
        <h1>购物清单</h1>
        <div class="from">
          <span class="lbl">来自菜谱：</span>
          <Chip v-for="r in listStore.sourceRecipes" :key="r" :label="r" />
          <span v-if="!listStore.sourceRecipes.length" class="lbl">暂无（可在菜谱详情页加入购物清单）</span>
        </div>
      </div>
      <div class="acts">
        <button class="btn btn-outline btn-sm" @click="onShare">分享</button>
        <button class="btn btn-outline btn-sm" @click="onExport">导出清单</button>
      </div>
    </div>

    <div v-if="pageMsg" class="msg-bar" :class="pageMsg.ok ? 'ok' : 'err'">
      {{ pageMsg.text }}
    </div>

    <div class="cols">
      <!-- 左栏：待购食材 -->
      <aside class="list-card card">
        <div class="lc-head">
          <h2>待购食材</h2>
          <span class="cnt">{{ listStore.pendingCount }} 项待购 · {{ listStore.doneCount }} 项已购</span>
        </div>
        <div v-for="g in listStore.groups" :key="g.category" class="grp">
          <div class="grp-name">{{ g.category }}</div>
          <div
            v-for="it in g.items"
            :key="it.id"
            class="item"
            @click="listStore.toggleChecked(it.id)"
          >
            <span class="cb" :class="{ on: it.checked }">{{ it.checked ? '✓' : '' }}</span>
            <div class="mid">
              <div class="nm-row">
                <span class="nm" :class="{ done: it.checked }">{{ it.name }}</span>
                <span class="am">{{ it.amount }}</span>
              </div>
              <div v-if="it.sellUnit" class="buy-line">
                购买按 {{ it.sellUnit
                }}<template v-if="it.bestPrice != null"> · ¥{{ fmtPrice(it.bestPrice) }}</template>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!listStore.items.length" class="list-empty">
          清单还是空的，去菜谱详情页点「加入购物清单」，或在下方手动添加食材
        </div>
        <div class="add-row">
          <input v-model="otherInput" placeholder="添加其他食材" @keyup.enter="addOther" />
          <button class="btn btn-outline btn-sm" @click="addOther">添加</button>
        </div>
      </aside>

      <!-- 右栏 -->
      <section class="right">
        <div class="panel card">
          <div class="tabs">
            <div :class="{ on: activeTab === 'platform' }" @click="switchTab('platform')">
              平台内商家比价
            </div>
            <div :class="{ on: activeTab === 'third' }" @click="switchTab('third')">
              第三方生鲜电商
            </div>
            <div :class="{ on: activeTab === 'catalog' }" @click="switchTab('catalog')">
              商家可购商品
            </div>
          </div>

          <template v-if="activeTab === 'platform'">
            <div class="sort-row">
              <span class="slbl">排序</span>
              <Chip
                v-for="s in sortOptions"
                :key="s"
                :variant="sort === s ? 'sel' : 'off'"
                :label="s"
                @click="pickSort(s)"
              />
            </div>

            <div v-if="cartMsg" class="msg-bar" :class="cartMsg.ok ? 'ok' : 'err'">
              {{ cartMsg.text }}
              <a v-if="cartMsg.ok" @click="$router.push('/orders')">查看订单 →</a>
            </div>

            <div v-if="merchantErr" class="msg-bar err">
              {{ merchantErr }}
              <a @click="loadMerchants()">重试 →</a>
            </div>

            <div v-for="m in merchants" :key="m.id" class="merchant">
              <div class="m-main">
                <div class="logo img-ph">店铺</div>
                <div class="info">
                  <div class="name-row">
                    <b>{{ m.name }}</b>
                    <span v-if="m.tag" class="tag-green">{{ m.tag }}</span>
                  </div>
                  <div class="sub">
                    {{ m.rating }} &nbsp; {{ m.distance }} &nbsp; {{ m.duration }}
                  </div>
                </div>
                <div class="price-col">
                  <div class="price">{{ m.price }}</div>
                  <div class="stock" :class="{ tight: m.stock === '紧张' }">
                    {{ m.stock === '紧张' ? '库存紧张' : '库存充足' }}
                  </div>
                </div>
              </div>
              <div class="m-foot">
                <span class="cover">{{ m.cover }}</span>
                <div class="m-acts">
                  <button
                    class="btn btn-outline btn-sm"
                    :disabled="detailLoadingId === m.id"
                    @click="onDetail(m)"
                  >
                    {{ detailLoadingId === m.id ? '加载中…' : '查看明细' }}
                  </button>
                  <button
                    class="btn btn-primary btn-sm"
                    :disabled="orderingId === m.id"
                    @click="onAddCart(m)"
                  >
                    {{ orderingId === m.id ? '下单中…' : '全部加入购物车' }}
                  </button>
                </div>
              </div>
            </div>
            <div v-if="!merchants.length && !merchantErr" class="cat-empty">
              暂无平台商家可参与比价（需管理端有审核通过且上架中的商家与商品）
            </div>
          </template>

          <template v-else-if="activeTab === 'catalog'">
            <div class="cat-tip">
              平台商家可购买的商品总清单，点击商品可展开各商家报价
            </div>
            <div
              v-for="c in catalog"
              :key="c.name"
              class="cat-item"
              @click="toggleOffers(c.name)"
            >
              <div class="cat-main">
                <div class="cat-info">
                  <b>{{ c.name }}</b>
                  <span class="cat-unit">{{ c.sellUnit }}</span>
                </div>
                <div class="cat-price">
                  <div class="cp">
                    ¥{{ fmtPrice(c.minPrice)
                    }}<template v-if="c.maxPrice != null && Number(c.maxPrice) !== Number(c.minPrice)"
                      > ~ ¥{{ fmtPrice(c.maxPrice) }}</template
                    >
                  </div>
                  <div class="cm">{{ c.merchantCount }} 家有售</div>
                </div>
              </div>
              <div v-if="expandedCatalog === c.name" class="offers">
                <div v-for="o in c.offers" :key="o.merchantId" class="offer">
                  <span class="o-name">{{ o.merchantName }}</span>
                  <span class="o-price">¥{{ fmtPrice(o.price) }}</span>
                  <span class="o-stock">{{
                    o.stock != null ? (o.stock > 0 ? `库存 ${o.stock}` : '无货') : ''
                  }}</span>
                </div>
              </div>
            </div>
            <div v-if="!catalog.length" class="cat-empty">
              暂无可购商品数据，请稍后重试
            </div>
          </template>
        </div>

        <!-- 没有合适的商家？ -->
        <div class="no-merchant card">
          <div>
            <h2>没有合适的商家？</h2>
            <p>带着清单跳到第三方生鲜平台购买，由对方配送上门。</p>
          </div>
          <div class="nm-acts">
            <button
              v-for="t in thirdParty"
              :key="t.name"
              class="btn btn-outline btn-sm"
              @click="goThird(t.url)"
            >
              {{ t.name }} ↗
            </button>
          </div>
        </div>

        <!-- 最近订单 -->
        <div class="recent card">
          <div class="r-head">
            <h2>最近订单</h2>
            <a @click="$router.push('/orders')">全部订单 →</a>
          </div>
          <div class="stats">
            <div v-for="(s, i) in orderStats" :key="s.label" class="stat">
              <span class="num" :class="{ light: i === orderStats.length - 1 }">{{ s.count }}</span>
              <span class="lb">{{ s.label }}</span>
            </div>
          </div>
          <div class="latest">{{ latestOrder || '暂无订单' }}</div>
        </div>
      </section>
    </div>

    <!-- 商家报价明细弹窗（该商家对购物清单内商品的可购报价） -->
    <div v-if="detailModal" class="modal-mask" @click.self="detailModal = null">
      <div class="modal card">
        <div class="md-head">
          <h3>{{ detailModal.raw.name }} · 报价明细</h3>
          <a @click="detailModal = null">关闭</a>
        </div>
        <div class="md-summary">
          覆盖清单 {{ detailModal.raw.coveredCount }}/{{ detailModal.raw.totalCount }} 项 · 合计
          ¥{{ fmtPrice(detailModal.raw.totalPrice) }}
          <template v-if="detailModal.raw.missingNames?.length">
            · 缺 {{ detailModal.raw.missingNames.join('、') }}
          </template>
        </div>
        <div class="md-table">
          <div class="md-row md-th">
            <span>商品</span><span>购买规格</span><span>单价</span><span>库存</span>
          </div>
          <div v-for="it in detailModal.raw.items" :key="it.productId" class="md-row">
            <span class="md-nm">{{ it.name }}</span>
            <span>{{ it.sellUnit || it.amount || '—' }}</span>
            <span class="md-pr">¥{{ fmtPrice(it.price) }}</span>
            <span>{{
              it.stock != null ? (it.stock > 0 ? `库存 ${it.stock}` : '无货') : '—'
            }}</span>
          </div>
          <div v-if="!detailModal.raw.items.length" class="md-empty">
            该商家暂无在售商品
          </div>
        </div>
        <div class="md-foot">
          <button class="btn btn-outline btn-sm" @click="detailModal = null">关闭</button>
          <button
            class="btn btn-primary btn-sm"
            :disabled="orderingId === detailModal.raw.id"
            @click="onAddCart(detailModal.merchant)"
          >
            {{ orderingId === detailModal.raw.id ? '下单中…' : '全部加入购物车（下单）' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 分享文本弹窗（浏览器禁止自动复制时，供手动全选复制） -->
    <div v-if="shareModal" class="modal-mask" @click.self="shareModal = null">
      <div class="modal card">
        <div class="md-head">
          <h3>分享购物清单</h3>
          <a @click="shareModal = null">关闭</a>
        </div>
        <p class="share-tip">浏览器未允许自动复制，请全选下方文本手动复制</p>
        <textarea
          class="share-text"
          readonly
          :value="shareModal.text"
          @focus="($event.target as HTMLTextAreaElement).select()"
        ></textarea>
        <div class="md-foot">
          <span class="share-copied">{{ shareCopied }}</span>
          <button class="btn btn-outline btn-sm" @click="shareModal = null">关闭</button>
          <button class="btn btn-primary btn-sm" @click="copyShareText">再次复制</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.shopping {
  padding-top: 26px;
  padding-bottom: 40px;
}
.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 22px;
}
.title-row h1 {
  font-size: 34px;
  font-weight: 700;
  margin-bottom: 10px;
}
.from {
  display: flex;
  align-items: center;
  gap: 8px;
}
.from .lbl {
  font-size: 13px;
  color: var(--text2);
}
.title-row .acts {
  display: flex;
  gap: 12px;
  margin-top: 6px;
}
.cols {
  display: grid;
  grid-template-columns: 1fr 1.33fr;
  gap: 28px;
  align-items: start;
}
.list-card {
  padding: 20px 22px;
}
.lc-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
}
.lc-head h2 {
  font-size: 18px;
  font-weight: 700;
}
.lc-head .cnt {
  color: var(--text3);
  font-size: 12px;
}
.grp {
  margin-bottom: 10px;
}
.grp-name {
  font-size: 12px;
  color: var(--text2);
  padding: 8px 0 4px;
}
.item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  font-size: 14px;
  cursor: pointer;
  border-bottom: 1px solid #f4f6f3;
}
.cb {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid var(--border);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #fff;
  flex: none;
}
.cb.on {
  background: var(--primary);
  border-color: var(--primary);
}
.item .mid {
  flex: 1;
  min-width: 0;
}
.nm-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}
.item .nm {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  /* 食材名称整词显示，不在字间折行（避免“番茄”被拆成两行） */
  white-space: nowrap;
}
.item .nm.done {
  color: var(--text3);
}
.item .am {
  color: var(--text2);
  font-size: 13px;
  flex: none;
}
.item .buy-line {
  margin-top: 3px;
  font-size: 12px;
  color: var(--primary);
  /* 只在空格处换行，不在中文词语中间拆字 */
  word-break: keep-all;
}
.list-empty {
  padding: 30px 12px;
  text-align: center;
  color: var(--text2);
  font-size: 12px;
  line-height: 1.7;
}
.add-row {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}
.add-row input {
  flex: 1;
  height: 38px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0 12px;
  font-size: 13px;
  outline: none;
  color: var(--text);
}
.right {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.panel {
  padding: 0 22px 22px;
}
.tabs {
  display: flex;
  gap: 28px;
  border-bottom: 1px solid var(--border);
  margin: 0 -22px 18px;
  padding: 0 22px;
}
.tabs div {
  padding: 16px 0;
  font-size: 14px;
  color: var(--text2);
  position: relative;
  cursor: pointer;
}
.tabs div.on {
  color: var(--text);
  font-weight: 700;
}
.tabs div.on:after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: var(--primary);
  border-radius: 2px;
}
.sort-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.sort-row .slbl {
  font-size: 13px;
  color: var(--text2);
  margin-right: 4px;
}
.sort-row .note {
  margin-left: auto;
  color: var(--text3);
  font-size: 12px;
}
.merchant {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 14px;
}
.merchant:last-child {
  margin-bottom: 0;
}
.m-main {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.logo {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  font-size: 12px;
  flex: none;
}
.info {
  flex: 1;
}
.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
}
.sub {
  color: var(--text3);
  font-size: 12px;
  margin-top: 6px;
}
.price-col {
  text-align: right;
}
.price {
  font-size: 22px;
  font-weight: 700;
}
.stock {
  font-size: 12px;
  color: var(--success);
  margin-top: 4px;
}
.stock.tight {
  color: var(--danger);
}
.m-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f0f3ee;
}
.cover {
  color: var(--text2);
  font-size: 13px;
}
.m-acts {
  display: flex;
  gap: 10px;
}
.no-merchant {
  padding: 20px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}
.no-merchant h2 {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 6px;
}
.no-merchant p {
  color: var(--text2);
  font-size: 13px;
  margin: 0;
  max-width: 360px;
}
.nm-acts {
  display: flex;
  gap: 12px;
  flex: none;
}
.recent {
  padding: 20px 22px;
}
.r-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.r-head h2 {
  font-size: 18px;
  font-weight: 700;
}
.r-head a {
  color: var(--text2);
  font-size: 13px;
  cursor: pointer;
}
.stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 16px;
}
.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.stat .num {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
}
.stat .num.light {
  background: #b9cdbf;
}
.stat .lb {
  color: var(--text2);
  font-size: 13px;
}
.latest {
  text-align: center;
  color: var(--text2);
  font-size: 13px;
  border-top: 1px solid #f4f6f3;
  padding-top: 14px;
}
/* 商家可购商品总清单 */
.cat-tip {
  color: var(--text3);
  font-size: 12px;
  margin-bottom: 14px;
}
.cat-item {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 12px;
  cursor: pointer;
}
.cat-item:last-child {
  margin-bottom: 0;
}
.cat-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.cat-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}
.cat-unit {
  color: var(--primary);
  font-size: 12px;
  background: rgba(47, 125, 68, 0.08);
  border-radius: 6px;
  padding: 2px 8px;
}
.cat-price {
  text-align: right;
}
.cat-price .cp {
  font-size: 18px;
  font-weight: 700;
}
.cat-price .cm {
  color: var(--text3);
  font-size: 12px;
  margin-top: 2px;
}
.offers {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e4e9e4;
}
.offer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
  font-size: 13px;
}
.offer .o-name {
  flex: 1;
  color: var(--text2);
}
.offer .o-price {
  font-weight: 600;
}
.offer .o-stock {
  color: var(--text3);
  font-size: 12px;
}
.cat-empty {
  text-align: center;
  color: var(--text3);
  font-size: 13px;
  padding: 28px 0;
}
/* 下单反馈提示条 */
.msg-bar {
  margin: 12px 0;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.6;
}
.msg-bar.ok {
  background: #edf7ef;
  color: #2f7a4d;
}
.msg-bar.err {
  background: #fdeeee;
  color: #c2452f;
}
.msg-bar a {
  color: var(--primary);
  cursor: pointer;
  margin-left: 6px;
}
/* 商家报价明细弹窗 */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(20, 30, 24, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 60;
}
.modal {
  width: 560px;
  max-width: calc(100vw - 48px);
  max-height: 82vh;
  overflow: auto;
  padding: 22px 24px;
}
.md-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.md-head h3 {
  font-size: 18px;
  font-weight: 700;
}
.md-head a {
  color: var(--text3);
  font-size: 13px;
  cursor: pointer;
}
.md-summary {
  background: #f4f8f4;
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 13px;
  color: var(--text2);
  margin-bottom: 12px;
}
.md-table {
  border: 1px solid #eef2ec;
  border-radius: 10px;
  overflow: hidden;
}
.md-row {
  display: grid;
  grid-template-columns: 1.4fr 1.2fr 0.8fr 0.8fr;
  gap: 8px;
  padding: 10px 14px;
  font-size: 13px;
  border-bottom: 1px solid #f2f5f0;
}
.md-row:last-child {
  border-bottom: none;
}
.md-row.md-th {
  background: #f8faf7;
  color: var(--text3);
  font-size: 12px;
}
.md-row .md-nm {
  font-weight: 600;
}
.md-row .md-pr {
  color: #c2452f;
  font-weight: 600;
}
.md-empty {
  text-align: center;
  color: var(--text3);
  font-size: 13px;
  padding: 26px 0;
}
.md-foot {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
.share-tip {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--text2);
}
.share-text {
  width: 100%;
  height: 150px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  font-family: inherit;
  color: var(--text);
  outline: none;
  resize: vertical;
  white-space: pre-wrap;
}
.share-copied {
  margin-right: auto;
  font-size: 12px;
  color: var(--text2);
  align-self: center;
}
</style>
