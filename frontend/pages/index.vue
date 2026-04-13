<template>
  <div class="space-y-5">
    <!-- Loading state -->
    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="text-gray-500 text-sm">Lade Spieldaten…</div>
    </div>

    <template v-else>
      <!-- Top stats -->
      <div class="grid grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard
          title="Kontostand"
          :value="formatCurrency(character?.cash ?? 0)"
          :trend-positive="cashTrend >= 0"
          :trend="cashTrend !== 0 ? formatCurrency(Math.abs(cashTrend)) : undefined"
        />
        <StatCard
          title="Nettovermögen"
          :value="formatCurrency(character?.netWorth ?? 0)"
        />
        <StatCard
          title="Monatliche Ausgaben"
          :value="formatCurrency(gameStore.totalMonthlyExpenses)"
          subtitle="Alle aktiven Posten"
        />
        <StatCard
          title="Aktueller Monat"
          :value="gameStore.currentMonthLabel"
          subtitle="Runde {{ character?.currentTurn ?? 1 }}"
        />
      </div>

      <!-- Needs -->
      <CharacterNeeds />

      <!-- Bottom row -->
      <div class="grid grid-cols-1 xl:grid-cols-2 gap-5">
        <!-- Expenses widget -->
        <ExpensesWidget />

        <!-- Quick links -->
        <div class="card space-y-3">
          <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Schnellzugriff</h3>

          <button
            class="w-full text-left flex items-center justify-between p-3 rounded-lg bg-surface-700 hover:bg-surface-600 transition-colors"
            @click="navigateTo('/karriere')"
          >
            <div>
              <p class="text-sm font-medium text-white">Karriere</p>
              <p class="text-xs text-gray-500 mt-0.5">Jobs suchen & verwalten</p>
            </div>
            <span class="text-gray-500">→</span>
          </button>

          <button
            class="w-full text-left flex items-center justify-between p-3 rounded-lg bg-surface-700 hover:bg-surface-600 transition-colors"
            @click="navigateTo('/investitionen')"
          >
            <div>
              <p class="text-sm font-medium text-white">Investitionen</p>
              <p class="text-xs text-gray-500 mt-0.5">Aktien, Immobilien & mehr</p>
            </div>
            <span class="text-gray-500">→</span>
          </button>

          <button
            class="w-full text-left flex items-center justify-between p-3 rounded-lg bg-surface-700 hover:bg-surface-600 transition-colors"
            @click="navigateTo('/ausbildung')"
          >
            <div>
              <p class="text-sm font-medium text-white">Ausbildung</p>
              <p class="text-xs text-gray-500 mt-0.5">Bildungsweg & Zertifikate</p>
            </div>
            <span class="text-gray-500">→</span>
          </button>
        </div>
      </div>

      <!-- Net worth chart placeholder — replaced in Step 12 -->
      <div class="card">
        <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-4">Nettovermögen-Verlauf</h3>
        <div
          class="h-36 flex items-center justify-center text-gray-600 text-sm
                 border border-dashed border-surface-600 rounded-lg"
        >
          Verlaufsdiagramm erscheint nach dem ersten Monatsabschluss (Schritt 12)
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useGameStore } from '~/stores/game'

definePageMeta({ layout: 'default' })

const gameStore = useGameStore()
const character = computed(() => gameStore.character)
const loading = ref(true)
const previousCash = ref<number | null>(null)

const cashTrend = computed(() => {
  if (previousCash.value === null || !character.value) return 0
  return character.value.cash - previousCash.value
})

function formatCurrency(value: number) {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

onMounted(async () => {
  try {
    await gameStore.init()
  } finally {
    loading.value = false
  }
})
</script>
