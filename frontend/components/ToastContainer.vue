<template>
  <div class="fixed bottom-4 right-4 flex flex-col gap-2 z-50 max-w-sm w-full pointer-events-none">
    <TransitionGroup name="toast">
      <div
        v-for="toast in toastStore.toasts"
        :key="toast.id"
        class="pointer-events-auto bg-surface-800 border rounded-xl p-4 shadow-2xl flex items-start gap-3"
        :class="borderClass(toast.type)"
      >
        <span class="text-lg flex-shrink-0 leading-none mt-0.5">{{ iconFor(toast.type) }}</span>
        <div class="flex-1 min-w-0">
          <p v-if="toast.title" class="text-sm font-semibold text-white">{{ toast.title }}</p>
          <p class="text-sm text-gray-300">{{ toast.message }}</p>
        </div>
        <button
          class="flex-shrink-0 text-gray-500 hover:text-white transition-colors"
          @click="toastStore.dismiss(toast.id)"
        >
          ✕
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { useToastStore } from '~/stores/toast'

const toastStore = useToastStore()

function borderClass(type: string) {
  switch (type) {
    case 'success': return 'border-green-600'
    case 'error': return 'border-red-600'
    case 'warning': return 'border-yellow-600'
    default: return 'border-surface-600'
  }
}

function iconFor(type: string) {
  switch (type) {
    case 'success': return '✓'
    case 'error': return '✕'
    case 'warning': return '!'
    default: return 'i'
  }
}
</script>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>
