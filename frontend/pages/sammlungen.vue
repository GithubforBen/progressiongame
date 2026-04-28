<template>
  <div class="space-y-6">
    <div class="flex items-center gap-4">
      <h2 class="text-xl font-bold text-white">Sammlungen</h2>
      <div class="flex gap-1 ml-auto">
        <button
          class="px-3 py-1.5 rounded text-xs font-medium transition-colors"
          :class="activeTab === 'meine' ? 'bg-accent text-white' : 'bg-surface-700 text-gray-400 hover:text-white'"
          @click="activeTab = 'meine'"
        >Meine Sammlungen</button>
        <button
          class="px-3 py-1.5 rounded text-xs font-medium transition-colors"
          :class="activeTab === 'vergleichen' ? 'bg-accent text-white' : 'bg-surface-700 text-gray-400 hover:text-white'"
          @click="activeTab = 'vergleichen'"
        >Vergleichen</button>
      </div>
    </div>

    <template v-if="activeTab === 'meine'">

    <!-- Travel context banner -->
    <div v-if="travelStatus" class="card py-2.5 px-4 flex items-center gap-3">
      <span class="text-xl flex-shrink-0">
        {{ travelStatus.traveling ? '✈' : travelStatus.currentCountry ? countryEmoji(travelStatus.currentCountry) : '🏠' }}
      </span>
      <div class="flex-1 min-w-0">
        <p v-if="travelStatus.traveling" class="text-blue-300 text-sm font-medium">
          Unterwegs nach {{ travelStatus.destinationCountry }} — Ankunft Monat {{ travelStatus.arriveAtTurn }}
        </p>
        <p v-else-if="travelStatus.currentCountry" class="text-green-300 text-sm font-medium">
          Du bist in {{ travelStatus.currentCountry }} — lokale Gegenstände sind verfügbar
        </p>
        <p v-else class="text-gray-400 text-sm">
          Du bist zuhause in Deutschland. Reise um exklusive Gegenstände freizuschalten.
        </p>
      </div>
      <router-link to="/reisen" class="text-xs text-accent hover:underline flex-shrink-0">Reisen →</router-link>
    </div>

    <!-- Collection grid -->
    <div class="card">
      <h3 class="text-base font-semibold text-white mb-4">Meine Sammlungen</h3>
      <div v-if="collectionsLoading" class="text-gray-500 text-sm">Lade...</div>
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3">
        <div
          v-for="col in collections"
          :key="col.name"
          class="rounded-lg border p-4 transition-colors"
          :class="col.locked
            ? 'border-white/5 bg-surface-800/40 opacity-60 cursor-not-allowed'
            : col.completed
            ? 'border-green-500/40 bg-green-500/5'
            : 'border-white/10 bg-white/3'"
          @dblclick="col.locked && goToUnlock(col.requiredCert)"
          :title="col.locked ? 'Doppelklick – Ausbildung ansehen' : ''"
        >
          <div class="flex items-start justify-between gap-2 mb-3">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <span v-if="col.locked" class="text-base">🔒</span>
                <p class="font-semibold text-sm" :class="col.locked ? 'text-gray-500' : 'text-white'">{{ col.displayName }}</p>
              </div>
              <p v-if="col.locked" class="text-xs text-gray-600 mt-0.5">
                Benötigt: {{ certLabel(col.requiredCert) }}
              </p>
              <p v-else class="text-xs text-gray-500 mt-0.5">{{ bonusLabel(col.bonusType, col.bonusValue) }}</p>
            </div>
            <span
              v-if="col.completed"
              class="text-xs px-2 py-0.5 rounded bg-green-500/20 text-green-400 font-medium flex-shrink-0"
            >
              Komplett ✓
            </span>
          </div>

          <!-- Progress bar (only for unlocked) -->
          <div v-if="!col.locked" class="space-y-1">
            <div class="flex justify-between text-xs text-gray-500">
              <span>{{ col.ownedCount }} / {{ col.itemCount }} Gegenstände</span>
              <span>{{ Math.round(col.ownedCount / col.itemCount * 100) }}%</span>
            </div>
            <div class="h-1.5 bg-surface-700 rounded-full overflow-hidden">
              <div
                class="h-full rounded-full transition-all duration-500"
                :class="col.completed ? 'bg-green-500' : 'bg-accent'"
                :style="{ width: `${col.itemCount > 0 ? (col.ownedCount / col.itemCount * 100) : 0}%` }"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Shop -->
    <div class="card">
      <div class="space-y-3 mb-4">
        <div class="flex items-center gap-3 flex-wrap">
          <h3 class="text-base font-semibold text-white flex-1">Sammelgegenstände kaufen</h3>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Suchen..."
            class="bg-surface-700 text-white text-sm rounded px-3 py-1.5 border border-white/10 focus:outline-none focus:border-accent/50 w-48"
          />
        </div>
        <div class="flex gap-1 flex-wrap">
          <button
            v-for="f in availabilityFilters"
            :key="f.value"
            class="px-2.5 py-1 rounded text-xs font-medium transition-colors"
            :class="availFilter === f.value
              ? 'bg-accent text-white'
              : 'bg-surface-700 text-gray-400 hover:text-white'"
            @click="availFilter = f.value"
          >
            {{ f.label }}
          </button>
        </div>
        <div class="flex gap-1 flex-wrap">
          <button
            v-for="f in typeFilters"
            :key="f.value"
            class="px-2.5 py-1 rounded text-xs font-medium transition-colors"
            :class="collectionFilter === f.value
              ? 'bg-accent text-white'
              : 'bg-surface-700 text-gray-400 hover:text-white'"
            @click="collectionFilter = f.value"
          >
            {{ f.label }}
          </button>
        </div>
      </div>

      <div v-if="itemsLoading" class="text-gray-500 text-sm">Lade...</div>
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3">
        <div
          v-for="item in filteredItems"
          :key="item.id"
          class="rounded-lg border p-3 transition-colors"
          :class="item.alreadyOwned
            ? 'border-green-500/30 bg-green-500/5 opacity-70'
            : !item.canBuy
            ? 'border-white/5 bg-white/2 opacity-60'
            : 'border-white/10 bg-white/3 hover:border-white/20'"
        >
          <div class="flex items-start gap-2">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap mb-1">
                <p class="text-white font-medium text-sm">{{ item.name }}</p>
                <span class="text-xs px-1.5 py-0.5 rounded" :class="rarityColor(item.rarity)">
                  {{ rarityLabel(item.rarity) }}
                </span>
              </div>
              <p class="text-gray-500 text-xs">{{ item.collectionName }}</p>
              <p v-if="item.description" class="text-gray-500 text-xs mt-0.5 line-clamp-2">{{ item.description }}</p>
              <!-- Country requirement -->
              <p v-if="item.countryRequired && item.countryRequired !== 'Deutschland'" class="text-xs mt-1 flex items-center gap-1">
                <span>{{ countryEmoji(item.countryRequired) }}</span>
                <span :class="item.canBuy || item.alreadyOwned ? 'text-gray-400' : 'text-yellow-500'">
                  {{ item.countryRequired }}
                </span>
              </p>
              <p class="text-xs mt-1.5">
                <span v-if="item.alreadyOwned" class="text-green-400">✓ Besessen</span>
                <span v-else-if="item.canBuy" class="text-yellow-400 font-medium">{{ formatCurrency(item.shopPrice ?? item.effectivePrice) }}</span>
                <span v-else-if="item.countryRequired && item.countryRequired !== 'Deutschland'" class="text-gray-600">Reise nach {{ item.countryRequired }}</span>
                <span v-else class="text-gray-600">Ausbildung erforderlich</span>
              </p>
            </div>
          </div>
          <button
            v-if="!item.alreadyOwned && item.canBuy"
            @click="buy(item)"
            :disabled="buyingId === item.id"
            class="btn-primary w-full mt-2 text-xs py-1.5"
          >
            {{ buyingId === item.id ? 'Kaufe...' : 'Kaufen' }}
          </button>
          <p v-else-if="!item.alreadyOwned && item.countryRequired && item.countryRequired !== 'Deutschland'" class="text-xs text-center text-gray-600 mt-2">
            🔒 Reise nach {{ item.countryRequired }}
          </p>
          <p v-else-if="!item.alreadyOwned && !item.canBuy" class="text-xs text-center text-gray-600 mt-2">
            🔒 Ausbildung nötig
          </p>
        </div>
      </div>
    </div>

    </template><!-- end meine -->

    <!-- Vergleichen Tab -->
    <template v-else>
      <!-- Player selector -->
      <div class="card">
        <p class="text-sm text-gray-400 mb-3">Wähle einen Spieler zum Vergleichen:</p>
        <div v-if="leaderboardLoading" class="text-gray-500 text-xs">Lade…</div>
        <div v-else-if="otherPlayers.length === 0" class="text-gray-600 text-xs">
          Noch keine anderen Spieler vorhanden.
        </div>
        <div v-else class="flex flex-wrap gap-2">
          <button
            v-for="p in otherPlayers"
            :key="p.playerId"
            class="px-3 py-1.5 rounded text-xs font-medium transition-colors"
            :class="comparePlayerId === p.playerId ? 'bg-accent text-white' : 'bg-surface-700 text-gray-400 hover:text-white'"
            @click="loadCompareCollections(p.playerId)"
          >
            #{{ p.rank }} {{ p.username }}
          </button>
        </div>
      </div>

      <!-- Side-by-side comparison -->
      <div v-if="comparePlayerId !== null && !compareLoading" class="card">
        <h3 class="text-base font-semibold text-white mb-4">
          Meine Sammlungen vs. {{ compareName }}
        </h3>
        <div class="space-y-2">
          <div
            v-for="myCol in collections"
            :key="myCol.name"
            class="grid grid-cols-2 gap-3 items-stretch"
          >
            <!-- My side -->
            <div
              class="rounded-lg border p-3"
              :class="myCol.locked ? 'border-white/5 bg-surface-800/40 opacity-50' : myCol.completed ? 'border-green-500/40 bg-green-500/5' : 'border-white/10 bg-white/3'"
            >
              <p class="text-xs font-medium text-white truncate">{{ myCol.displayName }}</p>
              <div class="mt-1 h-1.5 bg-surface-700 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full transition-all duration-500"
                  :class="myCol.completed ? 'bg-green-500' : 'bg-accent'"
                  :style="{ width: `${myCol.itemCount > 0 ? (myCol.ownedCount / myCol.itemCount * 100) : 0}%` }"
                />
              </div>
              <p class="text-xs text-gray-500 mt-1">
                {{ myCol.ownedCount }}/{{ myCol.itemCount }}
                <span v-if="myCol.completed" class="text-green-400 ml-1">✓</span>
              </p>
            </div>
            <!-- Their side -->
            <div
              class="rounded-lg border p-3"
              :class="compareMap[myCol.name]?.completed ? 'border-green-500/40 bg-green-500/5' : 'border-white/10 bg-white/3'"
            >
              <div v-if="compareMap[myCol.name]">
                <div class="mt-1 h-1.5 bg-surface-700 rounded-full overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all duration-500 bg-blue-400"
                    :style="{ width: `${compareMap[myCol.name].itemCount > 0 ? (compareMap[myCol.name].ownedCount / compareMap[myCol.name].itemCount * 100) : 0}%` }"
                  />
                </div>
                <p class="text-xs text-gray-500 mt-1">
                  {{ compareMap[myCol.name].ownedCount }}/{{ compareMap[myCol.name].itemCount }}
                  <span v-if="compareMap[myCol.name].completed" class="text-green-400 ml-1">✓</span>
                </p>
              </div>
              <p v-else class="text-xs text-gray-600">—</p>
            </div>
          </div>
        </div>
      </div>
      <div v-else-if="compareLoading" class="card text-gray-500 text-sm">Lade Vergleich…</div>
      <div v-else class="card text-gray-500 text-sm text-center py-8">
        Wähle oben einen Spieler aus.
      </div>
    </template><!-- end vergleichen -->

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'

definePageMeta({ layout: 'default' })

const api = useApi()
const toast = useToastStore()
const gameStore = useGameStore()
const { formatCurrency, certLabel } = useFormatting()
const router = useRouter()

interface CollectionRow {
  name: string; displayName: string; bonusType: string; bonusValue: number
  itemCount: number; ownedCount: number; completed: boolean
  requiredCert: string | null; locked: boolean
}
interface CollectibleItem {
  id: number; name: string; collectionName: string; collectionType: string
  countryRequired: string | null; rarity: string; baseValue: number
  effectivePrice: number; shopPrice: number | null
  description: string; canBuy: boolean; alreadyOwned: boolean; onSale: boolean
}
interface TravelStatus {
  currentCountry: string | null; destinationCountry: string | null
  arriveAtTurn: number | null; traveling: boolean; visitedCountries: string[]
}

// ── Tabs ───────────────────────────────────────────────────────────────────────
const activeTab = ref<'meine' | 'vergleichen'>('meine')

interface PublicCollection {
  name: string; displayName: string; itemCount: number; ownedCount: number; completed: boolean
}
interface LeaderboardPlayer {
  playerId: number; username: string; rank: number; isMe: boolean
}

const leaderboardPlayers = ref<LeaderboardPlayer[]>([])
const leaderboardLoading = ref(false)
const comparePlayerId = ref<number | null>(null)
const compareCollections = ref<PublicCollection[]>([])
const compareLoading = ref(false)

const otherPlayers = computed(() => (Array.isArray(leaderboardPlayers.value) ? leaderboardPlayers.value : []).filter(p => !p.isMe))
const compareName = computed(() =>
  (Array.isArray(leaderboardPlayers.value) ? leaderboardPlayers.value : []).find(p => p.playerId === comparePlayerId.value)?.username ?? ''
)
const compareMap = computed<Record<string, PublicCollection>>(() =>
  Object.fromEntries((Array.isArray(compareCollections.value) ? compareCollections.value : []).map(c => [c.name, c]))
)

async function loadCompareCollections(playerId: number) {
  comparePlayerId.value = playerId
  compareLoading.value = true
  try {
    const result = await api.get<PublicCollection[]>(`/api/leaderboard/player/${playerId}/collections`)
    compareCollections.value = Array.isArray(result) ? result : []
  } catch {
    toast.error('Vergleich konnte nicht geladen werden')
  } finally {
    compareLoading.value = false
  }
}

watch(activeTab, async (tab) => {
  if (tab === 'vergleichen' && leaderboardPlayers.value.length === 0) {
    leaderboardLoading.value = true
    try {
      const result = await api.get<LeaderboardPlayer[]>('/api/leaderboard')
      leaderboardPlayers.value = Array.isArray(result) ? result : []
    } finally {
      leaderboardLoading.value = false
    }
  }
})

// ── Collections + Shop ─────────────────────────────────────────────────────────
const collections = ref<CollectionRow[]>([])
const items = ref<CollectibleItem[]>([])
const travelStatus = ref<TravelStatus | null>(null)
const collectionsLoading = ref(false)
const itemsLoading = ref(false)
const buyingId = ref<number | null>(null)
const collectionFilter = ref('ALL')
const availFilter = ref('ALL')
const searchQuery = ref('')

const availabilityFilters = [
  { value: 'ALL', label: 'Alle' },
  { value: 'AVAILABLE', label: 'Verfügbar' },
  { value: 'TRAVEL', label: 'Reise nötig' },
  { value: 'OWNED', label: 'Besessen' },
]

const typeFilters = computed(() => [
  { value: 'ALL', label: 'Alle Sammlungen' },
  ...Array.from(new Set(items.value.map(i => i.collectionName))).sort().map(n => ({ value: n, label: n ?? '' }))
])

const filteredItems = computed(() => {
  let list = items.value
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(i => i.name.toLowerCase().includes(q) || i.collectionName.toLowerCase().includes(q))
  }
  if (collectionFilter.value !== 'ALL')
    list = list.filter(i => i.collectionName === collectionFilter.value)
  if (availFilter.value === 'AVAILABLE')
    list = list.filter(i => i.canBuy && !i.alreadyOwned)
  else if (availFilter.value === 'TRAVEL')
    list = list.filter(i => !i.canBuy && !i.alreadyOwned)
  else if (availFilter.value === 'OWNED')
    list = list.filter(i => i.alreadyOwned)
  return list
})

async function loadData() {
  collectionsLoading.value = true
  itemsLoading.value = true
  try {
    const [cols, its, travel] = await Promise.all([
      api.get<CollectionRow[]>('/api/collections'),
      api.get<CollectibleItem[]>('/api/collections/items'),
      api.get<TravelStatus>('/api/travel/status'),
    ])
    collections.value = Array.isArray(cols) ? cols : []
    items.value = Array.isArray(its) ? its : []
    travelStatus.value = travel
  } catch {
    toast.error('Daten konnten nicht geladen werden')
  } finally {
    collectionsLoading.value = false
    itemsLoading.value = false
  }
}

async function buy(item: CollectibleItem) {
  buyingId.value = item.id
  try {
    await api.post(`/api/collections/items/${item.id}/buy`)
    const idx = items.value.findIndex(i => i.id === item.id)
    if (idx !== -1) items.value[idx] = { ...items.value[idx], alreadyOwned: true, canBuy: false }
    const refreshed = await api.get<CollectionRow[]>('/api/collections')
    collections.value = Array.isArray(refreshed) ? refreshed : []
    toast.success(`${item.name} gekauft!`)
    await gameStore.fetchCharacter()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Kauf fehlgeschlagen')
  } finally {
    buyingId.value = null
  }
}

function bonusLabel(type: string, value: number): string {
  switch (type) {
    case 'MONTHLY_INCOME_BONUS': return `+${formatCurrency(value)}/Monat`
    case 'SALARY_MULTIPLIER': return `+${Math.round(value * 100)}% Gehalt`
    case 'EXPENSE_REDUCTION': return `-${Math.round(value * 100)}% Ausgaben`
    case 'HAPPINESS_BONUS': return `+${value} Glück/Monat`
    case 'SCHUFA_BONUS': return `+${value} SCHUFA/Monat`
    default: return type
  }
}

function rarityLabel(r: string): string {
  return { COMMON: 'Gewöhnlich', UNCOMMON: 'Ungewöhnlich', RARE: 'Selten', EPIC: 'Episch', LEGENDARY: 'Legendär' }[r] ?? r
}

function rarityColor(rarity: string): string {
  const map: Record<string, string> = {
    COMMON: 'bg-gray-500/20 text-gray-400',
    UNCOMMON: 'bg-green-500/20 text-green-400',
    RARE: 'bg-blue-500/20 text-blue-400',
    EPIC: 'bg-purple-500/20 text-purple-400',
    LEGENDARY: 'bg-yellow-500/20 text-yellow-400',
  }
  return map[rarity] ?? 'bg-gray-500/20 text-gray-400'
}

function countryEmoji(name: string): string {
  const map: Record<string, string> = {
    Japan: '🇯🇵', Italien: '🇮🇹', USA: '🇺🇸', Schweiz: '🇨🇭', UK: '🇬🇧', China: '🇨🇳',
    Deutschland: '🇩🇪', Frankreich: '🇫🇷', Spanien: '🇪🇸', Australien: '🇦🇺',
  }
  return map[name] ?? '🌍'
}

function goToUnlock(cert: string | null) {
  if (cert) router.push(`/ausbildung?highlight=${cert}`)
}

onMounted(loadData)
</script>
