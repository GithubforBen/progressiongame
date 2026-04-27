<template>
  <div class="-m-6 flex" style="height: calc(100vh - 3.5rem)">

    <!-- Skill-tree viewport -->
    <div
      ref="viewportEl"
      class="flex-1 relative overflow-hidden"
      :class="isDragging ? 'cursor-grabbing' : 'cursor-grab'"
      @mousedown="startPan"
      @mousemove="doPan"
      @mouseup="stopPan"
      @mouseleave="stopPan"
      @wheel.prevent="doZoom"
      @click="selectedNodeKey = null"
    >
      <!-- Canvas (pannable + zoomable) -->
      <div
        class="absolute origin-top-left will-change-transform"
        :style="{ transform: `translate(${panX}px,${panY}px) scale(${zoom})`, width: CANVAS_W + 'px', height: CANVAS_H + 'px' }"
      >
        <!-- SVG edges -->
        <svg class="absolute inset-0 pointer-events-none" :width="CANVAS_W" :height="CANVAS_H" overflow="visible">
          <path
            v-for="edge in allEdges"
            :key="edge.id"
            :d="edge.d"
            :stroke="edge.color"
            stroke-width="2"
            fill="none"
            stroke-linecap="round"
          />
        </svg>

        <!-- Family labels -->
        <div
          v-for="fl in familyLabels"
          :key="fl.id"
          class="absolute text-xs font-semibold tracking-widest uppercase whitespace-nowrap pointer-events-none"
          :style="{ left: fl.x + 'px', top: fl.y + 'px', color: '#374151', transform: 'translateX(-50%)' }"
        >{{ fl.text }}</div>

        <!-- Nodes -->
        <div
          v-for="node in allNodes"
          :key="node.id"
          class="absolute flex flex-col items-center justify-center rounded-lg border transition-colors duration-150 select-none"
          :class="nodeClasses(node)"
          :style="nodeStyle(node)"
          @click.stop="handleNodeClick(node.id)"
        >
          <div class="text-sm leading-none mb-0.5">
            <span v-if="node.state === 'done'">✓</span>
            <span v-else-if="node.state === 'active'" class="animate-pulse">▶</span>
            <span v-else-if="node.state === 'locked'" class="opacity-50">🔒</span>
            <span v-else class="font-bold font-mono">{{ node.levelIndex + 1 }}</span>
          </div>
          <div class="text-xs leading-tight px-1.5 w-full text-center truncate" :class="node.type === 'main' ? 'font-semibold' : ''">
            {{ node.line1 }}
          </div>
          <div v-if="node.line2" class="text-xs opacity-60 px-1.5 w-full text-center truncate">{{ node.line2 }}</div>
        </div>
      </div>

      <!-- Progress overlay -->
      <div v-if="hasActiveProgress && !loading" class="absolute top-3 left-1/2 -translate-x-1/2 z-30 pointer-events-none" style="min-width:260px;max-width:360px">
        <div class="bg-surface-800/95 backdrop-blur border border-surface-700 rounded-xl px-4 py-3 space-y-2 pointer-events-auto shadow-xl">
          <div v-if="progress && progress.mainStageMonthsRemaining > 0" class="space-y-1">
            <div class="flex justify-between text-xs">
              <span class="text-gray-300 font-medium">{{ mainStageLabel }}</span>
              <span class="text-gray-500">{{ progress.mainStageMonthsRemaining }} Mo.</span>
            </div>
            <div class="h-1.5 bg-surface-700 rounded-full overflow-hidden">
              <div class="h-full bg-accent rounded-full" :style="{ width: `${mainProgress}%` }" />
            </div>
          </div>
          <div v-if="progress && progress.sideCertMonthsRemaining > 0" class="space-y-1">
            <div class="flex justify-between text-xs">
              <span class="text-gray-300 font-medium">{{ sideCertDisplayLabel }}</span>
              <span class="text-gray-500">{{ progress.sideCertMonthsRemaining }} Mo.</span>
            </div>
            <div class="h-1.5 bg-surface-700 rounded-full overflow-hidden">
              <div class="h-full bg-purple-500 rounded-full" :style="{ width: `${sideProgress}%` }" />
            </div>
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
      <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-surface-900/80 z-50">
        <span class="text-gray-400 text-sm animate-pulse">Lade Bildungsstand…</span>
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
            <p class="text-white font-semibold text-sm leading-snug">{{ selectedDetail.label }}</p>
            <p v-if="selectedDetail.familyLabel" class="text-gray-500 text-xs mt-0.5">{{ selectedDetail.familyLabel }}</p>
          </div>
          <button class="text-gray-500 hover:text-white text-xl leading-none flex-shrink-0" @click="selectedNodeKey = null">×</button>
        </div>

        <div class="flex-1 p-4 space-y-4">
          <span
            class="inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium border"
            :class="{
              'bg-green-500/15 border-green-500/30 text-green-400': selectedDetail.state === 'done',
              'bg-purple-500/15 border-purple-500/30 text-purple-300': selectedDetail.state === 'active',
              'bg-accent/15 border-accent/30 text-accent': selectedDetail.state === 'available',
              'bg-surface-700 border-white/5 text-gray-500': selectedDetail.state === 'locked',
            }"
          >{{ STATE_LABELS[selectedDetail.state] }}</span>

          <!-- Main stage -->
          <template v-if="selectedDetail.type === 'main'">
            <template v-if="selectedDetail.availableStage">
              <div class="text-sm text-gray-400 space-y-1">
                <div>Dauer: <span class="text-white">{{ selectedDetail.availableStage.durationMonths }} Monate</span></div>
                <div v-if="selectedDetail.availableStage.cost > 0">Kosten: <span class="text-yellow-400 font-mono">{{ formatCost(selectedDetail.availableStage.cost) }}</span></div>
              </div>
              <div v-if="selectedDetail.availableStage.requiresField" class="space-y-2">
                <label class="text-xs text-gray-500 uppercase tracking-wider">Fachrichtung</label>
                <select v-model="selectedField" class="w-full bg-surface-700 border border-surface-600 rounded px-3 py-2 text-sm text-white focus:outline-none focus:border-accent">
                  <option value="" disabled>Bitte wählen…</option>
                  <option v-for="opt in selectedDetail.availableStage.fieldOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
                <p v-if="selectedField" class="text-xs text-gray-500">
                  Dauer: {{ selectedDetail.availableStage.fieldOptions.find(o => o.value === selectedField)?.durationMonths ?? selectedDetail.availableStage.durationMonths }} Monate
                </p>
              </div>
              <button
                class="btn-primary w-full text-sm"
                :disabled="(selectedDetail.availableStage.requiresField && !selectedField) || (progress?.mainStageMonthsRemaining ?? 0) > 0"
                @click="doEnrollMain(selectedDetail.id, selectedDetail.availableStage.requiresField ? selectedField : null)"
              >Einschreiben</button>
              <p v-if="(progress?.mainStageMonthsRemaining ?? 0) > 0" class="text-xs text-gray-500">Derzeit läuft eine andere Hauptausbildung.</p>
            </template>
            <template v-else-if="selectedDetail.state === 'active'">
              <p class="text-sm text-gray-400">Noch <span class="text-white font-semibold">{{ progress?.mainStageMonthsRemaining }} Monate</span> verbleibend.</p>
            </template>
            <template v-else-if="selectedDetail.state === 'locked'">
              <p class="text-sm text-gray-500">Schließe zuerst die vorausgehende Stufe ab.</p>
            </template>
          </template>

          <!-- Side cert -->
          <template v-else>
            <div class="text-sm text-gray-400 space-y-1">
              <div v-if="selectedDetail.durationMonths">Dauer: <span class="text-white">{{ selectedDetail.durationMonths }} {{ selectedDetail.durationMonths === 1 ? 'Monat' : 'Monate' }}</span></div>
              <div v-if="selectedDetail.cost">Kosten: <span class="text-yellow-400 font-mono">{{ formatCost(selectedDetail.cost) }}</span></div>
            </div>
            <div v-if="selectedDetail.certUnlocks" class="rounded-lg bg-surface-700/60 border border-surface-600/50 px-3 py-2.5">
              <p class="text-xs text-gray-500 uppercase tracking-wider mb-1.5">Schaltet frei</p>
              <p class="text-xs text-accent leading-relaxed">{{ selectedDetail.certUnlocks }}</p>
            </div>
            <template v-if="selectedDetail.state === 'available'">
              <button
                v-if="!(progress?.sideCertMonthsRemaining)"
                class="btn-primary w-full text-sm"
                :disabled="enrollingSide === selectedDetail.id"
                @click="doEnrollSide(selectedDetail.id)"
              >{{ enrollingSide === selectedDetail.id ? '…' : 'Einschreiben' }}</button>
              <p v-else class="text-xs text-yellow-500 bg-yellow-500/10 rounded px-3 py-2 border border-yellow-500/20">Weiterbildung aktiv — erst abschließen.</p>
            </template>
            <template v-else-if="selectedDetail.state === 'locked'">
              <p class="text-sm text-gray-500">Schließe die vorausgehenden Stufen ab.</p>
            </template>
          </template>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { useApi } from '~/composables/useApi'
import { useToastStore } from '~/stores/toast'

definePageMeta({ layout: 'default' })

// ── Types ─────────────────────────────────────────────────────────────────────
interface FieldOption { value: string; label: string; durationMonths: number }
interface AvailableStage { stageKey: string; label: string; durationMonths: number; requiresField: boolean; fieldOptions: FieldOption[]; cost: number }
interface SideCert { certKey: string; label: string; durationMonths: number; cost: number }
interface Progress {
  mainStage: string; mainStageMonthsRemaining: number; mainStageField: string | null
  sideCert: string | null; sideCertMonthsRemaining: number
  completedStages: string[]; availableMainStages: AvailableStage[]; availableSideCerts: SideCert[]
}
interface StaticNode { id: string; x: number; y: number; type: 'main' | 'side'; line1: string; line2?: string; levelIndex: number; familyLabel?: string }
interface TreeNode extends StaticNode { state: 'done' | 'active' | 'available' | 'locked' }
interface SelectedDetail {
  id: string; label: string; familyLabel?: string; state: TreeNode['state']; type: 'main' | 'side'
  durationMonths?: number; cost?: number; certUnlocks?: string; availableStage?: AvailableStage
}

// ── Canvas layout constants ───────────────────────────────────────────────────
const CANVAS_W = 3100, CANVAS_H = 1300
const COL_W = 200, ROW_H = 130
const CX = 1500, CY = 100   // origin of main path
const MAIN_W = 150, MAIN_H = 60
const SIDE_W = 120, SIDE_H = 48

const nx = (col: number) => CX + col * COL_W
const ny = (row: number) => CY + row * ROW_H

// ── Static layout definitions ─────────────────────────────────────────────────
const MAIN_DEF = [
  { id: 'GRUNDSCHULE',        line1: 'Grundschule',  line2: undefined,    col: 0,  row: 0 },
  { id: 'REALSCHULABSCHLUSS', line1: 'Realschul-',   line2: 'abschluss', col: 0,  row: 1 },
  { id: 'AUSBILDUNG',         line1: 'Ausbildung',   line2: undefined,    col: -2, row: 2 },
  { id: 'ABITUR',             line1: 'Abitur',       line2: undefined,    col: 0,  row: 3 },
  { id: 'BACHELOR',           line1: 'Bachelor',     line2: undefined,    col: 0,  row: 5 },
  { id: 'MASTER',             line1: 'Master',       line2: undefined,    col: 0,  row: 7 },
]

// dir: +1 = levels extend right, -1 = extend left
const FAM_DEF = [
  { name: 'BARKEEPER',         label: 'Barkeeper',       row: 1, sc: 1,  dir: 1,  anchor: 'REALSCHULABSCHLUSS', levels: ['WEITERBILDUNG_BARKEEPER_1','WEITERBILDUNG_BARKEEPER_2','WEITERBILDUNG_BARKEEPER_3'] },
  { name: 'FITNESSTRAINER',    label: 'Fitnesstrainer',  row: 1, sc: 4,  dir: 1,  anchor: 'REALSCHULABSCHLUSS', levels: ['WEITERBILDUNG_FITNESSTRAINER_1','WEITERBILDUNG_FITNESSTRAINER_2','WEITERBILDUNG_FITNESSTRAINER_3'] },
  { name: 'SOCIAL_MEDIA',      label: 'Social-Media',    row: 2, sc: 1,  dir: 1,  anchor: 'REALSCHULABSCHLUSS', levels: ['WEITERBILDUNG_SOCIAL_MEDIA_1','WEITERBILDUNG_SOCIAL_MEDIA_2','WEITERBILDUNG_SOCIAL_MEDIA_3'] },
  { name: 'EXCEL',             label: 'Excel',           row: 2, sc: 4,  dir: 1,  anchor: 'REALSCHULABSCHLUSS', levels: ['WEITERBILDUNG_EXCEL_1','WEITERBILDUNG_EXCEL_2','WEITERBILDUNG_EXCEL_3'] },
  { name: 'FUEHRERSCHEIN',     label: 'Führerschein',    row: 3, sc: 1,  dir: 1,  anchor: 'ABITUR',             levels: ['WEITERBILDUNG_FUEHRERSCHEIN_1','WEITERBILDUNG_FUEHRERSCHEIN_2','WEITERBILDUNG_FUEHRERSCHEIN_3'] },
  { name: 'CRYPTO',            label: 'Krypto-Trading',  row: 3, sc: 4,  dir: 1,  anchor: 'ABITUR',             levels: ['WEITERBILDUNG_CRYPTO_1','WEITERBILDUNG_CRYPTO_2','WEITERBILDUNG_CRYPTO_3'] },
  { name: 'BUCHHALTUNG',       label: 'Buchhaltung',     row: 4, sc: 1,  dir: 1,  anchor: 'ABITUR',             levels: ['WEITERBILDUNG_BUCHHALTUNG_1','WEITERBILDUNG_BUCHHALTUNG_2','WEITERBILDUNG_BUCHHALTUNG_3'] },
  { name: 'IMMOBILIEN',        label: 'Immobilien',      row: 4, sc: 4,  dir: 1,  anchor: 'ABITUR',             levels: ['WEITERBILDUNG_IMMOBILIEN_1','WEITERBILDUNG_IMMOBILIEN_2','WEITERBILDUNG_IMMOBILIEN_3','WEITERBILDUNG_IMMOBILIEN_4'] },
  { name: 'STEUERN',           label: 'Steuern',         row: 5, sc: 1,  dir: 1,  anchor: 'BACHELOR',           levels: ['WEITERBILDUNG_STEUERN_1','WEITERBILDUNG_STEUERN_2','WEITERBILDUNG_STEUERN_3'] },
  { name: 'PROJEKTMANAGEMENT', label: 'Projektmgmt.',    row: 5, sc: 4,  dir: 1,  anchor: 'BACHELOR',           levels: ['WEITERBILDUNG_PROJEKTMANAGEMENT_1','WEITERBILDUNG_PROJEKTMANAGEMENT_2','WEITERBILDUNG_PROJEKTMANAGEMENT_3'] },
  { name: 'HACKER',            label: 'Ethical Hacking', row: 6, sc: 1,  dir: 1,  anchor: 'BACHELOR',           levels: ['WEITERBILDUNG_HACKER_1','WEITERBILDUNG_HACKER_2','WEITERBILDUNG_HACKER_3'] },
  { name: 'STEUERHINTERZIEHUNG', label: 'Steuerhinterziehung', row: 6, sc: 4, dir: 1, anchor: 'BACHELOR', levels: ['WEITERBILDUNG_STEUERHINTERZIEHUNG_1','WEITERBILDUNG_STEUERHINTERZIEHUNG_2','WEITERBILDUNG_STEUERHINTERZIEHUNG_3'] },
  { name: 'OLDTIMER',          label: 'Oldtimer',        row: 3, sc: -1, dir: -1, anchor: 'ABITUR',             levels: ['WEITERBILDUNG_OLDTIMER_1','WEITERBILDUNG_OLDTIMER_2','WEITERBILDUNG_OLDTIMER_3'] },
  { name: 'WEINKENNER',        label: 'Weinkenner',      row: 3, sc: -4, dir: -1, anchor: 'ABITUR',             levels: ['WEITERBILDUNG_WEINKENNER_1','WEITERBILDUNG_WEINKENNER_2','WEITERBILDUNG_WEINKENNER_3'] },
  { name: 'KUNSTKENNER',       label: 'Kunstkenner',     row: 4, sc: -1, dir: -1, anchor: 'ABITUR',             levels: ['WEITERBILDUNG_KUNSTKENNER_1','WEITERBILDUNG_KUNSTKENNER_2','WEITERBILDUNG_KUNSTKENNER_3'] },
  { name: 'UHRMACHER',         label: 'Uhrmacher',       row: 4, sc: -4, dir: -1, anchor: 'ABITUR',             levels: ['WEITERBILDUNG_UHRMACHER_1','WEITERBILDUNG_UHRMACHER_2','WEITERBILDUNG_UHRMACHER_3'] },
  { name: 'NUMISMATIK',        label: 'Numismatik',      row: 5, sc: -1, dir: -1, anchor: 'BACHELOR',           levels: ['WEITERBILDUNG_NUMISMATIK_1','WEITERBILDUNG_NUMISMATIK_2'] },
  { name: 'PHILATELIE',        label: 'Philatelie',      row: 5, sc: -3, dir: -1, anchor: 'BACHELOR',           levels: ['WEITERBILDUNG_PHILATELIE_1','WEITERBILDUNG_PHILATELIE_2'] },
  { name: 'MINERALIEN',        label: 'Gemmologie',      row: 5, sc: -5, dir: -1, anchor: 'BACHELOR',           levels: ['WEITERBILDUNG_MINERALIEN_1','WEITERBILDUNG_MINERALIEN_2','WEITERBILDUNG_MINERALIEN_3'] },
  { name: 'ARCHAEOLOGIE',      label: 'Archäologie',     row: 6, sc: -1, dir: -1, anchor: 'BACHELOR',           levels: ['WEITERBILDUNG_ARCHAEOLOGIE_1','WEITERBILDUNG_ARCHAEOLOGIE_2'] },
  { name: 'SPORTSAMMLER',      label: 'Sportsammler',    row: 6, sc: -3, dir: -1, anchor: 'BACHELOR',           levels: ['WEITERBILDUNG_SPORTSAMMLER_1','WEITERBILDUNG_SPORTSAMMLER_2'] },
  { name: 'WHISKY',            label: 'Whisky',          row: 6, sc: -5, dir: -1, anchor: 'BACHELOR',           levels: ['WEITERBILDUNG_WHISKY_1','WEITERBILDUNG_WHISKY_2'] },
]

// Build static node map once (non-reactive)
const STATIC: Record<string, StaticNode> = {}
for (const m of MAIN_DEF) {
  STATIC[m.id] = { id: m.id, x: nx(m.col), y: ny(m.row), type: 'main', line1: m.line1, line2: m.line2, levelIndex: 0 }
}
for (const fam of FAM_DEF) {
  for (let i = 0; i < fam.levels.length; i++) {
    const id = fam.levels[i]
    STATIC[id] = { id, x: nx(fam.sc + fam.dir * i), y: ny(fam.row), type: 'side', line1: fam.label, levelIndex: i, familyLabel: fam.label }
  }
}

// ── Side cert data ────────────────────────────────────────────────────────────
const CERT_DURATIONS: Record<string, number> = {
  WEITERBILDUNG_BARKEEPER_1: 1, WEITERBILDUNG_BARKEEPER_2: 2, WEITERBILDUNG_BARKEEPER_3: 2,
  WEITERBILDUNG_FITNESSTRAINER_1: 1, WEITERBILDUNG_FITNESSTRAINER_2: 2, WEITERBILDUNG_FITNESSTRAINER_3: 2,
  WEITERBILDUNG_SOCIAL_MEDIA_1: 1, WEITERBILDUNG_SOCIAL_MEDIA_2: 2, WEITERBILDUNG_SOCIAL_MEDIA_3: 2,
  WEITERBILDUNG_EXCEL_1: 1, WEITERBILDUNG_EXCEL_2: 2, WEITERBILDUNG_EXCEL_3: 2,
  WEITERBILDUNG_FUEHRERSCHEIN_1: 1, WEITERBILDUNG_FUEHRERSCHEIN_2: 2, WEITERBILDUNG_FUEHRERSCHEIN_3: 3,
  WEITERBILDUNG_CRYPTO_1: 1, WEITERBILDUNG_CRYPTO_2: 2, WEITERBILDUNG_CRYPTO_3: 3,
  WEITERBILDUNG_BUCHHALTUNG_1: 1, WEITERBILDUNG_BUCHHALTUNG_2: 2, WEITERBILDUNG_BUCHHALTUNG_3: 3,
  WEITERBILDUNG_IMMOBILIEN_1: 2, WEITERBILDUNG_IMMOBILIEN_2: 3, WEITERBILDUNG_IMMOBILIEN_3: 4, WEITERBILDUNG_IMMOBILIEN_4: 3,
  WEITERBILDUNG_PROJEKTMANAGEMENT_1: 2, WEITERBILDUNG_PROJEKTMANAGEMENT_2: 3, WEITERBILDUNG_PROJEKTMANAGEMENT_3: 4,
  WEITERBILDUNG_STEUERN_1: 2, WEITERBILDUNG_STEUERN_2: 3, WEITERBILDUNG_STEUERN_3: 4,
  WEITERBILDUNG_STEUERHINTERZIEHUNG_1: 2, WEITERBILDUNG_STEUERHINTERZIEHUNG_2: 3, WEITERBILDUNG_STEUERHINTERZIEHUNG_3: 4,
  WEITERBILDUNG_HACKER_1: 2, WEITERBILDUNG_HACKER_2: 3, WEITERBILDUNG_HACKER_3: 4,
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
  WEITERBILDUNG_BARKEEPER_1: 'Barkeeper-Grundkurs', WEITERBILDUNG_BARKEEPER_2: 'Barkeeper-Aufbaukurs', WEITERBILDUNG_BARKEEPER_3: 'Barkeeper-Meister',
  WEITERBILDUNG_FITNESSTRAINER_1: 'Fitnesstrainer B-Lizenz', WEITERBILDUNG_FITNESSTRAINER_2: 'Fitnesstrainer A-Lizenz', WEITERBILDUNG_FITNESSTRAINER_3: 'Personal Trainer Zertifikat',
  WEITERBILDUNG_SOCIAL_MEDIA_1: 'Social-Media-Marketing (Grundkurs)', WEITERBILDUNG_SOCIAL_MEDIA_2: 'Social-Media-Marketing (Aufbaukurs)', WEITERBILDUNG_SOCIAL_MEDIA_3: 'Social-Media-Marketing (Expertenzertifikat)',
  WEITERBILDUNG_EXCEL_1: 'Excel-Grundlagen', WEITERBILDUNG_EXCEL_2: 'Excel & Datenanalyse', WEITERBILDUNG_EXCEL_3: 'Excel VBA & Power BI',
  WEITERBILDUNG_FUEHRERSCHEIN_1: 'Führerschein Klasse B', WEITERBILDUNG_FUEHRERSCHEIN_2: 'Führerschein Klasse BE', WEITERBILDUNG_FUEHRERSCHEIN_3: 'Führerschein Klasse C (LKW)',
  WEITERBILDUNG_CRYPTO_1: 'Krypto-Trading (Grundlagen)', WEITERBILDUNG_CRYPTO_2: 'DeFi & Blockchain', WEITERBILDUNG_CRYPTO_3: 'Certified Crypto Analyst',
  WEITERBILDUNG_BUCHHALTUNG_1: 'Buchhaltung & DATEV', WEITERBILDUNG_BUCHHALTUNG_2: 'Bilanzbuchhaltung (IHK)', WEITERBILDUNG_BUCHHALTUNG_3: 'Steuerrecht & Konzernbilanzierung',
  WEITERBILDUNG_IMMOBILIEN_1: 'Immobilien-Grundlagen (IHK)', WEITERBILDUNG_IMMOBILIEN_2: 'Immobilienmakler-Lizenz', WEITERBILDUNG_IMMOBILIEN_3: 'Immobilien-Investor Masterclass', WEITERBILDUNG_IMMOBILIEN_4: 'Immobilien-Portfolio Manager',
  WEITERBILDUNG_PROJEKTMANAGEMENT_1: 'PRINCE2 Foundation', WEITERBILDUNG_PROJEKTMANAGEMENT_2: 'PRINCE2 Practitioner', WEITERBILDUNG_PROJEKTMANAGEMENT_3: 'Agile Coach (SAFe)',
  WEITERBILDUNG_STEUERN_1: 'Steuerberater-Grundkurs', WEITERBILDUNG_STEUERN_2: 'Steuerberater-Aufbaukurs', WEITERBILDUNG_STEUERN_3: 'Steuerberater-Examen',
  WEITERBILDUNG_STEUERHINTERZIEHUNG_1: 'Bargeldzahlungen (Stufe 1)', WEITERBILDUNG_STEUERHINTERZIEHUNG_2: 'Briefkastenfirma (Stufe 2)', WEITERBILDUNG_STEUERHINTERZIEHUNG_3: 'Offshore-Konten (Stufe 3)',
  WEITERBILDUNG_HACKER_1: 'Ethical Hacking (CEH Foundation)', WEITERBILDUNG_HACKER_2: 'Certified Ethical Hacker', WEITERBILDUNG_HACKER_3: 'Offensive Security Expert (OSCP)',
  WEITERBILDUNG_OLDTIMER_1: 'Oldtimer-Grundkurs', WEITERBILDUNG_OLDTIMER_2: 'Classic-Car Experte', WEITERBILDUNG_OLDTIMER_3: 'Oldtimer-Auktionator',
  WEITERBILDUNG_ARCHAEOLOGIE_1: 'Archäologen-Hobbykurs', WEITERBILDUNG_ARCHAEOLOGIE_2: 'Antiquitäten-Experte',
  WEITERBILDUNG_WEINKENNER_1: 'Weinkenner Grundkurs', WEITERBILDUNG_WEINKENNER_2: 'Wine & Spirit Education (WSET)', WEITERBILDUNG_WEINKENNER_3: 'Master Sommelier',
  WEITERBILDUNG_KUNSTKENNER_1: 'Kunstgeschichte Einführung', WEITERBILDUNG_KUNSTKENNER_2: 'Kunstmarkt-Experte', WEITERBILDUNG_KUNSTKENNER_3: 'Art Advisor Zertifikat',
  WEITERBILDUNG_UHRMACHER_1: 'Uhrmacher-Grundkurs', WEITERBILDUNG_UHRMACHER_2: 'Zertifizierter Uhrenexperte', WEITERBILDUNG_UHRMACHER_3: 'Horologie Diplom',
  WEITERBILDUNG_NUMISMATIK_1: 'Münzkunde Grundkurs', WEITERBILDUNG_NUMISMATIK_2: 'Professioneller Numismatiker',
  WEITERBILDUNG_PHILATELIE_1: 'Briefmarken-Sammler Kurs', WEITERBILDUNG_PHILATELIE_2: 'Philatelie-Experte',
  WEITERBILDUNG_MINERALIEN_1: 'Gemmologie Grundkurs', WEITERBILDUNG_MINERALIEN_2: 'Zertifizierter Gemmologe (FGA)', WEITERBILDUNG_MINERALIEN_3: 'Diamond Grading Expert',
  WEITERBILDUNG_SPORTSAMMLER_1: 'Sport-Memorabilia Grundkurs', WEITERBILDUNG_SPORTSAMMLER_2: 'Sportartefakt-Authentifizierer',
  WEITERBILDUNG_WHISKY_1: 'Whisky & Spirituosen Grundkurs', WEITERBILDUNG_WHISKY_2: 'Master Distiller Zertifikat',
}

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

const STAGE_LABELS: Record<string, string> = {
  GRUNDSCHULE: 'Grundschule', REALSCHULABSCHLUSS: 'Realschulabschluss',
  ABITUR: 'Abitur', AUSBILDUNG: 'Ausbildung', BACHELOR: 'Bachelor-Studium', MASTER: 'Master-Studium',
}
const STATE_LABELS: Record<string, string> = {
  done: '✓ Abgeschlossen', active: '▶ In Bearbeitung', available: 'Verfügbar', locked: '🔒 Gesperrt',
}
const STAGE_DURATIONS: Record<string, number> = {
  REALSCHULABSCHLUSS: 2, ABITUR: 3, AUSBILDUNG: 4, BACHELOR: 6, MASTER: 4,
  BACHELOR_MEDIZIN: 8, BACHELOR_JURA: 7,
}
const FIELD_LABELS: Record<string, string> = {
  INFORMATIK: 'Informatik', BWL: 'BWL', MEDIZIN: 'Medizin', JURA: 'Rechtswiss.',
  INGENIEURWESEN: 'Ingenieurwesen', PSYCHOLOGIE: 'Psychologie',
  FACHINFORMATIKER: 'Fachinformatiker/-in', EINZELHANDEL: 'Einzelhandel',
  KFZTECH: 'KFZ-Mechatronik', PFLEGE: 'Pflegefachkraft', KOCH: 'Koch/Köchin', ELEKTRIKER: 'Elektriker/-in',
}
const MAIN_IDS = new Set(['GRUNDSCHULE', 'REALSCHULABSCHLUSS', 'ABITUR', 'AUSBILDUNG', 'BACHELOR', 'MASTER'])

// ── Reactive state ────────────────────────────────────────────────────────────
const api = useApi()
const toast = useToastStore()
const route = useRoute()

const progress = ref<Progress | null>(null)
const loading = ref(true)
const enrollingSide = ref<string | null>(null)

const panX = ref(0), panY = ref(0), zoom = ref(0.8)
const isDragging = ref(false), hasDragged = ref(false)
let lastMX = 0, lastMY = 0

const selectedNodeKey = ref<string | null>(null)
const selectedField = ref('')
const viewportEl = ref<HTMLElement | null>(null)

const highlightCert = computed(() => route.query.highlight as string | undefined)

// ── Node state ────────────────────────────────────────────────────────────────
function getState(id: string): TreeNode['state'] {
  const p = progress.value
  if (!p) return id === 'GRUNDSCHULE' ? 'done' : 'locked'
  if ((p.completedStages ?? []).includes(id)) return 'done'
  if (MAIN_IDS.has(id)) {
    if (p.mainStage === id && p.mainStageMonthsRemaining > 0) return 'active'
    return (p.availableMainStages ?? []).some(s => s.stageKey === id) ? 'available' : 'locked'
  }
  if (p.sideCert === id) return 'active'
  return (p.availableSideCerts ?? []).some(c => c.certKey === id) ? 'available' : 'locked'
}

const allNodes = computed((): TreeNode[] =>
  Object.values(STATIC).map(n => ({ ...n, state: getState(n.id) }))
)

const nodeMap = computed(() => {
  const m: Record<string, TreeNode> = {}
  for (const n of allNodes.value) m[n.id] = n
  return m
})

// ── Edges ─────────────────────────────────────────────────────────────────────
function edgeColor(a: string, b: string): string {
  if (a === 'done' && b === 'done') return 'rgba(34,197,94,0.28)'
  if (a === 'done' && b === 'available') return 'rgba(99,102,241,0.55)'
  if (a === 'done' && b === 'active') return 'rgba(168,85,247,0.5)'
  return 'rgba(255,255,255,0.07)'
}

function spath(x1: number, y1: number, x2: number, y2: number): string {
  if (Math.abs(y2 - y1) < 5 || Math.abs(x2 - x1) < 5) return `M${x1} ${y1}L${x2} ${y2}`
  const mx = (x1 + x2) / 2
  return `M${x1} ${y1}C${mx} ${y1} ${mx} ${y2} ${x2} ${y2}`
}

const allEdges = computed(() => {
  const edges: { id: string; d: string; color: string }[] = []
  const nm = nodeMap.value

  // Main vertical chain
  const mainChain = ['GRUNDSCHULE', 'REALSCHULABSCHLUSS', 'ABITUR', 'BACHELOR', 'MASTER']
  for (let i = 0; i < mainChain.length - 1; i++) {
    const a = nm[mainChain[i]], b = nm[mainChain[i + 1]]
    if (a && b) edges.push({ id: `m${i}`, d: spath(a.x, a.y + MAIN_H / 2, b.x, b.y - MAIN_H / 2), color: edgeColor(a.state, b.state) })
  }

  // Alt branch: REALSCHULABSCHLUSS → AUSBILDUNG
  const r = nm['REALSCHULABSCHLUSS'], aus = nm['AUSBILDUNG']
  if (r && aus) edges.push({ id: 'aus', d: spath(r.x - MAIN_W / 2, r.y, aus.x + MAIN_W / 2, aus.y), color: edgeColor(r.state, aus.state) })

  // Family anchor + level chains
  for (const fam of FAM_DEF) {
    const anch = nm[fam.anchor]
    const fnodes = fam.levels.map(id => nm[id]).filter((n): n is TreeNode => !!n)
    if (!anch || !fnodes.length) continue
    const first = fnodes[0]
    const anchor_d = fam.dir > 0
      ? spath(anch.x + MAIN_W / 2, anch.y, first.x - SIDE_W / 2, first.y)
      : spath(anch.x - MAIN_W / 2, anch.y, first.x + SIDE_W / 2, first.y)
    edges.push({ id: `a_${fam.name}`, d: anchor_d, color: edgeColor(anch.state, first.state) })
    for (let i = 0; i < fnodes.length - 1; i++) {
      const a = fnodes[i], b = fnodes[i + 1]
      const d = fam.dir > 0
        ? spath(a.x + SIDE_W / 2, a.y, b.x - SIDE_W / 2, b.y)
        : spath(a.x - SIDE_W / 2, a.y, b.x + SIDE_W / 2, b.y)
      edges.push({ id: `${a.id}_${b.id}`, d, color: edgeColor(a.state, b.state) })
    }
  }
  return edges
})

// ── Family labels ─────────────────────────────────────────────────────────────
const familyLabels = computed(() =>
  FAM_DEF.map(fam => {
    const ns = fam.levels.map(id => nodeMap.value[id]).filter(Boolean)
    if (!ns.length) return null
    const cx = (ns[0].x + ns[ns.length - 1].x) / 2
    return { id: fam.name, text: fam.label, x: cx, y: ns[0].y - SIDE_H / 2 - 18 }
  }).filter((x): x is NonNullable<typeof x> => x !== null)
)

// ── Selected detail ───────────────────────────────────────────────────────────
const selectedDetail = computed((): SelectedDetail | null => {
  const key = selectedNodeKey.value
  if (!key) return null
  const p = progress.value
  const state = getState(key)
  if (MAIN_IDS.has(key)) {
    return { id: key, label: STAGE_LABELS[key] ?? key, state, type: 'main', availableStage: p?.availableMainStages.find(s => s.stageKey === key) }
  }
  const avail = p?.availableSideCerts.find(c => c.certKey === key)
  return {
    id: key, label: SIDE_CERT_LABELS[key] ?? key,
    familyLabel: STATIC[key]?.familyLabel,
    state, type: 'side',
    durationMonths: avail?.durationMonths ?? CERT_DURATIONS[key],
    cost: avail?.cost,
    certUnlocks: CERT_UNLOCKS[key],
  }
})

// ── Progress helpers ──────────────────────────────────────────────────────────
const hasActiveProgress = computed(() => !!(progress.value?.mainStageMonthsRemaining || progress.value?.sideCertMonthsRemaining))

const mainStageLabel = computed(() => {
  const p = progress.value
  if (!p) return ''
  return (STAGE_LABELS[p.mainStage] ?? p.mainStage) + (p.mainStageField ? ': ' + (FIELD_LABELS[p.mainStageField] ?? p.mainStageField) : '')
})

const sideCertDisplayLabel = computed(() => SIDE_CERT_LABELS[progress.value?.sideCert ?? ''] ?? progress.value?.sideCert ?? '')

const mainProgress = computed(() => {
  const p = progress.value
  if (!p || !p.mainStageMonthsRemaining) return 100
  const key = p.mainStageField ? `${p.mainStage}_${p.mainStageField}` : p.mainStage
  const total = STAGE_DURATIONS[key] ?? STAGE_DURATIONS[p.mainStage] ?? 1
  return Math.round(((total - p.mainStageMonthsRemaining) / total) * 100)
})

const sideProgress = computed(() => {
  const p = progress.value
  if (!p || !p.sideCertMonthsRemaining) return 100
  const total = CERT_DURATIONS[p.sideCert ?? ''] ?? 1
  return Math.round(((total - p.sideCertMonthsRemaining) / total) * 100)
})

// ── Node render helpers ───────────────────────────────────────────────────────
function nodeClasses(node: TreeNode) {
  const sel = selectedNodeKey.value === node.id
  return [
    node.state === 'done'      ? 'bg-green-500/15 border-green-500/30 text-green-400' :
    node.state === 'active'    ? 'bg-purple-500/15 border-purple-500/30 text-purple-300' :
    node.state === 'available' ? 'bg-accent/10 border-accent/35 text-accent hover:bg-accent/20 cursor-pointer node-pulse' :
                                 'bg-surface-700 border-white/5 text-gray-600',
    sel ? 'ring-2 ring-white/30' : '',
    node.state !== 'locked' && node.state !== 'done' ? 'cursor-pointer' : '',
  ]
}

function nodeStyle(node: StaticNode) {
  const w = node.type === 'main' ? MAIN_W : SIDE_W
  const h = node.type === 'main' ? MAIN_H : SIDE_H
  return { left: `${node.x - w / 2}px`, top: `${node.y - h / 2}px`, width: `${w}px`, height: `${h}px` }
}

// ── Pan / zoom ────────────────────────────────────────────────────────────────
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
  const mx = e.clientX - rect.left, my = e.clientY - rect.top
  const ratio = newZoom / zoom.value
  panX.value = mx - ratio * (mx - panX.value)
  panY.value = my - ratio * (my - panY.value)
  zoom.value = newZoom
}

function handleNodeClick(id: string) {
  if (hasDragged.value) return
  selectedNodeKey.value = selectedNodeKey.value === id ? null : id
  selectedField.value = ''
}

function centerOn(x: number, y: number) {
  const el = viewportEl.value
  if (!el) return
  panX.value = el.clientWidth / 2 - x * zoom.value
  panY.value = el.clientHeight / 2 - y * zoom.value
}

function resetView() {
  zoom.value = 0.8
  const el = viewportEl.value
  if (!el) return
  panX.value = el.clientWidth / 2 - CX * zoom.value
  panY.value = 80 - CY * zoom.value
}

// ── API actions ───────────────────────────────────────────────────────────────
async function loadProgress() {
  loading.value = true
  try {
    progress.value = await api.get<Progress>('/api/education')
  } finally {
    loading.value = false
    await nextTick()
    if (highlightCert.value && STATIC[highlightCert.value]) {
      zoom.value = 1.1
      centerOn(STATIC[highlightCert.value].x, STATIC[highlightCert.value].y)
      selectedNodeKey.value = highlightCert.value
    } else {
      resetView()
    }
  }
}

async function doEnrollMain(stage: string, field: string | null) {
  try {
    progress.value = await api.post<Progress>('/api/education/main', { stage, field })
    toast.success('Einschreibung erfolgreich!', 'Ausbildung gestartet')
    selectedNodeKey.value = null
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Einschreibung fehlgeschlagen.')
  }
}

async function doEnrollSide(certKey: string) {
  enrollingSide.value = certKey
  try {
    progress.value = await api.post<Progress>('/api/education/side', { cert: certKey })
    toast.success('Weiterbildung gestartet!', 'Eingeschrieben')
    selectedNodeKey.value = null
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Einschreibung fehlgeschlagen.')
  } finally {
    enrollingSide.value = null
  }
}

function formatCost(cost: number) {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(cost)
}

onMounted(loadProgress)
</script>

<style scoped>
@keyframes node-glow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.35); }
  50%       { box-shadow: 0 0 0 8px rgba(99, 102, 241, 0); }
}
.node-pulse { animation: node-glow 2.2s ease-in-out infinite; }
</style>
