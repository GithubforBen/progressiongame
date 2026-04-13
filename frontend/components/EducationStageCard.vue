<template>
  <div
    class="flex gap-4 items-start"
    :style="{ paddingLeft: `${stage.depth * 20}px` }"
  >
    <!-- Status dot -->
    <div class="flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold z-10"
         :class="dotClass">
      {{ dotIcon }}
    </div>

    <!-- Card body -->
    <div class="flex-1 card mb-0 min-w-0" :class="cardClass">
      <div class="flex items-start justify-between gap-3">
        <div class="min-w-0">
          <p class="text-sm font-semibold" :class="titleClass">{{ stage.label }}</p>
          <p v-if="stage.months" class="text-xs text-gray-500 mt-0.5">{{ stage.months }} Monate</p>
          <p v-else class="text-xs text-gray-500 mt-0.5">Bereits abgeschlossen</p>

          <!-- Completed field variants -->
          <div v-if="stage.requiresField && completedFields.length" class="mt-1.5 flex flex-wrap gap-1">
            <span
              v-for="f in completedFields"
              :key="f"
              class="badge bg-green-500/15 text-green-400 text-xs"
            >
              ✓ {{ f }}
            </span>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex-shrink-0">
          <!-- Completed (no field) -->
          <span v-if="isCompleted && !stage.requiresField" class="text-green-400 text-sm font-semibold">✓</span>

          <!-- In progress -->
          <span v-else-if="isInProgress" class="badge bg-accent/20 text-accent text-xs">Läuft</span>

          <!-- Available -->
          <div v-else-if="availableStage && !busy">
            <!-- No field needed: direct enroll -->
            <button
              v-if="!availableStage.requiresField"
              class="btn-primary text-xs px-3 py-1.5"
              :disabled="enrolling"
              @click="doEnroll(null)"
            >
              {{ enrolling ? '…' : 'Einschreiben' }}
            </button>

            <!-- Field picker -->
            <div v-else class="space-y-2">
              <select
                v-model="selectedField"
                class="input text-xs py-1 w-full"
              >
                <option value="">Fachrichtung wählen…</option>
                <option
                  v-for="fo in availableStage.fieldOptions"
                  :key="fo.value"
                  :value="fo.value"
                >
                  {{ fo.label }}
                </option>
              </select>
              <button
                class="btn-primary text-xs px-3 py-1.5 w-full"
                :disabled="!selectedField || enrolling"
                @click="doEnroll(selectedField)"
              >
                {{ enrolling ? '…' : 'Einschreiben' }}
              </button>
            </div>
          </div>

          <!-- Locked -->
          <span v-else-if="!isCompleted && !isInProgress" class="text-gray-600 text-xs">Gesperrt</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface FieldOption { value: string; label: string }
interface AvailableStage {
  stageKey: string; label: string; durationMonths: number
  requiresField: boolean; fieldOptions: FieldOption[]
}
interface TreeStage {
  key: string; label: string; months: number | null
  depth: number; requiresField: boolean; fieldOptions: FieldOption[]
}

interface Props {
  stage: TreeStage
  availableStages: AvailableStage[]
  busy: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{ enroll: [stage: string, field: string | null] }>()

// Pull completion info from the parent page via provide/inject
const progress = inject<{ completedStages: string[], mainStage: string, mainStageMonthsRemaining: number } | null>('educationProgress', null)

const selectedField = ref('')
const enrolling = ref(false)

const completedStages = computed(() => progress?.completedStages ?? [])

const isCompleted = computed(() => {
  if (!props.stage.requiresField) return completedStages.value.includes(props.stage.key)
  // For field-based stages: completed if ALL selected fields are done — we just show badge per field
  return false
})

const completedFields = computed(() => {
  if (!props.stage.requiresField) return []
  return props.stage.fieldOptions
    .filter(fo => completedStages.value.includes(props.stage.key + '_' + fo.value))
    .map(fo => fo.label)
})

const isInProgress = computed(() =>
  progress?.mainStage === props.stage.key && (progress?.mainStageMonthsRemaining ?? 0) > 0,
)

const availableStage = computed(() =>
  props.availableStages.find(s => s.stageKey === props.stage.key) ?? null,
)

const dotClass = computed(() => {
  if (props.stage.key === 'GRUNDSCHULE') return 'bg-green-600 text-white'
  if (isCompleted.value) return 'bg-green-600 text-white'
  if (isInProgress.value) return 'bg-accent text-white'
  if (availableStage.value && !props.busy) return 'bg-surface-600 border-2 border-accent text-accent'
  return 'bg-surface-700 text-gray-600'
})

const dotIcon = computed(() => {
  if (props.stage.key === 'GRUNDSCHULE') return '✓'
  if (isCompleted.value) return '✓'
  if (isInProgress.value) return '▶'
  if (availableStage.value && !props.busy) return '○'
  return '🔒'
})

const cardClass = computed(() => {
  if (isCompleted.value || props.stage.key === 'GRUNDSCHULE') return 'border-green-600/30 opacity-75'
  if (isInProgress.value) return 'border-accent/50'
  if (availableStage.value && !props.busy) return 'hover:border-accent/40 transition-colors'
  return 'opacity-50'
})

const titleClass = computed(() => {
  if (isCompleted.value || props.stage.key === 'GRUNDSCHULE') return 'text-green-400'
  if (isInProgress.value) return 'text-accent'
  if (availableStage.value && !props.busy) return 'text-white'
  return 'text-gray-500'
})

async function doEnroll(field: string | null) {
  enrolling.value = true
  try {
    emit('enroll', props.stage.key, field || null)
  } finally {
    enrolling.value = false
    selectedField.value = ''
  }
}
</script>
