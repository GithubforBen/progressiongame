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
      this._persist(data.token, data.user)
    },

    async register(username: string, password: string) {
      const config = useRuntimeConfig()
      const data = await $fetch<{ token: string; user: User }>(`${config.public.apiBase}/api/auth/register`, {
        method: 'POST',
        body: { username, password },
      })
      this._persist(data.token, data.user)
    },

    logout() {
      this.user = null
      this.token = null
      if (import.meta.client) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
      }
      // Reset game store by re-navigating (store state clears naturally)
      navigateTo('/login')
    },

    restoreSession() {
      if (import.meta.client) {
        const token = localStorage.getItem('token')
        const userRaw = localStorage.getItem('user')
        if (token) {
          this.token = token
          if (userRaw) {
            try { this.user = JSON.parse(userRaw) } catch {}
          }
        }
      }
    },

    _persist(token: string, user: User) {
      this.token = token
      this.user = user
      if (import.meta.client) {
        localStorage.setItem('token', token)
        localStorage.setItem('user', JSON.stringify(user))
      }
    },
  },
})
