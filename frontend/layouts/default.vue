<template>
  <div class="flex h-screen bg-surface-950 text-gray-100 overflow-hidden">
    <!-- Desktop sidebar -->
    <div class="hidden md:flex flex-shrink-0">
      <Sidebar :compact="compactSidebar" />
    </div>

    <!-- Mobile backdrop -->
    <Transition name="backdrop">
      <div
        v-if="mobileMenuOpen"
        class="fixed inset-0 z-40 md:hidden bg-black/70"
        @click="mobileMenuOpen = false"
      />
    </Transition>

    <!-- Mobile drawer -->
    <Transition name="drawer">
      <div
        v-if="mobileMenuOpen"
        class="fixed left-0 top-0 bottom-0 z-50 md:hidden"
      >
        <Sidebar :compact="false" />
        <button
          class="absolute top-3.5 right-3 w-7 h-7 flex items-center justify-center rounded-lg text-gray-400 hover:text-gray-100 hover:bg-surface-700 transition-colors"
          aria-label="Menü schließen"
          @click="mobileMenuOpen = false"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
    </Transition>

    <div class="flex-1 flex flex-col overflow-hidden min-w-0">
      <!-- Top bar -->
      <header class="h-14 bg-surface-950 border-b border-surface-700 flex items-center justify-between px-3 sm:px-6 flex-shrink-0">
        <div class="flex items-center gap-2.5 min-w-0">
          <!-- Hamburger (mobile only) -->
          <button
            class="md:hidden flex items-center justify-center w-8 h-8 rounded-lg hover:bg-surface-700 transition-colors flex-shrink-0"
            aria-label="Menü öffnen"
            @click="mobileMenuOpen = true"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="21" y2="12" /><line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>
          <h1 class="text-sm font-semibold text-gray-100 tracking-tight truncate">{{ pageLabel }}</h1>
        </div>

        <div class="flex items-center gap-2 sm:gap-4 flex-shrink-0">
          <!-- Status indicators – full version on sm+ -->
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

          <!-- Status indicators – compact on mobile -->
          <span v-if="caughtPending" class="sm:hidden text-red-400 text-xs font-semibold">⚠️</span>
          <span v-else-if="inJail" class="sm:hidden text-orange-400 text-xs font-semibold">🔒 {{ gameStore.character?.jailMonthsRemaining }}M</span>
          <span v-else class="sm:hidden text-green-400 font-semibold font-mono text-xs">{{ formattedCash }}</span>

          <button
            class="px-2.5 sm:px-4 py-1.5 rounded-lg font-semibold transition-all tracking-wide border-none cursor-pointer disabled:opacity-50 whitespace-nowrap"
            :class="'text-xs sm:text-sm'"
            :style="caughtPending
              ? 'background: var(--accent-bg); border: 1px solid var(--accent-border); color: var(--accent)'
              : 'background: var(--accent); color: #fff'"
            :disabled="processingTurn || caughtPending"
            @click="endTurn"
            @mouseenter="e => { if (!processingTurn && !caughtPending) (e.currentTarget as HTMLElement).style.filter = 'brightness(1.12)' }"
            @mouseleave="e => (e.currentTarget as HTMLElement).style.filter = ''"
          >
            <span class="hidden sm:inline">
              {{ processingTurn ? 'Verarbeite…' : inJail ? `Absitzen (${gameStore.character?.jailMonthsRemaining})` : 'Monat abschließen →' }}
            </span>
            <span class="sm:hidden">
              {{ processingTurn ? '…' : inJail ? `🔒 ${gameStore.character?.jailMonthsRemaining}` : '→' }}
            </span>
          </button>
        </div>
      </header>

      <main class="flex-1 overflow-y-auto p-3 sm:p-6">
        <slot />
      </main>
    </div>

    <ToastContainer />

    <div class="fixed bottom-2 right-3 text-surface-600 text-xs font-mono select-none pointer-events-none">v15</div>

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
const mobileMenuOpen = ref(false)
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

watch(() => route.path, () => {
  mobileMenuOpen.value = false
})

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

<style scoped>
.backdrop-enter-active,
.backdrop-leave-active {
  transition: opacity 0.25s ease;
}
.backdrop-enter-from,
.backdrop-leave-to {
  opacity: 0;
}

.drawer-enter-active,
.drawer-leave-active {
  transition: transform 0.25s ease;
}
.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(-100%);
}
</style>
