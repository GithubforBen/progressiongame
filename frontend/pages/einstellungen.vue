<template>
  <div class="space-y-6 max-w-2xl">
    <h2 class="text-xl font-bold text-white">Einstellungen</h2>

    <!-- ── Profil ── -->
    <div class="card space-y-4">
      <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Profil</h3>
      <div class="flex items-center gap-4">
        <div class="w-14 h-14 rounded-full bg-accent/20 flex items-center justify-center text-2xl font-bold text-accent flex-shrink-0">
          {{ avatarLetter }}
        </div>
        <div>
          <p class="text-lg font-semibold text-white">{{ authStore.user?.username ?? '—' }}</p>
          <p class="text-xs text-gray-500">{{ gameStore.currentMonthLabel }}</p>
        </div>
      </div>
    </div>

    <!-- ── Spielstatistiken ── -->
    <div class="card space-y-4" v-if="character">
      <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Spielstatistiken</h3>
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div class="space-y-0.5">
          <p class="text-xs text-gray-500">Nettovermögen</p>
          <p class="text-base font-bold font-mono" :class="character.netWorth >= 0 ? 'text-green-400' : 'text-red-400'">
            {{ formatCurrency(character.netWorth) }}
          </p>
        </div>
        <div class="space-y-0.5">
          <p class="text-xs text-gray-500">Bargeld</p>
          <p class="text-base font-bold font-mono text-white">{{ formatCurrency(character.cash) }}</p>
        </div>
        <div class="space-y-0.5">
          <p class="text-xs text-gray-500">Spielmonat</p>
          <p class="text-base font-bold text-white">{{ character.currentTurn }}</p>
        </div>
        <div v-if="character.schufaScore != null" class="space-y-0.5">
          <p class="text-xs text-gray-500">SCHUFA-Score</p>
          <p class="text-base font-bold" :class="schufaStyle.color">
            {{ character.schufaScore }}
            <span class="text-xs font-normal ml-1">{{ schufaStyle.label }}</span>
          </p>
        </div>
      </div>

      <!-- Needs mini-overview -->
      <div class="pt-2 border-t border-surface-700">
        <p class="text-xs text-gray-500 mb-2">Zustand</p>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <NeedBar label="Hunger"   :value="character.hunger"   color="bg-orange-500" />
          <NeedBar label="Energie"  :value="character.energy"   color="bg-blue-500" />
          <NeedBar label="Glück"    :value="character.happiness" color="bg-yellow-500" />
          <NeedBar label="Stress"   :value="character.stress"   color="bg-red-500" :inverted="true" />
        </div>
      </div>
    </div>

    <!-- ── Spiel-Einstellungen ── -->
    <div class="card space-y-4">
      <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Darstellung</h3>

      <div class="flex items-center justify-between">
        <div>
          <p class="text-sm text-white">Toast-Benachrichtigungen</p>
          <p class="text-xs text-gray-500 mt-0.5">Ereignisse als Pop-up anzeigen</p>
        </div>
        <button
          class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors"
          :class="notifications ? 'bg-accent' : 'bg-surface-600'"
          @click="toggleNotifications"
        >
          <span
            class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform"
            :class="notifications ? 'translate-x-6' : 'translate-x-1'"
          />
        </button>
      </div>

      <div class="flex items-center justify-between">
        <div>
          <p class="text-sm text-white">Kompakte Ansicht</p>
          <p class="text-xs text-gray-500 mt-0.5">Weniger Abstände und kleinere Karten</p>
        </div>
        <button
          class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors"
          :class="compactView ? 'bg-accent' : 'bg-surface-600'"
          @click="toggleCompactView"
        >
          <span
            class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform"
            :class="compactView ? 'translate-x-6' : 'translate-x-1'"
          />
        </button>
      </div>
    </div>

    <!-- ── Konto ── -->
    <div class="card space-y-3">
      <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Konto</h3>
      <p class="text-xs text-gray-500">
        Eingeloggt als <span class="text-white font-medium">{{ authStore.user?.username }}</span>
      </p>
      <button class="btn-danger text-sm" @click="handleLogout">Abmelden</button>
    </div>

    <!-- ── Version ── -->
    <p class="text-xs text-gray-700 text-center pb-2">FinanzLeben · Entwicklungsversion</p>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'
import { useGameStore } from '~/stores/game'
import { useFormatting } from '~/composables/useFormatting'

definePageMeta({ layout: 'default' })

const authStore = useAuthStore()
const gameStore = useGameStore()
const { formatCurrency, formatSchufaScore } = useFormatting()

const character = computed(() => gameStore.character)

const avatarLetter = computed(() =>
  (authStore.user?.username ?? '?')[0].toUpperCase(),
)

const schufaStyle = computed(() =>
  formatSchufaScore(character.value?.schufaScore ?? 500),
)

// ── Settings stored in localStorage ──────────────────────────────────────────

const notifications = ref(true)
const compactView = ref(false)

onMounted(() => {
  notifications.value = localStorage.getItem('pref_notifications') !== 'false'
  compactView.value = localStorage.getItem('pref_compact') === 'true'
})

function toggleNotifications() {
  notifications.value = !notifications.value
  localStorage.setItem('pref_notifications', String(notifications.value))
}

function toggleCompactView() {
  compactView.value = !compactView.value
  localStorage.setItem('pref_compact', String(compactView.value))
}

function handleLogout() {
  authStore.logout()
}
</script>
