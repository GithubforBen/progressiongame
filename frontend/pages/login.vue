<template>
  <div
    class="bg-surface-900 rounded-2xl p-9"
    style="border: 1px solid rgba(255,255,255,0.1)"
  >
    <form class="space-y-5" @submit.prevent="handleLogin">
      <div>
        <label class="block text-xs font-medium text-gray-400 mb-1.5 uppercase tracking-wide">Benutzername</label>
        <input
          v-model="form.username"
          type="text"
          class="input"
          placeholder="Dein Benutzername"
          autocomplete="username"
          required
        />
      </div>
      <div>
        <label class="block text-xs font-medium text-gray-400 mb-1.5 uppercase tracking-wide">Passwort</label>
        <input
          v-model="form.password"
          type="password"
          class="input"
          placeholder="••••••••"
          autocomplete="current-password"
          required
          @keydown.enter="handleLogin"
        />
      </div>

      <div v-if="error" class="text-red-400 text-sm py-1">{{ error }}</div>

      <button
        type="submit"
        class="w-full py-2.5 rounded-lg text-sm font-bold text-white transition-all tracking-wide border-none cursor-pointer mt-1"
        style="background: var(--accent)"
        :disabled="loading"
        @mouseenter="e => e.currentTarget.style.background = 'var(--accent-hover)'"
        @mouseleave="e => e.currentTarget.style.background = 'var(--accent)'"
      >
        {{ loading ? 'Anmelden...' : 'Anmelden' }}
      </button>
    </form>

    <p class="text-center text-gray-600 text-sm mt-6">
      Noch kein Konto?
      <NuxtLink
        to="/register"
        class="transition-colors"
        style="color: var(--accent)"
        @mouseenter="e => e.currentTarget.style.color = 'var(--accent-light)'"
        @mouseleave="e => e.currentTarget.style.color = 'var(--accent)'"
      >Registrieren</NuxtLink>
    </p>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'

definePageMeta({ layout: 'auth' })

const authStore = useAuthStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    await navigateTo('/')
  }
  catch (e: any) {
    error.value = e?.data?.message ?? 'Anmeldung fehlgeschlagen. Bitte überprüfe deine Daten.'
  }
  finally {
    loading.value = false
  }
}
</script>
