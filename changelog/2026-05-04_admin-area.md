# 2026-05-04 — Admin Area

## What changed
- **Admin panel** at `/admin` — visible in the sidebar only for the configured admin account.
- Admin can: view all players with their character stats, edit cash and all character stats, delete real estate properties.
- Admin status is config-driven (`.env` variable), not stored in the DB or JWT.

## Update steps

### 1. Pull latest code
```bash
git pull origin main
```

### 2. Set the new environment variable
In your `.env` file, add:
```
ADMIN_USERNAME=<username-of-your-admin-account>
```

The admin account must already be registered in the game. If not, register it first via the normal signup page, then add the variable.

Leave it blank to disable the admin area entirely:
```
ADMIN_USERNAME=
```

### 3. Rebuild and restart
```bash
docker compose build backend frontend
docker compose up -d
```

### 4. Migrations
No new database migrations in this update.

### 5. Verify
- Log in as the admin account.
- The sidebar should show an **Admin** link at the bottom.
- Navigate to `/admin` and confirm the player list loads.
- Test editing a player's cash and confirm the value updates.

## Notes
- Changing `ADMIN_USERNAME` in `.env` requires a **backend restart** to take effect — it is read at startup.
- All `/api/admin/**` endpoints (except `/api/admin/me`) return HTTP 403 if the calling user is not the admin.
- Editing cash automatically recalculates `netWorth` (`cash + investments + real estate`).
- Deleting real estate is a **hard delete** — no cash is refunded to the player.
