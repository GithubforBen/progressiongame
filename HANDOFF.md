# FinanzLeben – Agent Handoff

> **Zweck:** Diese Datei ermöglicht dem nächsten Agenten, direkt weiterzumachen.
> Lies sie vollständig, bevor du irgendetwas änderst.

---

## Projekt-Überblick

Browser-basierte passive Finanz-Lebenssimulation.
- **Frontend:** Nuxt.js 3 + Tailwind CSS — `frontend/` (WebStorm öffnen)
- **Backend:** Java 21 + Spring Boot 3.3 + Hibernate direkt (kein Spring Data JPA) — `backend/` (IntelliJ IDEA öffnen, `pom.xml` im Root)
- **DB:** PostgreSQL 15, Flyway-Migrationen in `backend/src/main/resources/db/migration/`
- **Auth:** JWT (jjwt 0.12.x), stateless, BCrypt-Passwörter
- **Deploy:** Docker Compose (`docker-compose.yml` im Root)

---

## Git-Status

```
Branch: main
Letzter Commit: feat: Step 10 – Gambling (Slots, Blackjack, Poker)
```

Alle 10 Schritte committed, **kein uncommitted state**.

---

## Was bisher implementiert wurde (Schritte 1–6)

### Schritt 1 – Scaffolding + Docker ✅
- `docker-compose.yml`: Services `db` (PostgreSQL 15), `backend` (:8080), `frontend` (:3000)
- `.env.example` mit allen nötigen Variablen
- Flyway-Migration `V1__initial_schema.sql` — **alle Tabellen** sind bereits angelegt inkl. Seed-Daten für Jobs, Stocks, Collectibles

### Schritt 2 – Auth ✅
- `POST /api/auth/register` → erstellt Player + Character (€1000 Start) + EducationProgress + 3 Standard-Ausgaben
- `POST /api/auth/login` → gibt JWT zurück
- JWT-Filter (`JwtAuthenticationFilter`) setzt `PlayerPrincipal` (id + username) in SecurityContext
- In Controllern: `@AuthenticationPrincipal PlayerPrincipal principal` → `principal.id()`
- `GlobalExceptionHandler` liefert saubere JSON-Fehler an Frontend

### Schritt 3 – Character + Needs ✅
- `GET /api/character` → `CharacterDto`
- `GET /api/expenses`, `PATCH /api/expenses/{id}/toggle`
- `CharacterService` mit `applyNeedsDecay()`, `deductCash()`, `addCash()`, `recalculateNetWorth()`
- Decay pro Monat: Hunger −15 (oder −5 mit aktivem Essen-Expense), Energie −8, Glück −5

### Schritt 4 – Turn Engine ✅
- `POST /api/turn/end` → `TurnResultDto` (in **einer** `@Transactional` Methode in `TurnService`)
- Reihenfolge: Bewerbungen auflösen → Gehalt → Steuer → Ausgaben → Needs-Decay → Stress setzen → Bildung voranbringen → Turn++  → MonthlySnapshot speichern → Events loggen
- Progressive Steuer: 0% / 20% / 32% / 42%
- `MonthlySnapshot` wird pro Monat gespeichert (Basis für spätere Charts)
- Frontend: `MonthlyBalanceSheet.vue` Modal öffnet sich automatisch nach Monatsabschluss

### Schritt 5 – Jobs ✅
- `GET /api/jobs` → alle Jobs mit player-spezifischen Flags (`meetsRequirements`, `alreadyApplied`, `alreadyWorking`)
- `GET /api/jobs/my`, `GET /api/jobs/applications`
- `POST /api/jobs/{id}/apply` → erstellt `JobApplication` (Ergebnis kommt nächsten Monat)
- `DELETE /api/jobs/{id}/quit` → sofortige Kündigung
- Karriere-Seite: Job-Browser (3 Filter), aktive Jobs, Bewerbungshistorie

### Schritt 6 – Ausbildung ✅
- `GET /api/education`, `POST /api/education/main`, `POST /api/education/side`
- Statischer Stufenkatalog in `EducationService`:
  - Hauptpfad: GRUNDSCHULE → REALSCHULABSCHLUSS → ABITUR → BACHELOR (6 Mo.) → MASTER (4 Mo.)
  - Branch: AUSBILDUNG (4 Mo., 4 Fachrichtungen, erfordert nur REALSCHULABSCHLUSS)
  - 4 Weiterbildungen: SOCIAL_MEDIA, EXCEL, FUEHRERSCHEIN, CRYPTO (je 1 Monat, parallel)
- Education-Stage-Keys im `completed_stages`-Array: `"REALSCHULABSCHLUSS"`, `"BACHELOR_INFORMATIK"`, `"WEITERBILDUNG_SOCIAL_MEDIA"` usw.
- TurnService in Schritt 4 bringt Bildung automatisch voran und fügt abgeschlossene Stages zu `completed_stages` hinzu
- `EducationStageCard.vue` Komponente mit depth-basierter Einrückung, Feld-Picker, Status-Dot

---

## Architektur-Entscheidungen die du kennen MUSST

### Backend – Hibernate direkt, kein Spring Data JPA
Alle Repositories injizieren `EntityManager` via `@PersistenceContext` und haben explizite Methoden.
**Niemals** `JpaRepository` einführen. Beispielmuster:
```java
@Repository
public class XyzRepository {
    @PersistenceContext
    private EntityManager em;

    public Xyz save(Xyz entity) {
        if (entity.getId() == null) { em.persist(entity); return entity; }
        return em.merge(entity);
    }
}
```

### Backend – Transaktionen
`@Transactional` gehört auf **Service-Methoden**, nicht auf Repositories.
Lesende Methoden bekommen `@Transactional(readOnly = true)`.

### Backend – PlayerPrincipal
Jeder authenticated Endpoint bekommt den eingeloggten User so:
```java
@GetMapping("/something")
public Dto method(@AuthenticationPrincipal PlayerPrincipal principal) {
    // principal.id() = playerId (Long)
    // principal.username() = username
}
```

### Frontend – useApi Composable
Alle API-Calls gehen über `~/composables/useApi.ts`:
```typescript
const api = useApi()
await api.get<T>('/api/...')
await api.post<T>('/api/...', body)
await api.del('/api/...')
await api.patch<T>('/api/...', body)
```

### Frontend – useFormatting Composable
`~/composables/useFormatting.ts` hat `formatCurrency`, `formatEducationRequirement`, `stressLabel`, `stressColor`. Immer dort ergänzen, nie inline duplizieren.

### Frontend – Store-Struktur
- `stores/auth.ts` – token, user, login/register/logout/restoreSession
- `stores/game.ts` – character, expenses, lastTurnResult; `init()` lädt beides parallel
- `stores/toast.ts` – success/error/warning/info Toasts

### Flyway-Migrationen
`V1__initial_schema.sql` enthält alle Tabellen + Seed-Daten. **Nie V1 anfassen.**
Neue Tabellen/Spalten → neue Datei `V2__...sql`, `V3__...sql` usw.

### Education Stage Keys
Jobs prüfen Education-Anforderungen gegen das `completed_stages` TEXT[]-Array in `education_progress`.
Format: `"REALSCHULABSCHLUSS"`, `"ABITUR"`, `"AUSBILDUNG_FACHINFORMATIKER"`, `"BACHELOR_INFORMATIK"`, `"MASTER_BWL"`, `"WEITERBILDUNG_SOCIAL_MEDIA"`.
Die Methode `meetsEducationRequirement()` steht in `JobService` und `TurnService` (leicht dupliziert — kann in Schritt 7+ refactored werden).

---

## Bekannte Probleme / Technische Schulden

1. **`meetsEducationRequirement()` ist dupliziert** in `JobService` und `TurnService`. Sollte in einen gemeinsamen `EducationService.meetsRequirement(playerId, type, field)` extrahiert werden.

2. ~~**`MonthlyExpenseController.toggleExpense()` ist nicht `@Transactional`**~~ — **Behoben in Schritt 7**: `ExpenseService` mit `@Transactional` eingeführt, Controller nutzt jetzt den Service.

3. **`TurnController.endTurn()` hat `@Transactional` auf dem Controller** (nicht auf dem Service) — das ist unüblich. TurnService.endTurn() hat selbst `@Transactional`, was ausreicht. Die Annotation auf dem Controller ist redundant und kann entfernt werden.

4. **Stress-Berechnung**: Im TurnService wird Stress auf `sum(stressPerMonth)` der aktiven Jobs gesetzt. Das bedeutet: egal wie lange du einen stressigen Job hast, Stress bleibt konstant. Das ist die gewollte Mechanik (kein kumulativer Stress), aber gut zu wissen.

5. ~~**Net Worth** ist nur `= cash`~~ — **Behoben in Schritt 8**: `recalculateNetWorth()` summiert Cash + alle Investment-Werte.

6. **`frontend/pages/leben.vue`**, `investitionen.vue`, `rangliste.vue` sind noch Stubs.

7. **Maven** — `mvn` ist im PATH (`/usr/bin/mvn`). Lokal kompilieren mit `mvn compile -f backend/pom.xml`. Im Docker-Build passiert das automatisch.

8. **Frontend wurde nicht via `npm run dev` getestet** (kein Browser-Test möglich in diesem Setup). Der Code ist syntaktisch korrekt und folgt Nuxt-3-Konventionen, sollte aber beim ersten echten Start auf TS-Fehler oder fehlende Dependencies geprüft werden.

---

## Nächste Schritte (Schritte 7–15)

### ✅ Schritt 7 – Monatliche Ausgaben + Steuern + KV-Risiko (IMPLEMENTIERT)
**Backend:**
- `ExpenseService` mit `@Transactional` für alle Expense-Mutationen (behebt Schuld #2) ✅
- `POST /api/expenses` → neue Ausgaben hinzufügbar (GYM, STREAMING, KRANKENVERSICHERUNG, MOBILFUNK, INTERNET, ZEITSCHRIFTEN, SONSTIGES) ✅
- `DELETE /api/expenses/{id}` → nicht-mandatory Ausgaben löschen ✅
- `GET /api/tax/preview` → Steuervorschau basierend auf aktiven Jobs ✅
- KV-Risiko in `TurnService`: 10% Chance auf Arztrechnung (200–2000 €) ohne aktive KRANKENVERSICHERUNG-Ausgabe ✅
- `MonthlyExpenseController` nutzt jetzt `ExpenseService` statt direkten Repository-Zugriff ✅

**Frontend:**
- `pages/leben.vue` vollständig implementiert: KV-Warnung/Status, Steuervorschau mit Bracket-Highlight, Ausgaben-Liste mit Toggle/Delete, Neue-Ausgabe-Formular ✅

### ✅ Schritt 9 – Sammlerstücke + Reisen + Tages-Events (IMPLEMENTIERT)
**Backend:**
- V3-Migration: `countries` Tabelle (6 Länder), `player_travel` Tabelle, `player_id` auf `active_events` ✅
- `Country`, `PlayerTravel`, `Collectible`, `PlayerCollectible`, `ActiveEvent` Entities ✅
- `TravelService`: Länder anzeigen, Reise buchen (`POST /api/travel/depart`), heimkehren ✅
- `CollectibleService`: Sammlerstücke kaufen (nur im richtigen Land oder bei aktivem Sale-Event) ✅
- `TurnService`: prüft Reiseankunft + generiert 20% Zufalls-Tages-Events (COLLECTIBLE_SALE) ✅
- Tages-Events: 30% Rabatt auf zufällige Sammlerstücke, player-spezifisch, expire nach 2 Turns ✅

**Frontend:**
- `pages/reisen.vue`: Reisstatus, Länderkarten mit Buchungsbutton, Sammlerstücke-Liste mit Rarität/Rabatt, Meine Sammlung ✅
- `Sidebar.vue`: Reisen-Link hinzugefügt ✅
- `layouts/default.vue`: Bug gefixt (Events wurden nach clearTurnResult() gelesen → null); Tages-Events zeigen als warning-Toast ✅

### ✅ Schritt 10 – Glücksspiel (IMPLEMENTIERT)

### ✅ Schritt 8 – Investitionen (IMPLEMENTIERT)
**Backend:**
- `Stock`, `StockPriceHistory`, `Investment` Entities ✅
- V2-Migration: `stock_price_history` Tabelle (sauber statt JSONB) + `stock_id` auf `investments` ✅
- `StockService.simulatePrices()`: NORMAL ±15%, MEME ±80% pro Monat ✅
- `GET /api/stocks`, `POST /api/investments/stocks/buy`, `POST /api/investments/{id}/sell` ✅
- `CharacterService.recalculateNetWorth()`: Cash + Investment-Werte (behebt Schuld #5) ✅

**Frontend:**
- `pages/investitionen.vue`: Portfolio-Summary, Börse mit Filter, Chart.js Preischart, Kauf/Verkauf ✅

### Schritt 9 – Sammlerstücke + Reisen + Tages-Events
- Travel-System: Länder bereisen, Kosten, Reisezeit in Monaten
- Collectibles: nur in bereistem Land kaufbar
- Tages-Events: zeitlich begrenzte seltene Items → Toast-Notification

### ✅ Schritt 10 – Glücksspiel (IMPLEMENTIERT)
**Backend:**
- V4-Migration: `gambling_sessions` Tabelle ✅
- `GamblingSession` Entity + `GamblingRepository` ✅
- `GamblingService`: Slots (~85% RTP), Blackjack (stateful, Dealer zieht bis 17, BJ=2,5×), Poker (5-Karten vs KI, 5% Rake) ✅
- `POST /api/gambling/slots`, `/blackjack/start`, `/blackjack/{id}/hit`, `/blackjack/{id}/stand`, `/poker` ✅
- Blackjack-Spielzustand wird als JSON in `game_state` gespeichert (Jackson) ✅

**Frontend:**
- `pages/gluecksspiel.vue`: Tab-Navigation (Slots / Blackjack / Poker), Karten-Display, Einsatz-Input, Schnellauswahl-Buttons ✅
- `components/CardDisplay.vue`: Echte Spielkarten-Optik (rot/schwarz, Wert + Symbol) ✅
- `Sidebar.vue`: Glücksspiel-Link hinzugefügt ✅

### ✅ Schritt 11 – Zufallsereignisse (IMPLEMENTIERT)
**Backend:**
- `RandomEventService` mit 6 unabhängigen Ereignissen (je eigene Wahrscheinlichkeit) ✅
  - GEHALTSBONUS (7 %, nur wenn Job aktiv): +200–800 €, Happiness +5
  - AUTOPANNE (8 %): -150–500 €, Stress +10
  - GLUECKSFALL (5 %): +30–250 €, Happiness +10
  - DIEBSTAHL (5 %): 10–30 % des Bargelds (max. 400 €), Happiness -15, Stress +5
  - STRESSABBAU (6 %): Stress -25, Energie +15, Happiness +10
  - UNERWARTETE_RECHNUNG (7 %): -100–300 €
- `TurnService` ruft `randomEventService.applyRandomEvents()` nach Step 5 (Netto-Cashänderung) auf ✅
- KV-Risiko (applyHealthInsuranceRisk) bleibt separat in Step 4b (geht korrekt in expenseBreakdown) ✅
- Alle Events erscheinen in der `events`-Liste des `TurnResultDto` → Toast-Anzeige im Frontend ✅

### ✅ Schritt 12 – Monatsbilanz + Statistik-Dashboard (IMPLEMENTIERT)
**Backend:**
- `SnapshotDto` record (turn, cash, netWorth, totalIncome, totalExpenses) ✅
- `GET /api/stats/snapshots` → alle MonthlySnapshots des eingeloggten Spielers, aufsteigend nach Turn ✅

**Frontend:**
- `pages/index.vue`: Placeholder ersetzt durch zwei echte Chart.js-Diagramme ✅
  - Linienchart "Vermögensverlauf": Nettovermögen (indigo) + Bargeld (grün gestrichelt)
  - Balkenchart "Einnahmen vs. Ausgaben": grün/rot pro Monat
- Charts werden mit `<ClientOnly>` gerendert (SSR-safe), zeigen Platzhalter wenn < 2 Datenpunkte ✅
- `BarElement` zu ChartJS.register() hinzugefügt ✅

### ✅ Schritt 13 – Rangliste (IMPLEMENTIERT)
**Backend:**
- `LeaderboardEntryDto` record (rank, playerId, username, netWorth, currentTurn, isMe) ✅
- `GET /api/leaderboard` → native SQL gegen die bestehende `leaderboard` DB-View, Rang-Nummerierung serverseitig, `isMe`-Flag für den eingeloggten Spieler ✅

**Frontend:**
- `pages/rangliste.vue`: vollständig implementiert ✅
  - "Deine Platzierung"-Highlight-Card (accent-Border) oben
  - Tabelle: Rang (🥇/🥈/🥉 für Top 3), Spielername + "(du)"-Badge, Nettovermögen, Monat
  - Eigene Zeile wird farblich hervorgehoben

### ✅ Schritt 14 – Beziehungssystem (IMPLEMENTIERT)
**Backend:**
- V5-Migration: `npcs` Tabelle + `player_relationships` Tabelle, 5 Seed-NPCs (Klaus, Dr. Müller, Sarah, Marco, Lena) ✅
- `Npc`, `PlayerRelationship` Entities + `NpcRepository`, `PlayerRelationshipRepository` ✅
- `RelationshipService`: `getAll`, `meet`, `interact` (+10 Level, einmal pro Turn), `advanceRelationships` (+1 passiv/Monat, Happiness-Bonus) ✅
- `GET /api/npcs`, `POST /api/npcs/{id}/meet`, `POST /api/npcs/{id}/interact` ✅
- `TurnService`: ruft `relationshipService.advanceRelationships()` nach Needs-Decay auf, wendet Happiness-Bonus an ✅
- Happiness-Bonus-Formel: round(level × happinessBonusPerLevel / 100) pro Beziehung ✅

**Frontend:**
- `pages/beziehungen.vue`: Summary-Cards (Bekannte, Ø Level, Bonus), NPC-Grid mit Persönlichkeits-Farben, Level-Balken, "Kennenlernen"/"Zeit verbringen"-Buttons ✅
- `Sidebar.vue`: Beziehungen-Link (♥) hinzugefügt ✅

### ✅ V7 – Immobilien, Kredite, Collection-Progress & YAML-Datendateien (IMPLEMENTIERT)

**Migration:**
- `V7__real_estate_loans_schufa.sql`: `schufa_score INTEGER 0–1000` auf `characters` (DEFAULT 500), UNIQUE-Constraints auf `jobs.name` und `collectibles.name` (für Upserts), `education_requirements_json JSONB` auf `jobs`, neue Tabellen `real_estate_catalog`, `player_real_estate`, `player_loans`.

**YAML-Datendateien + DataLoader:**
- `backend/src/main/resources/data/jobs.yaml` (12 Jobs), `collectibles.yaml` (12 Items), `real_estate.yaml` (6 Objekte), `education.yaml` (alle Stufen + Zertifikate)
- `GameDataLoaderService` (`ApplicationRunner`): läuft nach Flyway, upsert per `ON CONFLICT (name) DO UPDATE` für Jobs, Collectibles und Immobilien-Katalog. Nutzt snakeyaml (bereits im Classpath via Spring Boot).
- `EducationService`: statische Maps durch Instanzvariablen ersetzt, `@PostConstruct loadEducationData()` lädt `education.yaml`. Fallback auf Hardcoded-Defaults bei Fehler.

**Immobilien-Backend:**
- Entities: `RealEstateCatalog`, `PlayerRealEstate`
- Repositories: `RealEstateCatalogRepository`, `PlayerRealEstateRepository` (EntityManager-Muster)
- `RealEstateService`: `getCatalog()`, `getMyProperties()`, `buy()` (deductCash + persist + recalculateNetWorth), `changeMode()` (SELF_OCCUPIED ↔ RENTED_OUT)
- `RealEstateController`: `GET /api/real-estate`, `GET /api/real-estate/my`, `POST /api/real-estate/{id}/buy`, `PATCH /api/real-estate/{id}/mode`
- `CharacterService.recalculateNetWorth()`: summiert jetzt auch `player_real_estate.purchase_price`

**Kredite & SCHUFA-Backend:**
- Entity: `PlayerLoan`, Repository: `PlayerLoanRepository`
- `GameCharacter` + `CharacterDto`: neues Feld `schufaScore` (int)
- `CharacterService.updateSchufaScore()`: neue Hilfsmethode (clamp 0–1000 + persist)
- `LoanService`: SCHUFA prüfen (< 300 → abgelehnt), Zinssatz nach Score (800+→3%, 600–799→5%, 400–599→8%, <400→12%), Annuitätenformel, SCHUFA –20 bei Aufnahme, Methoden als package-private statics (testbar)
- `LoanController`: `GET /api/loans`, `GET /api/loans/schufa`, `POST /api/loans/take`

**TurnService-Erweiterungen:**
- Nach `calculateSalaries()`: Mieteinnahmen aller `RENTED_OUT`-Immobilien werden zum Bruttoeinkommen addiert
- In `deductExpenses()`: MIETE-Ausgabe wird übersprungen (→ 0 €), wenn mind. 1 `SELF_OCCUPIED`-Immobilie vorhanden
- Nach Ausgaben: `processLoanRepayments()` — aktive Kredite werden abgezogen; SCHUFA +2 bei pünktlicher Rate, +5 bei vollständiger Tilgung, –50 bei Zahlungsausfall → Status DEFAULTED

**OR-Logik für Job-Anforderungen:**
- `Job.educationRequirementsJson` (JSONB, nullable): Format `[{"type":"BACHELOR","field":"BWL"},{"type":"MASTER","field":"BWL"}]`
- `JobService.meetsEducationRequirementWithJson()`: prüft JSON zuerst (OR-Logik), Fallback auf Legacy-Felder wenn JSON null
- `getAvailableJobs()` nutzt die neue Methode

**Collection-Progress:**
- `CollectibleService.getCollectionProgress()`: gruppiert alle Collectibles nach `collectionType`, zählt owned vs. total
- `CollectionProgressDto(collectionType, total, owned, percentage)`
- `GET /api/collectibles/progress` in `CollectibleController`

**Frontend:**
- `pages/immobilien.vue`: Katalog-Karten (Kaufpreis, Mieteinnahmen, Ersparnis), Kaufen-Button, eigene Immobilien mit Modus-Toggle (Einziehen / Vermieten), Status-Badge (grün / blau)
- `pages/kredite.vue`: SCHUFA-Score-Gauge (Farbbalken + Label), Zinsstaffel-Tabelle, aktive Kredite mit Restschuld-Fortschrittsbalken, Kreditformular mit Echtzeit-Vorschau (Rate + Gesamtkosten), abgeschlossene Kredite
- `pages/reisen.vue`: neue Sektion „Sammlungs-Fortschritt" mit Fortschrittsbalken pro Typ (AUTOS, UHREN, KUNST)
- `components/Sidebar.vue`: Links für Immobilien (🏠) und Kredite (🏦) ergänzt
- `composables/useFormatting.ts`: `formatSchufaScore(score)` → `{label, color, bgColor}`, `formatLoanRate(rate)` → `"5,00 % p.a."`

### ✅ V8 – Vollständiger Job- und Bildungskatalog (IMPLEMENTIERT)

**Migration:**
- `V8__jobs_and_education_catalog.sql`: Neue Spalten auf `jobs` (`category VARCHAR(50)`, `max_parallel INT DEFAULT 1`, `required_side_cert VARCHAR(100)`). Bestehende Jobs auf `available = false` gesetzt; DataLoader reaktiviert Katalog-Jobs per Upsert.

**Education YAML (`education.yaml`):**
- AUSBILDUNG: 6 Fachrichtungen (EINZELHANDEL, FACHINFORMATIKER, KFZTECH, PFLEGE, KOCH, ELEKTRIKER)
- BACHELOR: 6 Fachrichtungen inkl. INGENIEURWESEN + PSYCHOLOGIE; Feld-spezifische Dauer (MEDIZIN 8 Mo., JURA 7 Mo.)
- MASTER: 5 Fachrichtungen inkl. INGENIEURWESEN
- Kosten pro Stufe: Ausbildung €500, Bachelor €3.000, Master €5.000
- 11 Weiterbildungen mit individuellen Kosten und Voraussetzungen (BARKEEPER/FITNESSTRAINER ohne Voraussetzung, PROJEKTMANAGEMENT/STEUERN/HACKER erfordern bestimmten Bachelor)

**Jobs YAML (`jobs.yaml`):** 50 Jobs in 7 Kategorien — EINSTIEG, HANDWERK, BUERO, TECH, MANAGEMENT, GESUNDHEIT, RECHT

**Backend-Änderungen:**
- `Job.java`: neue Felder `category`, `maxParallel`, `requiredSideCert`
- `JobDto.java`: `category`, `maxParallel`, `requiredSideCert` (deutsche Anzeigename), Signatur von `from()` erweitert
- `EducationProgressDto.java`: `AvailableStageDto` + `SideCertDto` haben jetzt `cost`-Feld; `FieldOption` hat `durationMonths` für Feld-spezifische Abweichungen
- `EducationService.java`: `StageDefinition` mit `cost` + `fieldDurations`; neue `SideCertDef`-Record mit `requiresAny`; `enrollMain` zieht Kosten ab; `enrollSide` prüft per-Zertifikat-Voraussetzungen (kein globaler Realschulabschluss-Check mehr); MASTER in `buildAvailableMainStages` zeigt nur Felder, für die der passende Bachelor existiert; `CharacterService`-Abhängigkeit ergänzt; statische `sideCertLabel()`-Methode für JobService
- `GameDataLoaderService.java`: Upsert um `category`, `max_parallel`, `required_side_cert`, `available = true` erweitert; neues YAML-Feld `requiredStageKey` (voller Stage-Key direkt)
- `JobService.java`: `meetsSideCertRequirement()`; `meetsRequirements`-Flag kombiniert jetzt Bildung + Side-Cert + Erfahrung

**Frontend-Änderungen:**
- `useFormatting.ts`: `formatEducationRequirement` versteht jetzt vollständige Stage-Keys wie "AUSBILDUNG_EINZELHANDEL"; Stress-Label 4-stufig (Niedrig/Mittel/Hoch/Sehr hoch)
- `karriere.vue`: Kategorie-Filterleiste (Alle/Verfügbar/Meine + 7 Kategorie-Buttons); "Alle"-Ansicht gruppiert nach Kategorie mit farbigen Abschnitts-Headern; Flat-Ansicht für Filter/Kategorie zeigt Kategorie-Badge; Anforderungszeile zeigt 📚 Bildungsabschluss + 🎓 Weiterbildung + ⏱ Erfahrung

**Bekannte Einschränkung:**
- `max_parallel > 1` (Zeitungsausträger, Babysitter) wird gespeichert aber nicht durchgesetzt, da `player_jobs` eine (player_id, job_id) PRIMARY KEY hat (kein doppelter Eintrag möglich). Benötigt Schema-Änderung für vollständige Umsetzung.

### ✅ Frontend-Polish: Ausbildung, Monatsbilanz, Einstellungen (IMPLEMENTIERT)

**`ausbildung.vue`:**
- TREE aktualisiert: alle 6 Ausbildungsberufe, 6 Bachelor-Fächer (inkl. INGENIEURWESEN + PSYCHOLOGIE), 5 Master-Fächer
- `FieldOption.durationMonths`: zeigt abweichende Dauer im Dropdown (MEDIZIN 8 Mo., JURA 7 Mo. werden hervorgehoben)
- Kosten auf "Einschreiben"-Button (€ in Gelb, kommt aus Backend-DTO)
- `CERT_DURATIONS` + `SIDE_CERT_LABELS` Maps für alle 11 Weiterbildungen; Fortschrittsbalken korrekt auch für 2-Monats-Zertifikate
- `mainProgress`-Bar unterstützt feldspezifische Gesamtdauer via `STAGE_DURATIONS`-Lookup

**`EducationStageCard.vue`:**
- Zeigt Kosten in Gelb neben dem Einschreiben-Button
- Dropdown-Optionen zeigen abweichende Feld-Dauer (z.B. "Medizin (8 Mo.)")
- `FieldOption` + `AvailableStage` Interfaces um `durationMonths` und `cost` erweitert

**`MonthlyBalanceSheet.vue`:**
- Zwei Donut-Charts (Chart.js, client-side) für Einnahmen und Ausgaben
- Erstellt/zerstört on `show`-Änderung via `watch`; `onBeforeUnmount` cleanup
- Modalbreite auf `max-w-2xl` erweitert
- `stores/game.ts`: `Character.schufaScore?: number` ergänzt

**`einstellungen.vue`:**
- Profilbereich mit Avatar-Initial + Username + aktueller Spielmonat
- Spielstatistiken: Nettovermögen, Bargeld, Spielmonat, SCHUFA-Score (aus `gameStore.character`)
- Needs-Mini-Übersicht (NeedBar-Komponente)
- Toggle-Einstellungen (Toast-Benachrichtigungen, Kompakt-Ansicht) — gespeichert in `localStorage`

### Schritt 15 – Sprachwahl DE/EN (Optional)
- i18n-Toggle in Einstellungen

---

## Wichtige Dateipfade

```
/home/bestimmtnichtben/Documents/game/
├── docker-compose.yml
├── .env.example
├── HANDOFF.md                          ← diese Datei
├── backend/
│   ├── pom.xml                         ← Spring Boot 3.3.5, Java 21, jjwt 0.12.6
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/financegame/
│       │   ├── FinanceGameApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java         ← CORS, JWT-Filter, BCrypt Bean
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── security/
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   └── PlayerPrincipal.java        ← record(Long id, String username)
│       │   ├── entity/        ← Player, GameCharacter (+ schufaScore), Job (+ educationRequirementsJson),
│       │   │                     PlayerJob, PlayerJobId, JobApplication, EducationProgress,
│       │   │                     MonthlyExpense, MonthlySnapshot, EventLog, Investment, Stock,
│       │   │                     Collectible, PlayerCollectible, Country, PlayerTravel, ActiveEvent,
│       │   │                     GamblingSession, Npc, PlayerRelationship,
│       │   │                     RealEstateCatalog, PlayerRealEstate, PlayerLoan
│       │   ├── repository/    ← alle via EntityManager, kein JpaRepository
│       │   ├── service/       ← AuthService, CharacterService, JobService, EducationService (YAML),
│       │   │                     TurnService, JwtService, StockService, CollectibleService,
│       │   │                     TravelService, GamblingService, RandomEventService,
│       │   │                     RelationshipService, RealEstateService, LoanService,
│       │   │                     GameDataLoaderService (ApplicationRunner)
│       │   ├── controller/    ← Auth, Character, Job, Education, MonthlyExpense, Turn, Health,
│       │   │                     Stock, Investment, Collectible, Travel, Gambling, Npc,
│       │   │                     Leaderboard, Stats, Tax, RealEstate, Loan
│       │   └── dto/           ← alle Request/Response Records
│       └── resources/
│           ├── application.yml
│           ├── data/
│           │   ├── jobs.yaml             ← 12 Jobs (upsert via GameDataLoaderService)
│           │   ├── collectibles.yaml     ← 12 Items
│           │   ├── real_estate.yaml      ← 6 Immobilien-Objekte
│           │   └── education.yaml        ← Bildungsstufen + Zertifikate
│           └── db/migration/
│               ├── V1__initial_schema.sql
│               ├── V2__investments_and_price_history.sql
│               ├── V3__travel_collectibles_events.sql
│               ├── V4__gambling.sql
│               ├── V5__relationships.sql
│               ├── V6__fix_npc_id_types.sql
│               └── V7__real_estate_loans_schufa.sql
└── frontend/
    ├── nuxt.config.ts
    ├── tailwind.config.js
    ├── assets/css/main.css             ← .card, .btn-primary, .btn-secondary, .input, .badge
    ├── layouts/
    │   ├── default.vue                 ← Sidebar + Header + MonthlyBalanceSheet Modal
    │   └── auth.vue
    ├── middleware/auth.ts              ← Redirect zu /login wenn kein Token
    ├── composables/
    │   ├── useApi.ts                   ← get/post/del/patch mit Auth-Header
    │   └── useFormatting.ts           ← formatCurrency, formatEducationRequirement, stressLabel,
│                                      formatSchufaScore, formatLoanRate
    ├── stores/
    │   ├── auth.ts                     ← token, user, login/logout/restoreSession
    │   ├── game.ts                     ← character, expenses, lastTurnResult, init()
    │   └── toast.ts                    ← success/error/warning/info
    ├── components/
    │   ├── Sidebar.vue, StatCard.vue, NeedBar.vue, ToastContainer.vue
    │   ├── CharacterNeeds.vue, ExpensesWidget.vue
    │   ├── MonthlyBalanceSheet.vue     ← Turn-Result Modal
    │   └── EducationStageCard.vue
    └── pages/
        ├── index.vue                   ← Dashboard mit Charts
        ├── login.vue, register.vue     ← auth layout
        ├── karriere.vue               ← vollständig implementiert
        ├── ausbildung.vue             ← vollständig implementiert
        ├── leben.vue                  ← vollständig implementiert
        ├── investitionen.vue          ← vollständig implementiert
        ├── reisen.vue                 ← vollständig implementiert (inkl. Collection-Progress)
        ├── gluecksspiel.vue           ← vollständig implementiert
        ├── beziehungen.vue            ← vollständig implementiert
        ├── immobilien.vue             ← vollständig implementiert (V7)
        ├── kredite.vue                ← vollständig implementiert (V7)
        ├── rangliste.vue              ← vollständig implementiert
        └── einstellungen.vue          ← nur Logout
```

---

## Cloudflare Tunnel Kompatibilität

Das gesamte Setup ist für den Betrieb hinter einem Cloudflare Tunnel ausgelegt:

- **`application.yml`**: `forward-headers-strategy: framework` → Spring's `ForwardedHeaderFilter` verarbeitet `X-Forwarded-For` / `X-Forwarded-Proto` korrekt.
- **`nuxt.config.ts`**: Zwei API-Basis-URLs:
  - `runtimeConfig.apiBase` (privat, SSR) ← `NUXT_INTERNAL_API_BASE` (Standard: `http://backend:8080` im Docker-Netz)
  - `runtimeConfig.public.apiBase` (öffentlich, Client) ← `NUXT_PUBLIC_API_BASE` (die Cloudflare-Tunnel-URL)
- **`useApi.ts`**: `import.meta.server` → interne URL; Client → öffentliche URL. SSR-Calls gehen direkt im Docker-Netz zum Backend, ohne Cloudflare zu durchlaufen.
- **Keine WebSocket-Abhängigkeit** in Production; kein SSE oder Polling mit ws://-URLs.
- **CORS**: `allowedOriginPatterns("*")` mit `allowCredentials = true` — funktioniert hinter Cloudflare, da der Browser den `Origin`-Header setzt und der Tunnel ihn weiterleitet.

Für das Deployment nur `NUXT_PUBLIC_API_BASE` in `.env` auf die öffentliche Backend-Tunnel-URL setzen. `NUXT_INTERNAL_API_BASE` bleibt unverändert (`http://backend:8080`).

---

## Workflow-Hinweise

- **Vor jedem Schritt** kurz bestätigen lassen, dann implementieren
- **Nach jedem Schritt** `mvn compile` ausführen (Pfad oben) und committen
- Antworten auf **Deutsch**, Code auf **Englisch**
- Keine Spring Data JPA Repositories einführen
- Neue DB-Spalten/-Tabellen → neue Flyway-Migration (nie V1 anfassen)
- Alle neuen API-Calls im Frontend über `useApi` Composable
- Wiederverwendbare Formatierung in `useFormatting` ergänzen, nicht inline
