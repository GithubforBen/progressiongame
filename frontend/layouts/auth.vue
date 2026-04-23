<template>
  <div class="min-h-screen bg-surface-950 flex items-center justify-center p-4 relative overflow-hidden">
    <!-- Background glow -->
    <div
      class="absolute inset-0 pointer-events-none"
      style="background: radial-gradient(ellipse 600px 400px at 50% 35%, oklch(0.58 0.22 18 / 0.06), transparent)"
    />
    <div class="w-full max-w-sm relative z-10">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-extrabold tracking-tighter" style="color: var(--accent)">FinanzLeben</h1>
        <p class="text-gray-600 mt-2 text-sm">Dein finanzielles Leben wartet</p>
      </div>
      <slot />
    </div>
  </div>
</template>
