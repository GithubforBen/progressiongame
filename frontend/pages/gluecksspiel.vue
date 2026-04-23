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
        class="flex-1 py-2 px-3 rounded-md text-sm font-medium transition-colors"
        :class="activeTab === tab.id ? 'bg-accent text-white' : 'text-gray-400 hover:text-white'"
        @click="activeTab = tab.id"
      >
        {{ tab.icon }} {{ tab.label }}
      </button>
    </div>

    <!-- ===== SLOTS ===== -->
    <div v-if="activeTab === 'slots'" class="space-y-4">
      <div class="card">
        <h3 class="text-base font-semibold text-white mb-1">Spielautomat</h3>
        <p class="text-xs text-gray-400 mb-4">House Edge ~15%. Jackpot zahlt 50×, großer Gewinn 10×, Gewinn 5×.</p>

        <div class="flex justify-center gap-3 mb-6">
          <div
            v-for="(reel, i) in displayReels"
            :key="i"
            class="w-20 h-20 flex items-center justify-center rounded-xl text-4xl border-2 transition-all duration-300"
            :class="spinning
              ? 'border-accent/50 bg-accent/10 animate-pulse'
              : slotResult ? resultBorderClass(slotResult.outcome) : 'border-surface-600 bg-surface-700'"
          >
            {{ reel }}
          </div>
        </div>

        <div class="flex gap-3 items-center mb-4">
          <div class="flex-1">
            <label class="text-xs text-gray-400 mb-1 block">Einsatz (€)</label>
            <input v-model.number="slotBet" type="number" min="1" max="10000" class="input w-full" :disabled="spinning" />
          </div>
          <div class="flex gap-2 items-end">
            <button v-for="quick in [10, 50, 100, 500]" :key="quick" class="btn-secondary text-xs px-2 py-1" :disabled="spinning" @click="slotBet = quick">{{ quick }}</button>
          </div>
        </div>

        <button class="btn-primary w-full text-base py-3" :disabled="spinning || !slotBet || slotBet < 1" @click="playSlots">
          {{ spinning ? 'Dreht...' : 'Drehen!' }}
        </button>

        <div v-if="slotResult && !spinning" class="mt-4 p-4 rounded-lg border" :class="resultBgClass(slotResult.outcome)">
          <div class="flex items-center justify-between">
            <span class="font-semibold text-base" :class="resultTextClass(slotResult.outcome)">{{ outcomeLabel(slotResult.outcome) }}</span>
            <span class="font-bold text-lg" :class="slotResult.netChange >= 0 ? 'text-green-400' : 'text-red-400'">
              {{ slotResult.netChange >= 0 ? '+' : '' }}{{ formatCurrency(slotResult.netChange) }}
            </span>
          </div>
          <p v-if="slotResult.payout > 0" class="text-sm text-gray-300 mt-1">Gewinn: {{ formatCurrency(slotResult.payout) }}</p>
        </div>
      </div>

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

        <div v-if="bjGame">
          <div class="mb-4">
            <p class="text-xs text-gray-400 mb-2">Dealer <span class="ml-1 text-white font-semibold">({{ bjGame.status === 'IN_PROGRESS' ? bjGame.dealerVisible : calcHandTotal(bjGame.dealerCards) }})</span></p>
            <div class="flex gap-2 flex-wrap">
              <CardDisplay v-for="(card, i) in bjGame.dealerCards" :key="i" :card="card" />
            </div>
          </div>
          <div class="mb-4">
            <p class="text-xs text-gray-400 mb-2">Du <span class="ml-1 text-white font-semibold">({{ bjGame.playerTotal }})</span>
              <span v-if="bjGame.playerTotal === 21 && bjGame.playerCards.length === 2" class="ml-2 text-yellow-400 font-bold text-xs">BLACKJACK!</span>
              <span v-else-if="bjGame.playerTotal > 21" class="ml-2 text-red-400 font-bold text-xs">BUST!</span>
            </p>
            <div class="flex gap-2 flex-wrap">
              <CardDisplay v-for="(card, i) in bjGame.playerCards" :key="i" :card="card" />
            </div>
          </div>
          <div v-if="bjGame.status === 'IN_PROGRESS'" class="flex gap-3">
            <button class="btn-primary flex-1" :disabled="bjLoading" @click="bjHit">Karte ziehen (Hit)</button>
            <button class="btn-secondary flex-1" :disabled="bjLoading" @click="bjStand">Stehen (Stand)</button>
          </div>
          <div v-else class="mt-4 p-4 rounded-lg border" :class="resultBgClass(bjGame.status)">
            <div class="flex items-center justify-between">
              <span class="font-semibold text-base" :class="resultTextClass(bjGame.status)">{{ bjResultLabel(bjGame.status) }}</span>
              <span class="font-bold text-lg" :class="bjGame.netChange >= 0 ? 'text-green-400' : 'text-red-400'">
                {{ bjGame.netChange >= 0 ? '+' : '' }}{{ formatCurrency(bjGame.netChange) }}
              </span>
            </div>
            <button class="btn-secondary mt-3 text-sm w-full" @click="bjGame = null">Neues Spiel</button>
          </div>
        </div>
        <div v-else>
          <div class="flex gap-3 items-center mb-4">
            <div class="flex-1">
              <label class="text-xs text-gray-400 mb-1 block">Einsatz (€)</label>
              <input v-model.number="bjBet" type="number" min="1" max="10000" class="input w-full" />
            </div>
            <div class="flex gap-2 items-end">
              <button v-for="quick in [10, 50, 100]" :key="quick" class="btn-secondary text-xs px-2 py-1" @click="bjBet = quick">{{ quick }}</button>
            </div>
          </div>
          <button class="btn-primary w-full py-3 text-base" :disabled="bjLoading || !bjBet || bjBet < 1" @click="bjStart">Spiel starten</button>
        </div>
      </div>
    </div>

    <!-- ===== POKER ===== -->
    <div v-if="activeTab === 'poker'" class="space-y-4">
      <div class="card">
        <h3 class="text-base font-semibold text-white mb-1">Poker (5-Karten)</h3>
        <p class="text-xs text-gray-400 mb-4">Vereinfachtes 5-Karten-Poker gegen eine KI. Gewinn = 1,95× Einsatz (5% Rake).</p>

        <div v-if="pokerResult">
          <div class="grid grid-cols-2 gap-4 mb-4">
            <div>
              <p class="text-xs text-gray-400 mb-2">Deine Hand <span class="ml-1 badge" :class="handBadgeClass(pokerResult.result)">{{ pokerResult.playerHandName }}</span></p>
              <div class="flex gap-1.5 flex-wrap">
                <CardDisplay v-for="(card, i) in pokerResult.playerCards" :key="i" :card="card" />
              </div>
            </div>
            <div>
              <p class="text-xs text-gray-400 mb-2">KI-Hand <span class="ml-1 badge bg-white/10 text-gray-300">{{ pokerResult.aiHandName }}</span></p>
              <div class="flex gap-1.5 flex-wrap">
                <CardDisplay v-for="(card, i) in pokerResult.aiCards" :key="i" :card="card" />
              </div>
            </div>
          </div>
          <div class="p-4 rounded-lg border" :class="resultBgClass(pokerResult.result)">
            <div class="flex items-center justify-between">
              <span class="font-semibold" :class="resultTextClass(pokerResult.result)">{{ pokerResultLabel(pokerResult.result) }}</span>
              <span class="font-bold text-lg" :class="pokerResult.netChange >= 0 ? 'text-green-400' : 'text-red-400'">
                {{ pokerResult.netChange >= 0 ? '+' : '' }}{{ formatCurrency(pokerResult.netChange) }}
              </span>
            </div>
          </div>
          <button class="btn-secondary mt-3 text-sm w-full" @click="pokerResult = null">Nochmal spielen</button>
        </div>
        <div v-else>
          <div class="flex gap-3 items-center mb-4">
            <div class="flex-1">
              <label class="text-xs text-gray-400 mb-1 block">Einsatz (€)</label>
              <input v-model.number="pokerBet" type="number" min="1" max="10000" class="input w-full" />
            </div>
            <div class="flex gap-2 items-end">
              <button v-for="quick in [10, 50, 100]" :key="quick" class="btn-secondary text-xs px-2 py-1" @click="pokerBet = quick">{{ quick }}</button>
            </div>
          </div>
          <button class="btn-primary w-full py-3 text-base" :disabled="pokerLoading || !pokerBet || pokerBet < 1" @click="playPoker">
            {{ pokerLoading ? 'Karten werden gemischt...' : 'Karten ausgeben' }}
          </button>
        </div>
      </div>

      <div class="card">
        <h4 class="text-sm font-semibold text-gray-300 mb-3">Handreihenfolge (schwächste → stärkste)</h4>
        <div class="flex flex-wrap gap-2 text-xs">
          <span v-for="hand in handRankings" :key="hand" class="px-2 py-1 rounded bg-white/5 text-gray-300">{{ hand }}</span>
        </div>
      </div>
    </div>

    <!-- ===== TEXAS HOLD'EM ===== -->
    <div v-if="activeTab === 'texas'" class="space-y-4">

      <!-- Active game -->
      <div v-if="thGame" class="space-y-3">

        <!-- Table header -->
        <div class="card flex items-center justify-between py-3">
          <div>
            <p class="text-xs text-gray-500 uppercase tracking-wide">Pot</p>
            <p class="text-2xl font-bold text-white font-mono leading-tight">{{ formatCurrency(thGame.pot) }}</p>
          </div>
          <div class="flex items-center gap-3">
            <div class="text-right">
              <p class="text-xs text-gray-500">Dein Einsatz</p>
              <p class="text-sm font-semibold font-mono text-gray-300">{{ formatCurrency(thGame.playerStake) }}</p>
            </div>
            <span
              class="px-3 py-1 rounded-full text-xs font-bold uppercase tracking-widest border"
              :class="streetBadgeClass(thGame.street)"
            >{{ streetLabel(thGame.street) }}</span>
          </div>
        </div>

        <!-- Awaiting re-action banner -->
        <div v-if="thGame.awaitingPlayerResponse" class="rounded-lg border border-yellow-500/30 bg-yellow-500/10 px-4 py-2.5 text-sm text-yellow-300 font-medium">
          Ein Bot hat erhöht — deine Reaktion:
        </div>

        <!-- Poker table area -->
        <div class="card space-y-5">

          <!-- Bots row -->
          <div class="grid grid-cols-4 gap-2">
            <div
              v-for="bot in thGame.bots"
              :key="bot.index"
              class="rounded-xl p-2.5 text-center transition-all relative"
              :class="[
                bot.folded ? 'bg-surface-700/40 opacity-40' :
                bot.winner ? 'bg-red-500/10 border border-red-500/30' :
                activeActor === 'BOT_' + bot.index ? 'bg-accent/10 border border-accent/40 shadow-[0_0_12px_rgba(212,50,31,0.25)]' :
                'bg-surface-700'
              ]"
            >
              <!-- Pulsing ring when active -->
              <div v-if="activeActor === 'BOT_' + bot.index" class="absolute inset-0 rounded-xl border-2 border-accent/60 animate-pulse pointer-events-none" />

              <!-- Action bubble -->
              <div
                v-if="actorActionMap['BOT_' + bot.index]"
                class="absolute -top-5 left-1/2 -translate-x-1/2 px-2 py-0.5 rounded-full text-xs font-bold whitespace-nowrap z-10"
                :class="actorActionMap['BOT_' + bot.index]?.startsWith('Fold') ? 'bg-red-500/20 text-red-300' : actorActionMap['BOT_' + bot.index]?.startsWith('Raise') ? 'bg-accent/20 text-accent' : 'bg-white/10 text-gray-300'"
              >{{ actorActionMap['BOT_' + bot.index] }}</div>

              <p class="text-xs font-medium mb-1.5" :class="bot.folded ? 'text-gray-600' : bot.winner ? 'text-red-400' : activeActor === 'BOT_' + bot.index ? 'text-accent' : 'text-gray-400'">
                {{ bot.personality || ('Bot ' + (bot.index + 1)) }}
              </p>
              <div class="flex gap-1 justify-center">
                <CardDisplay v-for="(card, j) in bot.cards" :key="j" :card="card" />
              </div>
              <p v-if="bot.folded" class="text-xs text-gray-600 mt-1.5 uppercase tracking-wide">Fold</p>
              <p v-else-if="bot.handName" class="text-xs mt-1.5 font-semibold" :class="bot.winner ? 'text-red-400' : 'text-gray-400'">
                {{ bot.handName }}<span v-if="bot.winner"> ✗</span>
              </p>
              <p v-else class="text-xs text-gray-600 mt-1.5">im Spiel</p>
            </div>
          </div>

          <!-- Divider -->
          <div class="relative flex items-center">
            <div class="flex-1 border-t border-surface-600" />
            <span class="mx-3 text-xs text-gray-600 uppercase tracking-widest">Mittenhand</span>
            <div class="flex-1 border-t border-surface-600" />
          </div>

          <!-- Community cards -->
          <div class="flex gap-2 justify-center">
            <CardDisplay v-for="(card, i) in thGame.communityCards" :key="i" :card="card" />
            <div
              v-for="i in (5 - thGame.communityCards.length)"
              :key="'empty-' + i"
              class="w-12 h-16 rounded-lg border border-dashed border-surface-600/50 opacity-30"
            />
          </div>

          <!-- Divider -->
          <div class="relative flex items-center">
            <div class="flex-1 border-t border-surface-600" />
            <span class="mx-3 text-xs text-gray-600 uppercase tracking-widest">Deine Karten</span>
            <div class="flex-1 border-t border-surface-600" />
          </div>

          <!-- Player cards -->
          <div
            class="flex items-center justify-center gap-4 py-2 rounded-xl transition-all"
            :class="activeActor === 'PLAYER' ? 'bg-accent/5 border border-accent/20' : ''"
          >
            <div class="flex gap-2 relative">
              <div v-if="activeActor === 'PLAYER'" class="absolute -inset-1.5 rounded-xl border-2 border-accent/50 animate-pulse pointer-events-none" />
              <div v-if="actorActionMap['PLAYER']" class="absolute -top-6 left-1/2 -translate-x-1/2 px-2 py-0.5 rounded-full text-xs font-bold whitespace-nowrap z-10" :class="actorActionMap['PLAYER']?.startsWith('Fold') ? 'bg-red-500/20 text-red-300' : actorActionMap['PLAYER']?.startsWith('Raise') ? 'bg-accent/20 text-accent' : 'bg-white/10 text-gray-300'">{{ actorActionMap['PLAYER'] }}</div>
              <CardDisplay v-for="(card, i) in thGame.playerCards" :key="i" :card="card" />
            </div>
          </div>
        </div>

        <!-- Hand analysis panel -->
        <div v-if="thGame.playerCurrentHandName || thGame.playerDraws?.length" class="card space-y-2.5">
          <div v-if="thGame.playerCurrentHandName" class="flex items-center gap-2">
            <span class="text-xs text-gray-500 uppercase tracking-wide">Deine Hand:</span>
            <span class="text-sm font-bold" :class="thGame.status === 'WON' ? 'text-green-400' : 'text-white'">{{ thGame.playerCurrentHandName }}</span>
          </div>
          <div v-if="thGame.playerDraws?.length">
            <p class="text-xs text-gray-500 uppercase tracking-wide mb-2">Mögliche Draws</p>
            <div class="space-y-1.5">
              <div v-for="draw in thGame.playerDraws" :key="draw.type" class="flex items-center justify-between">
                <span class="text-sm text-gray-300">{{ draw.description }}</span>
                <div class="flex items-center gap-3">
                  <span class="text-xs text-gray-500">{{ draw.outs }} Outs</span>
                  <span class="text-sm font-bold tabular-nums" :class="draw.probability >= 30 ? 'text-green-400' : draw.probability >= 15 ? 'text-yellow-300' : 'text-gray-400'">{{ draw.probability }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Result banner -->
        <div v-if="thGame.status !== 'IN_PROGRESS'" class="p-4 rounded-xl border" :class="resultBgClass(thGame.status)">
          <div class="flex items-center justify-between">
            <span class="font-bold text-base" :class="resultTextClass(thGame.status)">
              {{ thResultLabel(thGame.status) }}
            </span>
            <span class="font-bold text-xl font-mono" :class="(thGame.netChange ?? 0) >= 0 ? 'text-green-400' : 'text-red-400'">
              {{ (thGame.netChange ?? 0) >= 0 ? '+' : '' }}{{ formatCurrency(thGame.netChange ?? 0) }}
            </span>
          </div>
          <button class="btn-secondary mt-3 text-sm w-full" @click="thGame = null; actorActionMap = {}">Neues Spiel</button>
        </div>

        <!-- Action buttons -->
        <div v-else-if="!thLoading" class="space-y-3">
          <!-- Raise amount row -->
          <div class="flex gap-2 items-center">
            <label class="text-xs text-gray-500 whitespace-nowrap">Erhöhen um:</label>
            <input
              v-model.number="thRaiseAmount"
              type="number"
              :min="thGame.initialBet"
              :max="gameStore.character?.cash ?? 0"
              class="input flex-1 text-sm py-1.5"
            />
            <button
              v-for="mult in [2, 5, 10]"
              :key="mult"
              class="btn-secondary text-xs px-2 py-1.5 whitespace-nowrap"
              @click="thRaiseAmount = Math.floor(thGame.initialBet * mult)"
            >{{ mult }}×</button>
          </div>
          <!-- Action buttons row -->
          <div class="grid grid-cols-4 gap-2">
            <button
              class="btn-secondary py-3 text-sm font-semibold text-red-400 border border-red-500/20 hover:bg-red-500/10"
              @click="thAct('FOLD')"
            >Folden</button>
            <button
              class="btn-secondary py-3 text-sm font-semibold"
              @click="thAct('CALL')"
            >
              <span v-if="thGame.toCall > 0">Callen<br><span class="text-xs font-normal opacity-70">{{ formatCurrency(thGame.toCall) }}</span></span>
              <span v-else>Checken</span>
            </button>
            <button
              class="btn-primary py-3 text-sm font-semibold"
              :disabled="thRaiseAmount < thGame.initialBet"
              @click="thAct('RAISE', thRaiseAmount)"
            >Erhöhen<br><span class="text-xs font-normal opacity-80">+{{ formatCurrency(thRaiseAmount) }}</span></button>
            <button
              class="py-3 text-sm font-bold rounded-lg border transition-colors"
              style="background: rgba(234,179,8,0.12); border-color: rgba(234,179,8,0.35); color: #fbbf24;"
              @click="thAllIn"
            >All In<br><span class="text-xs font-normal opacity-80">{{ formatCurrency(gameStore.character?.cash ?? 0) }}</span></button>
          </div>
        </div>
        <div v-else class="flex items-center justify-center py-5 gap-2 text-sm text-gray-500">
          <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/></svg>
          Bots überlegen...
        </div>
      </div>

      <!-- Start screen -->
      <div v-else class="space-y-4">
        <div class="card">
          <h3 class="text-base font-semibold text-white mb-1">Texas Hold'em</h3>
          <p class="text-xs text-gray-400 mb-4">Jeder Spieler bekommt 2 Karten. 5 Gemeinschaftskarten kommen schrittweise auf den Tisch. Erhöhe den Einsatz oder folde — gegen 4 Bots. Gewinnt das beste 5-Karten-Blatt.</p>

          <div class="grid grid-cols-3 gap-3 mb-4 text-center">
            <div class="rounded-lg bg-surface-700 p-3">
              <p class="text-lg font-bold text-white">2</p>
              <p class="text-xs text-gray-500">Karten pro Spieler</p>
            </div>
            <div class="rounded-lg bg-surface-700 p-3">
              <p class="text-lg font-bold text-white">4</p>
              <p class="text-xs text-gray-500">Bots</p>
            </div>
            <div class="rounded-lg bg-surface-700 p-3">
              <p class="text-lg font-bold" style="color: var(--accent)">5%</p>
              <p class="text-xs text-gray-500">Rake</p>
            </div>
          </div>

          <div class="flex gap-3 items-center mb-4">
            <div class="flex-1">
              <label class="text-xs text-gray-400 mb-1 block">Ante / Einsatz pro Spieler (€)</label>
              <input v-model.number="thBet" type="number" min="1" max="10000" class="input w-full" />
            </div>
            <div class="flex gap-2 items-end">
              <button v-for="quick in [10, 50, 100]" :key="quick" class="btn-secondary text-xs px-2 py-1" @click="thBet = quick">{{ quick }}</button>
            </div>
          </div>

          <button
            class="btn-primary w-full py-3 text-base font-semibold"
            :disabled="thLoading || !thBet || thBet < 1"
            @click="thStart"
          >
            {{ thLoading ? 'Karten werden gemischt...' : 'Spiel starten' }}
          </button>
        </div>

        <div class="card">
          <h4 class="text-sm font-semibold text-gray-300 mb-3">Handreihenfolge (schwächste → stärkste)</h4>
          <div class="flex flex-wrap gap-2 text-xs">
            <span v-for="hand in handRankings" :key="hand" class="px-2 py-1 rounded bg-white/5 text-gray-300">{{ hand }}</span>
          </div>
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

definePageMeta({ layout: 'default' })

const api = useApi()
const { formatCurrency } = useFormatting()
const gameStore = useGameStore()
const toast = useToastStore()

onMounted(async () => { await gameStore.init() })

// ── Tabs ──────────────────────────────────────────────────────────────────
const tabs = [
  { id: 'slots',     label: 'Slots',         icon: '🎰' },
  { id: 'blackjack', label: 'Blackjack',      icon: '🃏' },
  { id: 'poker',     label: 'Poker',          icon: '♠' },
  { id: 'texas',     label: "Hold'em",        icon: '🎴' },
]
const activeTab = ref<'slots' | 'blackjack' | 'poker' | 'texas'>('slots')

// ── Types ─────────────────────────────────────────────────────────────────
interface SlotResult { reels: string[]; outcome: string; betAmount: number; payout: number; netChange: number }
interface BjState { sessionId: number; playerCards: string[]; dealerCards: string[]; playerTotal: number; dealerVisible: number; status: string; betAmount: number; payout: number; netChange: number }
interface PokerResult { playerCards: string[]; aiCards: string[]; playerHandName: string; aiHandName: string; result: string; betAmount: number; payout: number; netChange: number }

interface ActionEntry { actor: string; actionType: string; amount: number | null }
interface DrawInfo { type: string; outs: number; probability: number; description: string }
interface BotInfo { index: number; folded: boolean; cards: string[]; handName: string | null; winner: boolean; personality: string }
interface THState {
  sessionId: number
  playerCards: string[]
  communityCards: string[]
  pot: number
  playerStake: number
  initialBet: number
  toCall: number
  raiseCost: number
  bots: BotInfo[]
  street: string
  status: string
  awaitingPlayerResponse: boolean
  actionLog: ActionEntry[]
  playerCurrentHandName: string | null
  playerDraws: DrawInfo[]
  payout: number | null
  netChange: number | null
}

// ── Slots ─────────────────────────────────────────────────────────────────
const slotBet = ref(10)
const spinning = ref(false)
const slotResult = ref<SlotResult | null>(null)
const slotSymbolMap: Record<string, string> = { SEVEN: '7️⃣', BELL: '🔔', BAR: '🎰', CHERRY: '🍒', LEMON: '🍋', BLANK: '⬛' }
const displayReels = computed<string[]>(() => {
  if (slotResult.value && !spinning.value) return slotResult.value.reels.map(r => slotSymbolMap[r] ?? r)
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
  } catch (e: any) { toast.error(e?.data?.message ?? 'Fehler beim Spielen') }
  finally { spinning.value = false }
}

// ── Blackjack ─────────────────────────────────────────────────────────────
const bjBet = ref(10)
const bjLoading = ref(false)
const bjGame = ref<BjState | null>(null)

async function bjStart() {
  bjLoading.value = true
  try { bjGame.value = await api.post<BjState>('/api/gambling/blackjack/start', { bet: bjBet.value }); await gameStore.fetchCharacter() }
  catch (e: any) { toast.error(e?.data?.message ?? 'Fehler beim Starten') }
  finally { bjLoading.value = false }
}
async function bjHit() {
  if (!bjGame.value) return
  bjLoading.value = true
  try { bjGame.value = await api.post<BjState>(`/api/gambling/blackjack/${bjGame.value.sessionId}/hit`, {}); await gameStore.fetchCharacter() }
  catch (e: any) { toast.error(e?.data?.message ?? 'Fehler') }
  finally { bjLoading.value = false }
}
async function bjStand() {
  if (!bjGame.value) return
  bjLoading.value = true
  try { bjGame.value = await api.post<BjState>(`/api/gambling/blackjack/${bjGame.value.sessionId}/stand`, {}); await gameStore.fetchCharacter() }
  catch (e: any) { toast.error(e?.data?.message ?? 'Fehler') }
  finally { bjLoading.value = false }
}
function calcHandTotal(cards: string[]): number {
  let total = 0, aces = 0
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
  try { pokerResult.value = await api.post<PokerResult>('/api/gambling/poker', { bet: pokerBet.value }); await gameStore.fetchCharacter() }
  catch (e: any) { toast.error(e?.data?.message ?? 'Fehler beim Spielen') }
  finally { pokerLoading.value = false }
}

// ── Texas Hold'em ──────────────────────────────────────────────────────────
const thBet = ref(10)
const thLoading = ref(false)
const thGame = ref<THState | null>(null)
const activeActor = ref<string | null>(null)
const actorActionMap = ref<Record<string, string>>({})
const thRaiseAmount = ref(10)

async function animateActions(log: ActionEntry[]) {
  actorActionMap.value = {}
  activeActor.value = null
  for (const entry of log) {
    activeActor.value = entry.actor
    const label =
      entry.actionType === 'FOLD'  ? 'Fold' :
      entry.actionType === 'CHECK' ? 'Check' :
      entry.actionType === 'CALL'  ? `Call ${entry.amount ? formatCurrency(entry.amount) : ''}` :
      entry.actionType === 'RAISE' ? `Raise ${entry.amount ? formatCurrency(entry.amount) : ''}` :
      entry.actionType
    actorActionMap.value = { ...actorActionMap.value, [entry.actor]: label }
    await new Promise(r => setTimeout(r, 650))
  }
  activeActor.value = null
}

async function thStart() {
  thLoading.value = true
  actorActionMap.value = {}
  try {
    const result = await api.post<THState>('/api/gambling/texasholdem/start', { bet: thBet.value })
    thGame.value = result
    thRaiseAmount.value = result.initialBet
    await gameStore.fetchCharacter()
  } catch (e: any) { toast.error(e?.data?.message ?? 'Fehler beim Starten') }
  finally { thLoading.value = false }
}

async function thAct(action: string, amount?: number) {
  if (!thGame.value) return
  thLoading.value = true
  try {
    const result = await api.post<THState>(`/api/gambling/texasholdem/${thGame.value.sessionId}/action`, { action, amount: amount ?? null })
    thGame.value = result
    await gameStore.fetchCharacter()
    if (result.actionLog?.length) await animateActions(result.actionLog)
    if (result.status === 'WON') toast.success('Gewonnen! ' + formatCurrency(result.netChange ?? 0))
  } catch (e: any) { toast.error(e?.data?.message ?? 'Fehler') }
  finally { thLoading.value = false }
}

async function thAllIn() {
  const cash = gameStore.character?.cash ?? 0
  if (cash <= 0) return
  // raiseBy = all remaining cash minus what's needed to call first
  const toCall = thGame.value?.toCall ?? 0
  const raiseBy = Math.max(cash - toCall, thGame.value?.initialBet ?? 0)
  await thAct('RAISE', raiseBy)
}

function streetLabel(street: string): string {
  return ({ PREFLOP: 'Pre-Flop', FLOP: 'Flop', TURN: 'Turn', RIVER: 'River', SHOWDOWN: 'Showdown' } as Record<string, string>)[street] ?? street
}

function streetBadgeClass(street: string): string {
  if (street === 'SHOWDOWN') return 'bg-yellow-500/10 border-yellow-500/30 text-yellow-300'
  if (street === 'RIVER')    return 'bg-red-500/10 border-red-500/30 text-red-300'
  return 'bg-accent/10 border-accent/30 text-accent'
}

function thResultLabel(status: string): string {
  return ({ WON: 'Gewonnen! 🏆', LOST: 'Verloren', PUSH: 'Unentschieden — Pot aufgeteilt' } as Record<string, string>)[status] ?? status
}

// ── Labels & Helpers ───────────────────────────────────────────────────────
const payoutTable = [
  { symbols: '7️⃣7️⃣7️⃣', label: 'JACKPOT', multiplier: '50×' },
  { symbols: '🔔🔔🔔', label: 'Großer Gewinn', multiplier: '10×' },
  { symbols: '🎰🎰🎰 / 🍒🍒🍒 / 🍋🍋🍋', label: 'Gewinn', multiplier: '5×' },
  { symbols: 'Zwei gleiche', label: 'Kleiner Gewinn', multiplier: '1,5×' },
  { symbols: 'Keine Übereinstimmung', label: 'Verlust', multiplier: '0×' },
]
const handRankings = ['Highcard', 'Ein Paar', 'Zwei Paare', 'Drilling', 'Straight', 'Flush', 'Full House', 'Vierling', 'Straight Flush', 'Royal Flush']

function outcomeLabel(outcome: string): string {
  return ({ JACKPOT: 'JACKPOT!', BIG_WIN: 'Großer Gewinn!', WIN: 'Gewinn!', SMALL_WIN: 'Kleiner Gewinn!', LOSS: 'Verloren' } as Record<string, string>)[outcome] ?? outcome
}
function bjResultLabel(status: string): string {
  return ({ WON: 'Gewonnen!', LOST: 'Verloren', PUSH: 'Unentschieden' } as Record<string, string>)[status] ?? status
}
function pokerResultLabel(result: string): string {
  return ({ WON: 'Gewonnen!', LOST: 'Verloren', PUSH: 'Unentschieden (Split Pot)' } as Record<string, string>)[result] ?? result
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
