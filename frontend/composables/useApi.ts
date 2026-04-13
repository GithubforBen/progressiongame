import { useAuthStore } from '~/stores/auth'

export function useApi() {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()

  function authHeaders() {
    return { Authorization: `Bearer ${authStore.token}` }
  }

  async function get<T>(path: string): Promise<T> {
    return $fetch<T>(`${config.public.apiBase}${path}`, {
      headers: authHeaders(),
    })
  }

  async function post<T>(path: string, body?: unknown): Promise<T> {
    return $fetch<T>(`${config.public.apiBase}${path}`, {
      method: 'POST',
      headers: authHeaders(),
      body,
    })
  }

  async function del(path: string): Promise<void> {
    await $fetch(`${config.public.apiBase}${path}`, {
      method: 'DELETE',
      headers: authHeaders(),
    })
  }

  async function patch<T>(path: string, body?: unknown): Promise<T> {
    return $fetch<T>(`${config.public.apiBase}${path}`, {
      method: 'PATCH',
      headers: authHeaders(),
      body,
    })
  }

  return { get, post, del, patch }
}
