<template>
  <aside class="w-56 bg-surface-800 border-r border-surface-700 flex flex-col flex-shrink-0">
    <!-- Logo -->
    <div class="h-14 flex items-center px-5 border-b border-surface-700">
      <span class="text-accent font-bold text-lg tracking-tight">FinanzLeben</span>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 py-3 space-y-0.5 px-2 overflow-y-auto">
      <NuxtLink
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors"
        :class="isActive(item.path)
          ? 'bg-accent text-white'
          : 'text-gray-400 hover:text-white hover:bg-surface-700'"
      >
        <span class="w-5 text-center text-base leading-none select-none">{{ item.icon }}</span>
        {{ item.label }}
      </NuxtLink>
    </nav>

    <!-- Player info -->
    <div class="border-t border-surface-700 p-3">
      <div class="flex items-center gap-3 px-2 py-2 rounded-lg hover:bg-surface-700 cursor-pointer transition-colors">
        <div class="w-8 h-8 rounded-full bg-accent flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
          {{ avatarInitial }}
        </div>
        <div class="min-w-0">
          <p class="text-sm font-medium text-white truncate">{{ username }}</p>
          <p class="text-xs text-gray-400">Einstellungen</p>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const navItems = [
  { path: '/', label: 'Dashboard', icon: '▦' },
  { path: '/investitionen', label: 'Investitionen', icon: '↗' },
  { path: '/karriere', label: 'Karriere', icon: '◈' },
  { path: '/ausbildung', label: 'Ausbildung', icon: '◎' },
  { path: '/leben', label: 'Leben', icon: '⌂' },
  { path: '/reisen', label: 'Reisen', icon: '✈' },
  { path: '/sammlungen', label: 'Sammlungen', icon: '★' },
  { path: '/immobilien', label: 'Immobilien', icon: '🏠' },
  { path: '/kredite', label: 'Kredite', icon: '🏦' },
  { path: '/gluecksspiel', label: 'Glücksspiel', icon: '🎰' },
  { path: '/beziehungen', label: 'Beziehungen', icon: '♥' },
  { path: '/rangliste', label: 'Rangliste', icon: '◆' },
  { path: '/einstellungen', label: 'Einstellungen', icon: '⚙' },
]

const isActive = (path: string) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const username = computed(() => authStore.user?.username ?? 'Spieler')
const avatarInitial = computed(() => username.value.charAt(0).toUpperCase())
</script>
