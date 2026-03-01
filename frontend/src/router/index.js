import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const MainLayout = () => import('../layouts/MainLayout.vue')
const LoginView = () => import('../views/auth/LoginView.vue')
const HomeView = () => import('../views/home/HomeView.vue')
const ProductListView = () => import('../views/product/ProductListView.vue')
const OrderView = () => import('../views/order/OrderView.vue')
const DisputeView = () => import('../views/order/DisputeView.vue')
const ShopView = () => import('../views/shop/ShopView.vue')
const ChatView = () => import('../views/chat/ChatView.vue')
const NotificationView = () => import('../views/user/NotificationView.vue')
const AdminView = () => import('../views/admin/AdminView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', component: HomeView },
        { path: 'products', component: ProductListView },
        { path: 'orders', component: OrderView },
        { path: 'shop', component: ShopView },
        { path: 'chat', component: ChatView },
        { path: 'notifications', component: NotificationView },
        { path: 'disputes', component: DisputeView },
        { path: 'admin', component: AdminView }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.token) {
    return '/login'
  }
  return true
})

export default router
