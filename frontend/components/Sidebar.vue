<template>
  <aside
    class="bg-surface-900 border-r border-surface-700 flex flex-col flex-shrink-0 overflow-hidden transition-all duration-200"
    :style="{ width: compact ? '52px' : '218px' }"
  >
    <!-- Logo -->
    <div
      class="h-14 flex items-center border-b border-surface-700 flex-shrink-0"
      :class="compact ? 'px-3.5' : 'px-4'"
    >
      <span
        class="font-extrabold tracking-tighter whitespace-nowrap"
        :class="compact ? 'text-base' : 'text-sm'"
        style="color: var(--accent)"
      >{{ compact ? 'FL' : 'FinanzLeben' }}</span>
    </div>

    <!-- Nav -->
    <nav class="flex-1 px-1.5 py-2.5 overflow-y-auto overflow-x-hidden">
      <div
        v-for="(group, gi) in [...NAV_GROUPS, ...victoryNavGroup, ...adminNavGroup]"
        :key="gi"
        :class="compact ? 'mb-1.5' : 'mb-1'"
      >
        <!-- Group divider (compact) -->
        <div
          v-if="compact && group.label && gi > 0"
          class="h-px mx-2.5 my-1.5 bg-surface-700"
        />
        <!-- Group label (expanded) -->
        <p
          v-if="!compact && group.label"
          class="text-surface-500 px-3 pt-1.5 pb-1 font-bold uppercase tracking-widest"
          style="font-size: 9.5px;"
        >{{ group.label }}</p>

        <NuxtLink
          v-for="item in group.items"
          :key="item.path"
          :to="item.path"
          :title="compact ? item.label : undefined"
          class="flex items-center gap-2.5 w-full rounded-lg border-l-2 mb-px transition-all duration-100 whitespace-nowrap"
          :class="[
            isActive(item.path)
              ? 'border-l-accent text-accent font-semibold'
              : 'border-l-transparent text-gray-500 hover:text-gray-100 hover:bg-surface-700',
            compact ? 'px-3 py-2' : 'px-2.5 py-1.5',
          ]"
          :style="isActive(item.path) ? 'background: var(--accent-bg)' : ''"
        >
          <!-- eslint-disable-next-line vue/no-v-html -->
          <svg
            width="15" height="15" viewBox="0 0 24 24"
            fill="none" stroke="currentColor" stroke-width="2"
            stroke-linecap="round" stroke-linejoin="round"
            class="flex-shrink-0"
            v-html="ICONS[item.icon]"
          />
          <span
            v-if="!compact"
            class="text-sm overflow-hidden text-ellipsis"
          >{{ item.label }}</span>
          <span
            v-if="item.path === '/steuerhinterziehung' && hasCaughtPending && !compact"
            class="ml-auto w-2 h-2 rounded-full bg-red-500 animate-pulse flex-shrink-0"
          />
          <span
            v-if="item.path === '/steuerhinterziehung' && hasCaughtPending && compact"
            class="absolute top-1 right-1 w-1.5 h-1.5 rounded-full bg-red-500"
          />
        </NuxtLink>
      </div>
    </nav>

    <!-- Playtime -->
    <div v-if="!compact" class="px-3.5 py-1.5 border-t border-surface-700">
      <p class="text-xs text-gray-600">⏱ Spielzeit: <span class="text-gray-400">{{ gameStore.formattedPlaytime }}</span></p>
    </div>

    <!-- Player info -->
    <div class="border-t border-surface-700 p-1.5">
      <NuxtLink
        to="/einstellungen"
        class="flex items-center gap-2.5 px-2.5 py-1.5 rounded-lg hover:bg-surface-700 cursor-pointer transition-colors"
      >
        <div
          class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0"
          style="background: var(--accent-bg); border: 1px solid var(--accent-border); color: var(--accent)"
        >{{ avatarInitial }}</div>
        <div v-if="!compact" class="min-w-0">
          <p class="text-sm font-medium text-gray-100 truncate">{{ username }}</p>
          <p class="text-xs text-gray-600">Einstellungen</p>
        </div>
      </NuxtLink>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'
import { useGameStore } from '~/stores/game'

defineProps<{ compact?: boolean }>()

const route = useRoute()
const authStore = useAuthStore()
const gameStore = useGameStore()

const ICONS: Record<string, string> = {
  LayoutDashboard: '<rect width="7" height="9" x="3" y="3" rx="1.5"/><rect width="7" height="5" x="14" y="3" rx="1.5"/><rect width="7" height="9" x="14" y="12" rx="1.5"/><rect width="7" height="5" x="3" y="15" rx="1.5"/>',
  TrendingUp:      '<polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/>',
  CreditCard:      '<rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" x2="22" y1="10" y2="10"/>',
  Dices:           '<rect width="18" height="18" x="3" y="3" rx="2"/><path d="M16 8h.01M8 8h.01M8 16h.01M16 16h.01M12 12h.01"/>',
  FileSearch:      '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><circle cx="11.5" cy="14.5" r="2.5"/><polyline points="13.25 16.25 15 18"/>',
  Briefcase:       '<rect width="20" height="14" x="2" y="7" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>',
  GraduationCap:   '<path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/>',
  Heart:           '<path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>',
  Activity:        '<polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>',
  Plane:           '<path d="M17.8 19.2 16 11l3.5-3.5C21 6 21.5 4 21 3c-1-.5-3 0-4.5 1.5L13 8 4.8 6.2c-.5-.1-.9.1-1.1.5l-.3.5c-.2.5-.1 1 .3 1.3L9 12l-2 3H4l-1 1 3 2 2 3 1-1v-3l3-2 3.5 5.3c.3.4.8.5 1.3.3l.5-.2c.4-.3.6-.7.5-1.2z"/>',
  Users:           '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  Star:            '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>',
  Building2:       '<path d="M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18Z"/><path d="M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2"/><path d="M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2"/><path d="M10 6h4M10 10h4M10 14h4M10 18h4"/>',
  Trophy:          '<path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/><path d="M4 22h16"/><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/><path d="M18 2H6v7a6 6 0 0 0 12 0V2z"/>',
  Crown:           '<path d="M11.562 3.266a1 1 0 0 1 .876 0L22 8l-4 13H6L2 8z"/><path d="M12 12v9"/><path d="M12 3v3"/><path d="m9 16.998-.9-3"/><path d="m15 17 .9-3"/>',
  Settings:        '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>',
  ShieldAlert:     '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M12 8v4"/><path d="M12 16h.01"/>',
}

const NAV_GROUPS = [
  {
    label: '',
    items: [
      { path: '/',              label: 'Dashboard',   icon: 'LayoutDashboard' },
    ],
  },
  {
    label: 'Finanzen',
    items: [
      { path: '/investitionen',      label: 'Investitionen', icon: 'TrendingUp' },
      { path: '/kredite',            label: 'Kredite',       icon: 'CreditCard' },
      { path: '/gluecksspiel',       label: 'Glücksspiel',  icon: 'Dices' },
      { path: '/steuerhinterziehung', label: 'Steuern',      icon: 'FileSearch' },
    ],
  },
  {
    label: 'Karriere',
    items: [
      { path: '/karriere',   label: 'Karriere',   icon: 'Briefcase' },
      { path: '/ausbildung', label: 'Ausbildung', icon: 'GraduationCap' },
    ],
  },
  {
    label: 'Privatleben',
    items: [
      { path: '/leben',        label: 'Leben',       icon: 'Heart' },
      { path: '/beduerfnisse', label: 'Bedürfnisse', icon: 'Activity' },
      { path: '/lebensstil',   label: 'Lebensstil',  icon: 'Crown' },
      { path: '/reisen',       label: 'Reisen',       icon: 'Plane' },
      { path: '/beziehungen',  label: 'Beziehungen', icon: 'Users' },
      { path: '/sammlungen',   label: 'Sammlungen',  icon: 'Star' },
      { path: '/immobilien',   label: 'Immobilien',  icon: 'Building2' },
    ],
  },
  {
    label: 'Community',
    items: [
      { path: '/rangliste',     label: 'Rangliste',     icon: 'Trophy' },
      { path: '/einstellungen', label: 'Einstellungen', icon: 'Settings' },
    ],
  },
]

const victoryNavGroup = computed(() => gameStore.character?.victoryAchieved
  ? [{ label: '', items: [{ path: '/sieg', label: '🏆 Sieg', icon: 'Crown' }] }]
  : []
)

const adminNavGroup = computed(() => authStore.isAdmin
  ? [{ label: 'Admin', items: [{ path: '/admin', label: 'Admin', icon: 'ShieldAlert' }] }]
  : []
)

const hasCaughtPending = computed(() => gameStore.character?.taxEvasionCaughtPending === true)

const isActive = (path: string) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const username = computed(() => authStore.user?.username ?? 'Spieler')
const avatarInitial = computed(() => username.value.charAt(0).toUpperCase())
</script>
