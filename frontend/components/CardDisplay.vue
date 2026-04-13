<template>
  <div
    class="w-12 h-16 rounded-lg border flex flex-col items-center justify-between p-1 text-xs font-bold select-none"
    :class="card === '??' ? 'bg-surface-700 border-surface-500 text-gray-500' : isRed ? 'bg-white text-red-600 border-red-300' : 'bg-white text-gray-900 border-gray-300'"
  >
    <template v-if="card === '??'">
      <span class="text-lg mt-2">?</span>
    </template>
    <template v-else>
      <span class="self-start leading-none">{{ displayValue }}</span>
      <span class="text-base leading-none">{{ suitSymbol }}</span>
      <span class="self-end leading-none rotate-180">{{ displayValue }}</span>
    </template>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ card: string }>()

const rankDisplay: Record<string, string> = { T: '10', J: 'J', Q: 'Q', K: 'K', A: 'A' }
const suitSymbols: Record<string, string> = { H: '♥', D: '♦', C: '♣', S: '♠' }

const displayValue = computed(() => {
  if (props.card === '??') return '?'
  const r = props.card[0]
  return rankDisplay[r] ?? r
})

const suitSymbol = computed(() => {
  if (props.card === '??') return ''
  return suitSymbols[props.card[1]] ?? props.card[1]
})

const isRed = computed(() => props.card !== '??' && (props.card[1] === 'H' || props.card[1] === 'D'))
</script>
