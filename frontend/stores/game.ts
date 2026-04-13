import { defineStore } from 'pinia'
import { useAuthStore } from './auth'
import { useToastStore } from './toast'

export interface Character {
  id: number
  cash: number
  netWorth: number
  stress: number
  hunger: number
  energy: number
  happiness: number
  currentTurn: number
}

export interface MonthlyExpense {
  id: number
  category: string
  label: string
  amount: number
  active: boolean
  mandatory: boolean
}

export const useGameStore = defineStore('game', {
  state: () => ({
    character: null as Character | null,
    expenses: [] as MonthlyExpense[],
    loading: false,
    initialized: false,
  }),

  getters: {
    totalMonthlyExpenses: (state) =>
      state.expenses
        .filter((e) => e.active)
        .reduce((sum, e) => sum + e.amount, 0),

    currentMonthLabel: (state) => {
      const months = [
        'Januar', 'Februar', 'März', 'April', 'Mai', 'Juni',
        'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember',
      ]
      const turn = state.character?.currentTurn ?? 1
      const year = 2025 + Math.floor((turn - 1) / 12)
      const month = months[(turn - 1) % 12]
      return `${month} ${year}`
    },
  },

  actions: {
    async init() {
      if (this.initialized) return
      await Promise.all([this.fetchCharacter(), this.fetchExpenses()])
      this.initialized = true
    },

    async fetchCharacter() {
      const config = useRuntimeConfig()
      const authStore = useAuthStore()
      this.character = await $fetch<Character>(`${config.public.apiBase}/api/character`, {
        headers: { Authorization: `Bearer ${authStore.token}` },
      })
    },

    async fetchExpenses() {
      const config = useRuntimeConfig()
      const authStore = useAuthStore()
      this.expenses = await $fetch<MonthlyExpense[]>(`${config.public.apiBase}/api/expenses`, {
        headers: { Authorization: `Bearer ${authStore.token}` },
      })
    },

    async endTurn() {
      const config = useRuntimeConfig()
      const authStore = useAuthStore()
      const toastStore = useToastStore()

      const result = await $fetch<{ character: Character }>(`${config.public.apiBase}/api/turn/end`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${authStore.token}` },
      })
      this.character = result.character
      toastStore.success(
        `Monat ${this.currentMonthLabel} abgeschlossen.`,
        'Neuer Monat',
      )
    },

    async toggleExpense(id: number) {
      const config = useRuntimeConfig()
      const authStore = useAuthStore()
      const updated = await $fetch<MonthlyExpense>(`${config.public.apiBase}/api/expenses/${id}/toggle`, {
        method: 'PATCH',
        headers: { Authorization: `Bearer ${authStore.token}` },
      })
      const idx = this.expenses.findIndex((e) => e.id === id)
      if (idx !== -1) this.expenses[idx] = updated
    },

    reset() {
      this.character = null
      this.expenses = []
      this.initialized = false
    },
  },
})
