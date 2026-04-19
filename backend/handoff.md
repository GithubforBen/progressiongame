# Backend Architecture — Handoff

Dieses Dokument beschreibt alle Erweiterungspunkte des Backends.
Neues Feature = genau einer der folgenden Pfade. Kein bestehender Code muss verändert werden.

---

## 1. Domain-Event-System (Lose Kopplung zwischen Services)

### Ziel
Services kommunizieren nicht direkt miteinander. Seiteneffekte (Logs, Rabatte, Notifications)
werden über Spring-Application-Events entkoppelt.

### Status
Vollständig implementiert. 18 Events, 7 Listener.

### Events (`com.financegame.domain.events`)

Alle Events sind Java Records, implementieren `DomainEvent` (Interface mit `playerId()` + `occurredAt()`).
Convenience-Konstruktor setzt `Instant.now()`.

| Event | Publisher | Wann |
|-------|-----------|------|
| `TurnEndedEvent` | TurnService | Nach vollständiger Turn-Verarbeitung |
| `JobApplicationResolvedEvent` | TurnService | Bewerbung angenommen/abgelehnt |
| `EducationStageCompletedEvent` | TurnService | Hauptausbildung abgeschlossen |
| `SideCertCompletedEvent` | TurnService | Weiterbildung abgeschlossen |
| `TravelArrivedEvent` | TurnService | Spieler kommt im Zielland an |
| `TravelDepartedEvent` | TravelService | Spieler bucht Flug |
| `LoanTakenEvent` | LoanService | Kredit aufgenommen |
| `LoanPaidOffEvent` | TurnService | Kredit vollständig abbezahlt |
| `LoanDefaultedEvent` | TurnService | Kreditausfall |
| `CollectiblePurchasedEvent` | CollectibleService, CollectionService | Sammlerstück gekauft |
| `CollectionCompletedEvent` | CollectionEventListener (sekundär) | Sammlung vervollständigt |
| `PropertyPurchasedEvent` | RealEstateService | Immobilie gekauft |
| `PropertyModeChangedEvent` | RealEstateService | Modus geändert (vermietet/selbst) |
| `StockPurchasedEvent` | InvestmentService | Aktie gekauft |
| `StockSoldEvent` | InvestmentService | Aktie verkauft |
| `TaxEvasionCaughtEvent` | TurnService | Steuerfahndung erwischt |
| `PlayerRegisteredEvent` | AuthService | Registrierung abgeschlossen |

### Listener (`com.financegame.listener`)

| Listener | Hört auf | Phase | Async | Was passiert |
|----------|----------|-------|-------|--------------|
| `TurnEventListener` | `TurnEndedEvent` | AFTER_COMMIT | nein | Schreibt alle Turn-Messages als EventLog-Einträge |
| `TravelEventListener` | `TravelArrivedEvent` | AFTER_COMMIT | nein | Erstellt COLLECTIBLE_SALE ActiveEvent bei Ankunft |
| `CollectionEventListener` | `CollectiblePurchasedEvent` | AFTER_COMMIT | nein | Prüft ob Sammlung komplett → `CollectionCompletedEvent` |
| `LoanEventListener` | `LoanTakenEvent`, `LoanPaidOffEvent`, `LoanDefaultedEvent` | AFTER_COMMIT | nein | Kredit-Historie in EventLog |
| `PlayerRegistrationListener` | `PlayerRegisteredEvent` | AFTER_COMMIT | **ja** | Willkommens-Eintrag in EventLog |
| `PropertyEventListener` | `PropertyPurchasedEvent`, `PropertyModeChangedEvent` | AFTER_COMMIT | nein | Immobilien-Aktionen in EventLog |
| `InvestmentEventListener` | `StockPurchasedEvent`, `StockSoldEvent` | AFTER_COMMIT | nein | Trades in EventLog |

### Regeln

- **`@TransactionalEventListener(phase = AFTER_COMMIT)`** — immer, wenn der Listener in die DB schreibt.
- **`@Transactional(propagation = REQUIRES_NEW)`** im Listener — immer bei AFTER_COMMIT + DB-Write.
- **`@EventListener`** (ohne Binding) — nur für rein In-Memory-Reaktionen.
- **`@Async`** — nur für unkritische Seiteneffekte. `@EnableAsync` ist in `FinanceGameApplication`, Thread-Pool in `AsyncConfig`.
- Listener dürfen Repositories direkt injizieren, **niemals** Services anderer Module.

### Neues Event hinzufügen

1. Record in `domain/events/`, implementiert `DomainEvent`, Convenience-Konstruktor mit `Instant.now()`.
2. Im Service: `ApplicationEventPublisher eventPublisher` per Konstruktor injizieren.
   Nach DB-Save: `eventPublisher.publishEvent(new MeinEvent(playerId, ...))`.
3. Listener in `com.financegame.listener/`:
   ```java
   @Component
   public class MeinEventListener {
       @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
       @Transactional(propagation = Propagation.REQUIRES_NEW)
       public void on(MeinEvent event) { ... }
   }
   ```
4. Integrationstest: Klasse in `src/test/.../listener/`, extends `AbstractListenerIT`,
   Listener direkt instanziieren (Mocks), in `transactionTemplate` einwickeln, DB-Seiteneffekt assertieren.

---

## 2. Condition-System (Zugangs- und Voraussetzungsprüfungen)

### Ziel
Alle Zugangsbeschränkungen (Zertifikat, SCHUFA, Jail, Reise) werden über eine einheitliche
`Condition`-Schnittstelle ausgedrückt statt über verstreute if-else-Ketten.

### Dateien
`com.financegame.domain.condition.*`

### Interface

```java
public interface Condition {
    boolean isMet(GameContext context);
    String describe();
}
```

### Implementierungen

| Klasse | Was sie prüft |
|--------|--------------|
| `HasCertCondition(certKey)` | `completedEducationStages.contains(certKey)` |
| `MinSchufaCondition(min)` | `character.schufaScore >= min` |
| `NotInJailCondition()` | `character.jailMonthsRemaining == 0` |
| `NotTravelingCondition()` | `!context.traveling()` |
| `EducationLevelCondition(prefix)` | completedStages enthält `prefix` oder beginnt mit `prefix + "_"` |
| `AllOfCondition(c1, c2, ...)` | AND — alle müssen erfüllt sein |
| `AnyOfCondition(c1, c2, ...)` | OR — mindestens eine muss erfüllt sein |
| `NotCondition(c)` | Invertierung |

### GameContext

```java
public record GameContext(
    GameCharacter character,
    List<String> completedEducationStages,
    String currentCountry,
    boolean traveling,
    int activeJobCount
) {}
```

Wird über `GameContextFactory` (`@Service`) aus der DB gebaut:
`gameContextFactory.build(playerId)` — liest Character, EducationProgress, Travel, Jobs.

### Neue Condition hinzufügen

1. Klasse in `domain/condition/`, implementiert `Condition`.
2. Kein bestehender Code muss verändert werden — überall, wo `Condition` verwendet wird,
   kann die neue Klasse sofort genutzt werden (Komposition über AllOf/AnyOf).

---

## 3. CollectionBonusApplier (Strategy-Pattern für Sammlungsboni)

### Ziel
Neue Sammlungs-Bonustypen können hinzugefügt werden ohne `TurnService` anzufassen.

### Dateien
`com.financegame.domain.effect.collection.*`

### Interface

```java
public interface CollectionBonusApplier {
    String getBonusType();   // z. B. "SALARY_MULTIPLIER"

    default BigDecimal modifyIncome(BigDecimal income, BigDecimal bonusValue,
                                    List<TurnResultDto.LineItem> breakdown) { return income; }

    default BigDecimal modifyExpenses(BigDecimal expenses, BigDecimal bonusValue) { return expenses; }

    default void applyStats(GameCharacter character, BigDecimal bonusValue) {}
}
```

### Implementierungen

| Klasse | bonusType | Was passiert |
|--------|-----------|-------------|
| `SalaryMultiplierApplier` | `SALARY_MULTIPLIER` | `income * (1 + bonusValue)` |
| `MonthlyIncomeBonusApplier` | `MONTHLY_INCOME_BONUS` | `income + bonusValue` + LineItem |
| `ExpenseReductionApplier` | `EXPENSE_REDUCTION` | `expenses * (1 - bonusValue)` |
| `HappinessBonusApplier` | `HAPPINESS_BONUS` | `happiness += bonusValue`, max 100 |
| `SchufaBonusApplier` | `SCHUFA_BONUS` | `schufaScore += bonusValue`, max 1000 |

### Verdrahtung in TurnService

```java
// TurnService injiziert alle CollectionBonusApplier-@Components automatisch:
Map<String, CollectionBonusApplier> bonusAppliers;  // bonusType → Applier

// Verwendung:
CollectionBonusApplier applier = bonusAppliers.get(bonus.bonusType());
if (applier != null) income = applier.modifyIncome(income, bonus.bonusValue(), breakdown);
```

### Neuen Bonustyp hinzufügen

1. Klasse in `domain/effect/collection/`, implementiert `CollectionBonusApplier`, annotiert mit `@Component`.
2. `getBonusType()` gibt den String zurück, der in der `Collection`-Entity gespeichert ist.
3. Nur die relevante Methode überschreiben (`modifyIncome`, `modifyExpenses` oder `applyStats`).
4. **Kein bestehender Code wird verändert.** TurnService entdeckt den Applier automatisch.

---

## 4. RandomGameEvent (Strategy-Pattern für Zufallsereignisse)

### Ziel
Neue monatliche Zufallsereignisse als eigenständige `@Component`-Klassen hinzufügbar
ohne `RandomEventService` zu verändern.

### Dateien
`com.financegame.domain.event.*`

### Interface

```java
public interface RandomGameEvent {
    void tryApply(Long playerId, GameCharacter character, List<String> events);
    // Implementierung rollt intern ihre eigene Wahrscheinlichkeit
}
```

### Implementierungen
`GluecksfallEvent`, `DiebstahlEvent`, `GehaltsbonusEvent`, `AutopanneEvent`,
`UnerwarteteRechnungEvent`, `StressabbauEvent`

### Neues Ereignis hinzufügen

1. Klasse in `domain/event/`, implementiert `RandomGameEvent`, annotiert mit `@Component`.
2. In `tryApply()`: eigene Wahrscheinlichkeit würfeln (z. B. `Math.random() < 0.05`),
   bei Treffer Charakter mutieren und Message-String in `events` einhängen.
3. **Kein bestehender Code wird verändert.** `RandomEventService` injiziert `List<RandomGameEvent>`.

---

## 5. GameConfig — Alle Spielzahlen in application.yml

### Ziel
Balancing-Werte (Steuer, Zinsen, Volatilität, Decay …) nur in YAML ändern — kein Recompile.

### Dateien
- `com.financegame.config.GameConfig` — `@Component @ConfigurationProperties(prefix = "game")`
- `src/main/resources/application.yml` — Abschnitt `game:`

### Konfigurationsstruktur

```yaml
game:
  stock-volatility:          # Map<String, Double>: Aktientyp → Volatilität (0..1)
    MEME: 0.80
    ETF: 0.05
    NORMAL: 0.15
    # Fallback für unbekannte Typen: 0.15 (hardcoded in StockService)

  needs:
    hunger-decay-base: 15    # Hunger-Verlust pro Monat ohne Verpflegung
    hunger-decay-with-food: 5
    energy-decay: 8
    happiness-decay: 5

  burnout:
    stress-trigger: 100      # Burnout ab diesem Stress-Wert
    stress-reset: 50         # Stress nach Burnout
    recovery-threshold: 40   # Unter diesem Wert gilt Erholung als abgeschlossen
    hospital-penalty: 100    # Cash-Abzug bei Krankenhausaufenthalt

  depression:
    duration-months: 3
    stress-per-month: 3

  loan:
    min-schufa: 300
    min-amount: 1000
    interest-tiers:           # Absteigend nach min-score sortieren!
      - min-score: 800
        rate: 3.0
        label: "Ausgezeichnet"
      - min-score: 600
        rate: 5.0
        label: "Gut"
      - min-score: 400
        rate: 8.0
        label: "Befriedigend"
      - min-score: 0
        rate: 12.0
        label: "Mangelhaft"

  schufa:
    education-bonuses:        # Map<String, Integer>: Stufen-Prefix → SCHUFA-Bonus
      MASTER: 80
      BACHELOR: 60
      ABITUR: 40
      AUSBILDUNG: 30
      REALSCHULABSCHLUSS: 20

  tax-evasion:
    levels:                   # Aufsteigend nach Risiko — Index = Level-1
      - cert-suffix: "STEUERHINTERZIEHUNG_1"   # Vollständiger Key: WEITERBILDUNG_<suffix>
        evasion-rate: 0.20
        detection-chance: 0.15
      - cert-suffix: "STEUERHINTERZIEHUNG_2"
        evasion-rate: 0.40
        detection-chance: 0.08
      - cert-suffix: "STEUERHINTERZIEHUNG_3"
        evasion-rate: 0.60
        detection-chance: 0.03

  tax:
    brackets:                 # Aufsteigend nach up-to
      - up-to: 1000
        rate: 0.0
        label: "0 – 1.000 €"
      - up-to: 3000
        rate: 0.20
        label: "1.001 – 3.000 €"
      - up-to: 6000
        rate: 0.32
        label: "3.001 – 6.000 €"
      - up-to: 999999999
        rate: 0.42
        label: "Über 6.000 €"
```

### Neue Spielzahl hinzufügen

1. Wert in `application.yml` eintragen.
2. Getter/Setter in der passenden inneren Klasse von `GameConfig` ergänzen.
3. Im jeweiligen Service `GameConfig gameConfig` per Konstruktor injizieren und lesen.

### Neue Kategorie hinzufügen (z. B. Gambling-Odds)

1. Innere Klasse `GamblingConfig` in `GameConfig` anlegen.
2. Feld `private GamblingConfig gambling = new GamblingConfig()` + Getter/Setter.
3. In `application.yml` unter `game: gambling:` eintragen.
4. `@ConfigurationProperties` bindet automatisch.

---

## 6. TaxService — Steuerberechnung

### Datei
`com.financegame.service.TaxService`

### Methoden

| Methode | Rückgabe | Beschreibung |
|---------|---------|-------------|
| `calculateTax(BigDecimal income)` | `BigDecimal` | Progressive Steuer über alle Brackets |
| `determineBracketLabel(BigDecimal income)` | `String` | Label des zutreffenden Brackets |
| `determineBracketPercent(BigDecimal income)` | `int` | Prozentsatz des zutreffenden Brackets |

Brackets kommen aus `gameConfig.getTax().getBrackets()` — rein datengetrieben.
Wird von `TurnService` und `TaxController` gemeinsam genutzt.

---

## 7. Education-Daten in education.yaml

### Dateipfad
`src/main/resources/data/education.yaml`

### Struktur

```yaml
mainStages:
  - stageKey: REALSCHULABSCHLUSS
    label: "Realschulabschluss"
    durationMonths: 2
    cost: 0
    requiresStage: GRUNDSCHULE
    fieldOptions: []           # leer = kein Fachrichtungs-Split

sideCertFamilies:              # Weiterbildungs-Familien (mehrere Stufen pro Familie)
  - family: "STEUERN"
    levels:
      - level: 1
        certKey: "WEITERBILDUNG_STEUERN_1"
        label: "Steuerlehre Grundkurs"
        durationMonths: 1
        cost: 300
        requiresAny: ["REALSCHULABSCHLUSS"]   # ODER-Verknüpfung

sideCerts:                     # Legacy-Format (flache Liste) — wird ebenfalls geladen
  - certKey: "WEITERBILDUNG_BARKEEPER_1"
    label: "Barkeeper-Kurs"
    durationMonths: 1
    cost: 250
    requiresAny: []
```

### Fallback
`EducationService.loadDefaults()` enthält dieselben Daten als Java-Code (ca. Zeile 167).
Schlägt das YAML-Laden fehl, greift der Fallback.
**Neue Certs müssen in beiden Orten eingetragen werden.**

### Neue Weiterbildungs-Familie hinzufügen

**Backend — education.yaml UND loadDefaults():**
```yaml
sideCertFamilies:
  - family: "NEUE_FAMILIE"
    levels:
      - level: 1
        certKey: "WEITERBILDUNG_NEUE_FAMILIE_1"
        label: "Stufe 1"
        durationMonths: 2
        cost: 500
        requiresAny: ["REALSCHULABSCHLUSS"]
```
```java
// EducationService.loadDefaults(), nach den anderen sideCerts:
sideCerts.put("WEITERBILDUNG_NEUE_FAMILIE_1",
    new SideCertDef("WEITERBILDUNG_NEUE_FAMILIE_1", "Stufe 1", 2, 500, List.of("REALSCHULABSCHLUSS")));
```

**Frontend — ausbildung.vue, drei Stellen:**
```ts
// 1. FAM_DEF — Position im Skill-Tree:
{ name: 'NEUE_FAMILIE', label: 'Neue Familie', row: X, sc: Y, dir: 1,
  anchor: 'BACHELOR', levels: ['WEITERBILDUNG_NEUE_FAMILIE_1'] }

// 2. CERT_DURATIONS — Dauer für Fortschrittsbalken:
WEITERBILDUNG_NEUE_FAMILIE_1: 2,

// 3. SIDE_CERT_LABELS — Label für Fortschrittsanzeige:
WEITERBILDUNG_NEUE_FAMILIE_1: 'Neue Familie Stufe 1',

// 4. Optional CERT_UNLOCKS — Tooltip-Text:
WEITERBILDUNG_NEUE_FAMILIE_1: 'Schaltet X frei',
```

### Neuen Aktientyp hinzufügen

1. `application.yml`: `game.stock-volatility.<TYP>: 0.xx`
2. Frontend: Aktientyp in der Investment-Seite ergänzen.
3. Kein Backend-Code ändern — `StockService` liest per Map-Lookup, Fallback 0.15.

---

## 8. Dateikarte

```
src/main/java/com/financegame/
├── domain/
│   ├── GameContext.java                ← Snapshot für Condition-Auswertung
│   ├── GameContextFactory.java         ← @Service: baut GameContext aus DB
│   ├── condition/                      ← Condition-Interface + 8 Implementierungen
│   ├── effect/
│   │   └── collection/                 ← CollectionBonusApplier-Interface + 5 @Components
│   ├── event/                          ← RandomGameEvent-Interface + 6 @Components
│   └── events/                         ← DomainEvent-Interface + 17 Records (Spring Events)
├── listener/                           ← 7 @TransactionalEventListener-Klassen
├── service/
│   ├── TurnService.java                ← Orchestrator; injiziert List<RandomGameEvent> + bonusAppliers-Map
│   ├── TaxService.java                 ← Steuerberechnung (datengetrieben via GameConfig)
│   ├── EducationService.java           ← Lädt education.yaml; Fallback in loadDefaults()
│   ├── LoanService.java                ← Zinsen via GameConfig.loan.interestTiers
│   ├── StockService.java               ← Volatilität via gameConfig.stockVolatility-Map
│   └── TaxEvasionService.java          ← Levels via GameConfig.taxEvasion.levels
├── config/
│   ├── GameConfig.java                 ← @ConfigurationProperties(prefix = "game")
│   └── AsyncConfig.java                ← Thread-Pool für @Async-Listener
└── FinanceGameApplication.java         ← @EnableAsync

src/main/resources/
├── application.yml                     ← game: Abschnitt mit allen Spielzahlen
└── data/education.yaml                 ← Ausbildungsbaum-Daten

frontend/pages/
└── ausbildung.vue                      ← FAM_DEF, CERT_DURATIONS, SIDE_CERT_LABELS, CERT_UNLOCKS
```

---

## 9. Exil/Knast im Travel-Modul

`TravelService.depart()` blockiert bei:
- `jailMonthsRemaining > 0` → Fehler: "Du kannst nicht reisen — du sitzt in Haft"
- `exileMonthsRemaining > 0 && Ziel == Deutschland` → Fehler: "Du befindest dich im Exil"

`returnHome()` blockiert bei `exileMonthsRemaining > 0`.

`PlayerTravelStatusDto` enthält `inJail`, `jailMonthsRemaining`, `inExile`, `exileMonthsRemaining`.
