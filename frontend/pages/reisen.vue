<template>
  <div class="-m-6 flex flex-col" style="height: calc(100vh - 3.5rem)">

    <!-- Top bar -->
    <div class="flex items-center gap-4 px-6 py-3 bg-surface-800 border-b border-surface-700 flex-shrink-0">
      <h2 class="text-base font-bold text-white">Reisen</h2>

      <!-- Current location badge -->
      <div v-if="status?.traveling" class="flex items-center gap-2 px-3 py-1 rounded-full bg-blue-500/15 border border-blue-500/30">
        <span class="text-sm">✈</span>
        <span class="text-blue-300 text-xs">Unterwegs nach {{ status.destinationCountry }} — Monat {{ status.arriveAtTurn }}</span>
      </div>
      <div v-else-if="status?.currentCountry" class="flex items-center gap-2 px-3 py-1 rounded-full bg-green-500/15 border border-green-500/30">
        <span class="text-sm">{{ countryEmoji(status.currentCountry) }}</span>
        <span class="text-green-300 text-xs">{{ status.currentCountry }}</span>
        <button @click="returnHome" :disabled="!!actionLoading" class="text-xs text-green-300/60 hover:text-green-300 ml-1">← Heimreise</button>
      </div>
      <div v-else class="flex items-center gap-2 px-3 py-1 rounded-full bg-white/5 border border-white/10">
        <span class="text-sm">🏠</span>
        <span class="text-gray-400 text-xs">Deutschland</span>
      </div>

      <!-- Active events badge -->
      <div v-if="activeEvents.length" class="flex items-center gap-2 px-3 py-1 rounded-full bg-yellow-500/15 border border-yellow-500/30">
        <span class="text-xs text-yellow-300">⭐ {{ activeEvents.length }} Event{{ activeEvents.length > 1 ? 's' : '' }} aktiv</span>
      </div>

      <!-- Visited counter -->
      <div class="flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/5 border border-white/10">
        <span class="text-xs text-gray-400">{{ visitedCount }}/{{ countries.length }} Länder besucht</span>
      </div>

      <div class="flex-1" />
      <button @click="resetView" class="text-xs text-gray-500 hover:text-gray-300 font-mono transition-colors">Ansicht zurücksetzen</button>
    </div>

    <!-- Map + Detail panel -->
    <div class="flex flex-1 overflow-hidden relative">

      <!-- SVG Map -->
      <div
        class="flex-1 overflow-hidden bg-surface-900 cursor-grab active:cursor-grabbing select-none relative"
        @mousedown="startPan"
        @mousemove="doPan"
        @mouseup="stopPan"
        @mouseleave="stopPan"
        @wheel.prevent="doZoom"
        ref="mapEl"
      >
        <div
          class="absolute origin-top-left"
          :style="{ transform: `translate(${panX}px, ${panY}px) scale(${zoom})` }"
        >
          <!-- Ocean background -->
          <svg :width="MAP_W" :height="MAP_H" class="block">
            <!-- Ocean -->
            <rect x="0" y="0" :width="MAP_W" :height="MAP_H" fill="#0a1628" />

            <!-- Country shapes from TopoJSON (Natural Earth 110m) -->
            <path
              v-for="cp in countryPaths"
              :key="cp.id"
              :d="cp.d"
              :fill="cp.isGame ? '#1f3a1f' : '#1a2a1a'"
              :stroke="cp.isGame ? '#3a5a3a' : '#2a3a2a'"
              stroke-width="0.5"
              stroke-linejoin="round"
            />

            <!-- Grid lines -->
            <line v-for="x in 20" :key="'vl'+x" :x1="x*MAP_W/20" y1="0" :x2="x*MAP_W/20" :y2="MAP_H"
              stroke="#ffffff04" stroke-width="1" />
            <line v-for="y in 12" :key="'hl'+y" x1="0" :y1="y*MAP_H/12" :x2="MAP_W" :y2="y*MAP_H/12"
              stroke="#ffffff04" stroke-width="1" />

            <!-- Continent labels -->
            <text x="490" y="88" text-anchor="middle" font-size="9" fill="#334155" font-weight="bold" letter-spacing="3" style="user-select:none">EUROPA</text>
            <text x="480" y="260" text-anchor="middle" font-size="9" fill="#334155" font-weight="bold" letter-spacing="3" style="user-select:none">AFRIKA</text>
            <text x="700" y="60" text-anchor="middle" font-size="9" fill="#334155" font-weight="bold" letter-spacing="3" style="user-select:none">ASIEN</text>
            <text x="185" y="60" text-anchor="middle" font-size="9" fill="#334155" font-weight="bold" letter-spacing="3" style="user-select:none">NORDAMERIKA</text>
            <text x="235" y="390" text-anchor="middle" font-size="9" fill="#334155" font-weight="bold" letter-spacing="3" style="user-select:none">SÜDAMERIKA</text>
            <text x="820" y="260" text-anchor="middle" font-size="9" fill="#334155" font-weight="bold" letter-spacing="3" style="user-select:none">AUSTRALIEN</text>

            <!-- Travel route line (dashed, from current/home to destination) -->
            <line
              v-if="travelRouteFrom && travelRouteTo"
              :x1="travelRouteFrom.x" :y1="travelRouteFrom.y"
              :x2="travelRouteTo.x" :y2="travelRouteTo.y"
              stroke="#3b82f6" stroke-width="1.5" stroke-dasharray="6 4" opacity="0.5"
            />

            <!-- Legend -->
            <g transform="translate(16, 400)">
              <circle cx="6" cy="6" r="5" fill="#22c55e" />
              <text x="14" y="10" font-size="7.5" fill="#94a3b8" style="user-select:none">Aktuell hier</text>
              <circle cx="6" cy="20" r="5" fill="#3b82f6" />
              <text x="14" y="24" font-size="7.5" fill="#94a3b8" style="user-select:none">Reiseziel</text>
              <circle cx="6" cy="34" r="5" fill="#6366f1" />
              <text x="14" y="38" font-size="7.5" fill="#94a3b8" style="user-select:none">Besucht</text>
              <circle cx="6" cy="48" r="5" fill="#334155" />
              <text x="14" y="52" font-size="7.5" fill="#94a3b8" style="user-select:none">Nicht besucht</text>
            </g>

            <!-- Country circles -->
            <g
              v-for="country in countries"
              :key="country.id"
              class="cursor-pointer"
              @click.stop="selectCountry(country)"
            >
              <!-- Glow for current location -->
              <circle
                v-if="country.currentlyHere"
                :cx="coords(country.name).x"
                :cy="coords(country.name).y"
                r="22"
                fill="rgba(34,197,94,0.15)"
                class="animate-pulse"
              />
              <!-- Glow for destination -->
              <circle
                v-else-if="country.travelingHere"
                :cx="coords(country.name).x"
                :cy="coords(country.name).y"
                r="20"
                fill="rgba(59,130,246,0.15)"
                class="animate-pulse"
              />
              <!-- Main circle -->
              <circle
                :cx="coords(country.name).x"
                :cy="coords(country.name).y"
                r="12"
                :fill="country.currentlyHere ? '#22c55e' : country.travelingHere ? '#3b82f6' : country.visited ? '#6366f1' : '#334155'"
                :stroke="selectedCountry?.id === country.id ? '#a5b4fc' : country.currentlyHere ? '#4ade80' : '#475569'"
                :stroke-width="selectedCountry?.id === country.id ? 2.5 : 1.5"
              />
              <!-- Emoji -->
              <text
                :x="coords(country.name).x"
                :y="coords(country.name).y + 5"
                text-anchor="middle"
                font-size="11"
                style="pointer-events: none; user-select: none;"
              >{{ country.emoji }}</text>
              <!-- Label -->
              <text
                :x="coords(country.name).x"
                :y="coords(country.name).y + 24"
                text-anchor="middle"
                font-size="8.5"
                :fill="country.currentlyHere ? '#4ade80' : country.travelingHere ? '#93c5fd' : '#94a3b8'"
                style="pointer-events: none; user-select: none;"
              >{{ country.name }}</text>
            </g>
          </svg>
        </div>

        <!-- Zoom hint -->
        <div class="absolute bottom-3 left-3 text-gray-600 text-xs font-mono pointer-events-none">
          Scroll: Zoom · Drag: Pan · Klick: Details
        </div>
        <div class="absolute bottom-3 right-3 text-gray-700 text-xs font-mono pointer-events-none">
          {{ Math.round(zoom * 100) }}%
        </div>
      </div>

      <!-- Detail panel -->
      <Transition
        enter-active-class="transition-all duration-300 ease-out"
        leave-active-class="transition-all duration-200 ease-in"
        enter-from-class="translate-x-full opacity-0"
        leave-to-class="translate-x-full opacity-0"
      >
        <div
          v-if="selectedCountry"
          class="w-72 bg-surface-800 border-l border-surface-700 flex flex-col overflow-y-auto flex-shrink-0"
          @mousedown.stop
        >
          <div class="p-5 flex flex-col gap-4">
            <!-- Header -->
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <span class="text-3xl">{{ selectedCountry.emoji }}</span>
                <div>
                  <h3 class="text-white font-bold">{{ selectedCountry.name }}</h3>
                  <p class="text-gray-400 text-xs">{{ selectedCountry.description }}</p>
                </div>
              </div>
              <button @click="selectedCountry = null" class="text-gray-500 hover:text-gray-300 text-lg leading-none">×</button>
            </div>

            <!-- Status badges -->
            <div v-if="selectedCountry.currentlyHere" class="p-2 rounded-lg bg-green-500/15 border border-green-500/30 text-center">
              <p class="text-green-400 text-sm font-medium">Du bist hier</p>
            </div>
            <div v-else-if="selectedCountry.travelingHere" class="p-2 rounded-lg bg-blue-500/15 border border-blue-500/30 text-center">
              <p class="text-blue-300 text-sm">Unterwegs — Ankunft Monat {{ status?.arriveAtTurn }}</p>
            </div>

            <!-- Travel info -->
            <div class="space-y-2">
              <div class="flex justify-between text-sm">
                <span class="text-gray-400">Reisekosten</span>
                <span class="text-white font-mono">{{ formatCurrency(selectedCountry.travelCost) }}</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-gray-400">Reisedauer</span>
                <span class="text-white">{{ selectedCountry.travelMonths }} Monat{{ selectedCountry.travelMonths > 1 ? 'e' : '' }}</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-gray-400">Status</span>
                <span class="text-white">{{ selectedCountry.visited ? 'Besucht ✓' : 'Noch nicht besucht' }}</span>
              </div>
            </div>

            <!-- Collectibles -->
            <div v-if="collectiblesAt(selectedCountry.name).length" class="space-y-2">
              <p class="text-xs font-semibold text-gray-400 uppercase tracking-wider">
                Sammelgegenstände
                <span class="text-accent font-normal normal-case ml-1">
                  {{ ownedAt(selectedCountry.name) }}/{{ collectiblesAt(selectedCountry.name).length }} gesammelt
                </span>
              </p>
              <div
                v-for="item in collectiblesAt(selectedCountry.name)"
                :key="item.id"
                class="flex items-center justify-between text-xs py-1 border-b border-surface-700 last:border-0"
              >
                <span :class="item.alreadyOwned ? 'text-gray-500 line-through' : 'text-gray-200'">{{ item.name }}</span>
                <span :class="rarityClass(item.rarity)" class="text-xs font-mono ml-2 flex-shrink-0">{{ item.rarity }}</span>
              </div>
              <NuxtLink to="/sammlungen" class="text-xs text-accent/70 hover:text-accent">→ Zu den Sammlungen</NuxtLink>
            </div>

            <!-- Active events -->
            <div v-if="eventsForCountry(selectedCountry.name).length" class="space-y-2">
              <p class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Aktive Events</p>
              <div
                v-for="e in eventsForCountry(selectedCountry.name)"
                :key="e.id"
                class="p-2 rounded-lg bg-yellow-500/10 border border-yellow-500/20 text-xs text-yellow-300"
              >
                ⭐ {{ e.message }}
              </div>
            </div>

            <!-- Action button -->
            <button
              v-if="!selectedCountry.currentlyHere && !selectedCountry.travelingHere && !status?.traveling"
              @click="depart(selectedCountry.name)"
              :disabled="!!actionLoading"
              class="btn-primary w-full"
            >
              {{ actionLoading === selectedCountry.name ? 'Buche...' : 'Reise buchen' }}
            </button>
            <button
              v-else-if="selectedCountry.currentlyHere"
              @click="returnHome"
              :disabled="!!actionLoading"
              class="btn-secondary w-full"
            >
              Nach Hause reisen
            </button>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { geoNaturalEarth1, geoPath } from 'd3-geo'
import { feature } from 'topojson-client'
import type { Topology } from 'topojson-specification'

definePageMeta({ layout: 'default' })

const api = useApi()
const toast = useToastStore()
const gameStore = useGameStore()
const { formatCurrency } = useFormatting()

interface TravelStatus {
  currentCountry: string | null
  destinationCountry: string | null
  arriveAtTurn: number | null
  traveling: boolean
  visitedCountries: string[]
}
interface Country {
  id: number; name: string; travelCost: number; travelMonths: number
  emoji: string; description: string; visited: boolean; currentlyHere: boolean; travelingHere: boolean
}
interface ActiveEvent {
  id: number; type: string; country: string; expiresAtTurn: number
  collectibleId: number | null; message: string
}
interface CollectibleSummary {
  id: number; name: string; rarity: string
  countryRequired: string | null; alreadyOwned: boolean; canBuy: boolean
}

// ── Map pan/zoom ──────────────────────────────────────────────────────────────
const MAP_W = 960
const MAP_H = 440

// ISO 3166-1 numeric IDs for the 15 game countries
const GAME_ISO_SET = new Set([392, 380, 840, 756, 826, 156, 276, 250, 724, 36, 643, 76, 124, 356, 710])

// Geographic centroids [lng, lat] for country pin placement
const GAME_COUNTRY_CENTROIDS: Record<string, [number, number]> = {
  Japan:       [138.0, 36.2],
  Italien:     [12.6,  42.5],
  USA:         [-100.0, 40.0],
  Schweiz:     [8.2,  46.8],
  UK:          [-1.5,  54.0],
  China:       [104.0, 35.0],
  Deutschland: [10.5,  51.2],
  Frankreich:  [2.3,  46.5],
  Spanien:     [-3.7,  40.4],
  Australien:  [134.0, -25.0],
  Russland:    [60.0,  60.0],
  Brasilien:   [-52.0, -10.0],
  Kanada:      [-95.0, 57.0],
  Indien:      [78.9,  20.6],
  Südafrika:   [25.0, -29.0],
}

const projection = geoNaturalEarth1()
  .scale(153)
  .translate([MAP_W / 2, MAP_H / 2])

const pathGenerator = geoPath(projection)

interface CountryPath { id: number; d: string; isGame: boolean }
const countryPaths = ref<CountryPath[]>([])

function coords(name: string): { x: number; y: number } {
  const ll = GAME_COUNTRY_CENTROIDS[name]
  if (!ll) return { x: MAP_W / 2, y: MAP_H / 2 }
  const pt = projection(ll)
  return pt ? { x: pt[0], y: pt[1] } : { x: MAP_W / 2, y: MAP_H / 2 }
}

const mapEl = ref<HTMLElement | null>(null)
const panX = ref(0)
const panY = ref(0)
const zoom = ref(1.5)

let isPanning = false
let hasDragged = false
let lastMouse = { x: 0, y: 0 }

function startPan(e: MouseEvent) {
  isPanning = true
  hasDragged = false
  lastMouse = { x: e.clientX, y: e.clientY }
}
function doPan(e: MouseEvent) {
  if (!isPanning) return
  const dx = e.clientX - lastMouse.x
  const dy = e.clientY - lastMouse.y
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasDragged = true
  panX.value += dx
  panY.value += dy
  lastMouse = { x: e.clientX, y: e.clientY }
}
function stopPan() { isPanning = false }

function doZoom(e: WheelEvent) {
  const delta = e.deltaY > 0 ? 0.9 : 1.1
  const newZoom = Math.max(0.5, Math.min(4, zoom.value * delta))
  const rect = mapEl.value!.getBoundingClientRect()
  const mouseX = e.clientX - rect.left
  const mouseY = e.clientY - rect.top
  const ratio = newZoom / zoom.value
  panX.value = mouseX - ratio * (mouseX - panX.value)
  panY.value = mouseY - ratio * (mouseY - panY.value)
  zoom.value = newZoom
}

function resetView() {
  zoom.value = 1.5
  nextTick(() => {
    const rect = mapEl.value?.getBoundingClientRect()
    if (!rect) return
    panX.value = (rect.width - MAP_W * zoom.value) / 2
    panY.value = (rect.height - MAP_H * zoom.value) / 2
  })
}

// ── Data ───────────────────────────────────────────────────────────────────────
const status = ref<TravelStatus | null>(null)
const countries = ref<Country[]>([])
const activeEvents = ref<ActiveEvent[]>([])
const collectibleSummary = ref<CollectibleSummary[]>([])
const selectedCountry = ref<Country | null>(null)
const actionLoading = ref<string | false>(false)

const visitedCount = computed(() => (Array.isArray(countries.value) ? countries.value : []).filter(c => c.visited).length)

function collectiblesAt(countryName: string): CollectibleSummary[] {
  return (Array.isArray(collectibleSummary.value) ? collectibleSummary.value : []).filter(c => c.countryRequired === countryName)
}
function ownedAt(countryName: string): number {
  return collectiblesAt(countryName).filter(c => c.alreadyOwned).length
}

function rarityClass(rarity: string): string {
  switch (rarity?.toUpperCase()) {
    case 'LEGENDARY': return 'text-yellow-400'
    case 'EPIC':      return 'text-purple-400'
    case 'RARE':      return 'text-blue-400'
    case 'UNCOMMON':  return 'text-green-400'
    default:          return 'text-gray-500'
  }
}

const travelRouteFrom = computed(() => {
  if (!status.value?.traveling) return null
  const home = status.value.currentCountry ?? 'Deutschland'
  return COUNTRY_COORDS[home] ?? null
})
const travelRouteTo = computed(() => {
  if (!status.value?.traveling || !status.value.destinationCountry) return null
  return COUNTRY_COORDS[status.value.destinationCountry] ?? null
})

function eventsForCountry(countryName: string): ActiveEvent[] {
  return (Array.isArray(activeEvents.value) ? activeEvents.value : []).filter(e => e.country === countryName)
}

function selectCountry(c: Country) {
  if (hasDragged) return
  selectedCountry.value = selectedCountry.value?.id === c.id ? null : c
}

async function loadAll() {
  try {
    const [s, c, ev, items] = await Promise.all([
      api.get<TravelStatus>('/api/travel/status'),
      api.get<Country[]>('/api/travel/countries'),
      api.get<ActiveEvent[]>('/api/collectibles/events'),
      api.get<CollectibleSummary[]>('/api/collectibles'),
    ])
    status.value = s
    countries.value = Array.isArray(c) ? c : []
    activeEvents.value = Array.isArray(ev) ? ev : []
    collectibleSummary.value = Array.isArray(items) ? items : []
  } catch {
    toast.error('Daten konnten nicht geladen werden')
  }
}

async function depart(countryName: string) {
  actionLoading.value = countryName
  try {
    status.value = await api.post<TravelStatus>('/api/travel/depart', { countryName })
    const updatedCountries = await api.get<Country[]>('/api/travel/countries')
    countries.value = Array.isArray(updatedCountries) ? updatedCountries : []
    if (selectedCountry.value?.name === countryName) {
      selectedCountry.value = countries.value.find(c => c.name === countryName) ?? null
    }
    toast.success(`Flug nach ${countryName} gebucht!`)
    await gameStore.fetchCharacter()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Buchung fehlgeschlagen')
  } finally {
    actionLoading.value = false
  }
}

async function returnHome() {
  actionLoading.value = 'home'
  try {
    status.value = await api.post<TravelStatus>('/api/travel/return-home')
    const homeCountries = await api.get<Country[]>('/api/travel/countries')
    countries.value = Array.isArray(homeCountries) ? homeCountries : []
    selectedCountry.value = null
    toast.success('Zurück zuhause in Deutschland.')
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler')
  } finally {
    actionLoading.value = false
  }
}

function countryEmoji(name: string): string {
  const c = countries.value.find(c => c.name === name)
  if (c?.emoji) return c.emoji
  const map: Record<string, string> = {
    Japan: '🇯🇵', Italien: '🇮🇹', USA: '🇺🇸', Schweiz: '🇨🇭', UK: '🇬🇧', China: '🇨🇳',
    Deutschland: '🇩🇪', Frankreich: '🇫🇷', Spanien: '🇪🇸', Australien: '🇦🇺',
  }
  return map[name] ?? '🌍'
}

onMounted(async () => {
  // Load world TopoJSON for country shapes
  try {
    const topo = await fetch('/countries-110m.json').then(r => r.json())
    const fc = feature(topo as Topology, (topo as any).objects.countries) as any
    countryPaths.value = fc.features
      .map((f: any) => ({
        id: +f.id,
        d: pathGenerator(f) ?? '',
        isGame: GAME_ISO_SET.has(+f.id),
      }))
      .filter((p: CountryPath) => p.d !== '')
  } catch {
    // Map still works without shapes
  }
  await loadAll()
  resetView()
})
</script>
