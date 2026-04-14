<template>
  <div class="space-y-6">
    <h2 class="text-xl font-bold text-white">Rangliste</h2>

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
              {{ formatCurrency(myEntry.netWorth) }} Nettovermögen · Monat {{ myEntry.currentTurn }}
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
              <th class="text-right px-4 py-3 hidden sm:table-cell">Monat</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="entry in entries"
              :key="entry.playerId"
              class="border-b border-white/5 last:border-0 transition-colors"
              :class="entry.isMe ? 'bg-accent/10' : 'hover:bg-white/3'"
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
              <!-- Turn -->
              <td class="px-4 py-3 text-right text-gray-400 hidden sm:table-cell">
                {{ entry.currentTurn }}
              </td>
            </tr>
            <tr v-if="entries.length === 0">
              <td colspan="4" class="px-4 py-8 text-center text-gray-500">
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
  currentTurn: number
  isMe: boolean
}

const entries = ref<LeaderboardEntry[]>([])
const loading = ref(true)

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

onMounted(async () => {
  try {
    entries.value = await api.get<LeaderboardEntry[]>('/api/leaderboard')
  } finally {
    loading.value = false
  }
})
</script>
