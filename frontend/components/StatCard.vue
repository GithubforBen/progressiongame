<template>
  <div class="card">
    <p class="text-xs text-gray-400 uppercase tracking-wider mb-1">{{ title }}</p>
    <p class="text-2xl font-bold text-white font-mono">{{ value }}</p>
    <div v-if="trend" class="flex items-center gap-1 mt-1">
      <span
        class="text-xs font-medium"
        :class="trendPositive ? 'text-green-400' : 'text-red-400'"
      >
        {{ trendPositive ? '+' : '' }}{{ trend }}
      </span>
      <span class="text-xs text-gray-500">ggü. letztem Monat</span>
    </div>
    <div v-if="subtitle" class="mt-1 text-xs text-gray-500">{{ subtitle }}</div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title: string
  value: string
  trend?: string
  trendPositive?: boolean
  subtitle?: string
}

defineProps<Props>()
</script>
