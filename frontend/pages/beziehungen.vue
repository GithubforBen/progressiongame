<template>
  <div class="space-y-6">
    <h2 class="text-xl font-bold text-white">Beziehungen</h2>

    <!-- Loading -->
    <div v-if="loading" class="card flex items-center justify-center h-32 text-gray-500 text-sm">
      Lade NPCs…
    </div>

    <template v-else>
      <!-- Summary -->
      <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
        <div class="card text-center">
          <p class="text-xs text-gray-400 mb-1">Bekannte Personen</p>
          <p class="text-white font-semibold text-lg">{{ metCount }} / {{ npcs.length }}</p>
        </div>
        <div class="card text-center">
          <p class="text-xs text-gray-400 mb-1">Ø Beziehungslevel</p>
          <p class="text-white font-semibold text-lg">{{ avgLevel }}</p>
        </div>
        <div class="card text-center col-span-2 sm:col-span-1">
          <p class="text-xs text-gray-400 mb-1">Happiness-Bonus / Monat</p>
          <p class="text-green-400 font-semibold text-lg">+{{ totalBonus }}</p>
        </div>
      </div>

      <!-- NPC cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
        <div
          v-for="npc in npcs"
          :key="npc.id"
          class="card space-y-3 border transition-colors"
          :class="npc.met ? 'border-white/10' : 'border-white/5 opacity-80'"
        >
          <!-- Header -->
          <div class="flex items-start gap-3">
            <!-- Avatar -->
            <div class="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold text-sm flex-shrink-0"
                 :class="personalityColor(npc.personality)">
              {{ npc.name.charAt(0) }}
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <p class="text-white font-semibold text-sm">{{ npc.name }}</p>
                <span class="text-xs px-1.5 py-0.5 rounded" :class="personalityBadge(npc.personality)">
                  {{ personalityLabel(npc.personality) }}
                </span>
              </div>
              <p class="text-gray-400 text-xs mt-0.5 leading-relaxed">{{ npc.description }}</p>
            </div>
          </div>

          <!-- Relationship level (only if met) -->
          <template v-if="npc.met">
            <div>
              <div class="flex justify-between text-xs text-gray-400 mb-1">
                <span>Beziehung</span>
                <span>{{ npc.level }} / 100</span>
              </div>
              <div class="h-1.5 bg-surface-700 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full transition-all"
                  :class="levelBarColor(npc.level)"
                  :style="{ width: npc.level + '%' }"
                />
              </div>
            </div>
            <div class="flex items-center justify-between text-xs">
              <span class="text-gray-500">{{ npc.monthsKnown }} Monate bekannt</span>
              <span class="text-green-400">
                +{{ Math.round(npc.level * npc.happinessBonusPerLevel / 100) }} Happiness/Monat
              </span>
            </div>
            <button
              @click="interact(npc)"
              :disabled="!npc.canInteract || interactingId === npc.id"
              class="w-full btn-primary text-sm py-2 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <span v-if="interactingId === npc.id">…</span>
              <span v-else-if="npc.canInteract">Zeit verbringen (+10 Level)</span>
              <span v-else>Bereits getroffen diesen Monat</span>
            </button>
          </template>

          <!-- Not met yet -->
          <template v-else>
            <p class="text-xs text-gray-500 italic">Du kennst diese Person noch nicht.</p>
            <button
              @click="meet(npc)"
              :disabled="meetingId === npc.id"
              class="w-full btn-secondary text-sm py-2"
            >
              {{ meetingId === npc.id ? '…' : 'Kennenlernen' }}
            </button>
          </template>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'default' })

const api = useApi()
const toast = useToastStore()
const { } = useFormatting()

interface Npc {
  id: number
  name: string
  description: string
  personality: string
  happinessBonusPerLevel: number
  met: boolean
  level: number
  monthsKnown: number
  canInteract: boolean
}

const npcs = ref<Npc[]>([])
const loading = ref(true)
const meetingId = ref<number | null>(null)
const interactingId = ref<number | null>(null)

const metCount = computed(() => npcs.value.filter(n => n.met).length)
const avgLevel = computed(() => {
  const met = npcs.value.filter(n => n.met)
  if (met.length === 0) return 0
  return Math.round(met.reduce((s, n) => s + n.level, 0) / met.length)
})
const totalBonus = computed(() =>
  npcs.value.filter(n => n.met)
    .reduce((s, n) => s + Math.round(n.level * n.happinessBonusPerLevel / 100), 0)
)

function personalityColor(p: string) {
  if (p === 'MENTOR') return 'bg-purple-500/30 text-purple-300'
  if (p === 'RIVAL') return 'bg-red-500/30 text-red-300'
  if (p === 'COLLEAGUE') return 'bg-blue-500/30 text-blue-300'
  return 'bg-green-500/30 text-green-300'
}

function personalityBadge(p: string) {
  if (p === 'MENTOR') return 'bg-purple-500/20 text-purple-300'
  if (p === 'RIVAL') return 'bg-red-500/20 text-red-300'
  if (p === 'COLLEAGUE') return 'bg-blue-500/20 text-blue-300'
  return 'bg-green-500/20 text-green-300'
}

function personalityLabel(p: string) {
  const map: Record<string, string> = {
    FRIENDLY: 'Freundlich', MENTOR: 'Mentor', RIVAL: 'Rivale', COLLEAGUE: 'Kollege',
  }
  return map[p] ?? p
}

function levelBarColor(level: number) {
  if (level >= 70) return 'bg-green-400'
  if (level >= 40) return 'bg-yellow-400'
  return 'bg-accent'
}

async function meet(npc: Npc) {
  meetingId.value = npc.id
  try {
    const updated = await api.post<Npc>(`/api/npcs/${npc.id}/meet`)
    const idx = npcs.value.findIndex(n => n.id === npc.id)
    if (idx !== -1) npcs.value[idx] = updated
    toast.success(`Du hast ${npc.name} kennengelernt!`)
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler')
  } finally {
    meetingId.value = null
  }
}

async function interact(npc: Npc) {
  interactingId.value = npc.id
  try {
    const updated = await api.post<Npc>(`/api/npcs/${npc.id}/interact`)
    const idx = npcs.value.findIndex(n => n.id === npc.id)
    if (idx !== -1) npcs.value[idx] = updated
    toast.success(`Zeit mit ${npc.name} verbracht! Beziehung +10.`)
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler')
  } finally {
    interactingId.value = null
  }
}

onMounted(async () => {
  try {
    npcs.value = await api.get<Npc[]>('/api/npcs')
  } finally {
    loading.value = false
  }
})
</script>
