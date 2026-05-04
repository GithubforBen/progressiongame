# Changelog

Each file documents the steps needed to update a **running production instance** from the previous state to that release.

## File naming

```
YYYY-MM-DD_short-description.md
```

Use the date the changes were merged/deployed. If multiple unrelated things land the same day, group them into one file.

## File structure

Every changelog file follows this template:

```markdown
# YYYY-MM-DD — Title

## What changed
Brief summary of what was added/changed/fixed.

## Update steps

### 1. Pull latest code
git pull origin main

### 2. New environment variables
(list any new .env keys and whether they are required or optional)

### 3. Rebuild and restart
docker compose build [service] && docker compose up -d [service]

### 4. Migrations (automatic on restart)
List any new Flyway migrations and what they do.
Note if a migration is destructive (e.g. truncates data).

### 5. Manual steps
(anything else the operator must do by hand)

## Notes
Edge cases, warnings, rollback considerations.
```

## Rules for agents

- **Create a new changelog file every session** that produces a deployable change.
- If a session only edits `agent.md` or documentation with no backend/frontend change, no changelog entry is needed.
- A session with multiple unrelated features still gets **one file** for that date.
- If a migration is destructive (drops columns, truncates tables), call it out explicitly under a `> Warning` block.
- After writing the changelog, update `agent.md` §3 (migration table) and §6 (feature list) as needed.
