<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-bold text-white">Dashboard</h2>
    </div>

    <!-- Stat cards -->
    <div class="grid grid-cols-2 xl:grid-cols-4 gap-4">
      <StatCard
        title="Kontostand"
        :value="formatCurrency(character?.cash ?? 0)"
      />
      <StatCard
        title="Nettovermögen"
        :value="formatCurrency(character?.netWorth ?? 0)"
      />
      <StatCard
        title="Monat"
        :value="String(character?.currentTurn ?? 1)"
        subtitle="Spielrunden gespielt"
      />
      <StatCard
        title="Stress"
        :value="`${character?.stress ?? 0}%`"
        :trend-positive="false"
      />
    </div>

    <!-- Needs -->
    <div class="card">
      <h3 class="text-sm font-semibold text-white uppercase tracking-wider mb-4">Bedürfnisse</h3>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
        <NeedBar label="Hunger" :value="character?.hunger ?? 100" color="bg-orange-500" />
        <NeedBar label="Energie" :value="character?.energy ?? 100" color="bg-blue-500" />
        <NeedBar label="Glück" :value="character?.happiness ?? 70" color="bg-yellow-500" />
        <NeedBar label="Stress" :value="character?.stress ?? 0" :invert="true" />
      </div>
    </div>

    <!-- Quick actions -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div class="card hover:border-accent/50 transition-colors cursor-pointer" @click="navigateTo('/karriere')">
        <p class="text-xs text-gray-400 uppercase tracking-wider mb-2">Karriere</p>
        <p class="text-white font-medium">Jobs verwalten</p>
        <p class="text-gray-500 text-sm mt-1">Bewerbungen, aktive Stellen</p>
      </div>
      <div class="card hover:border-accent/50 transition-colors cursor-pointer" @click="navigateTo('/investitionen')">
        <p class="text-xs text-gray-400 uppercase tracking-wider mb-2">Investitionen</p>
        <p class="text-white font-medium">Portfolio verwalten</p>
        <p class="text-gray-500 text-sm mt-1">Aktien, Immobilien, mehr</p>
      </div>
      <div class="card hover:border-accent/50 transition-colors cursor-pointer" @click="navigateTo('/ausbildung')">
        <p class="text-xs text-gray-400 uppercase tracking-wider mb-2">Ausbildung</p>
        <p class="text-white font-medium">Bildungsweg</p>
        <p class="text-gray-500 text-sm mt-1">Aktueller Fortschritt</p>
      </div>
    </div>

    <!-- Net worth placeholder -->
    <div class="card">
      <h3 class="text-sm font-semibold text-white uppercase tracking-wider mb-4">Nettovermögen-Verlauf</h3>
      <div class="h-40 flex items-center justify-center text-gray-600 text-sm border border-dashed border-surface-600 rounded-lg">
        Diagramm erscheint nach dem ersten Monatsabschluss
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useGameStore } from '~/stores/game'

definePageMeta({ layout: 'default' })

const gameStore = useGameStore()
const character = computed(() => gameStore.character)

function formatCurrency(value: number) {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

onMounted(() => {
  gameStore.fetchCharacter().catch(() => {})
})
</script>
