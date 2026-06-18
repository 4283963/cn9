import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/simulation'
  },
  {
    path: '/simulation',
    name: 'simulation',
    component: () => import('@/views/Simulation.vue')
  },
  {
    path: '/recipes',
    name: 'recipes',
    component: () => import('@/views/Recipes.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
