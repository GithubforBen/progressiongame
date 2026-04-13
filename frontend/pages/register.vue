<template>
  <div class="card">
    <h2 class="text-xl font-bold text-white mb-6">Konto erstellen</h2>

    <form class="space-y-4" @submit.prevent="handleRegister">
      <div>
        <label class="block text-sm text-gray-400 mb-1.5">Benutzername</label>
        <input
          v-model="form.username"
          type="text"
          class="input"
          placeholder="Wähle einen Benutzernamen"
          autocomplete="username"
          required
          minlength="3"
          maxlength="50"
        />
      </div>
      <div>
        <label class="block text-sm text-gray-400 mb-1.5">Passwort</label>
        <input
          v-model="form.password"
          type="password"
          class="input"
          placeholder="Mindestens 8 Zeichen"
          autocomplete="new-password"
          required
          minlength="8"
        />
      </div>
      <div>
        <label class="block text-sm text-gray-400 mb-1.5">Passwort bestätigen</label>
        <input
          v-model="form.passwordConfirm"
          type="password"
          class="input"
          placeholder="Passwort wiederholen"
          autocomplete="new-password"
          required
        />
      </div>

      <div v-if="error" class="text-red-400 text-sm py-2">{{ error }}</div>

      <button type="submit" class="btn-primary w-full" :disabled="loading">
        {{ loading ? 'Wird erstellt...' : 'Konto erstellen' }}
      </button>
    </form>

    <p class="text-center text-gray-500 text-sm mt-4">
      Bereits registriert?
      <NuxtLink to="/login" class="text-accent hover:text-accent-light">Anmelden</NuxtLink>
    </p>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'

definePageMeta({ layout: 'auth' })

const authStore = useAuthStore()
const form = reactive({ username: '', password: '', passwordConfirm: '' })
const loading = ref(false)
const error = ref('')

async function handleRegister() {
  error.value = ''
  if (form.password !== form.passwordConfirm) {
    error.value = 'Passwörter stimmen nicht überein.'
    return
  }
  loading.value = true
  try {
    await authStore.register(form.username, form.password)
    await navigateTo('/')
  }
  catch (e: any) {
    error.value = e?.data?.message ?? 'Registrierung fehlgeschlagen.'
  }
  finally {
    loading.value = false
  }
}
</script>
