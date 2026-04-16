<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="show"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        @click.self="$emit('close')"
      >
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" />

        <div class="relative bg-surface-800 rounded-2xl border border-surface-600 w-full max-w-2xl shadow-2xl overflow-hidden">
          <!-- Header -->
          <div class="bg-surface-900 px-6 py-4 border-b border-surface-700">
            <div class="flex items-center justify-between">
              <div>
                <h2 class="text-lg font-bold text-white">Monatsbilanz</h2>
                <p class="text-xs text-gray-500 mt-0.5">{{ monthLabel }}</p>
              </div>
              <div class="text-xl font-bold font-mono"
                :class="netChange >= 0 ? 'text-green-400' : 'text-red-400'">
                {{ netChange >= 0 ? '+' : '' }}{{ formatCurrency(netChange) }}
              </div>
            </div>
          </div>

          <div class="p-6 space-y-5 max-h-[70vh] overflow-y-auto">

            <!-- Charts row -->
            <ClientOnly>
              <div v-if="hasChartData" class="grid grid-cols-2 gap-4">
                <!-- Income chart -->
                <div v-if="result.incomeBreakdown.length > 0">
                  <p class="text-xs font-semibold text-gray-400 uppercase tracking-wider text-center mb-2">Einnahmen</p>
                  <div class="flex justify-center">
                    <canvas ref="incomeCanvas" width="180" height="180"></canvas>
                  </div>
                </div>
                <!-- Expense chart -->
                <div v-if="result.expenseBreakdown.length > 0">
                  <p class="text-xs font-semibold text-gray-400 uppercase tracking-wider text-center mb-2">Ausgaben</p>
                  <div class="flex justify-center">
                    <canvas ref="expenseCanvas" width="180" height="180"></canvas>
                  </div>
                </div>
              </div>
            </ClientOnly>

            <!-- Income breakdown -->
            <div>
              <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">Einnahmen</h3>
              <div v-if="result.incomeBreakdown.length" class="space-y-1.5">
                <div v-for="item in result.incomeBreakdown" :key="item.label"
                  class="flex items-center justify-between">
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

            <!-- Expense breakdown -->
            <div>
              <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">Ausgaben</h3>
              <div class="space-y-1.5">
                <div v-for="item in result.expenseBreakdown" :key="item.label"
                  class="flex items-center justify-between">
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
                <li v-for="event in result.events" :key="event"
                  class="text-sm text-gray-400 flex items-start gap-2">
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
import {
  Chart as ChartJS,
  DoughnutController,
  ArcElement,
  Tooltip,
  Legend,
} from 'chart.js'

ChartJS.register(DoughnutController, ArcElement, Tooltip, Legend)

interface Props {
  show: boolean
  result: TurnResult
  monthLabel: string
}

const props = defineProps<Props>()
defineEmits<{ close: [] }>()

const netChange = computed(() => props.result.netChange)
const hasChartData = computed(() =>
  props.result.incomeBreakdown.length > 0 || props.result.expenseBreakdown.length > 0,
)

const incomeCanvas = ref<HTMLCanvasElement | null>(null)
const expenseCanvas = ref<HTMLCanvasElement | null>(null)
let incomeChart: ChartJS | null = null
let expenseChart: ChartJS | null = null

const INCOME_COLORS = ['#6366f1', '#10b981', '#14b8a6', '#3b82f6', '#8b5cf6', '#06b6d4', '#84cc16']
const EXPENSE_COLORS = ['#ef4444', '#f97316', '#f59e0b', '#ec4899', '#a855f7', '#6b7280', '#64748b']

function buildDoughnut(
  canvas: HTMLCanvasElement,
  items: { label: string; amount: number }[],
  colors: string[],
): ChartJS {
  return new ChartJS(canvas, {
    type: 'doughnut',
    data: {
      labels: items.map(i => i.label),
      datasets: [{
        data: items.map(i => i.amount),
        backgroundColor: items.map((_, idx) => colors[idx % colors.length]),
        borderWidth: 0,
        hoverOffset: 4,
      }],
    },
    options: {
      responsive: false,
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color: '#9ca3af',
            font: { size: 10 },
            boxWidth: 10,
            padding: 8,
          },
        },
        tooltip: {
          callbacks: {
            label: (ctx) => {
              const val = ctx.parsed as number
              return ` ${new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(val)}`
            },
          },
        },
      },
    },
  })
}

function createCharts() {
  if (!import.meta.client) return
  // Defer one tick to ensure canvas elements are mounted after v-if resolves
  nextTick(() => {
    if (incomeCanvas.value && props.result.incomeBreakdown.length > 0) {
      incomeChart?.destroy()
      incomeChart = buildDoughnut(incomeCanvas.value, props.result.incomeBreakdown, INCOME_COLORS)
    }
    if (expenseCanvas.value && props.result.expenseBreakdown.length > 0) {
      expenseChart?.destroy()
      expenseChart = buildDoughnut(expenseCanvas.value, props.result.expenseBreakdown, EXPENSE_COLORS)
    }
  })
}

function destroyCharts() {
  incomeChart?.destroy(); incomeChart = null
  expenseChart?.destroy(); expenseChart = null
}

watch(() => props.show, (val) => {
  if (val) createCharts()
  else destroyCharts()
})

onBeforeUnmount(destroyCharts)

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
