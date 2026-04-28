<template>
  <div class="space-y-6">
    <h2 class="text-xl font-bold text-white">Bedürfnisse</h2>

    <!-- Status bars -->
    <div class="card">
      <h3 class="text-base font-semibold text-white mb-4">Aktueller Zustand</h3>

      <!-- Burnout banner -->
      <div v-if="character?.burnoutActive" class="mb-4 p-3 rounded-lg bg-red-500/15 border border-red-500/40 flex items-center gap-3">
        <span class="text-2xl">🔥</span>
        <div>
          <p class="text-red-400 font-semibold text-sm">Burnout aktiv</p>
          <p class="text-red-300/70 text-xs">Du bist ausgebrannt und kannst nicht arbeiten. Stress muss erst auf unter 40 sinken.</p>
        </div>
      </div>

      <!-- Depression banner -->
      <div v-if="character?.depressionMonthsRemaining > 0" class="mb-4 p-3 rounded-lg bg-blue-900/20 border border-blue-500/30 flex items-center gap-3">
        <span class="text-2xl">😞</span>
        <div>
          <p class="text-blue-300 font-semibold text-sm">Depression ({{ character.depressionMonthsRemaining }} Monate verbleibend)</p>
          <p class="text-blue-300/60 text-xs">+3 Stress pro Monat. Therapie-Sitzungen helfen beim Abbauen.</p>
        </div>
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div v-for="stat in stats" :key="stat.label">
          <div class="flex justify-between items-center mb-1">
            <span class="text-xs text-gray-400">{{ stat.icon }} {{ stat.label }}</span>
            <span class="text-xs font-mono" :class="stat.color">{{ stat.value }}%</span>
          </div>
          <div class="h-2 rounded-full bg-surface-700 overflow-hidden">
            <div
              class="h-full rounded-full transition-all duration-500"
              :class="stat.barColor"
              :style="{ width: stat.value + '%' }"
            />
          </div>
          <p v-if="(!stat.invert && stat.value < 20) || (stat.invert && stat.value > 80)" class="text-xs text-red-400 mt-0.5">Kritisch!</p>
          <p v-else-if="(!stat.invert && stat.value < 40) || (stat.invert && stat.value > 60)" class="text-xs text-yellow-400 mt-0.5">Erhöht</p>
        </div>
      </div>
    </div>

    <!-- Shop -->
    <div class="card">
      <div class="mb-4">
        <h3 class="text-base font-semibold text-white">Sofortartikel</h3>
        <p class="text-xs text-gray-500 mt-0.5">Einmalkäufe für sofortige Effekte auf deine Werte. Versicherungen findest du unter <NuxtLink to="/leben" class="text-accent hover:underline">Leben & Ausgaben</NuxtLink>.</p>
      </div>
      <div v-if="loading" class="text-gray-500 text-sm">Lade...</div>
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3">
        <div
          v-for="item in items"
          :key="item.id"
          class="rounded-lg border border-white/10 bg-white/3 p-4 flex flex-col gap-3 hover:border-white/20 transition-colors"
        >
          <div class="flex items-start justify-between gap-2">
            <div>
              <p class="text-white font-medium text-sm">{{ item.name }}</p>
              <p class="text-yellow-400 font-mono text-sm mt-0.5">
                {{ item.price > 0 ? formatCurrency(item.price) : 'Kostenlos' }}
              </p>
            </div>
          </div>

          <!-- Effects -->
          <div class="flex flex-wrap gap-1.5">
            <span v-if="item.hungerEffect !== 0" class="effect-chip" :class="item.hungerEffect > 0 ? 'positive' : 'negative'">
              🍽 Hunger {{ item.hungerEffect > 0 ? '+' : '' }}{{ item.hungerEffect }}
            </span>
            <span v-if="item.energyEffect !== 0" class="effect-chip" :class="item.energyEffect > 0 ? 'positive' : 'negative'">
              ⚡ Energie {{ item.energyEffect > 0 ? '+' : '' }}{{ item.energyEffect }}
            </span>
            <span v-if="item.happinessEffect !== 0" class="effect-chip" :class="item.happinessEffect > 0 ? 'positive' : 'negative'">
              😊 Glück {{ item.happinessEffect > 0 ? '+' : '' }}{{ item.happinessEffect }}
            </span>
            <span v-if="item.stressEffect !== 0" class="effect-chip" :class="item.stressEffect < 0 ? 'positive' : 'negative'">
              😤 Stress {{ item.stressEffect > 0 ? '+' : '' }}{{ item.stressEffect }}
            </span>
            <span v-if="item.depressionReduction" class="effect-chip positive">
              💊 Depression -1 Monat
            </span>
          </div>

          <button
            class="btn-primary text-xs py-1.5 w-full mt-auto"
            :disabled="buyingId === item.id"
            @click="purchase(item)"
          >
            {{ buyingId === item.id ? 'Kaufe...' : 'Kaufen' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'default' })

const api = useApi()
const toast = useToastStore()
const gameStore = useGameStore()
const { formatCurrency } = useFormatting()

interface NeedsItem {
  id: string
  name: string
  price: number
  hungerEffect: number
  energyEffect: number
  happinessEffect: number
  stressEffect: number
  depressionReduction: boolean
}

const items = ref<NeedsItem[]>([])
const loading = ref(false)
const buyingId = ref<string | null>(null)

const character = computed(() => gameStore.character)

const stats = computed(() => {
  const c = character.value
  if (!c) return []
  return [
    { label: 'Hunger', icon: '🍽', value: c.hunger ?? 0, color: statColor(c.hunger ?? 0), barColor: statBarColor(c.hunger ?? 0), invert: false },
    { label: 'Energie', icon: '⚡', value: c.energy ?? 0, color: statColor(c.energy ?? 0), barColor: statBarColor(c.energy ?? 0), invert: false },
    { label: 'Glück', icon: '😊', value: c.happiness ?? 0, color: statColor(c.happiness ?? 0), barColor: statBarColor(c.happiness ?? 0), invert: false },
    { label: 'Stress', icon: '😤', value: c.stress ?? 0, color: stressColor(c.stress ?? 0), barColor: stressBarColor(c.stress ?? 0), invert: true },
  ]
})

function statColor(v: number) {
  if (v < 20) return 'text-red-400'
  if (v < 40) return 'text-yellow-400'
  return 'text-green-400'
}
function statBarColor(v: number) {
  if (v < 20) return 'bg-red-500'
  if (v < 40) return 'bg-yellow-500'
  return 'bg-green-500'
}
function stressColor(v: number) {
  if (v >= 80) return 'text-red-400'
  if (v >= 60) return 'text-yellow-400'
  return 'text-green-400'
}
function stressBarColor(v: number) {
  if (v >= 80) return 'bg-red-500'
  if (v >= 60) return 'bg-yellow-500'
  return 'bg-green-500'
}

async function purchase(item: NeedsItem) {
  buyingId.value = item.id
  try {
    await api.post('/api/needs/purchase', { itemId: item.id })
    await gameStore.fetchCharacter()
    toast.success(`${item.name} genossen!`)
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Kauf fehlgeschlagen')
  } finally {
    buyingId.value = null
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const result = await api.get<NeedsItem[]>('/api/needs/items')
    items.value = Array.isArray(result) ? result : []
  } catch {
    toast.error('Items konnten nicht geladen werden')
  } finally {
    loading.value = false
  }
  await gameStore.fetchCharacter()
})
</script>

<style scoped>
.effect-chip {
  @apply text-xs px-1.5 py-0.5 rounded font-medium;
}
.effect-chip.positive {
  @apply bg-green-500/15 text-green-400;
}
.effect-chip.negative {
  @apply bg-red-500/15 text-red-400;
}
</style>
