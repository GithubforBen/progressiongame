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
          subtitle="`Runde ${character?.currentTurn ?? 1}`"
        />
      </div>

      <!-- Needs -->
      <CharacterNeeds />

      <!-- Charts row -->
      <div class="grid grid-cols-1 xl:grid-cols-2 gap-5">
        <!-- Net worth + cash chart -->
        <div class="card">
          <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-4">
            Vermögensverlauf
          </h3>
          <div v-if="snapshots.length < 2" class="h-40 flex items-center justify-center text-gray-600 text-sm border border-dashed border-surface-600 rounded-lg">
            Verlauf erscheint nach dem ersten Monatsabschluss
          </div>
          <div v-else class="h-40">
            <ClientOnly>
              <LineChart :data="netWorthChartData" :options="lineChartOptions" class="h-full" />
            </ClientOnly>
          </div>
        </div>

        <!-- Income vs Expenses bar chart -->
        <div class="card">
          <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-4">
            Einnahmen vs. Ausgaben
          </h3>
          <div v-if="snapshots.length < 2" class="h-40 flex items-center justify-center text-gray-600 text-sm border border-dashed border-surface-600 rounded-lg">
            Verlauf erscheint nach dem ersten Monatsabschluss
          </div>
          <div v-else class="h-40">
            <ClientOnly>
              <BarChart :data="incomeExpensesChartData" :options="barChartOptions" class="h-full" />
            </ClientOnly>
          </div>
        </div>
      </div>

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
    </template>
  </div>
</template>

<script setup lang="ts">
import { Line as LineChart, Bar as BarChart } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Filler,
  Tooltip,
  Legend,
} from 'chart.js'
import { useGameStore } from '~/stores/game'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Filler, Tooltip, Legend)

definePageMeta({ layout: 'default' })

const api = useApi()
const gameStore = useGameStore()
const { formatCurrency } = useFormatting()
const character = computed(() => gameStore.character)
const loading = ref(true)
const previousCash = ref<number | null>(null)

interface Snapshot {
  turn: number
  cash: number
  netWorth: number
  totalIncome: number
  totalExpenses: number
}

const snapshots = ref<Snapshot[]>([])

const cashTrend = computed(() => {
  if (previousCash.value === null || !character.value) return 0
  return character.value.cash - previousCash.value
})

const netWorthChartData = computed(() => ({
  labels: snapshots.value.map(s => `M${s.turn}`),
  datasets: [
    {
      label: 'Nettovermögen',
      data: snapshots.value.map(s => s.netWorth),
      borderColor: '#6366f1',
      backgroundColor: 'rgba(99,102,241,0.12)',
      fill: true,
      tension: 0.4,
      pointRadius: 2,
    },
    {
      label: 'Bargeld',
      data: snapshots.value.map(s => s.cash),
      borderColor: '#22c55e',
      backgroundColor: 'transparent',
      fill: false,
      tension: 0.4,
      pointRadius: 2,
      borderDash: [4, 3],
    },
  ],
}))

const incomeExpensesChartData = computed(() => ({
  labels: snapshots.value.map(s => `M${s.turn}`),
  datasets: [
    {
      label: 'Einnahmen',
      data: snapshots.value.map(s => s.totalIncome),
      backgroundColor: 'rgba(34,197,94,0.7)',
    },
    {
      label: 'Ausgaben',
      data: snapshots.value.map(s => s.totalExpenses),
      backgroundColor: 'rgba(239,68,68,0.7)',
    },
  ],
}))

const sharedScaleOptions = {
  x: { ticks: { color: '#6b7280', font: { size: 10 } }, grid: { color: '#ffffff0a' } },
  y: { ticks: { color: '#6b7280', font: { size: 10 } }, grid: { color: '#ffffff0a' } },
}

const lineChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: true, labels: { color: '#9ca3af', font: { size: 10 }, boxWidth: 12 } },
    tooltip: { mode: 'index' as const },
  },
  scales: sharedScaleOptions,
}

const barChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: true, labels: { color: '#9ca3af', font: { size: 10 }, boxWidth: 12 } },
    tooltip: { mode: 'index' as const },
  },
  scales: sharedScaleOptions,
}

onMounted(async () => {
  try {
    await gameStore.init()
    snapshots.value = await api.get<Snapshot[]>('/api/stats/snapshots')
  } finally {
    loading.value = false
  }
})
</script>
