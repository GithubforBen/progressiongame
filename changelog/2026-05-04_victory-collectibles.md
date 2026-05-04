# 2026-05-04 — Victory Condition: All Collectibles

## What changed

- Owning every collectible in the game is now a required victory condition.
- `TurnService.checkVictoryCondition` fetches all collectible IDs via `CollectibleRepository` and cross-checks against the player's owned set via `PlayerCollectibleRepository`. All must match.
- The victory screen (`sieg.vue`) shows "Alle Sammlerstücke gesammelt 🗂️" in the achievements checklist.
- Version badge bumped to v13.

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

- No DB changes. The check is a live query each turn end.
- Players who previously met all other conditions but haven't collected every collectible will not receive the victory flag retroactively — it triggers on the next turn end after all conditions are met.
