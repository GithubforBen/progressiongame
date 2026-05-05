<template>
  <div class="space-y-6">
    <div>
      <h2 class="text-xl font-bold text-white">Aktive Effekte</h2>
      <p class="text-sm text-gray-500 mt-1">Alle Boni und Rabatte, die aktuell auf deinen Charakter wirken — aus Beziehungen, Sammlungen und Lifestyle-Items.</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <div class="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin" />
    </div>

    <!-- Empty state -->
    <div v-else-if="!groups.length" class="bg-surface-800 border border-surface-700 rounded-xl p-10 text-center">
      <div class="text-4xl mb-3">⚡</div>
      <p class="text-gray-400 font-medium">Keine aktiven Effekte</p>
      <p class="text-gray-600 text-sm mt-1">Baue Beziehungen auf, vervollständige Sammlungen und kaufe Lifestyle-Items, um Boni freizuschalten.</p>
    </div>

    <!-- Effect groups -->
    <div v-else class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <div
        v-for="group in groups"
        :key="group.type"
        class="bg-surface-800 border border-surface-700 rounded-xl p-5 space-y-3"
      >
        <!-- Header -->
        <div class="flex items-start justify-between gap-2">
          <div>
            <p class="font-semibold text-gray-100 text-sm">{{ group.label }}</p>
            <p class="text-xs text-gray-500 mt-0.5">{{ group.unit }}</p>
          </div>
          <span
            class="text-lg font-bold font-mono whitespace-nowrap"
            :class="group.total > 0 ? 'text-green-400' : 'text-red-400'"
          >
            {{ group.total > 0 ? '+' : '' }}{{ formatValue(group.total, group.unit) }}
          </span>
        </div>

        <!-- Divider -->
        <div class="h-px bg-surface-700" />

        <!-- Source breakdown -->
        <div class="space-y-1.5">
          <div
            v-for="(c, i) in group.contributions"
            :key="i"
            class="flex items-center justify-between gap-2 text-xs"
          >
            <div class="flex items-center gap-1.5 min-w-0">
              <span class="text-gray-500 flex-shrink-0">{{ sourceIcon(c.source) }}</span>
              <span class="text-gray-400 truncate">{{ c.source }}: {{ c.detail }}</span>
            </div>
            <span class="text-gray-300 font-mono flex-shrink-0">
              {{ c.value > 0 ? '+' : '' }}{{ formatValue(c.value, group.unit) }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Legend -->
    <div class="flex flex-wrap gap-4 text-xs text-gray-600">
      <span>🤝 Beziehung</span>
      <span>🗂️ Sammlung</span>
      <span>✨ Lifestyle</span>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'default' })

const api = useApi()
const auth = useAuthStore()

interface ContributionDto {
  source: string
  detail: string
  value: number
}
interface EffectGroupDto {
  type: string
  total: number
  label: string
  unit: string
  contributions: ContributionDto[]
}

const loading = ref(true)
const groups = ref<EffectGroupDto[]>([])

async function load() {
  loading.value = true
  try {
    const data = await api.get<{ groups: EffectGroupDto[] }>('/api/effects')
    groups.value = data.groups
  } catch {
    groups.value = []
  } finally {
    loading.value = false
  }
}

function formatValue(value: number, unit: string): string {
  if (unit === '%') return `${Math.round(value * 10) / 10}%`
  if (unit === '€/Monat') return `${value.toLocaleString('de-DE', { minimumFractionDigits: 0, maximumFractionDigits: 0 })} €`
  return `${Math.round(value * 10) / 10}`
}

function sourceIcon(source: string): string {
  if (source === 'Beziehung') return '🤝'
  if (source === 'Sammlung') return '🗂️'
  if (source === 'Lifestyle') return '✨'
  return '•'
}

onMounted(load)
</script>
