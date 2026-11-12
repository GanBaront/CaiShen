import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },
        { path: 'ai', name: 'ai-recognize', component: () => import('@/views/AiRecognizeView.vue') },
        { path: 'recipes', name: 'recipes', component: () => import('@/views/RecipesView.vue') },
        {
          path: 'recipes/:id',
          name: 'recipe-detail',
          component: () => import('@/views/RecipeDetailView.vue')
        },
        {
          path: 'shopping-list',
          name: 'shopping-list',
          component: () => import('@/views/ShoppingListView.vue'),
          meta: { requiresAuth: true }
        },
        { path: 'member', name: 'member', component: () => import('@/views/MemberView.vue') },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/views/OrdersView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
          meta: { requiresAuth: true }
        }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

// 首页 / 推荐 / 详情未登录可浏览；仅个人中心等账户页做登录校验，
// 其余写操作（收藏、下单、评价、AI 生成等）在操作处单独校验。
router.beforeEach((to) => {
  const user = useUserStore()
  if (to.meta.requiresAuth && !user.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
