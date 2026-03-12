import { createRouter, createWebHistory } from 'vue-router'

// 页面视图
import ChatView from '@/views/ChatView.vue'
import DashboardView from '@/views/DashboardView.vue'
import KnowledgeView from '@/views/KnowledgeView.vue'
import SettingsView from '@/views/SettingsView.vue'

const routes = [
  {
    path: '/',
    name: 'chat',
    component: ChatView,
    meta: { title: '智能客服' }
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: DashboardView,
    meta: { title: '仪表盘' }
  },
  {
    path: '/knowledge',
    name: 'knowledge',
    component: KnowledgeView,
    meta: { title: '知识库管理' }
  },
  {
    path: '/settings',
    name: 'settings',
    component: SettingsView,
    meta: { title: '系统设置' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫 - 设置页面标题
router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || '智能客服'} - AgentScope`
  next()
})

export default router
