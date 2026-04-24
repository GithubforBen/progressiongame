<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-bold text-white">Lebensstil</h2>
      <span class="text-sm text-gray-400">
        Guthaben: <span class="font-semibold text-white">{{ formatCurrency(gameStore.character?.cash ?? 0) }}</span>
      </span>
    </div>

    <p class="text-sm text-gray-400">
      Einmalige Luxusgüter mit dauerhaften Auswirkungen. Lifestyle-Items geben monatliche Effekte oder schalten besondere Möglichkeiten frei.
    </p>

    <div v-if="loading" class="text-gray-500 text-sm">Lade...</div>

    <div v-else class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
      <div
        v-for="item in catalog"
        :key="item.id"
        class="rounded-xl border p-5 flex flex-col gap-3 transition-colors"
        :class="item.owned
          ? 'border-accent/50 bg-accent/5'
          : 'border-white/10 bg-white/3 hover:border-white/20'"
      >
        <!-- Header -->
        <div class="flex items-start justify-between gap-2">
          <div>
            <div class="flex items-center gap-2 mb-0.5">
              <span class="text-2xl">{{ item.icon }}</span>
              <p class="text-white font-semibold text-sm">{{ item.name }}</p>
              <span v-if="item.owned" class="text-xs px-1.5 py-0.5 rounded bg-accent/20 text-accent">Besessen</span>
            </div>
            <p class="text-yellow-400 font-mono text-sm">{{ formatCurrency(item.cost) }}</p>
          </div>
        </div>

        <!-- Description -->
        <p class="text-xs text-gray-400 leading-relaxed">{{ item.description }}</p>

        <!-- Effects -->
        <div class="flex flex-wrap gap-1.5">
          <span v-if="item.stressReductionMonth > 0" class="text-xs px-1.5 py-0.5 rounded bg-green-500/15 text-green-400">
            😌 −{{ item.stressReductionMonth }} Stress/Monat
          </span>
          <span v-if="item.monthlyCost > 0" class="text-xs px-1.5 py-0.5 rounded bg-red-500/15 text-red-400">
            💸 {{ formatCurrency(item.monthlyCost) }}/Monat Betrieb
          </span>
          <span v-if="item.taxEvasionBoost" class="text-xs px-1.5 py-0.5 rounded bg-blue-500/15 text-blue-400">
            🕵️ Steuerprüfung −15%
          </span>
          <span v-if="item.unlocksBillionaire" class="text-xs px-1.5 py-0.5 rounded bg-purple-500/15 text-purple-400">
            🤝 Milliardäre treffen
          </span>
        </div>

        <!-- Buy Button -->
        <button
          v-if="!item.owned"
          class="btn-primary text-xs py-2 mt-auto"
          :disabled="buying === item.id || (gameStore.character?.cash ?? 0) < item.cost"
          @click="buyItem(item)"
        >
          {{ buying === item.id ? 'Kaufe...' : 'Kaufen' }}
        </button>
        <div v-else class="text-xs text-accent font-medium mt-auto">✓ In deinem Besitz</div>
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

interface LifestyleItem {
  id: string
  name: string
  icon: string
  cost: number
  monthlyCost: number
  stressReductionMonth: number
  taxEvasionBoost: boolean
  unlocksBillionaire: boolean
  description: string
  owned: boolean
}

const catalog = ref<LifestyleItem[]>([])
const loading = ref(false)
const buying = ref<string | null>(null)

async function loadCatalog() {
  loading.value = true
  try {
    catalog.value = await api.get<LifestyleItem[]>('/api/lifestyle')
  } catch {
    toast.error('Lebensstil-Items konnten nicht geladen werden')
  } finally {
    loading.value = false
  }
}

async function buyItem(item: LifestyleItem) {
  buying.value = item.id
  try {
    const result = await api.post<LifestyleItem>(`/api/lifestyle/buy/${item.id}`)
    const idx = catalog.value.findIndex(i => i.id === item.id)
    if (idx !== -1) catalog.value[idx] = result
    toast.success(`${item.name} gekauft!`)
    await gameStore.fetchCharacter()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Kauf fehlgeschlagen')
  } finally {
    buying.value = null
  }
}

onMounted(() => {
  loadCatalog()
  gameStore.fetchCharacter()
})
</script>
