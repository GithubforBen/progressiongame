import { defineStore } from 'pinia'

interface User {
  id: number
  username: string
}

interface AuthState {
  user: User | null
  token: string | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    token: null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
  },

  actions: {
    async login(username: string, password: string) {
      const config = useRuntimeConfig()
      const data = await $fetch<{ token: string; user: User }>(`${config.public.apiBase}/api/auth/login`, {
        method: 'POST',
        body: { username, password },
      })
      this.token = data.token
      this.user = data.user
      if (import.meta.client) {
        localStorage.setItem('token', data.token)
      }
    },

    async register(username: string, password: string) {
      const config = useRuntimeConfig()
      const data = await $fetch<{ token: string; user: User }>(`${config.public.apiBase}/api/auth/register`, {
        method: 'POST',
        body: { username, password },
      })
      this.token = data.token
      this.user = data.user
      if (import.meta.client) {
        localStorage.setItem('token', data.token)
      }
    },

    logout() {
      this.user = null
      this.token = null
      if (import.meta.client) {
        localStorage.removeItem('token')
      }
      navigateTo('/login')
    },

    restoreSession() {
      if (import.meta.client) {
        const token = localStorage.getItem('token')
        if (token) {
          this.token = token
        }
      }
    },
  },
})
