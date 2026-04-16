export function useFormatting() {
  function formatCurrency(value: number) {
    return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
  }

  function _eduFieldLabel(field: string): string {
    const labels: Record<string, string> = {
      INFORMATIK: 'Informatik', BWL: 'Betriebswirtschaft', MEDIZIN: 'Medizin',
      JURA: 'Rechtswissenschaften', INGENIEURWESEN: 'Ingenieurwesen', PSYCHOLOGIE: 'Psychologie',
      FACHINFORMATIKER: 'Fachinformatiker/-in', EINZELHANDEL: 'Einzelhandelskaufmann/-frau',
      KFZTECH: 'KFZ-Mechatronik', PFLEGE: 'Pflegefachkraft', KOCH: 'Koch/Köchin',
      ELEKTRIKER: 'Elektriker/-in', SOCIAL_MEDIA: 'Social-Media-Marketing',
      EXCEL: 'Excel & Datenanalyse', FUEHRERSCHEIN: 'Führerschein Klasse B',
      CRYPTO: 'Krypto-Trading',
    }
    return labels[field] ?? field
  }

  function formatEducationRequirement(type: string | null, field: string | null): string {
    if (!type) return 'Keine Voraussetzung'
    // Full compound stage key (new format)
    if (type === 'REALSCHULABSCHLUSS') return 'Realschulabschluss'
    if (type === 'ABITUR') return 'Abitur'
    if (type === 'GRUNDSCHULE') return 'Grundschule'
    if (type.startsWith('AUSBILDUNG_')) return 'Ausbildung: ' + _eduFieldLabel(type.substring(11))
    if (type.startsWith('BACHELOR_'))  return 'Bachelor: '   + _eduFieldLabel(type.substring(9))
    if (type.startsWith('MASTER_'))    return 'Master: '     + _eduFieldLabel(type.substring(7))
    // Legacy: separate type + field
    const typeLabels: Record<string, string> = {
      AUSBILDUNG: 'Ausbildung', BACHELOR: 'Bachelor', MASTER: 'Master', WEITERBILDUNG: 'Weiterbildung',
    }
    const base = typeLabels[type] ?? type
    return field ? `${base}: ${_eduFieldLabel(field)}` : base
  }

  function stressLabel(stress: number): string {
    if (stress <= 15) return 'Niedrig'
    if (stress <= 30) return 'Mittel'
    if (stress <= 55) return 'Hoch'
    return 'Sehr hoch'
  }

  function stressColor(stress: number): string {
    if (stress <= 15) return 'text-green-400 bg-green-400/10'
    if (stress <= 30) return 'text-yellow-400 bg-yellow-400/10'
    if (stress <= 55) return 'text-orange-400 bg-orange-400/10'
    return 'text-red-400 bg-red-400/10'
  }

  function formatSchufaScore(score: number): { label: string; color: string; bgColor: string } {
    if (score >= 800) return { label: 'Ausgezeichnet', color: 'text-green-400', bgColor: 'bg-green-400' }
    if (score >= 600) return { label: 'Gut', color: 'text-blue-400', bgColor: 'bg-blue-400' }
    if (score >= 400) return { label: 'Befriedigend', color: 'text-yellow-400', bgColor: 'bg-yellow-400' }
    return { label: 'Mangelhaft', color: 'text-red-400', bgColor: 'bg-red-400' }
  }

  function formatLoanRate(rate: number): string {
    return (rate * 100).toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' % p.a.'
  }

  // Maps a WEITERBILDUNG_* cert key to a human-readable label
  function certLabel(key: string | null | undefined): string {
    if (!key) return ''
    const labels: Record<string, string> = {
      WEITERBILDUNG_BARKEEPER_1: 'Barkeeper-Kurs (Grundlagen)',
      WEITERBILDUNG_BARKEEPER_2: 'Barkeeper-Kurs (Fortgeschritten)',
      WEITERBILDUNG_BARKEEPER_3: 'Bar-Manager Zertifikat',
      WEITERBILDUNG_FITNESSTRAINER_1: 'Fitnesstrainer B-Lizenz',
      WEITERBILDUNG_FITNESSTRAINER_2: 'Fitnesstrainer A-Lizenz',
      WEITERBILDUNG_FITNESSTRAINER_3: 'Personal Trainer Zertifikat',
      WEITERBILDUNG_SOCIAL_MEDIA_1: 'Social-Media-Marketing (Grundkurs)',
      WEITERBILDUNG_SOCIAL_MEDIA_2: 'Social-Media-Marketing (Aufbaukurs)',
      WEITERBILDUNG_SOCIAL_MEDIA_3: 'Social-Media-Marketing (Expertenzertifikat)',
      WEITERBILDUNG_EXCEL_1: 'Excel & Datenanalyse (Grundkurs)',
      WEITERBILDUNG_EXCEL_2: 'Excel & Power BI (Aufbaukurs)',
      WEITERBILDUNG_EXCEL_3: 'Data Analyst Zertifikat (Microsoft)',
      WEITERBILDUNG_FUEHRERSCHEIN_1: 'Führerschein Klasse B',
      WEITERBILDUNG_FUEHRERSCHEIN_2: 'Führerschein Klasse BE + Anhänger',
      WEITERBILDUNG_FUEHRERSCHEIN_3: 'LKW-Führerschein Klasse C+E',
      WEITERBILDUNG_CRYPTO_1: 'Krypto-Trading Zertifikat (Grundlagen)',
      WEITERBILDUNG_CRYPTO_2: 'DeFi & Blockchain Zertifikat',
      WEITERBILDUNG_CRYPTO_3: 'Certified Crypto Analyst (CCA)',
      WEITERBILDUNG_BUCHHALTUNG_1: 'Buchhaltung & DATEV (Grundkurs)',
      WEITERBILDUNG_BUCHHALTUNG_2: 'Bilanzbuchhaltung (IHK)',
      WEITERBILDUNG_BUCHHALTUNG_3: 'Bilanzbuchhalter Zertifikat',
      WEITERBILDUNG_IMMOBILIEN_1: 'Immobilien-Grundlagen (IHK)',
      WEITERBILDUNG_IMMOBILIEN_2: 'Immobilienmakler-Lizenz',
      WEITERBILDUNG_IMMOBILIEN_3: 'Immobilien-Investor Masterclass',
      WEITERBILDUNG_IMMOBILIEN_4: 'Immobilien-Portfolio Manager',
      WEITERBILDUNG_PROJEKTMANAGEMENT_1: 'Projektmanagement Grundlagen (PMI)',
      WEITERBILDUNG_PROJEKTMANAGEMENT_2: 'Projektmanagement (PRINCE2 Foundation)',
      WEITERBILDUNG_PROJEKTMANAGEMENT_3: 'PMP Zertifizierung',
      WEITERBILDUNG_STEUERN_1: 'Steuerlehre Grundkurs',
      WEITERBILDUNG_STEUERN_2: 'Steuerberater-Vorbereitung',
      WEITERBILDUNG_STEUERN_3: 'Steuerberater-Examen',
      WEITERBILDUNG_HACKER_1: 'IT-Security Grundlagen',
      WEITERBILDUNG_HACKER_2: 'Ethical Hacking Zertifikat (CEH)',
      WEITERBILDUNG_HACKER_3: 'OSCP Penetration Testing',
      WEITERBILDUNG_OLDTIMER_1: 'Oldtimer-Kurs Grundlagen',
      WEITERBILDUNG_OLDTIMER_2: 'Classic-Car Experte',
      WEITERBILDUNG_OLDTIMER_3: 'Oldtimer-Auktionator Zertifikat',
      WEITERBILDUNG_ARCHAEOLOGIE_1: 'Archäologen-Hobbykurs',
      WEITERBILDUNG_ARCHAEOLOGIE_2: 'Antiquitäten-Experte',
      WEITERBILDUNG_WEINKENNER_1: 'Weinkenner Grundkurs',
      WEITERBILDUNG_WEINKENNER_2: 'Wine & Spirit Education (WSET)',
      WEITERBILDUNG_WEINKENNER_3: 'Master Sommelier',
      WEITERBILDUNG_KUNSTKENNER_1: 'Kunstgeschichte Einführung',
      WEITERBILDUNG_KUNSTKENNER_2: 'Kunstmarkt-Experte',
      WEITERBILDUNG_KUNSTKENNER_3: 'Art Advisor Zertifikat',
      WEITERBILDUNG_UHRMACHER_1: 'Uhrmacher-Grundkurs',
      WEITERBILDUNG_UHRMACHER_2: 'Zertifizierter Uhrenexperte',
      WEITERBILDUNG_UHRMACHER_3: 'Horologie Diplom',
      WEITERBILDUNG_NUMISMATIK_1: 'Münzkunde Grundkurs',
      WEITERBILDUNG_NUMISMATIK_2: 'Professioneller Numismatiker',
      WEITERBILDUNG_PHILATELIE_1: 'Briefmarken-Sammler Kurs',
      WEITERBILDUNG_PHILATELIE_2: 'Philatelie-Experte',
      WEITERBILDUNG_MINERALIEN_1: 'Gemmologie Grundkurs',
      WEITERBILDUNG_MINERALIEN_2: 'Zertifizierter Gemmologe (FGA)',
      WEITERBILDUNG_MINERALIEN_3: 'Diamond Grading Expert',
      WEITERBILDUNG_SPORTSAMMLER_1: 'Sport-Memorabilia Grundkurs',
      WEITERBILDUNG_SPORTSAMMLER_2: 'Sportartefakt-Authentifizierer',
      WEITERBILDUNG_WHISKY_1: 'Whisky & Spirituosen Grundkurs',
      WEITERBILDUNG_WHISKY_2: 'Master Distiller Zertifikat',
    }
    return labels[key] ?? key
  }

  return { formatCurrency, formatEducationRequirement, stressLabel, stressColor, formatSchufaScore, formatLoanRate, certLabel }
}
