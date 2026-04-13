import { useAuthStore } from '~/stores/auth'

export default defineNuxtRouteMiddleware((to) => {
  const authStore = useAuthStore()
  authStore.restoreSession()

  const publicRoutes = ['/login', '/register']
  if (!authStore.isAuthenticated && !publicRoutes.includes(to.path)) {
    return navigateTo('/login')
  }
  if (authStore.isAuthenticated && publicRoutes.includes(to.path)) {
    return navigateTo('/')
  }
})
