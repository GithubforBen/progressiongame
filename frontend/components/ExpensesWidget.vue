<template>
  <div class="card">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Monatliche Ausgaben</h3>
      <span class="text-sm font-mono font-semibold text-red-400">
        − {{ formatCurrency(gameStore.totalMonthlyExpenses) }}
      </span>
    </div>

    <div class="space-y-2">
      <div
        v-for="expense in gameStore.expenses"
        :key="expense.id"
        class="flex items-center justify-between py-1.5"
        :class="!expense.active ? 'opacity-40' : ''"
      >
        <div class="flex items-center gap-2 min-w-0">
          <span
            class="w-2 h-2 rounded-full flex-shrink-0"
            :class="expense.active ? categoryColor(expense.category) : 'bg-gray-600'"
          />
          <span class="text-sm text-gray-300 truncate">{{ expense.label }}</span>
          <span v-if="expense.mandatory" class="badge bg-surface-600 text-gray-400 text-xs">Pflicht</span>
        </div>
        <div class="flex items-center gap-3 flex-shrink-0">
          <span class="text-sm font-mono text-gray-400">{{ formatCurrency(expense.amount) }}</span>
          <button
            v-if="!expense.mandatory"
            class="text-xs px-2 py-0.5 rounded transition-colors"
            :class="expense.active
              ? 'text-gray-500 hover:text-red-400 hover:bg-red-400/10'
              : 'text-gray-600 hover:text-green-400 hover:bg-green-400/10'"
            @click="toggle(expense.id)"
          >
            {{ expense.active ? 'Deaktivieren' : 'Aktivieren' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="gameStore.expenses.length === 0" class="text-center text-gray-600 text-sm py-4">
      Keine Ausgaben gefunden
    </div>
  </div>
</template>

<script setup lang="ts">
import { useGameStore } from '~/stores/game'

const gameStore = useGameStore()

function formatCurrency(value: number) {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

function categoryColor(category: string) {
  const colors: Record<string, string> = {
    MIETE: 'bg-blue-500',
    ESSEN: 'bg-orange-500',
    KRANKENVERSICHERUNG: 'bg-green-500',
    GYM: 'bg-purple-500',
    STREAMING: 'bg-pink-500',
  }
  return colors[category] ?? 'bg-gray-500'
}

async function toggle(id: number) {
  await gameStore.toggleExpense(id)
}
</script>
