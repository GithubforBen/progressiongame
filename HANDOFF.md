# FinanzLeben – Agent Handoff

> **Zweck:** Diese Datei ermöglicht dem nächsten Agenten, direkt weiterzumachen.
> Lies sie vollständig, bevor du irgendetwas änderst.

---

## Projekt-Überblick

Browser-basierte passive Finanz-Lebenssimulation.
- **Frontend:** Nuxt.js 3 + Tailwind CSS — `frontend/`
- **Backend:** Java 21 + Spring Boot 3.3 + Hibernate direkt (kein Spring Data JPA) — `backend/`
- **DB:** PostgreSQL 15, Flyway-Migrationen in `backend/src/main/resources/db/migration/`
- **Auth:** JWT (jjwt 0.12.x), stateless, BCrypt-Passwörter
- **Deploy:** Docker Compose (`docker-compose.yml` im Root)
- **Version-Badge:** `v7` (in `frontend/layouts/default.vue` unten rechts — nach jeder Session auf `v(N+1)` erhöhen)

---

## Architektur-Entscheidungen die du kennen MUSST

### Backend – Hibernate direkt, kein Spring Data JPA
Alle Repositories injizieren `EntityManager` via `@PersistenceContext` und haben explizite Methoden.
**Niemals** `JpaRepository` einführen.

### Backend – Transaktionen
`@Transactional` gehört auf **Service-Methoden**, nicht auf Repositories.

### Backend – PlayerPrincipal
```java
@GetMapping("/something")
public Dto method(@AuthenticationPrincipal PlayerPrincipal principal) {
    // principal.id() = playerId (Long)
}
```

### Frontend – $fetch statt useApi
API-Calls in Seiten/Stores direkt mit `$fetch`:
```typescript
await $fetch<T>(`${config.public.apiBase}/api/...`, {
  headers: { Authorization: `Bearer ${authStore.token}` },
})
```

### Frontend – Store-Struktur
- `stores/auth.ts` – token, user, login/register/logout/restoreSession
- `stores/game.ts` – character, expenses, lastTurnResult, init(); **enthält alle Character-Felder inkl. Tax-Evasion-Status**
- `stores/toast.ts` – success/error/warning/info Toasts

### Flyway-Migrationen
Neue Tabellen/Spalten → neue Datei `V{N+1}__....sql`. **Nie V1 anfassen.**

### Education Stage Keys
Format im `completed_stages` TEXT[]-Array:
`"REALSCHULABSCHLUSS"`, `"AUSBILDUNG_FACHINFORMATIKER"`, `"BACHELOR_INFORMATIK"`, `"WEITERBILDUNG_STEUERN_1"`, `"WEITERBILDUNG_STEUERHINTERZIEHUNG_1"` usw.

---

## Implementierte Features (vollständig)

### Flyway-Migrationen V1–V14

| Migration | Inhalt |
|---|---|
| V1 | Initial Schema (alle Kerntabellen, Seed-Daten) |
| V2 | Investitionen + Stock Price History |
| V3 | Reisen, Sammlerstücke, Active Events |
| V4 | Glücksspiel (gambling_sessions) |
| V5 | Beziehungssystem (npcs, player_relationships) |
| V6 | Fix NPC-ID-Typen |
| V7 | Immobilien, Kredite, SCHUFA (real_estate_catalog, player_real_estate, player_loans, schufa_score) |
| V8 | Job-Katalog (category, max_parallel, required_side_cert auf jobs) |
| V9 | Level-System + Collections (collections, player_collections, player_collectibles, collection_name auf collectibles) |
| V10 | Cert-basiertes Unlock-System (required_cert auf stocks/real_estate_catalog/collectibles) |
| V11 | Erweiterte Stock-Typen (DIVIDEND_STOCK, BOND, REIT, LEVERAGE, WARRANT, SHORT, FUTURES) |
| V12 | Bedürfnissystem (needs_items, depression_months_remaining + burnout_active auf characters) |
| V13 | Steuerhinterziehung (tax_evasion_active, tax_evasion_caught_pending, cumulative_evaded_taxes, jail_months_remaining, exile_months_remaining auf characters) |
| V14 | Social Rework (player_social_relationships, player_social_group_unlocks, social_action_log, total_jail_months_served auf characters) |

### Feature Wave 2 (vollständig implementiert)

**Glücksspiel-Bug-Fix:**
- `gluecksspiel.vue`: Alle catch-Blöcke nutzen `toastStore.error(e?.data?.message ?? e?.message ?? 'Fehler')`

**Sammlungen – Suche + Filter:**
- `sammlungen.vue`: Suchfeld, Filter-Chips (Alle/Verfügbar/Besessen/Reise), Kollektions-Filter

**Kredite – SCHUFA-Breakdown:**
- `GET /api/loans/schufa-breakdown` → Faktoren (Basispunkte, Kredite, Bildung, Glücksspiel)
- `LoanController.java`, `LoanService.getSchufaBreakdown()`
- `kredite.vue`: Breakdown-Karte unter SCHUFA-Balken

**Aktien – YAML + 127 Stocks + Suche:**
- `stocks.yaml`: 127 Aktien (NORMAL, ETF, DIVIDEND_STOCK, BOND, REIT, CRYPTO, LEVERAGE, WARRANT, SHORT, FUTURES) mit required_cert-Staffelung
- `GameDataLoaderService.loadStocks()`: `ON CONFLICT (ticker) DO UPDATE SET name, type, required_cert` — **current_price wird NICHT überschrieben** (schützt laufende Spielpreise)
- `investitionen.vue`: Suche (Name/Ticker), Filter (Typ, Gesperrt), gesperrte Aktien mit Cert-Hinweis

**Karriere-Baum (Pan/Zoom):**
- `karriere.vue`: Vollständig neu als Pan/Zoom-Baum (wie ausbildung.vue)
- 5 Bildungsknoten auf linker Achse (Kein/Realschul/Ausbildung/Bachelor/Master)
- 9 Job-Reihen (51 Jobs), SVG-Kanten, slide-in Detail-Panel mit Gehalts-Delta
- "My Jobs"-Overlay, node-pulse Animation für verfügbare Jobs

**Bedürfnissystem:**
- `needs_items.yaml`: 20 Items (Wasser, Pizza, Kaffee, Sport, Therapie, …)
- `GameDataLoaderService.loadNeedsItems()`: Upsert per `ON CONFLICT (id)`
- `NeedsController`: `GET /api/needs/items`, `POST /api/needs/purchase`
- `CharacterService.purchaseNeedItem()`: Preis abziehen, Effekte clamped 0–100 anwenden
- `TurnService.applyNeedsCriticalEvents()`: Burnout (stress≥100 → Jobs weg, -100€) + Depression (happiness=0 → 3 Monate +3 Stress/Mo)
- `beduerfnisse.vue`: Grid mit Effekt-Chips, Needs-Balken oben, Burnout/Depression-Banner
- `Sidebar.vue`: Bedürfnisse-Link

**Reisen – Weltkarte:**
- `reisen.vue`: SVG-Weltkarte mit Länderpunkten, Klick → Detail-Panel, Panning/Zooming

**Version-Badge:** v5 → v6 → v7

---

### Steuerhinterziehungs-System (aktuelle Session)

**Design:**
- Toggle AN/AUS auf eigener Seite
- 3 Skill-Level via Ausbildungsbaum (STEUERHINTERZIEHUNG_1/2/3, Voraussetzung: WEITERBILDUNG_STEUERN_1)
- Entdeckungsrisiko pro Monat — bei Erwischung: Sofort-Wahl in der Monatsbilanz

| Level | Cert | Hinterziehungsquote | Entdeckungsrisiko/Mo | Dauer | Kosten |
|---|---|---|---|---|---|
| 1 | WEITERBILDUNG_STEUERHINTERZIEHUNG_1 | 20% | 15% | 2 Mo | 800€ |
| 2 | WEITERBILDUNG_STEUERHINTERZIEHUNG_2 | 40% | 8% | 3 Mo | 2.500€ |
| 3 | WEITERBILDUNG_STEUERHINTERZIEHUNG_3 | 60% | 3% | 4 Mo | 5.000€ |

**Erwischt-Optionen:**
- Gefängnis: 6 Monate kein Einkommen, Stress +15/Mo, Happiness -10/Mo, SCHUFA -100
- Kaution + Flucht: `max(5000, cumulativeEvadedTaxes × 3)` €, 3 Monate Exil (Einkommen bleibt), SCHUFA -50

**Backend:**
- `V13__tax_evasion.sql`: 5 neue Felder auf `characters`
- `GameCharacter.java`: +5 Felder (taxEvasionActive, taxEvasionCaughtPending, cumulativeEvadedTaxes, jailMonthsRemaining, exileMonthsRemaining)
- `CharacterDto.java`: alle 5 Felder exponiert
- `TurnResultDto.java`: +taxEvasionCaught, +taxEvasionCaughtAmount
- `TurnService.java`:
  - Jail/Exile-Ticks am Anfang von `endTurn()` (vor Gehalt)
  - Evasion-Hook nach `calculateTax()` (taxPaid wird reduziert, Entdeckungscheck)
  - Hilfsmethoden: `taxEvasionLevel(playerId)`, `evasionRate(int)`, `detectionChance(int)`
- `TaxEvasionService.java` (neu): `getStatus()`, `toggle()`, `resolveCaught()`
- `TaxEvasionController.java` (neu): `GET /api/tax-evasion/status`, `POST /api/tax-evasion/toggle`, `POST /api/tax-evasion/resolve-caught`
- `education.yaml`: neue Family `STEUERHINTERZIEHUNG` am Ende von `sideCertFamilies`

**Frontend:**
- `steuerhinterziehung.vue` (neu): Status-Card mit Toggle, Statistik-Card, Caught-Fallback, Info-Card
- `MonthlyBalanceSheet.vue`: caught-Sektion mit Gefängnis/Flucht-Buttons; "Weiter" gesperrt bis Wahl
- `Sidebar.vue`: 🕵️-Link mit rotem Pulsieren bei `taxEvasionCaughtPending`
- `game.ts`: Character-Interface +5 Felder, TurnResult +2 Felder, `toggleTaxEvasion()` + `resolveCaught()`

**DB-Hotfix:** `WEITERBILDUNG_STEUERHINTERZIEHUNG_1` manuell per SQL zu `01bensch+12@gmail.com` hinzugefügt (Nutzer hatte Kurs abgeschlossen aber Eintrag fehlte).

---

### Social Rework (aktuelle Session)

**Design:** 36 Personen in 8 Gruppen (OPEN/EXCLUSIVE), Score 0–100 pro Person, passiver Decay −1/Monat, 4 Aktionen pro Person, Netzwerk-Discovery über Threshold.

**Gruppen:**

| ID | Typ | Voraussetzung |
|---|---|---|
| NACHBARSCHAFT | OPEN | — |
| GASTRONOMEN | OPEN | — |
| AKADEMIKER | OPEN | Bachelor-Abschluss |
| GESCHAEFTSLEUTE | EXCLUSIVE | Nettovermögen ≥ 50.000€ |
| INVESTOREN | EXCLUSIVE | Nettovermögen ≥ 200.000€ |
| KRIMINELLE | OPEN | — |
| REISELUSTIGE | OPEN | — |
| ELITE | EXCLUSIVE | Nettovermögen ≥ 1.000.000€ |

**Aktionen:**
- `spendTime`: max 4×/Monat, +8 Score
- `giveGift`: beliebig oft (Cash-Kosten), +20 Score, optionale Voraussetzungs-Condition
- `insult`: max 1×/Monat, −15 Score, setzt hadConflict=true
- `rob`: max 1×/Monat, 20–50% Erfolg (je Score), bei ERWISCHT: −40 Opfer, −15 alle Gruppenmitglieder, 3-Monats-Sperre

**Boost-Typen (alle 20 aus Tabelle in promt.md implementiert):** werden monatlich im TurnService angewendet, Score-proportional (score/100 × boostValue).

**Backend – neue Dateien:**
- `V14__social_rework.sql`: 3 neue Tabellen + `total_jail_months_served` auf `characters`
- `resources/data/persons.yaml`: 36 Personen, 8 Gruppen, vollständige Conditions/Boosts/Gifts
- `entity/PlayerSocialRelationship.java` + `PlayerSocialRelationshipId.java` (@IdClass)
- `entity/PlayerSocialGroupUnlock.java` + `PlayerSocialGroupUnlockId.java` (@IdClass)
- `repository/PlayerSocialRelationshipRepository.java`
- `repository/PlayerSocialGroupUnlockRepository.java`
- `domain/social/PersonDef.java`, `GroupDef.java`, `BoostDef.java`, `GiftRequirement.java`
- `domain/condition/MinNetWorthCondition.java`, `OwnsCollectionCondition.java`, `HasRelationshipScoreCondition.java`, `HadConflictWithCondition.java`, `MinJailMonthsServedCondition.java`
- `domain/events/RelationshipChangedEvent.java`, `GroupUnlockedEvent.java`, `RobAttemptedEvent.java`, `SocialActionLockAppliedEvent.java`
- `service/PersonService.java`: @PostConstruct lädt persons.yaml, YAML-Condition-Parser (alle 9 Typen)
- `service/SocialService.java`: alle Aktionen + `advanceSocials()` + `getActiveBoosts()` + innere DTOs
- `controller/SocialController.java`: `GET /api/social/network`, `POST /api/social/persons/{id}/time|gift|insult|rob`
- `listener/SocialEventListener.java`: EventLog-Einträge für RelationshipChanged, GroupUnlocked, RobAttempted

**Backend – geänderte Dateien:**
- `domain/GameContext.java`: +`completedCollections`, `relationshipScores`, `hadConflictsWith`
- `domain/GameContextFactory.java`: +CollectionService + PlayerSocialRelationshipRepo für neue Felder
- `entity/GameCharacter.java`: +`totalJailMonthsServed`
- `service/TurnService.java`: `advanceSocials()` ersetzt `advanceRelationships()`, Social-Stat-Boosts (HAPPINESS/STRESS/ENERGY/SCHUFA) direkt auf character
- `service/CollectionService.java`, `LoanService.java`, `RealEstateService.java`, `TravelService.java`: GameContext-Konstruktor-Calls mit 3 leeren neuen Feldern erweitert (`Set.of(), Map.of(), Set.of()`)

**Frontend:**
- `beziehungen.vue`: Komplett neu als SVG Node-Graph mit Pan/Zoom, Score-Ringen (SVG stroke-dasharray), Gruppen-Cluster-Labels, klickbare Nodes, Detail-Panel (Score-Balken, Boost-Info, Unlock-Anforderungen, 4 Aktionsbuttons)

**Wichtig – altes NPC-System:**
- `entity/Npc.java`, `PlayerRelationship.java`, `service/RelationshipService.java`, `controller/RelationshipController.java` bleiben unberührt (DB-Kompatibilität, `npcs`/`player_relationships` Tabellen bleiben)
- `GET /api/npcs` funktioniert weiterhin — aber `beziehungen.vue` zeigt nur noch das neue System

**Offene TODOs für nächste Session:**
- Boost-Typen über einfache Stat-Boosts hinaus (LOAN_INTEREST_REDUCTION, PROPERTY_PRICE_DISCOUNT, TRAVEL_COST_REDUCTION, etc.) in den jeweiligen Services verdrahten (LoanService, RealEstateService, TravelService, CollectibleService, TaxEvasionService)
- `totalJailMonthsServed` in TurnService inkrementieren wenn Gefängnismonat abläuft (aktuell immer 0)
- Frontend-Tests / E2E-Verifikation nach Deployment

---

## Wichtige Dateipfade

```
progressiongame/
├── docker-compose.yml
├── HANDOFF.md
├── promt.md                                ← Vollständiger Social-Rework-Plan (Cold-Start-Kontext)
├── backend/src/main/
│   ├── java/com/financegame/
│   │   ├── entity/
│   │   │   ├── GameCharacter.java          ← +depression/burnout/taxEvasion/jail/exile/totalJailMonthsServed
│   │   │   ├── PlayerSocialRelationship.java   ← NEU (Social Rework)
│   │   │   ├── PlayerSocialGroupUnlock.java    ← NEU
│   │   │   ├── EducationProgress.java      ← completedStages TEXT[]
│   │   │   ├── Job.java, PlayerJob.java, JobApplication.java
│   │   │   ├── Investment.java, Stock.java
│   │   │   ├── Collectible.java, PlayerCollectible.java
│   │   │   ├── RealEstateCatalog.java, PlayerRealEstate.java
│   │   │   ├── PlayerLoan.java
│   │   │   ├── PlayerTravel.java, Country.java, ActiveEvent.java
│   │   │   ├── GamblingSession.java
│   │   │   └── Npc.java, PlayerRelationship.java  ← alt, bleibt (kein Delete)
│   │   ├── domain/
│   │   │   ├── GameContext.java            ← +completedCollections, relationshipScores, hadConflictsWith
│   │   │   ├── GameContextFactory.java     ← +CollectionService + SocialRelRepo
│   │   │   ├── condition/                  ← +MinNetWorth, OwnsCollection, HasRelationshipScore, HadConflictWith, MinJailMonthsServed
│   │   │   ├── events/                     ← +RelationshipChanged, GroupUnlocked, RobAttempted, SocialActionLockApplied
│   │   │   └── social/                     ← NEU: PersonDef, GroupDef, BoostDef, GiftRequirement
│   │   ├── dto/
│   │   │   ├── CharacterDto.java           ← alle Felder inkl. Tax-Evasion
│   │   │   └── TurnResultDto.java          ← +taxEvasionCaught, taxEvasionCaughtAmount
│   │   ├── service/
│   │   │   ├── TurnService.java            ← advanceSocials statt advanceRelationships, Social-Boosts
│   │   │   ├── SocialService.java          ← NEU: Aktionen + getNetwork + getActiveBoosts + innere DTOs
│   │   │   ├── PersonService.java          ← NEU: YAML-Loader für persons.yaml
│   │   │   ├── TaxEvasionService.java
│   │   │   ├── CharacterService.java       ← purchaseNeedItem()
│   │   │   ├── EducationService.java       ← loadEducationData() aus YAML
│   │   │   ├── GameDataLoaderService.java  ← loadCollections/Jobs/Collectibles/RealEstate/NeedsItems/Stocks
│   │   │   ├── LoanService.java            ← clampSchufa(), getSchufaBreakdown()
│   │   │   ├── StockService.java, CollectibleService.java, TravelService.java
│   │   │   ├── GamblingService.java, RandomEventService.java
│   │   │   ├── RelationshipService.java    ← alt, bleibt (kein Delete)
│   │   │   ├── RealEstateService.java, CollectionService.java
│   │   │   └── [weitere Services]
│   │   ├── repository/
│   │   │   ├── PlayerSocialRelationshipRepository.java  ← NEU
│   │   │   ├── PlayerSocialGroupUnlockRepository.java   ← NEU
│   │   │   └── [alle anderen Repositories]
│   │   ├── listener/
│   │   │   ├── SocialEventListener.java    ← NEU
│   │   │   └── [weitere Listener]
│   │   └── controller/
│   │       ├── SocialController.java       ← NEU (/api/social/*)
│   │       ├── TaxEvasionController.java   ← /api/tax-evasion/*
│   │       ├── LoanController.java         ← /api/loans/schufa-breakdown
│   │       ├── NeedsController.java        ← /api/needs/*
│   │       └── [alle anderen Controller]
│   └── resources/
│       ├── data/
│       │   ├── persons.yaml                ← NEU: 36 Personen, 8 Gruppen
│       │   ├── education.yaml              ← STEUERHINTERZIEHUNG Family am Ende
│       │   ├── jobs.yaml                   ← 51 Jobs
│       │   ├── stocks.yaml                 ← 127 Stocks
│       │   ├── collectibles.yaml, real_estate.yaml, collections.yaml, needs_items.yaml
│       └── db/migration/
│           ├── V1–V13 (bestehend)
│           └── V14__social_rework.sql      ← NEU
└── frontend/
    ├── layouts/default.vue                 ← v7 Badge, MonthlyBalanceSheet eingebunden
    ├── stores/game.ts                      ← Character +5 Tax-Felder, TurnResult +2
    ├── components/
    │   ├── Sidebar.vue                     ← 🕵️ Steuerhinterziehung + Pulse
    │   └── MonthlyBalanceSheet.vue         ← caught-Sektion, "Weiter" gesperrt
    └── pages/
        ├── beziehungen.vue                 ← NEU: SVG Node-Graph mit Pan/Zoom + Detail-Panel
        ├── steuerhinterziehung.vue
        ├── karriere.vue                    ← Pan/Zoom-Baum
        ├── ausbildung.vue                  ← Pan/Zoom-Baum
        ├── investitionen.vue               ← Suche + Filter + gesperrte Stocks
        ├── beduerfnisse.vue
        ├── reisen.vue                      ← SVG-Weltkarte
        ├── sammlungen.vue, kredite.vue, gluecksspiel.vue
        └── [alle anderen Seiten]
```

---

## Bekannte Technische Schulden

1. **`meetsEducationRequirement()` ist dupliziert** in `JobService` und `TurnService`. Sollte in `EducationService` extrahiert werden.
2. **`max_parallel > 1`** wird gespeichert aber nicht durchgesetzt (player_jobs hat (player_id, job_id) PRIMARY KEY).
3. **TurnController.endTurn()** hat redundante `@Transactional` — TurnService.endTurn() hat selbst eine.
4. **loadDefaults() in EducationService** enthält keine STEUERHINTERZIEHUNG-Certs — falls education.yaml nicht ladbar ist, fehlen diese im Fallback.
5. **Social Boost-Typen jenseits Stat-Boosts** (LOAN_INTEREST_REDUCTION, PROPERTY_PRICE_DISCOUNT, TRAVEL_COST_REDUCTION, COLLECTIBLE_PRICE_DISCOUNT, TAX_DETECTION_REDUCTION) sind in `SocialService.getActiveBoosts()` berechnet, aber noch **nicht** in LoanService / RealEstateService / TravelService / CollectibleService / TaxEvasionService verdrahtet. Muster: `socialService.getBoostValueForPlayer(playerId, "LOAN_INTEREST_REDUCTION")` aufrufen und vom Zinssatz subtrahieren.
6. **`totalJailMonthsServed`** ist als Feld + Spalte vorhanden, wird aber noch **nicht inkrementiert** wenn ein Gefängnismonat abläuft (TurnService jail-tick). `MinJailMonthsServedCondition` gibt daher immer false zurück. Fix: in `TurnService` wo `jailMonthsRemaining` dekrementiert wird, `totalJailMonthsServed` inkrementieren.

---

## Workflow-Hinweise

- Antworten auf **Deutsch**, Code auf **Englisch**
- Keine Spring Data JPA Repositories — immer `EntityManager` direkt
- Neue DB-Spalten/-Tabellen → neue Flyway-Migration (nie V1 anfassen)
- `mvn compile` nach Backend-Änderungen: `/opt/maven/bin/mvn compile` (aus `backend/`-Verzeichnis)
- Docker rebuild: `docker compose build && docker compose up -d`
- Version-Badge in `layouts/default.vue` nach jeder Session erhöhen

---

## Cloudflare Tunnel Kompatibilität

- `application.yml`: `forward-headers-strategy: framework`
- `nuxt.config.ts`: `runtimeConfig.public.apiBase` ← `NUXT_PUBLIC_API_BASE` (Tunnel-URL)
- SSR-Calls gehen intern über `http://backend:8080`, Client-Calls über Tunnel
- CORS: `allowedOriginPatterns("*")` mit `allowCredentials = true`
