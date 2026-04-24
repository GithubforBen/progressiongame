<template>
  <div class="space-y-6">
    <h2 class="text-xl font-bold text-white">Leben & Ausgaben</h2>

    <!-- KV Warning -->
    <div v-if="!hasKv" class="card border border-red-500/50 bg-red-500/10 flex items-start gap-3 p-4">
      <div class="text-red-400 text-xl mt-0.5">⚠</div>
      <div>
        <p class="text-red-400 font-semibold">Keine Krankenversicherung aktiv!</p>
        <p class="text-red-300/80 text-sm mt-1">
          Du bist unversichert. Bei jedem Monatsabschluss besteht eine 10%-Chance auf eine Arztrechnung
          von 200 – 2.000 €. Füge eine Krankenversicherung unter „Ausgabe hinzufügen" hinzu.
        </p>
      </div>
    </div>
    <div v-else class="card border border-green-500/30 bg-green-500/5 flex items-center gap-3 p-4">
      <div class="text-green-400 text-lg">✓</div>
      <p class="text-green-400 text-sm">Krankenversicherung aktiv — du bist gegen Arztrechnungen geschützt.</p>
    </div>

    <!-- Steuervorschau -->
    <div class="card">
      <h3 class="text-base font-semibold text-white mb-4">Steuervorschau</h3>
      <div v-if="taxLoading" class="text-gray-500 text-sm">Lade...</div>
      <div v-else-if="tax" class="space-y-3">
        <div class="grid grid-cols-2 gap-3 text-sm">
          <div class="bg-white/5 rounded-lg p-3">
            <p class="text-gray-400 text-xs mb-1">Bruttogehalt / Monat</p>
            <p class="text-white font-semibold">{{ formatCurrency(tax.grossIncome) }}</p>
          </div>
          <div class="bg-white/5 rounded-lg p-3">
            <p class="text-gray-400 text-xs mb-1">Steuer</p>
            <p class="text-red-400 font-semibold">− {{ formatCurrency(tax.taxAmount) }}</p>
          </div>
          <div class="bg-white/5 rounded-lg p-3">
            <p class="text-gray-400 text-xs mb-1">Nettogehalt</p>
            <p class="text-green-400 font-semibold">{{ formatCurrency(tax.netIncome) }}</p>
          </div>
          <div class="bg-white/5 rounded-lg p-3">
            <p class="text-gray-400 text-xs mb-1">Steuersatz (Spitzenbracket)</p>
            <p class="text-yellow-400 font-semibold">{{ tax.bracketPercent }} %</p>
          </div>
        </div>
        <p class="text-gray-500 text-xs">Steuerklasse: {{ tax.bracket }}</p>
        <div class="mt-2 text-xs text-gray-500 space-y-0.5">
          <p class="font-medium text-gray-400 mb-1">Progressive Steuerbrackets:</p>
          <p :class="tax.grossIncome <= 1000 ? 'text-yellow-300' : ''">0 – 1.000 € → 0 %</p>
          <p :class="tax.grossIncome > 1000 && tax.grossIncome <= 3000 ? 'text-yellow-300' : ''">1.001 – 3.000 € → 20 %</p>
          <p :class="tax.grossIncome > 3000 && tax.grossIncome <= 6000 ? 'text-yellow-300' : ''">3.001 – 6.000 € → 32 %</p>
          <p :class="tax.grossIncome > 6000 ? 'text-yellow-300' : ''">Über 6.000 € → 42 %</p>
        </div>
      </div>
    </div>

    <!-- Ausgaben-Liste -->
    <div class="card">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-base font-semibold text-white">Monatliche Ausgaben</h3>
        <p class="text-sm text-gray-400">
          Gesamt: <span class="text-white font-semibold">{{ formatCurrency(totalActiveExpenses) }}</span> / Monat
        </p>
      </div>

      <div v-if="expensesLoading" class="text-gray-500 text-sm">Lade...</div>
      <div v-else-if="expenses.length === 0" class="text-gray-500 text-sm">Keine Ausgaben vorhanden.</div>
      <div v-else class="space-y-2">
        <div
          v-for="expense in expenses"
          :key="expense.id"
          class="flex items-center gap-3 p-3 rounded-lg"
          :class="expense.active ? 'bg-white/5' : 'bg-white/2 opacity-50'"
        >
          <!-- Category icon -->
          <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm flex-shrink-0"
               :class="categoryStyle(expense.category)">
            {{ categoryIcon(expense.category) }}
          </div>
          <!-- Info -->
          <div class="flex-1 min-w-0">
            <p class="text-white text-sm font-medium truncate">{{ expense.label }}</p>
            <p class="text-gray-400 text-xs">{{ categoryLabel(expense.category) }}</p>
          </div>
          <!-- Amount -->
          <p class="text-white font-semibold text-sm flex-shrink-0">
            {{ formatCurrency(expense.amount) }}
          </p>
          <!-- Mandatory badge -->
          <span v-if="expense.mandatory" class="badge text-xs px-2 py-0.5 bg-gray-700 text-gray-400 rounded flex-shrink-0">
            Pflicht
          </span>
          <!-- Toggle -->
          <button
            v-if="!expense.mandatory"
            @click="toggleExpense(expense)"
            class="text-xs px-2 py-1 rounded flex-shrink-0 transition-colors"
            :class="expense.active
              ? 'bg-yellow-500/20 text-yellow-400 hover:bg-yellow-500/30'
              : 'bg-green-500/20 text-green-400 hover:bg-green-500/30'"
          >
            {{ expense.active ? 'Deaktivieren' : 'Aktivieren' }}
          </button>
          <!-- Delete -->
          <button
            v-if="!expense.mandatory"
            @click="deleteExpense(expense)"
            class="text-xs px-2 py-1 rounded bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors flex-shrink-0"
          >
            ✕
          </button>
        </div>
      </div>
    </div>

    <!-- Versicherungs-Katalog -->
    <div class="card">
      <h3 class="text-base font-semibold text-white mb-1">Versicherungen</h3>
      <p class="text-xs text-gray-500 mb-4">Wähle deinen Versicherungsschutz. Tiers sind Upgrades – nur eine Krankenkasse kann aktiv sein.</p>

      <!-- Krankenversicherung Tiers -->
      <div class="mb-5">
        <h4 class="text-sm font-semibold text-gray-300 mb-3">🏥 Krankenversicherung</h4>
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div
            v-for="tier in kvTiers"
            :key="tier.label"
            class="rounded-lg border p-4 flex flex-col gap-2"
            :class="activeKvLabel === tier.label
              ? 'border-green-500/50 bg-green-500/8'
              : 'border-white/10 bg-white/3'"
          >
            <div class="flex items-center justify-between">
              <span class="text-white font-medium text-sm">{{ tier.label }}</span>
              <span v-if="activeKvLabel === tier.label" class="text-xs px-1.5 py-0.5 rounded bg-green-500/20 text-green-400">Aktiv</span>
            </div>
            <p class="text-yellow-400 font-mono text-sm">{{ formatCurrency(tier.amount) }}/Monat</p>
            <ul class="space-y-1">
              <li v-for="effect in tier.effects" :key="effect" class="text-xs text-green-400">✓ {{ effect }}</li>
            </ul>
            <button
              v-if="activeKvLabel !== tier.label"
              class="btn-primary text-xs py-1.5 mt-auto"
              :disabled="catalogLoading"
              @click="selectKvTier(tier)"
            >
              {{ activeKvLabel ? 'Wechseln' : 'Hinzufügen' }}
            </button>
            <button
              v-else
              class="btn-secondary text-xs py-1.5 mt-auto bg-red-500/10 text-red-400 hover:bg-red-500/20 border-red-500/20"
              :disabled="catalogLoading"
              @click="removeKv()"
            >
              Kündigen
            </button>
          </div>
        </div>
      </div>

      <!-- Rechtsschutz -->
      <div>
        <h4 class="text-sm font-semibold text-gray-300 mb-3">⚖️ Rechtsschutzversicherung</h4>
        <div class="rounded-lg border p-4" :class="hasRechtsschutz ? 'border-blue-500/50 bg-blue-500/8' : rechtsschutzLocked ? 'border-white/5 bg-white/2 opacity-60' : 'border-white/10 bg-white/3'">
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-white font-medium text-sm">Rechtsschutz Premium</span>
                <span v-if="hasRechtsschutz" class="text-xs px-1.5 py-0.5 rounded bg-blue-500/20 text-blue-400">Aktiv</span>
                <span v-else-if="rechtsschutzLocked" class="text-xs px-1.5 py-0.5 rounded bg-gray-700 text-gray-500">🔒 Gesperrt</span>
              </div>
              <p class="text-yellow-400 font-mono text-sm mb-2">{{ formatCurrency(300) }}/Monat</p>
              <p class="text-xs text-green-400">✓ Steuerprüfungsrisiko −30%</p>
              <p v-if="rechtsschutzLocked" class="text-xs text-gray-500 mt-1">Benötigt: Schufa ≥ 600 (aktuell {{ gameStore.character?.schufaScore ?? '??' }})</p>
            </div>
            <div class="flex-shrink-0">
              <button
                v-if="!hasRechtsschutz && !rechtsschutzLocked"
                class="btn-primary text-xs py-1.5 px-3"
                :disabled="catalogLoading"
                @click="addRechtsschutz()"
              >Hinzufügen</button>
              <button
                v-else-if="hasRechtsschutz"
                class="btn-secondary text-xs py-1.5 px-3 bg-red-500/10 text-red-400 hover:bg-red-500/20 border-red-500/20"
                :disabled="catalogLoading"
                @click="removeRechtsschutz()"
              >Kündigen</button>
            </div>
          </div>
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
const { formatCurrency } = useFormatting()
const gameStore = useGameStore()

interface Expense {
  id: number
  category: string
  label: string
  amount: number
  active: boolean
  mandatory: boolean
}

interface TaxPreview {
  grossIncome: number
  taxAmount: number
  netIncome: number
  bracket: string
  bracketPercent: number
}

const expenses = ref<Expense[]>([])
const expensesLoading = ref(false)
const tax = ref<TaxPreview | null>(null)
const taxLoading = ref(false)
const catalogLoading = ref(false)

const kvTiers = [
  {
    label: 'Krankenkasse Basis',
    amount: 150,
    effects: ['Kein Arztrechnung-Risiko'],
  },
  {
    label: 'Krankenkasse Standard',
    amount: 250,
    effects: ['Kein Arztrechnung-Risiko', '−5 Stress pro Monat'],
  },
  {
    label: 'Krankenkasse Premium',
    amount: 400,
    effects: ['Kein Arztrechnung-Risiko', '−10 Stress pro Monat'],
  },
]

const hasKv = computed(() =>
  expenses.value.some(e => e.category === 'KRANKENVERSICHERUNG' && e.active)
)

const activeKvLabel = computed(() => {
  const kv = expenses.value.find(e => e.category === 'KRANKENVERSICHERUNG' && e.active)
  return kv?.label ?? null
})

const hasRechtsschutz = computed(() =>
  expenses.value.some(e => e.category === 'RECHTSSCHUTZ' && e.active)
)

const rechtsschutzLocked = computed(() =>
  (gameStore.character?.schufaScore ?? 0) < 600
)

const totalActiveExpenses = computed(() =>
  expenses.value
    .filter(e => e.active)
    .reduce((sum, e) => sum + e.amount, 0)
)

async function loadExpenses() {
  expensesLoading.value = true
  try {
    expenses.value = await api.get<Expense[]>('/api/expenses')
  } catch {
    toast.error('Ausgaben konnten nicht geladen werden')
  } finally {
    expensesLoading.value = false
  }
}

async function loadTax() {
  taxLoading.value = true
  try {
    tax.value = await api.get<TaxPreview>('/api/tax/preview')
  } catch {
    // no job = no tax, just ignore
  } finally {
    taxLoading.value = false
  }
}

async function toggleExpense(expense: Expense) {
  try {
    const updated = await api.patch<Expense>(`/api/expenses/${expense.id}/toggle`)
    const idx = expenses.value.findIndex(e => e.id === expense.id)
    if (idx !== -1) expenses.value[idx] = updated
    toast.success(updated.active ? 'Ausgabe aktiviert' : 'Ausgabe deaktiviert')
  } catch {
    toast.error('Fehler beim Umschalten der Ausgabe')
  }
}

async function deleteExpense(expense: Expense) {
  try {
    await api.del(`/api/expenses/${expense.id}`)
    expenses.value = expenses.value.filter(e => e.id !== expense.id)
    toast.success('Ausgabe gelöscht')
  } catch {
    toast.error('Ausgabe konnte nicht gelöscht werden')
  }
}

async function selectKvTier(tier: { label: string; amount: number }) {
  catalogLoading.value = true
  try {
    const existing = expenses.value.find(e => e.category === 'KRANKENVERSICHERUNG')
    if (existing) await api.del(`/api/expenses/${existing.id}`)
    const created = await api.post<Expense>('/api/expenses', {
      category: 'KRANKENVERSICHERUNG',
      label: tier.label,
      amount: tier.amount,
    })
    expenses.value = expenses.value.filter(e => e.category !== 'KRANKENVERSICHERUNG')
    expenses.value.push(created)
    toast.success(`${tier.label} aktiviert`)
  } catch (e: any) {
    toast.error((e as any)?.data?.message ?? 'Fehler beim Hinzufügen')
  } finally {
    catalogLoading.value = false
  }
}

async function removeKv() {
  const existing = expenses.value.find(e => e.category === 'KRANKENVERSICHERUNG')
  if (!existing) return
  catalogLoading.value = true
  try {
    await api.del(`/api/expenses/${existing.id}`)
    expenses.value = expenses.value.filter(e => e.id !== existing.id)
    toast.success('Krankenversicherung gekündigt')
  } catch {
    toast.error('Fehler beim Kündigen')
  } finally {
    catalogLoading.value = false
  }
}

async function addRechtsschutz() {
  catalogLoading.value = true
  try {
    const created = await api.post<Expense>('/api/expenses', {
      category: 'RECHTSSCHUTZ',
      label: 'Rechtsschutzversicherung',
      amount: 300,
    })
    expenses.value.push(created)
    toast.success('Rechtsschutzversicherung hinzugefügt')
  } catch (e: any) {
    toast.error((e as any)?.data?.message ?? 'Fehler')
  } finally {
    catalogLoading.value = false
  }
}

async function removeRechtsschutz() {
  const existing = expenses.value.find(e => e.category === 'RECHTSSCHUTZ')
  if (!existing) return
  catalogLoading.value = true
  try {
    await api.del(`/api/expenses/${existing.id}`)
    expenses.value = expenses.value.filter(e => e.id !== existing.id)
    toast.success('Rechtsschutz gekündigt')
  } catch {
    toast.error('Fehler beim Kündigen')
  } finally {
    catalogLoading.value = false
  }
}

function categoryIcon(cat: string): string {
  const map: Record<string, string> = {
    ESSEN: '🍔', WOHNEN: '🏠', KRANKENVERSICHERUNG: '🏥',
    GYM: '💪', STREAMING: '📺', MOBILFUNK: '📱',
    INTERNET: '🌐', ZEITSCHRIFTEN: '📰', SONSTIGES: '💼', RECHTSSCHUTZ: '⚖️',
  }
  return map[cat] ?? '💰'
}

function categoryLabel(cat: string): string {
  const map: Record<string, string> = {
    ESSEN: 'Ernährung', WOHNEN: 'Wohnen', KRANKENVERSICHERUNG: 'Krankenversicherung',
    GYM: 'Fitnessstudio', STREAMING: 'Streaming', MOBILFUNK: 'Mobilfunk',
    INTERNET: 'Internet', ZEITSCHRIFTEN: 'Abonnements', SONSTIGES: 'Sonstiges',
    RECHTSSCHUTZ: 'Rechtsschutz',
  }
  return map[cat] ?? cat
}

function categoryStyle(cat: string): string {
  const map: Record<string, string> = {
    KRANKENVERSICHERUNG: 'bg-green-500/20 text-green-400',
    GYM: 'bg-blue-500/20 text-blue-400',
    STREAMING: 'bg-purple-500/20 text-purple-400',
    MOBILFUNK: 'bg-cyan-500/20 text-cyan-400',
    INTERNET: 'bg-sky-500/20 text-sky-400',
    ESSEN: 'bg-yellow-500/20 text-yellow-400',
    WOHNEN: 'bg-orange-500/20 text-orange-400',
    RECHTSSCHUTZ: 'bg-blue-500/20 text-blue-400',
  }
  return map[cat] ?? 'bg-gray-500/20 text-gray-400'
}

onMounted(() => {
  loadExpenses()
  loadTax()
})
</script>
