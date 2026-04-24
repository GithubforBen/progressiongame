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
  schufaScore?: number
  depressionMonthsRemaining?: number
  burnoutActive?: boolean
  taxEvasionActive?: boolean
  taxEvasionCaughtPending?: boolean
  cumulativeEvadedTaxes?: number
  jailMonthsRemaining?: number
  exileMonthsRemaining?: number
  victoryAchieved?: boolean
  personalBestNetWorth?: number
}

export interface MonthlyExpense {
  id: number
  category: string
  label: string
  amount: number
  active: boolean
  mandatory: boolean
}

export interface LineItem {
  label: string
  amount: number
}

export interface TurnResult {
  character: Character
  newTurn: number
  grossIncome: number
  taxPaid: number
  totalExpenses: number
  netChange: number
  incomeBreakdown: LineItem[]
  expenseBreakdown: LineItem[]
  events: string[]
  taxEvasionCaught?: boolean
  taxEvasionCaughtAmount?: number
}

export const useGameStore = defineStore('game', {
  state: () => ({
    character: null as Character | null,
    expenses: [] as MonthlyExpense[],
    lastTurnResult: null as TurnResult | null,
    loading: false,
    initialized: false,
    playtimeSeconds: 0,
    _playtimeInterval: null as ReturnType<typeof setInterval> | null,
  }),

  getters: {
    totalMonthlyExpenses: (state) =>
      state.expenses.filter((e) => e.active).reduce((sum, e) => sum + e.amount, 0),

    formattedPlaytime: (state) => {
      const h = Math.floor(state.playtimeSeconds / 3600)
      const m = Math.floor((state.playtimeSeconds % 3600) / 60)
      const s = state.playtimeSeconds % 60
      if (h > 0) return `${h}h ${m}m`
      if (m > 0) return `${m}m ${s}s`
      return `${s}s`
    },

    currentMonthLabel: (state) => {
      const months = [
        'Januar', 'Februar', 'März', 'April', 'Mai', 'Juni',
        'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember',
      ]
      const turn = state.character?.currentTurn ?? 1
      const year = 2025 + Math.floor((turn - 1) / 12)
      return `${months[(turn - 1) % 12]} ${year}`
    },
  },

  actions: {
    async init() {
      if (this.initialized) return
      // Ensure session is restored before making API calls (guards SSR/hydration edge cases)
      const authStore = useAuthStore()
      authStore.restoreSession()
      if (!authStore.token) return
      await Promise.all([this.fetchCharacter(), this.fetchExpenses()])
      this.initialized = true
      this.startPlaytimeTracking()
    },

    startPlaytimeTracking() {
      if (import.meta.client) {
        const stored = localStorage.getItem('playtimeSeconds')
        if (stored) this.playtimeSeconds = parseInt(stored, 10) || 0
        if (this._playtimeInterval) return
        this._playtimeInterval = setInterval(() => {
          this.playtimeSeconds++
          localStorage.setItem('playtimeSeconds', String(this.playtimeSeconds))
        }, 1000)
      }
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

      const result = await $fetch<TurnResult>(`${config.public.apiBase}/api/turn/end`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${authStore.token}` },
      })

      this.character = result.character
      this.lastTurnResult = result

      // Refresh expenses in case they changed
      await this.fetchExpenses()
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

    async toggleTaxEvasion() {
      const config = useRuntimeConfig()
      const authStore = useAuthStore()
      this.character = await $fetch<Character>(`${config.public.apiBase}/api/tax-evasion/toggle`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${authStore.token}` },
      })
    },

    async resolveCaught(choice: 'JAIL' | 'FLEE') {
      const config = useRuntimeConfig()
      const authStore = useAuthStore()
      this.character = await $fetch<Character>(`${config.public.apiBase}/api/tax-evasion/resolve-caught`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${authStore.token}` },
        body: { choice },
      })
    },

    async resetCharacter() {
      const config = useRuntimeConfig()
      const authStore = useAuthStore()
      await $fetch(`${config.public.apiBase}/api/character/reset`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${authStore.token}` },
      })
      this.initialized = false
      await this.init()
    },

    clearTurnResult() {
      this.lastTurnResult = null
    },

    reset() {
      this.character = null
      this.expenses = []
      this.lastTurnResult = null
      this.initialized = false
    },
  },
})
