<template>
  <div class="card">
    <h2 class="text-xl font-bold text-white mb-6">Anmelden</h2>

    <form class="space-y-4" @submit.prevent="handleLogin">
      <div>
        <label class="block text-sm text-gray-400 mb-1.5">Benutzername</label>
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
        <label class="block text-sm text-gray-400 mb-1.5">Passwort</label>
        <input
          v-model="form.password"
          type="password"
          class="input"
          placeholder="Dein Passwort"
          autocomplete="current-password"
          required
        />
      </div>

      <div v-if="error" class="text-red-400 text-sm py-2">{{ error }}</div>

      <button type="submit" class="btn-primary w-full" :disabled="loading">
        {{ loading ? 'Anmelden...' : 'Anmelden' }}
      </button>
    </form>

    <p class="text-center text-gray-500 text-sm mt-4">
      Noch kein Konto?
      <NuxtLink to="/register" class="text-accent hover:text-accent-light">Registrieren</NuxtLink>
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
