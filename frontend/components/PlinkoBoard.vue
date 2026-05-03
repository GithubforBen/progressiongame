<template>
  <div class="plinko-outer">
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

const props = defineProps<{
  animating: boolean
  balls: PlinkoBall[] | null
}>()

const emit = defineEmits<{ done: [] }>()

// ── Board geometry ─────────────────────────────────────────────────────────
const ROWS      = 8
const SLOTS     = ROWS + 1
const PEG_R     = 7
const H_SPACING = 58
const V_SPACING = 54
const TOP_PAD   = 58
const SLOT_H    = 54

const CANVAS_W  = H_SPACING * (ROWS + 1) + H_SPACING
const CANVAS_H  = TOP_PAD + V_SPACING * ROWS + SLOT_H + 24

const MULTIPLIERS = [10, 4, 1.5, 0.5, 0.2, 0.5, 1.5, 4, 10]

// Slot visual config: index → color theme
const SLOT_CFG = [
  { bg: '#7a5c00', border: '#ffd700', text: '#ffd700', glow: 'rgba(255,215,0,0.6)'  },  // 10x gold
  { bg: '#0d3d2a', border: '#00d4aa', text: '#00d4aa', glow: 'rgba(0,212,170,0.45)' },  // 4x teal
  { bg: '#0a2050', border: '#4488ff', text: '#88aaff', glow: 'rgba(68,136,255,0.3)'  },  // 1.5x blue
  { bg: '#1a0a2a', border: '#7744aa', text: '#9966cc', glow: 'rgba(119,68,170,0.2)'  },  // 0.5x purple
  { bg: '#180808', border: '#cc2244', text: '#882222', glow: 'rgba(204,34,68,0.2)'   },  // 0.2x red
  { bg: '#1a0a2a', border: '#7744aa', text: '#9966cc', glow: 'rgba(119,68,170,0.2)'  },
  { bg: '#0a2050', border: '#4488ff', text: '#88aaff', glow: 'rgba(68,136,255,0.3)'  },
  { bg: '#0d3d2a', border: '#00d4aa', text: '#00d4aa', glow: 'rgba(0,212,170,0.45)' },
  { bg: '#7a5c00', border: '#ffd700', text: '#ffd700', glow: 'rgba(255,215,0,0.6)'  },
]

// Ball color palette (cycle for multiple balls)
const BALL_PALETTE = [
  { main: '#ff6b35', hi: '#fff9f5', glow: 'rgba(255,107,53,0.7)'  },
  { main: '#4ecdc4', hi: '#f0fffe', glow: 'rgba(78,205,196,0.7)'  },
  { main: '#ffe66d', hi: '#fffef0', glow: 'rgba(255,230,109,0.7)' },
  { main: '#ff6b6b', hi: '#fff5f5', glow: 'rgba(255,107,107,0.7)' },
  { main: '#a8e063', hi: '#f8fff0', glow: 'rgba(168,224,99,0.7)'  },
  { main: '#c77dff', hi: '#faf5ff', glow: 'rgba(199,125,255,0.7)' },
  { main: '#ff9f1c', hi: '#fffaf0', glow: 'rgba(255,159,28,0.7)'  },
  { main: '#2dc3ff', hi: '#f0fbff', glow: 'rgba(45,195,255,0.7)'  },
]

const canvasRef = ref<HTMLCanvasElement | null>(null)
let animFrame: number | null = null

// ── Geometry helpers ───────────────────────────────────────────────────────
function pegX(r: number, c: number) { return CANVAS_W / 2 + (c - r / 2) * H_SPACING }
function pegY(r: number)             { return TOP_PAD + r * V_SPACING }
function slotCX(i: number)           { return CANVAS_W / 2 + (i - ROWS / 2) * H_SPACING }

// ── Waypoint generation ────────────────────────────────────────────────────
interface Pt { x: number; y: number }

function buildWaypoints(path: boolean[]): Pt[] {
  const pts: Pt[] = []
  pts.push({ x: CANVAS_W / 2, y: TOP_PAD - PEG_R * 5 })

  let rights = 0
  for (let r = 0; r < ROWS; r++) {
    const px = pegX(r, rights)
    const py = pegY(r)
    pts.push({ x: px, y: py - PEG_R - 2 })

    const goRight = path[r]
    if (goRight) rights++
    const nextX = r < ROWS - 1 ? pegX(r + 1, rights) : slotCX(rights)
    pts.push({ x: (px + nextX) / 2, y: py + V_SPACING * 0.55 })
  }
  pts.push({ x: slotCX(rights), y: TOP_PAD + ROWS * V_SPACING + SLOT_H * 0.5 })
  return pts
}

// ── Animation state ────────────────────────────────────────────────────────
interface AnimBall {
  waypoints: Pt[]
  slot: number
  color: typeof BALL_PALETTE[0]
  radius: number
  startDelay: number
  x: number
  y: number
  done: boolean
  counted: boolean
}

// ── Drawing helpers ────────────────────────────────────────────────────────
function getCtx() {
  const c = canvasRef.value
  return c ? c.getContext('2d') : null
}

function drawBackground(ctx: CanvasRenderingContext2D) {
  const g = ctx.createLinearGradient(0, 0, 0, CANVAS_H)
  g.addColorStop(0, '#0b0b1e')
  g.addColorStop(0.5, '#080814')
  g.addColorStop(1, '#040410')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H)

  // Subtle dot grid for depth
  ctx.fillStyle = 'rgba(255,255,255,0.018)'
  for (let x = 10; x < CANVAS_W; x += 20) {
    for (let y = 10; y < CANVAS_H; y += 20) {
      ctx.beginPath()
      ctx.arc(x, y, 0.7, 0, Math.PI * 2)
      ctx.fill()
    }
  }
}

function drawPeg(ctx: CanvasRenderingContext2D, x: number, y: number, lit: boolean) {
  if (lit) {
    ctx.shadowColor = '#88aaff'
    ctx.shadowBlur = 12
  }

  const g = ctx.createRadialGradient(x - 2.5, y - 2.5, 0.5, x, y, PEG_R)
  if (lit) {
    g.addColorStop(0, '#ddeeff')
    g.addColorStop(0.35, '#99bbff')
    g.addColorStop(1, '#1133bb')
  } else {
    g.addColorStop(0, '#c8d0e0')
    g.addColorStop(0.35, '#6677aa')
    g.addColorStop(1, '#10153a')
  }
  ctx.beginPath()
  ctx.arc(x, y, PEG_R, 0, Math.PI * 2)
  ctx.fillStyle = g
  ctx.fill()
  ctx.shadowBlur = 0

  ctx.beginPath()
  ctx.arc(x - 2.2, y - 2.2, PEG_R * 0.28, 0, Math.PI * 2)
  ctx.fillStyle = 'rgba(255,255,255,0.5)'
  ctx.fill()
}

function drawAllPegs(ctx: CanvasRenderingContext2D, litRows: number) {
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < r + 2; c++) {
      drawPeg(ctx, pegX(r, c), pegY(r), r < litRows)
    }
  }
}

function drawSlots(ctx: CanvasRenderingContext2D, slotCounts: number[]) {
  const slotY = TOP_PAD + ROWS * V_SPACING + 4
  const slotW  = H_SPACING - 6
  const slotH  = SLOT_H - 6

  for (let i = 0; i < SLOTS; i++) {
    const cx  = slotCX(i)
    const sx  = cx - slotW / 2
    const cfg = SLOT_CFG[i]
    const cnt = slotCounts[i] ?? 0
    const hot = cnt > 0

    if (hot) {
      ctx.shadowColor = cfg.glow
      ctx.shadowBlur  = 18
    }

    // Slot body
    const g = ctx.createLinearGradient(sx, slotY, sx, slotY + slotH)
    g.addColorStop(0, hot ? cfg.bg + 'ff' : cfg.bg + 'bb')
    g.addColorStop(1, '#000000aa')
    ctx.fillStyle = g
    ctx.beginPath()
    ctx.roundRect(sx, slotY, slotW, slotH, [4, 4, 0, 0])
    ctx.fill()

    // Border
    ctx.strokeStyle = hot ? cfg.border : cfg.border + '66'
    ctx.lineWidth   = hot ? 1.5 : 1
    ctx.stroke()
    ctx.shadowBlur  = 0

    // Multiplier label
    ctx.fillStyle    = hot ? cfg.text : cfg.text + '99'
    ctx.font         = `bold 11px "JetBrains Mono", monospace`
    ctx.textAlign    = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(`${MULTIPLIERS[i]}×`, cx, slotY + slotH * 0.4)

    // Ball count badge
    if (cnt > 0) {
      ctx.fillStyle    = cfg.text
      ctx.font         = `bold 9px sans-serif`
      ctx.textAlign    = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(`${cnt}`, cx, slotY + slotH * 0.76)
    }
  }
}

function drawBall(ctx: CanvasRenderingContext2D, x: number, y: number, color: typeof BALL_PALETTE[0], r: number) {
  if (r <= 0) return

  ctx.shadowColor = color.glow
  ctx.shadowBlur  = r > 4 ? 10 : 6

  if (r <= 3) {
    // Simple filled circle for tiny balls (performance)
    ctx.beginPath()
    ctx.arc(x, y, r, 0, Math.PI * 2)
    ctx.fillStyle = color.main
    ctx.fill()
  } else {
    const g = ctx.createRadialGradient(x - r * 0.35, y - r * 0.35, r * 0.05, x, y, r)
    g.addColorStop(0, color.hi)
    g.addColorStop(0.2, color.main)
    g.addColorStop(0.7, color.main + 'cc')
    g.addColorStop(1, '#000000bb')
    ctx.beginPath()
    ctx.arc(x, y, r, 0, Math.PI * 2)
    ctx.fillStyle = g
    ctx.fill()

    // Specular
    ctx.beginPath()
    ctx.arc(x - r * 0.32, y - r * 0.32, r * 0.22, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(255,255,255,0.65)'
    ctx.fill()
  }
  ctx.shadowBlur = 0
}

function redraw(
  ctx: CanvasRenderingContext2D,
  animBalls: AnimBall[],
  slotCounts: number[],
  litRows: number,
) {
  drawBackground(ctx)
  drawAllPegs(ctx, litRows)
  drawSlots(ctx, slotCounts)
  for (const b of animBalls) {
    drawBall(ctx, b.x, b.y, b.color, b.radius)
  }
}

// ── Animation ──────────────────────────────────────────────────────────────
const TOTAL_SEGS = 1 + ROWS * 2  // waypoints - 1

function startAnimation(balls: PlinkoBall[]) {
  if (animFrame !== null) cancelAnimationFrame(animFrame)

  const count   = balls.length
  const radius  = count <= 5 ? 10 : count <= 15 ? 8 : count <= 40 ? 5 : count <= 100 ? 3.5 : 2.5
  const stagger = count <= 1 ? 0 : Math.min(60, (2200 * 0.55) / (count - 1))
  const segMs   = count <= 5 ? 140 : count <= 15 ? 100 : count <= 40 ? 75 : count <= 100 ? 55 : 38
  const ballMs  = segMs * TOTAL_SEGS

  const slotCounts = new Array<number>(SLOTS).fill(0)

  const animBalls: AnimBall[] = balls.map((b, i) => ({
    waypoints:  buildWaypoints(b.path),
    slot:       b.slot,
    color:      BALL_PALETTE[i % BALL_PALETTE.length]!,
    radius,
    startDelay: i * stagger,
    x:          CANVAS_W / 2,
    y:          TOP_PAD - radius * 5,
    done:       false,
    counted:    false,
  }))

  const ctx = getCtx()
  if (!ctx) return

  const t0 = performance.now()

  function frame(now: number) {
    const elapsed  = now - t0
    let allDone    = true
    let maxLitRows = 0

    for (const ab of animBalls) {
      const be = elapsed - ab.startDelay
      if (be < 0) {
        ab.x = CANVAS_W / 2
        ab.y = TOP_PAD - ab.radius * 5
        allDone = false
        continue
      }

      const rawP = Math.min(be / ballMs, 1)
      if (rawP < 1) allDone = false

      const rawSeg = rawP * TOTAL_SEGS
      const seg    = Math.min(Math.floor(rawSeg), TOTAL_SEGS - 1)
      const t      = rawSeg - seg
      const eased  = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2

      const from = ab.waypoints[seg]!
      const to   = ab.waypoints[seg + 1]!
      ab.x = from.x + (to.x - from.x) * eased
      ab.y = from.y + (to.y - from.y) * eased

      const litRows = Math.floor((seg + 1) / 2)
      if (litRows > maxLitRows) maxLitRows = litRows

      if (rawP >= 1 && !ab.counted) {
        ab.counted = true
        slotCounts[ab.slot]++
      }
      ab.done = rawP >= 1
    }

    redraw(ctx, animBalls, slotCounts, maxLitRows)

    if (!allDone) {
      animFrame = requestAnimationFrame(frame)
    } else {
      animFrame = null
      emit('done')
    }
  }

  animFrame = requestAnimationFrame(frame)
}

// ── Idle draw ──────────────────────────────────────────────────────────────
function drawIdle() {
  const ctx = getCtx()
  if (!ctx) return
  drawBackground(ctx)
  drawAllPegs(ctx, 0)
  drawSlots(ctx, new Array<number>(SLOTS).fill(0))

  ctx.fillStyle    = 'rgba(255,255,255,0.12)'
  ctx.font         = '18px sans-serif'
  ctx.textAlign    = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('▼', CANVAS_W / 2, TOP_PAD - PEG_R * 3)
}

// ── Lifecycle ──────────────────────────────────────────────────────────────
onMounted(drawIdle)

watch(() => props.animating, (nowAnim) => {
  if (nowAnim && props.balls?.length) startAnimation(props.balls)
})

onUnmounted(() => { if (animFrame !== null) cancelAnimationFrame(animFrame) })
</script>

<style scoped>
.plinko-outer {
  perspective: 1100px;
  perspective-origin: 50% -5%;
  display: flex;
  justify-content: center;
}

.plinko-canvas {
  display: block;
  border-radius: 14px;
  transform: rotateX(12deg);
  transform-origin: top center;
  box-shadow:
    0 32px 80px rgba(0, 0, 0, 0.85),
    0 8px 24px rgba(0, 0, 0, 0.6),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset;
  max-width: 100%;
}
</style>
