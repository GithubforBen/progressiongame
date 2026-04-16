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
              <div class="h-full bg-accent rounded-full transition-all duration-500"
                :style="{ width: `${mainProgress}%` }" />
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
              <div class="h-full bg-purple-500 rounded-full transition-all duration-500"
                :style="{ width: `${sideProgress}%` }" />
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

          <!-- Cert families with level chains -->
          <div
            v-for="family in certFamilies"
            :key="family.name"
            :id="`cert-family-${family.name}`"
            class="card space-y-2 transition-all duration-300"
            :class="isFamilyHighlighted(family) ? 'ring-2 ring-accent/60 bg-accent/5' : ''"
          >
            <p class="text-xs font-semibold uppercase tracking-wider"
               :class="isFamilyHighlighted(family) ? 'text-accent' : 'text-gray-400'">
              {{ family.label }}
              <span v-if="isFamilyHighlighted(family)" class="ml-1 text-accent">← Hier!</span>
            </p>
            <div class="flex items-center gap-1">
              <template v-for="(lvl, idx) in family.levels" :key="lvl.key">
                <!-- connector line -->
                <div v-if="idx > 0" class="h-px w-3 bg-surface-600 flex-shrink-0" />
                <!-- level node -->
                <div
                  class="flex-1 min-w-0 rounded px-2 py-1.5 text-center text-xs font-medium border"
                  :class="lvl.state === 'done'
                    ? 'bg-green-500/15 border-green-500/30 text-green-400'
                    : lvl.state === 'active'
                    ? 'bg-purple-500/15 border-purple-500/40 text-purple-300'
                    : lvl.state === 'available'
                    ? 'bg-accent/10 border-accent/30 text-accent cursor-pointer hover:bg-accent/20'
                    : 'bg-surface-700 border-white/5 text-gray-600'"
                  :title="lvl.label + (CERT_UNLOCKS[lvl.key] ? ' → ' + CERT_UNLOCKS[lvl.key] : '')"
                  @click="lvl.state === 'available' && progress.sideCertMonthsRemaining === 0 && enrollSide(lvl.key)"
                >
                  <span v-if="lvl.state === 'done'">✓</span>
                  <span v-else-if="lvl.state === 'active'">▶</span>
                  <span v-else-if="lvl.state === 'locked'">🔒</span>
                  <span v-else>{{ idx + 1 }}</span>
                </div>
              </template>
            </div>
            <!-- Available cert in this family: show details + enroll -->
            <template v-for="lvl in family.levels" :key="`btn-${lvl.key}`">
              <div v-if="lvl.state === 'available' && progress.sideCertMonthsRemaining === 0"
                   class="flex items-start justify-between gap-2 pt-1">
                <div>
                  <p class="text-xs text-white">{{ lvl.label }}</p>
                  <p class="text-xs text-gray-500">{{ lvl.durationMonths }} {{ lvl.durationMonths === 1 ? 'Monat' : 'Monate' }}</p>
                  <p v-if="CERT_UNLOCKS[lvl.key]" class="text-xs text-accent/70 mt-0.5">
                    Schaltet frei: {{ CERT_UNLOCKS[lvl.key] }}
                  </p>
                </div>
                <div class="flex flex-col items-end gap-1 flex-shrink-0">
                  <span v-if="lvl.cost > 0" class="text-xs text-yellow-400 font-mono">{{ formatCost(lvl.cost) }}</span>
                  <button
                    class="btn-primary text-xs px-3 py-1"
                    :disabled="enrollingSide === lvl.key"
                    @click="enrollSide(lvl.key)"
                  >
                    {{ enrollingSide === lvl.key ? '…' : 'Einschreiben' }}
                  </button>
                </div>
              </div>
            </template>
          </div>

          <div
            v-if="progress.availableSideCerts.length === 0 && progress.sideCertMonthsRemaining === 0
                  && completedSideCerts.length === 0"
            class="card border-dashed text-center text-gray-600 text-sm py-4"
          >
            Keine Weiterbildungen verfügbar
          </div>

          <!-- Info -->
          <div class="card border-dashed">
            <p class="text-xs text-gray-500 leading-relaxed">
              Weiterbildungen laufen parallel zur Hauptausbildung und schalten neue Jobs frei.
              Nur eine Weiterbildung gleichzeitig möglich. Kosten werden sofort abgezogen.
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

interface FieldOption { value: string; label: string; durationMonths: number }
interface AvailableStage {
  stageKey: string; label: string; durationMonths: number
  requiresField: boolean; fieldOptions: FieldOption[]; cost: number
}
interface SideCert { certKey: string; label: string; durationMonths: number; cost: number }

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

interface TreeStage {
  key: string; label: string; months: number | null
  depth: number; requiresField: boolean; fieldOptions: FieldOption[]
}

// Static display tree — synced with education.yaml
// FieldOption.durationMonths: shown in dropdown only if different from stage default
const TREE: TreeStage[] = [
  { key: 'GRUNDSCHULE',        label: 'Grundschule',        months: null, depth: 0, requiresField: false, fieldOptions: [] },
  { key: 'REALSCHULABSCHLUSS', label: 'Realschulabschluss', months: 2,    depth: 1, requiresField: false, fieldOptions: [] },
  { key: 'ABITUR',             label: 'Abitur',             months: 3,    depth: 2, requiresField: false, fieldOptions: [] },
  { key: 'AUSBILDUNG', label: 'Ausbildung', months: 4, depth: 2, requiresField: true, fieldOptions: [
    { value: 'EINZELHANDEL',   label: 'Einzelhandelskaufmann/-frau', durationMonths: 4 },
    { value: 'FACHINFORMATIKER', label: 'Fachinformatiker/-in',      durationMonths: 4 },
    { value: 'KFZTECH',        label: 'KFZ-Mechatroniker/-in',       durationMonths: 4 },
    { value: 'PFLEGE',         label: 'Pflegefachkraft',             durationMonths: 4 },
    { value: 'KOCH',           label: 'Koch/Köchin',                 durationMonths: 4 },
    { value: 'ELEKTRIKER',     label: 'Elektriker/-in',              durationMonths: 4 },
  ]},
  { key: 'BACHELOR', label: 'Bachelor', months: 6, depth: 3, requiresField: true, fieldOptions: [
    { value: 'INFORMATIK',    label: 'Informatik (B.Sc.)',                      durationMonths: 6 },
    { value: 'BWL',           label: 'Betriebswirtschaft (B.Sc.)',              durationMonths: 6 },
    { value: 'MEDIZIN',       label: 'Medizin (Staatsexamen)',                  durationMonths: 8 },
    { value: 'JURA',          label: 'Rechtswissenschaften (1. Staatsexamen)', durationMonths: 7 },
    { value: 'INGENIEURWESEN',label: 'Ingenieurwesen (B.Eng.)',                 durationMonths: 6 },
    { value: 'PSYCHOLOGIE',   label: 'Psychologie (B.Sc.)',                     durationMonths: 6 },
  ]},
  { key: 'MASTER', label: 'Master', months: 4, depth: 4, requiresField: true, fieldOptions: [
    { value: 'INFORMATIK',    label: 'Informatik (M.Sc.)',                       durationMonths: 4 },
    { value: 'BWL',           label: 'Betriebswirtschaft (MBA)',                 durationMonths: 4 },
    { value: 'MEDIZIN',       label: 'Medizin (Approbation)',                   durationMonths: 4 },
    { value: 'JURA',          label: 'Rechtswissenschaften (2. Staatsexamen)', durationMonths: 4 },
    { value: 'INGENIEURWESEN',label: 'Ingenieurwesen (M.Eng.)',                  durationMonths: 4 },
  ]},
]

// For progress bar: total duration per stage+field combination
const STAGE_DURATIONS: Record<string, number> = {
  REALSCHULABSCHLUSS: 2, ABITUR: 3, AUSBILDUNG: 4, BACHELOR: 6, MASTER: 4,
  BACHELOR_MEDIZIN: 8, BACHELOR_JURA: 7,
}

// Total durations for side certs (for progress bar only)
const CERT_DURATIONS: Record<string, number> = {
  WEITERBILDUNG_BARKEEPER_1: 1, WEITERBILDUNG_BARKEEPER_2: 2, WEITERBILDUNG_BARKEEPER_3: 2,
  WEITERBILDUNG_FITNESSTRAINER_1: 1, WEITERBILDUNG_FITNESSTRAINER_2: 2, WEITERBILDUNG_FITNESSTRAINER_3: 2,
  WEITERBILDUNG_SOCIAL_MEDIA_1: 1, WEITERBILDUNG_SOCIAL_MEDIA_2: 2, WEITERBILDUNG_SOCIAL_MEDIA_3: 2,
  WEITERBILDUNG_EXCEL_1: 1, WEITERBILDUNG_EXCEL_2: 2, WEITERBILDUNG_EXCEL_3: 2,
  WEITERBILDUNG_FUEHRERSCHEIN_1: 1, WEITERBILDUNG_FUEHRERSCHEIN_2: 2, WEITERBILDUNG_FUEHRERSCHEIN_3: 3,
  WEITERBILDUNG_CRYPTO_1: 1, WEITERBILDUNG_CRYPTO_2: 2, WEITERBILDUNG_CRYPTO_3: 3,
  WEITERBILDUNG_BUCHHALTUNG_1: 1, WEITERBILDUNG_BUCHHALTUNG_2: 2, WEITERBILDUNG_BUCHHALTUNG_3: 3,
  WEITERBILDUNG_IMMOBILIEN_1: 2, WEITERBILDUNG_IMMOBILIEN_2: 3, WEITERBILDUNG_IMMOBILIEN_3: 4,
  WEITERBILDUNG_PROJEKTMANAGEMENT_1: 2, WEITERBILDUNG_PROJEKTMANAGEMENT_2: 3, WEITERBILDUNG_PROJEKTMANAGEMENT_3: 4,
  WEITERBILDUNG_STEUERN_1: 2, WEITERBILDUNG_STEUERN_2: 3, WEITERBILDUNG_STEUERN_3: 4,
  WEITERBILDUNG_HACKER_1: 2, WEITERBILDUNG_HACKER_2: 3, WEITERBILDUNG_HACKER_3: 4,
  WEITERBILDUNG_IMMOBILIEN_4: 3,
  WEITERBILDUNG_OLDTIMER_1: 1, WEITERBILDUNG_OLDTIMER_2: 2, WEITERBILDUNG_OLDTIMER_3: 2,
  WEITERBILDUNG_ARCHAEOLOGIE_1: 1, WEITERBILDUNG_ARCHAEOLOGIE_2: 2,
  WEITERBILDUNG_WEINKENNER_1: 1, WEITERBILDUNG_WEINKENNER_2: 2, WEITERBILDUNG_WEINKENNER_3: 3,
  WEITERBILDUNG_KUNSTKENNER_1: 1, WEITERBILDUNG_KUNSTKENNER_2: 2, WEITERBILDUNG_KUNSTKENNER_3: 3,
  WEITERBILDUNG_UHRMACHER_1: 1, WEITERBILDUNG_UHRMACHER_2: 2, WEITERBILDUNG_UHRMACHER_3: 3,
  WEITERBILDUNG_NUMISMATIK_1: 1, WEITERBILDUNG_NUMISMATIK_2: 2,
  WEITERBILDUNG_PHILATELIE_1: 1, WEITERBILDUNG_PHILATELIE_2: 2,
  WEITERBILDUNG_MINERALIEN_1: 1, WEITERBILDUNG_MINERALIEN_2: 2, WEITERBILDUNG_MINERALIEN_3: 2,
  WEITERBILDUNG_SPORTSAMMLER_1: 1, WEITERBILDUNG_SPORTSAMMLER_2: 2,
  WEITERBILDUNG_WHISKY_1: 1, WEITERBILDUNG_WHISKY_2: 2,
}

const SIDE_CERT_LABELS: Record<string, string> = {
  WEITERBILDUNG_BARKEEPER_1: 'Barkeeper-Grundkurs',
  WEITERBILDUNG_BARKEEPER_2: 'Barkeeper-Aufbaukurs',
  WEITERBILDUNG_BARKEEPER_3: 'Barkeeper-Meister',
  WEITERBILDUNG_FITNESSTRAINER_1: 'Fitnesstrainer B-Lizenz',
  WEITERBILDUNG_FITNESSTRAINER_2: 'Fitnesstrainer A-Lizenz',
  WEITERBILDUNG_FITNESSTRAINER_3: 'Personal Trainer Zertifikat',
  WEITERBILDUNG_SOCIAL_MEDIA_1: 'Social-Media-Marketing (Grundkurs)',
  WEITERBILDUNG_SOCIAL_MEDIA_2: 'Social-Media-Marketing (Aufbaukurs)',
  WEITERBILDUNG_SOCIAL_MEDIA_3: 'Social-Media-Marketing (Expertenzertifikat)',
  WEITERBILDUNG_EXCEL_1: 'Excel-Grundlagen',
  WEITERBILDUNG_EXCEL_2: 'Excel & Datenanalyse',
  WEITERBILDUNG_EXCEL_3: 'Excel VBA & Power BI',
  WEITERBILDUNG_FUEHRERSCHEIN_1: 'Führerschein Klasse B',
  WEITERBILDUNG_FUEHRERSCHEIN_2: 'Führerschein Klasse BE (Anhänger)',
  WEITERBILDUNG_FUEHRERSCHEIN_3: 'Führerschein Klasse C (LKW)',
  WEITERBILDUNG_CRYPTO_1: 'Krypto-Trading Zertifikat (Grundlagen)',
  WEITERBILDUNG_CRYPTO_2: 'DeFi & Blockchain Zertifikat',
  WEITERBILDUNG_CRYPTO_3: 'Certified Crypto Analyst (CCA)',
  WEITERBILDUNG_BUCHHALTUNG_1: 'Buchhaltung & DATEV (Grundkurs)',
  WEITERBILDUNG_BUCHHALTUNG_2: 'Bilanzbuchhaltung (IHK)',
  WEITERBILDUNG_BUCHHALTUNG_3: 'Steuerrecht & Konzernbilanzierung',
  WEITERBILDUNG_IMMOBILIEN_1: 'Immobilien-Grundlagen (IHK)',
  WEITERBILDUNG_IMMOBILIEN_2: 'Immobilienmakler-Lizenz',
  WEITERBILDUNG_IMMOBILIEN_3: 'Immobilien-Investor Masterclass',
  WEITERBILDUNG_PROJEKTMANAGEMENT_1: 'Projektmanagement (PRINCE2 Foundation)',
  WEITERBILDUNG_PROJEKTMANAGEMENT_2: 'Projektmanagement (PRINCE2 Practitioner)',
  WEITERBILDUNG_PROJEKTMANAGEMENT_3: 'Agile Coach Zertifikat (SAFe)',
  WEITERBILDUNG_STEUERN_1: 'Steuerberater-Grundkurs',
  WEITERBILDUNG_STEUERN_2: 'Steuerberater-Aufbaukurs',
  WEITERBILDUNG_STEUERN_3: 'Steuerberater-Examen (StBExPrV)',
  WEITERBILDUNG_HACKER_1: 'Ethical Hacking Zertifikat (CEH Foundation)',
  WEITERBILDUNG_HACKER_2: 'Certified Ethical Hacker (CEH)',
  WEITERBILDUNG_HACKER_3: 'Offensive Security Expert (OSCP)',
  WEITERBILDUNG_IMMOBILIEN_4: 'Immobilien-Portfolio Manager',
  WEITERBILDUNG_OLDTIMER_1: 'Oldtimer-Kurs Grundlagen',
  WEITERBILDUNG_OLDTIMER_2: 'Classic-Car Experte',
  WEITERBILDUNG_OLDTIMER_3: 'Oldtimer-Auktionator Zertifikat',
  WEITERBILDUNG_ARCHAEOLOGIE_1: 'Archäologen-Hobbykurs',
  WEITERBILDUNG_ARCHAEOLOGIE_2: 'Antiquitäten-Experte',
  WEITERBILDUNG_WEINKENNER_1: 'Weinkenner Grundkurs',
  WEITERBILDUNG_WEINKENNER_2: 'Wine & Spirit Education (WSET)',
  WEITERBILDUNG_WEINKENNER_3: 'Master Sommelier',
  WEITERBILDUNG_KUNSTKENNER_1: 'Kunstgeschichte Einführung',
  WEITERBILDUNG_KUNSTKENNER_2: 'Kunstmarkt-Experte',
  WEITERBILDUNG_KUNSTKENNER_3: 'Art Advisor Zertifikat',
  WEITERBILDUNG_UHRMACHER_1: 'Uhrmacher-Grundkurs',
  WEITERBILDUNG_UHRMACHER_2: 'Zertifizierter Uhrenexperte',
  WEITERBILDUNG_UHRMACHER_3: 'Horologie Diplom',
  WEITERBILDUNG_NUMISMATIK_1: 'Münzkunde Grundkurs',
  WEITERBILDUNG_NUMISMATIK_2: 'Professioneller Numismatiker',
  WEITERBILDUNG_PHILATELIE_1: 'Briefmarken-Sammler Kurs',
  WEITERBILDUNG_PHILATELIE_2: 'Philatelie-Experte',
  WEITERBILDUNG_MINERALIEN_1: 'Gemmologie Grundkurs',
  WEITERBILDUNG_MINERALIEN_2: 'Zertifizierter Gemmologe (FGA)',
  WEITERBILDUNG_MINERALIEN_3: 'Diamond Grading Expert',
  WEITERBILDUNG_SPORTSAMMLER_1: 'Sport-Memorabilia Grundkurs',
  WEITERBILDUNG_SPORTSAMMLER_2: 'Sportartefakt-Authentifizierer',
  WEITERBILDUNG_WHISKY_1: 'Whisky & Spirituosen Grundkurs',
  WEITERBILDUNG_WHISKY_2: 'Master Distiller Zertifikat',
}

const route = useRoute()
const progress = ref<Progress | null>(null)
const loading = ref(true)
const enrollingSide = ref<string | null>(null)
const highlightCert = computed(() => route.query.highlight as string | undefined)

const mainTree = computed(() => TREE)

const mainStageLabel = computed(() => {
  if (!progress.value) return ''
  const p = progress.value
  const field = p.mainStageField ? ': ' + fieldLabel(p.mainStageField) : ''
  return stageLabel(p.mainStage) + field
})

const sideCertDisplayLabel = computed(() =>
  formatSideCertKey(progress.value?.sideCert ?? ''),
)

const completedSideCerts = computed(() =>
  (progress.value?.completedStages ?? []).filter(s => s.startsWith('WEITERBILDUNG_')),
)

// Cert family definitions for the level-chain display
const CERT_FAMILY_DEFS = [
  { name: 'BARKEEPER', label: 'Barkeeper', levels: ['WEITERBILDUNG_BARKEEPER_1','WEITERBILDUNG_BARKEEPER_2','WEITERBILDUNG_BARKEEPER_3'] },
  { name: 'FITNESSTRAINER', label: 'Fitnesstrainer', levels: ['WEITERBILDUNG_FITNESSTRAINER_1','WEITERBILDUNG_FITNESSTRAINER_2','WEITERBILDUNG_FITNESSTRAINER_3'] },
  { name: 'SOCIAL_MEDIA', label: 'Social-Media', levels: ['WEITERBILDUNG_SOCIAL_MEDIA_1','WEITERBILDUNG_SOCIAL_MEDIA_2','WEITERBILDUNG_SOCIAL_MEDIA_3'] },
  { name: 'EXCEL', label: 'Excel', levels: ['WEITERBILDUNG_EXCEL_1','WEITERBILDUNG_EXCEL_2','WEITERBILDUNG_EXCEL_3'] },
  { name: 'FUEHRERSCHEIN', label: 'Führerschein', levels: ['WEITERBILDUNG_FUEHRERSCHEIN_1','WEITERBILDUNG_FUEHRERSCHEIN_2','WEITERBILDUNG_FUEHRERSCHEIN_3'] },
  { name: 'CRYPTO', label: 'Krypto-Trading', levels: ['WEITERBILDUNG_CRYPTO_1','WEITERBILDUNG_CRYPTO_2','WEITERBILDUNG_CRYPTO_3'] },
  { name: 'BUCHHALTUNG', label: 'Buchhaltung', levels: ['WEITERBILDUNG_BUCHHALTUNG_1','WEITERBILDUNG_BUCHHALTUNG_2','WEITERBILDUNG_BUCHHALTUNG_3'] },
  { name: 'IMMOBILIEN', label: 'Immobilien', levels: ['WEITERBILDUNG_IMMOBILIEN_1','WEITERBILDUNG_IMMOBILIEN_2','WEITERBILDUNG_IMMOBILIEN_3','WEITERBILDUNG_IMMOBILIEN_4'] },
  { name: 'PROJEKTMANAGEMENT', label: 'Projektmanagement', levels: ['WEITERBILDUNG_PROJEKTMANAGEMENT_1','WEITERBILDUNG_PROJEKTMANAGEMENT_2','WEITERBILDUNG_PROJEKTMANAGEMENT_3'] },
  { name: 'STEUERN', label: 'Steuern', levels: ['WEITERBILDUNG_STEUERN_1','WEITERBILDUNG_STEUERN_2','WEITERBILDUNG_STEUERN_3'] },
  { name: 'HACKER', label: 'Ethical Hacking', levels: ['WEITERBILDUNG_HACKER_1','WEITERBILDUNG_HACKER_2','WEITERBILDUNG_HACKER_3'] },
  // Sammler-Weiterbildungen
  { name: 'OLDTIMER', label: 'Oldtimer', levels: ['WEITERBILDUNG_OLDTIMER_1','WEITERBILDUNG_OLDTIMER_2','WEITERBILDUNG_OLDTIMER_3'] },
  { name: 'ARCHAEOLOGIE', label: 'Archäologie', levels: ['WEITERBILDUNG_ARCHAEOLOGIE_1','WEITERBILDUNG_ARCHAEOLOGIE_2'] },
  { name: 'WEINKENNER', label: 'Weinkenner', levels: ['WEITERBILDUNG_WEINKENNER_1','WEITERBILDUNG_WEINKENNER_2','WEITERBILDUNG_WEINKENNER_3'] },
  { name: 'KUNSTKENNER', label: 'Kunstkenner', levels: ['WEITERBILDUNG_KUNSTKENNER_1','WEITERBILDUNG_KUNSTKENNER_2','WEITERBILDUNG_KUNSTKENNER_3'] },
  { name: 'UHRMACHER', label: 'Uhrmacher', levels: ['WEITERBILDUNG_UHRMACHER_1','WEITERBILDUNG_UHRMACHER_2','WEITERBILDUNG_UHRMACHER_3'] },
  { name: 'NUMISMATIK', label: 'Numismatik', levels: ['WEITERBILDUNG_NUMISMATIK_1','WEITERBILDUNG_NUMISMATIK_2'] },
  { name: 'PHILATELIE', label: 'Philatelie', levels: ['WEITERBILDUNG_PHILATELIE_1','WEITERBILDUNG_PHILATELIE_2'] },
  { name: 'MINERALIEN', label: 'Gemmologie', levels: ['WEITERBILDUNG_MINERALIEN_1','WEITERBILDUNG_MINERALIEN_2','WEITERBILDUNG_MINERALIEN_3'] },
  { name: 'SPORTSAMMLER', label: 'Sportsammler', levels: ['WEITERBILDUNG_SPORTSAMMLER_1','WEITERBILDUNG_SPORTSAMMLER_2'] },
  { name: 'WHISKY', label: 'Whisky & Spirituosen', levels: ['WEITERBILDUNG_WHISKY_1','WEITERBILDUNG_WHISKY_2'] },
]

interface CertLevelInfo {
  key: string
  label: string
  durationMonths: number
  cost: number
  state: 'done' | 'active' | 'available' | 'locked'
}

const certFamilies = computed(() => {
  const p = progress.value
  if (!p) return []
  const completed = new Set(p.completedStages)
  const inProgressKey = p.sideCert
  const availableKeys = new Set(p.availableSideCerts.map(c => c.certKey))
  const availableMap = new Map(p.availableSideCerts.map(c => [c.certKey, c]))

  return CERT_FAMILY_DEFS.map(fam => {
    const levels: CertLevelInfo[] = fam.levels.map(key => {
      const available = availableMap.get(key)
      let state: CertLevelInfo['state']
      if (completed.has(key)) state = 'done'
      else if (key === inProgressKey) state = 'active'
      else if (availableKeys.has(key)) state = 'available'
      else state = 'locked'

      return {
        key,
        label: SIDE_CERT_LABELS[key] ?? key,
        durationMonths: available?.durationMonths ?? CERT_DURATIONS[key] ?? 1,
        cost: available?.cost ?? 0,
        state,
      }
    })
    return { name: fam.name, label: fam.label, levels }
  })
})

provide('educationProgress', computed(() => progress.value
  ? { completedStages: progress.value.completedStages,
      mainStage: progress.value.mainStage,
      mainStageMonthsRemaining: progress.value.mainStageMonthsRemaining }
  : null,
))

const mainProgress = computed(() => {
  const p = progress.value
  if (!p || p.mainStageMonthsRemaining === 0) return 100
  const key = p.mainStageField ? `${p.mainStage}_${p.mainStageField}` : p.mainStage
  const total = STAGE_DURATIONS[key] ?? STAGE_DURATIONS[p.mainStage] ?? 1
  return Math.round(((total - p.mainStageMonthsRemaining) / total) * 100)
})

const sideProgress = computed(() => {
  const p = progress.value
  if (!p || p.sideCertMonthsRemaining === 0) return 100
  const total = CERT_DURATIONS[p.sideCert ?? ''] ?? 1
  return Math.round(((total - p.sideCertMonthsRemaining) / total) * 100)
})

async function loadProgress() {
  loading.value = true
  try {
    progress.value = await api.get<Progress>('/api/education')
  } finally {
    loading.value = false
    // After load, scroll to highlighted cert if requested
    if (highlightCert.value) {
      await nextTick()
      const el = document.getElementById(`cert-family-${highlightCert.value.replace('WEITERBILDUNG_', '').replace(/_\d+$/, '')}`)
      el?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
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
    JURA: 'Rechtswissenschaften', INGENIEURWESEN: 'Ingenieurwesen', PSYCHOLOGIE: 'Psychologie',
    FACHINFORMATIKER: 'Fachinformatiker/-in', EINZELHANDEL: 'Einzelhandelskaufmann/-frau',
    KFZTECH: 'KFZ-Mechatronik', PFLEGE: 'Pflegefachkraft', KOCH: 'Koch/Köchin',
    ELEKTRIKER: 'Elektriker/-in',
  }
  return map[field] ?? field
}

function formatSideCertKey(key: string) {
  return SIDE_CERT_LABELS[key] ?? key
}

function formatCost(cost: number) {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(cost)
}

function isFamilyHighlighted(family: { name: string; levels: string[] }): boolean {
  const h = highlightCert.value
  if (!h) return false
  return family.levels.some(key => key === h || key.startsWith(h + '_'))
}

// What each cert level unlocks — shown as chip in the level node tooltip/detail
const CERT_UNLOCKS: Record<string, string> = {
  WEITERBILDUNG_BUCHHALTUNG_1: 'ETF-Aktien',
  WEITERBILDUNG_BUCHHALTUNG_2: 'Dividenden-Aktien',
  WEITERBILDUNG_IMMOBILIEN_1: 'Anleihen · REIT · Immobilien Stufe 2',
  WEITERBILDUNG_IMMOBILIEN_2: 'Immobilien Stufe 3',
  WEITERBILDUNG_IMMOBILIEN_3: 'Immobilien Stufe 4',
  WEITERBILDUNG_IMMOBILIEN_4: 'Immobilien Stufe 5 (Magnaten)',
  WEITERBILDUNG_CRYPTO_1: 'Krypto-Aktien · Krypto-Wallet Sammlung · Krypto-Mining Sammlung',
  WEITERBILDUNG_CRYPTO_2: 'Hebelprodukte · Optionsscheine · NFT Sammlung',
  WEITERBILDUNG_CRYPTO_3: 'Short & Futures',
  WEITERBILDUNG_OLDTIMER_1: 'Autos-Sammlungen: Klassiker, Japanisch, Oldtimer',
  WEITERBILDUNG_OLDTIMER_2: 'Autos-Sammlung: Luxusflotte',
  WEITERBILDUNG_WEINKENNER_1: 'Bordeaux-Sammlung',
  WEITERBILDUNG_WEINKENNER_2: 'Champagner-Sammlung',
  WEITERBILDUNG_UHRMACHER_1: 'Uhren-Sammlungen: Schweizer, Sport',
  WEITERBILDUNG_UHRMACHER_2: 'Uhren-Sammlung: Vintage',
  WEITERBILDUNG_KUNSTKENNER_1: 'Kunst-Sammlungen: Moderne, Street Art',
  WEITERBILDUNG_KUNSTKENNER_2: 'Kunst-Sammlung: Klassisch · Luxusobjekte',
  WEITERBILDUNG_MINERALIEN_1: 'Schmuck-Sammlungen: Gold, Vintage',
  WEITERBILDUNG_MINERALIEN_2: 'Schmuck-Sammlung: Diamanten',
  WEITERBILDUNG_NUMISMATIK_1: 'Münz-Sammlung: Goldmünzen',
  WEITERBILDUNG_NUMISMATIK_2: 'Münz-Sammlung: Historisch',
  WEITERBILDUNG_PHILATELIE_1: 'Briefmarken-Sammlung',
  WEITERBILDUNG_ARCHAEOLOGIE_1: 'Vintage-Spielzeug · Kuriosa & Raritäten',
  WEITERBILDUNG_SPORTSAMMLER_1: 'Sport-Memorabilia',
  WEITERBILDUNG_WHISKY_1: 'Whisky-Sammlung',
}

onMounted(loadProgress)
</script>
