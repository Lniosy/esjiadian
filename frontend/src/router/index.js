import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const MainLayout = () => import('../layouts/MainLayout.vue')
const LoginView = () => import('../views/auth/LoginView.vue')
const HomeView = () => import('../views/home/HomeView.vue')
const ProductListView = () => import('../views/product/ProductListView.vue')
const OrderLayoutView = () => import('../views/order/OrderLayoutView.vue')
const OrderListView = () => import('../views/order/OrderListView.vue')
const OrderCartView = () => import('../views/order/OrderCartView.vue')
const OrderAddressView = () => import('../views/order/OrderAddressView.vue')
const DisputeView = () => import('../views/order/DisputeView.vue')
const ShopView = () => import('../views/shop/ShopView.vue')
const SellerHomeView = () => import('../views/shop/SellerHomeView.vue')
const ChatView = () => import('../views/chat/ChatView.vue')
const NotificationView = () => import('../views/user/NotificationView.vue')
const AdminLayoutView = () => import('../views/admin/AdminLayoutView.vue')
const AdminOverviewView = () => import('../views/admin/AdminOverviewView.vue')
const AdminMonitorView = () => import('../views/admin/AdminMonitorView.vue')
const AdminGovernanceView = () => import('../views/admin/AdminGovernanceView.vue')
const AdminRiskView = () => import('../views/admin/AdminRiskView.vue')

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
        {
          path: 'orders',
          component: OrderLayoutView,
          children: [
            { path: '', redirect: '/orders/list' },
            { path: 'list', component: OrderListView },
            { path: 'cart', component: OrderCartView },
            { path: 'address', component: OrderAddressView }
          ]
        },
        { path: 'shop', component: ShopView },
        { path: 'users/:id', component: SellerHomeView },
        { path: 'chat', component: ChatView },
        { path: 'notifications', component: NotificationView },
        { path: 'disputes', component: DisputeView },
        {
          path: 'admin',
          component: AdminLayoutView,
          children: [
            { path: '', redirect: '/admin/overview' },
            { path: 'overview', component: AdminOverviewView },
            { path: 'monitor', component: AdminMonitorView },
            { path: 'governance', component: AdminGovernanceView },
            { path: 'risk', component: AdminRiskView }
          ]
        }
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
