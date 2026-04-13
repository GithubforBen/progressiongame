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
          950: '#08090e',
          900: '#0f1117',
          800: '#1a1d27',
          700: '#242736',
          600: '#2e3347',
          500: '#3a4060',
        },
        accent: {
          DEFAULT: '#6366f1',
          light: '#818cf8',
          hover: '#4f46e5',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
    },
  },
  plugins: [],
}
