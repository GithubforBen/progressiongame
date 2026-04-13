<template>
  <div class="flex h-screen bg-surface-900 text-gray-100 overflow-hidden">
    <Sidebar />

    <div class="flex-1 flex flex-col overflow-hidden min-w-0">
      <!-- Top bar -->
      <header class="h-14 bg-surface-800 border-b border-surface-700 flex items-center justify-between px-6 flex-shrink-0">
        <div class="flex items-center gap-2">
          <span class="text-gray-400 text-sm">Monat</span>
          <span class="text-white font-semibold text-sm font-mono">{{ currentMonthLabel }}</span>
        </div>

        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2 text-sm">
            <span class="text-gray-400">Kontostand:</span>
            <span class="text-green-400 font-semibold font-mono">{{ formattedCash }}</span>
          </div>
          <button
            class="btn-primary"
            :disabled="processingTurn"
            @click="endTurn"
          >
            {{ processingTurn ? 'Wird verarbeitet...' : 'Monat abschliessen' }}
          </button>
        </div>
      </header>

      <!-- Page content -->
      <main class="flex-1 overflow-y-auto p-6">
        <slot />
      </main>
    </div>

    <!-- Toast notifications -->
    <ToastContainer />
  </div>
</template>

<script setup lang="ts">
import { useGameStore } from '~/stores/game'

const gameStore = useGameStore()

const processingTurn = ref(false)

const currentMonthLabel = computed(() => {
  const months = [
    'Januar', 'Februar', 'März', 'April', 'Mai', 'Juni',
    'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember',
  ]
  const turn = gameStore.character?.currentTurn ?? 1
  const year = 2025 + Math.floor((turn - 1) / 12)
  const month = months[(turn - 1) % 12]
  return `${month} ${year}`
})

const formattedCash = computed(() => {
  const cash = gameStore.character?.cash ?? 0
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(cash)
})

async function endTurn() {
  processingTurn.value = true
  try {
    await gameStore.endTurn()
  } finally {
    processingTurn.value = false
  }
}
</script>
