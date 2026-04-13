<template>
  <div class="flex h-screen bg-surface-900 text-gray-100 overflow-hidden">
    <Sidebar />

    <div class="flex-1 flex flex-col overflow-hidden min-w-0">
      <!-- Top bar -->
      <header class="h-14 bg-surface-800 border-b border-surface-700 flex items-center justify-between px-6 flex-shrink-0">
        <div class="flex items-center gap-2">
          <span class="text-gray-500 text-xs uppercase tracking-wider">Monat</span>
          <span class="text-white font-semibold text-sm font-mono">{{ gameStore.currentMonthLabel }}</span>
        </div>

        <div class="flex items-center gap-4">
          <div class="hidden sm:flex items-center gap-2 text-sm">
            <span class="text-gray-500 text-xs">Kontostand</span>
            <span class="text-green-400 font-semibold font-mono text-sm">{{ formattedCash }}</span>
          </div>
          <button
            class="btn-primary text-sm"
            :disabled="processingTurn"
            @click="endTurn"
          >
            {{ processingTurn ? 'Verarbeite…' : 'Monat abschliessen' }}
          </button>
        </div>
      </header>

      <!-- Page content -->
      <main class="flex-1 overflow-y-auto p-6">
        <slot />
      </main>
    </div>

    <ToastContainer />
  </div>
</template>

<script setup lang="ts">
import { useGameStore } from '~/stores/game'
import { useToastStore } from '~/stores/toast'

const gameStore = useGameStore()
const toastStore = useToastStore()
const processingTurn = ref(false)

const formattedCash = computed(() => {
  const cash = gameStore.character?.cash ?? 0
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(cash)
})

async function endTurn() {
  processingTurn.value = true
  try {
    await gameStore.endTurn()
  }
  catch (e: any) {
    toastStore.error(e?.data?.message ?? 'Fehler beim Monatsabschluss.', 'Fehler')
  }
  finally {
    processingTurn.value = false
  }
}
</script>
