# Socials Feature — Vollständiger Rework-Plan

> **Morgen weitermachen:** Claude sagen "Hey mach hier weiter" — dieser Plan hat den vollen Kontext.
> **Erster Schritt:** `promt.md` im Projekt-Root schreiben (Schritt 0), dann mit Schritt 1 beginnen.

---

## Kontext & Codebase-Überblick

**Projekt:** FinanzLeben — Browser-basierte Finanz-Lebenssimulation
**Stack:** Spring Boot 3.3 + Hibernate (direkt, kein JpaRepository) + PostgreSQL 15 + Nuxt 3 + Pinia
**Working directory backend:** `/home/ben/Documents/game/progressiongame/backend`
**Working directory frontend:** `/home/ben/Documents/game/progressiongame/frontend`
**Maven:** `/usr/bin/mvn`

### Was bereits existiert (NICHT anfassen, nur ersetzen/erweitern):

**Altes NPC-System (deprecated, bleibt im DB aber wird nicht mehr aktiv genutzt):**
- `entity/Npc.java` — id, name, description, personality, happinessBonusPerLevel
- `entity/PlayerRelationship.java` — id, playerId, npcId, level, monthsKnown, lastInteractedTurn
- `repository/NpcRepository.java`, `PlayerRelationshipRepository.java`
- `service/RelationshipService.java` — getAll, meet, interact, advanceRelationships
- `controller/RelationshipController.java` — GET /api/npcs, POST /api/npcs/{id}/meet, POST /api/npcs/{id}/interact
- `dto/NpcDto.java`
- DB-Tabellen: `npcs` (5 NPCs: Klaus, Dr.Müller, Sarah, Marco, Lena), `player_relationships`
- Frontend: `pages/beziehungen.vue` — einfaches NPC-Kartenraster mit "Zeit verbringen"-Button

**Architektur-Patterns die wiederverwendet werden:**
- `domain/condition/Condition.java` — Interface: `isMet(GameContext ctx)` + `describe()`
  - Implementierungen: AllOf, AnyOf, Not, HasCert, MinSchufa, NotInJail, NotTraveling, EducationLevel
- `domain/GameContext.java` — Record mit: character, completedEducationStages, currentCountry, traveling, activeJobCount
- `domain/GameContextFactory.java` — @Service, baut GameContext aus DB
- `domain/effect/collection/CollectionBonusApplier.java` — Interface: getBonusType(), modifyIncome(), modifyExpenses(), applyStats()
  - 5 @Component-Implementierungen: Salary, MonthlyIncome, ExpenseReduction, Happiness, Schufa
- `domain/events/*.java` — 18 Spring DomainEvent Records, published via ApplicationEventPublisher
- `listener/*.java` — 7 @TransactionalEventListener(AFTER_COMMIT) + @Transactional(REQUIRES_NEW)
- `config/GameConfig.java` — @ConfigurationProperties(prefix="game"), liest application.yml
- Education-Pattern: EducationService lädt `data/education.yaml` at @PostConstruct, Fallback in loadDefaults()
- TurnService ruft `advanceRelationships()` jeden Monat auf (Zeile ~207)
- Flyway-Migrationen: V1–V13 vorhanden, nächste ist **V14**

**Frontend-Patterns:**
- `useApi()` composable — get/post/del/patch mit Bearer-Token
- `useToastStore()` — info/success/error/warning Toasts
- `definePageMeta({ layout: 'default' })` auf jeder Seite
- Dark theme: bg-surface-900/800/700, accent #6366f1 (indigo)
- Kein Graph-Library installiert (nur Chart.js + vue-chartjs)
- Skill-Tree in `ausbildung.vue` als Vorbild: custom SVG + Canvas, pan/zoom, force-free layout

---

## Feature-Anforderungen (Zusammenfassung aus promt.md)

1. **30–50 Personen** in **5–10 Gruppen** — als YAML-Datendatei, nicht hardcoded
2. **Gruppen:** OPEN (via Netzwerk) oder EXCLUSIVE (Net-Worth, Properties, Items)
3. **Unlock-Requirements** pro Person: Bildung, Collections, vergangene Events, Beziehungen
4. **Score 0–100** pro Person-Beziehung, passiv dauerhafter Boost
5. **Aktionen:** Zeit verbringen (4×/Monat, +Score), Geschenk (1×?, +Score, Kosten), Beleidigung (1×/Monat, −Score), Ausrauben (Erfolg/Erwischt)
6. **Rob-Erwischt:** −Score Opfer, −Score alle Gruppenmmitglieder, 3-Monats-Sperre für nette Aktionen
7. **Netzwerk-Discovery:** Threshold → Freundesgruppe sichtbar → "???" Nodes
8. **Frontend:** Spider/Node-Diagramm, klickbare Nodes, Detail-Panel

---

## Datenmodell (vollständig)

### DB-Tabellen (V14-Migration)

```sql
-- Tracking welche Gruppen der Spieler freigeschaltet hat
CREATE TABLE player_social_group_unlocks (
    player_id        BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    group_id         VARCHAR(100) NOT NULL,
    unlocked_at_turn INT          NOT NULL,
    PRIMARY KEY (player_id, group_id)
);

-- Pro-Spieler-Pro-Person Beziehungsstatus
CREATE TABLE player_social_relationships (
    player_id                  BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    person_id                  VARCHAR(100) NOT NULL,
    score                      INT          NOT NULL DEFAULT 0,
    unlocked_at_turn           INT,
    locked_actions_until_turn  INT          NOT NULL DEFAULT 0,
    monthly_time_spent_count   INT          NOT NULL DEFAULT 0,
    monthly_insult_done        BOOLEAN      NOT NULL DEFAULT FALSE,
    monthly_rob_attempted      BOOLEAN      NOT NULL DEFAULT FALSE,
    had_conflict               BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (player_id, person_id)
);

-- Aktions-Log für EventLog-Integration
CREATE TABLE social_action_log (
    id           BIGSERIAL    PRIMARY KEY,
    player_id    BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    person_id    VARCHAR(100) NOT NULL,
    action_type  VARCHAR(50)  NOT NULL,  -- SPEND_TIME, GIFT, INSULT, ROB
    score_delta  INT,
    outcome      VARCHAR(50),            -- SUCCESS, CAUGHT, FAILED
    turn_number  INT          NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

### Java-Entities (neue Dateien)

```java
// entity/PlayerSocialRelationship.java
@Entity @Table(name = "player_social_relationships")
public class PlayerSocialRelationship {
    @EmbeddedId PlayerSocialRelationshipId id;  // playerId + personId
    int score;
    Integer unlockedAtTurn;
    int lockedActionsUntilTurn;
    int monthlyTimeSpentCount;
    boolean monthlyInsultDone;
    boolean monthlyRobAttempted;
    boolean hadConflict;
}

// entity/PlayerSocialGroupUnlock.java
@Entity @Table(name = "player_social_group_unlocks")
public class PlayerSocialGroupUnlock {
    @EmbeddedId PlayerSocialGroupUnlockId id;  // playerId + groupId
    int unlockedAtTurn;
}
```

### YAML-Daten (keine DB-Tabelle)

**`src/main/resources/data/persons.yaml`** (wie education.yaml):

```yaml
groups:
  - id: NACHBARSCHAFT
    name: "Nachbarschaft"
    type: OPEN
    description: "Deine direkten Nachbarn"
  - id: GASTRONOMEN
    name: "Gastronomen"
    type: OPEN
    description: "Gastronomen und Servicekräfte"
  - id: AKADEMIKER
    name: "Akademiker"
    type: OPEN
    exclusiveCondition: {type: education-level, prefix: BACHELOR}
  - id: GESCHAEFTSLEUTE
    name: "Geschäftsleute"
    type: EXCLUSIVE
    exclusiveCondition: {type: min-net-worth, amount: 50000}
  - id: INVESTOREN
    name: "Investoren"
    type: EXCLUSIVE
    exclusiveCondition: {type: min-net-worth, amount: 200000}
  - id: KRIMINELLE
    name: "Underground"
    type: OPEN
    description: "Leute am Rande der Gesellschaft"
  - id: REISELUSTIGE
    name: "Globetrotter"
    type: OPEN
  - id: ELITE
    name: "Elite"
    type: EXCLUSIVE
    exclusiveCondition: {type: min-net-worth, amount: 1000000}

persons:
  # NACHBARSCHAFT (5 Personen)
  - id: HANS_MUELLER
    name: "Hans Müller"
    description: "Freundlicher Rentner, immer gut für ein Gespräch."
    groupIds: [NACHBARSCHAFT]
    networkThreshold: 40
    friendNetwork: [GRETA_SCHMIDT, JUERGEN_VOGEL]
    boost: {type: HAPPINESS_PER_TURN, value: 3}
    unlockCondition: null
    giftRequirement: {cost: 30, description: "Kleines Mitbringsel"}

  - id: GRETA_SCHMIDT
    name: "Greta Schmidt"
    description: "Pensionierte Lehrerin mit vielen Kontakten."
    groupIds: [NACHBARSCHAFT]
    networkThreshold: 50
    friendNetwork: [RENATE_BAUER, OSMAN_DEMIR]
    boost: {type: STRESS_REDUCTION_PER_TURN, value: 2}
    unlockCondition: {type: has-relationship-score, personId: HANS_MUELLER, minScore: 30}
    giftRequirement: {cost: 50, description: "Blumenstrauß"}

  - id: RENATE_BAUER
    name: "Renate Bauer"
    description: "Geschickte Haushaltsführerin, kennt jeden Spar-Trick."
    groupIds: [NACHBARSCHAFT]
    networkThreshold: 60
    friendNetwork: []
    boost: {type: EXPENSE_REDUCTION, value: 0.03}
    unlockCondition: {type: has-relationship-score, personId: GRETA_SCHMIDT, minScore: 40}
    giftRequirement: {cost: 80, description: "Kochbuch"}

  - id: KARL_WEBER
    name: "Karl Weber"
    description: "Geselliger Typ, kennt die halbe Stadt."
    groupIds: [NACHBARSCHAFT, GASTRONOMEN]
    networkThreshold: 30
    friendNetwork: [OSMAN_DEMIR, YUKI_TANAKA]
    boost: {type: HAPPINESS_PER_TURN, value: 2}
    unlockCondition: null
    giftRequirement: {cost: 40, description: "Sixpack Bier"}

  - id: JUERGEN_VOGEL
    name: "Jürgen Vogel"
    description: "Ehemaliger Buchhalter, kennt die Steuergesetze in- und auswendig."
    groupIds: [NACHBARSCHAFT]
    networkThreshold: 70
    friendNetwork: [DIRK_HOFFMANN]
    boost: {type: TAX_DETECTION_REDUCTION, value: 0.03}
    unlockCondition: {type: has-relationship-score, personId: HANS_MUELLER, minScore: 50}
    giftRequirement: {cost: 120, description: "Steuersoftware-Lizenz"}

  # GASTRONOMEN (4)
  - id: OSMAN_DEMIR
    name: "Osman Demir"
    description: "Betreibt die beste Bar im Viertel."
    groupIds: [GASTRONOMEN]
    networkThreshold: 40
    friendNetwork: [YUKI_TANAKA, PETRA_KURZ]
    boost: {type: HAPPINESS_PER_TURN, value: 4}
    unlockCondition: null
    giftRequirement: {cost: 60, description: "Flasche Whisky"}

  - id: YUKI_TANAKA
    name: "Yuki Tanaka"
    description: "Japanische Restaurantbesitzerin, Meisterin der Effizienz."
    groupIds: [GASTRONOMEN]
    networkThreshold: 50
    friendNetwork: []
    boost: {type: HUNGER_DECAY_REDUCTION, value: 3}
    unlockCondition: null
    giftRequirement: {cost: 100, description: "Premium-Sake"}

  - id: PETRA_KURZ
    name: "Petra Kurz"
    description: "Energiegeladene Bäckerin, die jeden Morgen um 4 Uhr aufsteht."
    groupIds: [GASTRONOMEN]
    networkThreshold: 40
    friendNetwork: []
    boost: {type: ENERGY_BONUS_PER_TURN, value: 3}
    unlockCondition: {type: has-relationship-score, personId: OSMAN_DEMIR, minScore: 30}
    giftRequirement: {cost: 50, description: "Premium-Kaffeeröstung"}

  - id: SARAH_HOFFMANN
    name: "Sarah Hoffmann"
    description: "Optimistische Studentin, bringt frischen Wind."
    groupIds: [GASTRONOMEN, AKADEMIKER]
    networkThreshold: 35
    friendNetwork: [LISA_KRAUSE, ANNA_WOLF]
    boost: {type: HAPPINESS_PER_TURN, value: 3}
    unlockCondition: null
    giftRequirement: {cost: 40, description: "Konzertkarten"}

  # AKADEMIKER (5)
  - id: DR_MUELLER
    name: "Dr. Müller"
    description: "Erfahrener Unternehmer mit starkem Netzwerk."
    groupIds: [AKADEMIKER, GESCHAEFTSLEUTE]
    networkThreshold: 60
    friendNetwork: [PROF_FISCHER, MONIKA_SCHAEFER]
    boost: {type: JOB_ACCEPTANCE_BOOST, value: 0.10}
    unlockCondition: {type: education-level, prefix: BACHELOR}
    giftRequirement:
      cost: 300
      condition: {type: education-level, prefix: BACHELOR}
      description: "Fachbuch + Nachweis Bachelor-Abschluss"

  - id: PROF_FISCHER
    name: "Prof. Fischer"
    description: "Universitätsprofessor für Volkswirtschaft."
    groupIds: [AKADEMIKER]
    networkThreshold: 70
    friendNetwork: [THOMAS_BRAUN]
    boost: {type: SALARY_BONUS, value: 100}
    unlockCondition:
      type: all-of
      conditions:
        - {type: education-level, prefix: BACHELOR}
        - {type: has-relationship-score, personId: DR_MUELLER, minScore: 40}
    giftRequirement:
      cost: 500
      condition: {type: education-level, prefix: MASTER}
      description: "Wissenschaftliche Publikation + Master-Abschluss"

  - id: LISA_KRAUSE
    name: "Lisa Krause"
    description: "HR-Managerin bei einem großen Konzern."
    groupIds: [AKADEMIKER, GESCHAEFTSLEUTE]
    networkThreshold: 50
    friendNetwork: [MONIKA_SCHAEFER]
    boost: {type: JOB_ACCEPTANCE_BOOST, value: 0.08}
    unlockCondition: {type: has-relationship-score, personId: SARAH_HOFFMANN, minScore: 40}
    giftRequirement: {cost: 200, description: "Karrierecoaching-Session"}

  - id: THOMAS_BRAUN
    name: "Thomas Braun"
    description: "Kreditberater mit exzellenten Bankkontakten."
    groupIds: [AKADEMIKER, GESCHAEFTSLEUTE]
    networkThreshold: 60
    friendNetwork: [STEFAN_BRANDT]
    boost: {type: SCHUFA_BONUS_MONTHLY, value: 2}
    unlockCondition: {type: education-level, prefix: BACHELOR}
    giftRequirement: {cost: 250, description: "Exklusiver Terminplaner"}

  - id: ANNA_WOLF
    name: "Anna Wolf"
    description: "Psychologin, die dir hilft Stress abzubauen."
    groupIds: [AKADEMIKER]
    networkThreshold: 45
    friendNetwork: []
    boost: {type: STRESS_REDUCTION_PER_TURN, value: 4}
    unlockCondition: {type: has-relationship-score, personId: SARAH_HOFFMANN, minScore: 30}
    giftRequirement: {cost: 150, description: "Entspannungs-Kurs Gutschein"}

  # GESCHAEFTSLEUTE (5)
  - id: STEFAN_BRANDT
    name: "Stefan Brandt"
    description: "Filialleiter der Sparkasse, diskret und zuverlässig."
    groupIds: [GESCHAEFTSLEUTE]
    networkThreshold: 60
    friendNetwork: [CLARA_RICHTER, MONIKA_SCHAEFER]
    boost: {type: LOAN_INTEREST_REDUCTION, value: 0.01}
    unlockCondition: {type: min-net-worth, amount: 10000}
    giftRequirement:
      cost: 600
      condition: {type: has-cert, certKey: WEITERBILDUNG_BUCHHALTUNG_1}
      description: "Exklusiver Wein + Buchhaltungskenntnisse"

  - id: FRANK_ZIMMERMANN
    name: "Frank Zimmermann"
    description: "Immobilienmakler mit den besten Deals in der Stadt."
    groupIds: [GESCHAEFTSLEUTE]
    networkThreshold: 55
    friendNetwork: [ISABELLA_VON_HOHENSTEIN]
    boost: {type: PROPERTY_PRICE_DISCOUNT, value: 0.05}
    unlockCondition: {type: min-net-worth, amount: 20000}
    giftRequirement:
      cost: 800
      condition: {type: has-cert, certKey: WEITERBILDUNG_IMMOBILIEN_1}
      description: "Hochwertige Geschäftsreise-Tasche"

  - id: MONIKA_SCHAEFER
    name: "Monika Schäfer"
    description: "Personalchefin mit Einfluss auf Bewerbungsverfahren."
    groupIds: [GESCHAEFTSLEUTE]
    networkThreshold: 50
    friendNetwork: [LISA_KRAUSE]
    boost: {type: JOB_ACCEPTANCE_BOOST, value: 0.12}
    unlockCondition: {type: min-net-worth, amount: 15000}
    giftRequirement: {cost: 400, description: "Business-Dinner Einladung"}

  - id: DIRK_HOFFMANN
    name: "Dirk Hoffmann"
    description: "Steuerberater, der gerne ein Auge zudrückt."
    groupIds: [GESCHAEFTSLEUTE, KRIMINELLE]
    networkThreshold: 65
    friendNetwork: [TINA_SCHWARZ]
    boost: {type: TAX_DETECTION_REDUCTION, value: 0.05}
    unlockCondition:
      type: any-of
      conditions:
        - {type: has-relationship-score, personId: JUERGEN_VOGEL, minScore: 60}
        - {type: has-cert, certKey: WEITERBILDUNG_STEUERN_2}
    giftRequirement:
      cost: 1000
      condition: {type: has-cert, certKey: WEITERBILDUNG_STEUERHINTERZIEHUNG_1}
      description: "Offshore-Tipp + Steuerhinterziehungs-Wissen"

  - id: SANDRA_KOCH
    name: "Sandra Koch"
    description: "Erfolgreiche Unternehmerin mit breitem Netzwerk."
    groupIds: [GESCHAEFTSLEUTE]
    networkThreshold: 70
    friendNetwork: [VIKTOR_LEHMANN, SIR_REGINALD]
    boost: {type: SALARY_MULTIPLIER_BONUS, value: 0.03}
    unlockCondition: {type: min-net-worth, amount: 30000}
    giftRequirement: {cost: 1200, description: "Luxus-Uhr"}

  # INVESTOREN (4)
  - id: MARCO_RICHTER
    name: "Marco Richter"
    description: "Ehrgeiziger Trader mit Insider-Kontakten."
    groupIds: [INVESTOREN]
    networkThreshold: 65
    friendNetwork: [CLARA_RICHTER, VIKTOR_LEHMANN]
    boost: {type: STOCK_VOLATILITY_REDUCTION, value: 0.05}
    unlockCondition:
      type: all-of
      conditions:
        - {type: min-net-worth, amount: 50000}
        - {type: has-cert, certKey: WEITERBILDUNG_CRYPTO_1}
    giftRequirement:
      cost: 2000
      condition: {type: has-cert, certKey: WEITERBILDUNG_BUCHHALTUNG_2}
      description: "Exklusive Marktanalyse + Buchhaltungsexpertise"

  - id: CLARA_RICHTER
    name: "Clara Richter"
    description: "Portfoliomanagerin mit Gespür für passive Einkommen."
    groupIds: [INVESTOREN]
    networkThreshold: 70
    friendNetwork: []
    boost: {type: MONTHLY_INCOME_BONUS, value: 150}
    unlockCondition: {type: min-net-worth, amount: 100000}
    giftRequirement: {cost: 3000, description: "Kunstwerk aus bekannter Galerie"}

  - id: VIKTOR_LEHMANN
    name: "Viktor Lehmann"
    description: "Professioneller Pokerspieler, kennt jeden Trick."
    groupIds: [INVESTOREN, KRIMINELLE]
    networkThreshold: 55
    friendNetwork: [RALF_RATTE]
    boost: {type: GAMBLING_LUCK_BOOST, value: 0.03}
    unlockCondition: {type: min-net-worth, amount: 40000}
    giftRequirement: {cost: 1500, description: "Limitierte Spielkarten-Edition"}

  - id: PETRA_STERN
    name: "Petra Stern"
    description: "Sammlerin seltener Objekte mit weltweiten Kontakten."
    groupIds: [INVESTOREN]
    networkThreshold: 60
    friendNetwork: [SOFIA_MARTINEZ]
    boost: {type: COLLECTIBLE_PRICE_DISCOUNT, value: 0.08}
    unlockCondition:
      type: all-of
      conditions:
        - {type: min-net-worth, amount: 60000}
        - {type: owns-collection, collectionName: KUNSTKENNER}
    giftRequirement:
      cost: 2500
      condition: {type: owns-collection, collectionName: UHRMACHER}
      description: "Seltenes Sammlerstück aus Uhrmacher-Kollektion"

  # KRIMINELLE (4)
  - id: RALF_RATTE
    name: "Ralf 'Ratte' König"
    description: "Zwielichtige Gestalt, die immer einen Deal parat hat."
    groupIds: [KRIMINELLE]
    networkThreshold: 40
    friendNetwork: [BOGDAN_NOWAK, TINA_SCHWARZ]
    boost: {type: ROB_SUCCESS_BOOST, value: 0.08}
    unlockCondition:
      type: any-of
      conditions:
        - {type: had-conflict-with, personId: MARCO_RICHTER}
        - {type: min-jail-months-served, months: 1}
    giftRequirement: {cost: 500, description: "Flasche billigen Schnaps + keine Fragen"}

  - id: TINA_SCHWARZ
    name: "Tina Schwarz"
    description: "Expertin für digitale Spuren löschen."
    groupIds: [KRIMINELLE]
    networkThreshold: 55
    friendNetwork: []
    boost: {type: TAX_DETECTION_REDUCTION, value: 0.06}
    unlockCondition: {type: has-relationship-score, personId: RALF_RATTE, minScore: 40}
    giftRequirement:
      cost: 800
      condition: {type: has-cert, certKey: WEITERBILDUNG_HACKER_1}
      description: "Hacking-Tool + technisches Know-how"

  - id: BOGDAN_NOWAK
    name: "Bogdan Nowak"
    description: "Streng schweigsamer Mann mit dunkler Vergangenheit."
    groupIds: [KRIMINELLE]
    networkThreshold: 50
    friendNetwork: []
    boost: {type: ROB_LOOT_MULTIPLIER, value: 0.15}
    unlockCondition: {type: has-relationship-score, personId: RALF_RATTE, minScore: 50}
    giftRequirement: {cost: 600, description: "Bargeld, keine Quittung"}

  - id: UNDERWORLD_KONTAKT
    name: "???"
    description: "Jemand, der von Ralf erwähnt wird, aber nie seinen Namen nennt."
    groupIds: [KRIMINELLE]
    networkThreshold: 80
    friendNetwork: []
    boost: {type: ROB_SUCCESS_BOOST, value: 0.12}
    unlockCondition:
      type: all-of
      conditions:
        - {type: has-relationship-score, personId: BOGDAN_NOWAK, minScore: 60}
        - {type: had-conflict-with, personId: RALF_RATTE}
    giftRequirement: {cost: 2000, description: "Nur Bargeld"}

  # REISELUSTIGE (3)
  - id: LENA_BERG
    name: "Lena Berg"
    description: "Weltenbummlerin mit Rabattcodes für alles."
    groupIds: [REISELUSTIGE]
    networkThreshold: 45
    friendNetwork: [AHMED_KARIMI, SOFIA_MARTINEZ]
    boost: {type: TRAVEL_COST_REDUCTION, value: 0.10}
    unlockCondition: null
    giftRequirement: {cost: 200, description: "Reiseführer für exotisches Land"}

  - id: AHMED_KARIMI
    name: "Ahmed Karimi"
    description: "Vielreisender Journalist mit Kontakten in aller Welt."
    groupIds: [REISELUSTIGE]
    networkThreshold: 55
    friendNetwork: []
    boost: {type: TRAVEL_DURATION_REDUCTION, value: 1}
    unlockCondition: {type: has-relationship-score, personId: LENA_BERG, minScore: 40}
    giftRequirement: {cost: 300, description: "Hochwertige Kamera"}

  - id: SOFIA_MARTINEZ
    name: "Sofia Martinez"
    description: "Antiquitätenhändlerin mit Kontakten zu Sammlern weltweit."
    groupIds: [REISELUSTIGE, INVESTOREN]
    networkThreshold: 60
    friendNetwork: [PETRA_STERN]
    boost: {type: COLLECTIBLE_DROP_RATE_BOOST, value: 0.05}
    unlockCondition: {type: has-relationship-score, personId: LENA_BERG, minScore: 50}
    giftRequirement:
      cost: 400
      condition: {type: owns-collection, collectionName: ARCHAEOLOGIE}
      description: "Seltenes archäologisches Fundstück"

  # ELITE (3)
  - id: SIR_REGINALD
    name: "Sir Reginald Blackwood"
    description: "Britischer Adeliger mit undurchschaubaren Geschäftsinteressen."
    groupIds: [ELITE]
    networkThreshold: 75
    friendNetwork: [ISABELLA_VON_HOHENSTEIN, DR_MAXIMILIAN_FURST]
    boost: {type: SALARY_MULTIPLIER_BONUS, value: 0.08}
    unlockCondition:
      type: all-of
      conditions:
        - {type: min-net-worth, amount: 500000}
        - {type: has-relationship-score, personId: SANDRA_KOCH, minScore: 70}
    giftRequirement:
      cost: 10000
      condition: {type: owns-collection, collectionName: WEINKENNER}
      description: "Château Pétrus 1982 + Weinkenner-Sammlung"

  - id: ISABELLA_VON_HOHENSTEIN
    name: "Isabella von Hohenstein"
    description: "Alte Adels-Familie, enormes Immobilien-Portfolio."
    groupIds: [ELITE]
    networkThreshold: 80
    friendNetwork: []
    boost: {type: PROPERTY_PRICE_DISCOUNT, value: 0.12}
    unlockCondition:
      type: all-of
      conditions:
        - {type: min-net-worth, amount: 700000}
        - {type: has-relationship-score, personId: FRANK_ZIMMERMANN, minScore: 80}
    giftRequirement:
      cost: 15000
      condition: {type: owns-collection, collectionName: KUNSTKENNER}
      description: "Signiertes Kunstwerk + vollständige Kunstkenner-Sammlung"

  - id: DR_MAXIMILIAN_FURST
    name: "Dr. Maximilian Fürst"
    description: "Privatbankier, verwaltet das Vermögen der Reichen."
    groupIds: [ELITE]
    networkThreshold: 85
    friendNetwork: []
    boost: {type: LOAN_INTEREST_REDUCTION, value: 0.03}
    unlockCondition:
      type: all-of
      conditions:
        - {type: min-net-worth, amount: 1000000}
        - {type: has-relationship-score, personId: SIR_REGINALD, minScore: 60}
    giftRequirement:
      cost: 20000
      condition: {type: education-level, prefix: MASTER}
      description: "Limitierte Schweizer Uhr + Master-Abschluss"
```

### Boost-Typen (vollständige Liste)

| Boost-Typ | Wirkt auf | Wie |
|-----------|-----------|-----|
| `HAPPINESS_PER_TURN` | character.happiness | +N pro Monat (wie altes System) |
| `STRESS_REDUCTION_PER_TURN` | character.stress | −N pro Monat |
| `ENERGY_BONUS_PER_TURN` | character.energy | +N pro Monat (capped 100) |
| `HUNGER_DECAY_REDUCTION` | hunger-decay-base | −N (via GameConfig override) |
| `SALARY_BONUS` | Monats-Einkommen | +N€ flat (wie MONTHLY_INCOME_BONUS) |
| `SALARY_MULTIPLIER_BONUS` | Monats-Einkommen | ×(1+N) (wie SALARY_MULTIPLIER) |
| `EXPENSE_REDUCTION` | Monatsausgaben | ×(1−N) (wie EXPENSE_REDUCTION) |
| `LOAN_INTEREST_REDUCTION` | Zinssatz | −N Prozentpunkte |
| `PROPERTY_PRICE_DISCOUNT` | Kaufpreis Immobilien | ×(1−N) |
| `COLLECTIBLE_PRICE_DISCOUNT` | Kaufpreis Sammlerstücke | ×(1−N) |
| `COLLECTIBLE_DROP_RATE_BOOST` | Zufalls-Sale-Event Chance | +N (base 0.20) |
| `STOCK_VOLATILITY_REDUCTION` | Stock-Volatilität | ×(1−N) |
| `TRAVEL_COST_REDUCTION` | Reisekosten | ×(1−N) |
| `TRAVEL_DURATION_REDUCTION` | Reisedauer | −N Monate |
| `GAMBLING_LUCK_BOOST` | Gewinnwahrscheinlichkeit | +N auf alle Win-Tiers |
| `TAX_DETECTION_REDUCTION` | Entdeckungswahrscheinlichkeit | −N |
| `JOB_ACCEPTANCE_BOOST` | Job-Bewerbung Erfolg | +N Wahrscheinlichkeit |
| `SCHUFA_BONUS_MONTHLY` | SCHUFA-Score | +N pro Monat |
| `ROB_SUCCESS_BOOST` | Ausrauben Erfolg-Chance | +N |
| `ROB_LOOT_MULTIPLIER` | Beute beim Ausrauben | ×(1+N) |

---

## Neue Domain-Events

Neue Records in `domain/events/` (alle implementieren DomainEvent):

```java
// Wann immer sich ein Score ändert
RelationshipChangedEvent(Long playerId, String personId, int oldScore, int newScore, String reason)

// Wenn networkThreshold überschritten wird → neue Personen sichtbar
GroupUnlockedEvent(Long playerId, String groupId, String groupName)

// Jeder Rob-Versuch
RobAttemptedEvent(Long playerId, String personId, boolean success, boolean caught, BigDecimal lootAmount)

// Wenn rob-caught 3-Monats-Sperre gesetzt wird
SocialActionLockAppliedEvent(Long playerId, String groupId, int untilTurn)
```

---

## Neue Condition-Implementierungen

In `domain/condition/` (erweitern das bestehende Interface):

```java
MinNetWorthCondition(BigDecimal minAmount)
// → context.character().getNetWorth().compareTo(minAmount) >= 0

OwnsCollectionCondition(String collectionName)
// → context.completedCollections().contains(collectionName)

HasRelationshipScoreCondition(String personId, int minScore)
// → context.relationshipScores().getOrDefault(personId, 0) >= minScore

HadConflictWithCondition(String personId)
// → context.hadConflictsWith().contains(personId)

MinJailMonthsServedCondition(int months)
// → (neu) character.totalJailMonthsServed >= months (neues Feld nötig!)
```

**GameContext-Erweiterung** (neues Feld hinzufügen, Konstruktor + Factory updaten):
```java
public record GameContext(
    GameCharacter character,
    List<String> completedEducationStages,
    String currentCountry,
    boolean traveling,
    int activeJobCount,
    Set<String> completedCollections,        // NEU: CollectionRepository-Query
    Map<String, Integer> relationshipScores, // NEU: PlayerSocialRelationshipRepo-Query
    Set<String> hadConflictsWith             // NEU: PlayerSocialRelationshipRepo-Query
) {}
```

---

## Backend-Implementierung: Schritte

### Schritt 0 — promt.md anlegen (IMMER ZUERST)
Diesen Plan als `promt.md` im Projekt-Root speichern für Cold-Start-Fähigkeit.

### Schritt 1 — DB-Migration V14
Datei: `src/main/resources/db/migration/V14__social_rework.sql`
- Tabellen: `player_social_relationships`, `player_social_group_unlocks`, `social_action_log`
- Optional: Feld `total_jail_months_served INT DEFAULT 0` in `characters` hinzufügen (für MinJailMonthsServedCondition)
- Compile + Test

### Schritt 2 — Entities + Repositories
- `entity/PlayerSocialRelationship.java` + `PlayerSocialRelationshipId.java`
- `entity/PlayerSocialGroupUnlock.java` + `PlayerSocialGroupUnlockId.java`
- `repository/PlayerSocialRelationshipRepository.java`
- `repository/PlayerSocialGroupUnlockRepository.java`
- Compile + Test

### Schritt 3 — PersonService (YAML-Daten laden)
- `src/main/resources/data/persons.yaml` anlegen (36 Personen, 8 Gruppen, YAML oben)
- `domain/social/PersonDef.java` — Record: id, name, description, groupIds, networkThreshold, friendNetwork, boost, unlockCondition, giftRequirement
- `domain/social/GroupDef.java` — Record: id, name, type, exclusiveCondition, description
- `domain/social/BoostDef.java` — Record: type, value
- `domain/social/GiftRequirement.java` — Record: cost, condition (nullable), description
- `service/PersonService.java` — @Service, @PostConstruct lädt persons.yaml, Fallback loadDefaults()
- Compile + Test

### Schritt 4 — Neue Condition-Implementierungen + GameContext erweitern
- `domain/condition/MinNetWorthCondition.java`
- `domain/condition/OwnsCollectionCondition.java`
- `domain/condition/HasRelationshipScoreCondition.java`
- `domain/condition/HadConflictWithCondition.java`
- `domain/condition/MinJailMonthsServedCondition.java` (optional)
- `domain/GameContext.java` — 3 neue Felder hinzufügen
- `domain/GameContextFactory.java` — neue Repos injizieren, neue Felder befüllen
- Unit-Tests für neue Conditions
- Compile + Test

### Schritt 5 — Neue Domain-Events
- `domain/events/RelationshipChangedEvent.java`
- `domain/events/GroupUnlockedEvent.java`
- `domain/events/RobAttemptedEvent.java`
- `domain/events/SocialActionLockAppliedEvent.java`
- Compile + Test

### Schritt 6 — SocialService (Kern-Logik)
- `service/SocialService.java`:
  - `getNetwork(playerId)` → SocialNetworkDto (alle sichtbaren Personen + Scores + Groups)
  - `spendTime(playerId, personId)` → max 4×/Monat, +8 Score, publishEvent
  - `giveGift(playerId, personId)` → GiftRequirement prüfen, Cash abziehen, +20 Score, publishEvent
  - `insult(playerId, personId)` → max 1×/Monat, −15 Score, hadConflict=true, publishEvent
  - `rob(playerId, personId)` → Erfolgschance, Beute oder Caught-Logik, publishEvent
  - `advanceSocials(playerId, events)` → monatlicher Decay (−1/Monat passiv), Counters resetten, Boosts berechnen
  - `getActiveBoosts(playerId)` → List<ActiveBoost(type, totalValue)> für TurnService

**Rob-Logik im Detail:**
```
successChance = 0.20 + (score / 100.0 * 0.30)  // 20–50% je nach Score
caughtChance = 0.50 wenn success=false, 0.20 wenn success=true (aber "zu riskant" abort)
Bei CAUGHT:
  - score des Opfers: −40
  - alle Gruppenmitglieder: −15
  - lockedActionsUntilTurn = currentTurn + 3 für alle Gruppenmitglieder
  - publishEvent(SocialActionLockAppliedEvent)
Bei SUCCESS:
  - loot = (person.wealth * 0.10 * robLootMultiplierBoost)
  - character.cash += loot
  - publishEvent(RobAttemptedEvent success=true caught=false)
```

### Schritt 7 — SocialEventListener
- `listener/SocialEventListener.java`:
  - @TransactionalEventListener(AFTER_COMMIT) auf RelationshipChangedEvent → EventLog-Eintrag
  - @TransactionalEventListener(AFTER_COMMIT) auf GroupUnlockedEvent → EventLog-Eintrag + Toast-freundliche Nachricht
  - @TransactionalEventListener(AFTER_COMMIT) auf RobAttemptedEvent → EventLog-Eintrag

### Schritt 8 — TurnService-Integration
TurnService.endTurn() anpassen:
1. Alten `advanceRelationships()`-Call durch `socialService.advanceSocials(playerId, events)` ersetzen
2. Nach Collection-Bonus-Anwendung: `socialService.getActiveBoosts(playerId)` abrufen und über bonusAppliers-Map anwenden (gleiche Infrastruktur wie CollectionBonusApplier!)
3. Spezielle Boosts die nicht durch bonusAppliers gehen: direkt auf character anwenden (HAPPINESS, STRESS, ENERGY)
4. Neue Boost-Anwendungen in relevanten Services:
   - `LoanService.takeLoan()`: Zinssatz durch LOAN_INTEREST_REDUCTION reduzieren
   - `TravelService.depart()`: Kosten + Dauer durch TRAVEL_* anpassen
   - `RealEstateService.buy()`: Preis durch PROPERTY_PRICE_DISCOUNT reduzieren
   - `CollectibleService.buyCollectible()`: Preis durch COLLECTIBLE_PRICE_DISCOUNT reduzieren
   - `TaxEvasionService.evasionLevel()`: detectionChance durch TAX_DETECTION_REDUCTION reduzieren

### Schritt 9 — SocialController
- `controller/SocialController.java`:
  - `GET /api/social/network` → SocialNetworkDto
  - `POST /api/social/persons/{personId}/time` → SpendTimeResponseDto
  - `POST /api/social/persons/{personId}/gift` → GiftResponseDto
  - `POST /api/social/persons/{personId}/insult` → InsultResponseDto
  - `POST /api/social/persons/{personId}/rob` → RobResponseDto

**DTOs:**
```java
SocialNetworkDto {
    List<PersonNodeDto> persons,       // alle sichtbaren (known + unknown via network)
    List<PersonEdgeDto> edges,         // Verbindungen zwischen Personen (nicht Player-Person)
    List<GroupDto> unlockedGroups,
    List<ActiveBoostDto> activeBoosts
}

PersonNodeDto {
    String personId,
    String displayName,       // "???" wenn nicht met
    String groupId,
    boolean met,              // true wenn player hat Beziehung
    boolean known,            // true wenn met ODER via Netzwerk sichtbar
    int score,                // 0 wenn nicht met
    boolean locked,           // lockedActionsUntilTurn > currentTurn
    int lockedUntilTurn,
    boolean canSpendTime,     // nicht locked, monthlyTimeSpentCount < 4
    boolean canInsult,        // nicht locked, !monthlyInsultDone
    boolean canRob,           // nicht locked, !monthlyRobAttempted
    BoostDef boost,
    List<String> unlockRequirements  // describe() der Conditions
}

PersonEdgeDto { String sourcePersonId, String targetPersonId }
```

### Schritt 10 — Tests
- `RobCaughtFlowTest` — rob → caught → Gruppen-Penalty + 3-Monats-Sperre
- `GroupUnlockThresholdTest` — score >= networkThreshold → neue Personen in network sichtbar
- `ExclusiveGroupGateTest` — net worth < Schwelle → Gruppe nicht freigeschaltet
- `PassiveBoostTurnTest` — Beziehung aktiv → Boost in TurnService angewendet
- `GiftConditionTest` — Geschenk ohne Voraussetzungen → 400 Bad Request
- Unit-Tests für alle neuen Conditions

---

## Frontend-Implementierung

### Schritt 11 — `beziehungen.vue` komplett neuschreiben

**Kein neues npm-Package nötig** — gleiche SVG+Canvas-Technik wie ausbildung.vue.

**Layout:**
```
[Linkes Panel: Netzwerk-Graph (70%)] | [Rechtes Panel: Detail (30%)]
```

**Graph-Rendering (force-free, manuelles Layout):**
- Personen werden in Gruppen-Clustern positioniert (ähnlich wie FAM_DEF in ausbildung.vue)
- NACHBARSCHAFT: links oben, GASTRONOMEN: links mitte, AKADEMIKER: mitte, etc.
- Pan + Zoom wie ausbildung.vue
- SVG-Edges: zwischen PersonEdgeDto-Paaren
- Nodes: Kreis (met=farbig nach Gruppe, unknown=grau, known-not-met=grau mit "???")
- Player-Connections: hervorgehobene Edge-Farbe (accent)
- Score-Ring um jeden Met-Node (wie ein Progress-Ring)

**Detail-Panel (bei Klick):**
- Person-Name, Gruppe, Score-Balken
- Aktive Boosts
- Unlock-Anforderungen (grün/rot je nach erfüllt)
- Aktionsbuttons: Zeit verbringen, Geschenk, Beleidigen, Ausrauben
- Sperranzeige wenn locked

**State management:**
- `network` ref: SocialNetworkDto
- `selectedPersonId` ref
- `loading` ref
- API-Calls via useApi()

**Sidebar-Update:**
- `Sidebar.vue`: Pulsindikator auf /beziehungen wenn neue Personen sichtbar (ähnlich /steuerhinterziehung)

---

## Dateikarte: neue + geänderte Dateien

```
BACKEND — NEUE DATEIEN:
src/main/resources/db/migration/V14__social_rework.sql
src/main/resources/data/persons.yaml
src/main/java/com/financegame/
├── entity/
│   ├── PlayerSocialRelationship.java
│   ├── PlayerSocialRelationshipId.java
│   ├── PlayerSocialGroupUnlock.java
│   └── PlayerSocialGroupUnlockId.java
├── repository/
│   ├── PlayerSocialRelationshipRepository.java
│   └── PlayerSocialGroupUnlockRepository.java
├── domain/
│   ├── social/
│   │   ├── PersonDef.java
│   │   ├── GroupDef.java
│   │   ├── BoostDef.java
│   │   └── GiftRequirement.java
│   ├── condition/
│   │   ├── MinNetWorthCondition.java
│   │   ├── OwnsCollectionCondition.java
│   │   ├── HasRelationshipScoreCondition.java
│   │   └── HadConflictWithCondition.java
│   └── events/
│       ├── RelationshipChangedEvent.java
│       ├── GroupUnlockedEvent.java
│       ├── RobAttemptedEvent.java
│       └── SocialActionLockAppliedEvent.java
├── service/
│   ├── PersonService.java          ← lädt persons.yaml
│   └── SocialService.java          ← Kern-Logik
├── controller/
│   └── SocialController.java
└── listener/
    └── SocialEventListener.java

BACKEND — GEÄNDERTE DATEIEN:
├── domain/GameContext.java               ← 3 neue Felder
├── domain/GameContextFactory.java        ← neue Repos + Felder befüllen
├── service/TurnService.java              ← advanceSocials statt advanceRelationships
├── service/LoanService.java              ← LOAN_INTEREST_REDUCTION
├── service/TravelService.java            ← TRAVEL_* Boosts
├── service/RealEstateService.java        ← PROPERTY_PRICE_DISCOUNT
├── service/CollectibleService.java       ← COLLECTIBLE_PRICE_DISCOUNT
└── service/TaxEvasionService.java        ← TAX_DETECTION_REDUCTION

FRONTEND — GEÄNDERTE DATEIEN:
frontend/pages/beziehungen.vue            ← komplett neuschreiben
frontend/components/Sidebar.vue           ← Pulsindikator für neue Kontakte

ALTES SYSTEM (bleibt unberührt, wird nicht gelöscht):
entity/Npc.java, PlayerRelationship.java
service/RelationshipService.java
controller/RelationshipController.java
db/migration/V5__relationships.sql, V6__fix_npc_id_types.sql
```

---

## Verifikation

```bash
# Nach jedem Schritt:
cd /home/ben/Documents/game/progressiongame/backend
/usr/bin/mvn compile -q
/usr/bin/mvn test -q

# End-to-End nach Schritt 10:
# 1. App starten, Player registrieren
# 2. GET /api/social/network → Hans Müller + Karl Weber sichtbar (kein unlock needed)
# 3. POST /api/social/persons/HANS_MUELLER/time 4× → 4×+8 = 32 Score
# 4. Score >= 30 → GRETA_SCHMIDT wird sichtbar als "???"
# 5. POST /api/social/persons/HANS_MUELLER/time 5× → 409 Conflict (Monat-Limit)
# 6. Turn abschliessen → Counters reset, passive Boosts angewendet
# 7. POST /api/social/persons/KARL_WEBER/rob → Erfolg oder Caught-Flow
# 8. Frontend: beziehungen.vue öffnen → Node-Graph zeigt Netzwerk
```

---

## Offene Entscheidungen (bereits beantwortet)

- **Visualization library**: kein D3.js, stattdessen custom SVG (wie ausbildung.vue) — kein neues npm-Package
- **Persons als YAML**: PersonService wie EducationService, Java-Fallback in loadDefaults()
- **Altes NPC-System**: bleibt im Code, wird nicht gelöscht (DB-Kompatibilität)
- **Rob-Loot-Berechnung**: % vom wealth des Opfers (in PersonDef als `wealthLevel: 1-5` encoded)
- **Score-Decay**: −1 pro Monat passiv für alle aktiven Beziehungen (nicht für "not met")
- **Gift-Cooldown**: kein explizites Limit (kann beliebig oft geschenkt werden, wenn Cash vorhanden)
