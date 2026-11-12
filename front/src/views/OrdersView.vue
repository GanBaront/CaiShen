<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useRequireLogin } from '@/composables/useRequireLogin'
import * as orderApi from '@/api/order'
import * as memberApi from '@/api/member'

const router = useRouter()
const user = useUserStore()
const ensureLogin = useRequireLogin()

const orders = ref<any[]>([])
const loading = ref(false)

// 查看进度弹窗
const progressOrder = ref<any>(null)
const progressDetail = ref<any>(null)
const progressLoading = ref(false)

const progressPercent = computed(() => {
  const s = String(progressDetail.value?.status || progressOrder.value?.status || '')
  if (s.includes('完成')) return 100
  if (s.includes('配送') || s.includes('运输')) return 60
  if (s.includes('备货') || s.includes('发货')) return 35
  if (s.includes('支付')) return 10
  return 30
})

// 去评价弹窗
const reviewOrder = ref<any>(null)
const reviewRating = ref(5)
const reviewContent = ref('')
const reviewSubmitting = ref(false)
const reviewMsg = ref('')

// 去支付弹窗（模拟支付）
const payOrder = ref<any>(null)
const paying = ref(false)
const payMsg = ref('')

// 页面级操作反馈（支付 / 确认收货）
const actionMsg = ref<{ ok: boolean; text: string } | null>(null)
const receivingId = ref<number | string | null>(null)

async function receive(o: any) {
  if (receivingId.value) return
  receivingId.value = o.id
  actionMsg.value = null
  try {
    await orderApi.confirmReceive(o.id)
    actionMsg.value = { ok: true, text: `「${o.merchant}」订单已确认收货，现在可以对订单评价了` }
    await loadOrders()
  } catch (e: any) {
    actionMsg.value = { ok: false, text: e?.message || '确认收货失败，请稍后重试' }
  } finally {
    receivingId.value = null
  }
}

function openPay(o: any) {
  if (!ensureLogin()) return
  payOrder.value = o
  payMsg.value = ''
}

async function confirmPay() {
  if (!payOrder.value) return
  payMsg.value = ''
  paying.value = true
  try {
    await memberApi.mockPay({ orderId: payOrder.value.id })
    actionMsg.value = { ok: true, text: '支付成功，订单已进入配送流程，送达后可确认收货' }
    payOrder.value = null
    await loadOrders()
  } catch (e: any) {
    payMsg.value = e?.message || '支付失败，请稍后重试'
  } finally {
    paying.value = false
  }
}

async function loadOrders() {
  loading.value = true
  try {
    const res: any = await orderApi.myOrders()
    // 后端返回 OrderListVO { list, statusCount, total }，兼容历史 records 结构
    const list = res?.list || res?.records || res
    if (Array.isArray(list) && list.length) {
      orders.value = list.map((o: any, i: number) => ({
        id: o.id ?? i,
        merchant: o.merchantName || o.merchant,
        amount: o.amountText || (typeof o.amount === 'number' ? `¥${o.amount}` : o.amount),
        status: o.status,
        payable: o.status === '待支付',
        canReceive: o.status === '已支付' || o.status === '配送中',
        reviewed: !!o.reviewed,
        op: o.reviewed ? '已评价' : '去评价'
      }))
    } else {
      orders.value = []
    }
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
}

async function viewProgress(o: any) {
  if (!ensureLogin()) return
  progressOrder.value = o
  progressDetail.value = null
  progressLoading.value = true
  try {
    progressDetail.value = await orderApi.orderDetail(o.id)
  } catch {
    progressDetail.value = null
  } finally {
    progressLoading.value = false
  }
}

function openReview(o: any) {
  if (!ensureLogin()) return
  reviewOrder.value = o
  reviewRating.value = 5
  reviewContent.value = ''
  reviewMsg.value = ''
}

async function submitReview() {
  if (!reviewOrder.value) return
  reviewMsg.value = ''
  reviewSubmitting.value = true
  try {
    await orderApi.reviewOrder(reviewOrder.value.id, {
      rating: reviewRating.value,
      content: reviewContent.value.trim() || undefined
    })
    reviewOrder.value = null
    await loadOrders()
  } catch (e: any) {
    reviewMsg.value = e?.message || '提交失败，请稍后重试'
  } finally {
    reviewSubmitting.value = false
  }
}

onMounted(async () => {
  await user.fetchMe()
  loadOrders()
})
</script>

<template>
  <div class="orders-page sec">
    <div class="head">
      <h1>我的订单</h1>
      <p class="sub">买菜订单的进度跟踪与评价</p>
      <p v-if="actionMsg" class="action-msg" :class="{ err: !actionMsg.ok }">{{ actionMsg.text }}</p>
    </div>

    <div class="blk table">
      <table>
        <thead>
          <tr>
            <th>商家</th>
            <th>金额</th>
            <th>状态</th>
            <th style="text-align: right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in orders" :key="o.id">
            <td>{{ o.merchant }}</td>
            <td>{{ o.amount }}</td>
            <td>
              <span :class="o.status === '已完成' ? 'tag-green' : 'tag-amber'">{{ o.status }}</span>
            </td>
            <td class="op">
              <template v-if="o.payable">
                <button class="btn btn-sm btn-primary" @click="openPay(o)">去支付</button>
                <button class="btn btn-sm btn-outline" @click="viewProgress(o)">查看进度</button>
              </template>
              <template v-else-if="o.canReceive">
                <button class="btn btn-sm btn-outline" @click="viewProgress(o)">查看进度</button>
                <button
                  class="btn btn-sm btn-primary"
                  :disabled="receivingId === o.id"
                  @click="receive(o)"
                >
                  {{ receivingId === o.id ? '处理中…' : '确认收货' }}
                </button>
              </template>
              <button
                v-else
                class="btn btn-sm"
                :class="o.reviewed ? 'btn-outline' : 'btn-primary'"
                :disabled="o.reviewed"
                @click="openReview(o)"
              >
                {{ o.op }}
              </button>
            </td>
          </tr>
          <tr v-if="!orders.length && !loading">
            <td colspan="4" class="empty-line">暂无订单，去<a @click="router.push('/recipes')">菜谱推荐</a>挑一道菜，缺的食材一键下单吧</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 查看进度弹窗 -->
    <div v-if="progressOrder" class="modal-mask" @click.self="progressOrder = null">
      <div class="modal">
        <div class="modal-head">
          <h3>订单进度</h3>
          <span class="modal-close" @click="progressOrder = null">✕</span>
        </div>
        <div v-if="progressLoading" class="modal-body">加载中…</div>
        <div v-else-if="progressDetail" class="modal-body">
          <div class="kv"><span>订单号</span><b>{{ progressDetail.orderNo || progressOrder.id }}</b></div>
          <div class="kv"><span>商家</span><b>{{ progressDetail.merchantName || progressOrder.merchant }}</b></div>
          <div class="kv"><span>金额</span><b>{{ progressOrder.amount }}</b></div>
          <div class="kv"><span>下单时间</span><b>{{ (progressDetail.createTime || '').replace('T', ' ').slice(0, 19) || '-' }}</b></div>
          <div class="kv"><span>当前状态</span><b>{{ progressDetail.status || progressOrder.status }}</b></div>
          <div class="progress-track">
            <div class="progress-bar" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <div class="progress-desc">{{ progressDetail.deliveryDesc || '订单正在处理中，请耐心等待' }}</div>
        </div>
        <div v-else class="modal-body">暂时获取不到订单详情，请稍后重试</div>
      </div>
    </div>

    <!-- 去支付弹窗（模拟支付） -->
    <div v-if="payOrder" class="modal-mask" @click.self="payOrder = null">
      <div class="modal">
        <div class="modal-head">
          <h3>订单支付 · {{ payOrder.merchant }}</h3>
          <span class="modal-close" @click="payOrder = null">✕</span>
        </div>
        <div class="modal-body">
          <div class="kv"><span>订单号</span><b>{{ payOrder.id }}</b></div>
          <div class="kv"><span>商家</span><b>{{ payOrder.merchant }}</b></div>
          <div class="kv"><span>金额</span><b>{{ payOrder.amount }}</b></div>
          <p class="pay-tip">模拟支付：不接入真实商户与扣款，确认后订单将转为「已支付」并开始备货配送</p>
          <p v-if="payMsg" class="review-msg">{{ payMsg }}</p>
          <div class="modal-acts">
            <button class="btn btn-ghost" @click="payOrder = null">取消</button>
            <button class="btn btn-primary" :disabled="paying" @click="confirmPay">
              {{ paying ? '支付中…' : '确认支付' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 去评价弹窗 -->
    <div v-if="reviewOrder" class="modal-mask" @click.self="reviewOrder = null">
      <div class="modal">
        <div class="modal-head">
          <h3>评价订单 · {{ reviewOrder.merchant }}</h3>
          <span class="modal-close" @click="reviewOrder = null">✕</span>
        </div>
        <div class="modal-body">
          <div class="stars">
            <span
              v-for="s in 5"
              :key="s"
              class="star"
              :class="{ on: s <= reviewRating }"
              @click="reviewRating = s"
              >★</span
            >
          </div>
          <textarea
            v-model="reviewContent"
            class="review-text"
            rows="3"
            maxlength="200"
            placeholder="说说这次买菜体验如何？（选填）"
          ></textarea>
          <p v-if="reviewMsg" class="review-msg">{{ reviewMsg }}</p>
          <div class="modal-acts">
            <button class="btn btn-ghost" @click="reviewOrder = null">取消</button>
            <button class="btn btn-primary" :disabled="reviewSubmitting" @click="submitReview">
              {{ reviewSubmitting ? '提交中…' : '提交评价' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.orders-page {
  padding-top: 26px;
  padding-bottom: 40px;
}
.head h1 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 6px;
}
.head .sub {
  color: var(--text2);
  font-size: 13px;
  margin: 0 0 22px;
}
.action-msg {
  margin: -10px 0 16px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #eef7f0;
  color: #2f7d4f;
  font-size: 12px;
}
.action-msg.err {
  background: #fdeeee;
  color: var(--danger, #c0392b);
}
table th {
  padding: 8px 0;
}
table td {
  padding: 13px 0;
}
td.op {
  text-align: right;
}
td.op .btn + .btn {
  margin-left: 8px;
}
.pay-tip {
  color: var(--text2);
  font-size: 12px;
  margin: 12px 0 0;
  line-height: 1.6;
}
.empty-line {
  text-align: center;
  color: var(--text2);
  font-size: 13px;
}
.empty-line a {
  color: var(--primary);
  cursor: pointer;
}
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.modal {
  width: 420px;
  max-width: calc(100vw - 48px);
  background: #fff;
  border-radius: 14px;
  padding: 20px 22px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.modal-head h3 {
  font-size: 16px;
  margin: 0;
}
.modal-close {
  cursor: pointer;
  color: var(--text3);
  font-size: 14px;
}
.modal-body {
  font-size: 13px;
  color: var(--text);
}
.kv {
  display: flex;
  justify-content: space-between;
  padding: 7px 0;
}
.kv span {
  color: var(--text2);
}
.progress-track {
  height: 8px;
  background: var(--chip-bg, #eef2ee);
  border-radius: 999px;
  overflow: hidden;
  margin: 14px 0 8px;
}
.progress-bar {
  height: 100%;
  background: var(--primary);
  border-radius: 999px;
  transition: width 0.3s;
}
.progress-desc {
  color: var(--text2);
  font-size: 12px;
}
.stars {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
}
.star {
  font-size: 24px;
  color: #d8ddd9;
  cursor: pointer;
  line-height: 1;
}
.star.on {
  color: #f5a623;
}
.review-text {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--text);
  outline: none;
  resize: vertical;
  font-family: inherit;
}
.review-msg {
  color: var(--text2);
  font-size: 12px;
  margin: 10px 0 0;
}
.modal-acts {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}
</style>
