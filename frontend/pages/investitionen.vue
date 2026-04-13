<template>
  <div class="space-y-6">
    <h2 class="text-xl font-bold text-white">Investitionen</h2>

    <!-- Portfolio Summary -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
      <div class="card text-center">
        <p class="text-xs text-gray-400 mb-1">Investiert</p>
        <p class="text-white font-semibold text-sm">{{ formatCurrency(totalInvested) }}</p>
      </div>
      <div class="card text-center">
        <p class="text-xs text-gray-400 mb-1">Aktueller Wert</p>
        <p class="text-white font-semibold text-sm">{{ formatCurrency(totalCurrentValue) }}</p>
      </div>
      <div class="card text-center">
        <p class="text-xs text-gray-400 mb-1">Gewinn / Verlust</p>
        <p class="font-semibold text-sm" :class="totalGainLoss >= 0 ? 'text-green-400' : 'text-red-400'">
          {{ totalGainLoss >= 0 ? '+' : '' }}{{ formatCurrency(totalGainLoss) }}
        </p>
      </div>
      <div class="card text-center">
        <p class="text-xs text-gray-400 mb-1">Positionen</p>
        <p class="text-white font-semibold text-sm">{{ portfolio.length }}</p>
      </div>
    </div>

    <!-- Portfolio Positions -->
    <div class="card">
      <h3 class="text-base font-semibold text-white mb-4">Mein Portfolio</h3>
      <div v-if="portfolioLoading" class="text-gray-500 text-sm">Lade...</div>
      <div v-else-if="portfolio.length === 0" class="text-gray-500 text-sm py-4 text-center">
        Noch keine Investitionen. Kauf deine erste Aktie unten!
      </div>
      <div v-else class="space-y-2">
        <div
          v-for="inv in portfolio"
          :key="inv.id"
          class="flex items-center gap-3 p-3 rounded-lg bg-white/5"
        >
          <div class="flex-1 min-w-0">
            <p class="text-white font-medium text-sm">{{ inv.name }}</p>
            <p class="text-gray-400 text-xs">
              {{ inv.quantity }} Stk. · Gekauft für {{ formatCurrency(inv.amountInvested) }}
            </p>
          </div>
          <div class="text-right flex-shrink-0">
            <p class="text-white font-semibold text-sm">{{ formatCurrency(inv.currentValue) }}</p>
            <p class="text-xs font-medium" :class="inv.gainLoss >= 0 ? 'text-green-400' : 'text-red-400'">
              {{ inv.gainLoss >= 0 ? '+' : '' }}{{ formatCurrency(inv.gainLoss) }}
              ({{ inv.gainLossPct !== null ? (inv.gainLoss >= 0 ? '+' : '') + inv.gainLossPct + '%' : '–' }})
            </p>
          </div>
          <button
            @click="sellPosition(inv)"
            :disabled="sellLoading === inv.id"
            class="btn-secondary text-xs px-3 py-1.5 flex-shrink-0 bg-red-500/10 text-red-400 hover:bg-red-500/20 border-red-500/20"
          >
            {{ sellLoading === inv.id ? '...' : 'Verkaufen' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Stock Market -->
    <div class="card">
      <div class="flex items-center gap-3 mb-4">
        <h3 class="text-base font-semibold text-white flex-1">Börse</h3>
        <div class="flex gap-1">
          <button
            v-for="f in typeFilters"
            :key="f.value"
            class="px-2.5 py-1 rounded text-xs font-medium transition-colors"
            :class="typeFilter === f.value
              ? 'bg-accent text-white'
              : 'bg-surface-700 text-gray-400 hover:text-white'"
            @click="typeFilter = f.value"
          >
            {{ f.label }}
          </button>
        </div>
      </div>

      <div v-if="stocksLoading" class="text-gray-500 text-sm">Lade...</div>
      <div v-else class="space-y-2">
        <div
          v-for="stock in filteredStocks"
          :key="stock.id"
          class="rounded-lg border transition-colors cursor-pointer"
          :class="selectedStock?.id === stock.id
            ? 'border-accent/50 bg-accent/5'
            : 'border-white/5 bg-white/3 hover:bg-white/5'"
          @click="selectStock(stock)"
        >
          <div class="flex items-center gap-3 p-3">
            <!-- Ticker badge -->
            <div class="w-12 h-12 rounded-lg flex items-center justify-center flex-shrink-0"
                 :class="stock.type === 'MEME' ? 'bg-purple-500/20' : 'bg-blue-500/20'">
              <span class="text-xs font-bold"
                    :class="stock.type === 'MEME' ? 'text-purple-300' : 'text-blue-300'">
                {{ stock.ticker }}
              </span>
            </div>
            <!-- Info -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <p class="text-white font-medium text-sm">{{ stock.name }}</p>
                <span class="text-xs px-1.5 py-0.5 rounded"
                      :class="stock.type === 'MEME'
                        ? 'bg-purple-500/20 text-purple-300'
                        : 'bg-blue-500/20 text-blue-300'">
                  {{ stock.type === 'MEME' ? 'Meme' : 'Normal' }}
                </span>
              </div>
              <p class="text-gray-400 text-xs">Klicken für Preischart</p>
            </div>
            <!-- Price -->
            <div class="text-right flex-shrink-0">
              <p class="text-white font-semibold">{{ formatCurrency(stock.currentPrice) }}</p>
              <p v-if="stock.priceChangePct !== null" class="text-xs font-medium"
                 :class="stock.priceChangePct >= 0 ? 'text-green-400' : 'text-red-400'">
                {{ stock.priceChangePct >= 0 ? '▲' : '▼' }}
                {{ Math.abs(stock.priceChangePct) }}%
              </p>
            </div>
          </div>

          <!-- Expanded: Chart + Buy form -->
          <div v-if="selectedStock?.id === stock.id" class="border-t border-white/5 p-3 space-y-3">
            <!-- Mini price chart -->
            <div v-if="stock.history.length >= 2" class="h-32">
              <ClientOnly>
                <LineChart :data="buildChartData(stock)" :options="chartOptions" class="h-full" />
              </ClientOnly>
            </div>
            <p v-else class="text-gray-500 text-xs">Nicht genug Preisdaten für einen Chart.</p>

            <!-- Buy form -->
            <div class="flex items-end gap-3">
              <div class="flex-1">
                <label class="block text-xs text-gray-400 mb-1">Menge kaufen</label>
                <input
                  v-model.number="buyQuantity"
                  type="number" min="0.001" step="1"
                  class="input w-full"
                  placeholder="z.B. 5"
                />
              </div>
              <div class="flex-shrink-0">
                <p class="text-xs text-gray-400 mb-1">Kosten</p>
                <p class="text-white font-semibold text-sm">
                  {{ buyQuantity > 0 ? formatCurrency(stock.currentPrice * buyQuantity) : '–' }}
                </p>
              </div>
              <button
                @click.stop="buyStock(stock)"
                :disabled="buyLoading || !buyQuantity || buyQuantity <= 0"
                class="btn-primary text-sm px-4 py-2 flex-shrink-0"
              >
                {{ buyLoading ? 'Kaufe...' : 'Kaufen' }}
              </button>
            </div>
            <p v-if="buyError" class="text-red-400 text-xs">{{ buyError }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Line as LineChart } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip)

definePageMeta({ layout: 'default' })

const api = useApi()
const toast = useToastStore()
const gameStore = useGameStore()
const { formatCurrency } = useFormatting()

interface PricePoint { price: number; turn: number }
interface Stock {
  id: number; name: string; ticker: string; type: string
  currentPrice: number; priceChangePct: number | null; history: PricePoint[]
}
interface Investment {
  id: number; type: string; name: string; stockId: number | null
  quantity: number; amountInvested: number; currentValue: number
  gainLoss: number; gainLossPct: number | null; acquiredAtTurn: number
}

const stocks = ref<Stock[]>([])
const stocksLoading = ref(false)
const portfolio = ref<Investment[]>([])
const portfolioLoading = ref(false)

const selectedStock = ref<Stock | null>(null)
const buyQuantity = ref<number>(1)
const buyLoading = ref(false)
const buyError = ref('')
const sellLoading = ref<number | null>(null)

const typeFilter = ref('ALL')
const typeFilters = [
  { value: 'ALL', label: 'Alle' },
  { value: 'NORMAL', label: 'Normal' },
  { value: 'MEME', label: 'Meme' },
]

const filteredStocks = computed(() =>
  typeFilter.value === 'ALL' ? stocks.value : stocks.value.filter(s => s.type === typeFilter.value)
)

const totalInvested = computed(() =>
  portfolio.value.reduce((s, i) => s + i.amountInvested, 0)
)
const totalCurrentValue = computed(() =>
  portfolio.value.reduce((s, i) => s + i.currentValue, 0)
)
const totalGainLoss = computed(() => totalCurrentValue.value - totalInvested.value)

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false }, tooltip: { mode: 'index' as const } },
  scales: {
    x: { ticks: { color: '#6b7280', font: { size: 10 } }, grid: { color: '#ffffff0a' } },
    y: { ticks: { color: '#6b7280', font: { size: 10 } }, grid: { color: '#ffffff0a' } },
  },
}

function buildChartData(stock: Stock) {
  return {
    labels: stock.history.map(h => `M${h.turn}`),
    datasets: [{
      data: stock.history.map(h => h.price),
      borderColor: stock.type === 'MEME' ? '#a855f7' : '#6366f1',
      backgroundColor: stock.type === 'MEME' ? 'rgba(168,85,247,0.1)' : 'rgba(99,102,241,0.1)',
      fill: true,
      tension: 0.4,
      pointRadius: 2,
    }],
  }
}

function selectStock(stock: Stock) {
  selectedStock.value = selectedStock.value?.id === stock.id ? null : stock
  buyQuantity.value = 1
  buyError.value = ''
}

async function loadStocks() {
  stocksLoading.value = true
  try {
    stocks.value = await api.get<Stock[]>('/api/stocks')
  } catch {
    toast.error('Aktien konnten nicht geladen werden')
  } finally {
    stocksLoading.value = false
  }
}

async function loadPortfolio() {
  portfolioLoading.value = true
  try {
    portfolio.value = await api.get<Investment[]>('/api/investments')
  } catch {
    toast.error('Portfolio konnte nicht geladen werden')
  } finally {
    portfolioLoading.value = false
  }
}

async function buyStock(stock: Stock) {
  buyError.value = ''
  if (!buyQuantity.value || buyQuantity.value <= 0) {
    buyError.value = 'Bitte eine gültige Menge eingeben.'
    return
  }
  buyLoading.value = true
  try {
    const inv = await api.post<Investment>('/api/investments/stocks/buy', {
      ticker: stock.ticker,
      quantity: buyQuantity.value,
    })
    portfolio.value.push(inv)
    toast.success(`${buyQuantity.value}x ${stock.ticker} gekauft!`)
    buyQuantity.value = 1
    // Refresh cash display
    await gameStore.fetchCharacter()
  } catch (e: any) {
    buyError.value = e?.data?.message ?? 'Kauf fehlgeschlagen'
  } finally {
    buyLoading.value = false
  }
}

async function sellPosition(inv: Investment) {
  sellLoading.value = inv.id
  try {
    await api.post(`/api/investments/${inv.id}/sell`)
    portfolio.value = portfolio.value.filter(i => i.id !== inv.id)
    toast.success(`${inv.name} verkauft für ${formatCurrency(inv.currentValue)}`)
    await gameStore.init()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Verkauf fehlgeschlagen')
  } finally {
    sellLoading.value = null
  }
}

onMounted(() => {
  loadStocks()
  loadPortfolio()
})
</script>
