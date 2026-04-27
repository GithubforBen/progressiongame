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
      <div class="space-y-3 mb-4">
        <div class="flex items-center gap-3 flex-wrap">
          <h3 class="text-base font-semibold text-white flex-1">Börse</h3>
          <input
            v-model="stockSearch"
            type="text"
            placeholder="Suchen (Name, Ticker)..."
            class="bg-surface-700 text-white text-sm rounded px-3 py-1.5 border border-white/10 focus:outline-none focus:border-accent/50 w-52"
          />
        </div>
        <div class="flex gap-1 flex-wrap">
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
        <label class="flex items-center gap-2 cursor-pointer w-fit">
          <input type="checkbox" v-model="showLocked" class="accent-accent" />
          <span class="text-xs text-gray-400">Gesperrte anzeigen</span>
        </label>
      </div>

      <div v-if="stocksLoading" class="text-gray-500 text-sm">Lade...</div>
      <div v-else class="space-y-2">
        <div
          v-for="stock in filteredStocks"
          :key="stock.id"
          class="rounded-lg border transition-colors"
          :class="stock.locked
            ? 'border-white/5 bg-surface-800/50 opacity-60 cursor-not-allowed'
            : selectedStock?.id === stock.id
            ? 'border-accent/50 bg-accent/5 cursor-pointer'
            : 'border-white/5 bg-white/3 hover:bg-white/5 cursor-pointer'"
          @click="!stock.locked && selectStock(stock)"
          @dblclick="stock.locked && goToUnlock(stock.requiredCert)"
          :title="stock.locked ? 'Doppelklick – Ausbildung ansehen' : ''"
        >
          <div class="flex items-center gap-3 p-3">
            <!-- Ticker badge -->
            <div class="w-12 h-12 rounded-lg flex items-center justify-center flex-shrink-0"
                 :class="stock.locked ? 'bg-surface-700' : stockTypeColor(stock.type).split(' ')[0]">
              <span v-if="stock.locked" class="text-lg">🔒</span>
              <span v-else class="text-xs font-bold" :class="stockTypeColor(stock.type).split(' ')[1]">
                {{ stock.ticker }}
              </span>
            </div>
            <!-- Info -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <p class="font-medium text-sm" :class="stock.locked ? 'text-gray-500' : 'text-white'">{{ stock.name }}</p>
                <span class="text-xs px-1.5 py-0.5 rounded" :class="stock.locked ? 'bg-surface-700 text-gray-600' : stockTypeColor(stock.type)">
                  {{ stockTypeLabel(stock.type) }}
                </span>
              </div>
              <p v-if="stock.locked" class="text-gray-600 text-xs">
                Benötigt: {{ certLabel(stock.requiredCert) }} · Doppelklick für Ausbildung
              </p>
              <p v-else class="text-gray-500 text-xs">Klicken für Preischart</p>
            </div>
            <!-- Price -->
            <div class="text-right flex-shrink-0">
              <p class="font-semibold" :class="stock.locked ? 'text-gray-600' : 'text-white'">{{ formatCurrency(stock.currentPrice) }}</p>
              <p v-if="!stock.locked && stock.priceChangePct !== null" class="text-xs font-medium"
                 :class="stock.priceChangePct >= 0 ? 'text-green-400' : 'text-red-400'">
                {{ stock.priceChangePct >= 0 ? '▲' : '▼' }}
                {{ Math.abs(stock.priceChangePct) }}%
              </p>
            </div>
          </div>

          <!-- Expanded: Chart + Buy form (only for unlocked) -->
          <div v-if="!stock.locked && selectedStock?.id === stock.id" class="border-t border-white/5 p-3 space-y-3">
            <!-- Mini price chart -->
            <div v-if="(stock.history?.length ?? 0) >= 2" class="h-32">
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
const { formatCurrency, certLabel } = useFormatting()
const router = useRouter()

interface PricePoint { price: number; turn: number }
interface Stock {
  id: number; name: string; ticker: string; type: string
  currentPrice: number; priceChangePct: number | null; history: PricePoint[]
  requiredCert: string | null; locked: boolean
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
const stockSearch = ref('')
const showLocked = ref(true)
const typeFilters = [
  { value: 'ALL',           label: 'Alle' },
  { value: 'NORMAL',        label: 'Normal' },
  { value: 'MEME',          label: 'Meme' },
  { value: 'ETF',           label: 'ETF' },
  { value: 'DIVIDEND_STOCK',label: 'Dividende' },
  { value: 'BOND',          label: 'Anleihe' },
  { value: 'REIT',          label: 'REIT' },
  { value: 'CRYPTO',        label: 'Krypto' },
  { value: 'LEVERAGE',      label: 'Hebel' },
  { value: 'SHORT',         label: 'Short' },
]

const filteredStocks = computed(() => {
  let list = stocks.value
  if (!showLocked.value) list = list.filter(s => !s.locked)
  if (typeFilter.value !== 'ALL') list = list.filter(s => s.type === typeFilter.value)
  if (stockSearch.value.trim()) {
    const q = stockSearch.value.toLowerCase()
    list = list.filter(s => s.name.toLowerCase().includes(q) || s.ticker.toLowerCase().includes(q))
  }
  return list
})

function stockTypeLabel(type: string): string {
  const map: Record<string, string> = {
    NORMAL: 'Normal', MEME: 'Meme', ETF: 'ETF', DIVIDEND_STOCK: 'Dividende',
    BOND: 'Anleihe', REIT: 'REIT', CRYPTO: 'Krypto',
    LEVERAGE: 'Hebel', WARRANT: 'Optionsschein', SHORT: 'Short', FUTURES: 'Futures',
  }
  return map[type] ?? type
}

function stockTypeColor(type: string): string {
  const map: Record<string, string> = {
    NORMAL: 'bg-blue-500/20 text-blue-300',
    MEME: 'bg-purple-500/20 text-purple-300',
    ETF: 'bg-green-500/20 text-green-300',
    DIVIDEND_STOCK: 'bg-emerald-500/20 text-emerald-300',
    BOND: 'bg-sky-500/20 text-sky-300',
    REIT: 'bg-teal-500/20 text-teal-300',
    CRYPTO: 'bg-orange-500/20 text-orange-300',
    LEVERAGE: 'bg-red-500/20 text-red-300',
    WARRANT: 'bg-pink-500/20 text-pink-300',
    SHORT: 'bg-rose-500/20 text-rose-300',
    FUTURES: 'bg-yellow-500/20 text-yellow-300',
  }
  return map[type] ?? 'bg-gray-500/20 text-gray-300'
}

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
  const history = stock.history ?? []
  return {
    labels: history.map(h => `M${h.turn}`),
    datasets: [{
      data: history.map(h => h.price),
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

function goToUnlock(cert: string | null) {
  if (cert) router.push(`/ausbildung?highlight=${cert}`)
}

async function loadStocks() {
  stocksLoading.value = true
  try {
    const result = await api.get<Stock[]>('/api/stocks')
    stocks.value = Array.isArray(result) ? result : []
  } catch {
    toast.error('Aktien konnten nicht geladen werden')
  } finally {
    stocksLoading.value = false
  }
}

async function loadPortfolio() {
  portfolioLoading.value = true
  try {
    const result = await api.get<Investment[]>('/api/investments')
    portfolio.value = Array.isArray(result) ? result : []
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
    // Refresh price before submitting to avoid stale-price errors
    const freshStocks = await api.get<Stock[]>('/api/stocks')
    const fresh = freshStocks.find(s => s.id === stock.id)
    if (fresh && fresh.currentPrice !== stock.currentPrice) {
      const idx = stocks.value.findIndex(s => s.id === stock.id)
      if (idx !== -1) stocks.value[idx].currentPrice = fresh.currentPrice
      if (selectedStock.value?.id === stock.id) selectedStock.value.currentPrice = fresh.currentPrice
      buyError.value = `Preis aktualisiert auf ${formatCurrency(fresh.currentPrice)}. Bitte bestätigen.`
      buyLoading.value = false
      return
    }

    const inv = await api.post<Investment>('/api/investments/stocks/buy', {
      ticker: stock.ticker,
      quantity: buyQuantity.value,
    })
    portfolio.value.push(inv)
    toast.success(`${buyQuantity.value}x ${stock.ticker} gekauft!`)
    buyQuantity.value = 1
    await gameStore.fetchCharacter()
  } catch (e: any) {
    buyError.value = e?.data?.message ?? 'Kauf fehlgeschlagen'
  } finally {
    buyLoading.value = false
  }
}

interface SellResult {
  ticker: string
  proceeds: number
  costBasis: number
  grossProfit: number
  taxPaid: number
  netProceeds: number
}

async function sellPosition(inv: Investment) {
  sellLoading.value = inv.id
  try {
    const result = await api.post<SellResult>(`/api/investments/${inv.id}/sell`)
    portfolio.value = portfolio.value.filter(i => i.id !== inv.id)
    if (result.taxPaid > 0) {
      toast.success(
        `${inv.name} verkauft · Gewinn: ${formatCurrency(result.grossProfit)} · Steuer (25%): ${formatCurrency(result.taxPaid)} · Netto: ${formatCurrency(result.netProceeds)}`
      )
    } else {
      toast.success(`${inv.name} verkauft für ${formatCurrency(result.netProceeds)}`)
    }
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
