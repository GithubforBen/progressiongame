<template>
  <div class="space-y-6">
    <h2 class="text-xl font-bold text-white">Reisen & Sammlerstücke</h2>

    <!-- Active Events Banner -->
    <div v-if="activeEvents.length > 0" class="space-y-2">
      <div
        v-for="event in activeEvents"
        :key="event.id"
        class="card border border-yellow-500/40 bg-yellow-500/5 flex items-start gap-3 p-3"
      >
        <span class="text-yellow-400 text-lg flex-shrink-0">⭐</span>
        <div>
          <p class="text-yellow-300 font-semibold text-sm">Tages-Event</p>
          <p class="text-yellow-200/80 text-sm">{{ event.message }}</p>
        </div>
      </div>
    </div>

    <!-- Travel Status -->
    <div class="card">
      <h3 class="text-base font-semibold text-white mb-3">Aktueller Aufenthaltsort</h3>
      <div v-if="statusLoading" class="text-gray-500 text-sm">Lade...</div>
      <div v-else-if="status">
        <!-- Traveling -->
        <div v-if="status.traveling" class="flex items-center gap-3 p-3 bg-blue-500/10 rounded-lg border border-blue-500/30">
          <span class="text-2xl">✈</span>
          <div class="flex-1">
            <p class="text-blue-300 font-semibold">Unterwegs nach {{ status.destinationCountry }}</p>
            <p class="text-blue-200/70 text-sm">Ankunft in Monat {{ status.arriveAtTurn }}</p>
          </div>
        </div>
        <!-- Abroad -->
        <div v-else-if="status.currentCountry" class="flex items-center justify-between p-3 bg-green-500/10 rounded-lg border border-green-500/30">
          <div class="flex items-center gap-3">
            <span class="text-2xl">{{ countryEmoji(status.currentCountry) }}</span>
            <div>
              <p class="text-green-300 font-semibold">Du bist in {{ status.currentCountry }}</p>
              <p class="text-green-200/70 text-sm">Lokale Sammlerstücke verfügbar</p>
            </div>
          </div>
          <button @click="returnHome" :disabled="actionLoading" class="btn-secondary text-sm">
            Nach Hause
          </button>
        </div>
        <!-- Home -->
        <div v-else class="flex items-center gap-3 p-3 bg-white/5 rounded-lg">
          <span class="text-2xl">🏠</span>
          <p class="text-gray-300">Du bist zuhause in Deutschland.</p>
        </div>

        <!-- Visited -->
        <div v-if="status.visitedCountries?.length" class="mt-3">
          <p class="text-xs text-gray-400 mb-2">Bereits bereist:</p>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="c in status.visitedCountries"
              :key="c"
              class="text-xs px-2 py-1 rounded bg-white/5 text-gray-300"
            >
              {{ countryEmoji(c) }} {{ c }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Countries -->
    <div class="card">
      <h3 class="text-base font-semibold text-white mb-4">Reiseziele</h3>
      <div v-if="countriesLoading" class="text-gray-500 text-sm">Lade...</div>
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div
          v-for="country in countries"
          :key="country.id"
          class="rounded-lg border p-4 transition-colors"
          :class="country.currentlyHere
            ? 'border-green-500/40 bg-green-500/5'
            : country.travelingHere
              ? 'border-blue-500/40 bg-blue-500/5'
              : 'border-white/10 bg-white/3'"
        >
          <div class="flex items-start gap-3">
            <span class="text-3xl">{{ country.emoji }}</span>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <p class="text-white font-semibold">{{ country.name }}</p>
                <span v-if="country.currentlyHere" class="text-xs px-1.5 py-0.5 rounded bg-green-500/20 text-green-400">Hier</span>
                <span v-if="country.travelingHere" class="text-xs px-1.5 py-0.5 rounded bg-blue-500/20 text-blue-400">Unterwegs</span>
                <span v-if="country.visited && !country.currentlyHere" class="text-xs px-1.5 py-0.5 rounded bg-white/10 text-gray-400">Besucht</span>
              </div>
              <p class="text-gray-400 text-xs mt-0.5">{{ country.description }}</p>
              <div class="flex items-center gap-3 mt-2 text-xs text-gray-400">
                <span>✈ {{ country.travelMonths }} Monat{{ country.travelMonths > 1 ? 'e' : '' }}</span>
                <span>{{ formatCurrency(country.travelCost) }}</span>
              </div>
            </div>
          </div>
          <button
            v-if="!country.currentlyHere && !country.travelingHere && !status?.traveling"
            @click="depart(country.name)"
            :disabled="actionLoading"
            class="btn-primary w-full mt-3 text-sm py-1.5"
          >
            {{ actionLoading === country.name ? 'Buche...' : 'Reise buchen' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Collectibles -->
    <div class="card">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-base font-semibold text-white">Sammlerstücke</h3>
        <div class="flex gap-1">
          <button
            v-for="f in collectibleFilters"
            :key="f.value"
            class="px-2.5 py-1 rounded text-xs font-medium transition-colors"
            :class="collectibleFilter === f.value
              ? 'bg-accent text-white'
              : 'bg-surface-700 text-gray-400 hover:text-white'"
            @click="collectibleFilter = f.value"
          >{{ f.label }}</button>
        </div>
      </div>

      <div v-if="collectiblesLoading" class="text-gray-500 text-sm">Lade...</div>
      <div v-else class="space-y-2">
        <div
          v-for="item in filteredCollectibles"
          :key="item.id"
          class="flex items-center gap-3 p-3 rounded-lg"
          :class="item.alreadyOwned
            ? 'bg-white/3 opacity-60'
            : item.onSale
              ? 'bg-yellow-500/5 border border-yellow-500/20'
              : 'bg-white/5'"
        >
          <div class="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 text-lg"
               :class="rarityBg(item.rarity)">
            {{ rarityIcon(item.rarity) }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <p class="text-white font-medium text-sm">{{ item.name }}</p>
              <span class="text-xs px-1.5 py-0.5 rounded" :class="rarityBadge(item.rarity)">
                {{ rarityLabel(item.rarity) }}
              </span>
              <span v-if="item.onSale" class="text-xs px-1.5 py-0.5 rounded bg-yellow-500/20 text-yellow-300">
                -30% Sale
              </span>
            </div>
            <p class="text-gray-400 text-xs">{{ item.countryRequired }} · {{ item.collectionType }}</p>
          </div>
          <div class="text-right flex-shrink-0">
            <p class="text-white font-semibold text-sm">{{ formatCurrency(item.effectivePrice) }}</p>
            <p v-if="item.onSale" class="text-gray-500 text-xs line-through">{{ formatCurrency(item.baseValue) }}</p>
          </div>
          <div class="flex-shrink-0">
            <span v-if="item.alreadyOwned" class="text-xs px-2 py-1 rounded bg-green-500/10 text-green-400">Besessen</span>
            <button
              v-else-if="item.canBuy"
              @click="buyCollectible(item)"
              :disabled="buyingId === item.id"
              class="btn-primary text-xs px-3 py-1.5"
            >
              {{ buyingId === item.id ? '...' : 'Kaufen' }}
            </button>
            <span v-else class="text-xs text-gray-600">{{ item.countryRequired }} nötig</span>
          </div>
        </div>
      </div>
    </div>

    <!-- My Collection -->
    <div v-if="myCollection.length > 0" class="card">
      <h3 class="text-base font-semibold text-white mb-4">Meine Sammlung ({{ myCollection.length }})</h3>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
        <div
          v-for="item in myCollection"
          :key="item.collectibleId"
          class="flex items-center gap-3 p-3 rounded-lg bg-white/5"
        >
          <div class="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 text-sm"
               :class="rarityBg(item.rarity)">
            {{ rarityIcon(item.rarity) }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-white text-sm font-medium truncate">{{ item.name }}</p>
            <p class="text-gray-400 text-xs">{{ item.countryRequired }} · Gekauft {{ formatCurrency(item.purchasePrice) }}</p>
          </div>
          <span class="text-xs px-1.5 py-0.5 rounded flex-shrink-0" :class="rarityBadge(item.rarity)">
            {{ rarityLabel(item.rarity) }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

definePageMeta({ layout: 'default' })

const api = useApi()
const toast = useToastStore()
const gameStore = useGameStore()
const { formatCurrency } = useFormatting()

interface TravelStatus {
  currentCountry: string | null
  destinationCountry: string | null
  arriveAtTurn: number | null
  traveling: boolean
  visitedCountries: string[]
}
interface Country {
  id: number; name: string; travelCost: number; travelMonths: number
  emoji: string; description: string; visited: boolean; currentlyHere: boolean; travelingHere: boolean
}
interface Collectible {
  id: number; name: string; collectionType: string; countryRequired: string
  rarity: string; baseValue: number; effectivePrice: number; description: string
  canBuy: boolean; alreadyOwned: boolean; onSale: boolean
}
interface OwnedCollectible {
  collectibleId: number; name: string; collectionType: string; countryRequired: string
  rarity: string; baseValue: number; purchasePrice: number; acquiredAtTurn: number
}
interface ActiveEvent {
  id: number; type: string; country: string; expiresAtTurn: number
  collectibleId: number | null; message: string
}

const status = ref<TravelStatus | null>(null)
const countries = ref<Country[]>([])
const collectibles = ref<Collectible[]>([])
const myCollection = ref<OwnedCollectible[]>([])
const activeEvents = ref<ActiveEvent[]>([])

const statusLoading = ref(false)
const countriesLoading = ref(false)
const collectiblesLoading = ref(false)
const actionLoading = ref<string | false>(false)
const buyingId = ref<number | null>(null)

const collectibleFilter = ref('ALL')
const collectibleFilters = [
  { value: 'ALL', label: 'Alle' },
  { value: 'AVAILABLE', label: 'Kaufbar' },
  { value: 'OWNED', label: 'Besessen' },
]

const filteredCollectibles = computed(() => {
  if (collectibleFilter.value === 'AVAILABLE') return collectibles.value.filter(c => c.canBuy)
  if (collectibleFilter.value === 'OWNED') return collectibles.value.filter(c => c.alreadyOwned)
  return collectibles.value
})

async function loadAll() {
  statusLoading.value = true
  countriesLoading.value = true
  collectiblesLoading.value = true
  try {
    const [s, c, col, my, ev] = await Promise.all([
      api.get<TravelStatus>('/api/travel/status'),
      api.get<Country[]>('/api/travel/countries'),
      api.get<Collectible[]>('/api/collectibles'),
      api.get<OwnedCollectible[]>('/api/collectibles/my'),
      api.get<ActiveEvent[]>('/api/collectibles/events'),
    ])
    status.value = s
    countries.value = c
    collectibles.value = col
    myCollection.value = my
    activeEvents.value = ev
  } catch {
    toast.error('Daten konnten nicht geladen werden')
  } finally {
    statusLoading.value = false
    countriesLoading.value = false
    collectiblesLoading.value = false
  }
}

async function depart(countryName: string) {
  actionLoading.value = countryName
  try {
    status.value = await api.post<TravelStatus>('/api/travel/depart', { countryName })
    countries.value = await api.get<Country[]>('/api/travel/countries')
    toast.success(`Flug nach ${countryName} gebucht!`)
    await gameStore.fetchCharacter()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Buchung fehlgeschlagen')
  } finally {
    actionLoading.value = false
  }
}

async function returnHome() {
  actionLoading.value = 'home'
  try {
    status.value = await api.post<TravelStatus>('/api/travel/return-home')
    countries.value = await api.get<Country[]>('/api/travel/countries')
    toast.success('Zurück zuhause in Deutschland.')
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler')
  } finally {
    actionLoading.value = false
  }
}

async function buyCollectible(item: Collectible) {
  buyingId.value = item.id
  try {
    const owned = await api.post<OwnedCollectible>(`/api/collectibles/${item.id}/buy`)
    myCollection.value.push(owned)
    // Update canBuy + alreadyOwned in list
    const idx = collectibles.value.findIndex(c => c.id === item.id)
    if (idx !== -1) {
      collectibles.value[idx] = { ...collectibles.value[idx], alreadyOwned: true, canBuy: false }
    }
    toast.success(`${item.name} gekauft!`)
    await gameStore.fetchCharacter()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Kauf fehlgeschlagen')
  } finally {
    buyingId.value = null
  }
}

function countryEmoji(name: string): string {
  const map: Record<string, string> = {
    Japan: '🇯🇵', Italien: '🇮🇹', USA: '🇺🇸', Schweiz: '🇨🇭', UK: '🇬🇧', China: '🇨🇳',
  }
  return map[name] ?? '🌍'
}
function rarityIcon(r: string): string {
  return { COMMON: '⚪', RARE: '🔵', EPIC: '🟣', LEGENDARY: '🟡' }[r] ?? '⚪'
}
function rarityLabel(r: string): string {
  return { COMMON: 'Gewöhnlich', RARE: 'Selten', EPIC: 'Episch', LEGENDARY: 'Legendär' }[r] ?? r
}
function rarityBg(r: string): string {
  return { COMMON: 'bg-gray-500/20', RARE: 'bg-blue-500/20', EPIC: 'bg-purple-500/20', LEGENDARY: 'bg-yellow-500/20' }[r] ?? 'bg-gray-500/20'
}
function rarityBadge(r: string): string {
  return {
    COMMON: 'bg-gray-500/20 text-gray-400',
    RARE: 'bg-blue-500/20 text-blue-400',
    EPIC: 'bg-purple-500/20 text-purple-400',
    LEGENDARY: 'bg-yellow-500/20 text-yellow-400',
  }[r] ?? 'bg-gray-500/20 text-gray-400'
}

onMounted(loadAll)
</script>
