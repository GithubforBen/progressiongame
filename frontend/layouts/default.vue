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
          <!-- Jail / pending-catch banner -->
          <div v-if="caughtPending" class="hidden sm:flex items-center gap-1.5 text-xs text-red-400 font-semibold">
            <span>⚠️</span>
            <NuxtLink to="/steuerhinterziehung" class="underline underline-offset-2">Steuerfahndung auflösen</NuxtLink>
          </div>
          <div v-else-if="inJail" class="hidden sm:flex items-center gap-1.5 text-xs text-orange-400 font-semibold">
            <span>🔒</span>
            <span>{{ gameStore.character?.jailMonthsRemaining }} Monate Haft verbleiben</span>
          </div>
          <div v-else class="hidden sm:flex items-center gap-2">
            <span class="text-gray-500 text-xs">Kontostand</span>
            <span class="text-green-400 font-semibold font-mono text-sm">{{ formattedCash }}</span>
          </div>
          <button
            class="text-sm"
            :class="caughtPending ? 'btn-danger' : 'btn-primary'"
            :disabled="processingTurn || caughtPending"
            @click="endTurn"
          >
            {{ processingTurn ? 'Verarbeite…' : inJail ? `Monat absitzen (${gameStore.character?.jailMonthsRemaining} verbleiben)` : 'Monat abschliessen' }}
          </button>
        </div>
      </header>

      <main class="flex-1 overflow-y-auto p-6">
        <slot />
      </main>
    </div>

    <ToastContainer />

    <!-- Version badge -->
    <div class="fixed bottom-2 right-3 text-gray-700 text-xs font-mono select-none pointer-events-none">
      v10
    </div>

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

const inJail = computed(() => (gameStore.character?.jailMonthsRemaining ?? 0) > 0)
const caughtPending = computed(() => gameStore.character?.taxEvasionCaughtPending ?? false)

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
  // Surface events before clearing
  const events = gameStore.lastTurnResult?.events ?? []
  gameStore.clearTurnResult()
  for (const event of events) {
    // Tages-Events get a special warning style
    if (event.includes('Tages-Event') || event.includes('Angekommen')) {
      toastStore.warning(event)
    } else {
      toastStore.info(event)
    }
  }
}
</script>
