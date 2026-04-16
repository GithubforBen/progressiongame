<template>
  <div class="space-y-5">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-bold text-white">Karriere</h2>
      <div class="text-xs text-gray-500">
        Aktive Jobs: <span class="text-white font-semibold">{{ activeJobs.length }}</span>
        &nbsp;·&nbsp;
        Monatliches Gehalt: <span class="text-green-400 font-semibold font-mono">{{ formatCurrency(totalSalary) }}</span>
      </div>
    </div>

    <div class="grid grid-cols-1 xl:grid-cols-3 gap-5">
      <!-- ── Left: Job browser ── -->
      <div class="xl:col-span-2 space-y-3">
        <!-- Filter bar -->
        <div class="flex items-center flex-wrap gap-1.5">
          <button
            v-for="f in modeFilters"
            :key="f.value"
            class="px-2.5 py-1 rounded text-xs font-medium transition-colors"
            :class="activeFilter === f.value
              ? 'bg-accent text-white'
              : 'bg-surface-700 text-gray-400 hover:text-white'"
            @click="activeFilter = f.value"
          >
            {{ f.label }}
          </button>

          <span class="h-4 w-px bg-surface-600 mx-0.5"></span>

          <button
            v-for="cat in categoryFilters"
            :key="cat.value"
            class="px-2.5 py-1 rounded text-xs font-medium transition-colors"
            :class="activeFilter === cat.value
              ? 'bg-accent/80 text-white'
              : 'bg-surface-700 text-gray-400 hover:text-white'"
            @click="activeFilter = cat.value"
          >
            {{ cat.label }}
          </button>
        </div>

        <!-- Loading -->
        <div v-if="jobsLoading" class="card flex items-center justify-center h-32 text-gray-600 text-sm">
          Lade Stellenangebote…
        </div>

        <!-- Grouped view (Alle) -->
        <template v-else-if="activeFilter === 'alle'">
          <div v-for="group in jobGroups" :key="group.category" class="space-y-2">
            <div class="flex items-center gap-2 pt-1">
              <span class="text-xs font-semibold uppercase tracking-wider" :class="categoryColor(group.category)">
                {{ group.label }}
              </span>
              <span class="text-xs text-gray-600">{{ group.jobs.length }} Stelle{{ group.jobs.length !== 1 ? 'n' : '' }}</span>
            </div>
            <div
              v-for="job in group.jobs"
              :key="job.id"
              class="card transition-colors"
              :class="job.alreadyWorking ? 'border-green-600/40' : job.meetsRequirements ? '' : 'opacity-60'"
            >
              <div class="flex items-start justify-between gap-4">
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2 flex-wrap">
                    <h4 class="text-sm font-semibold text-white">{{ job.name }}</h4>
                    <span v-if="job.alreadyWorking" class="badge bg-green-500/15 text-green-400">Aktiv</span>
                    <span v-else-if="job.alreadyApplied" class="badge bg-yellow-500/15 text-yellow-400">Beworben</span>
                    <span v-else-if="!job.meetsRequirements" class="badge bg-surface-600 text-gray-500">Gesperrt</span>
                  </div>
                  <p v-if="job.description" class="text-xs text-gray-500 mt-0.5">{{ job.description }}</p>
                  <div class="flex items-center gap-2 mt-2 flex-wrap">
                    <span v-if="job.requiredEducationType" class="text-xs text-gray-400">
                      📚 {{ formatEducationRequirement(job.requiredEducationType, job.requiredEducationField) }}
                    </span>
                    <span v-if="job.requiredSideCert" class="text-xs text-gray-400">
                      🎓 {{ job.requiredSideCert }}
                    </span>
                    <span v-if="job.requiredMonthsExperience > 0" class="text-xs text-gray-400">
                      ⏱ {{ job.requiredMonthsExperience }} Monate Erfahrung
                    </span>
                    <span class="badge text-xs" :class="stressColor(job.stressPerMonth)">
                      Stress: {{ stressLabel(job.stressPerMonth) }}
                    </span>
                  </div>
                </div>
                <div class="flex flex-col items-end gap-2 flex-shrink-0">
                  <span class="text-base font-bold font-mono text-white">{{ formatCurrency(job.salary) }}</span>
                  <span class="text-xs text-gray-500">pro Monat</span>
                  <button v-if="job.alreadyWorking"
                    class="btn-danger text-xs px-3 py-1.5"
                    :disabled="quittingId === job.id"
                    @click="quit(job.id)">
                    {{ quittingId === job.id ? '…' : 'Kündigen' }}
                  </button>
                  <button v-else-if="job.alreadyApplied"
                    class="btn-secondary text-xs px-3 py-1.5 opacity-50 cursor-not-allowed" disabled>
                    Beworben
                  </button>
                  <button v-else-if="job.meetsRequirements"
                    class="btn-primary text-xs px-3 py-1.5"
                    :disabled="applyingId === job.id"
                    @click="apply(job.id)">
                    {{ applyingId === job.id ? '…' : 'Bewerben' }}
                  </button>
                  <button v-else class="btn-secondary text-xs px-3 py-1.5 opacity-40 cursor-not-allowed" disabled>
                    Gesperrt
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div v-if="jobGroups.length === 0" class="card text-center text-gray-600 text-sm py-8">
            Keine Stellen gefunden
          </div>
        </template>

        <!-- Flat filtered view -->
        <template v-else>
          <div class="space-y-2">
            <div
              v-for="job in filteredJobs"
              :key="job.id"
              class="card transition-colors"
              :class="job.alreadyWorking ? 'border-green-600/40' : job.meetsRequirements ? '' : 'opacity-60'"
            >
              <div class="flex items-start justify-between gap-4">
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2 flex-wrap">
                    <h4 class="text-sm font-semibold text-white">{{ job.name }}</h4>
                    <span class="badge text-xs" :class="categoryBadge(job.category)">
                      {{ categoryLabels[job.category] ?? job.category }}
                    </span>
                    <span v-if="job.alreadyWorking" class="badge bg-green-500/15 text-green-400">Aktiv</span>
                    <span v-else-if="job.alreadyApplied" class="badge bg-yellow-500/15 text-yellow-400">Beworben</span>
                    <span v-else-if="!job.meetsRequirements" class="badge bg-surface-600 text-gray-500">Gesperrt</span>
                  </div>
                  <p v-if="job.description" class="text-xs text-gray-500 mt-0.5">{{ job.description }}</p>
                  <div class="flex items-center gap-2 mt-2 flex-wrap">
                    <span v-if="job.requiredEducationType" class="text-xs text-gray-400">
                      📚 {{ formatEducationRequirement(job.requiredEducationType, job.requiredEducationField) }}
                    </span>
                    <span v-if="job.requiredSideCert" class="text-xs text-gray-400">
                      🎓 {{ job.requiredSideCert }}
                    </span>
                    <span v-if="job.requiredMonthsExperience > 0" class="text-xs text-gray-400">
                      ⏱ {{ job.requiredMonthsExperience }} Monate Erfahrung
                    </span>
                    <span class="badge text-xs" :class="stressColor(job.stressPerMonth)">
                      Stress: {{ stressLabel(job.stressPerMonth) }}
                    </span>
                  </div>
                </div>
                <div class="flex flex-col items-end gap-2 flex-shrink-0">
                  <span class="text-base font-bold font-mono text-white">{{ formatCurrency(job.salary) }}</span>
                  <span class="text-xs text-gray-500">pro Monat</span>
                  <button v-if="job.alreadyWorking"
                    class="btn-danger text-xs px-3 py-1.5"
                    :disabled="quittingId === job.id"
                    @click="quit(job.id)">
                    {{ quittingId === job.id ? '…' : 'Kündigen' }}
                  </button>
                  <button v-else-if="job.alreadyApplied"
                    class="btn-secondary text-xs px-3 py-1.5 opacity-50 cursor-not-allowed" disabled>
                    Beworben
                  </button>
                  <button v-else-if="job.meetsRequirements"
                    class="btn-primary text-xs px-3 py-1.5"
                    :disabled="applyingId === job.id"
                    @click="apply(job.id)">
                    {{ applyingId === job.id ? '…' : 'Bewerben' }}
                  </button>
                  <button v-else class="btn-secondary text-xs px-3 py-1.5 opacity-40 cursor-not-allowed" disabled>
                    Gesperrt
                  </button>
                </div>
              </div>
            </div>
            <div v-if="filteredJobs.length === 0" class="card text-center text-gray-600 text-sm py-8">
              Keine Stellen in dieser Kategorie
            </div>
          </div>
        </template>
      </div>

      <!-- ── Right: My jobs + applications ── -->
      <div class="space-y-4">
        <!-- Active jobs -->
        <div class="card">
          <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Meine Jobs</h3>
          <div v-if="activeJobs.length" class="space-y-3">
            <div
              v-for="pj in activeJobs"
              :key="pj.jobId"
              class="p-3 rounded-lg bg-surface-700"
            >
              <div class="flex items-start justify-between gap-2">
                <div>
                  <p class="text-sm font-medium text-white">{{ pj.jobName }}</p>
                  <p class="text-xs text-gray-500 mt-0.5">
                    {{ pj.monthsWorked }} {{ pj.monthsWorked === 1 ? 'Monat' : 'Monate' }} gearbeitet
                  </p>
                </div>
                <span class="text-sm font-mono text-green-400 font-semibold flex-shrink-0">
                  {{ formatCurrency(pj.salary) }}
                </span>
              </div>
            </div>
          </div>
          <p v-else class="text-sm text-gray-600 text-center py-3">Kein aktiver Job</p>
        </div>

        <!-- Applications -->
        <div class="card">
          <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Bewerbungen</h3>
          <div v-if="applications.length" class="space-y-2">
            <div
              v-for="app in applications.slice(0, 8)"
              :key="app.id"
              class="flex items-center justify-between py-1.5"
            >
              <div class="min-w-0">
                <p class="text-sm text-gray-300 truncate">{{ app.jobName }}</p>
                <p class="text-xs text-gray-600">Runde {{ app.appliedAtTurn }}</p>
              </div>
              <span class="badge text-xs flex-shrink-0 ml-2" :class="statusBadge(app.status)">
                {{ statusLabel(app.status) }}
              </span>
            </div>
          </div>
          <p v-else class="text-sm text-gray-600 text-center py-3">Noch keine Bewerbungen</p>
        </div>

        <!-- Info box -->
        <div class="card border-dashed">
          <h4 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">Hinweise</h4>
          <ul class="space-y-1.5 text-xs text-gray-500">
            <li>Bewerbungen werden am Ende des nächsten Monats bearbeitet.</li>
            <li>Mehrere Jobs gleichzeitig erhöhen den Stress.</li>
            <li>Kündigung wirkt sofort.</li>
            <li>🎓 = Weiterbildung erforderlich, 📚 = Bildungsabschluss erforderlich.</li>
          </ul>
        </div>
      </div>
    </div>
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

interface Application {
  id: number
  jobId: number
  jobName: string
  jobSalary: number
  appliedAtTurn: number
  status: string
  resolvedAtTurn: number | null
}

// ── Filter config ──────────────────────────────────────────────────────────────

const modeFilters = [
  { value: 'alle',       label: 'Alle' },
  { value: 'verfuegbar', label: 'Verfügbar' },
  { value: 'meine',      label: 'Meine' },
] as const

const CATEGORY_ORDER = ['EINSTIEG', 'HANDWERK', 'BUERO', 'TECH', 'MANAGEMENT', 'GESUNDHEIT', 'RECHT', 'KREATIV']

const categoryLabels: Record<string, string> = {
  EINSTIEG:   'Einstieg',
  HANDWERK:   'Handwerk',
  BUERO:      'Büro',
  TECH:       'Tech',
  MANAGEMENT: 'Management',
  GESUNDHEIT: 'Gesundheit',
  RECHT:      'Recht',
  KREATIV:    'Kreativ',
}

const categoryFilters = computed(() => {
  const present = new Set(availableJobs.value.map(j => j.category))
  return CATEGORY_ORDER.filter(v => present.has(v)).map(v => ({ value: v, label: categoryLabels[v] ?? v }))
})

function categoryColor(cat: string): string {
  const map: Record<string, string> = {
    EINSTIEG:   'text-gray-400',
    HANDWERK:   'text-orange-400',
    BUERO:      'text-blue-400',
    TECH:       'text-cyan-400',
    MANAGEMENT: 'text-purple-400',
    GESUNDHEIT: 'text-green-400',
    RECHT:      'text-yellow-400',
    KREATIV:    'text-pink-400',
  }
  return map[cat] ?? 'text-gray-400'
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

// ── State ──────────────────────────────────────────────────────────────────────

const availableJobs = ref<Job[]>([])
const activeJobs = ref<PlayerJob[]>([])
const applications = ref<Application[]>([])
const jobsLoading = ref(true)
const applyingId = ref<number | null>(null)
const quittingId = ref<number | null>(null)
const activeFilter = ref<string>('alle')

// ── Computed ───────────────────────────────────────────────────────────────────

const filteredJobs = computed(() => {
  const f = activeFilter.value
  if (f === 'verfuegbar') return availableJobs.value.filter(j => j.meetsRequirements && !j.alreadyWorking)
  if (f === 'meine')      return availableJobs.value.filter(j => j.alreadyWorking)
  return availableJobs.value.filter(j => j.category === f)
})

const jobGroups = computed(() => {
  const seen = new Set<string>()
  const result: { category: string; label: string; jobs: Job[] }[] = []
  for (const cat of CATEGORY_ORDER) {
    const group = availableJobs.value.filter(j => j.category === cat)
    if (group.length > 0) {
      result.push({ category: cat, label: categoryLabels[cat] ?? cat, jobs: group })
    }
    seen.add(cat)
  }
  // Catch any unknown categories
  for (const job of availableJobs.value) {
    if (!seen.has(job.category)) {
      seen.add(job.category)
      result.push({
        category: job.category,
        label: job.category,
        jobs: availableJobs.value.filter(j => j.category === job.category),
      })
    }
  }
  return result
})

const totalSalary = computed(() =>
  activeJobs.value.reduce((sum, j) => sum + j.salary, 0),
)

// ── Actions ────────────────────────────────────────────────────────────────────

async function loadAll() {
  jobsLoading.value = true
  try {
    const [jobs, myJobs, apps] = await Promise.all([
      api.get<Job[]>('/api/jobs'),
      api.get<PlayerJob[]>('/api/jobs/my'),
      api.get<Application[]>('/api/jobs/applications'),
    ])
    availableJobs.value = jobs
    activeJobs.value = myJobs
    applications.value = apps
  } finally {
    jobsLoading.value = false
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
    await loadAll()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Kündigung fehlgeschlagen.')
  } finally {
    quittingId.value = null
  }
}

function statusLabel(status: string) {
  return { PENDING: 'Ausstehend', ACCEPTED: 'Angenommen', REJECTED: 'Abgelehnt' }[status] ?? status
}

function statusBadge(status: string) {
  return {
    PENDING:  'bg-yellow-500/15 text-yellow-400',
    ACCEPTED: 'bg-green-500/15 text-green-400',
    REJECTED: 'bg-red-500/15 text-red-400',
  }[status] ?? 'bg-surface-600 text-gray-400'
}

onMounted(loadAll)
</script>
