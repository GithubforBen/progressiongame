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
        <div class="flex items-center gap-2">
          <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider flex-1">Stellenangebote</h3>
          <div class="flex gap-1">
            <button
              v-for="f in filters"
              :key="f.value"
              class="px-2.5 py-1 rounded text-xs font-medium transition-colors"
              :class="activeFilter === f.value
                ? 'bg-accent text-white'
                : 'bg-surface-700 text-gray-400 hover:text-white'"
              @click="activeFilter = f.value"
            >
              {{ f.label }}
            </button>
          </div>
        </div>

        <div v-if="jobsLoading" class="card flex items-center justify-center h-32 text-gray-600 text-sm">
          Lade Stellenangebote…
        </div>

        <div v-else class="space-y-2">
          <div
            v-for="job in filteredJobs"
            :key="job.id"
            class="card transition-colors"
            :class="job.alreadyWorking ? 'border-green-600/40' : job.meetsRequirements ? '' : 'opacity-60'"
          >
            <div class="flex items-start justify-between gap-4">
              <!-- Info -->
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2 flex-wrap">
                  <h4 class="text-sm font-semibold text-white">{{ job.name }}</h4>
                  <span v-if="job.alreadyWorking" class="badge bg-green-500/15 text-green-400">Aktiv</span>
                  <span v-else-if="job.alreadyApplied" class="badge bg-yellow-500/15 text-yellow-400">Beworben</span>
                  <span v-else-if="!job.meetsRequirements" class="badge bg-surface-600 text-gray-500">Gesperrt</span>
                </div>
                <p v-if="job.description" class="text-xs text-gray-500 mt-0.5">{{ job.description }}</p>

                <div class="flex items-center gap-3 mt-2 flex-wrap">
                  <span class="text-xs text-gray-400">
                    {{ formatEducationRequirement(job.requiredEducationType, job.requiredEducationField) }}
                  </span>
                  <span v-if="job.requiredMonthsExperience > 0" class="text-xs text-gray-400">
                    {{ job.requiredMonthsExperience }} Monate Erfahrung
                  </span>
                  <span
                    class="badge text-xs font-medium px-2 py-0.5 rounded"
                    :class="stressColor(job.stressPerMonth)"
                  >
                    Stress: {{ stressLabel(job.stressPerMonth) }}
                  </span>
                </div>
              </div>

              <!-- Salary + action -->
              <div class="flex flex-col items-end gap-2 flex-shrink-0">
                <span class="text-base font-bold font-mono text-white">{{ formatCurrency(job.salary) }}</span>
                <span class="text-xs text-gray-500">pro Monat</span>

                <button
                  v-if="job.alreadyWorking"
                  class="btn-danger text-xs px-3 py-1.5"
                  :disabled="quittingId === job.id"
                  @click="quit(job.id)"
                >
                  {{ quittingId === job.id ? '…' : 'Kündigen' }}
                </button>
                <button
                  v-else-if="job.alreadyApplied"
                  class="btn-secondary text-xs px-3 py-1.5 opacity-50 cursor-not-allowed"
                  disabled
                >
                  Beworben
                </button>
                <button
                  v-else-if="job.meetsRequirements"
                  class="btn-primary text-xs px-3 py-1.5"
                  :disabled="applyingId === job.id"
                  @click="apply(job.id)"
                >
                  {{ applyingId === job.id ? '…' : 'Bewerben' }}
                </button>
                <button
                  v-else
                  class="btn-secondary text-xs px-3 py-1.5 opacity-40 cursor-not-allowed"
                  disabled
                >
                  Gesperrt
                </button>
              </div>
            </div>
          </div>

          <div v-if="filteredJobs.length === 0" class="card text-center text-gray-600 text-sm py-8">
            Keine Stellen in dieser Kategorie
          </div>
        </div>
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
              <span
                class="badge text-xs flex-shrink-0 ml-2"
                :class="statusBadge(app.status)"
              >
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
  requiredEducationType: string | null
  requiredEducationField: string | null
  requiredMonthsExperience: number
  salary: number
  stressPerMonth: number
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

const availableJobs = ref<Job[]>([])
const activeJobs = ref<PlayerJob[]>([])
const applications = ref<Application[]>([])
const jobsLoading = ref(true)
const applyingId = ref<number | null>(null)
const quittingId = ref<number | null>(null)
const activeFilter = ref<'all' | 'available' | 'mine'>('all')

const filters = [
  { value: 'all', label: 'Alle' },
  { value: 'available', label: 'Verfügbar' },
  { value: 'mine', label: 'Meine' },
] as const

const filteredJobs = computed(() => {
  if (activeFilter.value === 'available') return availableJobs.value.filter(j => j.meetsRequirements && !j.alreadyWorking)
  if (activeFilter.value === 'mine') return availableJobs.value.filter(j => j.alreadyWorking)
  return availableJobs.value
})

const totalSalary = computed(() =>
  activeJobs.value.reduce((sum, j) => sum + j.salary, 0),
)

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
    PENDING: 'bg-yellow-500/15 text-yellow-400',
    ACCEPTED: 'bg-green-500/15 text-green-400',
    REJECTED: 'bg-red-500/15 text-red-400',
  }[status] ?? 'bg-surface-600 text-gray-400'
}

onMounted(loadAll)
</script>
