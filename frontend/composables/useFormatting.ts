export function useFormatting() {
  function formatCurrency(value: number) {
    return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
  }

  function formatEducationRequirement(type: string | null, field: string | null): string {
    if (!type) return 'Keine Voraussetzung'
    const labels: Record<string, string> = {
      REALSCHULABSCHLUSS: 'Realschulabschluss',
      ABITUR: 'Abitur',
      AUSBILDUNG: 'Ausbildung',
      BACHELOR: 'Bachelor',
      MASTER: 'Master',
      STUDIUM: 'Studium',
      WEITERBILDUNG: 'Weiterbildung',
    }
    const fieldLabels: Record<string, string> = {
      INFORMATIK: 'Informatik',
      BWL: 'BWL',
      MEDIZIN: 'Medizin',
      FACHINFORMATIKER: 'Fachinformatiker',
      SOCIAL_MEDIA: 'Social Media Marketing',
      EXCEL: 'Excel',
      FUEHRERSCHEIN: 'Führerschein',
      CRYPTO: 'Crypto Trading',
    }
    const base = labels[type] ?? type
    return field ? `${base}: ${fieldLabels[field] ?? field}` : base
  }

  function stressLabel(stress: number): string {
    if (stress <= 10) return 'Niedrig'
    if (stress <= 20) return 'Mittel'
    return 'Hoch'
  }

  function stressColor(stress: number): string {
    if (stress <= 10) return 'text-green-400 bg-green-400/10'
    if (stress <= 20) return 'text-yellow-400 bg-yellow-400/10'
    return 'text-red-400 bg-red-400/10'
  }

  return { formatCurrency, formatEducationRequirement, stressLabel, stressColor }
}
