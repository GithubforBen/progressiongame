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
Letzter Commit: b4352ce  feat: Step 6 – Education tree + Weiterbildungen
```

Alle 6 Schritte committed, **kein uncommitted state**.

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

2. **`MonthlyExpenseController.toggleExpense()` ist nicht `@Transactional`** — der `save()`-Call im Repository läuft ohne Transaktion. Funktioniert aktuell durch Hibernate-Autoflush, aber sollte einen `@Transactional` Service bekommen (z.B. `ExpenseService`).

3. **`TurnController.endTurn()` hat `@Transactional` auf dem Controller** (nicht auf dem Service) — das ist unüblich. TurnService.endTurn() hat selbst `@Transactional`, was ausreicht. Die Annotation auf dem Controller ist redundant und kann entfernt werden.

4. **Stress-Berechnung**: Im TurnService wird Stress auf `sum(stressPerMonth)` der aktiven Jobs gesetzt. Das bedeutet: egal wie lange du einen stressigen Job hast, Stress bleibt konstant. Das ist die gewollte Mechanik (kein kumulativer Stress), aber gut zu wissen.

5. **Net Worth** ist aktuell nur `= cash`. In Schritt 8 (Investitionen) muss `recalculateNetWorth()` in `CharacterService` um Investment-Werte ergänzt werden.

6. **`frontend/pages/leben.vue`**, `investitionen.vue`, `rangliste.vue` sind noch Stubs.

7. **Maven nicht im PATH** — lokal kompilieren mit:
   ```
   /home/bestimmtnichtben/.m2/wrapper/dists/apache-maven-3.9.9-bin/4nf9hui3q3djbarqar9g711ggc/apache-maven-3.9.9/bin/mvn compile
   ```
   Im Docker-Build passiert das automatisch über das Dockerfile.

8. **Frontend wurde nicht via `npm run dev` getestet** (kein Browser-Test möglich in diesem Setup). Der Code ist syntaktisch korrekt und folgt Nuxt-3-Konventionen, sollte aber beim ersten echten Start auf TS-Fehler oder fehlende Dependencies geprüft werden.

---

## Nächste Schritte (Schritte 7–15)

### ✅ Schritt 7 – Monatliche Ausgaben + Steuern + KV-Risiko (NÄCHSTER SCHRITT)
**Backend:**
- `ExpenseService` mit `@Transactional` für alle Expense-Mutationen (behebt Schuld #2)
- Neue Ausgaben-Typen: GYM, STREAMING, etc. über `POST /api/expenses` hinzufügbar
- Krankenversicherungs-Risiko-Mechanic: beim Monatsabschluss prüft TurnService ob KV aktiv ist; wenn ein medizinisches Zufallsereignis auftritt → teuer ohne KV, günstig mit
- Steuern sind bereits implementiert (progressive Brackets in TurnService) — evtl. verfeinern
- Optional: `GET /api/tax/preview` → zeigt voraussichtliche Steuer bei aktuellem Gehalt

**Frontend:**
- `pages/leben.vue` ausbauen: Ausgaben verwalten (neue hinzufügen, bestehende deaktivieren), KV-Status mit Risikohinweis
- Steuertabelle anzeigen (welche Bracket der Spieler gerade trifft)

### Schritt 8 – Investitionen
**Backend:**
- Entities: `Investment`, `Stock` bereits in DB (V1-Migration). `Stock`-Entity Java-Klasse muss noch erstellt werden.
- `StockService`: Preissimulation pro Monat (Normal: ±5-15%, Meme: ±30-80%)
- `InvestmentService`: Aktien kaufen/verkaufen, Immobilien, NFTs, Kunst
- `GET /api/stocks`, `POST /api/investments/stocks/buy`, `POST /api/investments/stocks/sell`
- `recalculateNetWorth()` in `CharacterService` erweitern (Schuld #5)

**Frontend:**
- `pages/investitionen.vue`: Aktienliste mit Preishistorie-Chart (Chart.js via vue-chartjs, bereits in package.json)
- Portfolio-Übersicht

### Schritt 9 – Sammlerstücke + Reisen + Tages-Events
- Travel-System: Länder bereisen, Kosten, Reisezeit in Monaten
- Collectibles: nur in bereistem Land kaufbar
- Tages-Events: zeitlich begrenzte seltene Items → Toast-Notification

### Schritt 10 – Glücksspiel
- Poker (vereinfachtes Texas Hold'em vs AI), Blackjack, Slots
- House-Edge eingebaut

### Schritt 11 – Zufallsereignisse
- `RandomEventService` aufrufen in `TurnService` (Platzhalter bereits vorhanden als `// TODO Step 11`)
- Ereignistabelle mit Wahrscheinlichkeiten
- KV-Risiko-Mechanic hier implementieren (Unfall ohne KV → hohe Kosten)

### Schritt 12 – Monatsbilanz + Statistik-Dashboard
- `MonthlySnapshot`-Tabelle ist bereits befüllt seit Schritt 4
- `GET /api/stats/snapshots` → Verlaufsdaten für Charts
- Dashboard `pages/index.vue`: Chart.js Nettovermögen-Linie, Einnahmen/Ausgaben Pie-Charts ersetzen die Platzhalter

### Schritt 13 – Rangliste
- `GET /api/leaderboard` → View `leaderboard` existiert bereits in DB
- `pages/rangliste.vue` ausbauen

### Schritt 14 – Beziehungssystem
- NPCs, Beziehungsaufbau über Monate, Glück-Bonus

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
│       │   ├── entity/        ← Player, GameCharacter, Job, PlayerJob, PlayerJobId,
│       │   │                     JobApplication, EducationProgress, MonthlyExpense,
│       │   │                     MonthlySnapshot, EventLog
│       │   ├── repository/    ← alle via EntityManager, kein JpaRepository
│       │   ├── service/       ← AuthService, CharacterService, JobService,
│       │   │                     EducationService, TurnService, JwtService
│       │   ├── controller/    ← Auth, Character, Job, Education, MonthlyExpense,
│       │   │                     Turn, Health
│       │   └── dto/           ← alle Request/Response Records
│       └── resources/
│           ├── application.yml
│           └── db/migration/V1__initial_schema.sql
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
    │   └── useFormatting.ts           ← formatCurrency, formatEducationRequirement, stressLabel
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
        ├── index.vue                   ← Dashboard (live)
        ├── login.vue, register.vue     ← auth layout
        ├── karriere.vue               ← vollständig implementiert
        ├── ausbildung.vue             ← vollständig implementiert
        ├── leben.vue                  ← STUB
        ├── investitionen.vue          ← STUB
        ├── rangliste.vue              ← STUB
        └── einstellungen.vue          ← nur Logout
```

---

## Workflow-Hinweise

- **Vor jedem Schritt** kurz bestätigen lassen, dann implementieren
- **Nach jedem Schritt** `mvn compile` ausführen (Pfad oben) und committen
- Antworten auf **Deutsch**, Code auf **Englisch**
- Keine Spring Data JPA Repositories einführen
- Neue DB-Spalten/-Tabellen → neue Flyway-Migration (nie V1 anfassen)
- Alle neuen API-Calls im Frontend über `useApi` Composable
- Wiederverwendbare Formatierung in `useFormatting` ergänzen, nicht inline
