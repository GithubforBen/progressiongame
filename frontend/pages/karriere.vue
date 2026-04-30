<template>
  <div class="-m-3 sm:-m-6 flex" style="height: calc(100vh - 3.5rem)">

    <!-- Viewport -->
    <div
      ref="viewportEl"
      class="flex-1 relative overflow-hidden"
      :class="isDragging ? 'cursor-grabbing' : 'cursor-grab'"
      @mousedown="startPan"
      @mousemove="doPan"
      @mouseup="stopPan"
      @mouseleave="stopPan"
      @wheel.prevent="doZoom"
      @click="selectedJobName = null"
    >
      <!-- Canvas -->
      <div
        class="absolute origin-top-left will-change-transform"
        :style="{ transform: `translate(${panX}px,${panY}px) scale(${zoom})`, width: CANVAS_W + 'px', height: CANVAS_H + 'px' }"
      >
        <!-- SVG edges -->
        <svg class="absolute inset-0 pointer-events-none" :width="CANVAS_W" :height="CANVAS_H" overflow="visible">
          <path
            v-for="e in allEdges"
            :key="e.id"
            :d="e.d"
            :stroke="e.color"
            stroke-width="1.5"
            fill="none"
            stroke-linecap="round"
          />
        </svg>

        <!-- Education milestone nodes -->
        <div
          v-for="edu in EDU_NODES"
          :key="edu.id"
          class="absolute flex flex-col items-center justify-center rounded-lg border border-surface-600 bg-surface-800/80 pointer-events-none select-none"
          :style="{ left: `${edu.x - EDU_W/2}px`, top: `${edu.y - EDU_H/2}px`, width: `${EDU_W}px`, height: `${EDU_H}px` }"
        >
          <div class="text-xs font-semibold text-gray-400 text-center leading-tight px-1">{{ edu.label }}</div>
          <div v-if="edu.sublabel" class="text-xs text-gray-600 text-center leading-tight px-1">{{ edu.sublabel }}</div>
        </div>

        <!-- Job nodes -->
        <div
          v-for="node in jobNodes"
          :key="node.name"
          class="absolute flex flex-col items-center justify-center rounded-lg border transition-colors duration-150 select-none"
          :class="nodeClasses(node)"
          :style="{ left: `${node.x - NODE_W/2}px`, top: `${node.y - NODE_H/2}px`, width: `${NODE_W}px`, height: `${NODE_H}px` }"
          @click.stop="handleJobClick(node.name)"
        >
          <div class="text-xs font-medium leading-tight px-2 w-full text-center truncate">{{ node.name }}</div>
          <div v-if="node.job" class="text-xs font-mono leading-tight opacity-60 mt-0.5">{{ formatCurrency(node.job.salary) }}</div>
        </div>
      </div>

      <!-- Active jobs overlay -->
      <div v-if="activeJobs.length && !jobsLoading" class="absolute top-3 left-3 z-30 pointer-events-none" style="max-width:230px">
        <div class="bg-surface-800/95 backdrop-blur border border-surface-700 rounded-xl px-3 py-2.5 shadow-xl">
          <p class="text-xs text-gray-500 uppercase tracking-wider mb-2">Meine Jobs</p>
          <div class="space-y-1.5">
            <div v-for="pj in activeJobs" :key="pj.jobId" class="flex items-center justify-between gap-2">
              <p class="text-xs text-gray-300 truncate">{{ pj.jobName }}</p>
              <span class="text-xs font-mono text-green-400 flex-shrink-0">{{ formatCurrency(pj.salary) }}</span>
            </div>
          </div>
          <div class="mt-2 pt-2 border-t border-surface-600 flex justify-between">
            <span class="text-xs text-gray-500">Gesamt</span>
            <span class="text-xs font-mono font-semibold text-green-400">{{ formatCurrency(totalSalary) }}</span>
          </div>
        </div>
      </div>

      <!-- Controls -->
      <div class="absolute bottom-4 left-4 flex items-center gap-2 z-20">
        <button class="bg-surface-800/90 border border-surface-700 text-gray-400 hover:text-white text-xs px-3 py-1.5 rounded transition-colors" @click.stop="resetView">
          ⌂ Startansicht
        </button>
        <span class="bg-surface-900/70 text-gray-600 text-xs px-2 py-1 rounded font-mono">{{ Math.round(zoom * 100) }}%</span>
        <span class="text-gray-700 text-xs hidden lg:block">Ziehen · Scroll = Zoom</span>
      </div>

      <!-- Loading -->
      <div v-if="jobsLoading" class="absolute inset-0 flex items-center justify-center bg-surface-900/80 z-50">
        <span class="text-gray-400 text-sm animate-pulse">Lade Karriere-Baum…</span>
      </div>
    </div>

    <!-- Detail panel -->
    <Transition
      enter-from-class="opacity-0 translate-x-8"
      enter-active-class="transition-all duration-200 ease-out"
      leave-to-class="opacity-0 translate-x-8"
      leave-active-class="transition-all duration-150 ease-in"
    >
      <div
        v-if="selectedDetail"
        class="w-72 flex-shrink-0 bg-surface-800 border-l border-surface-700 flex flex-col overflow-y-auto z-40"
        @mousedown.stop
        @click.stop
      >
        <div class="flex items-start justify-between p-4 border-b border-surface-700 gap-2">
          <div class="flex-1 min-w-0">
            <p class="text-white font-semibold text-sm leading-snug">{{ selectedDetail.name }}</p>
            <span class="badge mt-1.5 inline-block" :class="categoryBadge(selectedDetail.category)">
              {{ categoryLabels[selectedDetail.category] ?? selectedDetail.category }}
            </span>
          </div>
          <button class="text-gray-500 hover:text-white text-xl leading-none flex-shrink-0 mt-0.5" @click="selectedJobName = null">×</button>
        </div>

        <div class="flex-1 p-4 space-y-4">
          <!-- Status -->
          <span class="inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium border"
            :class="{
              'bg-green-500/15 border-green-500/30 text-green-400':  selectedDetail.state === 'active',
              'bg-yellow-500/15 border-yellow-500/30 text-yellow-400': selectedDetail.state === 'applied',
              'bg-accent/15 border-accent/30 text-accent':            selectedDetail.state === 'available',
              'bg-surface-700 border-white/5 text-gray-500':          selectedDetail.state === 'locked',
            }"
          >{{ STATE_LABELS[selectedDetail.state] }}</span>

          <!-- Description -->
          <p v-if="selectedDetail.description" class="text-xs text-gray-500 leading-relaxed">{{ selectedDetail.description }}</p>

          <!-- Salary & stress card -->
          <div class="rounded-lg bg-surface-700/60 border border-surface-600/50 px-3 py-2.5 space-y-1.5">
            <div class="flex justify-between items-center">
              <span class="text-xs text-gray-500">Gehalt/Mo.</span>
              <span class="text-base font-bold font-mono text-white">{{ formatCurrency(selectedDetail.salary) }}</span>
            </div>
            <div v-if="selectedDetail.salaryDelta !== null" class="flex justify-between items-center">
              <span class="text-xs text-gray-500">vs. aktuell</span>
              <span class="text-xs font-semibold font-mono"
                :class="selectedDetail.salaryDelta > 0 ? 'text-green-400' : selectedDetail.salaryDelta < 0 ? 'text-red-400' : 'text-gray-400'">
                {{ selectedDetail.salaryDelta > 0 ? '▲ +' : selectedDetail.salaryDelta < 0 ? '▼ ' : '→ ' }}{{ formatCurrency(Math.abs(selectedDetail.salaryDelta)) }}
              </span>
            </div>
            <div class="flex justify-between items-center">
              <span class="text-xs text-gray-500">Stress</span>
              <span class="text-xs font-medium" :class="stressColor(selectedDetail.stressPerMonth)">{{ stressLabel(selectedDetail.stressPerMonth) }}</span>
            </div>
          </div>

          <!-- Requirements -->
          <div v-if="selectedDetail.hasReqs" class="space-y-1.5">
            <p class="text-xs text-gray-500 uppercase tracking-wider">Voraussetzungen</p>
            <div v-if="selectedDetail.requiredEducationType" class="flex items-start gap-1.5">
              <span class="text-sm flex-shrink-0">📚</span>
              <span class="text-xs text-gray-300">{{ formatEducationRequirement(selectedDetail.requiredEducationType, selectedDetail.requiredEducationField) }}</span>
            </div>
            <div v-if="selectedDetail.requiredSideCert" class="flex items-start gap-1.5">
              <span class="text-sm flex-shrink-0">🎓</span>
              <span class="text-xs text-gray-300">{{ selectedDetail.requiredSideCert }}</span>
            </div>
            <div v-if="selectedDetail.requiredMonthsExperience > 0" class="flex items-start gap-1.5">
              <span class="text-sm flex-shrink-0">⏱</span>
              <span class="text-xs text-gray-300">{{ selectedDetail.requiredMonthsExperience }} Monate Erfahrung</span>
            </div>
          </div>

          <!-- Action -->
          <div class="pt-1">
            <button v-if="selectedDetail.state === 'active'"
              class="btn-danger w-full text-sm"
              :disabled="quittingId === selectedDetail.id"
              @click="quit(selectedDetail.id)">
              {{ quittingId === selectedDetail.id ? '…' : 'Kündigen' }}
            </button>
            <button v-else-if="selectedDetail.state === 'applied'"
              class="btn-secondary w-full text-sm opacity-50 cursor-not-allowed" disabled>
              Bewerbung läuft…
            </button>
            <button v-else-if="selectedDetail.state === 'available'"
              class="btn-primary w-full text-sm"
              :disabled="applyingId === selectedDetail.id"
              @click="apply(selectedDetail.id)">
              {{ applyingId === selectedDetail.id ? '…' : 'Bewerben' }}
            </button>
            <p v-else class="text-xs text-gray-600 text-center py-1">Voraussetzungen nicht erfüllt</p>
          </div>

          <!-- Tip -->
          <div v-if="selectedDetail.state === 'locked' && selectedDetail.requiredEducationType" class="rounded-lg bg-surface-700/40 border border-surface-600/40 px-3 py-2 text-xs text-gray-500">
            Schließe zuerst die erforderliche Ausbildung unter <span class="text-accent">Ausbildung</span> ab.
          </div>
          <div v-else-if="selectedDetail.state === 'locked' && selectedDetail.requiredSideCert" class="rounded-lg bg-surface-700/40 border border-surface-600/40 px-3 py-2 text-xs text-gray-500">
            Absolviere zuerst die Weiterbildung unter <span class="text-accent">Ausbildung</span>.
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { useApi } from '~/composables/useApi'
import { useFormatting } from '~/composables/useFormatting'
import { useToastStore } from '~/stores/toast'

definePageMeta({ layout: 'default' })

const api = useApi()
const toast = useToastStore()
const { formatCurrency, formatEducationRequirement, stressLabel, stressColor } = useFormatting()

// ── Types ──────────────────────────────────────────────────────────────────────
interface Job {
  id: number
  name: string
  description: string | null
  category: string
  requiredEducationType: string | null
  requiredEducationField: string | null
  requiredSideCert: string | null
  requiredMonthsExperience: number
  salary: number
  stressPerMonth: number
  maxParallel: number
  meetsRequirements: boolean
  alreadyApplied: boolean
  alreadyWorking: boolean
}

interface PlayerJob {
  jobId: number
  jobName: string
  salary: number
  stressPerMonth: number
  monthsWorked: number
  startedAtTurn: number
}

type NodeState = 'active' | 'applied' | 'available' | 'locked'

interface JobNode {
  name: string
  x: number
  y: number
  job: Job | null
  state: NodeState
}

// ── Canvas constants ───────────────────────────────────────────────────────────
const CANVAS_W = 1840
const CANVAS_H = 1420
const NODE_W = 150
const NODE_H = 50
const EDU_W = 128
const EDU_H = 56
const EDU_X = 160
const JOB_X0 = 375
const JOB_STEP = 185

// ── Education milestones ───────────────────────────────────────────────────────
const EDU_NODES = [
  { id: 'KEIN', label: 'Kein',        sublabel: 'Abschluss',    x: EDU_X, y: 165  },
  { id: 'REAL', label: 'Realschul-',  sublabel: 'abschluss',    x: EDU_X, y: 430  },
  { id: 'AUS',  label: 'Ausbildung',  sublabel: '(verschiedene)', x: EDU_X, y: 695  },
  { id: 'BACH', label: 'Bachelor',    sublabel: '(verschiedene)', x: EDU_X, y: 970  },
  { id: 'MAST', label: 'Master',      sublabel: '(verschiedene)', x: EDU_X, y: 1280 },
]

// ── Job layout rows ────────────────────────────────────────────────────────────
const ROW_DEFS: Array<{ y: number; eduId: string; names: string[] }> = [
  { y: 115, eduId: 'KEIN', names: ['Zeitungsausträger', 'Supermarkt-Kassierer', 'Lagerhelfer', 'Reinigungskraft', 'Kellner', 'Babysitter', 'Nachhilfelehrer', 'Fitnessstudio-Mitarbeiter'] },
  { y: 215, eduId: 'KEIN', names: ['Pizzabote', 'Barkeeper', 'Personal Trainer'] },
  { y: 430, eduId: 'REAL', names: ['Bürokaufmann', 'Content Creator', 'Social-Media-Manager', 'Buchhalter', 'Immobilienmakler', 'Steuerfachangestellter'] },
  { y: 640, eduId: 'AUS',  names: ['Verkäufer im Einzelhandel', 'Filialleiter-Assistent', 'KFZ-Mechatroniker', 'KFZ-Meister', 'Koch', 'Küchenchef'] },
  { y: 750, eduId: 'AUS',  names: ['Elektriker', 'Elektromeister', 'Pflegefachkraft', 'Stationsleitung', 'IT-Support', 'Systemadministrator'] },
  { y: 920, eduId: 'BACH', names: ['Junior Developer', 'Software Engineer', 'Senior Developer', 'Data Scientist', 'Cybersecurity Analyst', 'IT-Projektleiter', 'Maschinenbauingenieur'] },
  { y: 1020, eduId: 'BACH', names: ['Marketing-Assistent', 'Produktmanager', 'Unternehmensberater', 'Abteilungsleiter'] },
  { y: 1120, eduId: 'BACH', names: ['Arzt (Assistenzarzt)', 'Psychologe', 'Psychotherapeut', 'Rechtsreferendar'] },
  { y: 1280, eduId: 'MAST', names: ['Leitender Ingenieur', 'Geschäftsführer (extern)', 'Facharzt', 'Chefarzt', 'Rechtsanwalt', 'Fachanwalt', 'Richter'] },
]

// Build static position map
const STATIC_POS: Record<string, { x: number; y: number }> = {}
for (const row of ROW_DEFS) {
  for (let i = 0; i < row.names.length; i++) {
    STATIC_POS[row.names[i]] = { x: JOB_X0 + i * JOB_STEP, y: row.y }
  }
}

// Edu → rows map (for edges)
const EDU_ROW_MAP: Record<string, number[]> = {}
for (const row of ROW_DEFS) {
  if (!EDU_ROW_MAP[row.eduId]) EDU_ROW_MAP[row.eduId] = []
  if (!EDU_ROW_MAP[row.eduId].includes(row.y)) EDU_ROW_MAP[row.eduId].push(row.y)
}

// ── Labels & colors ────────────────────────────────────────────────────────────
const categoryLabels: Record<string, string> = {
  EINSTIEG: 'Einstieg', HANDWERK: 'Handwerk', BUERO: 'Büro',
  TECH: 'Tech', MANAGEMENT: 'Management', GESUNDHEIT: 'Gesundheit', RECHT: 'Recht', KREATIV: 'Kreativ',
}

function categoryBadge(cat: string): string {
  const map: Record<string, string> = {
    EINSTIEG:   'bg-gray-500/15 text-gray-400',
    HANDWERK:   'bg-orange-500/15 text-orange-400',
    BUERO:      'bg-blue-500/15 text-blue-400',
    TECH:       'bg-cyan-500/15 text-cyan-400',
    MANAGEMENT: 'bg-purple-500/15 text-purple-400',
    GESUNDHEIT: 'bg-green-500/15 text-green-400',
    RECHT:      'bg-yellow-500/15 text-yellow-400',
    KREATIV:    'bg-pink-500/15 text-pink-400',
  }
  return map[cat] ?? 'bg-surface-600 text-gray-400'
}

const STATE_LABELS: Record<string, string> = {
  active: '✓ Aktiv', applied: '⏳ Beworben', available: 'Verfügbar', locked: '🔒 Gesperrt',
}

// ── Reactive state ─────────────────────────────────────────────────────────────
const availableJobs = ref<Job[]>([])
const activeJobs = ref<PlayerJob[]>([])
const jobsLoading = ref(true)
const applyingId = ref<number | null>(null)
const quittingId = ref<number | null>(null)
const selectedJobName = ref<string | null>(null)

const panX = ref(0), panY = ref(0), zoom = ref(0.75)
const isDragging = ref(false), hasDragged = ref(false)
let lastMX = 0, lastMY = 0
const viewportEl = ref<HTMLElement | null>(null)

// ── Computed ───────────────────────────────────────────────────────────────────
const jobByName = computed(() => {
  const m: Record<string, Job> = {}
  for (const j of availableJobs.value) m[j.name] = j
  return m
})

const totalSalary = computed(() => activeJobs.value.reduce((s, j) => s + j.salary, 0))

const maxActiveSalary = computed(() => activeJobs.value.reduce((m, j) => Math.max(m, j.salary), 0))

const jobNodes = computed((): JobNode[] =>
  ROW_DEFS.flatMap(row =>
    row.names.map(name => {
      const pos = STATIC_POS[name]
      const job = jobByName.value[name] ?? null
      let state: NodeState = 'locked'
      if (job) {
        if (job.alreadyWorking) state = 'active'
        else if (job.alreadyApplied) state = 'applied'
        else if (job.meetsRequirements) state = 'available'
      }
      return { name, x: pos.x, y: pos.y, job, state }
    })
  )
)

const selectedDetail = computed(() => {
  if (!selectedJobName.value) return null
  const node = jobNodes.value.find(n => n.name === selectedJobName.value)
  if (!node || !node.job) return null
  const job = node.job
  return {
    id: job.id,
    name: job.name,
    description: job.description,
    category: job.category,
    salary: job.salary,
    salaryDelta: (activeJobs.value.length && !job.alreadyWorking)
      ? job.salary - maxActiveSalary.value
      : null,
    stressPerMonth: job.stressPerMonth,
    requiredEducationType: job.requiredEducationType,
    requiredEducationField: job.requiredEducationField,
    requiredSideCert: job.requiredSideCert,
    requiredMonthsExperience: job.requiredMonthsExperience,
    hasReqs: !!(job.requiredEducationType || job.requiredSideCert || job.requiredMonthsExperience),
    state: node.state,
  }
})

// ── Edges ──────────────────────────────────────────────────────────────────────
const allEdges = computed(() => {
  const edges: Array<{ id: string; d: string; color: string }> = []
  const BRANCH_X = EDU_X + EDU_W / 2 + 22

  // Vertical edu chain
  for (let i = 0; i < EDU_NODES.length - 1; i++) {
    const a = EDU_NODES[i], b = EDU_NODES[i + 1]
    edges.push({ id: `chain_${i}`, d: `M${EDU_X} ${a.y + EDU_H / 2 + 2}V${b.y - EDU_H / 2 - 2}`, color: 'rgba(255,255,255,0.06)' })
  }

  // Branches from edu node to row(s)
  for (const edu of EDU_NODES) {
    const rows = EDU_ROW_MAP[edu.id] ?? []
    if (!rows.length) continue
    const minY = Math.min(...rows), maxY = Math.max(...rows)
    const eduRight = EDU_X + EDU_W / 2

    edges.push({ id: `h_${edu.id}`, d: `M${eduRight} ${edu.y}H${BRANCH_X}`, color: 'rgba(255,255,255,0.06)' })
    if (rows.length > 1)
      edges.push({ id: `v_${edu.id}`, d: `M${BRANCH_X} ${minY}V${maxY}`, color: 'rgba(255,255,255,0.06)' })
    for (const ry of rows)
      edges.push({ id: `br_${edu.id}_${ry}`, d: `M${BRANCH_X} ${ry}H${JOB_X0 - 14}`, color: 'rgba(255,255,255,0.06)' })
  }

  return edges
})

// ── Node classes ───────────────────────────────────────────────────────────────
function nodeClasses(node: JobNode) {
  const sel = selectedJobName.value === node.name
  return [
    node.state === 'active'    ? 'bg-green-500/15 border-green-500/35 text-green-300 cursor-pointer' :
    node.state === 'applied'   ? 'bg-yellow-500/10 border-yellow-500/30 text-yellow-300 cursor-pointer' :
    node.state === 'available' ? 'bg-accent/10 border-accent/35 text-accent hover:bg-accent/20 cursor-pointer node-pulse' :
                                 'bg-surface-700/60 border-white/5 text-gray-600 cursor-pointer',
    sel ? 'ring-2 ring-white/25' : '',
  ]
}

// ── Pan / zoom ─────────────────────────────────────────────────────────────────
function startPan(e: MouseEvent) {
  isDragging.value = true; hasDragged.value = false
  lastMX = e.clientX; lastMY = e.clientY
}

function doPan(e: MouseEvent) {
  if (!isDragging.value) return
  const dx = e.clientX - lastMX, dy = e.clientY - lastMY
  if (Math.abs(dx) > 4 || Math.abs(dy) > 4) hasDragged.value = true
  panX.value += dx; panY.value += dy
  lastMX = e.clientX; lastMY = e.clientY
}

function stopPan() { isDragging.value = false }

function doZoom(e: WheelEvent) {
  const delta = e.deltaY > 0 ? -0.07 : 0.07
  const newZoom = Math.max(0.2, Math.min(2.5, zoom.value + delta))
  const el = viewportEl.value
  if (!el) { zoom.value = newZoom; return }
  const rect = el.getBoundingClientRect()
  const ratio = newZoom / zoom.value
  panX.value = (e.clientX - rect.left) - ratio * ((e.clientX - rect.left) - panX.value)
  panY.value = (e.clientY - rect.top)  - ratio * ((e.clientY - rect.top)  - panY.value)
  zoom.value = newZoom
}

function handleJobClick(name: string) {
  if (hasDragged.value) return
  selectedJobName.value = selectedJobName.value === name ? null : name
}

function resetView() {
  zoom.value = 0.75
  const el = viewportEl.value
  if (!el) return
  panX.value = el.clientWidth / 2 - 900 * zoom.value
  panY.value = el.clientHeight / 2 - 700 * zoom.value
}

// ── API actions ────────────────────────────────────────────────────────────────
async function loadAll() {
  jobsLoading.value = true
  try {
    const [jobs, myJobs] = await Promise.all([
      api.get<Job[]>('/api/jobs'),
      api.get<PlayerJob[]>('/api/jobs/my'),
    ])
    availableJobs.value = Array.isArray(jobs) ? jobs : []
    activeJobs.value = Array.isArray(myJobs) ? myJobs : []
  } finally {
    jobsLoading.value = false
    await nextTick()
    resetView()
  }
}

async function apply(jobId: number) {
  applyingId.value = jobId
  try {
    await api.post('/api/jobs/' + jobId + '/apply')
    toast.success('Bewerbung eingereicht! Ergebnis kommt nächsten Monat.', 'Beworben')
    await loadAll()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Bewerbung fehlgeschlagen.')
  } finally {
    applyingId.value = null
  }
}

async function quit(jobId: number) {
  quittingId.value = jobId
  try {
    await api.del('/api/jobs/' + jobId + '/quit')
    toast.info('Job erfolgreich gekündigt.', 'Gekündigt')
    selectedJobName.value = null
    await loadAll()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Kündigung fehlgeschlagen.')
  } finally {
    quittingId.value = null
  }
}

onMounted(loadAll)
</script>

<style scoped>
@keyframes node-glow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.35); }
  50%       { box-shadow: 0 0 0 8px rgba(99, 102, 241, 0); }
}
.node-pulse { animation: node-glow 2.2s ease-in-out infinite; }
</style>
