/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './components/**/*.{js,vue,ts}',
    './layouts/**/*.vue',
    './pages/**/*.vue',
    './plugins/**/*.{js,ts}',
    './app.vue',
    './error.vue',
  ],
  theme: {
    extend: {
      colors: {
        surface: {
          950: '#0a0a0a',
          900: '#111111',
          800: '#181818',
          700: '#222222',
          600: '#2c2c2c',
          500: '#383838',
        },
        accent: {
          DEFAULT: '#d4321f',
          light: '#e8614e',
          hover: '#b42a18',
          bg: 'rgba(212,50,31,0.12)',
          border: 'rgba(212,50,31,0.38)',
        },
      },
      fontFamily: {
        sans: ['Space Grotesk', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
    },
  },
  plugins: [],
}
