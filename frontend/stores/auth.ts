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
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('user')
      }
      // Reset game store by re-navigating (store state clears naturally)
      navigateTo('/login')
    },

    restoreSession() {
      if (import.meta.client) {
        const token = sessionStorage.getItem('token')
        const userRaw = sessionStorage.getItem('user')
        if (token) {
          this.token = token
          if (userRaw) {
            try {
              const parsed = JSON.parse(userRaw)
              // Validate the parsed object has the expected shape before trusting it
              if (parsed && typeof parsed.id === 'number' && typeof parsed.username === 'string') {
                this.user = parsed
              }
            } catch {}
          }
        }
      }
    },

    _persist(token: string, user: User) {
      this.token = token
      this.user = user
      if (import.meta.client) {
        sessionStorage.setItem('token', token)
        sessionStorage.setItem('user', JSON.stringify(user))
      }
    },
  },
})
