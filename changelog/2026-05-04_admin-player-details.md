# 2026-05-04 — Admin: Player Detail Overview

## What changed

- New endpoint `GET /api/admin/players/{playerId}/details` returns read-only per-player details:
  - **Sammlerstücke**: all owned collectibles (name, rarity, collection, country), sorted by collection then name
  - **Beziehungen**: all unlocked social relationships with person name and score (sorted by score desc)
  - **Reisen**: current country, in-transit destination + arrival turn, list of visited countries
  - **Investitionen**: all investments (name, type, invested, current value), sorted by value desc
  - **Ausbildung**: completed education stage keys
- Admin panel fetches details automatically when a player is selected and renders them in a second row of cards below the existing character editor / real estate panel.
- Rarity badge colors: LEGENDARY = yellow, EPIC = purple, RARE = blue, COMMON = gray.
- Version badge bumped to v14.

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
None.

### 5. Manual steps
None.

## Notes

- All detail data is read-only. No editing or deletion is provided in these new sections.
- If a player has no record for a section (e.g. never traveled), the section shows an empty state.
