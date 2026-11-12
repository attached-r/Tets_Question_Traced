import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        // 支持人员默认进看板，员工默认进提交页
        redirect: () => (useAuthStore().isSupport ? '/board' : '/submit'),
      },
      {
        path: 'submit',
        name: 'submit',
        component: () => import('@/views/employee/SubmitView.vue'),
      },
      {
        path: 'my',
        name: 'my',
        component: () => import('@/views/employee/MyListView.vue'),
      },
      {
        path: 'board',
        name: 'board',
        component: () => import('@/views/support/BoardView.vue'),
        meta: { roles: ['SUPPORT', 'ADMIN'] },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { public: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * 前端守卫只解决体验：没登录就别进业务页，员工别进看板。
 * 真正的边界在后端（拦截器 + Service 内的归属校验），手改前端绕不过去。
 */
router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    return true
  }
  if (!auth.isLogin) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const roles = to.meta.roles as string[] | undefined
  if (roles && !roles.includes(auth.role)) {
    return { name: 'my' }
  }
  return true
})

export default router
