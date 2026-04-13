<template>
  <div class="space-y-5">
    <h2 class="text-xl font-bold text-white">Ausbildung</h2>

    <div v-if="loading" class="card flex items-center justify-center h-32 text-gray-600 text-sm">
      Lade Bildungsstand…
    </div>

    <template v-else-if="progress">
      <!-- Active progress banner -->
      <div
        v-if="progress.mainStageMonthsRemaining > 0 || progress.sideCertMonthsRemaining > 0"
        class="card border-accent/40 bg-accent/5"
      >
        <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Laufende Ausbildung</h3>
        <div class="flex flex-col sm:flex-row gap-4">
          <div v-if="progress.mainStageMonthsRemaining > 0" class="flex-1">
            <p class="text-sm text-gray-300 mb-1">
              <span class="text-white font-medium">{{ mainStageLabel }}</span>
              <span class="text-gray-500"> · Hauptausbildung</span>
            </p>
            <div class="h-2 bg-surface-700 rounded-full overflow-hidden">
              <div
                class="h-full bg-accent rounded-full transition-all duration-500"
                :style="{ width: `${mainProgress}%` }"
              />
            </div>
            <p class="text-xs text-gray-500 mt-1">
              {{ progress.mainStageMonthsRemaining }} {{ progress.mainStageMonthsRemaining === 1 ? 'Monat' : 'Monate' }} verbleibend
            </p>
          </div>
          <div v-if="progress.sideCertMonthsRemaining > 0" class="flex-1">
            <p class="text-sm text-gray-300 mb-1">
              <span class="text-white font-medium">{{ sideCertDisplayLabel }}</span>
              <span class="text-gray-500"> · Weiterbildung</span>
            </p>
            <div class="h-2 bg-surface-700 rounded-full overflow-hidden">
              <div
                class="h-full bg-purple-500 rounded-full transition-all duration-500"
                :style="{ width: `${sideProgress}%` }"
              />
            </div>
            <p class="text-xs text-gray-500 mt-1">
              {{ progress.sideCertMonthsRemaining }} {{ progress.sideCertMonthsRemaining === 1 ? 'Monat' : 'Monate' }} verbleibend
            </p>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 xl:grid-cols-3 gap-5">
        <!-- ── Main education tree ── -->
        <div class="xl:col-span-2 space-y-3">
          <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Hauptausbildungsweg</h3>

          <div class="relative">
            <!-- Connector line -->
            <div class="absolute left-5 top-8 bottom-8 w-px bg-surface-600 z-0" />

            <div class="space-y-2 relative z-10">
              <EducationStageCard
                v-for="stage in mainTree"
                :key="stage.key"
                :stage="stage"
                :available-stages="progress.availableMainStages"
                :busy="progress.mainStageMonthsRemaining > 0"
                @enroll="enrollMain"
              />
            </div>
          </div>
        </div>

        <!-- ── Side certifications ── -->
        <div class="space-y-3">
          <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Weiterbildungen</h3>

          <!-- Completed side certs -->
          <div
            v-for="cert in completedSideCerts"
            :key="cert"
            class="card border-green-600/30"
          >
            <div class="flex items-center gap-3">
              <span class="text-green-400 text-base">✓</span>
              <div>
                <p class="text-sm font-medium text-white">{{ formatSideCertKey(cert) }}</p>
                <p class="text-xs text-gray-500">Abgeschlossen</p>
              </div>
            </div>
          </div>

          <!-- In-progress side cert -->
          <div v-if="progress.sideCertMonthsRemaining > 0" class="card border-purple-600/30">
            <div class="flex items-center gap-3">
              <span class="text-purple-400 text-base">▶</span>
              <div>
                <p class="text-sm font-medium text-white">{{ sideCertDisplayLabel }}</p>
                <p class="text-xs text-gray-500">{{ progress.sideCertMonthsRemaining }} Monate verbleibend</p>
              </div>
            </div>
          </div>

          <!-- Available side certs -->
          <template v-if="progress.sideCertMonthsRemaining === 0">
            <div
              v-for="cert in progress.availableSideCerts"
              :key="cert.certKey"
              class="card hover:border-accent/40 transition-colors"
            >
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="text-sm font-medium text-white">{{ cert.label }}</p>
                  <p class="text-xs text-gray-500 mt-0.5">{{ cert.durationMonths }} Monat</p>
                </div>
                <button
                  class="btn-primary text-xs px-3 py-1.5 flex-shrink-0"
                  :disabled="enrollingSide === cert.certKey"
                  @click="enrollSide(cert.certKey)"
                >
                  {{ enrollingSide === cert.certKey ? '…' : 'Einschreiben' }}
                </button>
              </div>
            </div>
          </template>

          <div
            v-if="progress.availableSideCerts.length === 0 && progress.sideCertMonthsRemaining === 0
                  && completedSideCerts.length === 0"
            class="card border-dashed text-center text-gray-600 text-sm py-4"
          >
            Realschulabschluss erforderlich
          </div>

          <!-- Info -->
          <div class="card border-dashed">
            <p class="text-xs text-gray-500 leading-relaxed">
              Weiterbildungen laufen parallel zur Hauptausbildung und schalten neue Jobs frei.
              Nur eine Weiterbildung gleichzeitig möglich.
            </p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useApi } from '~/composables/useApi'
import { useToastStore } from '~/stores/toast'

definePageMeta({ layout: 'default' })

const api = useApi()
const toast = useToastStore()

interface FieldOption { value: string; label: string }
interface AvailableStage {
  stageKey: string; label: string; durationMonths: number
  requiresField: boolean; fieldOptions: FieldOption[]
}
interface SideCert { certKey: string; label: string; durationMonths: number }

interface Progress {
  mainStage: string
  mainStageMonthsRemaining: number
  mainStageField: string | null
  sideCert: string | null
  sideCertMonthsRemaining: number
  completedStages: string[]
  availableMainStages: AvailableStage[]
  availableSideCerts: SideCert[]
}

// Static tree definition (display only — enrollment driven by backend)
interface TreeStage {
  key: string
  label: string
  months: number | null
  depth: number
  requiresField: boolean
  fieldOptions: FieldOption[]
  branches?: string[] // for forking display
}

const TREE: TreeStage[] = [
  { key: 'GRUNDSCHULE',        label: 'Grundschule',       months: null, depth: 0, requiresField: false, fieldOptions: [] },
  { key: 'REALSCHULABSCHLUSS', label: 'Realschulabschluss',months: 2,    depth: 1, requiresField: false, fieldOptions: [] },
  { key: 'ABITUR',             label: 'Abitur',            months: 3,    depth: 2, requiresField: false, fieldOptions: [] },
  { key: 'AUSBILDUNG',         label: 'Ausbildung',        months: 4,    depth: 2, requiresField: true,  fieldOptions: [
    { value: 'FACHINFORMATIKER', label: 'Fachinformatiker' },
    { value: 'KAUFMANN',         label: 'Kaufmann/-frau' },
    { value: 'ELEKTRIKER',       label: 'Elektriker/-in' },
    { value: 'ERZIEHER',         label: 'Erzieher/-in' },
  ]},
  { key: 'BACHELOR',           label: 'Bachelor',          months: 6,    depth: 3, requiresField: true,  fieldOptions: [
    { value: 'INFORMATIK', label: 'Informatik' },
    { value: 'BWL',        label: 'Betriebswirtschaft' },
    { value: 'MEDIZIN',    label: 'Medizin' },
    { value: 'JURA',       label: 'Rechtswissenschaften' },
  ]},
  { key: 'MASTER',             label: 'Master',            months: 4,    depth: 4, requiresField: true,  fieldOptions: [
    { value: 'INFORMATIK', label: 'Informatik' },
    { value: 'BWL',        label: 'Betriebswirtschaft' },
    { value: 'MEDIZIN',    label: 'Medizin' },
    { value: 'JURA',       label: 'Rechtswissenschaften' },
  ]},
]

const progress = ref<Progress | null>(null)
const loading = ref(true)
const enrollingSide = ref<string | null>(null)

const mainTree = computed(() => TREE)

const mainStageLabel = computed(() => {
  if (!progress.value) return ''
  const p = progress.value
  const field = p.mainStageField ? ': ' + fieldLabel(p.mainStageField) : ''
  return stageLabel(p.mainStage) + field
})

const sideCertDisplayLabel = computed(() => {
  const cert = progress.value?.sideCert
  if (!cert) return ''
  return formatSideCertKey(cert)
})

const completedSideCerts = computed(() =>
  (progress.value?.completedStages ?? []).filter(s => s.startsWith('WEITERBILDUNG_')),
)

// Provide progress to EducationStageCard children
provide('educationProgress', computed(() => progress.value
  ? { completedStages: progress.value.completedStages,
      mainStage: progress.value.mainStage,
      mainStageMonthsRemaining: progress.value.mainStageMonthsRemaining }
  : null,
))

// Progress percentages for active stages — estimated from known durations
const STAGE_DURATIONS: Record<string, number> = {
  REALSCHULABSCHLUSS: 2, ABITUR: 3, AUSBILDUNG: 4, BACHELOR: 6, MASTER: 4,
}
const mainProgress = computed(() => {
  const p = progress.value
  if (!p || p.mainStageMonthsRemaining === 0) return 100
  const total = STAGE_DURATIONS[p.mainStage] ?? 1
  return Math.round(((total - p.mainStageMonthsRemaining) / total) * 100)
})
const sideProgress = computed(() => {
  const p = progress.value
  if (!p || p.sideCertMonthsRemaining === 0) return 100
  return Math.round(((1 - p.sideCertMonthsRemaining) / 1) * 100)
})

async function loadProgress() {
  loading.value = true
  try {
    progress.value = await api.get<Progress>('/api/education')
  } finally {
    loading.value = false
  }
}

async function enrollMain(stage: string, field: string | null) {
  try {
    progress.value = await api.post<Progress>('/api/education/main', { stage, field })
    toast.success('Einschreibung erfolgreich!', 'Ausbildung gestartet')
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Einschreibung fehlgeschlagen.')
  }
}

async function enrollSide(certKey: string) {
  enrollingSide.value = certKey
  try {
    progress.value = await api.post<Progress>('/api/education/side', { cert: certKey })
    toast.success('Weiterbildung gestartet!', 'Eingeschrieben')
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Einschreibung fehlgeschlagen.')
  } finally {
    enrollingSide.value = null
  }
}

function stageLabel(key: string) {
  const map: Record<string, string> = {
    GRUNDSCHULE: 'Grundschule', REALSCHULABSCHLUSS: 'Realschulabschluss',
    ABITUR: 'Abitur', AUSBILDUNG: 'Ausbildung', BACHELOR: 'Bachelor', MASTER: 'Master',
  }
  return map[key] ?? key
}

function fieldLabel(field: string) {
  const map: Record<string, string> = {
    INFORMATIK: 'Informatik', BWL: 'Betriebswirtschaft', MEDIZIN: 'Medizin',
    JURA: 'Rechtswissenschaften', FACHINFORMATIKER: 'Fachinformatiker',
    KAUFMANN: 'Kaufmann/-frau', ELEKTRIKER: 'Elektriker/-in', ERZIEHER: 'Erzieher/-in',
  }
  return map[field] ?? field
}

function formatSideCertKey(key: string) {
  const map: Record<string, string> = {
    'WEITERBILDUNG_SOCIAL_MEDIA': 'Social Media Marketing',
    'WEITERBILDUNG_EXCEL': 'Excel-Kurs',
    'WEITERBILDUNG_FUEHRERSCHEIN': 'Führerschein',
    'WEITERBILDUNG_CRYPTO': 'Crypto Trading Zertifikat',
  }
  return map[key] ?? key
}

onMounted(loadProgress)
</script>
