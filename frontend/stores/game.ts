import { defineStore } from 'pinia'
import { useAuthStore } from './auth'

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

export const useGameStore = defineStore('game', {
  state: () => ({
    character: null as Character | null,
    loading: false,
  }),

  actions: {
    async fetchCharacter() {
      const config = useRuntimeConfig()
      const authStore = useAuthStore()
      this.character = await $fetch<Character>(`${config.public.apiBase}/api/character`, {
        headers: { Authorization: `Bearer ${authStore.token}` },
      })
    },

    async endTurn() {
      const config = useRuntimeConfig()
      const authStore = useAuthStore()
      const result = await $fetch<{ character: Character }>(`${config.public.apiBase}/api/turn/end`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${authStore.token}` },
      })
      this.character = result.character
    },
  },
})
