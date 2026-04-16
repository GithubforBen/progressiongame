import { useAuthStore } from '~/stores/auth'

export function useApi() {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()

  const baseUrl = import.meta.server ? config.apiBase : config.public.apiBase

  function authHeaders() {
    return { Authorization: `Bearer ${authStore.token}` }
  }

  function handleError(err: unknown): never {
    const status = (err as any)?.response?.status ?? (err as any)?.status
    if (status === 401) {
      authStore.logout()
    }
    throw err
  }

  async function get<T>(path: string): Promise<T> {
    try {
      return await $fetch<T>(`${baseUrl}${path}`, { headers: authHeaders() })
    } catch (err) {
      return handleError(err)
    }
  }

  async function post<T>(path: string, body?: unknown): Promise<T> {
    try {
      return await $fetch<T>(`${baseUrl}${path}`, {
        method: 'POST',
        headers: authHeaders(),
        body,
      })
    } catch (err) {
      return handleError(err)
    }
  }

  async function del(path: string): Promise<void> {
    try {
      await $fetch(`${baseUrl}${path}`, {
        method: 'DELETE',
        headers: authHeaders(),
      })
    } catch (err) {
      return handleError(err)
    }
  }

  async function patch<T>(path: string, body?: unknown): Promise<T> {
    try {
      return await $fetch<T>(`${baseUrl}${path}`, {
        method: 'PATCH',
        headers: authHeaders(),
        body,
      })
    } catch (err) {
      return handleError(err)
    }
  }

  return { get, post, del, patch }
}
