<template>
  <div class="max-w-2xl mx-auto space-y-6 py-4">
    <!-- Trophy header -->
    <div class="card text-center space-y-4 py-10">
      <div class="text-7xl">🏆</div>
      <h1 class="text-3xl font-extrabold tracking-tight" style="color: var(--accent)">
        Glückwunsch!
      </h1>
      <p class="text-gray-300 text-lg">
        Du hast FinanzLeben gemeistert und bist zum ultimativen Finanzgenie aufgestiegen.
      </p>

      <div v-if="character" class="grid grid-cols-2 gap-4 mt-6 text-left">
        <div class="rounded-lg bg-surface-700 px-4 py-3">
          <p class="text-xs text-gray-500 uppercase tracking-wider mb-1">Erreicht in</p>
          <p class="text-lg font-bold text-white">{{ character.currentTurn }} Monate</p>
        </div>
        <div class="rounded-lg bg-surface-700 px-4 py-3">
          <p class="text-xs text-gray-500 uppercase tracking-wider mb-1">Nettovermögen</p>
          <p class="text-lg font-bold text-white">{{ formatCurrency(character.netWorth) }}</p>
        </div>
        <div class="rounded-lg bg-surface-700 px-4 py-3 col-span-2">
          <p class="text-xs text-gray-500 uppercase tracking-wider mb-1">Persönlicher Bestwert</p>
          <p class="text-xl font-bold" style="color: var(--accent)">{{ formatCurrency(character.personalBestNetWorth ?? 0) }}</p>
        </div>
      </div>
    </div>

    <!-- Victory items checklist -->
    <div class="card space-y-3">
      <h2 class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Errungenschaften</h2>
      <div class="space-y-2">
        <div v-for="item in victoryItems" :key="item.id" class="flex items-center gap-3">
          <span class="text-green-400 flex-shrink-0">✓</span>
          <span class="text-sm text-gray-200">{{ item.icon }} {{ item.label }}</span>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-green-400 flex-shrink-0">✓</span>
          <span class="text-sm text-gray-200">💰 500 Milliarden Nettovermögen erreicht</span>
        </div>
      </div>
    </div>

    <!-- Actions -->
    <div class="card space-y-3">
      <h2 class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Was möchtest du tun?</h2>

      <NuxtLink to="/rangliste" class="w-full flex items-center justify-between p-3 rounded-lg bg-surface-700 hover:bg-surface-600 transition-colors">
        <div>
          <p class="text-sm font-medium text-white">Rangliste anzeigen</p>
          <p class="text-xs text-gray-500 mt-0.5">Vergleich mit anderen Spielern</p>
        </div>
        <span class="text-gray-500">→</span>
      </NuxtLink>

      <button
        class="w-full flex items-center justify-between p-3 rounded-lg bg-surface-700 hover:bg-surface-600 transition-colors"
        @click="navigateTo('/')"
      >
        <div>
          <p class="text-sm font-medium text-white">Dashboard</p>
          <p class="text-xs text-gray-500 mt-0.5">Weiterspielen und Bestwert verbessern</p>
        </div>
        <span class="text-gray-500">→</span>
      </button>

      <button
        class="w-full flex items-center justify-between p-3 rounded-lg border border-red-500/30 hover:bg-red-500/10 transition-colors"
        :disabled="resetting"
        @click="confirmReset"
      >
        <div>
          <p class="text-sm font-medium text-red-400">Neues Spiel starten</p>
          <p class="text-xs text-gray-500 mt-0.5">
            Setzt alles zurück – Bestwert {{ formatCurrency(character?.personalBestNetWorth ?? 0) }} bleibt erhalten
          </p>
        </div>
        <span v-if="resetting" class="text-gray-500 text-xs">Lädt…</span>
        <span v-else class="text-red-500">↺</span>
      </button>
    </div>

    <!-- Reset confirmation modal -->
    <div
      v-if="showResetModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/70"
      @click.self="showResetModal = false"
    >
      <div class="card w-full max-w-sm space-y-4">
        <h3 class="font-bold text-white">Neues Spiel starten?</h3>
        <p class="text-sm text-gray-400">
          Alle Fortschritte, Investitionen, Immobilien und Ausgaben werden zurückgesetzt.
          Dein Bestwert von <strong class="text-white">{{ formatCurrency(character?.personalBestNetWorth ?? 0) }}</strong> bleibt erhalten.
        </p>
        <div class="flex gap-3 justify-end">
          <button class="btn-secondary text-sm" @click="showResetModal = false">Abbrechen</button>
          <button class="btn-danger text-sm" :disabled="resetting" @click="doReset">
            {{ resetting ? 'Wird zurückgesetzt…' : 'Ja, neu starten' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useGameStore } from '~/stores/game'

definePageMeta({ layout: 'default' })

const gameStore = useGameStore()
const { formatCurrency } = useFormatting()
const character = computed(() => gameStore.character)

const resetting = ref(false)
const showResetModal = ref(false)

const victoryItems = [
  { id: 'SCHLOSS', icon: '🏰', label: 'Schloss erworben (€200M)' },
  { id: 'SUPERCAR', icon: '🚗', label: 'Supercar-Sammlung erworben (€50M)' },
  { id: 'SUPER_YACHT', icon: '🛥️', label: 'Super-Yacht erworben (€500M)' },
  { id: 'SPACE_STATION', icon: '🛸', label: 'Raumstation erworben (€100B)' },
  { id: 'ALL_COLLECTIBLES', icon: '🗂️', label: 'Alle Sammlerstücke gesammelt' },
]

function confirmReset() {
  showResetModal.value = true
}

async function doReset() {
  resetting.value = true
  try {
    await gameStore.resetCharacter()
    showResetModal.value = false
    navigateTo('/')
  } finally {
    resetting.value = false
  }
}

onMounted(() => gameStore.init())
</script>
