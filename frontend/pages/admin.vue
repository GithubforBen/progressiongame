<template>
  <div class="space-y-6">
    <div class="flex items-center gap-3">
      <h2 class="text-xl font-bold text-white">Admin</h2>
      <span class="text-xs px-2 py-0.5 rounded-full bg-red-900/50 text-red-400 border border-red-800">Restricted</span>
    </div>

    <!-- Still resolving admin status -->
    <div v-if="checking" class="card text-center py-12">
      <p class="text-gray-500 text-sm">Prüfe Berechtigung…</p>
    </div>

    <!-- Access denied -->
    <div v-else-if="!authStore.isAdmin" class="card text-center py-12">
      <p class="text-red-400 font-semibold">Kein Zugriff</p>
      <p class="text-gray-500 text-sm mt-1">Du bist kein Administrator.</p>
    </div>

    <template v-else-if="authStore.isAdmin">
      <!-- Player list -->
      <div class="card">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-base font-semibold text-white">Spieler ({{ players.length }})</h3>
          <button class="btn-secondary text-xs" @click="load">Aktualisieren</button>
        </div>

        <div v-if="loading" class="text-gray-500 text-sm">Lade...</div>
        <div v-else class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-gray-500 text-xs border-b border-surface-700">
                <th class="text-left pb-2 pr-4">Spieler</th>
                <th class="text-right pb-2 pr-4">Cash</th>
                <th class="text-right pb-2 pr-4">Vermögen</th>
                <th class="text-right pb-2 pr-4">Monat</th>
                <th class="text-right pb-2 pr-4">SCHUFA</th>
                <th class="text-right pb-2">Immobilien</th>
                <th class="pb-2" />
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="p in players"
                :key="p.playerId"
                class="border-b border-surface-800 hover:bg-surface-800 transition-colors cursor-pointer"
                :class="{ 'bg-surface-800': selected?.playerId === p.playerId }"
                @click="select(p)"
              >
                <td class="py-2 pr-4 font-medium text-white">{{ p.username }}</td>
                <td class="py-2 pr-4 text-right text-green-400">{{ fmt(p.cash) }}</td>
                <td class="py-2 pr-4 text-right text-gray-300">{{ fmt(p.netWorth) }}</td>
                <td class="py-2 pr-4 text-right text-gray-400">{{ p.currentTurn }}</td>
                <td class="py-2 pr-4 text-right text-gray-400">{{ p.schufaScore }}</td>
                <td class="py-2 pr-4 text-right text-gray-400">{{ p.realEstate.length }}</td>
                <td class="py-2">
                  <button class="text-xs text-indigo-400 hover:text-indigo-300" @click.stop="select(p)">
                    Bearbeiten
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Edit panel -->
      <div v-if="selected" class="space-y-6">

        <!-- Row 1: character editor + real estate -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

          <!-- Character editor -->
          <div class="card space-y-4">
            <h3 class="text-base font-semibold text-white">
              Charakter bearbeiten — <span style="color: var(--accent)">{{ selected.username }}</span>
            </h3>

            <div class="grid grid-cols-2 gap-3">
              <label class="block">
                <span class="text-xs text-gray-400">Cash (€)</span>
                <input v-model.number="edit.cash" type="number" step="100" class="admin-input" />
              </label>
              <label class="block">
                <span class="text-xs text-gray-400">SCHUFA (0–1000)</span>
                <input v-model.number="edit.schufaScore" type="number" min="0" max="1000" class="admin-input" />
              </label>
              <label class="block">
                <span class="text-xs text-gray-400">Stress (0–100)</span>
                <input v-model.number="edit.stress" type="number" min="0" max="100" class="admin-input" />
              </label>
              <label class="block">
                <span class="text-xs text-gray-400">Happiness (0–100)</span>
                <input v-model.number="edit.happiness" type="number" min="0" max="100" class="admin-input" />
              </label>
              <label class="block">
                <span class="text-xs text-gray-400">Energie (0–100)</span>
                <input v-model.number="edit.energy" type="number" min="0" max="100" class="admin-input" />
              </label>
              <label class="block">
                <span class="text-xs text-gray-400">Hunger (0–100)</span>
                <input v-model.number="edit.hunger" type="number" min="0" max="100" class="admin-input" />
              </label>
              <label class="block">
                <span class="text-xs text-gray-400">Gefängnismonate</span>
                <input v-model.number="edit.jailMonthsRemaining" type="number" min="0" class="admin-input" />
              </label>
              <label class="block">
                <span class="text-xs text-gray-400">Exilmonate</span>
                <input v-model.number="edit.exileMonthsRemaining" type="number" min="0" class="admin-input" />
              </label>
            </div>

            <label class="flex items-center gap-2 cursor-pointer">
              <input v-model="edit.burnoutActive" type="checkbox" class="accent-indigo-500" />
              <span class="text-sm text-gray-300">Burnout aktiv</span>
            </label>

            <div class="flex gap-2 pt-1">
              <button class="btn-primary text-sm" :disabled="saving" @click="saveCharacter">
                {{ saving ? 'Speichern…' : 'Speichern' }}
              </button>
              <button class="btn-secondary text-sm" @click="selected = null; detail = null">Abbrechen</button>
            </div>

            <p v-if="saveError" class="text-red-400 text-xs">{{ saveError }}</p>
          </div>

          <!-- Real estate -->
          <div class="card space-y-3">
            <h3 class="text-base font-semibold text-white">Immobilien</h3>

            <p v-if="selected.realEstate.length === 0" class="text-gray-500 text-sm">Keine Immobilien.</p>

            <div
              v-for="prop in selected.realEstate"
              :key="prop.id"
              class="flex items-center justify-between py-2 border-b border-surface-700 last:border-0"
            >
              <div>
                <p class="text-sm text-white">{{ prop.name }}</p>
                <p class="text-xs text-gray-500">{{ prop.mode }} · {{ fmt(prop.purchasePrice) }}</p>
              </div>
              <button
                class="text-xs text-red-400 hover:text-red-300 transition-colors"
                :disabled="deletingId === prop.id"
                @click="deleteProperty(prop.id)"
              >
                {{ deletingId === prop.id ? '…' : 'Löschen' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Row 2: player detail overview -->
        <div v-if="detailLoading" class="card py-6 text-center text-gray-500 text-sm">Lade Details…</div>
        <div v-else-if="detail" class="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">

          <!-- Collectibles -->
          <div class="card space-y-2">
            <h3 class="text-sm font-semibold text-white">
              Sammlerstücke
              <span class="text-gray-500 font-normal ml-1">({{ detail.collectibles.length }})</span>
            </h3>
            <p v-if="detail.collectibles.length === 0" class="text-gray-500 text-xs">Keine.</p>
            <div class="max-h-64 overflow-y-auto space-y-1 pr-1">
              <div
                v-for="c in detail.collectibles"
                :key="c.name"
                class="flex items-center justify-between py-1 border-b border-surface-800 last:border-0"
              >
                <div class="min-w-0">
                  <p class="text-xs text-white truncate">{{ c.name }}</p>
                  <p class="text-xs text-gray-500 truncate">{{ c.collectionName }} · {{ c.country }}</p>
                </div>
                <span class="text-xs ml-2 flex-shrink-0" :class="rarityColor(c.rarity)">{{ c.rarity }}</span>
              </div>
            </div>
          </div>

          <!-- Social relationships -->
          <div class="card space-y-2">
            <h3 class="text-sm font-semibold text-white">
              Beziehungen
              <span class="text-gray-500 font-normal ml-1">({{ detail.socialRelationships.length }})</span>
            </h3>
            <p v-if="detail.socialRelationships.length === 0" class="text-gray-500 text-xs">Keine.</p>
            <div class="max-h-64 overflow-y-auto space-y-1.5 pr-1">
              <div
                v-for="r in detail.socialRelationships"
                :key="r.personId"
                class="flex items-center gap-2"
              >
                <span class="text-xs text-gray-300 w-28 flex-shrink-0 truncate">{{ r.personName }}</span>
                <div class="flex-1 bg-surface-700 rounded-full h-1.5 overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all"
                    :style="{ width: r.score + '%' }"
                    :class="r.score >= 70 ? 'bg-green-500' : r.score >= 40 ? 'bg-yellow-500' : 'bg-red-500'"
                  />
                </div>
                <span class="text-xs text-gray-400 w-7 text-right flex-shrink-0">{{ r.score }}</span>
              </div>
            </div>
          </div>

          <!-- Travel -->
          <div class="card space-y-3">
            <h3 class="text-sm font-semibold text-white">Reisen</h3>
            <div class="space-y-1 text-xs">
              <div class="flex justify-between">
                <span class="text-gray-500">Aktuell:</span>
                <span class="text-white">{{ detail.travel.currentCountry ?? '—' }}</span>
              </div>
              <div v-if="detail.travel.destinationCountry" class="flex justify-between">
                <span class="text-gray-500">Unterwegs nach:</span>
                <span class="text-yellow-400">{{ detail.travel.destinationCountry }} (M{{ detail.travel.arriveAtTurn }})</span>
              </div>
              <div class="flex justify-between">
                <span class="text-gray-500">Besuchte Länder:</span>
                <span class="text-gray-300">{{ detail.travel.visitedCountries.length }}</span>
              </div>
            </div>
            <div v-if="detail.travel.visitedCountries.length > 0" class="flex flex-wrap gap-1 pt-1">
              <span
                v-for="c in detail.travel.visitedCountries"
                :key="c"
                class="text-xs bg-surface-700 text-gray-300 px-1.5 py-0.5 rounded"
              >{{ c }}</span>
            </div>
          </div>

          <!-- Investments -->
          <div class="card space-y-2">
            <h3 class="text-sm font-semibold text-white">
              Investitionen
              <span class="text-gray-500 font-normal ml-1">({{ detail.investments.length }})</span>
            </h3>
            <p v-if="detail.investments.length === 0" class="text-gray-500 text-xs">Keine.</p>
            <div class="max-h-64 overflow-y-auto space-y-1 pr-1">
              <div
                v-for="inv in detail.investments"
                :key="inv.name + inv.type"
                class="flex items-center justify-between py-1 border-b border-surface-800 last:border-0"
              >
                <div class="min-w-0">
                  <p class="text-xs text-white truncate">{{ inv.name }}</p>
                  <p class="text-xs text-gray-500">{{ inv.type }}</p>
                </div>
                <div class="text-right ml-2 flex-shrink-0">
                  <p class="text-xs text-white">{{ fmt(inv.currentValue) }}</p>
                  <p class="text-xs" :class="inv.currentValue >= inv.amountInvested ? 'text-green-400' : 'text-red-400'">
                    {{ inv.currentValue >= inv.amountInvested ? '+' : '' }}{{ fmt(inv.currentValue - inv.amountInvested) }}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <!-- Education -->
          <div class="card space-y-2">
            <h3 class="text-sm font-semibold text-white">
              Ausbildung
              <span class="text-gray-500 font-normal ml-1">({{ detail.completedEducationStages.length }} Stufen)</span>
            </h3>
            <p v-if="detail.completedEducationStages.length === 0" class="text-gray-500 text-xs">Keine abgeschlossenen Stufen.</p>
            <div class="max-h-64 overflow-y-auto flex flex-wrap gap-1 pr-1">
              <span
                v-for="stage in detail.completedEducationStages"
                :key="stage"
                class="text-xs bg-indigo-900/40 text-indigo-300 border border-indigo-800/50 px-1.5 py-0.5 rounded font-mono"
              >{{ stage }}</span>
            </div>
          </div>

        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'default' })

import { useAuthStore } from '~/stores/auth'
import { useToastStore } from '~/stores/toast'

const authStore = useAuthStore()
const toastStore = useToastStore()
const config = useRuntimeConfig()

interface RealEstateRow {
  id: number
  name: string
  mode: string
  purchasePrice: number
}

interface AdminPlayer {
  playerId: number
  username: string
  cash: number
  netWorth: number
  stress: number
  happiness: number
  energy: number
  hunger: number
  schufaScore: number
  jailMonthsRemaining: number
  exileMonthsRemaining: number
  burnoutActive: boolean
  currentTurn: number
  realEstate: RealEstateRow[]
}

interface CollectibleRow { name: string; rarity: string; collectionName: string; country: string }
interface TravelRow { currentCountry: string; destinationCountry: string | null; arriveAtTurn: number | null; visitedCountries: string[] }
interface SocialRow { personId: string; personName: string; score: number }
interface InvestmentRow { name: string; type: string; amountInvested: number; currentValue: number }
interface AdminPlayerDetail {
  collectibles: CollectibleRow[]
  travel: TravelRow
  socialRelationships: SocialRow[]
  investments: InvestmentRow[]
  completedEducationStages: string[]
}

const checking = ref(true)
const players = ref<AdminPlayer[]>([])
const loading = ref(false)
const selected = ref<AdminPlayer | null>(null)
const detail = ref<AdminPlayerDetail | null>(null)
const detailLoading = ref(false)
const saving = ref(false)
const saveError = ref('')
const deletingId = ref<number | null>(null)

const edit = reactive({
  cash: 0,
  stress: 0,
  happiness: 0,
  energy: 0,
  hunger: 0,
  schufaScore: 0,
  jailMonthsRemaining: 0,
  exileMonthsRemaining: 0,
  burnoutActive: false,
})

function headers() {
  return { Authorization: `Bearer ${authStore.token}` }
}

async function load() {
  if (!authStore.isAdmin) return
  loading.value = true
  try {
    players.value = await $fetch<AdminPlayer[]>(`${config.public.apiBase}/api/admin/players`, { headers: headers() })
    if (selected.value) {
      selected.value = players.value.find(p => p.playerId === selected.value!.playerId) ?? null
    }
  } catch (e: any) {
    toastStore.error(e?.data?.message ?? 'Fehler beim Laden')
  } finally {
    loading.value = false
  }
}

async function select(p: AdminPlayer) {
  selected.value = p
  detail.value = null
  saveError.value = ''
  Object.assign(edit, {
    cash: p.cash,
    stress: p.stress,
    happiness: p.happiness,
    energy: p.energy,
    hunger: p.hunger,
    schufaScore: p.schufaScore,
    jailMonthsRemaining: p.jailMonthsRemaining,
    exileMonthsRemaining: p.exileMonthsRemaining,
    burnoutActive: p.burnoutActive,
  })
  detailLoading.value = true
  try {
    detail.value = await $fetch<AdminPlayerDetail>(
      `${config.public.apiBase}/api/admin/players/${p.playerId}/details`,
      { headers: headers() }
    )
  } catch {
    // non-critical — detail panel just won't show
  } finally {
    detailLoading.value = false
  }
}

function rarityColor(rarity: string): string {
  return { LEGENDARY: 'text-yellow-400', EPIC: 'text-purple-400', RARE: 'text-blue-400', COMMON: 'text-gray-500' }[rarity] ?? 'text-gray-500'
}

async function saveCharacter() {
  if (!selected.value) return
  saving.value = true
  saveError.value = ''
  try {
    const updated = await $fetch<AdminPlayer>(
      `${config.public.apiBase}/api/admin/players/${selected.value.playerId}/character`,
      { method: 'PATCH', headers: headers(), body: { ...edit } }
    )
    const idx = players.value.findIndex(p => p.playerId === updated.playerId)
    if (idx !== -1) players.value[idx] = updated
    selected.value = updated
    toastStore.success('Gespeichert')
  } catch (e: any) {
    saveError.value = e?.data?.message ?? 'Fehler beim Speichern'
  } finally {
    saving.value = false
  }
}

async function deleteProperty(propId: number) {
  if (!selected.value) return
  if (!confirm('Immobilie wirklich löschen?')) return
  deletingId.value = propId
  try {
    await $fetch(
      `${config.public.apiBase}/api/admin/players/${selected.value.playerId}/real-estate/${propId}`,
      { method: 'DELETE', headers: headers() }
    )
    selected.value.realEstate = selected.value.realEstate.filter(p => p.id !== propId)
    const playerInList = players.value.find(p => p.playerId === selected.value!.playerId)
    if (playerInList) playerInList.realEstate = selected.value.realEstate
    toastStore.success('Immobilie gelöscht')
  } catch (e: any) {
    toastStore.error(e?.data?.message ?? 'Fehler beim Löschen')
  } finally {
    deletingId.value = null
  }
}

function fmt(value: number) {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(value)
}

onMounted(async () => {
  // Wait for admin status if it's still being fetched after session restore
  if (!authStore.isAdmin && authStore.isAuthenticated) {
    await authStore._fetchAdminStatus()
  }
  checking.value = false
  if (authStore.isAdmin) load()
})
</script>

<style scoped>
.admin-input {
  @apply mt-1 w-full bg-surface-800 border border-surface-600 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:border-indigo-500;
}
</style>
