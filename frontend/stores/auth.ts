import { defineStore } from 'pinia'

interface User {
  id: number
  username: string
}

interface AuthState {
  user: User | null
  token: string | null
  isAdmin: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    token: null,
    isAdmin: false,
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
      await this._fetchAdminStatus()
    },

    async register(username: string, password: string) {
      const config = useRuntimeConfig()
      const data = await $fetch<{ token: string; user: User }>(`${config.public.apiBase}/api/auth/register`, {
        method: 'POST',
        body: { username, password },
      })
      this._persist(data.token, data.user)
      await this._fetchAdminStatus()
    },

    logout() {
      this.user = null
      this.token = null
      this.isAdmin = false
      if (import.meta.client) {
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('user')
      }
      // Reset game store by re-navigating (store state clears naturally)
      navigateTo('/login')
    },

    async restoreSession() {
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
          await this._fetchAdminStatus()
        }
      }
    },

    async _fetchAdminStatus() {
      if (!this.token) return
      try {
        const config = useRuntimeConfig()
        const data = await $fetch<{ isAdmin: boolean }>(`${config.public.apiBase}/api/admin/me`, {
          headers: { Authorization: `Bearer ${this.token}` },
        })
        this.isAdmin = data.isAdmin
      } catch {
        this.isAdmin = false
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
