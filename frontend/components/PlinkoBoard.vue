<template>
  <div class="plinko-wrapper">
    <canvas ref="canvasRef" :width="CANVAS_W" :height="CANVAS_H" class="plinko-canvas" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  animating: boolean
  path: boolean[] | null
  winningSlot: number | null
}>()

const emit = defineEmits<{ done: [] }>()

// ── Board geometry ────────────────────────────────────────────────────────
const ROWS = 8
const SLOTS = ROWS + 1          // 9
const PEG_RADIUS = 7
const BALL_RADIUS = 10
const H_SPACING = 54
const V_SPACING = 56
const TOP_PAD = 50
const SLOT_HEIGHT = 44

// Row r has (r + 2) pegs. Rightmost peg of last row is at index (ROWS + 1).
// Total board width must fit ROWS + 1 = 9 peg columns.
const CANVAS_W = H_SPACING * (ROWS + 1) + PEG_RADIUS * 4   // ~504px
const CANVAS_H = TOP_PAD + V_SPACING * ROWS + SLOT_HEIGHT + 16

const MULTIPLIERS = [10, 4, 1.5, 0.5, 0.2, 0.5, 1.5, 4, 10]

const canvasRef = ref<HTMLCanvasElement | null>(null)
let animFrame: number | null = null

// ── Geometry helpers ──────────────────────────────────────────────────────

/** X-center of peg at column c in row r (both 0-indexed). */
function pegX(r: number, c: number): number {
  return CANVAS_W / 2 + (c - r / 2) * H_SPACING
}
function pegY(r: number): number {
  return TOP_PAD + r * V_SPACING
}

/** X-center of slot i (0-indexed). */
function slotCenterX(i: number): number {
  // Slots align with the gaps between pegs of the last row.
  // Last row (r = ROWS-1 = 7) has 9 pegs at cols 0..8.
  // Slot i sits centered under the gap between peg i-1 and peg i,
  // which equals pegX(7, i) - H_SPACING/2 for the center of that gap…
  // Simpler: treat slot i as if it's peg column i in a virtual row 8.
  return CANVAS_W / 2 + (i - ROWS / 2) * H_SPACING
}

// ── Waypoint generation ───────────────────────────────────────────────────

interface Point { x: number; y: number }

function buildWaypoints(path: boolean[]): Point[] {
  const pts: Point[] = []
  // Ball starts high above the apex peg (row 0, peg 0 at center-ish)
  pts.push({ x: CANVAS_W / 2, y: TOP_PAD - BALL_RADIUS * 4 })

  let rights = 0
  for (let r = 0; r < ROWS; r++) {
    const px = pegX(r, rights)
    const py = pegY(r)

    // Approach point: just above the peg surface
    pts.push({ x: px, y: py - PEG_RADIUS - 2 })

    // Deflection: move left or right, then drop toward next row
    const goRight = path[r]
    if (goRight) rights++
    const nextPegX = r < ROWS - 1 ? pegX(r + 1, rights) : slotCenterX(rights)
    pts.push({ x: (px + nextPegX) / 2, y: py + V_SPACING * 0.55 })
  }

  // Land in slot
  pts.push({ x: slotCenterX(rights), y: TOP_PAD + ROWS * V_SPACING + SLOT_HEIGHT * 0.55 })
  return pts
}

// ── Drawing ───────────────────────────────────────────────────────────────

function drawBackground(ctx: CanvasRenderingContext2D) {
  const grad = ctx.createLinearGradient(0, 0, 0, CANVAS_H)
  grad.addColorStop(0, '#0e0e24')
  grad.addColorStop(1, '#06060f')
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H)

  // Subtle grid lines for depth feel
  ctx.strokeStyle = 'rgba(255,255,255,0.025)'
  ctx.lineWidth = 1
  for (let y = 0; y < CANVAS_H; y += 20) {
    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(CANVAS_W, y); ctx.stroke()
  }
}

function drawPeg(ctx: CanvasRenderingContext2D, x: number, y: number, lit: boolean) {
  // Outer glow for lit pegs
  if (lit) {
    ctx.shadowColor = '#66aaff'
    ctx.shadowBlur = 14
  }

  const g = ctx.createRadialGradient(x - 2.5, y - 2.5, 0.5, x, y, PEG_RADIUS)
  if (lit) {
    g.addColorStop(0, '#d0e8ff')
    g.addColorStop(0.35, '#88bbff')
    g.addColorStop(1, '#1133aa')
  } else {
    g.addColorStop(0, '#b0b8cc')
    g.addColorStop(0.35, '#5566aa')
    g.addColorStop(1, '#0f1133')
  }

  ctx.beginPath()
  ctx.arc(x, y, PEG_RADIUS, 0, Math.PI * 2)
  ctx.fillStyle = g
  ctx.fill()
  ctx.shadowBlur = 0

  // Specular highlight
  ctx.beginPath()
  ctx.arc(x - 2, y - 2, PEG_RADIUS * 0.3, 0, Math.PI * 2)
  ctx.fillStyle = 'rgba(255,255,255,0.45)'
  ctx.fill()
}

function drawAllPegs(ctx: CanvasRenderingContext2D, litRows: number) {
  for (let r = 0; r < ROWS; r++) {
    const pegCount = r + 2
    for (let c = 0; c < pegCount; c++) {
      drawPeg(ctx, pegX(r, c), pegY(r), r < litRows)
    }
  }
}

function slotColor(mult: number, isWinner: boolean): string {
  if (isWinner) return '#e85530'
  if (mult >= 4) return '#b8860b'
  if (mult >= 1) return '#1a5276'
  return '#1c2833'
}

function drawSlots(ctx: CanvasRenderingContext2D, winner: number | null) {
  const slotY = TOP_PAD + ROWS * V_SPACING + 4
  const slotW = H_SPACING - 4
  const slotH = SLOT_HEIGHT - 4

  for (let i = 0; i < SLOTS; i++) {
    const cx = slotCenterX(i)
    const sx = cx - slotW / 2
    const isWinner = winner === i

    if (isWinner) {
      ctx.shadowColor = '#ff6633'
      ctx.shadowBlur = 20
    }

    // Slot background
    ctx.fillStyle = slotColor(MULTIPLIERS[i], isWinner)
    ctx.beginPath()
    ctx.roundRect(sx, slotY, slotW, slotH, 4)
    ctx.fill()
    ctx.shadowBlur = 0

    // Border
    ctx.strokeStyle = isWinner ? '#ff9966' : 'rgba(255,255,255,0.12)'
    ctx.lineWidth = isWinner ? 2 : 1
    ctx.stroke()

    // Multiplier label
    ctx.fillStyle = isWinner ? '#ffffff' : MULTIPLIERS[i] >= 1 ? '#dddddd' : '#888899'
    ctx.font = `bold ${isWinner ? 12 : 11}px "JetBrains Mono", monospace`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(`${MULTIPLIERS[i]}×`, cx, slotY + slotH / 2)
  }
}

function drawBall(ctx: CanvasRenderingContext2D, x: number, y: number) {
  // Drop shadow
  ctx.shadowColor = 'rgba(255,80,20,0.6)'
  ctx.shadowBlur = 18

  const g = ctx.createRadialGradient(x - 3.5, y - 3.5, 1, x, y, BALL_RADIUS)
  g.addColorStop(0, '#ffffff')
  g.addColorStop(0.25, '#ffcc99')
  g.addColorStop(0.6, '#ff6633')
  g.addColorStop(1, '#991100')

  ctx.beginPath()
  ctx.arc(x, y, BALL_RADIUS, 0, Math.PI * 2)
  ctx.fillStyle = g
  ctx.fill()
  ctx.shadowBlur = 0

  // Specular
  ctx.beginPath()
  ctx.arc(x - 3, y - 3, BALL_RADIUS * 0.28, 0, Math.PI * 2)
  ctx.fillStyle = 'rgba(255,255,255,0.75)'
  ctx.fill()
}

function redraw(ballX: number, ballY: number, litRows: number, winner: number | null) {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  drawBackground(ctx)
  drawAllPegs(ctx, litRows)
  drawSlots(ctx, winner)
  drawBall(ctx, ballX, ballY)
}

// ── Animation loop ────────────────────────────────────────────────────────

const MS_PER_SEGMENT = 145

function startAnimation(path: boolean[]) {
  if (animFrame !== null) cancelAnimationFrame(animFrame)

  const pts = buildWaypoints(path)
  const totalSegments = pts.length - 1
  const totalMs = totalSegments * MS_PER_SEGMENT
  const t0 = performance.now()

  function frame(now: number) {
    const elapsed = now - t0
    const rawProgress = Math.min(elapsed / totalMs, 1)

    const rawSeg = rawProgress * totalSegments
    const seg = Math.min(Math.floor(rawSeg), totalSegments - 1)
    const t = rawSeg - seg

    // Cubic ease-in-out per segment
    const eased = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2

    const from = pts[seg]
    const to = pts[seg + 1]
    const bx = from.x + (to.x - from.x) * eased
    const by = from.y + (to.y - from.y) * eased

    // Lit rows = how many rows the ball has passed
    const litRows = Math.floor((seg + 1) / 2)

    redraw(bx, by, litRows, null)

    if (rawProgress < 1) {
      animFrame = requestAnimationFrame(frame)
    } else {
      const finalSlot = path.filter(Boolean).length
      redraw(pts[pts.length - 1].x, pts[pts.length - 1].y, ROWS, finalSlot)
      animFrame = null
      emit('done')
    }
  }

  animFrame = requestAnimationFrame(frame)
}

// ── Idle board (shown before first play) ─────────────────────────────────

function drawIdle() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  drawBackground(ctx)
  drawAllPegs(ctx, 0)
  drawSlots(ctx, props.winningSlot)

  // Faint "drop here" arrow above board
  ctx.fillStyle = 'rgba(255,255,255,0.15)'
  ctx.font = '22px sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('▼', CANVAS_W / 2, TOP_PAD - BALL_RADIUS * 2.5)
}

// ── Lifecycle ─────────────────────────────────────────────────────────────

onMounted(() => {
  drawIdle()
})

watch(
  () => props.animating,
  (nowAnimating) => {
    if (nowAnimating && props.path) {
      startAnimation(props.path)
    }
  }
)

watch(
  () => props.winningSlot,
  () => {
    if (!props.animating) drawIdle()
  }
)

onUnmounted(() => {
  if (animFrame !== null) cancelAnimationFrame(animFrame)
})
</script>

<style scoped>
.plinko-wrapper {
  perspective: 1000px;
  perspective-origin: 50% -10%;
  display: flex;
  justify-content: center;
}

.plinko-canvas {
  transform: rotateX(14deg);
  transform-origin: top center;
  display: block;
  border-radius: 14px;
  box-shadow:
    0 28px 70px rgba(0, 0, 0, 0.8),
    0 6px 18px rgba(0, 0, 0, 0.55),
    inset 0 1px 0 rgba(255, 255, 255, 0.07);
  max-width: 100%;
}
</style>
