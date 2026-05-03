<template>
  <div class="plinko-root">
    <canvas ref="canvasRef" :width="CANVAS_W" :height="CANVAS_H" class="plinko-canvas" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'

export interface PlinkoBall {
  path: boolean[]
  slot: number
  multiplier: number
}

export interface PlinkoGroup {
  id: number
  balls: PlinkoBall[]
  ballValue: number
}

const props = defineProps<{
  queue: PlinkoGroup[]
}>()

const emit = defineEmits<{
  groupDone: [id: number]
  ballLanded: [groupId: number, slot: number, multiplier: number, groupColor: string]
}>()

// ── Board constants ────────────────────────────────────────────────────────
const ROWS      = 16
const SLOTS     = ROWS + 1   // 17
const PEG_R     = 5
const H_SPACING = 34
const V_SPACING = 32
const TOP_PAD   = 48
const SLOT_H    = 48

// CANVAS_W = (ROWS + 2) * H_SPACING  so there's exactly H_SPACING margin on each side
const CANVAS_W = H_SPACING * (ROWS + 2)  // 612
const CANVAS_H = TOP_PAD + V_SPACING * ROWS + SLOT_H + 22  // 582

// Multipliers for 17 slots — symmetric, EV ~94%
const MULTIPLIERS = [500, 100, 20, 8, 3, 1.5, 0.5, 0.4, 0.2, 0.4, 0.5, 1.5, 3, 8, 20, 100, 500]

// Slot color config — edges=gold, middle=dark blue
const SLOT_CFG = [
  { bg: '#4a3800', border: '#ffd700', text: '#ffd700', glow: 'rgba(255,215,0,0.7)'    }, // 0  500×
  { bg: '#4a2200', border: '#ff8c00', text: '#ffaa33', glow: 'rgba(255,140,0,0.65)'   }, // 1  100×
  { bg: '#481400', border: '#ff5500', text: '#ff7744', glow: 'rgba(255,85,0,0.55)'    }, // 2  20×
  { bg: '#440a0a', border: '#ff2233', text: '#ff5566', glow: 'rgba(255,34,51,0.5)'    }, // 3  8×
  { bg: '#3a0a1a', border: '#dd2266', text: '#ff5599', glow: 'rgba(221,34,102,0.45)'  }, // 4  3×
  { bg: '#28083a', border: '#9933cc', text: '#cc66ff', glow: 'rgba(153,51,204,0.4)'   }, // 5  1.5×
  { bg: '#0c1040', border: '#3355cc', text: '#6688ff', glow: 'rgba(51,85,204,0.3)'    }, // 6  0.5×
  { bg: '#080c2c', border: '#1a3388', text: '#4466bb', glow: 'rgba(26,51,136,0.2)'    }, // 7  0.4×
  { bg: '#060818', border: '#111e55', text: '#223377', glow: 'rgba(17,30,85,0.15)'    }, // 8  0.2× center
  { bg: '#080c2c', border: '#1a3388', text: '#4466bb', glow: 'rgba(26,51,136,0.2)'    }, // 9
  { bg: '#0c1040', border: '#3355cc', text: '#6688ff', glow: 'rgba(51,85,204,0.3)'    }, // 10
  { bg: '#28083a', border: '#9933cc', text: '#cc66ff', glow: 'rgba(153,51,204,0.4)'   }, // 11
  { bg: '#3a0a1a', border: '#dd2266', text: '#ff5599', glow: 'rgba(221,34,102,0.45)'  }, // 12
  { bg: '#440a0a', border: '#ff2233', text: '#ff5566', glow: 'rgba(255,34,51,0.5)'    }, // 13
  { bg: '#481400', border: '#ff5500', text: '#ff7744', glow: 'rgba(255,85,0,0.55)'    }, // 14
  { bg: '#4a2200', border: '#ff8c00', text: '#ffaa33', glow: 'rgba(255,140,0,0.65)'   }, // 15
  { bg: '#4a3800', border: '#ffd700', text: '#ffd700', glow: 'rgba(255,215,0,0.7)'    }, // 16
]

// Ball color palette — one distinct color per concurrent group
const GROUP_COLORS = [
  { main: '#ff6b35', hi: '#fff8f5', glow: 'rgba(255,107,53,0.65)'   },
  { main: '#4ecdc4', hi: '#f0fffe', glow: 'rgba(78,205,196,0.65)'   },
  { main: '#ffe66d', hi: '#fffef0', glow: 'rgba(255,230,109,0.65)'  },
  { main: '#ff6b6b', hi: '#fff5f5', glow: 'rgba(255,107,107,0.65)'  },
  { main: '#a8e063', hi: '#f8fff0', glow: 'rgba(168,224,99,0.65)'   },
  { main: '#c77dff', hi: '#faf5ff', glow: 'rgba(199,125,255,0.65)'  },
]

// ── Types ──────────────────────────────────────────────────────────────────
interface Pt { x: number; y: number }

interface AnimBall {
  waypoints: Pt[]
  slot:    number
  index:   number
  x: number; y: number
  done:    boolean
  counted: boolean
}

interface GroupState {
  id:         number
  balls:      AnimBall[]
  color:      typeof GROUP_COLORS[0]
  radius:     number
  segMs:      number   // CONSTANT — same drama for all counts
  stagger:    number
  slotCounts: number[]
  startTime:  number
  doneAt:     number
  emitted:    boolean
}

// ── Canvas + state ─────────────────────────────────────────────────────────
const canvasRef    = ref<HTMLCanvasElement | null>(null)
const activeGroups = new Map<number, GroupState>()
let   animFrame: number | null = null
let   colorIdx = 0

function getCtx() { return canvasRef.value?.getContext('2d') ?? null }

// ── Geometry ───────────────────────────────────────────────────────────────
// Centered pyramid: all rows symmetric around CANVAS_W/2
function pegX(r: number, c: number) { return CANVAS_W / 2 + (c - (r + 1) / 2) * H_SPACING }
function pegY(r: number)             { return TOP_PAD + r * V_SPACING }
function slotCX(i: number)           { return CANVAS_W / 2 + (i - ROWS / 2) * H_SPACING }

const TOTAL_SEGS = 1 + ROWS * 2  // 33 segments for 16 rows

// ── Waypoints ──────────────────────────────────────────────────────────────
function buildWaypoints(path: boolean[]): Pt[] {
  const pts: Pt[] = [{ x: CANVAS_W / 2, y: TOP_PAD - PEG_R * 5 }]
  let rights = 0
  for (let r = 0; r < ROWS; r++) {
    const px = pegX(r, rights)
    const py = pegY(r)
    pts.push({ x: px, y: py - PEG_R - 1 })
    if (path[r]) rights++
    const nextX = r < ROWS - 1 ? pegX(r + 1, rights) : slotCX(rights)
    pts.push({ x: (px + nextX) / 2, y: py + V_SPACING * 0.55 })
  }
  pts.push({ x: slotCX(rights), y: TOP_PAD + ROWS * V_SPACING + SLOT_H * 0.5 })
  return pts
}

// ── Drawing ────────────────────────────────────────────────────────────────
function drawBackground(ctx: CanvasRenderingContext2D) {
  const g = ctx.createLinearGradient(0, 0, 0, CANVAS_H)
  g.addColorStop(0,   '#0c0c20')
  g.addColorStop(0.6, '#080816')
  g.addColorStop(1,   '#050510')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H)

  // Subtle dot pattern
  ctx.fillStyle = 'rgba(255,255,255,0.014)'
  for (let x = 18; x < CANVAS_W; x += 24)
    for (let y = 18; y < CANVAS_H; y += 24) {
      ctx.beginPath(); ctx.arc(x, y, 0.5, 0, Math.PI * 2); ctx.fill()
    }
}

function drawPeg(ctx: CanvasRenderingContext2D, x: number, y: number, lit: boolean) {
  if (lit) { ctx.shadowColor = '#7799ff'; ctx.shadowBlur = 8 }
  const g = ctx.createRadialGradient(x - 1.8, y - 1.8, 0.4, x, y, PEG_R)
  if (lit) {
    g.addColorStop(0, '#ddeeff'); g.addColorStop(0.4, '#88aaff'); g.addColorStop(1, '#1133bb')
  } else {
    g.addColorStop(0, '#c0cce0'); g.addColorStop(0.4, '#5566aa'); g.addColorStop(1, '#0d1230')
  }
  ctx.beginPath(); ctx.arc(x, y, PEG_R, 0, Math.PI * 2)
  ctx.fillStyle = g; ctx.fill()
  ctx.shadowBlur = 0
  // Specular highlight
  ctx.beginPath(); ctx.arc(x - 1.5, y - 1.5, PEG_R * 0.28, 0, Math.PI * 2)
  ctx.fillStyle = 'rgba(255,255,255,0.55)'; ctx.fill()
}

function drawAllPegs(ctx: CanvasRenderingContext2D, litRows: number) {
  for (let r = 0; r < ROWS; r++)
    for (let c = 0; c < r + 2; c++)
      drawPeg(ctx, pegX(r, c), pegY(r), r < litRows)
}

function slotLabel(mult: number): string {
  if (mult >= 100) return `${mult}`
  if (mult >= 1)   return `${mult}×`
  return `${mult}×`
}

function drawSlots(ctx: CanvasRenderingContext2D, combined: number[]) {
  const slotY = TOP_PAD + ROWS * V_SPACING + 4
  const slotW = H_SPACING - 4
  const slotH = SLOT_H - 6

  for (let i = 0; i < SLOTS; i++) {
    const cx  = slotCX(i)
    const sx  = cx - slotW / 2
    const cfg = SLOT_CFG[i]!
    const cnt = combined[i] ?? 0
    const hot = cnt > 0

    if (hot) { ctx.shadowColor = cfg.glow; ctx.shadowBlur = 14 }

    // Slot body gradient
    const g = ctx.createLinearGradient(sx, slotY, sx, slotY + slotH)
    g.addColorStop(0, hot ? cfg.bg + 'ff' : cfg.bg + 'aa')
    g.addColorStop(1, '#00000099')
    ctx.fillStyle = g
    ctx.beginPath(); ctx.roundRect(sx, slotY, slotW, slotH, [3, 3, 0, 0]); ctx.fill()

    ctx.strokeStyle = hot ? cfg.border : cfg.border + '55'
    ctx.lineWidth   = hot ? 1.5 : 0.8; ctx.stroke()
    ctx.shadowBlur  = 0

    // Multiplier label — smaller font for large values
    const mult = MULTIPLIERS[i]!
    const fontSize = mult >= 100 ? 7 : mult >= 10 ? 8 : 9
    ctx.fillStyle    = hot ? cfg.text : cfg.text + '88'
    ctx.font         = `bold ${fontSize}px "JetBrains Mono", monospace`
    ctx.textAlign    = 'center'; ctx.textBaseline = 'middle'
    ctx.fillText(slotLabel(mult), cx, slotY + slotH * (cnt > 0 ? 0.34 : 0.5))

    // Ball count badge
    if (cnt > 0) {
      ctx.fillStyle = cfg.text
      ctx.font      = `bold 8px sans-serif`
      ctx.fillText(`${cnt}`, cx, slotY + slotH * 0.72)
    }
  }
}

function drawBall(ctx: CanvasRenderingContext2D, x: number, y: number, color: typeof GROUP_COLORS[0], r: number) {
  if (r <= 0) return
  ctx.shadowColor = color.glow
  ctx.shadowBlur  = r > 5 ? 10 : 5

  if (r <= 3) {
    ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2)
    ctx.fillStyle = color.main; ctx.fill()
  } else {
    const g = ctx.createRadialGradient(x - r * 0.35, y - r * 0.35, r * 0.06, x, y, r)
    g.addColorStop(0, color.hi)
    g.addColorStop(0.2, color.main)
    g.addColorStop(0.75, color.main + 'bb')
    g.addColorStop(1, '#000000aa')
    ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2)
    ctx.fillStyle = g; ctx.fill()
    ctx.beginPath(); ctx.arc(x - r * 0.3, y - r * 0.3, r * 0.2, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(255,255,255,0.6)'; ctx.fill()
  }
  ctx.shadowBlur = 0
}

function fullRedraw() {
  const ctx = getCtx()
  if (!ctx) return

  drawBackground(ctx)

  // Lit rows = ROWS if any ball has finished (pegs light up progressively per group)
  let maxLit = 0
  for (const gs of activeGroups.values())
    for (const ab of gs.balls)
      if (ab.done) { maxLit = ROWS; break }

  drawAllPegs(ctx, maxLit)

  // Combined slot counts across all groups
  const combined = new Array<number>(SLOTS).fill(0)
  for (const gs of activeGroups.values())
    for (let i = 0; i < SLOTS; i++) combined[i] += gs.slotCounts[i] ?? 0
  drawSlots(ctx, combined)

  // Draw balls — front to back (higher groups on top)
  for (const gs of activeGroups.values())
    for (const ab of gs.balls)
      drawBall(ctx, ab.x, ab.y, gs.color, gs.radius)
}

// ── Animation loop ─────────────────────────────────────────────────────────
const LINGER_MS = 2000

function animLoop(now: number) {
  let anyAlive = false
  const newlyLanded: { groupId: number; slot: number; color: string }[] = []

  for (const [id, gs] of activeGroups) {
    // Linger phase after animation complete
    if (gs.doneAt > 0) {
      if (now - gs.doneAt > LINGER_MS) activeGroups.delete(id)
      else anyAlive = true
      continue
    }

    const elapsed = now - gs.startTime
    const ballMs  = gs.segMs * TOTAL_SEGS
    let   allDone = true

    for (const ab of gs.balls) {
      const be = elapsed - gs.stagger * ab.index
      if (be < 0) {
        ab.x = CANVAS_W / 2; ab.y = TOP_PAD - gs.radius * 5
        allDone = false; continue
      }

      const rawP   = Math.min(be / ballMs, 1)
      if (rawP < 1) allDone = false

      const rawSeg = rawP * TOTAL_SEGS
      const seg    = Math.min(Math.floor(rawSeg), TOTAL_SEGS - 1)
      const t      = rawSeg - seg
      const eased  = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2

      const fromPt = ab.waypoints[seg]!
      const toPt   = ab.waypoints[seg + 1]!
      ab.x = fromPt.x + (toPt.x - fromPt.x) * eased
      ab.y = fromPt.y + (toPt.y - fromPt.y) * eased

      if (rawP >= 1 && !ab.counted) {
        ab.counted = true; ab.done = true
        gs.slotCounts[ab.slot]++
        newlyLanded.push({ groupId: id, slot: ab.slot, color: gs.color.main })
      }
    }

    if (allDone) {
      gs.doneAt = now
      if (!gs.emitted) { gs.emitted = true; emit('groupDone', id) }
    }
    anyAlive = true
  }

  fullRedraw()

  for (const { groupId, slot, color } of newlyLanded)
    emit('ballLanded', groupId, slot, MULTIPLIERS[slot]!, color)

  if (anyAlive || activeGroups.size > 0) {
    animFrame = requestAnimationFrame(animLoop)
  } else {
    animFrame = null
    drawIdle()
  }
}

// ── Start a group ──────────────────────────────────────────────────────────
function startGroup(group: PlinkoGroup) {
  const count = group.balls.length

  // Ball radius: bigger for fewer balls, tiny dots for many
  const radius = count <= 3 ? 8 : count <= 10 ? 7 : count <= 30 ? 5 : count <= 100 ? 3.5 : 2.5

  // segMs is CONSTANT — every ball falls at the same dramatic speed
  const segMs = 135

  // Stagger: total stagger window = 2s regardless of count
  const stagger = count <= 1 ? 0 : Math.min(8, 2000 / Math.max(count - 1, 1))

  const color = GROUP_COLORS[colorIdx % GROUP_COLORS.length]!
  colorIdx++

  activeGroups.set(group.id, {
    id: group.id,
    balls: group.balls.map((b, i) => ({
      waypoints: buildWaypoints(b.path),
      slot: b.slot, index: i,
      x: CANVAS_W / 2, y: TOP_PAD - radius * 5,
      done: false, counted: false,
    })),
    color, radius, segMs, stagger,
    slotCounts: new Array<number>(SLOTS).fill(0),
    startTime: performance.now(),
    doneAt: 0, emitted: false,
  })

  if (!animFrame) animFrame = requestAnimationFrame(animLoop)
}

// ── Idle draw ──────────────────────────────────────────────────────────────
function drawIdle() {
  const ctx = getCtx()
  if (!ctx) return
  drawBackground(ctx)
  drawAllPegs(ctx, 0)
  drawSlots(ctx, new Array<number>(SLOTS).fill(0))
  ctx.fillStyle = 'rgba(255,255,255,0.1)'
  ctx.font = '16px sans-serif'
  ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
  ctx.fillText('▼', CANVAS_W / 2, TOP_PAD - PEG_R * 3)
}

// ── Lifecycle ──────────────────────────────────────────────────────────────
onMounted(() => {
  drawIdle()
  for (const g of props.queue) startGroup(g)
})

watch(
  () => props.queue.map(g => g.id),
  (newIds, oldIds) => {
    const prev = oldIds ?? []
    for (const id of newIds) {
      if (!prev.includes(id)) {
        const g = props.queue.find(q => q.id === id)
        if (g) startGroup(g)
      }
    }
  },
)

onUnmounted(() => { if (animFrame !== null) cancelAnimationFrame(animFrame) })
</script>

<style scoped>
.plinko-root {
  display: flex;
  justify-content: center;
  width: 100%;
}

.plinko-canvas {
  display: block;
  border-radius: 10px;
  max-width: 100%;
  height: auto;
  box-shadow:
    0 16px 50px rgba(0, 0, 0, 0.8),
    0 3px 12px rgba(0, 0, 0, 0.5),
    inset 0 0 0 1px rgba(255, 255, 255, 0.03);
}
</style>
