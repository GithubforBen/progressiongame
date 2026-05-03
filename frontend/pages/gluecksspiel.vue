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
            v-for="(reel, i) in animatingReels"
            :key="i"
            class="w-20 h-20 flex items-center justify-center rounded-xl text-4xl border-2 transition-all duration-300 overflow-hidden relative"
            :class="[
              lockedReels[i] ? (slotResult ? resultBorderClass(slotResult.outcome) : 'border-green-500/50 bg-green-500/10') : '',
              !lockedReels[i] && spinning ? 'border-accent/50 bg-accent/10' : '',
              !spinning && !lockedReels[i] ? (slotResult ? resultBorderClass(slotResult.outcome) : 'border-surface-600 bg-surface-700') : '',
            ]"
          >
            <span :class="spinning && !lockedReels[i] ? 'reel-spin' : ''">{{ reel }}</span>
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
              <span v-if="bjGame.playerTotal === 21 && bjGame.playerCards?.length === 2" class="ml-2 text-yellow-400 font-bold text-xs">BLACKJACK!</span>
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

    <!-- ===== PLINKO ===== -->
    <div v-if="activeTab === 'plinko'" class="space-y-4">

      <!-- Board card -->
      <div class="card">
        <div class="flex items-start justify-between mb-1">
          <h3 class="text-base font-semibold text-white">Plinko</h3>
          <span class="text-xs text-gray-500">House Edge ~4,3%</span>
        </div>
        <p class="text-xs text-gray-400 mb-3">Bälle fallen durch 8 Reihen Stifte. Randspalten zahlen bis zu 10×.</p>

        <PlinkoBoard
          :queue="plinkoQueue"
          class="mb-4"
          @group-done="onPlinkoGroupDone"
          @ball-landed="onBallLanded"
        />

        <!-- Bet info -->
        <div v-if="plinkoBet >= 1" class="mb-3 px-3 py-1.5 rounded-lg bg-white/4 border border-white/8 text-xs flex items-center justify-between">
          <span class="text-gray-400">Bälle:</span>
          <span class="font-semibold text-white font-mono">{{ plinkoBallCount }} × {{ formatCurrency(plinkoBallValue) }}/Ball</span>
        </div>

        <!-- Bet input -->
        <div class="flex gap-2 items-center mb-3">
          <div class="flex-1">
            <label class="text-xs text-gray-400 mb-1 block">Einsatz (€)</label>
            <input v-model.number="plinkoBet" type="number" min="1" class="input w-full" />
          </div>
          <div class="flex gap-1.5 items-end flex-wrap">
            <button v-for="q in [10, 50, 100, 300, 500]" :key="q" class="btn-secondary text-xs px-2 py-1" @click="plinkoBet = q">{{ q }}</button>
          </div>
        </div>

        <button
          class="btn-primary w-full py-3 text-base font-semibold"
          :disabled="plinkoLoading || !plinkoBet || plinkoBet < 1"
          @click="playPlinko"
        >
          <span v-if="plinkoLoading">Server berechnet...</span>
          <span v-else>{{ plinkoBallCount }} {{ plinkoBallCount === 1 ? 'Ball' : 'Bälle' }} loslassen!</span>
        </button>
      </div>

      <!-- Live feed -->
      <div v-if="plinkoFeed.length > 0" class="card p-0 overflow-hidden">
        <div class="px-4 py-2.5 border-b border-surface-600/40 flex items-center justify-between">
          <span class="text-xs font-semibold text-gray-300">Ergebnis-Feed</span>
          <button class="text-xs text-gray-600 hover:text-gray-400 transition-colors" @click="plinkoFeed.splice(0)">Leeren</button>
        </div>
        <div class="feed-scroll">
          <div
            v-for="entry in plinkoFeed"
            :key="entry.key"
            class="feed-row"
            :class="entry.netChange >= 0 ? 'feed-win' : 'feed-loss'"
          >
            <span class="feed-dot" :style="{ background: entry.color }" />
            <span class="feed-mult">{{ entry.multiplier }}×</span>
            <span class="feed-payout" :class="entry.netChange >= 0 ? 'text-green-400' : 'text-red-400'">
              {{ entry.netChange >= 0 ? '+' : '' }}{{ formatCurrency(entry.netChange) }}
            </span>
            <span class="feed-time">{{ entry.time }}</span>
          </div>
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

              <p class="text-xs font-medium mb-0.5" :class="bot.folded ? 'text-gray-600' : bot.winner ? 'text-red-400' : activeActor === 'BOT_' + bot.index ? 'text-accent' : 'text-gray-400'">
                {{ bot.personality || ('Bot ' + (bot.index + 1)) }}
              </p>
              <span v-if="bot.riskProfile && !bot.folded" class="inline-block text-[10px] font-bold px-1.5 py-0.5 rounded mb-1"
                :class="bot.riskProfile === 'LOW' ? 'bg-green-900/40 text-green-400' : bot.riskProfile === 'HIGH' ? 'bg-red-900/40 text-red-400' : 'bg-yellow-900/40 text-yellow-400'">
                {{ bot.riskProfile === 'LOW' ? 'Konservativ' : bot.riskProfile === 'HIGH' ? 'Risikoreich' : 'Ausgewogen' }}
              </span>
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
              v-for="i in (5 - (thGame.communityCards?.length ?? 0))"
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

    <!-- ===== ROULETTE ===== -->
    <div v-if="activeTab === 'roulette'" class="space-y-4">

      <!-- Wheel card -->
      <div class="card flex flex-col items-center gap-4">
        <div class="self-start">
          <h3 class="text-base font-semibold text-white">Europäisches Roulette</h3>
          <p class="text-xs text-gray-400">Einzelne Null (0–36). Einfach auf Zahlen oder Chancen setzen.</p>
        </div>

        <div class="relative w-52 h-52 flex items-center justify-center select-none">
          <!-- Spinning wheel (conic-gradient sectors) -->
          <div
            class="absolute inset-0 rounded-full border-4 border-surface-600 overflow-hidden"
            :style="{
              background: `conic-gradient(
                #166534 0deg 9.73deg,
                #111 9.73deg 19.46deg,
                #b91c1c 19.46deg 29.19deg,
                #111 29.19deg 38.92deg,
                #b91c1c 38.92deg 48.65deg,
                #111 48.65deg 58.38deg,
                #b91c1c 58.38deg 68.11deg,
                #111 68.11deg 77.84deg,
                #b91c1c 77.84deg 87.57deg,
                #111 87.57deg 97.3deg,
                #b91c1c 97.3deg 107.03deg,
                #111 107.03deg 116.76deg,
                #b91c1c 116.76deg 126.49deg,
                #111 126.49deg 136.22deg,
                #b91c1c 136.22deg 145.95deg,
                #111 145.95deg 155.68deg,
                #b91c1c 155.68deg 165.41deg,
                #111 165.41deg 175.14deg,
                #b91c1c 175.14deg 184.87deg,
                #111 184.87deg 194.6deg,
                #b91c1c 194.6deg 204.33deg,
                #111 204.33deg 214.06deg,
                #b91c1c 214.06deg 223.79deg,
                #111 223.79deg 233.52deg,
                #b91c1c 233.52deg 243.25deg,
                #111 243.25deg 252.98deg,
                #b91c1c 252.98deg 262.71deg,
                #111 262.71deg 272.44deg,
                #b91c1c 272.44deg 282.17deg,
                #111 282.17deg 291.9deg,
                #b91c1c 291.9deg 301.63deg,
                #111 301.63deg 311.36deg,
                #b91c1c 311.36deg 321.09deg,
                #111 321.09deg 330.82deg,
                #b91c1c 330.82deg 340.55deg,
                #111 340.55deg 350.27deg,
                #b91c1c 350.27deg 360deg
              )`,
              transform: `rotate(${rouletteWheelAngle}deg)`,
              transition: rouletteSpinning ? 'none' : 'transform 0.9s cubic-bezier(0.17,0.67,0.12,0.99)'
            }"
          />
          <!-- Outer ring border numbers -->
          <div class="absolute inset-0 rounded-full border-4 border-yellow-700/40 pointer-events-none" />
          <!-- Ball orbiting -->
          <div
            class="absolute w-3.5 h-3.5 rounded-full bg-white shadow-[0_0_6px_rgba(255,255,255,0.8)] z-10"
            :style="{
              transform: `rotate(${-rouletteWheelAngle * 1.4}deg) translateX(88px)`,
              transition: rouletteSpinning ? 'none' : 'transform 0.9s ease-out'
            }"
          />
          <!-- Center hub -->
          <div class="absolute w-14 h-14 rounded-full bg-surface-900 border-2 border-yellow-700/60 z-20 flex items-center justify-center shadow-lg">
            <span
              v-if="winningNumber !== null"
              class="text-base font-black"
              :class="rouletteNumberColor(winningNumber) === 'red' ? 'text-red-400' : rouletteNumberColor(winningNumber) === 'black' ? 'text-white' : 'text-green-400'"
            >{{ winningNumber }}</span>
            <span v-else class="text-gray-500 text-xl">🎡</span>
          </div>
        </div>

        <!-- Winning announcement -->
        <div v-if="winningNumber !== null" class="text-center">
          <span
            class="px-5 py-1.5 rounded-full text-sm font-bold border"
            :class="rouletteNumberColor(winningNumber) === 'red'   ? 'bg-red-500/20 border-red-500/40 text-red-300' :
                    rouletteNumberColor(winningNumber) === 'black' ? 'bg-white/10 border-white/20 text-white' :
                    'bg-green-500/20 border-green-500/40 text-green-300'"
          >Gewinnzahl: {{ winningNumber }}</span>
        </div>
      </div>

      <!-- Chip selector -->
      <div class="card">
        <p class="text-xs text-gray-400 mb-2">Chip-Wert wählen</p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="chip in chipOptions"
            :key="chip"
            class="w-12 h-12 rounded-full text-xs font-bold border-2 transition-all"
            :class="selectedChip === chip
              ? 'bg-accent border-accent text-white scale-110 shadow-[0_0_14px_rgba(212,50,31,0.5)]'
              : 'bg-surface-700 border-surface-600 text-gray-300 hover:border-accent/50'"
            :disabled="rouletteSpinning"
            @click="selectedChip = chip"
          >{{ chip >= 1000 ? (chip / 1000) + 'K' : chip }}</button>
        </div>
      </div>

      <!-- Betting table -->
      <div class="card overflow-x-auto">
        <p class="text-xs text-gray-400 mb-3">Setze auf Zahlen — Klick auf eine Zahl = Plein (35:1)</p>

        <!-- Number grid -->
        <div class="roulette-table min-w-[340px]">

          <!-- Zero — spans all 12 rows -->
          <div
            class="roulette-cell zero-cell"
            :class="{ 'has-bet': betAmountFor('STRAIGHT', [0]), 'cell-winner': winningNumber === 0 }"
            @click="addBet('STRAIGHT', [0])"
          >
            <span>0</span>
            <span v-if="betAmountFor('STRAIGHT', [0])" class="chip-indicator">{{ betAmountFor('STRAIGHT', [0]) }}</span>
          </div>

          <!-- Numbers 1–36 in rows of 3 -->
          <template v-for="row in 12" :key="'row-' + row">
            <template v-for="col in 3" :key="'cell-' + row + '-' + col">
              <div
                class="roulette-cell number-cell"
                :class="[
                  rouletteNumberColor((row - 1) * 3 + col) === 'red' ? 'cell-red' : 'cell-black',
                  winningNumber === (row - 1) * 3 + col ? 'cell-winner' : '',
                  betAmountFor('STRAIGHT', [(row - 1) * 3 + col]) ? 'has-bet' : ''
                ]"
                @click="addBet('STRAIGHT', [(row - 1) * 3 + col])"
              >
                <span>{{ (row - 1) * 3 + col }}</span>
                <span v-if="betAmountFor('STRAIGHT', [(row - 1) * 3 + col])" class="chip-indicator">
                  {{ betAmountFor('STRAIGHT', [(row - 1) * 3 + col]) }}
                </span>
              </div>
            </template>
          </template>

          <!-- Column 2:1 bets (right side) -->
          <div class="roulette-cell column-cell" :class="{ 'has-bet': betAmountFor('COLUMN_1', []) }" @click="addBet('COLUMN_1', [])">
            <span class="text-xs leading-tight text-center">2:1<br/><span class="text-[9px] text-gray-400">Kol.1</span></span>
            <span v-if="betAmountFor('COLUMN_1', [])" class="chip-indicator">{{ betAmountFor('COLUMN_1', []) }}</span>
          </div>
          <div class="roulette-cell column-cell" :class="{ 'has-bet': betAmountFor('COLUMN_2', []) }" @click="addBet('COLUMN_2', [])">
            <span class="text-xs leading-tight text-center">2:1<br/><span class="text-[9px] text-gray-400">Kol.2</span></span>
            <span v-if="betAmountFor('COLUMN_2', [])" class="chip-indicator">{{ betAmountFor('COLUMN_2', []) }}</span>
          </div>
          <div class="roulette-cell column-cell" :class="{ 'has-bet': betAmountFor('COLUMN_3', []) }" @click="addBet('COLUMN_3', [])">
            <span class="text-xs leading-tight text-center">2:1<br/><span class="text-[9px] text-gray-400">Kol.3</span></span>
            <span v-if="betAmountFor('COLUMN_3', [])" class="chip-indicator">{{ betAmountFor('COLUMN_3', []) }}</span>
          </div>

          <!-- Dozen row -->
          <div style="grid-column: 2 / 5; grid-row: 13;" class="grid grid-cols-3 gap-[2px]">
            <div class="roulette-cell outside-cell" :class="{ 'has-bet': betAmountFor('DOZEN_1', []) }" @click="addBet('DOZEN_1', [])">
              <span>1–12</span><span v-if="betAmountFor('DOZEN_1', [])" class="chip-indicator">{{ betAmountFor('DOZEN_1', []) }}</span>
            </div>
            <div class="roulette-cell outside-cell" :class="{ 'has-bet': betAmountFor('DOZEN_2', []) }" @click="addBet('DOZEN_2', [])">
              <span>13–24</span><span v-if="betAmountFor('DOZEN_2', [])" class="chip-indicator">{{ betAmountFor('DOZEN_2', []) }}</span>
            </div>
            <div class="roulette-cell outside-cell" :class="{ 'has-bet': betAmountFor('DOZEN_3', []) }" @click="addBet('DOZEN_3', [])">
              <span>25–36</span><span v-if="betAmountFor('DOZEN_3', [])" class="chip-indicator">{{ betAmountFor('DOZEN_3', []) }}</span>
            </div>
          </div>

          <!-- Even-money row -->
          <div style="grid-column: 2 / 5; grid-row: 14;" class="grid grid-cols-6 gap-[2px]">
            <div class="roulette-cell outside-cell" :class="{ 'has-bet': betAmountFor('LOW', []) }" @click="addBet('LOW', [])">
              <span class="text-[10px]">1–18</span><span v-if="betAmountFor('LOW', [])" class="chip-indicator">{{ betAmountFor('LOW', []) }}</span>
            </div>
            <div class="roulette-cell outside-cell" :class="{ 'has-bet': betAmountFor('EVEN', []) }" @click="addBet('EVEN', [])">
              <span class="text-[10px]">Ger.</span><span v-if="betAmountFor('EVEN', [])" class="chip-indicator">{{ betAmountFor('EVEN', []) }}</span>
            </div>
            <div class="roulette-cell outside-cell cell-red" :class="{ 'has-bet': betAmountFor('RED', []) }" @click="addBet('RED', [])">
              <span class="text-[10px]">Rot</span><span v-if="betAmountFor('RED', [])" class="chip-indicator">{{ betAmountFor('RED', []) }}</span>
            </div>
            <div class="roulette-cell outside-cell cell-black" :class="{ 'has-bet': betAmountFor('BLACK', []) }" @click="addBet('BLACK', [])">
              <span class="text-[10px]">Schw.</span><span v-if="betAmountFor('BLACK', [])" class="chip-indicator">{{ betAmountFor('BLACK', []) }}</span>
            </div>
            <div class="roulette-cell outside-cell" :class="{ 'has-bet': betAmountFor('ODD', []) }" @click="addBet('ODD', [])">
              <span class="text-[10px]">Ung.</span><span v-if="betAmountFor('ODD', [])" class="chip-indicator">{{ betAmountFor('ODD', []) }}</span>
            </div>
            <div class="roulette-cell outside-cell" :class="{ 'has-bet': betAmountFor('HIGH', []) }" @click="addBet('HIGH', [])">
              <span class="text-[10px]">19–36</span><span v-if="betAmountFor('HIGH', [])" class="chip-indicator">{{ betAmountFor('HIGH', []) }}</span>
            </div>
          </div>

        </div>

        <!-- Combination bets panel -->
        <div class="mt-5 space-y-4">
          <p class="text-xs text-gray-500 uppercase tracking-wider">Kombinations-Einsätze</p>

          <!-- Street bets -->
          <div>
            <p class="text-xs text-gray-400 mb-1.5">Street — 3 Zahlen (11:1)</p>
            <div class="flex flex-wrap gap-1">
              <button
                v-for="row in 12" :key="'st-' + row"
                class="px-2 py-1 text-[11px] rounded border transition-colors"
                :class="betAmountFor('STREET', [(row-1)*3+1,(row-1)*3+2,(row-1)*3+3])
                  ? 'bg-accent/20 border-accent/60 text-accent'
                  : 'bg-surface-700 border-surface-600 text-gray-400 hover:border-accent/40'"
                :disabled="rouletteSpinning"
                @click="addBet('STREET', [(row-1)*3+1,(row-1)*3+2,(row-1)*3+3])"
              >
                {{ (row-1)*3+1 }}–{{ (row-1)*3+3 }}
                <span v-if="betAmountFor('STREET', [(row-1)*3+1,(row-1)*3+2,(row-1)*3+3])" class="ml-0.5 text-accent/80">({{ betAmountFor('STREET', [(row-1)*3+1,(row-1)*3+2,(row-1)*3+3]) }})</span>
              </button>
            </div>
          </div>

          <!-- Six Line bets -->
          <div>
            <p class="text-xs text-gray-400 mb-1.5">Six Line — 6 Zahlen (5:1)</p>
            <div class="flex flex-wrap gap-1">
              <button
                v-for="row in 11" :key="'sl-' + row"
                class="px-2 py-1 text-[11px] rounded border transition-colors"
                :class="betAmountFor('SIX_LINE', [(row-1)*3+1,(row-1)*3+2,(row-1)*3+3,(row-1)*3+4,(row-1)*3+5,(row-1)*3+6])
                  ? 'bg-accent/20 border-accent/60 text-accent'
                  : 'bg-surface-700 border-surface-600 text-gray-400 hover:border-accent/40'"
                :disabled="rouletteSpinning"
                @click="addBet('SIX_LINE', [(row-1)*3+1,(row-1)*3+2,(row-1)*3+3,(row-1)*3+4,(row-1)*3+5,(row-1)*3+6])"
              >{{ (row-1)*3+1 }}–{{ (row-1)*3+6 }}</button>
            </div>
          </div>

          <!-- Trio bets -->
          <div>
            <p class="text-xs text-gray-400 mb-1.5">Trio — 0+1+2 oder 0+2+3 (11:1)</p>
            <div class="flex gap-2">
              <button
                class="px-3 py-1.5 text-[11px] rounded border transition-colors"
                :class="betAmountFor('TRIO',[0,1,2]) ? 'bg-accent/20 border-accent/60 text-accent' : 'bg-surface-700 border-surface-600 text-gray-400 hover:border-accent/40'"
                :disabled="rouletteSpinning" @click="addBet('TRIO',[0,1,2])"
              >0-1-2 <span v-if="betAmountFor('TRIO',[0,1,2])" class="text-accent/80">({{ betAmountFor('TRIO',[0,1,2]) }})</span></button>
              <button
                class="px-3 py-1.5 text-[11px] rounded border transition-colors"
                :class="betAmountFor('TRIO',[0,2,3]) ? 'bg-accent/20 border-accent/60 text-accent' : 'bg-surface-700 border-surface-600 text-gray-400 hover:border-accent/40'"
                :disabled="rouletteSpinning" @click="addBet('TRIO',[0,2,3])"
              >0-2-3 <span v-if="betAmountFor('TRIO',[0,2,3])" class="text-accent/80">({{ betAmountFor('TRIO',[0,2,3]) }})</span></button>
            </div>
          </div>

          <!-- Split horizontal -->
          <div>
            <p class="text-xs text-gray-400 mb-1.5">Split — horizontal benachbart (17:1)</p>
            <div class="flex flex-wrap gap-1">
              <button
                v-for="n in splitHorizontal" :key="'sh-' + n"
                class="px-1.5 py-0.5 text-[10px] rounded border transition-colors"
                :class="betAmountFor('SPLIT',[n, n+1]) ? 'bg-accent/20 border-accent/60 text-accent' : 'bg-surface-700 border-surface-600 text-gray-500 hover:border-accent/40'"
                :disabled="rouletteSpinning"
                @click="addBet('SPLIT',[n, n+1])"
              >{{ n }}/{{ n+1 }}</button>
            </div>
          </div>

          <!-- Split vertical -->
          <div>
            <p class="text-xs text-gray-400 mb-1.5">Split — vertikal benachbart (17:1)</p>
            <div class="flex flex-wrap gap-1">
              <button
                v-for="n in splitVertical" :key="'sv-' + n"
                class="px-1.5 py-0.5 text-[10px] rounded border transition-colors"
                :class="betAmountFor('SPLIT',[n, n+3]) ? 'bg-accent/20 border-accent/60 text-accent' : 'bg-surface-700 border-surface-600 text-gray-500 hover:border-accent/40'"
                :disabled="rouletteSpinning"
                @click="addBet('SPLIT',[n, n+3])"
              >{{ n }}/{{ n+3 }}</button>
            </div>
          </div>

          <!-- Corner bets -->
          <div>
            <p class="text-xs text-gray-400 mb-1.5">Carré — Ecke 4 Zahlen (8:1)</p>
            <div class="flex flex-wrap gap-1">
              <button
                v-for="c in cornerBets" :key="'cb-' + c[0]"
                class="px-1.5 py-0.5 text-[10px] rounded border transition-colors"
                :class="betAmountFor('CORNER', c) ? 'bg-accent/20 border-accent/60 text-accent' : 'bg-surface-700 border-surface-600 text-gray-500 hover:border-accent/40'"
                :disabled="rouletteSpinning"
                @click="addBet('CORNER', c)"
              >{{ c[0] }}-{{ c[1] }}-{{ c[2] }}-{{ c[3] }}</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Active bets summary -->
      <div v-if="placedBets.length > 0" class="card space-y-2">
        <div class="flex items-center justify-between">
          <p class="text-sm font-semibold text-white">Platzierte Einsätze ({{ placedBets.length }})</p>
          <button class="text-xs text-gray-500 hover:text-red-400 transition-colors" @click="clearBets">Alle löschen</button>
        </div>
        <div class="space-y-1 max-h-36 overflow-y-auto pr-1">
          <div v-for="(bet, i) in placedBets" :key="i" class="flex items-center justify-between text-xs">
            <span class="text-gray-300">{{ rouletteBetLabel(bet) }}</span>
            <div class="flex items-center gap-2">
              <span class="font-mono text-white">{{ formatCurrency(bet.amount) }}</span>
              <button class="text-gray-600 hover:text-red-400 text-base leading-none" @click="removeBet(i)">×</button>
            </div>
          </div>
        </div>
        <div class="flex justify-between text-sm pt-2 border-t border-surface-600">
          <span class="text-gray-400">Gesamteinsatz</span>
          <span class="font-bold text-white">{{ formatCurrency(totalPlacedBet) }}</span>
        </div>
      </div>

      <!-- Spin button -->
      <button
        class="btn-primary w-full text-base py-3 font-semibold"
        :disabled="rouletteSpinning || placedBets.length === 0"
        @click="playRoulette"
      >
        {{ rouletteSpinning ? '🎡 Dreht...' : 'Drehen!' }}
      </button>

      <!-- Result -->
      <div v-if="rouletteResult && !rouletteSpinning" class="card space-y-3">
        <div class="flex items-center justify-between">
          <span class="font-bold text-base" :class="rouletteResult.netChange >= 0 ? 'text-green-300' : 'text-red-400'">
            {{ rouletteResult.netChange >= 0 ? 'Gewonnen!' : 'Verloren' }}
          </span>
          <span class="font-bold text-lg font-mono" :class="rouletteResult.netChange >= 0 ? 'text-green-400' : 'text-red-400'">
            {{ rouletteResult.netChange >= 0 ? '+' : '' }}{{ formatCurrency(rouletteResult.netChange) }}
          </span>
        </div>
        <div class="space-y-1">
          <div
            v-for="(br, i) in rouletteResult.betResults"
            :key="i"
            class="flex justify-between text-xs"
            :class="br.won ? 'text-green-300' : 'text-gray-500'"
          >
            <span>{{ rouletteBetLabel(br) }}</span>
            <span class="font-mono">{{ br.won ? '+' + formatCurrency(br.payout - br.amount) : '-' + formatCurrency(br.amount) }}</span>
          </div>
        </div>
      </div>

      <!-- Payout table -->
      <div class="card">
        <h4 class="text-sm font-semibold text-gray-300 mb-3">Auszahlungstabelle</h4>
        <div class="grid grid-cols-2 gap-x-6 gap-y-1 text-xs">
          <div class="flex justify-between"><span class="text-gray-400">Plein (1 Zahl)</span><span class="text-white font-medium">35:1</span></div>
          <div class="flex justify-between"><span class="text-gray-400">Cheval (2 Zahlen)</span><span class="text-white font-medium">17:1</span></div>
          <div class="flex justify-between"><span class="text-gray-400">Street / Trio (3)</span><span class="text-white font-medium">11:1</span></div>
          <div class="flex justify-between"><span class="text-gray-400">Carré (4 Zahlen)</span><span class="text-white font-medium">8:1</span></div>
          <div class="flex justify-between"><span class="text-gray-400">Sixain (6 Zahlen)</span><span class="text-white font-medium">5:1</span></div>
          <div class="flex justify-between"><span class="text-gray-400">Dutzend / Kolonne</span><span class="text-white font-medium">2:1</span></div>
          <div class="flex justify-between"><span class="text-gray-400">Rot / Schwarz</span><span class="text-white font-medium">1:1</span></div>
          <div class="flex justify-between"><span class="text-gray-400">Gerade / Ungerade</span><span class="text-white font-medium">1:1</span></div>
          <div class="flex justify-between"><span class="text-gray-400">1–18 / 19–36</span><span class="text-white font-medium">1:1</span></div>
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
  { id: 'plinko',    label: 'Plinko',         icon: '🎯' },
  { id: 'texas',     label: "Hold'em",        icon: '🎴' },
  { id: 'roulette',  label: 'Roulette',       icon: '🎡' },
]
const activeTab = ref<'slots' | 'blackjack' | 'plinko' | 'texas' | 'roulette'>('slots')

// ── Types ─────────────────────────────────────────────────────────────────
interface SlotResult { reels: string[]; outcome: string; betAmount: number; payout: number; netChange: number }
interface BjState { sessionId: number; playerCards: string[]; dealerCards: string[]; playerTotal: number; dealerVisible: number; status: string; betAmount: number; payout: number; netChange: number }
interface PlinkoBallResult { path: boolean[]; slot: number; multiplier: number; payout: number }
interface PlinkoResult { balls: PlinkoBallResult[]; ballCount: number; ballValue: number; betAmount: number; totalPayout: number; netChange: number }

interface ActionEntry { actor: string; actionType: string; amount: number | null }
interface DrawInfo { type: string; outs: number; probability: number; description: string }
interface BotInfo { index: number; folded: boolean; cards: string[]; handName: string | null; winner: boolean; personality: string; riskProfile: string }
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

interface RouletteBet { type: string; numbers: number[]; amount: number }
interface RouletteResult {
  winningNumber: number
  winningColor: string
  totalBet: number
  totalPayout: number
  netChange: number
  betResults: { type: string; numbers: number[]; amount: number; won: boolean; payout: number }[]
}

// ── Slots ─────────────────────────────────────────────────────────────────
const slotBet = ref(10)
const spinning = ref(false)
const slotResult = ref<SlotResult | null>(null)
const slotSymbolMap: Record<string, string> = { SEVEN: '7️⃣', BELL: '🔔', BAR: '🎰', CHERRY: '🍒', LEMON: '🍋', BLANK: '⬛' }
const allSymbols = ['7️⃣', '🔔', '🎰', '🍒', '🍋', '⬛']
const animatingReels = ref<string[]>(['❓', '❓', '❓'])
const lockedReels = ref<boolean[]>([false, false, false])

async function playSlots() {
  spinning.value = true
  slotResult.value = null
  lockedReels.value = [false, false, false]
  animatingReels.value = ['❓', '❓', '❓']

  // Start rapid symbol cycling
  const intervals: ReturnType<typeof setInterval>[] = []
  for (let i = 0; i < 3; i++) {
    let idx = 0
    intervals[i] = setInterval(() => {
      animatingReels.value[i] = allSymbols[idx % allSymbols.length]
      idx++
    }, 80)
  }

  try {
    const result = await api.post<SlotResult>('/api/gambling/slots', { bet: slotBet.value })
    const finalSymbols = (result.reels ?? []).map(r => slotSymbolMap[r] ?? r)

    // Stop reels sequentially: 600ms, 1200ms, 1800ms after result
    for (let i = 0; i < 3; i++) {
      await new Promise<void>(resolve => setTimeout(resolve, 600))
      clearInterval(intervals[i])
      animatingReels.value[i] = finalSymbols[i]
      lockedReels.value[i] = true
    }

    slotResult.value = result
    await gameStore.fetchCharacter()
    if (result.netChange >= 0) toast.success(outcomeLabel(result.outcome) + ' ' + formatCurrency(result.netChange))
  } catch (e: any) {
    intervals.forEach(clearInterval)
    animatingReels.value = ['❌', '❌', '❌']
    toast.error((e as any)?.data?.message ?? 'Fehler beim Spielen')
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
function calcHandTotal(cards: string[] | undefined): number {
  if (!cards) return 0
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

// ── Plinko ────────────────────────────────────────────────────────────────
const plinkoBet      = ref(10)
const plinkoLoading  = ref(false)

interface PlinkoQueueItem {
  id:         number
  balls:      PlinkoBallResult[]
  ballValue:  number
  betAmount:  number
  totalPayout: number
  netChange:  number
}
interface FeedEntry {
  key:        number
  multiplier: number
  netChange:  number
  color:      string
  time:       string
}

const plinkoQueue  = ref<PlinkoQueueItem[]>([])
const plinkoFeed   = ref<FeedEntry[]>([])
let   plinkoIdSeq  = 0
let   feedKeySeq   = 0

const plinkoBallCount = computed(() => {
  const bet = plinkoBet.value ?? 0
  return Math.max(1, Math.min(500, Math.floor(bet / 1)))
})
const plinkoBallValue = computed(() => {
  const bet = plinkoBet.value ?? 0
  if (bet < 1) return 0
  return bet / plinkoBallCount.value
})

async function playPlinko() {
  plinkoLoading.value = true
  try {
    const r = await api.post<PlinkoResult>('/api/gambling/plinko', { bet: plinkoBet.value })
    await gameStore.fetchCharacter()
    plinkoIdSeq++
    plinkoQueue.value.push({
      id: plinkoIdSeq,
      balls: r.balls,
      ballValue: r.ballValue,
      betAmount: r.betAmount,
      totalPayout: r.totalPayout,
      netChange: r.netChange,
    })
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Fehler beim Spielen')
  } finally {
    plinkoLoading.value = false
  }
}

function onPlinkoGroupDone(id: number) {
  const item = plinkoQueue.value.find(q => q.id === id)
  if (!item) return
  if (item.netChange >= 0) toast.success(`${item.balls.length} Bälle — +${formatCurrency(item.netChange)}`)
  plinkoQueue.value = plinkoQueue.value.filter(q => q.id !== id)
}

function onBallLanded(groupId: number, slot: number, multiplier: number, color: string) {
  const item = plinkoQueue.value.find(q => q.id === groupId)
  if (!item) return
  const payout    = item.ballValue * multiplier
  const netChange = payout - item.ballValue
  feedKeySeq++
  plinkoFeed.value.unshift({
    key:        feedKeySeq,
    multiplier,
    netChange,
    color,
    time:       new Date().toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
  })
  if (plinkoFeed.value.length > 60) plinkoFeed.value.splice(60)
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

// ── Roulette ──────────────────────────────────────────────────────────────
const rouletteSpinning   = ref(false)
const rouletteResult     = ref<RouletteResult | null>(null)
const placedBets         = ref<RouletteBet[]>([])
const selectedChip       = ref(10)
const chipOptions        = [1, 5, 10, 25, 50, 100, 500]
const rouletteWheelAngle = ref(0)
const winningNumber      = ref<number | null>(null)

const totalPlacedBet = computed(() => placedBets.value.reduce((s, b) => s + b.amount, 0))

const ROULETTE_RED = new Set([1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36])

function rouletteNumberColor(n: number): 'red' | 'black' | 'green' {
  if (n === 0) return 'green'
  return ROULETTE_RED.has(n) ? 'red' : 'black'
}

const splitHorizontal = computed(() =>
  Array.from({length: 35}, (_, i) => i + 1).filter(n => n % 3 !== 0))

const splitVertical = computed(() =>
  Array.from({length: 33}, (_, i) => i + 1))

const cornerBets = computed(() => {
  const r: number[][] = []
  for (let n = 1; n <= 33; n++) {
    if ((n - 1) % 3 === 2) continue
    r.push([n, n + 1, n + 3, n + 4])
  }
  return r
})

function betKey(type: string, numbers: number[]): string {
  return type + ':' + [...numbers].sort((a, b) => a - b).join(',')
}

function betAmountFor(type: string, numbers: number[]): number | null {
  const key = betKey(type, numbers)
  return placedBets.value.find(b => betKey(b.type, b.numbers) === key)?.amount ?? null
}

function addBet(type: string, numbers: number[]) {
  if (rouletteSpinning.value) return
  if (totalPlacedBet.value + selectedChip.value > 10000) {
    toast.error('Maximaleinsatz 10.000 € erreicht')
    return
  }
  const sorted = [...numbers].sort((a, b) => a - b)
  const key = betKey(type, sorted)
  const existing = placedBets.value.find(b => betKey(b.type, b.numbers) === key)
  if (existing) {
    existing.amount += selectedChip.value
  } else {
    placedBets.value.push({ type, numbers: sorted, amount: selectedChip.value })
  }
}

function removeBet(i: number) { placedBets.value.splice(i, 1) }
function clearBets() { placedBets.value = [] }

function rouletteBetLabel(bet: { type: string; numbers: number[] }): string {
  const map: Record<string, string> = {
    RED: 'Rot', BLACK: 'Schwarz', EVEN: 'Gerade', ODD: 'Ungerade',
    LOW: '1–18', HIGH: '19–36',
    DOZEN_1: 'Dutzend 1–12', DOZEN_2: 'Dutzend 13–24', DOZEN_3: 'Dutzend 25–36',
    COLUMN_1: '1. Kolonne', COLUMN_2: '2. Kolonne', COLUMN_3: '3. Kolonne',
  }
  if (map[bet.type]) return map[bet.type]
  const nums = bet.numbers.join('/')
  return ({
    STRAIGHT: `Plein ${nums}`,
    SPLIT:    `Cheval ${nums}`,
    STREET:   `Street ${nums}`,
    CORNER:   `Carré ${nums}`,
    SIX_LINE: `Sixain ${nums}`,
    TRIO:     `Trio ${nums}`,
  } as Record<string, string>)[bet.type] ?? bet.type
}

async function playRoulette() {
  if (!placedBets.value.length) return
  rouletteSpinning.value = true
  rouletteResult.value = null
  winningNumber.value = null

  let spinInterval = setInterval(() => { rouletteWheelAngle.value += 8 }, 20)

  try {
    const result = await api.post<RouletteResult>('/api/gambling/roulette', { bets: placedBets.value })

    await new Promise<void>(r => setTimeout(r, 1500))
    clearInterval(spinInterval)
    rouletteWheelAngle.value += 360 * 3
    winningNumber.value = result.winningNumber

    await new Promise<void>(r => setTimeout(r, 900))
    rouletteResult.value = result
    placedBets.value = []
    await gameStore.fetchCharacter()
  } catch (e: any) {
    clearInterval(spinInterval)
    const msg = e?.data?.message ?? e?.message ?? 'Fehler beim Roulette'
    toast.error(msg)
  } finally {
    rouletteSpinning.value = false
  }
}

// ── Labels & Helpers ───────────────────────────────────────────────────────
const handRankings = [
  'Highcard', 'Pärchen', 'Zwei Pärchen', 'Drilling', 'Straße',
  'Flush', 'Full House', 'Vierling', 'Straight Flush', 'Royal Flush',
]

const payoutTable = [
  { symbols: '7️⃣7️⃣7️⃣', label: 'JACKPOT', multiplier: '50×' },
  { symbols: '🔔🔔🔔', label: 'Großer Gewinn', multiplier: '10×' },
  { symbols: '🎰🎰🎰 / 🍒🍒🍒 / 🍋🍋🍋', label: 'Gewinn', multiplier: '5×' },
  { symbols: 'Zwei gleiche', label: 'Kleiner Gewinn', multiplier: '1,5×' },
  { symbols: 'Keine Übereinstimmung', label: 'Verlust', multiplier: '0×' },
]

function outcomeLabel(outcome: string): string {
  return ({ JACKPOT: 'JACKPOT!', BIG_WIN: 'Großer Gewinn!', WIN: 'Gewinn!', SMALL_WIN: 'Kleiner Gewinn!', LOSS: 'Verloren' } as Record<string, string>)[outcome] ?? outcome
}
function bjResultLabel(status: string): string {
  return ({ WON: 'Gewonnen!', LOST: 'Verloren', PUSH: 'Unentschieden' } as Record<string, string>)[status] ?? status
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
</script>

<style scoped>
/* ── Plinko feed ───────────────────────────────────────────────────────── */
.feed-scroll {
  max-height: 220px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(255,255,255,0.1) transparent;
}
.feed-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
  font-size: 0.72rem;
  transition: background 0.15s;
}
.feed-row:last-child { border-bottom: none; }
.feed-win  { background: rgba(34, 197, 94, 0.04); }
.feed-loss { background: rgba(239, 68, 68, 0.04); }
.feed-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.feed-mult {
  width: 36px;
  font-weight: 700;
  font-family: "JetBrains Mono", monospace;
  color: rgba(255,255,255,0.75);
  flex-shrink: 0;
}
.feed-payout {
  flex: 1;
  font-weight: 700;
  font-family: "JetBrains Mono", monospace;
}
.feed-time {
  color: rgba(255,255,255,0.25);
  font-size: 0.65rem;
  flex-shrink: 0;
}

@keyframes reelSpin {
  0%   { transform: translateY(-60%) scale(0.8); opacity: 0.4; }
  50%  { transform: translateY(0)    scale(1.1); opacity: 1; }
  100% { transform: translateY(60%)  scale(0.8); opacity: 0.4; }
}
.reel-spin {
  display: inline-block;
  animation: reelSpin 0.16s ease-in-out infinite;
}

/* ── Roulette ──────────────────────────────────────────────────────────── */
.roulette-table {
  display: grid;
  grid-template-columns: 48px repeat(3, 1fr) 52px;
  grid-template-rows: repeat(12, 40px) 36px 36px;
  gap: 2px;
}

.roulette-cell {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  border: 1px solid rgb(var(--color-surface-600, 75 85 99) / 0.5);
  font-size: 0.8rem;
  font-weight: 600;
  color: white;
  transition: filter 0.1s, transform 0.1s;
}
.roulette-cell:hover  { filter: brightness(1.25); }
.roulette-cell:active { transform: scale(0.93); }

.cell-red    { background: rgb(185 28 28 / 0.75); border-color: rgb(220 38 38 / 0.4); }
.cell-black  { background: rgb(30 30 35 / 0.9);   border-color: rgb(75 85 99 / 0.5); }
.zero-cell   { background: rgb(22 101 52 / 0.85); border-color: rgb(34 197 94 / 0.4); grid-column: 1; grid-row: 1 / 13; }
.column-cell { grid-column: 5; background: rgb(30 30 35 / 0.6); }
.outside-cell { background: rgb(30 30 35 / 0.6); font-size: 0.7rem; color: rgb(209 213 219); }

.has-bet {
  outline: 2px solid var(--accent, #d4321f);
  outline-offset: -2px;
}

@keyframes winnerPulse {
  0%,100% { box-shadow: 0 0 0 0 rgba(250, 204, 21, 0.7); }
  50%      { box-shadow: 0 0 0 8px rgba(250, 204, 21, 0); }
}
.cell-winner {
  outline: 3px solid rgb(250 204 21);
  outline-offset: -2px;
  filter: brightness(1.5);
  animation: winnerPulse 0.7s ease-out 4;
}

.chip-indicator {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--accent, #d4321f);
  color: white;
  font-size: 9px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  box-shadow: 0 1px 4px rgba(0,0,0,0.4);
}
</style>
