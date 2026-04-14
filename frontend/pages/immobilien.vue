<template>
  <div class="space-y-6">
    <h2 class="text-xl font-bold text-white">Immobilien</h2>

    <!-- My Properties -->
    <div v-if="myProperties.length > 0" class="card">
      <h3 class="text-base font-semibold text-white mb-4">Meine Immobilien</h3>
      <div class="space-y-3">
        <div
          v-for="prop in myProperties"
          :key="prop.id"
          class="flex items-start gap-4 p-4 rounded-lg bg-white/5 border border-white/10"
        >
          <div class="text-2xl flex-shrink-0">{{ categoryIcon(prop.category) }}</div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <p class="text-white font-semibold">{{ prop.name }}</p>
              <span
                class="text-xs px-2 py-0.5 rounded font-medium"
                :class="prop.mode === 'SELF_OCCUPIED'
                  ? 'bg-green-500/20 text-green-400'
                  : 'bg-blue-500/20 text-blue-400'"
              >
                {{ prop.mode === 'SELF_OCCUPIED' ? 'Selbst bewohnt' : 'Vermietet' }}
              </span>
            </div>
            <p class="text-gray-400 text-xs mt-0.5">{{ prop.location }} · Kaufpreis: {{ formatCurrency(prop.purchasePrice) }}</p>
            <p class="text-gray-400 text-xs">
              <span v-if="prop.mode === 'RENTED_OUT'">
                Mieteinnahmen: <span class="text-green-400">{{ formatCurrency(prop.monthlyRent) }}/Monat</span>
              </span>
              <span v-else>
                Mietersparnis: <span class="text-green-400">{{ formatCurrency(prop.rentSavings) }}/Monat</span>
              </span>
            </p>
          </div>
          <div class="flex gap-2 flex-shrink-0">
            <button
              v-if="prop.mode !== 'SELF_OCCUPIED'"
              @click="changeMode(prop.id, 'SELF_OCCUPIED')"
              :disabled="actionId === prop.id"
              class="btn-secondary text-xs px-3 py-1.5"
            >
              Einziehen
            </button>
            <button
              v-if="prop.mode !== 'RENTED_OUT'"
              @click="changeMode(prop.id, 'RENTED_OUT')"
              :disabled="actionId === prop.id"
              class="btn-secondary text-xs px-3 py-1.5"
            >
              Vermieten
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Catalog -->
    <div class="card">
      <h3 class="text-base font-semibold text-white mb-4">Immobilien-Katalog</h3>
      <div v-if="loading" class="text-gray-500 text-sm">Lade...</div>
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div
          v-for="item in catalog"
          :key="item.id"
          class="rounded-lg border p-4 transition-colors"
          :class="item.owned
            ? 'border-green-500/30 bg-green-500/5 opacity-70'
            : 'border-white/10 bg-white/3 hover:border-white/20'"
        >
          <div class="flex items-start gap-3">
            <div class="text-3xl flex-shrink-0">{{ categoryIcon(item.category) }}</div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap mb-1">
                <p class="text-white font-semibold text-sm">{{ item.name }}</p>
                <span v-if="item.owned" class="text-xs px-1.5 py-0.5 rounded bg-green-500/20 text-green-400">Besessen</span>
                <span class="text-xs px-1.5 py-0.5 rounded bg-white/10 text-gray-400">{{ item.category }}</span>
              </div>
              <p class="text-gray-400 text-xs">{{ item.location }}</p>
              <p v-if="item.description" class="text-gray-500 text-xs mt-1">{{ item.description }}</p>
              <div class="mt-2 space-y-0.5 text-xs text-gray-400">
                <p>Kaufpreis: <span class="text-white font-medium">{{ formatCurrency(item.purchasePrice) }}</span></p>
                <p>Mieteinnahmen: <span class="text-green-400">{{ formatCurrency(item.monthlyRent) }}/Monat</span></p>
                <p>Mietersparnis: <span class="text-blue-400">{{ formatCurrency(item.rentSavings) }}/Monat</span></p>
              </div>
            </div>
          </div>
          <button
            v-if="!item.owned"
            @click="buy(item)"
            :disabled="buyingId === item.id"
            class="btn-primary w-full mt-3 text-sm py-1.5"
          >
            {{ buyingId === item.id ? 'Kaufe...' : 'Kaufen' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

definePageMeta({ layout: 'default' })

const api = useApi()
const toast = useToastStore()
const gameStore = useGameStore()
const { formatCurrency } = useFormatting()

interface CatalogItem {
  id: number; name: string; location: string; category: string; description: string
  purchasePrice: number; monthlyRent: number; rentSavings: number; owned: boolean
}
interface PlayerProperty {
  id: number; catalogId: number; name: string; location: string; category: string
  description: string; mode: string; purchasedAtTurn: number
  purchasePrice: number; monthlyRent: number; rentSavings: number
}

const catalog = ref<CatalogItem[]>([])
const myProperties = ref<PlayerProperty[]>([])
const loading = ref(false)
const buyingId = ref<number | null>(null)
const actionId = ref<number | null>(null)

async function loadAll() {
  loading.value = true
  try {
    const [cat, my] = await Promise.all([
      api.get<CatalogItem[]>('/api/real-estate'),
      api.get<PlayerProperty[]>('/api/real-estate/my'),
    ])
    catalog.value = cat
    myProperties.value = my
  } catch {
    toast.error('Daten konnten nicht geladen werden')
  } finally {
    loading.value = false
  }
}

async function buy(item: CatalogItem) {
  buyingId.value = item.id
  try {
    const owned = await api.post<PlayerProperty>(`/api/real-estate/${item.id}/buy`)
    myProperties.value.push(owned)
    const idx = catalog.value.findIndex(c => c.id === item.id)
    if (idx !== -1) catalog.value[idx] = { ...catalog.value[idx], owned: true }
    toast.success(`${item.name} gekauft!`)
    await gameStore.fetchCharacter()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Kauf fehlgeschlagen')
  } finally {
    buyingId.value = null
  }
}

async function changeMode(propertyId: number, mode: string) {
  actionId.value = propertyId
  try {
    const updated = await api.patch<PlayerProperty>(`/api/real-estate/${propertyId}/mode`, { mode })
    const idx = myProperties.value.findIndex(p => p.id === propertyId)
    if (idx !== -1) myProperties.value[idx] = updated
    toast.success(mode === 'SELF_OCCUPIED' ? 'Du wohnst jetzt selbst dort.' : 'Immobilie wird jetzt vermietet.')
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler beim Moduswechsel')
  } finally {
    actionId.value = null
  }
}

function categoryIcon(category: string): string {
  return { WOHNUNG: '🏢', HAUS: '🏡', GEWERBE: '🏪' }[category] ?? '🏠'
}

onMounted(loadAll)
</script>
