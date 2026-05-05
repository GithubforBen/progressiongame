# FinanzLeben — Agent Reference

> Read this file completely before touching any code.  
> After every session: bump the version badge in `frontend/layouts/default.vue` to `v(N+1)`.  
> After every session that changes code: create a git commit (§9), write a changelog entry (§10), and update this file (§11).

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Setup & Pull](#2-setup--pull)
3. [Migrations — Which to Run](#3-migrations--which-to-run)
4. [Core Architecture](#4-core-architecture)
5. [Implementing a New Feature](#5-implementing-a-new-feature)
6. [Implemented Features & DB State](#6-implemented-features--db-state)
7. [Open TODOs / Technical Debt](#7-open-todos--technical-debt)
8. [Key File Paths](#8-key-file-paths)
9. [Committing Changes](#9-committing-changes)
10. [Changelog](#10-changelog)
11. [Editing This File](#11-editing-this-file)

---

## 1. Project Overview

**FinanzLeben** is a browser-based, turn-based passive financial life simulation.

| Layer | Technology |
|---|---|
| Frontend | Nuxt.js 3 + Tailwind CSS (dark theme, desktop-first, UI in German) |
| Backend | Java 21 + Spring Boot 3.3 + Hibernate ORM **directly** (no Spring Data JPA) |
| Database | PostgreSQL 15, schema managed by Flyway |
| Auth | JWT (jjwt 0.12.x), stateless, BCrypt passwords |
| Deploy | Docker Compose (`docker-compose.yml` in repo root) |
| Proxy | Cloudflare Tunnel compatible (`NUXT_PUBLIC_API_BASE` env var, X-Forwarded-* headers) |

```
/home/bestimmtnichtben/Documents/game/
├── docker-compose.yml
├── agent.md                  ← you are here
├── promt.md                  ← full Social-Rework plan (cold-start context for that feature)
├── install.sh
├── migrate.sh
├── backend/
└── frontend/
```

---

## 2. Setup & Pull

### Pull latest code

```bash
git pull origin main
```

If there are conflicts in migration files, **never** auto-merge — investigate manually.

### First-time / fresh environment

```bash
# 1. Copy env file and set secrets
cp .env.example .env   # fill JWT_SECRET, DB passwords

# 2. Build and start all containers
docker compose build
docker compose up -d

# Migrations run automatically on backend startup (Flyway).
```

### Rebuild after backend changes

```bash
docker compose build backend && docker compose up -d backend
```

### Rebuild after frontend changes

```bash
docker compose build frontend && docker compose up -d frontend
```

### Compile check (no Docker)

```bash
# Maven wrapper is under ~/.m2/wrapper/dists/ — use the full path:
~/.m2/wrapper/dists/apache-maven-3.9.9/3477a4f1/bin/mvn compile -f backend/pom.xml
```

### Ports

| Service | Port |
|---|---|
| Frontend (Nuxt) | 3000 |
| Backend (Spring) | 8080 |
| PostgreSQL | 5432 |

---

## 3. Migrations — Which to Run

Flyway runs automatically on startup. After a `git pull` that includes new migration files, a container restart is sufficient:

```bash
docker compose restart backend
```

**Never edit an existing migration.** New schema changes always go in a new file:

```
backend/src/main/resources/db/migration/V{N+1}__short_description.sql
```

### Migration history

| File | Contents |
|---|---|
| V1 | Initial schema — all core tables + seed data |
| V2 | Investments + stock price history |
| V3 | Travel, collectibles, active events |
| V4 | Gambling (`gambling_sessions`) |
| V5 | Relationship system (`npcs`, `player_relationships`) |
| V6 | Fix NPC ID types |
| V7 | Real estate, loans, SCHUFA (`real_estate_catalog`, `player_real_estate`, `player_loans`, `schufa_score`) |
| V8 | Job catalog (`category`, `max_parallel`, `required_side_cert` on jobs) |
| V9 | Level system + collections (`collections`, `player_collections`, `player_collectibles`, `collection_name` on collectibles) |
| V10 | Cert-based unlock system (`required_cert` on stocks / real_estate_catalog / collectibles) |
| V11 | Extended stock types (DIVIDEND_STOCK, BOND, REIT, LEVERAGE, WARRANT, SHORT, FUTURES) |
| V12 | Needs system (`needs_items`, `depression_months_remaining` + `burnout_active` on characters) |
| V13 | Tax evasion (`tax_evasion_active`, `tax_evasion_caught_pending`, `cumulative_evaded_taxes`, `jail_months_remaining`, `exile_months_remaining` on characters) |
| V14 | Social rework (`player_social_relationships`, `player_social_group_unlocks`, `social_action_log`, `total_jail_months_served` on characters) |
| V15 | Per-player stock price history — adds `player_id` to `stock_price_history`, **truncates** existing shared price history |
| V16 | `monthly_gift_done` boolean on `player_social_relationships` |
| V17 | Lifestyle items (`lifestyle_item_catalog`, `player_lifestyle_items` tables) |
| V18 | Victory condition — `victory_achieved` + `personal_best_net_worth` on `characters` |
| V19 | Finanzamt audit — `finanzamt_audit_months_remaining` on `characters` |
| V20 | Needs cooldown — `cooldown_turns` on `needs_items`; new `player_needs_usage` table |
| V21 | Remove precision limits — all monetary `NUMERIC(p,s)` columns changed to unconstrained `NUMERIC` |
| V22 | Stock delisting — adds `initial_price` to `stocks`; creates `player_delisted_stocks` table |
| V23 | Add countries — inserts 24 missing countries into `countries` table; Deutschland/Internet handled as special cases in `CollectibleService` |

**Next migration is V24.**

### If you need to apply a migration manually (emergency only)

```bash
docker compose exec db psql -U financegame -d financegame -f /path/to/VN.sql
```

---

## 4. Core Architecture

### 4.1 Backend — Hibernate direct, no Spring Data JPA

Every repository injects `EntityManager` via `@PersistenceContext` and exposes explicit methods.  
**Never** introduce `JpaRepository` or `CrudRepository`.

```java
@Repository
public class MyEntityRepository {
    @PersistenceContext
    private EntityManager em;

    public Optional<MyEntity> findById(Long id) {
        return Optional.ofNullable(em.find(MyEntity.class, id));
    }
}
```

### 4.2 Transactions

`@Transactional` belongs on **service methods**, not on repositories.  
Controllers are never `@Transactional` (except `TurnController` which has a redundant one — known debt, see §7).

### 4.3 Auth — PlayerPrincipal

Every secured endpoint extracts the player ID from the JWT via `@AuthenticationPrincipal`:

```java
@GetMapping("/something")
public ResponseEntity<Dto> method(@AuthenticationPrincipal PlayerPrincipal principal) {
    Long playerId = principal.id();
    // ...
}
```

### 4.4 Domain Event System (loose coupling between services)

**18 events, 7 listeners.** Services never call each other directly for side effects — they publish events.

All events are Java Records in `com.financegame.domain.events`, implementing `DomainEvent` (interface: `playerId()` + `occurredAt()`). A convenience constructor sets `Instant.now()`.

| Event | Published by | When |
|---|---|---|
| `TurnEndedEvent` | TurnService | After full turn processing |
| `JobApplicationResolvedEvent` | TurnService | Application accepted/rejected |
| `EducationStageCompletedEvent` | TurnService | Main education stage done |
| `SideCertCompletedEvent` | TurnService | Side certificate done |
| `TravelArrivedEvent` | TurnService | Player arrives at destination |
| `TravelDepartedEvent` | TravelService | Player books a flight |
| `LoanTakenEvent` | LoanService | Loan taken out |
| `LoanPaidOffEvent` | TurnService | Loan fully paid |
| `LoanDefaultedEvent` | TurnService | Loan defaulted |
| `CollectiblePurchasedEvent` | CollectibleService, CollectionService | Collectible bought |
| `CollectionCompletedEvent` | CollectionEventListener (secondary) | Collection completed |
| `PropertyPurchasedEvent` | RealEstateService | Property purchased |
| `PropertyModeChangedEvent` | RealEstateService | Mode changed (rent/own) |
| `StockPurchasedEvent` | InvestmentService | Stock bought |
| `StockSoldEvent` | InvestmentService | Stock sold |
| `TaxEvasionCaughtEvent` | TurnService | Tax investigation caught player |
| `PlayerRegisteredEvent` | AuthService | Registration completed |

**Listener rules:**
- `@TransactionalEventListener(phase = AFTER_COMMIT)` — always when listener writes to DB.
- `@Transactional(propagation = REQUIRES_NEW)` in listener — always combined with AFTER_COMMIT + DB write.
- `@EventListener` (no binding) — only for pure in-memory reactions.
- `@Async` — only for non-critical side effects. `@EnableAsync` is in `FinanceGameApplication`, thread pool in `AsyncConfig`.
- Listeners may inject repositories directly, **never** services from other modules.

**Adding a new event:**

```java
// 1. domain/events/MyEvent.java
public record MyEvent(Long playerId, String detail, Instant occurredAt) implements DomainEvent {
    public MyEvent(Long playerId, String detail) { this(playerId, detail, Instant.now()); }
}

// 2. In service — inject ApplicationEventPublisher via constructor, publish after DB save:
eventPublisher.publishEvent(new MyEvent(playerId, "some detail"));

// 3. listener/MyEventListener.java
@Component
public class MyEventListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(MyEvent event) { /* write to DB */ }
}
```

### 4.5 Condition System (access/unlock checks)

All access restrictions (cert, SCHUFA, jail, travel) are expressed via a single `Condition` interface instead of scattered if-else chains.

```java
// domain/condition/Condition.java
public interface Condition {
    boolean isMet(GameContext context);
    String describe();
}
```

**Available implementations:**

| Class | What it checks |
|---|---|
| `HasCertCondition(certKey)` | `completedEducationStages.contains(certKey)` |
| `MinSchufaCondition(min)` | `character.schufaScore >= min` |
| `NotInJailCondition()` | `character.jailMonthsRemaining == 0` |
| `NotTravelingCondition()` | `!context.traveling()` |
| `EducationLevelCondition(prefix)` | completedStages contains prefix or begins with `prefix + "_"` |
| `MinNetWorthCondition(amount)` | net worth >= amount |
| `OwnsCollectionCondition(id)` | player has completed collection with id |
| `HasRelationshipScoreCondition(personId, min)` | social score >= min |
| `HadConflictWithCondition(personId)` | player has had conflict with person |
| `MinJailMonthsServedCondition(min)` | totalJailMonthsServed >= min |
| `AllOfCondition(c1, c2, ...)` | AND — all must be met |
| `AnyOfCondition(c1, c2, ...)` | OR — at least one must be met |
| `NotCondition(c)` | inversion |

**GameContext** is the snapshot passed to every condition:

```java
public record GameContext(
    GameCharacter character,
    List<String> completedEducationStages,
    String currentCountry,
    boolean traveling,
    int activeJobCount,
    Set<String> completedCollections,
    Map<String, Integer> relationshipScores,
    Set<String> hadConflictsWith
) {}
```

Build it via `gameContextFactory.build(playerId)` — reads character, education, travel, jobs, social state from DB.

**Adding a new condition:** Create class in `domain/condition/`, implement `Condition`. No existing code changes needed.

### 4.6 CollectionBonusApplier (Strategy for collection bonuses)

New bonus types can be added without touching `TurnService`.

```java
// domain/effect/collection/CollectionBonusApplier.java
public interface CollectionBonusApplier {
    String getBonusType();
    default BigDecimal modifyIncome(BigDecimal income, BigDecimal bonusValue,
                                    List<TurnResultDto.LineItem> breakdown) { return income; }
    default BigDecimal modifyExpenses(BigDecimal expenses, BigDecimal bonusValue) { return expenses; }
    default void applyStats(GameCharacter character, BigDecimal bonusValue) {}
}
```

**Existing implementations:** `SalaryMultiplierApplier`, `MonthlyIncomeBonusApplier`, `ExpenseReductionApplier`, `HappinessBonusApplier`, `SchufaBonusApplier`.

`TurnService` auto-discovers all `@Component` implementors and maps them by `getBonusType()`.

**Adding a new bonus type:**
1. Create class in `domain/effect/collection/`, annotate `@Component`, implement the interface.
2. Override only the relevant method (`modifyIncome`, `modifyExpenses`, or `applyStats`).
3. No existing code changes needed.

### 4.7 RandomGameEvent (Strategy for monthly random events)

New random monthly events are standalone `@Component` classes. `RandomEventService` injects `List<RandomGameEvent>` automatically.

```java
// domain/event/RandomGameEvent.java
public interface RandomGameEvent {
    void tryApply(Long playerId, GameCharacter character, List<String> events);
    // implementation rolls its own probability internally
}
```

**Existing:** `GluecksfallEvent`, `DiebstahlEvent`, `GehaltsbonusEvent`, `AutopanneEvent`, `UnerwarteteRechnungEvent`, `StressabbauEvent`.

**Adding a new event:**
1. Create class in `domain/event/`, annotate `@Component`, implement the interface.
2. Roll your own probability in `tryApply()` (e.g. `Math.random() < 0.05`), mutate character on hit, append a message string to `events`.
3. No existing code changes needed.

### 4.8 GameConfig — All game numbers in application.yml

Balance values (tax, interest, volatility, decay…) live in `application.yml` under `game:`, bound via `@ConfigurationProperties(prefix = "game")` in `GameConfig.java`. No recompile needed to change a number.

```yaml
game:
  stock-volatility:           # Map<String, Double> — type → volatility (0..1)
    MEME: 0.80
    ETF: 0.05
    NORMAL: 0.15              # unknown types fall back to 0.15 in StockService

  stock-reversion-speed:      # Map<String, Double> — OU mean-reversion θ per type (0..1)
    BOND: 0.15                # strong reversion (stable)
    MEME: 0.02                # near-zero reversion (wild); fallback 0.08

  stock-delisting:
    threshold-fraction: 0.10  # delist at 10% of initial price (or <0.01 for penny stocks)
    min-delist-turns: 6       # minimum months before relist is possible
    relist-chance-per-turn: 0.20  # 20% chance per month to relist after min wait

  needs:
    hunger-decay-base: 15
    hunger-decay-with-food: 5
    energy-decay: 8
    happiness-decay: 5

  burnout:
    stress-trigger: 100
    stress-reset: 70          # was 50 before recent fix
    recovery-threshold: 40
    hospital-penalty: 100

  depression:
    duration-months: 3
    stress-per-month: 3

  loan:
    min-schufa: 300
    min-amount: 1000
    interest-tiers:            # sort descending by min-score!
      - { min-score: 800, rate: 3.0,  label: "Ausgezeichnet" }
      - { min-score: 600, rate: 5.0,  label: "Gut" }
      - { min-score: 400, rate: 8.0,  label: "Befriedigend" }
      - { min-score: 0,   rate: 12.0, label: "Mangelhaft" }

  schufa:
    education-bonuses:         # Map<String, Integer>: stage-prefix → SCHUFA bonus
      MASTER: 80
      BACHELOR: 60
      ABITUR: 40
      AUSBILDUNG: 30
      REALSCHULABSCHLUSS: 20

  tax-evasion:
    levels:                    # index = level-1
      - { cert-suffix: "STEUERHINTERZIEHUNG_1", evasion-rate: 0.20, detection-chance: 0.15 }
      - { cert-suffix: "STEUERHINTERZIEHUNG_2", evasion-rate: 0.40, detection-chance: 0.08 }
      - { cert-suffix: "STEUERHINTERZIEHUNG_3", evasion-rate: 0.60, detection-chance: 0.03 }

  tax:
    brackets:                  # sort ascending by up-to
      - { up-to: 1000,      rate: 0.0,  label: "0 – 1.000 €" }
      - { up-to: 3000,      rate: 0.20, label: "1.001 – 3.000 €" }
      - { up-to: 6000,      rate: 0.32, label: "3.001 – 6.000 €" }
      - { up-to: 999999999, rate: 0.42, label: "Über 6.000 €" }
```

**Adding a new config value:** add to `application.yml`, add getter/setter to the matching inner class in `GameConfig`, inject `GameConfig` in the service that needs it.

**Adding a new config category:** create inner class `XyzConfig` in `GameConfig`, add field + getter/setter, add `game: xyz:` block in `application.yml`. `@ConfigurationProperties` binds automatically.

### 4.9 Education Data (education.yaml)

`backend/src/main/resources/data/education.yaml` — loaded at `@PostConstruct` in `EducationService`.

```yaml
mainStages:
  - stageKey: REALSCHULABSCHLUSS
    label: "Realschulabschluss"
    durationMonths: 2
    cost: 0
    requiresStage: GRUNDSCHULE
    fieldOptions: []           # empty = no field-of-study split

sideCertFamilies:              # multi-level families
  - family: "STEUERN"
    levels:
      - level: 1
        certKey: "WEITERBILDUNG_STEUERN_1"
        label: "Steuerlehre Grundkurs"
        durationMonths: 1
        cost: 300
        requiresAny: ["REALSCHULABSCHLUSS"]

sideCerts:                     # legacy flat list — also loaded
  - certKey: "WEITERBILDUNG_BARKEEPER_1"
    label: "Barkeeper-Kurs"
    durationMonths: 1
    cost: 250
    requiresAny: []
```

**Fallback:** `EducationService.loadDefaults()` (Java code, ~line 167) contains the same data. If YAML loading fails, the fallback kicks in. **New certs must be added in both places.**

**Education stage key format:** `"REALSCHULABSCHLUSS"`, `"AUSBILDUNG_FACHINFORMATIKER"`, `"BACHELOR_INFORMATIK"`, `"WEITERBILDUNG_STEUERN_1"`, `"WEITERBILDUNG_STEUERHINTERZIEHUNG_1"`, etc.

### 4.10 Frontend Patterns

**API calls:** use `$fetch` directly, not a composable:

```typescript
await $fetch<T>(`${config.public.apiBase}/api/...`, {
  headers: { Authorization: `Bearer ${authStore.token}` },
})
```

**Stores:**
- `stores/auth.ts` — token, user, login / register / logout / restoreSession
- `stores/game.ts` — character (all fields incl. tax evasion status), expenses, lastTurnResult, `init()`
- `stores/toast.ts` — success / error / warning / info toasts

**Toasts in catch blocks:**
```typescript
catch (e: any) {
  toastStore.error(e?.data?.message ?? e?.message ?? 'Fehler')
}
```

**Every page:**
```typescript
definePageMeta({ layout: 'default' })
```

**Visual style:** `bg-surface-900/800/700`, accent `#6366f1` (indigo).  
**No graph library** — all node graphs / trees built with custom SVG + pan/zoom.  
**Charts:** Chart.js + vue-chartjs only.

### 4.11 Admin Auth Model

The admin is a **normally registered player** whose username matches the `ADMIN_USERNAME` env var. There is no DB column or JWT claim for admin status — the check is a pure string comparison at request time in `AdminService.requireAdmin()`.

- If `ADMIN_USERNAME` is blank or unset in `.env`, **no one is admin** and all `/api/admin/**` calls (except `/me`) return 403.
- The frontend fetches `/api/admin/me` after login and on session restore to populate `authStore.isAdmin`. The sidebar admin link is rendered only when this is `true`.
- Changing `ADMIN_USERNAME` in `.env` requires a backend container restart to take effect.
- The admin account must be registered first via the normal `/api/auth/register` endpoint.

### 4.13 PlayerEffectsService — Centralized Effect Aggregation

All multipliers, discounts, and stat-ticks that affect a player's gameplay flow through a single service: `PlayerEffectsService.getEffects(Long playerId)`.

**Adding a new effect source:** create a `@Component` class in `domain/effect/` implementing `EffectContributor` (returns `List<EffectContribution>`). Spring auto-discovers it — no other code changes needed.

**Adding a new effect type:** add entry to `EffectType` enum, add value in the source YAML/data, implement the contributor mapping, add the consumer call-site.

**Active contributors:**
- `SocialEffectContributor` — maps all `persons.yaml` boost types to EffectType (21 types)
- `CollectionEffectContributor` — maps DB `bonus_type` from completed collections
- `LifestyleEffectContributor` — maps `stressReductionMonth` and `taxEvasionBoost` from owned lifestyle items

**Consumers:** TurnService (loads snapshot once at `endTurn()` start), LoanService, RealEstateService, TravelService, CollectibleService, StockService.

**REST endpoint:** `GET /api/effects` → `EffectSummaryDto` — grouped by type with source breakdowns, displayed at `/effekte`.

**Effect stacking:** additive across all sources. Caps enforced per consumer (e.g. max 50% property discount, max 80% travel cost reduction, max 90% detection reduction).

### 4.12 Cloudflare Tunnel Compatibility

- `application.yml`: `forward-headers-strategy: framework`
- `nuxt.config.ts`: `runtimeConfig.public.apiBase` ← `NUXT_PUBLIC_API_BASE` (tunnel URL)
- SSR calls go internally via `http://backend:8080`, client-side calls via tunnel
- CORS: `allowedOriginPatterns("*")` with `allowCredentials = true`

---

## 5. Implementing a New Feature

This section maps feature types to the exact files and patterns to use. Pick the matching path.

### 5.1 New REST endpoint

1. **Controller** in `com.financegame.controller/`:
   - Annotate `@RestController`, `@RequestMapping("/api/...")`, `@RequiredArgsConstructor`
   - Use `@AuthenticationPrincipal PlayerPrincipal principal` for the player ID
2. **Service** in `com.financegame.service/`:
   - `@Service`, `@RequiredArgsConstructor`, `@Transactional` on methods (not class)
   - Inject repositories via constructor
3. **Repository** in `com.financegame.repository/`:
   - `@Repository`, `@PersistenceContext EntityManager em`
   - No `JpaRepository` — write JPQL / native SQL explicitly
4. **DTO** in `com.financegame.dto/`: plain Java record or class.
5. **Frontend** call with `$fetch`, token from `authStore.token`.

### 5.2 New DB column

1. Create `V{N+1}__description.sql` in `db/migration/`.
2. Add field to the entity class.
3. Expose in the relevant DTO.
4. Restart backend — Flyway runs automatically.

### 5.3 New DB table

Same as §5.2 plus:
- Create entity class in `com.financegame.entity/`
- Create repository in `com.financegame.repository/`

### 5.4 New Flyway-locked balance value

Add to `application.yml` → `GameConfig` inner class → inject `GameConfig` in service. No migration needed.

### 5.5 New education cert / side-cert family

1. Add entry to `education.yaml` (sideCertFamilies or sideCerts)
2. Add same entry to `EducationService.loadDefaults()` (fallback)
3. In `ausbildung.vue`, update four constants:
   - `FAM_DEF` — position in skill tree
   - `CERT_DURATIONS` — duration for progress bar
   - `SIDE_CERT_LABELS` — display label
   - `CERT_UNLOCKS` (optional) — tooltip text

### 5.6 New random monthly event

1. Create class in `domain/event/`, implement `RandomGameEvent`, annotate `@Component`.
2. Roll probability in `tryApply()`, mutate character, append message. Done.

### 5.7 New collection bonus type

1. Create class in `domain/effect/collection/`, implement `CollectionBonusApplier`, annotate `@Component`.
2. Override one of: `modifyIncome`, `modifyExpenses`, `applyStats`. Done.

### 5.8 New domain event

See §4.4 — 3 steps: Record → publishEvent() call → Listener.

### 5.9 New access condition

See §4.5 — create class in `domain/condition/`. Done.

### 5.10 New stock type

1. Add `game.stock-volatility.<TYPE>: 0.xx` to `application.yml`.
2. Add the type to the investment frontend page filter.
3. No backend code changes — `StockService` reads from a map with 0.15 fallback.

---

## 6. Implemented Features & DB State

### Admin Area (no migration — config-only)

Admin panel at `/admin`, visible only when logged in as the configured admin account.

**Setup:** set `ADMIN_USERNAME=<username>` in `.env`, restart backend. The account must be registered first normally.

**Endpoints:**
- `GET /api/admin/me` — returns `{ isAdmin: boolean }`, public (any authenticated user)
- `GET /api/admin/players` — list all players with character stats and real estate
- `GET /api/admin/players/{playerId}/details` — read-only detail view: collectibles, social relationships, travel, investments, completed education stages
- `PATCH /api/admin/players/{playerId}/character` — edit: `cash`, `stress`, `happiness`, `energy`, `hunger`, `schufaScore`, `jailMonthsRemaining`, `exileMonthsRemaining`, `burnoutActive` (all fields optional, pass only what you want to change)
- `DELETE /api/admin/players/{playerId}/real-estate/{realEstateId}` — hard-delete a property; net worth is recalculated immediately

Editing `cash` automatically calls `CharacterService.recalculateNetWorth()` so `netWorth` stays consistent.

**Key files:**
- `backend: service/AdminService.java`, `controller/AdminController.java`
- `backend: dto/AdminPlayerDto.java`, `dto/AdminCharacterEditRequest.java`
- `frontend: pages/admin.vue`, `stores/auth.ts` (isAdmin field), `components/Sidebar.vue` (conditional link)

### Social System (V14)

36 persons in 8 groups (OPEN/EXCLUSIVE). Score 0–100 per person, passive decay −1/month. 4 actions per person. Network discovery via score threshold.

| Group | Type | Requirement |
|---|---|---|
| NACHBARSCHAFT | OPEN | — |
| GASTRONOMEN | OPEN | — |
| AKADEMIKER | OPEN | Bachelor degree |
| GESCHAEFTSLEUTE | EXCLUSIVE | Net worth ≥ 50,000€ |
| INVESTOREN | EXCLUSIVE | Net worth ≥ 200,000€ |
| KRIMINELLE | OPEN | — |
| REISELUSTIGE | OPEN | — |
| ELITE | EXCLUSIVE | Net worth ≥ 1,000,000€ |

Actions: `spendTime` (max 4×/month, +8 score), `giveGift` (cash cost, +20 score), `insult` (max 1×/month, −15 score), `rob` (20–50% success by score).

API: `GET /api/social/network`, `POST /api/social/persons/{id}/time|gift|insult|rob`

### Tax Evasion System (V13)

Toggle on/off, 3 skill levels via education tree (requires WEITERBILDUNG_STEUERN_1).

| Level | Cert | Evasion Rate | Detection Risk/Month |
|---|---|---|---|
| 1 | WEITERBILDUNG_STEUERHINTERZIEHUNG_1 | 20% | 15% |
| 2 | WEITERBILDUNG_STEUERHINTERZIEHUNG_2 | 40% | 8% |
| 3 | WEITERBILDUNG_STEUERHINTERZIEHUNG_3 | 60% | 3% |

When caught: choose Jail (6 months no income, stress +15/month, happiness −10/month, SCHUFA −100) or Bail+Exile (`max(5000, cumulativeEvadedTaxes × 3)` €, 3 months exile, SCHUFA −50).

API: `GET /api/tax-evasion/status`, `POST /api/tax-evasion/toggle`, `POST /api/tax-evasion/resolve-caught`

### Needs System (V12)

20 items (water, pizza, coffee, sport, therapy…). Monthly: burnout if stress ≥ 100 (jobs lost, −100€), depression if happiness = 0 (3 months, +3 stress/month).

API: `GET /api/needs/items`, `POST /api/needs/purchase`

### Loans & SCHUFA (V7 + extensions)

SCHUFA breakdown endpoint: `GET /api/loans/schufa-breakdown` — returns factors (base, loans, education, gambling).

### Stock Delisting & Mean-Reversion (V22)

Price simulation replaced pure random walk with Ornstein-Uhlenbeck mean-reversion in log-space. Each turn: `logReturn = θ·ln(P_initial/P_current) + Uniform(-σ,σ)`. When computed price falls below `max(0.01, initialPrice×10%)`, the stock is **delisted per player**: all investments wiped, event logged, trading blocked (HTTP 410). After ≥6 months, 20% chance/turn to relist at seed price.

Frontend: delisted stocks show INSOLVENT badge, sorted to bottom, buy form replaced by blocked message.

Config keys: `game.stock-reversion-speed` (θ per type), `game.stock-delisting` (threshold, timing).

New entity: `PlayerDelistedStock` → `player_delisted_stocks` table.

### Investments (V2 + V10 + V11)

127 stocks in `stocks.yaml` across types: NORMAL, ETF, DIVIDEND_STOCK, BOND, REIT, CRYPTO, LEVERAGE, WARRANT, SHORT, FUTURES. Cert-gated. `GameDataLoaderService.loadStocks()` upserts on conflict — **does not overwrite current_price or initial_price** (protects live game prices).

### Travel / Jail / Exile interaction

`TravelService.depart()` blocks when `jailMonthsRemaining > 0` or `exileMonthsRemaining > 0 && destination == Germany`. `returnHome()` blocks during exile. `PlayerTravelStatusDto` includes `inJail`, `jailMonthsRemaining`, `inExile`, `exileMonthsRemaining`.

---

## 7. Open TODOs / Technical Debt

These are known issues. Fix them in a future session.

1. **`meetsEducationRequirement()` duplicated** in `JobService` and `TurnService` — should be extracted into `EducationService`.

2. **`max_parallel > 1`** is stored but not enforced — `player_jobs` has `(player_id, job_id)` as primary key.

3. **`TurnController.endTurn()`** has redundant `@Transactional` — `TurnService.endTurn()` already has one.

4. **`loadDefaults()` in `EducationService`** does not include STEUERHINTERZIEHUNG certs — if `education.yaml` fails to load, these certs are missing from the fallback.

5. ~~Social boost types not wired into consumers~~ — **Resolved.** All 21 social boost types are now wired through `PlayerEffectsService`. See §4.13.

6. **`totalJailMonthsServed`** field exists in DB and entity but is never incremented in `TurnService`. Fix: in the jail-tick block where `jailMonthsRemaining` is decremented, also increment `totalJailMonthsServed`. Until fixed, `MinJailMonthsServedCondition` always returns false.

7. **Old NPC system** (`entity/Npc.java`, `PlayerRelationship.java`, `RelationshipService.java`, `RelationshipController.java`) is deprecated but kept for DB compatibility. Do not delete; do not extend.

---

## 8. Key File Paths

```
game/
├── docker-compose.yml
├── agent.md                          ← this file
├── changelog/                        ← per-release update instructions (see §10)
├── promt.md                          ← full Social-Rework plan
└── backend/src/main/
    ├── java/com/financegame/
    │   ├── entity/
    │   │   ├── GameCharacter.java    ← all character fields (burnout, tax evasion, jail, exile, social)
    │   │   ├── EducationProgress.java ← completedStages TEXT[]
    │   │   ├── PlayerSocialRelationship.java
    │   │   ├── PlayerSocialGroupUnlock.java
    │   │   └── [all other entities]
    │   ├── domain/
    │   │   ├── GameContext.java
    │   │   ├── GameContextFactory.java
    │   │   ├── condition/            ← Condition interface + 10 implementations
    │   │   ├── effect/collection/    ← CollectionBonusApplier interface + 5 @Components
    │   │   ├── event/                ← RandomGameEvent interface + 6 @Components
    │   │   ├── events/               ← DomainEvent interface + 17 Records
    │   │   └── social/               ← PersonDef, GroupDef, BoostDef, GiftRequirement
    │   ├── dto/
    │   │   ├── CharacterDto.java     ← all character fields incl. tax evasion
    │   │   └── TurnResultDto.java    ← taxEvasionCaught, taxEvasionCaughtAmount
    │   ├── service/
    │   │   ├── TurnService.java      ← monthly turn orchestrator
    │   │   ├── TaxService.java       ← progressive tax (data-driven via GameConfig)
    │   │   ├── SocialService.java    ← actions + network + boosts
    │   │   ├── PersonService.java    ← YAML loader for persons.yaml
    │   │   ├── TaxEvasionService.java
    │   │   ├── CharacterService.java ← purchaseNeedItem()
    │   │   ├── EducationService.java ← loads education.yaml + loadDefaults() fallback
    │   │   ├── GameDataLoaderService.java ← upserts stocks/jobs/collectibles/real estate/needs
    │   │   ├── LoanService.java      ← clampSchufa(), getSchufaBreakdown()
    │   │   ├── RandomEventService.java
    │   │   └── [all other services]
    │   ├── repository/
    │   │   ├── PlayerSocialRelationshipRepository.java
    │   │   ├── PlayerSocialGroupUnlockRepository.java
    │   │   └── [all other repositories]
    │   ├── listener/                 ← 7 @TransactionalEventListener classes
    │   ├── config/
    │   │   ├── GameConfig.java       ← @ConfigurationProperties(prefix = "game")
    │   │   └── AsyncConfig.java      ← thread pool for @Async listeners
    │   └── controller/
    │       ├── SocialController.java     ← /api/social/*
    │       ├── TaxEvasionController.java ← /api/tax-evasion/*
    │       ├── LoanController.java       ← /api/loans/schufa-breakdown
    │       ├── NeedsController.java      ← /api/needs/*
    │       └── [all other controllers]
    └── resources/
        ├── application.yml               ← game: section with all balance values
        └── data/
            ├── persons.yaml              ← 36 persons, 8 social groups
            ├── education.yaml            ← education tree
            ├── jobs.yaml                 ← 51 jobs
            ├── stocks.yaml               ← 127 stocks
            ├── collectibles.yaml
            ├── real_estate.yaml
            ├── collections.yaml
            └── needs_items.yaml

frontend/
├── layouts/default.vue           ← version badge (bump after every session), MonthlyBalanceSheet
├── stores/
│   ├── auth.ts
│   ├── game.ts                   ← character + all game state
│   └── toast.ts
├── components/
│   ├── Sidebar.vue               ← navigation + tax evasion pulse indicator
│   └── MonthlyBalanceSheet.vue   ← caught-section, gate on "Weiter"
└── pages/
    ├── beziehungen.vue           ← SVG node graph (pan/zoom) for social system
    ├── steuerhinterziehung.vue
    ├── karriere.vue              ← pan/zoom job tree
    ├── ausbildung.vue            ← pan/zoom education tree (reference implementation)
    ├── investitionen.vue         ← search + filter + locked stocks
    ├── beduerfnisse.vue
    ├── reisen.vue                ← SVG world map
    └── [all other pages]
```

---

## 9. Committing Changes

**After every session that changes code, create a git commit — do not push.**  
The user handles pushing to GitHub manually.

```bash
git add -p   # stage relevant changes (never blindly git add -A)
git commit -m "short imperative summary"
```

Commit message rules:
- Imperative mood, present tense: "Add", "Fix", "Update" — not "Added" or "Fixes".
- One line summary (≤72 chars). Add a blank line + detail if needed.
- Reference the feature area: e.g. "Add OU mean-reversion model for stock prices".

Do **not** `git push` — the user does that themselves.

---

## 10. Changelog

Update steps for each release live in `changelog/`. Read the relevant file before updating a running instance.

```
changelog/
├── README.md                                      ← format rules for writing changelog entries
├── 2026-04-13_initial-release.md                  ← V1–V13, first-time setup
├── 2026-04-20_social-rework-stock-isolation.md    ← V14–V15
├── 2026-04-30_security-mobile-features.md         ← V16–V21, lifestyle items, victory, audit, cooldown
├── 2026-05-01_plinko-remove-limits.md             ← Plinko game, no migrations
├── 2026-05-03_loan-repayment-burnout-fix.md       ← loan UI, burnout balance fix
├── 2026-05-04_admin-area.md                       ← admin panel, ADMIN_USERNAME env var
├── 2026-05-04_stock-delisting-ou-model.md         ← V22, OU price model, per-player bankruptcy
├── 2026-05-04_texas-holdem-max-bet.md             ← Texas Hold'em raise capped at 15× initial bet
├── 2026-05-04_victory-collectibles.md             ← all collectibles required for victory
├── 2026-05-04_admin-player-details.md             ← admin detail view: collectibles, social, travel, investments, education
├── 2026-05-04_world-map-countries.md              ← V23, 24 new countries, Deutschland/Internet special-casing, map fixes
└── 2026-05-05_central-effects-service.md          ← PlayerEffectsService, EffectContributor pattern, all 21 boost types wired, /effekte page
```

**Rule for agents: every session that produces a deployable change gets a new changelog file.**  
File naming: `YYYY-MM-DD_short-description.md`. See `changelog/README.md` for the required template.

---

## 11. Editing This File

This file is the single source of truth for agents. Keep it current. **Update it at the end of every session that changes code.**

**When to update:**

| Situation | What to update |
|---|---|
| New migration added | §3 Migration history — add row, update "Next migration is VN" |
| New feature completed | §6 Implemented Features — add subsection |
| New changelog file written | §10 Changelog — add a line to the file tree |
| TODO resolved | §7 — remove the item |
| New TODO discovered | §7 — add the item |
| New core pattern introduced | §4 — add a new numbered subsection |
| New implementation recipe | §5 — add a new numbered subsection |
| Key file added/moved | §8 File Paths — update the tree |
| Balance value changed | §4.8 GameConfig — update the yaml example |
| New env variable added | §2 Setup & Pull + the relevant feature section |

**Format rules:**
- Headings use `###` for subsections (never deeper than `####`).
- Code blocks use language tags (`java`, `typescript`, `yaml`, `bash`).
- Tables preferred over bullet lists for structured comparisons.
- No decorative emoji — functional markers only (`✅`, `🔄` in tables if needed).
- Keep the Table of Contents in sync with heading anchors.
- English content, German only where it mirrors actual code strings (cert keys, labels, etc.).
