<template>
  <div class="flex h-[calc(100vh-4rem)] overflow-hidden gap-0">
    <!-- Graph Panel -->
    <div class="relative flex-1 bg-surface-900 overflow-hidden" ref="graphContainer"
         @mousedown="onPanStart" @mousemove="onPanMove" @mouseup="onPanEnd" @mouseleave="onPanEnd"
         @wheel.prevent="onWheel">
      <!-- Header -->
      <div class="absolute top-3 left-3 z-10 flex items-center gap-2">
        <h2 class="text-white font-bold text-lg">Netzwerk</h2>
        <span class="text-gray-400 text-xs">({{ network?.persons.length ?? 0 }} Kontakte)</span>
      </div>
      <!-- Zoom controls -->
      <div class="absolute top-3 right-3 z-10 flex gap-1">
        <button @click="zoom(1.2)" class="w-7 h-7 rounded bg-surface-700 text-white text-sm hover:bg-surface-600">+</button>
        <button @click="zoom(0.8)" class="w-7 h-7 rounded bg-surface-700 text-white text-sm hover:bg-surface-600">−</button>
        <button @click="resetView" class="px-2 h-7 rounded bg-surface-700 text-gray-300 text-xs hover:bg-surface-600">Reset</button>
      </div>

      <div v-if="loading" class="absolute inset-0 flex items-center justify-center text-gray-500 text-sm">
        Lade Netzwerk…
      </div>

      <!-- SVG Graph -->
      <svg v-else class="w-full h-full select-none" :viewBox="`0 0 ${SVG_W} ${SVG_H}`"
           preserveAspectRatio="xMidYMid meet"
           :style="{ cursor: panning ? 'grabbing' : 'grab' }">
        <g :transform="`translate(${panX},${panY}) scale(${scale})`">
          <!-- Group labels -->
          <g v-for="g in visibleGroups" :key="g.id">
            <text :x="groupCenter(g.id).x" :y="groupCenter(g.id).y - 72"
                  text-anchor="middle" :fill="groupColor(g.id)" opacity="0.5" font-size="10" font-weight="bold">
              {{ g.name }}
            </text>
            <circle :cx="groupCenter(g.id).x" :cy="groupCenter(g.id).y"
                    :r="groupRadius(g.id)"
                    :stroke="groupColor(g.id)" stroke-width="1" fill="none" opacity="0.08" stroke-dasharray="4 4"/>
          </g>

          <!-- Edges between persons -->
          <line v-for="edge in edges" :key="`${edge.sourcePersonId}-${edge.targetPersonId}`"
                :x1="pos(edge.sourcePersonId).x" :y1="pos(edge.sourcePersonId).y"
                :x2="pos(edge.targetPersonId).x" :y2="pos(edge.targetPersonId).y"
                stroke="#4f4f6f" stroke-width="1.5" opacity="0.6"/>

          <!-- Person nodes -->
          <g v-for="person in network.persons" :key="person.personId"
             style="cursor:pointer" @click="selectPerson(person)">
            <!-- Score ring -->
            <circle v-if="person.met && person.score > 0"
                    :cx="pos(person.personId).x" :cy="pos(person.personId).y"
                    :r="24"
                    :stroke="groupColor(person.groupId ?? '')"
                    stroke-width="3" fill="none"
                    :stroke-dasharray="`${151 * person.score / 100} 151`"
                    stroke-dashoffset="0"
                    transform-origin="center"
                    :transform="`rotate(-90, ${pos(person.personId).x}, ${pos(person.personId).y})`"
                    opacity="0.9"/>

            <!-- Selection ring -->
            <circle v-if="selectedId === person.personId"
                    :cx="pos(person.personId).x" :cy="pos(person.personId).y"
                    :r="28" stroke="#6366f1" stroke-width="2" fill="none"/>

            <!-- Node circle -->
            <circle :cx="pos(person.personId).x" :cy="pos(person.personId).y"
                    :r="18"
                    :fill="person.met ? groupColor(person.groupId ?? '') + '33' : '#1e1e2e'"
                    :stroke="person.met ? groupColor(person.groupId ?? '') : '#3f3f5f'"
                    :stroke-width="person.locked ? 1.5 : 2"
                    :stroke-dasharray="person.locked ? '3 3' : 'none'"/>

            <!-- Avatar letter or ? -->
            <text :x="pos(person.personId).x" :y="pos(person.personId).y + 5"
                  text-anchor="middle" font-size="12" font-weight="bold"
                  :fill="person.met ? groupColor(person.groupId ?? '') : '#4f4f6f'">
              {{ person.displayName === '???' ? '?' : person.displayName.charAt(0) }}
            </text>

            <!-- Name label -->
            <text :x="pos(person.personId).x" :y="pos(person.personId).y + 36"
                  text-anchor="middle" font-size="8.5"
                  :fill="person.met ? '#e2e8f0' : '#6b7280'">
              {{ person.displayName.length > 14 ? person.displayName.slice(0, 13) + '…' : person.displayName }}
            </text>

            <!-- Lock icon (small) -->
            <text v-if="person.locked"
                  :x="pos(person.personId).x + 12" :y="pos(person.personId).y - 12"
                  font-size="9" fill="#f87171">🔒</text>
          </g>
        </g>
      </svg>
    </div>

    <!-- Detail Panel -->
    <div class="w-80 bg-surface-800 border-l border-white/10 flex flex-col overflow-hidden">
      <!-- Active boosts -->
      <div v-if="network && network.activeBoosts.length" class="p-3 border-b border-white/10">
        <p class="text-xs text-gray-400 font-semibold uppercase tracking-wide mb-2">Aktive Beziehungs-Boosts</p>
        <div class="space-y-1">
          <div v-for="b in network.activeBoosts" :key="b.type" class="flex justify-between text-xs">
            <span class="text-gray-300">{{ boostLabel(b.type) }}</span>
            <span class="text-green-400 font-mono">{{ formatBoostValue(b.type, b.totalValue) }}</span>
          </div>
        </div>
      </div>

      <!-- Person detail -->
      <div v-if="selected" class="flex-1 overflow-y-auto p-4 space-y-4">
        <!-- Header -->
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-full flex items-center justify-center text-lg font-bold flex-shrink-0"
               :style="{ background: groupColor(selected.groupId ?? '') + '33', color: groupColor(selected.groupId ?? '') }">
            {{ selected.displayName === '???' ? '?' : selected.displayName.charAt(0) }}
          </div>
          <div>
            <p class="text-white font-semibold">{{ selected.displayName }}</p>
            <p class="text-xs" :style="{ color: groupColor(selected.groupId ?? '') }">
              {{ groupName(selected.groupId ?? '') }}
            </p>
          </div>
        </div>

        <!-- Score bar -->
        <div v-if="selected.met">
          <div class="flex justify-between text-xs text-gray-400 mb-1">
            <span>Beziehung</span>
            <span>{{ selected.score }} / 100</span>
          </div>
          <div class="h-2 bg-surface-700 rounded-full overflow-hidden">
            <div class="h-full rounded-full transition-all"
                 :style="{ width: selected.score + '%', background: groupColor(selected.groupId ?? '') }"/>
          </div>
        </div>

        <!-- Boost -->
        <div v-if="selected.boost && selected.boost.type !== 'NONE'" class="bg-surface-700 rounded-lg p-3">
          <p class="text-xs text-gray-400 mb-1">Passiver Boost</p>
          <p class="text-green-400 text-sm font-semibold">{{ boostLabel(selected.boost.type) }}: {{ formatBoostValue(selected.boost.type, selected.boost.value) }}</p>
          <p class="text-xs text-gray-500 mt-0.5">{{ selected.score > 0 ? Math.round(selected.score) + '% effektiv' : 'Beziehung aufbauen für Boost' }}</p>
        </div>

        <!-- Unlock requirements -->
        <div v-if="selected.unlockRequirements?.length" class="space-y-1">
          <p class="text-xs text-gray-400 font-semibold">Voraussetzungen</p>
          <p v-for="req in selected.unlockRequirements" :key="req"
             class="text-xs text-red-400 flex items-center gap-1">
            <span>✗</span> {{ req }}
          </p>
        </div>

        <!-- Lock warning -->
        <div v-if="selected.locked" class="bg-red-500/10 border border-red-500/20 rounded-lg p-3">
          <p class="text-red-400 text-xs">Aktionen gesperrt bis Monat {{ selected.lockedUntilTurn }}</p>
        </div>

        <!-- Action buttons -->
        <div v-if="selected.met && !selected.locked" class="space-y-2">
          <button @click="doSpendTime" :disabled="!selected.canSpendTime || actionLoading"
                  class="w-full text-sm py-2 rounded-lg font-medium transition-colors"
                  :class="selected.canSpendTime ? 'bg-indigo-600 hover:bg-indigo-500 text-white' : 'bg-surface-700 text-gray-500 cursor-not-allowed'">
            <span v-if="actionLoading === 'time'">…</span>
            <span v-else-if="!selected.canSpendTime">Zeit verbringen (Limit erreicht)</span>
            <span v-else>Zeit verbringen (+{{ SCORE_TIME }} Score)</span>
          </button>

          <button @click="doGiveGift" :disabled="actionLoading !== null"
                  class="w-full text-sm py-2 rounded-lg font-medium bg-yellow-600/20 hover:bg-yellow-600/30 text-yellow-300 transition-colors border border-yellow-600/20">
            <span v-if="actionLoading === 'gift'">…</span>
            <span v-else>Geschenk (+{{ SCORE_GIFT }} Score)</span>
          </button>

          <button @click="doInsult" :disabled="!selected.canInsult || actionLoading !== null"
                  class="w-full text-sm py-2 rounded-lg font-medium transition-colors"
                  :class="selected.canInsult ? 'bg-orange-600/20 hover:bg-orange-600/30 text-orange-300 border border-orange-600/20' : 'bg-surface-700 text-gray-500 cursor-not-allowed'">
            <span v-if="actionLoading === 'insult'">…</span>
            <span v-else-if="!selected.canInsult">Beleidigt (diesen Monat)</span>
            <span v-else>Beleidigen ({{ SCORE_INSULT }} Score)</span>
          </button>

          <button @click="doRob" :disabled="!selected.canRob || actionLoading !== null"
                  class="w-full text-sm py-2 rounded-lg font-medium transition-colors"
                  :class="selected.canRob ? 'bg-red-600/20 hover:bg-red-600/30 text-red-300 border border-red-600/20' : 'bg-surface-700 text-gray-500 cursor-not-allowed'">
            <span v-if="actionLoading === 'rob'">…</span>
            <span v-else-if="!selected.canRob">Ausgeraubt (diesen Monat)</span>
            <span v-else>Ausrauben (riskant)</span>
          </button>
        </div>

        <!-- Not met yet -->
        <div v-else-if="!selected.met && !selected.unlockRequirements?.length" class="space-y-2">
          <p class="text-xs text-gray-500 italic">Du kennst diese Person noch nicht.</p>
          <button @click="doSpendTime" :disabled="actionLoading !== null"
                  class="w-full text-sm py-2 rounded-lg font-medium bg-indigo-600 hover:bg-indigo-500 text-white transition-colors">
            <span v-if="actionLoading === 'time'">…</span>
            <span v-else>Kennenlernen</span>
          </button>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else class="flex-1 flex items-center justify-center text-gray-600 text-sm p-8 text-center">
        Klicke auf eine Person im Graph um Details zu sehen.
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'default' })

const api = useApi()
const toast = useToastStore()

const SCORE_TIME = 8
const SCORE_GIFT = 20
const SCORE_INSULT = -15
const SVG_W = 900
const SVG_H = 680

interface BoostDef { type: string; value: number }
interface PersonNode {
  personId: string
  displayName: string
  groupId: string | null
  met: boolean
  known: boolean
  score: number
  locked: boolean
  lockedUntilTurn: number
  canSpendTime: boolean
  canInsult: boolean
  canRob: boolean
  boost: BoostDef | null
  unlockRequirements: string[]
}
interface PersonEdge { sourcePersonId: string; targetPersonId: string }
interface GroupDto { id: string; name: string; type: string }
interface ActiveBoostDto { type: string; totalValue: number }
interface SocialNetwork {
  persons: PersonNode[]
  edges: PersonEdge[]
  unlockedGroups: GroupDto[]
  activeBoosts: ActiveBoostDto[]
}

const network = ref<SocialNetwork | null>(null)
const loading = ref(true)
const selectedId = ref<string | null>(null)
const actionLoading = ref<string | null>(null)

const selected = computed(() =>
  network.value?.persons.find(p => p.personId === selectedId.value) ?? null
)

const edges = computed(() => {
  if (!network.value) return []
  const ids = new Set(network.value.persons.map(p => p.personId))
  return network.value.edges.filter(e => ids.has(e.sourcePersonId) && ids.has(e.targetPersonId))
})

const visibleGroups = computed(() => {
  if (!network.value) return []
  const groupIds = new Set(network.value.persons.flatMap(p => p.groupId ? [p.groupId] : []))
  return (network.value.unlockedGroups ?? []).filter(g => groupIds.has(g.id))
})

// --- Graph layout ---

const GROUP_CENTERS: Record<string, { x: number; y: number }> = {
  NACHBARSCHAFT:    { x: 150, y: 175 },
  GASTRONOMEN:      { x: 195, y: 380 },
  AKADEMIKER:       { x: 420, y: 195 },
  GESCHAEFTSLEUTE:  { x: 605, y: 230 },
  INVESTOREN:       { x: 710, y: 420 },
  KRIMINELLE:       { x: 175, y: 545 },
  REISELUSTIGE:     { x: 460, y: 495 },
  ELITE:            { x: 790, y: 140 },
}

function groupRadius(gid: string) {
  return { ELITE: 65, REISELUSTIGE: 65, KRIMINELLE: 70 }[gid] ?? 80
}

const PERSON_POSITIONS: Record<string, { x: number; y: number }> = {
  HANS_MUELLER:           { x: 155, y: 175 },
  GRETA_SCHMIDT:          { x: 80,  y: 150 },
  RENATE_BAUER:           { x: 78,  y: 215 },
  KARL_WEBER:             { x: 200, y: 285 },
  JUERGEN_VOGEL:          { x: 185, y: 115 },

  OSMAN_DEMIR:            { x: 155, y: 415 },
  YUKI_TANAKA:            { x: 120, y: 360 },
  PETRA_KURZ:             { x: 200, y: 450 },
  SARAH_HOFFMANN:         { x: 308, y: 295 },

  DR_MUELLER:             { x: 510, y: 215 },
  PROF_FISCHER:           { x: 395, y: 140 },
  LISA_KRAUSE:            { x: 515, y: 270 },
  THOMAS_BRAUN:           { x: 515, y: 165 },
  ANNA_WOLF:              { x: 370, y: 255 },

  STEFAN_BRANDT:          { x: 640, y: 185 },
  FRANK_ZIMMERMANN:       { x: 660, y: 255 },
  MONIKA_SCHAEFER:        { x: 590, y: 290 },
  DIRK_HOFFMANN:          { x: 385, y: 425 },
  SANDRA_KOCH:            { x: 625, y: 160 },

  MARCO_RICHTER:          { x: 680, y: 365 },
  CLARA_RICHTER:          { x: 745, y: 385 },
  VIKTOR_LEHMANN:         { x: 435, y: 490 },
  PETRA_STERN:            { x: 715, y: 460 },

  RALF_RATTE:             { x: 155, y: 510 },
  TINA_SCHWARZ:           { x: 220, y: 565 },
  BOGDAN_NOWAK:           { x: 140, y: 580 },
  UNDERWORLD_KONTAKT:     { x: 190, y: 630 },

  LENA_BERG:              { x: 445, y: 470 },
  AHMED_KARIMI:           { x: 495, y: 530 },
  SOFIA_MARTINEZ:         { x: 585, y: 455 },

  SIR_REGINALD:           { x: 775, y: 115 },
  ISABELLA_VON_HOHENSTEIN:{ x: 830, y: 160 },
  DR_MAXIMILIAN_FURST:    { x: 775, y: 180 },
}

function groupCenter(gid: string) {
  return GROUP_CENTERS[gid] ?? { x: 450, y: 340 }
}

function pos(personId: string) {
  return PERSON_POSITIONS[personId] ?? { x: 450, y: 340 }
}

const GROUP_COLORS: Record<string, string> = {
  NACHBARSCHAFT:   '#4ade80',
  GASTRONOMEN:     '#fb923c',
  AKADEMIKER:      '#60a5fa',
  GESCHAEFTSLEUTE: '#22d3ee',
  INVESTOREN:      '#fbbf24',
  KRIMINELLE:      '#f87171',
  REISELUSTIGE:    '#2dd4bf',
  ELITE:           '#c084fc',
}

function groupColor(gid: string) {
  return GROUP_COLORS[gid] ?? '#6366f1'
}

function groupName(gid: string) {
  return network.value?.unlockedGroups.find(g => g.id === gid)?.name ?? gid
}

// --- Pan & Zoom ---

const graphContainer = ref<HTMLElement | null>(null)
const panX = ref(0)
const panY = ref(0)
const scale = ref(1)
let panning = ref(false)
let lastMouse = { x: 0, y: 0 }

function onPanStart(e: MouseEvent) {
  panning.value = true
  lastMouse = { x: e.clientX, y: e.clientY }
}
function onPanMove(e: MouseEvent) {
  if (!panning.value) return
  panX.value += e.clientX - lastMouse.x
  panY.value += e.clientY - lastMouse.y
  lastMouse = { x: e.clientX, y: e.clientY }
}
function onPanEnd() { panning.value = false }
function onWheel(e: WheelEvent) {
  const delta = e.deltaY > 0 ? 0.9 : 1.1
  scale.value = Math.max(0.4, Math.min(3, scale.value * delta))
}
function zoom(factor: number) {
  scale.value = Math.max(0.4, Math.min(3, scale.value * factor))
}
function resetView() { panX.value = 0; panY.value = 0; scale.value = 1 }

// --- Actions ---

function selectPerson(p: PersonNode) { selectedId.value = p.personId }

async function doSpendTime() {
  if (!selected.value) return
  actionLoading.value = 'time'
  try {
    const res = await api.post<{ newScore: number; message: string }>(
      `/api/social/persons/${selected.value.personId}/time`)
    toast.success(res.message)
    await refresh()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler')
  } finally { actionLoading.value = null }
}

async function doGiveGift() {
  if (!selected.value) return
  actionLoading.value = 'gift'
  try {
    const res = await api.post<{ newScore: number; message: string }>(
      `/api/social/persons/${selected.value.personId}/gift`)
    toast.success(res.message)
    await refresh()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler')
  } finally { actionLoading.value = null }
}

async function doInsult() {
  if (!selected.value) return
  actionLoading.value = 'insult'
  try {
    const res = await api.post<{ newScore: number; message: string }>(
      `/api/social/persons/${selected.value.personId}/insult`)
    toast.warning(res.message)
    await refresh()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler')
  } finally { actionLoading.value = null }
}

async function doRob() {
  if (!selected.value) return
  actionLoading.value = 'rob'
  try {
    const res = await api.post<{ success: boolean; caught: boolean; lootAmount: number; message: string }>(
      `/api/social/persons/${selected.value.personId}/rob`)
    if (res.caught) toast.error(res.message)
    else if (res.success) toast.success(res.message)
    else toast.info(res.message)
    await refresh()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler')
  } finally { actionLoading.value = null }
}

async function refresh() {
  const data = await api.get<SocialNetwork>('/api/social/network')
  network.value = data
  if (selectedId.value) {
    const still = data.persons.find(p => p.personId === selectedId.value)
    if (!still) selectedId.value = null
  }
}

// --- Boost formatting ---

const BOOST_LABELS: Record<string, string> = {
  HAPPINESS_PER_TURN:       'Happiness/Monat',
  STRESS_REDUCTION_PER_TURN:'Stress-Reduktion/Monat',
  ENERGY_BONUS_PER_TURN:    'Energie/Monat',
  HUNGER_DECAY_REDUCTION:   'Hunger-Decay −',
  SALARY_BONUS:             'Gehalts-Bonus',
  SALARY_MULTIPLIER_BONUS:  'Gehalts-Multiplikator',
  EXPENSE_REDUCTION:        'Ausgaben-Reduktion',
  LOAN_INTEREST_REDUCTION:  'Kredit-Zinsen −',
  PROPERTY_PRICE_DISCOUNT:  'Immobilien-Rabatt',
  COLLECTIBLE_PRICE_DISCOUNT:'Sammlerstück-Rabatt',
  COLLECTIBLE_DROP_RATE_BOOST:'Sammlerstück-Fundchance',
  STOCK_VOLATILITY_REDUCTION:'Aktien-Volatilität −',
  TRAVEL_COST_REDUCTION:    'Reisekosten −',
  TRAVEL_DURATION_REDUCTION:'Reisedauer −',
  GAMBLING_LUCK_BOOST:      'Glücksspiel-Glück',
  TAX_DETECTION_REDUCTION:  'Entdeckungsrisiko −',
  JOB_ACCEPTANCE_BOOST:     'Job-Bewerbung +',
  SCHUFA_BONUS_MONTHLY:     'SCHUFA/Monat',
  ROB_SUCCESS_BOOST:        'Ausrauben-Chance',
  ROB_LOOT_MULTIPLIER:      'Beute-Multiplikator',
  MONTHLY_INCOME_BONUS:     'Passiveinkommen/Monat',
}

function boostLabel(type: string) {
  return BOOST_LABELS[type] ?? type
}

function formatBoostValue(type: string, val: number) {
  if (['EXPENSE_REDUCTION','LOAN_INTEREST_REDUCTION','PROPERTY_PRICE_DISCOUNT',
       'COLLECTIBLE_PRICE_DISCOUNT','STOCK_VOLATILITY_REDUCTION','TRAVEL_COST_REDUCTION',
       'TAX_DETECTION_REDUCTION','JOB_ACCEPTANCE_BOOST','GAMBLING_LUCK_BOOST',
       'ROB_SUCCESS_BOOST','COLLECTIBLE_DROP_RATE_BOOST'].includes(type)) {
    return (val * 100).toFixed(1) + '%'
  }
  if (type === 'SALARY_MULTIPLIER_BONUS' || type === 'ROB_LOOT_MULTIPLIER') {
    return '+' + (val * 100).toFixed(1) + '%'
  }
  if (type === 'TRAVEL_DURATION_REDUCTION') return val.toFixed(1) + ' Monate'
  if (type === 'SALARY_BONUS' || type === 'MONTHLY_INCOME_BONUS') return '+' + val.toFixed(0) + '€'
  return '+' + val.toFixed(1)
}

onMounted(async () => {
  try {
    network.value = await api.get<SocialNetwork>('/api/social/network')
  } finally {
    loading.value = false
  }
})
</script>
