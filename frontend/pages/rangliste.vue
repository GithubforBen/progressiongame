<template>
  <div class="space-y-6">
    <h2 class="text-xl font-bold text-white">Rangliste</h2>

    <!-- Sort toggle -->
    <div class="flex gap-1">
      <button
        class="px-3 py-1.5 rounded text-xs font-medium transition-colors"
        :class="sortBy === 'income' ? 'bg-accent text-white' : 'bg-surface-700 text-gray-400 hover:text-white'"
        @click="setSortBy('income')"
      >Monatseinkommen</button>
      <button
        class="px-3 py-1.5 rounded text-xs font-medium transition-colors"
        :class="sortBy === 'netWorth' ? 'bg-accent text-white' : 'bg-surface-700 text-gray-400 hover:text-white'"
        @click="setSortBy('netWorth')"
      >Nettovermögen</button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="card flex items-center justify-center h-32 text-gray-500 text-sm">
      Lade Rangliste…
    </div>

    <template v-else>
      <!-- My rank highlight -->
      <div v-if="myEntry" class="card border border-accent/30 bg-accent/5">
        <p class="text-xs text-gray-400 mb-1">Deine Platzierung</p>
        <div class="flex items-center gap-4">
          <span class="text-3xl font-bold text-accent">#{{ myEntry.rank }}</span>
          <div>
            <p class="text-white font-semibold">{{ myEntry.username }}</p>
            <p class="text-xs text-gray-400">
              {{ formatCurrency(myEntry.netWorth) }} Nettovermögen
              · <span class="text-blue-300">{{ formatCurrency(myEntry.monthlyIncome) }}/Monat</span>
              · Monat {{ myEntry.currentTurn }}
            </p>
          </div>
        </div>
      </div>

      <!-- Table -->
      <div class="card overflow-hidden p-0">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-white/5 text-xs text-gray-400 uppercase tracking-wider">
              <th class="text-left px-4 py-3 w-12">#</th>
              <th class="text-left px-4 py-3">Spieler</th>
              <th class="text-right px-4 py-3">Nettovermögen</th>
              <th class="text-right px-4 py-3 hidden sm:table-cell">Einkommen/Monat</th>
              <th class="text-right px-4 py-3 hidden md:table-cell">Sammlungen</th>
              <th class="text-right px-4 py-3 hidden sm:table-cell">Monat</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="entry in entries" :key="entry.playerId">
              <tr
                class="border-b border-white/5 transition-colors cursor-pointer select-none"
                :class="[
                  entry.isMe ? 'bg-accent/10' : 'hover:bg-white/3',
                  expandedPlayerId === entry.playerId ? 'bg-white/5' : ''
                ]"
                @click="togglePlayerDetail(entry)"
              >
                <!-- Rank -->
                <td class="px-4 py-3 font-bold" :class="rankColor(entry.rank)">
                  {{ entry.rank <= 3 ? rankEmoji(entry.rank) : entry.rank }}
                </td>
                <!-- Username -->
                <td class="px-4 py-3">
                  <span class="text-white font-medium">{{ entry.username }}</span>
                  <span v-if="entry.isMe" class="ml-2 text-xs text-accent">(du)</span>
                </td>
                <!-- Net worth -->
                <td class="px-4 py-3 text-right font-semibold"
                    :class="entry.netWorth >= 0 ? 'text-green-400' : 'text-red-400'">
                  {{ formatCurrency(entry.netWorth) }}
                </td>
                <!-- Monthly income -->
                <td class="px-4 py-3 text-right text-blue-300 hidden sm:table-cell">
                  {{ entry.monthlyIncome != null ? formatCurrency(entry.monthlyIncome) : '—' }}
                </td>
                <!-- Completed collections -->
                <td class="px-4 py-3 text-right text-gray-400 hidden md:table-cell">
                  {{ entry.completedCollections }} ✓
                </td>
                <!-- Turn -->
                <td class="px-4 py-3 text-right text-gray-400 hidden sm:table-cell">
                  {{ entry.currentTurn }}
                </td>
              </tr>

              <!-- Expanded collection detail row -->
              <tr v-if="expandedPlayerId === entry.playerId" class="border-b border-white/5">
                <td colspan="6" class="px-4 py-4 bg-surface-800/60">
                  <div v-if="collectionsLoading" class="text-gray-500 text-xs">Lade Sammlungen…</div>
                  <div v-else-if="expandedCollections.length === 0" class="text-gray-600 text-xs">
                    Noch keine Sammlungen.
                  </div>
                  <div v-else>
                    <p class="text-xs text-gray-500 mb-2 font-semibold uppercase tracking-wider">
                      Sammlungen von {{ entry.username }}
                    </p>
                    <div class="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-2">
                      <div
                        v-for="col in expandedCollections"
                        :key="col.name"
                        class="rounded p-2 bg-surface-700/50 border"
                        :class="col.completed ? 'border-green-500/30' : 'border-white/5'"
                      >
                        <p class="text-xs font-medium truncate" :class="col.completed ? 'text-green-400' : 'text-gray-300'">
                          {{ col.displayName }}<span v-if="col.completed"> ✓</span>
                        </p>
                        <div class="mt-1 h-1 bg-surface-600 rounded-full overflow-hidden">
                          <div
                            class="h-full rounded-full"
                            :class="col.completed ? 'bg-green-500' : 'bg-accent'"
                            :style="{ width: `${col.itemCount > 0 ? Math.round(col.ownedCount / col.itemCount * 100) : 0}%` }"
                          />
                        </div>
                        <p class="text-xs text-gray-500 mt-0.5">{{ col.ownedCount }}/{{ col.itemCount }}</p>
                      </div>
                    </div>
                  </div>
                </td>
              </tr>
            </template>

            <tr v-if="entries.length === 0">
              <td colspan="6" class="px-4 py-8 text-center text-gray-500">
                Noch keine Spieler registriert.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'default' })

const api = useApi()
const { formatCurrency } = useFormatting()

interface LeaderboardEntry {
  rank: number
  playerId: number
  username: string
  netWorth: number
  monthlyIncome: number | null
  completedCollections: number
  currentTurn: number
  isMe: boolean
}

interface PublicCollection {
  name: string
  displayName: string
  itemCount: number
  ownedCount: number
  completed: boolean
}

const entries = ref<LeaderboardEntry[]>([])
const loading = ref(true)
const sortBy = ref<'income' | 'netWorth'>('income')

const expandedPlayerId = ref<number | null>(null)
const expandedCollections = ref<PublicCollection[]>([])
const collectionsLoading = ref(false)

const myEntry = computed(() => entries.value.find(e => e.isMe) ?? null)

function rankColor(rank: number) {
  if (rank === 1) return 'text-yellow-400'
  if (rank === 2) return 'text-gray-300'
  if (rank === 3) return 'text-orange-400'
  return 'text-gray-500'
}

function rankEmoji(rank: number) {
  return ['🥇', '🥈', '🥉'][rank - 1]
}

async function fetchLeaderboard() {
  loading.value = true
  try {
    entries.value = await api.get<LeaderboardEntry[]>(`/api/leaderboard?sort=${sortBy.value}`)
  } finally {
    loading.value = false
  }
}

function setSortBy(s: 'income' | 'netWorth') {
  sortBy.value = s
}

async function togglePlayerDetail(entry: LeaderboardEntry) {
  if (expandedPlayerId.value === entry.playerId) {
    expandedPlayerId.value = null
    return
  }
  expandedPlayerId.value = entry.playerId
  collectionsLoading.value = true
  try {
    expandedCollections.value = await api.get<PublicCollection[]>(
      `/api/leaderboard/player/${entry.playerId}/collections`
    )
  } finally {
    collectionsLoading.value = false
  }
}

watch(sortBy, fetchLeaderboard)

onMounted(fetchLeaderboard)
</script>
