<template>
  <div class="flex h-screen bg-surface-950 text-gray-100 overflow-hidden">
    <Sidebar :compact="compactSidebar" />

    <div class="flex-1 flex flex-col overflow-hidden min-w-0">
      <!-- Top bar -->
      <header class="h-14 bg-surface-950 border-b border-surface-700 flex items-center justify-between px-6 flex-shrink-0">
        <h1 class="text-sm font-semibold text-gray-100 tracking-tight">{{ pageLabel }}</h1>

        <div class="flex items-center gap-4">
          <!-- Status indicators -->
          <div v-if="caughtPending" class="hidden sm:flex items-center gap-1.5 text-xs text-red-400 font-semibold">
            <span>⚠️</span>
            <NuxtLink to="/steuerhinterziehung" class="underline underline-offset-2">Steuerfahndung auflösen</NuxtLink>
          </div>
          <div v-else-if="inJail" class="hidden sm:flex items-center gap-1.5 text-xs text-orange-400 font-semibold">
            <span>🔒</span>
            <span>{{ gameStore.character?.jailMonthsRemaining }} Monate Haft</span>
          </div>
          <div v-else class="hidden sm:flex items-center gap-2">
            <span class="text-gray-600 text-xs">{{ gameStore.currentMonthLabel }}</span>
            <span class="text-green-400 font-semibold font-mono text-sm">{{ formattedCash }}</span>
          </div>

          <button
            class="text-sm px-4 py-1.5 rounded-lg font-semibold transition-all text-sm tracking-wide border-none cursor-pointer disabled:opacity-50"
            :style="caughtPending
              ? 'background: var(--accent-bg); border: 1px solid var(--accent-border); color: var(--accent)'
              : 'background: var(--accent); color: #fff'"
            :disabled="processingTurn || caughtPending"
            @click="endTurn"
            @mouseenter="e => { if (!processingTurn && !caughtPending) e.currentTarget.style.filter = 'brightness(1.12)' }"
            @mouseleave="e => e.currentTarget.style.filter = ''"
          >
            {{ processingTurn ? 'Verarbeite…' : inJail ? `Absitzen (${gameStore.character?.jailMonthsRemaining})` : 'Monat abschließen →' }}
          </button>
        </div>
      </header>

      <main class="flex-1 overflow-y-auto p-6">
        <slot />
      </main>
    </div>

    <ToastContainer />

    <div class="fixed bottom-2 right-3 text-surface-600 text-xs font-mono select-none pointer-events-none">v10</div>

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

const route = useRoute()
const gameStore = useGameStore()
const toastStore = useToastStore()

const compactSidebar = ref(false)
const processingTurn = ref(false)
const showBalanceSheet = ref(false)
const completedMonthLabel = ref('')

const inJail = computed(() => (gameStore.character?.jailMonthsRemaining ?? 0) > 0)
const caughtPending = computed(() => gameStore.character?.taxEvasionCaughtPending ?? false)

const formattedCash = computed(() => {
  const cash = gameStore.character?.cash ?? 0
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(cash)
})

const PAGE_LABELS: Record<string, string> = {
  '/':                    'Dashboard',
  '/investitionen':       'Investitionen',
  '/karriere':            'Karriere',
  '/ausbildung':          'Ausbildung',
  '/leben':               'Leben',
  '/beduerfnisse':        'Bedürfnisse',
  '/reisen':              'Reisen',
  '/sammlungen':          'Sammlungen',
  '/immobilien':          'Immobilien',
  '/kredite':             'Kredite',
  '/gluecksspiel':        'Glücksspiel',
  '/beziehungen':         'Beziehungen',
  '/steuerhinterziehung': 'Steuern',
  '/rangliste':           'Rangliste',
  '/einstellungen':       'Einstellungen',
}

const pageLabel = computed(() => PAGE_LABELS[route.path] ?? 'FinanzLeben')

async function endTurn() {
  processingTurn.value = true
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
  const events = gameStore.lastTurnResult?.events ?? []
  gameStore.clearTurnResult()
  for (const event of events) {
    if (event.includes('Tages-Event') || event.includes('Angekommen')) {
      toastStore.warning(event)
    } else {
      toastStore.info(event)
    }
  }
}
</script>
