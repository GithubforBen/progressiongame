<template>
  <div class="space-y-6">
    <h2 class="text-xl font-bold text-white">Kredite & SCHUFA</h2>

    <!-- SCHUFA Score -->
    <div class="card">
      <h3 class="text-base font-semibold text-white mb-4">SCHUFA-Score</h3>
      <div v-if="!schufa" class="text-gray-500 text-sm">Lade...</div>
      <div v-else>
        <div class="flex items-end gap-4 mb-3">
          <div>
            <p class="text-4xl font-bold" :class="schufaInfo.color">{{ schufa.score }}</p>
            <p class="text-gray-400 text-xs mt-0.5">von 1000</p>
          </div>
          <div class="mb-1">
            <span
              class="text-sm font-semibold px-3 py-1 rounded-full"
              :class="schufaInfo.color + ' ' + schufaInfo.color.replace('text-', 'bg-').replace('400', '400/15')"
            >
              {{ schufaInfo.label }}
            </span>
          </div>
        </div>
        <!-- Score bar -->
        <div class="w-full h-3 rounded-full bg-white/10 overflow-hidden">
          <div
            class="h-full rounded-full transition-all duration-700"
            :class="schufaInfo.bgColor"
            :style="{ width: (schufa.score / 10) + '%' }"
          />
        </div>
        <!-- Interest rate info -->
        <div class="mt-4 grid grid-cols-2 sm:grid-cols-4 gap-2">
          <div
            v-for="tier in scoreTiers"
            :key="tier.label"
            class="rounded-lg p-3 text-center"
            :class="isCurrentTier(tier) ? 'bg-accent/20 border border-accent/40' : 'bg-white/5'"
          >
            <p class="text-xs font-medium" :class="tier.color">{{ tier.range }}</p>
            <p class="text-white font-bold text-sm mt-0.5">{{ tier.rate }}</p>
            <p class="text-gray-500 text-xs">{{ tier.label }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- SCHUFA Breakdown -->
    <div class="card" v-if="breakdown">
      <h3 class="text-base font-semibold text-white mb-4">So setzt sich dein Score zusammen</h3>
      <div class="space-y-2">
        <div
          v-for="f in breakdown.factors"
          :key="f.label"
          class="flex items-center gap-3"
        >
          <div class="flex-1 min-w-0">
            <p class="text-sm text-gray-300">{{ f.label }}</p>
            <p class="text-xs text-gray-500">{{ f.detail }}</p>
          </div>
          <span
            class="text-sm font-mono font-semibold flex-shrink-0"
            :class="f.impact > 0 ? 'text-green-400' : f.impact < 0 ? 'text-red-400' : 'text-gray-400'"
          >
            {{ f.impact > 0 ? '+' : '' }}{{ f.impact }}
          </span>
        </div>
      </div>
    </div>

    <!-- Active Loans -->
    <div class="card">
      <h3 class="text-base font-semibold text-white mb-4">Aktive Kredite</h3>
      <div v-if="loans.filter(l => l.status === 'ACTIVE').length === 0" class="text-gray-500 text-sm">
        Keine aktiven Kredite.
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="loan in loans.filter(l => l.status === 'ACTIVE')"
          :key="loan.id"
          class="p-4 rounded-lg bg-white/5 border border-white/10 transition-colors"
          :class="payingOff === loan.id ? 'border-green-500/30 bg-green-500/5' : ''"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="flex-1">
              <p class="text-white font-semibold">{{ loan.label }}</p>
              <p class="text-gray-400 text-xs mt-0.5">
                Zinssatz: {{ formatLoanRate(loan.interestRate) }} · Seit Monat {{ loan.takenAtTurn }}
              </p>
            </div>
            <div class="text-right flex-shrink-0">
              <p class="text-red-400 font-bold text-sm">{{ formatCurrency(loan.amountRemaining) }}</p>
              <p class="text-gray-500 text-xs">Restschuld</p>
            </div>
          </div>
          <div class="mt-3 space-y-1">
            <div class="flex justify-between text-xs text-gray-400">
              <span>Monatsrate: <span class="text-white">{{ formatCurrency(loan.monthlyPayment) }}</span></span>
              <span>Noch <span class="text-white">{{ loan.turnsRemaining }}</span> Monate</span>
            </div>
            <!-- Progress bar -->
            <div class="w-full h-1.5 rounded-full bg-white/10 overflow-hidden">
              <div
                class="h-full rounded-full bg-red-400/70"
                :style="{ width: (loan.amountRemaining / loan.amountBorrowed * 100) + '%' }"
              />
            </div>
          </div>

          <!-- Sofort tilgen -->
          <div class="mt-3 pt-3 border-t border-white/8">
            <div v-if="canAfford(loan)" class="flex items-center gap-3">
              <button
                class="flex-1 py-2 px-3 rounded-lg text-sm font-semibold transition-colors border"
                :class="payingOff === loan.id
                  ? 'bg-green-500/20 border-green-500/40 text-green-300 cursor-wait'
                  : 'bg-white/5 border-white/15 text-white hover:bg-green-500/15 hover:border-green-500/40 hover:text-green-300'"
                :disabled="payingOff !== null"
                @click="payOff(loan)"
              >
                <span v-if="payingOff === loan.id">Wird getilgt...</span>
                <span v-else>Sofort tilgen — {{ formatCurrency(loan.amountRemaining) }}</span>
              </button>
              <span class="text-xs text-green-400/70">+5 SCHUFA</span>
            </div>
            <div v-else class="flex items-center gap-2 text-xs text-gray-500">
              <span class="text-amber-500/70">⚠</span>
              Nicht genug Guthaben (fehlen {{ formatCurrency(loan.amountRemaining - (gameStore.character?.cash ?? 0)) }})
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Loan Form -->
    <div class="card">
      <h3 class="text-base font-semibold text-white mb-4">Kredit aufnehmen</h3>
      <div v-if="!schufa" class="text-gray-500 text-sm">Lade...</div>
      <div v-else-if="schufa.score < 300" class="p-3 rounded-lg bg-red-500/10 border border-red-500/30">
        <p class="text-red-400 text-sm font-medium">Kredit abgelehnt</p>
        <p class="text-red-300/70 text-xs mt-0.5">
          Dein SCHUFA-Score ({{ schufa.score }}) ist zu niedrig. Mindestens 300 erforderlich.
        </p>
      </div>
      <div v-else class="space-y-4">
        <div>
          <label class="block text-xs text-gray-400 mb-1">Bezeichnung</label>
          <input
            v-model="form.label"
            type="text"
            placeholder="z.B. Autokredit"
            class="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-accent/50"
          />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs text-gray-400 mb-1">Betrag (€)</label>
            <input
              v-model.number="form.amount"
              type="number"
              min="1000"
              step="1000"
              placeholder="10000"
              class="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-accent/50"
            />
          </div>
          <div>
            <label class="block text-xs text-gray-400 mb-1">Laufzeit (Monate)</label>
            <input
              v-model.number="form.termMonths"
              type="number"
              min="6"
              max="360"
              step="6"
              placeholder="24"
              class="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-accent/50"
            />
          </div>
        </div>

        <!-- Preview -->
        <div v-if="form.amount >= 1000 && form.termMonths >= 6" class="p-3 rounded-lg bg-white/5 text-sm space-y-1">
          <div class="flex justify-between">
            <span class="text-gray-400">Zinssatz</span>
            <span class="text-white">{{ formatLoanRate(schufa.interestRate) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">Monatliche Rate</span>
            <span class="text-white font-semibold">{{ formatCurrency(previewRate) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">Gesamtkosten</span>
            <span class="text-yellow-400">{{ formatCurrency(previewRate * form.termMonths) }}</span>
          </div>
        </div>

        <button
          @click="takeLoan"
          :disabled="submitting || form.amount < 1000 || form.termMonths < 6"
          class="btn-primary w-full"
        >
          {{ submitting ? 'Wird beantragt...' : 'Kredit beantragen' }}
        </button>
        <p class="text-xs text-gray-500">SCHUFA-Score sinkt bei jedem neuen Kredit um 20 Punkte.</p>
      </div>
    </div>

    <!-- Closed Loans -->
    <div v-if="loans.filter(l => l.status !== 'ACTIVE').length > 0" class="card">
      <h3 class="text-base font-semibold text-white mb-4">Abgeschlossene Kredite</h3>
      <div class="space-y-2">
        <div
          v-for="loan in loans.filter(l => l.status !== 'ACTIVE')"
          :key="loan.id"
          class="flex items-center justify-between p-3 rounded-lg bg-white/3 opacity-70"
        >
          <div>
            <p class="text-white text-sm">{{ loan.label }}</p>
            <p class="text-gray-500 text-xs">{{ formatCurrency(loan.amountBorrowed) }} · Seit Monat {{ loan.takenAtTurn }}</p>
          </div>
          <span
            class="text-xs px-2 py-0.5 rounded font-medium"
            :class="loan.status === 'PAID_OFF'
              ? 'bg-green-500/20 text-green-400'
              : 'bg-red-500/20 text-red-400'"
          >
            {{ loan.status === 'PAID_OFF' ? 'Abbezahlt' : 'Ausgefallen' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

definePageMeta({ layout: 'default' })

const api = useApi()
const toast = useToastStore()
const gameStore = useGameStore()
const { formatCurrency, formatSchufaScore, formatLoanRate } = useFormatting()

interface Schufa { score: number; label: string; interestRate: number }
interface SchufaFactor { label: string; impact: number; detail: string }
interface SchufaBreakdown { score: number; factors: SchufaFactor[] }
interface Loan {
  id: number; label: string; amountBorrowed: number; amountRemaining: number
  interestRate: number; monthlyPayment: number; turnsRemaining: number
  takenAtTurn: number; status: string
}

const schufa = ref<Schufa | null>(null)
const breakdown = ref<SchufaBreakdown | null>(null)
const loans = ref<Loan[]>([])
const submitting = ref(false)
const payingOff = ref<number | null>(null)

const form = ref({ label: '', amount: 10000, termMonths: 24 })

const schufaInfo = computed(() => schufa.value
  ? formatSchufaScore(schufa.value.score)
  : { label: '', color: 'text-gray-400', bgColor: 'bg-gray-400' })

const scoreTiers = [
  { range: '800–1000', rate: '3,00 % p.a.', label: 'Ausgezeichnet', color: 'text-green-400', minScore: 800 },
  { range: '600–799', rate: '5,00 % p.a.', label: 'Gut', color: 'text-blue-400', minScore: 600 },
  { range: '400–599', rate: '8,00 % p.a.', label: 'Befriedigend', color: 'text-yellow-400', minScore: 400 },
  { range: '0–399', rate: '12,00 % p.a.', label: 'Mangelhaft', color: 'text-red-400', minScore: 0 },
]

function isCurrentTier(tier: typeof scoreTiers[0]): boolean {
  if (!schufa.value) return false
  const s = schufa.value.score
  const tierIdx = scoreTiers.indexOf(tier)
  const nextTier = scoreTiers[tierIdx - 1]
  const upper = nextTier ? nextTier.minScore - 1 : 1000
  return s >= tier.minScore && s <= upper
}

const previewRate = computed(() => {
  if (!schufa.value || form.value.amount < 1000 || form.value.termMonths < 6) return 0
  const r = schufa.value.interestRate / 12
  if (r === 0) return form.value.amount / form.value.termMonths
  const n = form.value.termMonths
  const rn = Math.pow(1 + r, n)
  return Math.round(form.value.amount * (r * rn) / (rn - 1) * 100) / 100
})

async function loadAll() {
  try {
    const [s, l, b] = await Promise.all([
      api.get<Schufa>('/api/loans/schufa'),
      api.get<Loan[]>('/api/loans'),
      api.get<SchufaBreakdown>('/api/loans/schufa-breakdown'),
    ])
    schufa.value = s
    loans.value = Array.isArray(l) ? l : []
    breakdown.value = b
  } catch {
    toast.error('Daten konnten nicht geladen werden')
  }
}

function canAfford(loan: Loan): boolean {
  return (gameStore.character?.cash ?? 0) >= loan.amountRemaining
}

async function payOff(loan: Loan) {
  if (payingOff.value !== null) return
  payingOff.value = loan.id
  try {
    const updated = await api.post<Loan>(`/api/loans/${loan.id}/payoff`, {})
    loans.value = loans.value.map(l => l.id === updated.id ? updated : l)
    toast.success(`${loan.label} vollständig getilgt! SCHUFA +5`)
    await Promise.all([gameStore.fetchCharacter(), loadAll()])
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Tilgung fehlgeschlagen')
  } finally {
    payingOff.value = null
  }
}

async function takeLoan() {
  if (!form.value.amount || form.value.amount < 1000) {
    toast.error('Mindestbetrag: 1.000 €')
    return
  }
  submitting.value = true
  try {
    const loan = await api.post<Loan>('/api/loans/take', {
      label: form.value.label || 'Kredit',
      amount: form.value.amount,
      termMonths: form.value.termMonths,
    })
    loans.value.unshift(loan)
    form.value = { label: '', amount: 10000, termMonths: 24 }
    toast.success(`Kredit über ${formatCurrency(loan.amountBorrowed)} genehmigt!`)
    await Promise.all([
      gameStore.fetchCharacter(),
      loadAll(), // Refresh SCHUFA score
    ])
  } catch (e: any) {
    toast.error(e?.data?.message ?? 'Kredit abgelehnt')
  } finally {
    submitting.value = false
  }
}

onMounted(loadAll)
</script>
