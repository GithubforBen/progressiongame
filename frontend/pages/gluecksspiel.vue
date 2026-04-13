<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-bold text-white">Glücksspiel</h2>
      <span class="text-sm text-gray-400">
        Guthaben: <span class="text-white font-semibold">{{ formatCurrency(gameStore.character?.cash ?? 0) }}</span>
      </span>
    </div>

    <!-- Tab Navigation -->
    <div class="flex gap-1 bg-surface-900 rounded-lg p-1">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        @click="activeTab = tab.id"
        class="flex-1 py-2 px-4 rounded-md text-sm font-medium transition-colors"
        :class="activeTab === tab.id
          ? 'bg-accent text-white'
          : 'text-gray-400 hover:text-white'"
      >
        {{ tab.icon }} {{ tab.label }}
      </button>
    </div>

    <!-- ===== SLOTS ===== -->
    <div v-if="activeTab === 'slots'" class="space-y-4">
      <div class="card">
        <h3 class="text-base font-semibold text-white mb-1">Spielautomat</h3>
        <p class="text-xs text-gray-400 mb-4">House Edge ~15%. Jackpot zahlt 50×, großer Gewinn 10×, Gewinn 5×.</p>

        <!-- Reels -->
        <div class="flex justify-center gap-3 mb-6">
          <div
            v-for="(reel, i) in displayReels"
            :key="i"
            class="w-20 h-20 flex items-center justify-center rounded-xl text-4xl border-2 transition-all duration-300"
            :class="spinning
              ? 'border-accent/50 bg-accent/10 animate-pulse'
              : slotResult
                ? resultBorderClass(slotResult.outcome)
                : 'border-surface-600 bg-surface-700'"
          >
            {{ reel }}
          </div>
        </div>

        <!-- Bet Input -->
        <div class="flex gap-3 items-center mb-4">
          <div class="flex-1">
            <label class="text-xs text-gray-400 mb-1 block">Einsatz (€)</label>
            <input
              v-model.number="slotBet"
              type="number"
              min="1"
              max="10000"
              class="input w-full"
              :disabled="spinning"
            />
          </div>
          <div class="flex gap-2 items-end">
            <button
              v-for="quick in [10, 50, 100, 500]"
              :key="quick"
              @click="slotBet = quick"
              class="btn-secondary text-xs px-2 py-1"
              :disabled="spinning"
            >
              {{ quick }}
            </button>
          </div>
        </div>

        <button
          @click="playSlots"
          :disabled="spinning || !slotBet || slotBet < 1"
          class="btn-primary w-full text-base py-3"
        >
          {{ spinning ? 'Dreht...' : 'Drehen!' }}
        </button>

        <!-- Result -->
        <div v-if="slotResult && !spinning" class="mt-4 p-4 rounded-lg border" :class="resultBgClass(slotResult.outcome)">
          <div class="flex items-center justify-between">
            <span class="font-semibold text-base" :class="resultTextClass(slotResult.outcome)">
              {{ outcomeLabel(slotResult.outcome) }}
            </span>
            <span class="font-bold text-lg" :class="slotResult.netChange >= 0 ? 'text-green-400' : 'text-red-400'">
              {{ slotResult.netChange >= 0 ? '+' : '' }}{{ formatCurrency(slotResult.netChange) }}
            </span>
          </div>
          <p v-if="slotResult.payout > 0" class="text-sm text-gray-300 mt-1">
            Gewinn: {{ formatCurrency(slotResult.payout) }}
          </p>
        </div>
      </div>

      <!-- Payout Table -->
      <div class="card">
        <h4 class="text-sm font-semibold text-gray-300 mb-3">Gewinnplan</h4>
        <div class="space-y-1.5 text-sm">
          <div v-for="row in payoutTable" :key="row.label" class="flex justify-between">
            <span class="text-gray-400">{{ row.symbols }} {{ row.label }}</span>
            <span class="text-white font-medium">{{ row.multiplier }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== BLACKJACK ===== -->
    <div v-if="activeTab === 'blackjack'" class="space-y-4">
      <div class="card">
        <h3 class="text-base font-semibold text-white mb-1">Blackjack</h3>
        <p class="text-xs text-gray-400 mb-4">Dealer zieht bis 17. Blackjack zahlt 2,5×. Ziel: näher an 21 als der Dealer.</p>

        <!-- Active Game -->
        <div v-if="bjGame">
          <!-- Dealer -->
          <div class="mb-4">
            <p class="text-xs text-gray-400 mb-2">
              Dealer
              <span class="ml-1 text-white font-semibold">
                ({{ bjGame.status === 'IN_PROGRESS' ? bjGame.dealerVisible : calcHandTotal(bjGame.dealerCards) }})
              </span>
            </p>
            <div class="flex gap-2 flex-wrap">
              <div v-for="(card, i) in bjGame.dealerCards" :key="i">
                <CardDisplay :card="card" />
              </div>
            </div>
          </div>

          <!-- Player -->
          <div class="mb-4">
            <p class="text-xs text-gray-400 mb-2">
              Du
              <span class="ml-1 text-white font-semibold">({{ bjGame.playerTotal }})</span>
              <span v-if="bjGame.playerTotal === 21 && bjGame.playerCards.length === 2" class="ml-2 text-yellow-400 font-bold text-xs">BLACKJACK!</span>
              <span v-else-if="bjGame.playerTotal > 21" class="ml-2 text-red-400 font-bold text-xs">BUST!</span>
            </p>
            <div class="flex gap-2 flex-wrap">
              <div v-for="(card, i) in bjGame.playerCards" :key="i">
                <CardDisplay :card="card" />
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div v-if="bjGame.status === 'IN_PROGRESS'" class="flex gap-3">
            <button @click="bjHit" :disabled="bjLoading" class="btn-primary flex-1">Karte ziehen (Hit)</button>
            <button @click="bjStand" :disabled="bjLoading" class="btn-secondary flex-1">Stehen (Stand)</button>
          </div>

          <!-- Result -->
          <div v-else class="mt-4 p-4 rounded-lg border" :class="resultBgClass(bjGame.status)">
            <div class="flex items-center justify-between">
              <span class="font-semibold text-base" :class="resultTextClass(bjGame.status)">
                {{ bjResultLabel(bjGame.status) }}
              </span>
              <span class="font-bold text-lg" :class="bjGame.netChange >= 0 ? 'text-green-400' : 'text-red-400'">
                {{ bjGame.netChange >= 0 ? '+' : '' }}{{ formatCurrency(bjGame.netChange) }}
              </span>
            </div>
            <button @click="bjGame = null" class="btn-secondary mt-3 text-sm w-full">Neues Spiel</button>
          </div>
        </div>

        <!-- Start New Game -->
        <div v-else>
          <div class="flex gap-3 items-center mb-4">
            <div class="flex-1">
              <label class="text-xs text-gray-400 mb-1 block">Einsatz (€)</label>
              <input v-model.number="bjBet" type="number" min="1" max="10000" class="input w-full" />
            </div>
            <div class="flex gap-2 items-end">
              <button v-for="quick in [10, 50, 100]" :key="quick" @click="bjBet = quick" class="btn-secondary text-xs px-2 py-1">
                {{ quick }}
              </button>
            </div>
          </div>
          <button @click="bjStart" :disabled="bjLoading || !bjBet || bjBet < 1" class="btn-primary w-full py-3 text-base">
            Spiel starten
          </button>
        </div>
      </div>
    </div>

    <!-- ===== POKER ===== -->
    <div v-if="activeTab === 'poker'" class="space-y-4">
      <div class="card">
        <h3 class="text-base font-semibold text-white mb-1">Poker (5-Karten)</h3>
        <p class="text-xs text-gray-400 mb-4">Vereinfachtes 5-Karten-Poker gegen eine KI. Gewinn = 1,95× Einsatz (5% Rake).</p>

        <!-- Active Result -->
        <div v-if="pokerResult">
          <div class="grid grid-cols-2 gap-4 mb-4">
            <div>
              <p class="text-xs text-gray-400 mb-2">
                Deine Hand
                <span class="ml-1 badge" :class="handBadgeClass(pokerResult.result)">
                  {{ pokerResult.playerHandName }}
                </span>
              </p>
              <div class="flex gap-1.5 flex-wrap">
                <CardDisplay v-for="(card, i) in pokerResult.playerCards" :key="i" :card="card" />
              </div>
            </div>
            <div>
              <p class="text-xs text-gray-400 mb-2">
                KI-Hand
                <span class="ml-1 badge bg-white/10 text-gray-300">{{ pokerResult.aiHandName }}</span>
              </p>
              <div class="flex gap-1.5 flex-wrap">
                <CardDisplay v-for="(card, i) in pokerResult.aiCards" :key="i" :card="card" />
              </div>
            </div>
          </div>

          <div class="p-4 rounded-lg border" :class="resultBgClass(pokerResult.result)">
            <div class="flex items-center justify-between">
              <span class="font-semibold" :class="resultTextClass(pokerResult.result)">
                {{ pokerResultLabel(pokerResult.result) }}
              </span>
              <span class="font-bold text-lg" :class="pokerResult.netChange >= 0 ? 'text-green-400' : 'text-red-400'">
                {{ pokerResult.netChange >= 0 ? '+' : '' }}{{ formatCurrency(pokerResult.netChange) }}
              </span>
            </div>
          </div>

          <button @click="pokerResult = null" class="btn-secondary mt-3 text-sm w-full">Nochmal spielen</button>
        </div>

        <!-- Bet + Play -->
        <div v-else>
          <div class="flex gap-3 items-center mb-4">
            <div class="flex-1">
              <label class="text-xs text-gray-400 mb-1 block">Einsatz (€)</label>
              <input v-model.number="pokerBet" type="number" min="1" max="10000" class="input w-full" />
            </div>
            <div class="flex gap-2 items-end">
              <button v-for="quick in [10, 50, 100]" :key="quick" @click="pokerBet = quick" class="btn-secondary text-xs px-2 py-1">
                {{ quick }}
              </button>
            </div>
          </div>
          <button @click="playPoker" :disabled="pokerLoading || !pokerBet || pokerBet < 1" class="btn-primary w-full py-3 text-base">
            {{ pokerLoading ? 'Karten werden gemischt...' : 'Karten ausgeben' }}
          </button>
        </div>
      </div>

      <!-- Hand Rankings -->
      <div class="card">
        <h4 class="text-sm font-semibold text-gray-300 mb-3">Handreihenfolge (schwächste → stärkste)</h4>
        <div class="flex flex-wrap gap-2 text-xs">
          <span v-for="hand in handRankings" :key="hand" class="px-2 py-1 rounded bg-white/5 text-gray-300">{{ hand }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useApi } from '~/composables/useApi'
import { useFormatting } from '~/composables/useFormatting'
import { useGameStore } from '~/stores/game'
import { useToastStore } from '~/stores/toast'

definePageMeta({ middleware: 'auth' })

const api = useApi()
const { formatCurrency } = useFormatting()
const gameStore = useGameStore()
const toast = useToastStore()

// ── Tabs ──────────────────────────────────────────────────────────────────
const tabs = [
  { id: 'slots', label: 'Slots', icon: '🎰' },
  { id: 'blackjack', label: 'Blackjack', icon: '🃏' },
  { id: 'poker', label: 'Poker', icon: '♠' },
]
const activeTab = ref<'slots' | 'blackjack' | 'poker'>('slots')

// ── Types ─────────────────────────────────────────────────────────────────
interface SlotResult {
  reels: string[]
  outcome: string
  betAmount: number
  payout: number
  netChange: number
}

interface BjState {
  sessionId: number
  playerCards: string[]
  dealerCards: string[]
  playerTotal: number
  dealerVisible: number
  status: string
  betAmount: number
  payout: number
  netChange: number
}

interface PokerResult {
  playerCards: string[]
  aiCards: string[]
  playerHandName: string
  aiHandName: string
  result: string
  betAmount: number
  payout: number
  netChange: number
}

// ── Slots ─────────────────────────────────────────────────────────────────
const slotBet = ref(10)
const spinning = ref(false)
const slotResult = ref<SlotResult | null>(null)

const slotSymbolMap: Record<string, string> = {
  SEVEN: '7️⃣', BELL: '🔔', BAR: '🎰', CHERRY: '🍒', LEMON: '🍋', BLANK: '⬛',
}

const displayReels = computed<string[]>(() => {
  if (slotResult.value && !spinning.value) {
    return slotResult.value.reels.map(r => slotSymbolMap[r] ?? r)
  }
  return ['❓', '❓', '❓']
})

async function playSlots() {
  spinning.value = true
  slotResult.value = null
  try {
    const result = await api.post<SlotResult>('/api/gambling/slots', { bet: slotBet.value })
    slotResult.value = result
    await gameStore.fetchCharacter()
    if (result.netChange >= 0) toast.success(outcomeLabel(result.outcome) + ' ' + formatCurrency(result.netChange))
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler beim Spielen')
  } finally {
    spinning.value = false
  }
}

// ── Blackjack ─────────────────────────────────────────────────────────────
const bjBet = ref(10)
const bjLoading = ref(false)
const bjGame = ref<BjState | null>(null)

async function bjStart() {
  bjLoading.value = true
  try {
    bjGame.value = await api.post<BjState>('/api/gambling/blackjack/start', { bet: bjBet.value })
    await gameStore.fetchCharacter()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler beim Starten')
  } finally {
    bjLoading.value = false
  }
}

async function bjHit() {
  if (!bjGame.value) return
  bjLoading.value = true
  try {
    bjGame.value = await api.post<BjState>(`/api/gambling/blackjack/${bjGame.value.sessionId}/hit`, {})
    await gameStore.fetchCharacter()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler')
  } finally {
    bjLoading.value = false
  }
}

async function bjStand() {
  if (!bjGame.value) return
  bjLoading.value = true
  try {
    bjGame.value = await api.post<BjState>(`/api/gambling/blackjack/${bjGame.value.sessionId}/stand`, {})
    await gameStore.fetchCharacter()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler')
  } finally {
    bjLoading.value = false
  }
}

function calcHandTotal(cards: string[]): number {
  let total = 0
  let aces = 0
  for (const card of cards) {
    if (card === '??') continue
    const rank = card[0]
    let v = parseInt(rank) || 0
    if (['T', 'J', 'Q', 'K'].includes(rank)) v = 10
    else if (rank === 'A') { v = 11; aces++ }
    total += v
  }
  while (total > 21 && aces > 0) { total -= 10; aces-- }
  return total
}

// ── Poker ─────────────────────────────────────────────────────────────────
const pokerBet = ref(10)
const pokerLoading = ref(false)
const pokerResult = ref<PokerResult | null>(null)

async function playPoker() {
  pokerLoading.value = true
  try {
    pokerResult.value = await api.post<PokerResult>('/api/gambling/poker', { bet: pokerBet.value })
    await gameStore.fetchCharacter()
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler beim Spielen')
  } finally {
    pokerLoading.value = false
  }
}

// ── Helpers / Labels ──────────────────────────────────────────────────────
const payoutTable = [
  { symbols: '7️⃣7️⃣7️⃣', label: 'JACKPOT', multiplier: '50×' },
  { symbols: '🔔🔔🔔', label: 'Großer Gewinn', multiplier: '10×' },
  { symbols: '🎰🎰🎰 / 🍒🍒🍒 / 🍋🍋🍋', label: 'Gewinn', multiplier: '5×' },
  { symbols: 'Zwei gleiche', label: 'Kleiner Gewinn', multiplier: '1,5×' },
  { symbols: 'Keine Übereinstimmung', label: 'Verlust', multiplier: '0×' },
]

const handRankings = [
  'Highcard', 'Ein Paar', 'Zwei Paare', 'Drilling', 'Straight',
  'Flush', 'Full House', 'Vierling', 'Straight Flush', 'Royal Flush',
]

function outcomeLabel(outcome: string): string {
  const map: Record<string, string> = {
    JACKPOT: 'JACKPOT!', BIG_WIN: 'Großer Gewinn!', WIN: 'Gewinn!',
    SMALL_WIN: 'Kleiner Gewinn!', LOSS: 'Verloren',
  }
  return map[outcome] ?? outcome
}

function bjResultLabel(status: string): string {
  return { WON: 'Gewonnen!', LOST: 'Verloren', PUSH: 'Unentschieden' }[status] ?? status
}

function pokerResultLabel(result: string): string {
  return { WON: 'Gewonnen!', LOST: 'Verloren', PUSH: 'Unentschieden (Split Pot)' }[result] ?? result
}

function resultBorderClass(key: string): string {
  if (['JACKPOT', 'BIG_WIN', 'WIN', 'WON'].includes(key)) return 'border-green-500/50'
  if (['SMALL_WIN', 'PUSH'].includes(key)) return 'border-yellow-500/50'
  return 'border-red-500/30'
}

function resultBgClass(key: string): string {
  if (['JACKPOT', 'BIG_WIN', 'WIN', 'WON'].includes(key)) return 'bg-green-500/10 border-green-500/30'
  if (['SMALL_WIN', 'PUSH'].includes(key)) return 'bg-yellow-500/10 border-yellow-500/30'
  return 'bg-red-500/10 border-red-500/30'
}

function resultTextClass(key: string): string {
  if (['JACKPOT', 'BIG_WIN', 'WIN', 'WON'].includes(key)) return 'text-green-300'
  if (['SMALL_WIN', 'PUSH'].includes(key)) return 'text-yellow-300'
  return 'text-red-400'
}

function handBadgeClass(result: string): string {
  if (result === 'WON') return 'bg-green-500/20 text-green-300'
  if (result === 'PUSH') return 'bg-yellow-500/20 text-yellow-300'
  return 'bg-red-500/20 text-red-400'
}
</script>
