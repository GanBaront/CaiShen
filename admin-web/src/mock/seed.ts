/**
 * 后端未启动时的降级示例数据（取自 sql/一键建库建表sql.txt 与 08 后台设计图）
 * 仅用于「优雅降级」，真实数据一律以 /api/admin/** 接口为准。
 */
import type {
  AdminUser,
  AllergenTag,
  CommentItem,
  Feedback,
  Ingredient,
  Member,
  Merchant,
  MerchantProduct,
  Nutrition,
  Order,
  OverviewStat,
  PendingItem,
  Recipe,
  SysConfig,
  TrendPoint,
} from '@/api/types'

export const overview: OverviewStat[] = [
  { key: 'activeUsers', label: '活跃用户', value: '1,284', deltaText: '较上周 ↑ 12% 比上周上升 12%', deltaUp: true },
  { key: 'aiCount', label: 'AI 菜谱生成次数', value: '5,730', deltaText: '较上周 ↑ 8% 比上周上升 8%', deltaUp: true },
  { key: 'members', label: '会员订阅', value: '96', deltaText: '较上周 ↓ 3% 比上周下降 3%', deltaUp: false },
  { key: 'orderAmount', label: '订单金额', value: '¥8,420', deltaText: '较上周 ↑ 5% 比上周上升 5%', deltaUp: true },
]

export const aiTrend: TrendPoint[] = [
  { label: '周一', value: 42 },
  { label: '周二', value: 56 },
  { label: '周三', value: 47 },
  { label: '周四', value: 71 },
  { label: '周五', value: 63 },
  { label: '周六', value: 79 },
  { label: '周日', value: 75 },
]

export const pending: PendingItem[] = [
  { key: 'merchants', label: '商家入驻申请', count: 3, actionText: '处理', to: '/admin/merchants' },
  { key: 'products', label: '商品上架审核', count: 12, actionText: '处理', to: '/admin/products' },
  { key: 'comments', label: '评论内容审核', count: 8, actionText: '处理', to: '/admin/comments' },
  { key: 'feedback', label: '用户反馈', count: 5, actionText: '查看', to: '/admin/feedback' },
]

export const recipes: Recipe[] = [
  { id: 1, name: '番茄炒蛋', cuisine: '家常菜', difficulty: '简单', source: '人工', status: '已上线', cookTime: 15, taste: '清淡' },
  { id: 2, name: '青椒土豆丝', cuisine: '家常菜', difficulty: '简单', source: '人工', status: '已上线', cookTime: 20, taste: '清淡' },
  { id: 13, name: '茄汁土豆蛋卷', cuisine: '创意菜', difficulty: '中等', source: 'AI生成', status: '待审核', cookTime: 35, taste: '清淡微酸' },
  { id: 14, name: '低卡鸡胸沙拉', cuisine: '轻食', difficulty: '简单', source: '人工', status: '已下架', cookTime: 20, taste: '清淡' },
  { id: 3, name: '番茄蛋花汤', cuisine: '家常菜', difficulty: '简单', source: '人工', status: '已上线', cookTime: 15, taste: '清淡' },
  { id: 10, name: '番茄牛腩', cuisine: '家常菜', difficulty: '较难', source: '人工', status: '已上线', cookTime: 60, taste: '酸甜' },
]

export const ingredients: Ingredient[] = [
  { id: 1, name: '番茄', alias: '西红柿', category: '蔬菜', defaultUnit: '个', status: 1 },
  { id: 2, name: '鸡蛋', alias: '蛋', category: '蛋类', defaultUnit: '个', status: 1 },
  { id: 8, name: '土豆', alias: '马铃薯', category: '蔬菜', defaultUnit: '个', status: 1 },
  { id: 16, name: '面粉', alias: '小麦粉', category: '主食', defaultUnit: '克', status: 1 },
  { id: 23, name: '鸡胸肉', alias: '', category: '肉类', defaultUnit: '克', status: 1 },
]

export const nutrition: Nutrition[] = [
  { id: 1, ingredientId: 1, ingredientName: '番茄', calories: 20, protein: 0.9, fat: 0.2, carbs: 4, unitDesc: '每100g' },
  { id: 2, ingredientId: 2, ingredientName: '鸡蛋', calories: 144, protein: 13.3, fat: 8.8, carbs: 2.8, unitDesc: '每100g' },
  { id: 3, ingredientId: 7, ingredientName: '青椒', calories: 22, protein: 1, fat: 0.2, carbs: 5.4, unitDesc: '每100g' },
  { id: 4, ingredientId: 8, ingredientName: '土豆', calories: 77, protein: 2, fat: 0.2, carbs: 17.2, unitDesc: '每100g' },
  { id: 5, ingredientId: 23, ingredientName: '鸡胸肉', calories: 133, protein: 24.6, fat: 2.5, carbs: 0.6, unitDesc: '每100g' },
]

export const allergenTags: AllergenTag[] = [
  { id: 1, name: '花生', remark: '花生及其制品', status: 1 },
  { id: 2, name: '坚果', remark: '坚果类', status: 1 },
  { id: 3, name: '乳制品', remark: '牛奶及奶制品', status: 1 },
  { id: 4, name: '蛋类', remark: '鸡蛋及蛋制品', status: 1 },
  { id: 5, name: '海鲜', remark: '虾蟹贝类', status: 1 },
  { id: 6, name: '麸质', remark: '小麦及含麸质谷物', status: 1 },
]

export const users: AdminUser[] = [
  { id: 1, account: 'demo', nickname: '[昵称]', phone: '13800000000', status: 1, createTime: '2026-08-01 10:00' },
  { id: 2, account: 'user002', nickname: '厨房小白', phone: '', status: 1, createTime: '2026-08-02 11:20' },
  { id: 3, account: 'user003', nickname: '爱做菜', phone: '', status: 1, createTime: '2026-08-03 09:40' },
  { id: 4, account: 'user004', nickname: '减脂餐', phone: '', status: 1, createTime: '2026-08-04 15:10' },
  { id: 5, account: 'user005', nickname: '一日三餐', phone: '', status: 0, createTime: '2026-08-05 18:30' },
]

export const members: Member[] = [
  { id: 1, userId: 3, account: 'user003', planType: '会员', startTime: '2026-08-07', endTime: '2026-09-06', status: 1 },
  { id: 2, userId: 2, account: 'user002', planType: '会员', startTime: '2026-08-14', endTime: '2026-09-13', status: 1 },
]

export const orders: Order[] = [
  { id: 1, orderNo: 'CS202608010001', merchantName: '邻里鲜蔬铺', amount: 18.5, status: '配送中', deliveryDesc: '预计 30 分钟内送达', createTime: '2026-08-16 12:00' },
  { id: 2, orderNo: 'CS202608010002', merchantName: '早市调味杂货', amount: 16.2, status: '已完成', deliveryDesc: '', createTime: '2026-08-12 09:30' },
  { id: 3, orderNo: 'CS202608010003', merchantName: '邻里鲜蔬铺', amount: 18.5, status: '配送中', deliveryDesc: '预计 30 分钟内送达', createTime: '2026-08-17 10:00' },
  { id: 4, orderNo: 'CS202608010007', merchantName: '早市调味杂货', amount: 16.2, status: '待支付', deliveryDesc: '', createTime: '2026-08-17 11:00' },
  { id: 5, orderNo: 'CS202608010008', merchantName: '邻里鲜蔬铺', amount: 18.5, status: '已完成', deliveryDesc: '', createTime: '2026-08-08 19:00' },
]

export const merchants: Merchant[] = [
  { id: 1, name: '邻里鲜蔬铺', rating: 98, distanceKm: 0.8, deliveryMinutes: 30, tags: '综合推荐', stockStatus: '库存充足', auditStatus: '已通过' },
  { id: 2, name: '早市调味杂货', rating: 95, distanceKm: 1.6, deliveryMinutes: 45, tags: '价格最低', stockStatus: '库存充足', auditStatus: '已通过' },
  { id: 3, name: '家门口菜场', rating: 99, distanceKm: 0.4, deliveryMinutes: 25, tags: '', stockStatus: '库存紧张', auditStatus: '已通过' },
  { id: 4, name: '城西生鲜合作社', rating: 96, distanceKm: 2.1, deliveryMinutes: 40, tags: '', stockStatus: '库存充足', auditStatus: '待审核' },
  { id: 5, name: '四季菜篮子', rating: 94, distanceKm: 1.2, deliveryMinutes: 35, tags: '', stockStatus: '库存充足', auditStatus: '待审核' },
  { id: 6, name: '老王菜摊', rating: 91, distanceKm: 0.6, deliveryMinutes: 20, tags: '', stockStatus: '库存充足', auditStatus: '待审核' },
]

export const products: MerchantProduct[] = [
  { id: 1, merchantId: 1, merchantName: '邻里鲜蔬铺', name: '番茄', price: 4.5, stock: 100, auditStatus: '待审核' },
  { id: 2, merchantId: 1, merchantName: '邻里鲜蔬铺', name: '鸡蛋', price: 8, stock: 100, auditStatus: '待审核' },
  { id: 3, merchantId: 1, merchantName: '邻里鲜蔬铺', name: '青椒', price: 3.2, stock: 100, auditStatus: '待审核' },
  { id: 4, merchantId: 2, merchantName: '早市调味杂货', name: '洋葱', price: 2.5, stock: 100, auditStatus: '待审核' },
  { id: 5, merchantId: 1, merchantName: '邻里鲜蔬铺', name: '食用油', price: 5, stock: 200, auditStatus: '已通过' },
  { id: 6, merchantId: 3, merchantName: '家门口菜场', name: '五花肉', price: 18, stock: 60, auditStatus: '待审核' },
]

export const comments: CommentItem[] = [
  { id: 1, userId: 2, recipeId: 1, recipeName: '番茄炒蛋', rating: 5, content: '番茄出汁后放糖确实好吃。', status: '待审核', createTime: '2026-08-17 09:00' },
  { id: 2, userId: 3, recipeId: 1, recipeName: '番茄炒蛋', rating: 4, content: '火候有点过，蛋老了。', status: '待审核', createTime: '2026-08-17 07:00' },
  { id: 3, userId: 4, recipeId: 2, recipeName: '青椒土豆丝', rating: 5, content: '泡水这一步很关键。', status: '待审核', createTime: '2026-08-16 20:00' },
  { id: 4, userId: 2, recipeId: 3, recipeName: '番茄蛋花汤', rating: 5, content: '蛋花很漂亮。', status: '待审核', createTime: '2026-08-16 12:00' },
  { id: 5, userId: 1, recipeId: 1, recipeName: '番茄炒蛋', rating: 5, content: '最常做的一道菜。', status: '已通过', createTime: '2026-08-11 21:00' },
]

export const feedback: Feedback[] = [
  { id: 1, userId: 1, content: '希望支持导出 PDF 版菜谱。', contact: '13800000000', status: '待处理', createTime: '2026-08-17 10:00' },
  { id: 2, userId: 2, content: 'AI 生成的菜谱很有创意，赞！', contact: '', status: '待处理', createTime: '2026-08-17 06:00' },
  { id: 3, userId: 3, content: '购物清单能否按商家自动分组？', contact: '', status: '待处理', createTime: '2026-08-16 11:00' },
  { id: 4, userId: 4, content: '找不到某个菜谱的替代方案。', contact: '', status: '已处理', createTime: '2026-08-13 09:00' },
  { id: 5, userId: 5, content: '希望增加低卡筛选。', contact: '', status: '已处理', createTime: '2026-08-10 16:00' },
]

export const config: SysConfig = {
  'free.daily.limit': '5',
  'member.month.price': '19',
  'ai.single.price': '2.99',
  'ai.provider': 'openai-compatible',
  'ai.base-url': 'https://api.deepseek.com/v1',
  'ai.model': 'deepseek-chat',
  'ai.api-key': '',
  'ai.vision-base-url': 'https://open.bigmodel.cn/api/paas/v4',
  'ai.vision-model': 'glm-4v-flash',
  'ai.vision-api-key': '',
  'ai.vision-image-mode': 'auto',
  'third.meituan.url': 'https://i.meituan.com/',
  'third.duoduo.url': 'https://mobile.yangkeduo.com/',
}
