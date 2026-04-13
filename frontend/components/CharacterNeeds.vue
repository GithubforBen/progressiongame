<template>
  <div class="card">
    <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-4">Bedürfnisse</h3>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-x-8 gap-y-5">
      <div v-for="need in needs" :key="need.key">
        <div class="flex items-center justify-between mb-1.5">
          <span class="text-sm text-gray-300 font-medium">{{ need.label }}</span>
          <span
            class="text-xs font-mono font-semibold"
            :class="needValueColor(need.value, need.invert)"
          >
            {{ need.value }}%
          </span>
        </div>
        <div class="h-2 bg-surface-700 rounded-full overflow-hidden">
          <div
            class="h-full rounded-full transition-all duration-700 ease-in-out"
            :class="barColor(need.value, need.invert)"
            :style="{ width: `${need.value}%` }"
          />
        </div>
        <p class="text-xs text-gray-600 mt-1">{{ need.hint(need.value) }}</p>
      </div>
    </div>

    <p class="text-xs text-gray-600 mt-4 border-t border-surface-700 pt-3">
      Bedürfnisse sinken jeden Monat. Hoher Stress und niedrige Werte beeinflussen das Gehalt.
    </p>
  </div>
</template>

<script setup lang="ts">
import { useGameStore } from '~/stores/game'

const gameStore = useGameStore()
const char = computed(() => gameStore.character)

const needs = computed(() => [
  {
    key: 'hunger',
    label: 'Hunger',
    value: char.value?.hunger ?? 100,
    invert: false,
    hint: (v: number) => v < 30 ? 'Kritisch – wirkt sich auf Leistung aus' : v < 60 ? 'Etwas hungrig' : 'Gut versorgt',
  },
  {
    key: 'energy',
    label: 'Energie',
    value: char.value?.energy ?? 100,
    invert: false,
    hint: (v: number) => v < 30 ? 'Erschöpft' : v < 60 ? 'Müde' : 'Ausgeruht',
  },
  {
    key: 'happiness',
    label: 'Glück',
    value: char.value?.happiness ?? 70,
    invert: false,
    hint: (v: number) => v < 30 ? 'Unglücklich' : v < 60 ? 'Zufrieden' : 'Glücklich',
  },
  {
    key: 'stress',
    label: 'Stress',
    value: char.value?.stress ?? 0,
    invert: true,
    hint: (v: number) => v > 70 ? 'Sehr gestresst' : v > 40 ? 'Mäßiger Stress' : 'Entspannt',
  },
])

function barColor(value: number, invert: boolean) {
  const bad = invert ? value > 70 : value < 30
  const warn = invert ? value > 40 : value < 60
  if (bad) return 'bg-red-500'
  if (warn) return 'bg-yellow-500'
  return invert ? 'bg-green-500' : 'bg-indigo-500'
}

function needValueColor(value: number, invert: boolean) {
  const bad = invert ? value > 70 : value < 30
  const warn = invert ? value > 40 : value < 60
  if (bad) return 'text-red-400'
  if (warn) return 'text-yellow-400'
  return 'text-gray-300'
}
</script>
