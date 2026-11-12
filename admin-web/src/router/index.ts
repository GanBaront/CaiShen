import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/api/token'
import AdminLayout from '@/layouts/AdminLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '数据概览' } },
      { path: 'recipes', name: 'recipes', component: () => import('@/views/RecipeList.vue'), meta: { title: '菜谱库' } },
      { path: 'ingredients', name: 'ingredients', component: () => import('@/views/IngredientList.vue'), meta: { title: '食材库' } },
      { path: 'nutrition', name: 'nutrition', component: () => import('@/views/NutritionList.vue'), meta: { title: '营养数据' } },
      { path: 'allergen-tags', name: 'allergenTags', component: () => import('@/views/AllergenTagList.vue'), meta: { title: '过敏源标签' } },
      { path: 'users', name: 'users', component: () => import('@/views/UserList.vue'), meta: { title: '用户管理' } },
      { path: 'members-orders', name: 'membersOrders', component: () => import('@/views/MemberOrderList.vue'), meta: { title: '会员与订单' } },
      { path: 'merchants', name: 'merchants', component: () => import('@/views/MerchantAudit.vue'), meta: { title: '商家审核' } },
      { path: 'products', name: 'products', component: () => import('@/views/ProductAudit.vue'), meta: { title: '商品审核' } },
      { path: 'comments', name: 'comments', component: () => import('@/views/CommentAudit.vue'), meta: { title: '评论审核' } },
      { path: 'feedback', name: 'feedback', component: () => import('@/views/FeedbackList.vue'), meta: { title: '用户反馈' } },
      { path: 'config', name: 'config', component: () => import('@/views/SystemConfig.vue'), meta: { title: '系统配置' } },
    ],
  },
  { path: '/', redirect: '/admin/dashboard' },
  { path: '/:pathMatch(.*)*', redirect: '/admin/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!getToken()) return { path: '/login', query: { redirect: to.fullPath } }
  return true
})

export default router
