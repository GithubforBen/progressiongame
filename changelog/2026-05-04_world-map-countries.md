# 2026-05-04 — World Map: Add Missing Countries

## What changed

- **V23 migration**: Added 24 new countries to the `countries` table so every collectible's `countryRequired` now has a matching travel destination:
  - New: Ägypten, Argentinien, Australien, Belgien, Brasilien, Dänemark, Frankreich, Griechenland, Guatemala, Guyana, Indien, Kanada, Mauritius, Mexiko, Niederlande, Österreich, Pakistan, Peru, Russland, Spanien, Südafrika, Tibet, Tschechien, Türkei
  - `ON CONFLICT DO NOTHING` — safe to run on existing installations.
- **Deutschland collectibles**: purchasable when at home (currentCountry = null). No backend country entry needed for the home country.
- **Internet collectibles**: purchasable from anywhere regardless of current location.
- **World map** (`reisen.vue`):
  - 15 new ISO codes added to `GAME_ISO_SET` (country shapes now highlighted).
  - Centroids added for all 24 new countries (pins render at correct geographic positions).
  - Fixed broken `COUNTRY_COORDS` reference (undefined variable) → replaced with `coords()` call so the travel route line now renders correctly.
  - Emoji map extended to cover all new countries.
- **Sammlungen page**: emoji map extended; "Internet" collectibles now show "🌐 Überall verfügbar" instead of a travel requirement.
- Version badge bumped to v15.

## Update steps

### 1. Pull latest code
```bash
git pull origin main
```

### 2. New environment variables
None.

### 3. Rebuild and restart
```bash
docker compose build backend frontend && docker compose up -d
```

### 4. Migrations
V23 runs automatically on backend startup. No manual steps needed.

### 5. Manual steps
None.

## Notes

- Tibet has no ISO 3166-1 numeric code (it is part of China). Its map pin is placed at Lhasa coordinates [91.1°E, 29.6°N] over the highlighted China shape.
- Guyana and Mauritius are small/island nations — their shapes may not be visible in the 110m TopoJSON used for the map, but their pins render at the correct coordinates.
