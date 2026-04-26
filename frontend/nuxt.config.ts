export default defineNuxtConfig({
  devtools: { enabled: process.env.NODE_ENV !== 'production' },

  modules: [
    '@nuxtjs/tailwindcss',
    '@pinia/nuxt',
  ],

  css: ['~/assets/css/main.css'],

  runtimeConfig: {
    // Private – nur serverseitig (SSR → Docker-internes Netz)
    apiBase: process.env.NUXT_INTERNAL_API_BASE || process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080',
    public: {
      // Public – clientseitig (Browser → Cloudflare Tunnel)
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080',
    },
  },

  app: {
    head: {
      title: 'FinanzLeben',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
      ],
    },
  },

  router: {
    middleware: ['auth'],
  },

  ssr: false,

  nitro: {
    preset: 'node-server',
  },

  compatibilityDate: '2024-11-01',
})
