<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="show"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        @click.self="$emit('close')"
      >
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" />

        <!-- Modal -->
        <div class="relative bg-surface-800 rounded-2xl border border-surface-600 w-full max-w-lg shadow-2xl overflow-hidden">
          <!-- Header -->
          <div class="bg-surface-900 px-6 py-4 border-b border-surface-700">
            <div class="flex items-center justify-between">
              <div>
                <h2 class="text-lg font-bold text-white">Monatsbilanz</h2>
                <p class="text-xs text-gray-500 mt-0.5">{{ monthLabel }}</p>
              </div>
              <div
                class="text-xl font-bold font-mono"
                :class="netChange >= 0 ? 'text-green-400' : 'text-red-400'"
              >
                {{ netChange >= 0 ? '+' : '' }}{{ formatCurrency(netChange) }}
              </div>
            </div>
          </div>

          <div class="p-6 space-y-5 max-h-[60vh] overflow-y-auto">
            <!-- Income -->
            <div>
              <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
                Einnahmen
              </h3>
              <div v-if="result.incomeBreakdown.length" class="space-y-1.5">
                <div
                  v-for="item in result.incomeBreakdown"
                  :key="item.label"
                  class="flex items-center justify-between"
                >
                  <span class="text-sm text-gray-300">{{ item.label }}</span>
                  <span class="text-sm font-mono text-green-400">+{{ formatCurrency(item.amount) }}</span>
                </div>
              </div>
              <p v-else class="text-sm text-gray-600">Kein Einkommen diesen Monat</p>
              <div class="mt-2 pt-2 border-t border-surface-700 flex justify-between">
                <span class="text-sm font-medium text-gray-300">Brutto gesamt</span>
                <span class="text-sm font-mono font-semibold text-green-400">{{ formatCurrency(result.grossIncome) }}</span>
              </div>
            </div>

            <!-- Expenses -->
            <div>
              <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
                Ausgaben
              </h3>
              <div class="space-y-1.5">
                <div
                  v-for="item in result.expenseBreakdown"
                  :key="item.label"
                  class="flex items-center justify-between"
                >
                  <span class="text-sm text-gray-300">{{ item.label }}</span>
                  <span class="text-sm font-mono text-red-400">−{{ formatCurrency(item.amount) }}</span>
                </div>
              </div>
              <div class="mt-2 pt-2 border-t border-surface-700 flex justify-between">
                <span class="text-sm font-medium text-gray-300">Ausgaben gesamt</span>
                <span class="text-sm font-mono font-semibold text-red-400">−{{ formatCurrency(result.totalExpenses) }}</span>
              </div>
            </div>

            <!-- Events -->
            <div v-if="result.events.length">
              <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">Ereignisse</h3>
              <ul class="space-y-1">
                <li
                  v-for="event in result.events"
                  :key="event"
                  class="text-sm text-gray-400 flex items-start gap-2"
                >
                  <span class="text-gray-600 flex-shrink-0 mt-0.5">›</span>
                  {{ event }}
                </li>
              </ul>
            </div>
          </div>

          <!-- Footer -->
          <div class="px-6 py-4 border-t border-surface-700 flex items-center justify-between">
            <div class="text-xs text-gray-500">
              Neuer Kontostand:
              <span class="font-mono text-white font-semibold">{{ formatCurrency(result.character.cash) }}</span>
            </div>
            <button class="btn-primary" @click="$emit('close')">Weiter</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import type { TurnResult } from '~/stores/game'

interface Props {
  show: boolean
  result: TurnResult
  monthLabel: string
}

const props = defineProps<Props>()
defineEmits<{ close: [] }>()

const netChange = computed(() => props.result.netChange)

function formatCurrency(value: number) {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-active .relative,
.modal-leave-active .relative {
  transition: transform 0.2s ease;
}
.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.96) translateY(8px);
}
</style>
