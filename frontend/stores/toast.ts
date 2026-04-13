import { defineStore } from 'pinia'

export interface Toast {
  id: string
  type: 'info' | 'success' | 'error' | 'warning'
  title?: string
  message: string
  duration?: number
}

export const useToastStore = defineStore('toast', {
  state: () => ({
    toasts: [] as Toast[],
  }),

  actions: {
    show(toast: Omit<Toast, 'id'>) {
      const id = Date.now().toString()
      this.toasts.push({ ...toast, id })

      const duration = toast.duration ?? 5000
      if (duration > 0) {
        setTimeout(() => this.dismiss(id), duration)
      }
    },

    success(message: string, title?: string) {
      this.show({ type: 'success', message, title })
    },

    error(message: string, title?: string) {
      this.show({ type: 'error', message, title, duration: 8000 })
    },

    warning(message: string, title?: string) {
      this.show({ type: 'warning', message, title })
    },

    info(message: string, title?: string) {
      this.show({ type: 'info', message, title })
    },

    dismiss(id: string) {
      const idx = this.toasts.findIndex((t) => t.id === id)
      if (idx !== -1) this.toasts.splice(idx, 1)
    },
  },
})
