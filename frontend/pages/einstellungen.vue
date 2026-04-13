<template>
  <div class="space-y-6">
    <h2 class="text-xl font-bold text-white">Einstellungen</h2>

    <div class="card max-w-md">
      <h3 class="text-sm font-semibold text-white uppercase tracking-wider mb-4">Konto</h3>
      <button class="btn-danger text-sm" @click="handleLogout">Abmelden</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'

definePageMeta({ layout: 'default' })

const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
}
</script>
