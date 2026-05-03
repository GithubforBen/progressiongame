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
const ROWS      = 8
const SLOTS     = ROWS + 1
const PEG_R     = 7
const H_SPACING = 58
const V_SPACING = 54
const TOP_PAD   = 58
const SLOT_H    = 54

const CANVAS_W = H_SPACING * (ROWS + 1) + H_SPACING   // 580
const CANVAS_H = TOP_PAD + V_SPACING * ROWS + SLOT_H + 24  // 568

const MULTIPLIERS = [10, 4, 1.5, 0.5, 0.2, 0.5, 1.5, 4, 10]

const SLOT_CFG = [
  { bg: '#5c4400', border: '#ffd700', text: '#ffd700', glow: 'rgba(255,215,0,0.55)'  },
  { bg: '#0a3020', border: '#00d4aa', text: '#00d4aa', glow: 'rgba(0,212,170,0.45)'  },
  { bg: '#081840', border: '#4488ff', text: '#7aabff', glow: 'rgba(68,136,255,0.35)' },
  { bg: '#180a28', border: '#7744aa', text: '#9966cc', glow: 'rgba(119,68,170,0.2)'  },
  { bg: '#160606', border: '#aa2233', text: '#882233', glow: 'rgba(170,34,51,0.2)'   },
  { bg: '#180a28', border: '#7744aa', text: '#9966cc', glow: 'rgba(119,68,170,0.2)'  },
  { bg: '#081840', border: '#4488ff', text: '#7aabff', glow: 'rgba(68,136,255,0.35)' },
  { bg: '#0a3020', border: '#00d4aa', text: '#00d4aa', glow: 'rgba(0,212,170,0.45)'  },
  { bg: '#5c4400', border: '#ffd700', text: '#ffd700', glow: 'rgba(255,215,0,0.55)'  },
]

// Group color palette — one color per concurrent group
const GROUP_COLORS = [
  { main: '#ff6b35', hi: '#fff9f5', glow: 'rgba(255,107,53,0.65)'   },
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
  slot:      number
  index:     number   // position in group (for stagger)
  x: number; y: number
  done:    boolean
  counted: boolean
}

interface GroupState {
  id:         number
  balls:      AnimBall[]
  color:      typeof GROUP_COLORS[0]
  radius:     number
  segMs:      number
  stagger:    number
  slotCounts: number[]
  startTime:  number
  doneAt:     number   // 0 = not done
  emitted:    boolean
}

// ── Canvas + active groups ─────────────────────────────────────────────────
const canvasRef   = ref<HTMLCanvasElement | null>(null)
const activeGroups = new Map<number, GroupState>()
let animFrame: number | null = null
let colorIndex = 0

function getCtx() {
  return canvasRef.value?.getContext('2d') ?? null
}

// ── Geometry ───────────────────────────────────────────────────────────────
function pegX(r: number, c: number) { return CANVAS_W / 2 + (c - r / 2) * H_SPACING }
function pegY(r: number)             { return TOP_PAD + r * V_SPACING }
function slotCX(i: number)           { return CANVAS_W / 2 + (i - ROWS / 2) * H_SPACING }

function buildWaypoints(path: boolean[]): Pt[] {
  const pts: Pt[] = [{ x: CANVAS_W / 2, y: TOP_PAD - PEG_R * 5 }]
  let rights = 0
  for (let r = 0; r < ROWS; r++) {
    const px = pegX(r, rights)
    const py = pegY(r)
    pts.push({ x: px, y: py - PEG_R - 2 })
    if (path[r]) rights++
    const nextX = r < ROWS - 1 ? pegX(r + 1, rights) : slotCX(rights)
    pts.push({ x: (px + nextX) / 2, y: py + V_SPACING * 0.55 })
  }
  pts.push({ x: slotCX(rights), y: TOP_PAD + ROWS * V_SPACING + SLOT_H * 0.5 })
  return pts
}

const TOTAL_SEGS = 1 + ROWS * 2  // 17

// ── Drawing ────────────────────────────────────────────────────────────────
function drawBackground(ctx: CanvasRenderingContext2D) {
  const g = ctx.createLinearGradient(0, 0, 0, CANVAS_H)
  g.addColorStop(0, '#0b0b1e')
  g.addColorStop(0.6, '#08081a')
  g.addColorStop(1, '#040410')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H)

  ctx.fillStyle = 'rgba(255,255,255,0.016)'
  for (let x = 15; x < CANVAS_W; x += 22) {
    for (let y = 15; y < CANVAS_H; y += 22) {
      ctx.beginPath()
      ctx.arc(x, y, 0.6, 0, Math.PI * 2)
      ctx.fill()
    }
  }
}

function drawPeg(ctx: CanvasRenderingContext2D, x: number, y: number, lit: boolean) {
  if (lit) { ctx.shadowColor = '#8899ff'; ctx.shadowBlur = 10 }
  const g = ctx.createRadialGradient(x - 2.5, y - 2.5, 0.5, x, y, PEG_R)
  if (lit) {
    g.addColorStop(0, '#ddeeff'); g.addColorStop(0.35, '#99bbff'); g.addColorStop(1, '#1133bb')
  } else {
    g.addColorStop(0, '#c0c8d8'); g.addColorStop(0.35, '#5566aa'); g.addColorStop(1, '#0e1235')
  }
  ctx.beginPath(); ctx.arc(x, y, PEG_R, 0, Math.PI * 2)
  ctx.fillStyle = g; ctx.fill()
  ctx.shadowBlur = 0
  ctx.beginPath(); ctx.arc(x - 2.2, y - 2.2, PEG_R * 0.28, 0, Math.PI * 2)
  ctx.fillStyle = 'rgba(255,255,255,0.52)'; ctx.fill()
}

function drawAllPegs(ctx: CanvasRenderingContext2D, litRows: number) {
  for (let r = 0; r < ROWS; r++)
    for (let c = 0; c < r + 2; c++)
      drawPeg(ctx, pegX(r, c), pegY(r), r < litRows)
}

function drawSlots(ctx: CanvasRenderingContext2D, combined: number[]) {
  const slotY = TOP_PAD + ROWS * V_SPACING + 4
  const slotW = H_SPACING - 6
  const slotH = SLOT_H - 6

  for (let i = 0; i < SLOTS; i++) {
    const cx  = slotCX(i)
    const sx  = cx - slotW / 2
    const cfg = SLOT_CFG[i]!
    const cnt = combined[i] ?? 0
    const hot = cnt > 0

    if (hot) { ctx.shadowColor = cfg.glow; ctx.shadowBlur = 16 }

    const g = ctx.createLinearGradient(sx, slotY, sx, slotY + slotH)
    g.addColorStop(0, hot ? cfg.bg + 'ff' : cfg.bg + 'bb')
    g.addColorStop(1, '#00000099')
    ctx.fillStyle = g
    ctx.beginPath(); ctx.roundRect(sx, slotY, slotW, slotH, [4, 4, 0, 0]); ctx.fill()

    ctx.strokeStyle = hot ? cfg.border : cfg.border + '55'
    ctx.lineWidth   = hot ? 1.5 : 1; ctx.stroke()
    ctx.shadowBlur  = 0

    ctx.fillStyle    = hot ? cfg.text : cfg.text + '88'
    ctx.font         = 'bold 11px "JetBrains Mono", monospace'
    ctx.textAlign    = 'center'; ctx.textBaseline = 'middle'
    ctx.fillText(`${MULTIPLIERS[i]}×`, cx, slotY + slotH * 0.38)

    if (cnt > 0) {
      ctx.fillStyle    = cfg.text
      ctx.font         = 'bold 9px sans-serif'
      ctx.fillText(`${cnt}`, cx, slotY + slotH * 0.74)
    }
  }
}

function drawBall(ctx: CanvasRenderingContext2D, x: number, y: number, color: typeof GROUP_COLORS[0], r: number) {
  if (r <= 0) return
  ctx.shadowColor = color.glow
  ctx.shadowBlur  = r > 5 ? 12 : 6

  if (r <= 3) {
    ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2)
    ctx.fillStyle = color.main; ctx.fill()
  } else {
    const g = ctx.createRadialGradient(x - r * 0.35, y - r * 0.35, r * 0.06, x, y, r)
    g.addColorStop(0, color.hi); g.addColorStop(0.18, color.main)
    g.addColorStop(0.7, color.main + 'cc'); g.addColorStop(1, '#000000bb')
    ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2)
    ctx.fillStyle = g; ctx.fill()
    ctx.beginPath(); ctx.arc(x - r * 0.32, y - r * 0.32, r * 0.22, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(255,255,255,0.65)'; ctx.fill()
  }
  ctx.shadowBlur = 0
}

function fullRedraw() {
  const ctx = getCtx()
  if (!ctx) return

  drawBackground(ctx)

  // Lit rows = highest row any ball has passed
  let maxLit = 0
  for (const gs of activeGroups.values())
    for (const ab of gs.balls)
      if (ab.done) maxLit = ROWS

  drawAllPegs(ctx, maxLit)

  // Combined slot counts from all active groups
  const combined = new Array<number>(SLOTS).fill(0)
  for (const gs of activeGroups.values())
    for (let i = 0; i < SLOTS; i++) combined[i] += gs.slotCounts[i] ?? 0
  drawSlots(ctx, combined)

  // Draw all balls from all groups
  for (const gs of activeGroups.values())
    for (const ab of gs.balls)
      drawBall(ctx, ab.x, ab.y, gs.color, gs.radius)
}

// ── Animation loop ─────────────────────────────────────────────────────────
const LINGER_MS = 1800  // ms to keep showing landed balls after group finishes

function animLoop(now: number) {
  let anyActive = false
  const newlyLanded: { groupId: number; slot: number; multiplier: number; color: string }[] = []

  for (const [id, gs] of activeGroups) {
    if (gs.doneAt > 0) {
      // Linger phase — keep rendering, then clean up
      if (now - gs.doneAt > LINGER_MS) activeGroups.delete(id)
      else anyActive = true
      continue
    }

    const elapsed  = now - gs.startTime
    const ballMs   = gs.segMs * TOTAL_SEGS
    let   allDone  = true
    let   maxLit   = 0

    for (const ab of gs.balls) {
      const be     = elapsed - gs.stagger * ab.index
      if (be < 0) { ab.x = CANVAS_W / 2; ab.y = TOP_PAD - gs.radius * 5; allDone = false; continue }

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

      const litRows = Math.floor((seg + 1) / 2)
      if (litRows > maxLit) maxLit = litRows

      if (rawP >= 1 && !ab.counted) {
        ab.counted = true
        gs.slotCounts[ab.slot]++
        newlyLanded.push({ groupId: id, slot: ab.slot, multiplier: MULTIPLIERS[ab.slot]!, color: gs.color.main })
      }
      ab.done = rawP >= 1
    }

    if (allDone) {
      gs.doneAt = now
      if (!gs.emitted) {
        gs.emitted = true
        emit('groupDone', id)
      }
    }

    anyActive = true
  }

  fullRedraw()

  // Batch-emit ball landings after draw
  for (const landed of newlyLanded)
    emit('ballLanded', landed.groupId, landed.slot, landed.multiplier, landed.color)

  if (anyActive || activeGroups.size > 0) {
    animFrame = requestAnimationFrame(animLoop)
  } else {
    animFrame = null
    drawIdle()
  }
}

// ── Start a group ──────────────────────────────────────────────────────────
function startGroup(group: PlinkoGroup) {
  const count  = group.balls.length
  const radius = count <= 3 ? 10 : count <= 10 ? 9 : count <= 30 ? 7 : count <= 80 ? 5 : count <= 200 ? 3.5 : 2.5
  const segMs  = count <= 3 ? 240 : count <= 10 ? 200 : count <= 30 ? 165 : count <= 80 ? 135 : count <= 200 ? 110 : 85
  const stagger = count <= 1 ? 0 : Math.min(70, 2200 / Math.max(count - 1, 1))
  const color  = GROUP_COLORS[colorIndex % GROUP_COLORS.length]!
  colorIndex++

  const animBalls: AnimBall[] = group.balls.map((b, i) => ({
    waypoints: buildWaypoints(b.path),
    slot: b.slot,
    index: i,
    x: CANVAS_W / 2,
    y: TOP_PAD - radius * 5,
    done: false,
    counted: false,
  }))

  activeGroups.set(group.id, {
    id: group.id,
    balls: animBalls,
    color,
    radius,
    segMs,
    stagger,
    slotCounts: new Array<number>(SLOTS).fill(0),
    startTime: performance.now(),
    doneAt:  0,
    emitted: false,
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
  ctx.fillStyle    = 'rgba(255,255,255,0.1)'
  ctx.font         = '18px sans-serif'
  ctx.textAlign    = 'center'; ctx.textBaseline = 'middle'
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
  border-radius: 12px;
  max-width: 100%;
  height: auto;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.8),
    0 4px 16px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset;
}
</style>
