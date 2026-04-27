<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">🕵️ Steuerhinterziehung</h1>
        <p class="text-gray-500 text-sm mt-1">Riskante Steueroptimierung — auf eigene Gefahr</p>
      </div>
      <div v-if="status" class="text-right">
        <div class="text-xs text-gray-500 uppercase tracking-wider">Level</div>
        <div class="text-2xl font-bold" :class="levelColor">{{ status.level }}</div>
      </div>
    </div>

    <!-- Caught pending fallback -->
    <div v-if="status?.caughtPending" class="rounded-xl border border-red-500 bg-red-950/30 p-5 space-y-4">
      <div class="flex items-center gap-3">
        <span class="text-3xl">⚠️</span>
        <div>
          <p class="text-red-400 font-bold">STEUERFAHNDUNG — Dringende Entscheidung!</p>
          <p class="text-gray-400 text-sm">Hinterzogene Steuern: {{ fmt(status.cumulativeEvaded) }}</p>
        </div>
      </div>
      <p class="text-gray-300 text-sm">Du wurdest bei der Steuerhinterziehung erwischt. Wähle dein Schicksal:</p>
      <div class="grid grid-cols-2 gap-4">
        <button
          class="flex flex-col items-center gap-2 p-4 rounded-lg border border-surface-600 bg-surface-700 hover:bg-surface-600 transition-colors disabled:opacity-40"
          :disabled="resolving"
          @click="resolve('JAIL')"
        >
          <span class="text-2xl">🔒</span>
          <span class="font-semibold text-white">Gefängnis</span>
          <span class="text-sm text-gray-400">6 Monate, kein Einkommen</span>
          <span class="text-sm text-red-400">Stress +15/Mo, SCHUFA −100</span>
        </button>
        <button
          class="flex flex-col items-center gap-2 p-4 rounded-lg border transition-colors disabled:opacity-40"
          :class="canAffordFlee ? 'border-surface-600 bg-surface-700 hover:bg-surface-600' : 'border-surface-700 bg-surface-800 cursor-not-allowed'"
          :disabled="!canAffordFlee || resolving"
          @click="resolve('FLEE')"
        >
          <span class="text-2xl">✈️</span>
          <span class="font-semibold text-white">Kaution + Flucht</span>
          <span class="text-sm text-yellow-400 font-mono">{{ fmt(bailAmount) }}</span>
          <span class="text-sm text-gray-400">3 Monate Exil</span>
          <span class="text-sm text-orange-400">SCHUFA −50</span>
          <span v-if="!canAffordFlee" class="text-xs text-red-400">Nicht genug Geld</span>
        </button>
      </div>
    </div>

    <!-- Jail / Exile status -->
    <div v-if="status && status.jailMonthsRemaining > 0" class="rounded-xl border border-orange-500/50 bg-orange-950/20 p-4 flex items-center gap-3">
      <span class="text-2xl">🔒</span>
      <div>
        <p class="text-orange-400 font-semibold">Du sitzt in Haft</p>
        <p class="text-gray-400 text-sm">{{ status.jailMonthsRemaining }} Monate verbleiben — kein Einkommen</p>
      </div>
    </div>
    <div v-else-if="status && status.exileMonthsRemaining > 0" class="rounded-xl border border-blue-500/50 bg-blue-950/20 p-4 flex items-center gap-3">
      <span class="text-2xl">✈️</span>
      <div>
        <p class="text-blue-400 font-semibold">Du lebst im Exil</p>
        <p class="text-gray-400 text-sm">{{ status.exileMonthsRemaining }} Monate Mindestaufenthalt verbleiben</p>
      </div>
    </div>

    <div v-if="status" class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <!-- Status card -->
      <div class="card space-y-4">
        <h2 class="text-sm font-semibold text-gray-400 uppercase tracking-wider">Status</h2>

        <div v-if="status.level === 0" class="rounded-lg bg-surface-700 p-3 text-sm space-y-2">
          <p class="text-gray-400 font-semibold">Kein Level freigeschaltet</p>
          <p class="text-gray-500">Absolviere folgende Kurse im <NuxtLink to="/ausbildung" class="text-accent hover:underline">Ausbildungsbaum</NuxtLink>:</p>
          <div class="space-y-1 text-xs">
            <p class="text-gray-400">1. <span class="font-mono text-yellow-500">WEITERBILDUNG_STEUERN_1</span> (Voraussetzung)</p>
            <p class="text-gray-400">2. <span class="font-mono text-orange-400">WEITERBILDUNG_STEUERHINTERZIEHUNG_1</span> → 2 Monate, 800€</p>
          </div>
        </div>

        <div v-else class="space-y-3">
          <div class="flex items-center justify-between">
            <span class="text-sm text-gray-400">Level</span>
            <span class="font-semibold" :class="levelColor">{{ levelLabel }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-sm text-gray-400">Hinterziehungsquote</span>
            <span class="font-mono font-semibold text-green-400">{{ evasionPercent }}%</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-sm text-gray-400">Entdeckungsrisiko/Monat</span>
            <span class="font-mono font-semibold" :class="detectionColor">{{ (status.detectionChancePercent ?? 0).toFixed(0) }}%</span>
          </div>
          <div class="pt-1 border-t border-surface-700">
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-400">Toggle</span>
              <button
                class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none disabled:opacity-40"
                :class="status.active ? 'bg-red-500' : 'bg-surface-600'"
                :disabled="toggleDisabled"
                @click="toggle"
              >
                <span
                  class="inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform"
                  :class="status.active ? 'translate-x-6' : 'translate-x-1'"
                />
              </button>
            </div>
            <p v-if="status.active" class="text-xs text-red-400 mt-1">Aktiv — Steuern werden hinterzogen</p>
            <p v-else class="text-xs text-gray-500 mt-1">Inaktiv — normaler Steuerbetrieb</p>
          </div>
        </div>
      </div>

      <!-- Statistik card -->
      <div class="card space-y-3">
        <h2 class="text-sm font-semibold text-gray-400 uppercase tracking-wider">Statistik</h2>
        <div class="flex items-center justify-between">
          <span class="text-sm text-gray-400">Kumulativ hinterzogen</span>
          <span class="font-mono font-semibold text-yellow-400">{{ fmt(status.cumulativeEvaded) }}</span>
        </div>
        <div v-if="status.active && status.level > 0" class="text-xs text-gray-500 rounded-lg bg-surface-700 p-2">
          Im Falle einer Entdeckung:<br>
          Kaution = <span class="text-yellow-400 font-mono">{{ fmt(bailAmount) }}</span><br>
          Gefängnisstrafe = 6 Monate
        </div>
        <div v-if="!status.active && status.cumulativeEvaded > 0" class="text-xs text-gray-500">
          Kumulierte Steuern werden beim Deaktivieren nicht zurückgesetzt — Risiko bleibt bestehen bis zur Entdeckung oder Reset.
        </div>
      </div>
    </div>

    <!-- Info card -->
    <div class="card space-y-3 text-sm text-gray-400">
      <h2 class="text-sm font-semibold text-gray-400 uppercase tracking-wider">Regeln & Risiken</h2>
      <div class="space-y-2">
        <div class="flex gap-2"><span class="text-gray-600">›</span> <span>Level 1 (Bargeldzahlungen): 20% Steuerersparnis, 15% Entdeckungsrisiko/Monat</span></div>
        <div class="flex gap-2"><span class="text-gray-600">›</span> <span>Level 2 (Briefkastenfirma): 40% Steuerersparnis, 8% Entdeckungsrisiko/Monat</span></div>
        <div class="flex gap-2"><span class="text-gray-600">›</span> <span>Level 3 (Offshore-Konten): 60% Steuerersparnis, 3% Entdeckungsrisiko/Monat</span></div>
        <div class="flex gap-2"><span class="text-red-500">›</span> <span class="text-red-400">Erwischt? Gefängnis (6 Mo, kein Einkommen, SCHUFA −100) oder Kaution + Exil (3 Mo, SCHUFA −50)</span></div>
        <div class="flex gap-2"><span class="text-gray-600">›</span> <span>Voraussetzung: WEITERBILDUNG_STEUERN_1 im Ausbildungsbaum</span></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useGameStore } from '~/stores/game'
import { useToastStore } from '~/stores/toast'

definePageMeta({ layout: 'default' })

const gameStore = useGameStore()
const toastStore = useToastStore()
const config = useRuntimeConfig()
const authStore = useAuthStore()

interface TaxStatus {
  active: boolean
  level: number
  cumulativeEvaded: number
  detectionChancePercent: number
  jailMonthsRemaining: number
  exileMonthsRemaining: number
  caughtPending: boolean
  bailAmount: number
}

const status = ref<TaxStatus | null>(null)
const resolving = ref(false)

async function fetchStatus() {
  try {
    status.value = await $fetch<TaxStatus>(`${config.public.apiBase}/api/tax-evasion/status`, {
      headers: { Authorization: `Bearer ${authStore.token}` },
    })
  } catch {}
}

onMounted(fetchStatus)

const toggleDisabled = computed(() =>
  !status.value
  || status.value.level === 0
  || status.value.caughtPending
  || status.value.jailMonthsRemaining > 0
)

async function toggle() {
  try {
    await gameStore.toggleTaxEvasion()
    await fetchStatus()
  } catch (e: any) {
    toastStore.error(e?.data?.message ?? 'Fehler beim Toggle')
  }
}

const bailAmount = computed(() => {
  if (!status.value) return 5000
  return Math.max(5000, (status.value.cumulativeEvaded ?? 0) * 3)
})

const canAffordFlee = computed(() =>
  (gameStore.character?.cash ?? 0) >= bailAmount.value
)

async function resolve(choice: 'JAIL' | 'FLEE') {
  if (resolving.value) return
  resolving.value = true
  try {
    await gameStore.resolveCaught(choice)
    await fetchStatus()
    toastStore.info(choice === 'JAIL' ? '🔒 Ins Gefängnis — 6 Monate.' : '✈️ Flucht ins Exil — 3 Monate.')
  } catch (e: any) {
    toastStore.error(e?.data?.message ?? 'Fehler')
  } finally {
    resolving.value = false
  }
}

const levelLabel = computed(() => {
  switch (status.value?.level) {
    case 1: return 'Level 1 — Bargeldzahlungen'
    case 2: return 'Level 2 — Briefkastenfirma'
    case 3: return 'Level 3 — Offshore-Konten'
    default: return 'Kein Level'
  }
})

const levelColor = computed(() => {
  switch (status.value?.level) {
    case 1: return 'text-yellow-400'
    case 2: return 'text-orange-400'
    case 3: return 'text-red-400'
    default: return 'text-gray-500'
  }
})

const evasionPercent = computed(() => {
  const rates = [0, 20, 40, 60]
  return rates[status.value?.level ?? 0]
})

const detectionColor = computed(() => {
  const pct = status.value?.detectionChancePercent ?? 0
  if (pct >= 10) return 'text-red-400'
  if (pct >= 5) return 'text-orange-400'
  return 'text-green-400'
})

function fmt(val: number) {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(val ?? 0)
}
</script>
