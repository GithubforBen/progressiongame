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
          <div class="hidden sm:flex items-center gap-2">
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

      <main class="flex-1 overflow-y-auto p-6">
        <slot />
      </main>
    </div>

    <ToastContainer />

    <!-- Monthly balance sheet modal -->
    <MonthlyBalanceSheet
      v-if="gameStore.lastTurnResult"
      :show="showBalanceSheet"
      :result="gameStore.lastTurnResult"
      :month-label="completedMonthLabel"
      @close="closeBalanceSheet"
    />
  </div>
</template>

<script setup lang="ts">
import { useGameStore } from '~/stores/game'
import { useToastStore } from '~/stores/toast'

const gameStore = useGameStore()
const toastStore = useToastStore()

const processingTurn = ref(false)
const showBalanceSheet = ref(false)
const completedMonthLabel = ref('')

const formattedCash = computed(() => {
  const cash = gameStore.character?.cash ?? 0
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(cash)
})

async function endTurn() {
  processingTurn.value = true
  // Capture the month label before the turn advances
  completedMonthLabel.value = gameStore.currentMonthLabel
  try {
    await gameStore.endTurn()
    showBalanceSheet.value = true
  }
  catch (e: any) {
    toastStore.error(e?.data?.message ?? 'Fehler beim Monatsabschluss.', 'Fehler')
  }
  finally {
    processingTurn.value = false
  }
}

function closeBalanceSheet() {
  showBalanceSheet.value = false
  gameStore.clearTurnResult()
  // Surface events from the turn as toasts
  if (gameStore.lastTurnResult?.events.length) {
    for (const event of gameStore.lastTurnResult.events) {
      toastStore.info(event)
    }
  }
}
</script>
