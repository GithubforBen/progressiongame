<template>
  <div>
    <div class="flex items-center justify-between mb-1.5">
      <span class="text-xs text-gray-400 font-medium">{{ label }}</span>
      <span class="text-xs font-mono text-gray-300">{{ displayValue }}%</span>
    </div>
    <div class="h-2 bg-surface-700 rounded-full overflow-hidden">
      <div
        class="h-full rounded-full transition-all duration-500"
        :class="barColor"
        :style="{ width: `${displayValue}%` }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  label: string
  value: number
  color?: string
  invert?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  color: 'bg-indigo-500',
  invert: false,
})

const displayValue = computed(() => Math.max(0, Math.min(100, props.value)))

const barColor = computed(() => {
  if (props.invert) {
    if (displayValue.value > 70) return 'bg-red-500'
    if (displayValue.value > 40) return 'bg-yellow-500'
    return 'bg-green-500'
  }
  if (displayValue.value < 30) return 'bg-red-500'
  if (displayValue.value < 60) return 'bg-yellow-500'
  return props.color
})
</script>
