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

    <!-- Neue Ausgabe hinzufügen -->
    <div class="card">
      <h3 class="text-base font-semibold text-white mb-4">Ausgabe hinzufügen</h3>
      <form @submit.prevent="addExpense" class="space-y-3">
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label class="block text-xs text-gray-400 mb-1">Kategorie</label>
            <select v-model="form.category" class="input w-full">
              <option value="">Wählen...</option>
              <option value="KRANKENVERSICHERUNG">Krankenversicherung</option>
              <option value="GYM">Fitnessstudio</option>
              <option value="STREAMING">Streaming</option>
              <option value="MOBILFUNK">Mobilfunk</option>
              <option value="INTERNET">Internet</option>
              <option value="ZEITSCHRIFTEN">Zeitschriften/Abos</option>
              <option value="SONSTIGES">Sonstiges</option>
            </select>
          </div>
          <div>
            <label class="block text-xs text-gray-400 mb-1">Bezeichnung</label>
            <input v-model="form.label" type="text" class="input w-full" placeholder="z.B. Netflix" />
          </div>
          <div>
            <label class="block text-xs text-gray-400 mb-1">Betrag (€ / Monat)</label>
            <input v-model.number="form.amount" type="number" min="1" step="0.01" class="input w-full" placeholder="z.B. 12.99" />
          </div>
        </div>
        <button type="submit" :disabled="addLoading" class="btn-primary w-full sm:w-auto">
          {{ addLoading ? 'Wird hinzugefügt...' : '+ Ausgabe hinzufügen' }}
        </button>
        <p v-if="addError" class="text-red-400 text-sm">{{ addError }}</p>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

definePageMeta({ layout: 'default' })

const api = useApi()
const toast = useToastStore()
const { formatCurrency } = useFormatting()

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
const addLoading = ref(false)
const addError = ref('')

const form = ref({ category: '', label: '', amount: 0 })

const hasKv = computed(() =>
  expenses.value.some(e => e.category === 'KRANKENVERSICHERUNG' && e.active)
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

async function addExpense() {
  addError.value = ''
  if (!form.value.category) { addError.value = 'Bitte Kategorie wählen.'; return }
  if (!form.value.label.trim()) { addError.value = 'Bezeichnung darf nicht leer sein.'; return }
  if (!form.value.amount || form.value.amount <= 0) { addError.value = 'Betrag muss größer als 0 sein.'; return }

  addLoading.value = true
  try {
    const created = await api.post<Expense>('/api/expenses', {
      category: form.value.category,
      label: form.value.label.trim(),
      amount: form.value.amount,
    })
    expenses.value.push(created)
    form.value = { category: '', label: '', amount: 0 }
    toast.success('Ausgabe hinzugefügt')
  } catch (e: any) {
    addError.value = e?.data?.message ?? 'Fehler beim Hinzufügen'
  } finally {
    addLoading.value = false
  }
}

function categoryIcon(cat: string): string {
  const map: Record<string, string> = {
    ESSEN: '🍔', WOHNEN: '🏠', KRANKENVERSICHERUNG: '🏥',
    GYM: '💪', STREAMING: '📺', MOBILFUNK: '📱',
    INTERNET: '🌐', ZEITSCHRIFTEN: '📰', SONSTIGES: '💼',
  }
  return map[cat] ?? '💰'
}

function categoryLabel(cat: string): string {
  const map: Record<string, string> = {
    ESSEN: 'Ernährung', WOHNEN: 'Wohnen', KRANKENVERSICHERUNG: 'Krankenversicherung',
    GYM: 'Fitnessstudio', STREAMING: 'Streaming', MOBILFUNK: 'Mobilfunk',
    INTERNET: 'Internet', ZEITSCHRIFTEN: 'Abonnements', SONSTIGES: 'Sonstiges',
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
  }
  return map[cat] ?? 'bg-gray-500/20 text-gray-400'
}

onMounted(() => {
  loadExpenses()
  loadTax()
})
</script>
