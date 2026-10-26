import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: {
      title: '首页',
      showNavbar: true,
      showFooter: true,
      requireAuth: false,
    },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: {
      title: '登录',
      showNavbar: false,
      showFooter: false,
      requireAuth: false,
    },
  },
  {
    path: '/interview',
    name: 'InterviewList',
    component: () => import('@/views/InterviewListView.vue'),
    meta: {
      title: '我的面试',
      showNavbar: true,
      showFooter: true,
      requireAuth: true,
    },
  },
  {
    path: '/interview/create',
    name: 'InterviewCreate',
    component: () => import('@/views/InterviewCreateView.vue'),
    meta: {
      title: '创建面试',
      showNavbar: true,
      showFooter: false,
      requireAuth: true,
    },
  },
  {
    path: '/interview/:id',
    name: 'InterviewDetail',
    component: () => import('@/views/InterviewDetailView.vue'),
    meta: {
      title: '面试详情',
      showNavbar: true,
      showFooter: false,
      requireAuth: true,
    },
  },
  {
    path: '/interview/:id/chat',
    name: 'InterviewChat',
    component: () => import('@/views/InterviewChatView.vue'),
    meta: {
      title: '面试答题',
      showNavbar: false,
      showFooter: false,
      requireAuth: true,
    },
  },
  {
    path: '/interview/:id/report',
    name: 'InterviewReport',
    component: () => import('@/views/InterviewReportView.vue'),
    meta: {
      title: '面试报告',
      showNavbar: true,
      showFooter: false,
      requireAuth: true,
    },
  },
  {
    path: '/resume',
    name: 'Resume',
    component: () => import('@/views/ResumeView.vue'),
    meta: {
      title: '简历管理',
      showNavbar: true,
      showFooter: true,
      requireAuth: true,
    },
  },
  {
    path: '/pricing',
    name: 'Pricing',
    component: () => import('@/views/PricingView.vue'),
    meta: {
      title: '套餐价格',
      showNavbar: true,
      showFooter: true,
      requireAuth: false,
    },
  },
  {
    path: '/order',
    name: 'OrderList',
    component: () => import('@/views/OrderListView.vue'),
    meta: {
      title: '我的订单',
      showNavbar: true,
      showFooter: true,
      requireAuth: true,
    },
  },
  {
    path: '/user',
    name: 'UserCenter',
    component: () => import('@/views/UserCenterView.vue'),
    meta: {
      title: '个人中心',
      showNavbar: true,
      showFooter: true,
      requireAuth: true,
    },
  },
  {
    path: '/ai-chat',
    name: 'AiChat',
    component: () => import('@/views/AiChatView.vue'),
    meta: {
      title: 'AI对话助手',
      showNavbar: true,
      showFooter: false,
      requireAuth: true,
    },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: {
      title: '页面不存在',
      showNavbar: true,
      showFooter: true,
      requireAuth: false,
    },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

// 全局前置守卫 - 认证检查
router.beforeEach((to, _from, next) => {
  // 设置页面标题
  const title = to.meta.title as string
  document.title = title ? `${title} - DCloud AI面试` : 'DCloud AI面试'

  // 认证检查
  const requireAuth = to.meta.requireAuth as boolean
  const token = localStorage.getItem('token')

  if (requireAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
