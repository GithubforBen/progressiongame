-- V23: Add all countries referenced in collectibles.yaml that were missing from the countries table.
-- Deutschland is NOT added here: home country uses currentCountry = NULL (see CollectibleService).
-- Internet is NOT added here: treated as "accessible from anywhere" in CollectibleService.

INSERT INTO countries (name, travel_cost, travel_months, emoji, description) VALUES
('Ägypten',     1400.00, 2, '🇪🇬', 'Pharaonenland am Nil — Pyramiden und antike Schätze'),
('Argentinien', 2800.00, 3, '🇦🇷', 'Tango, Steak und die Weiten Patagoniens'),
('Australien',  3500.00, 3, '🇦🇺', 'Down Under — Kängurus, Koalas und das Great Barrier Reef'),
('Belgien',      300.00, 1, '🇧🇪', 'Schokolade, Bier und der Sitz der EU'),
('Brasilien',   2500.00, 3, '🇧🇷', 'Karneval, Amazonas und der Zuckerhut'),
('Dänemark',     400.00, 1, '🇩🇰', 'LEGO, Design und die schönsten Fahrradstädte'),
('Frankreich',   600.00, 1, '🇫🇷', 'Haute Cuisine, Mode und das Louvre'),
('Griechenland',1000.00, 1, '🇬🇷', 'Antike Tempel, blaue Dome und Mittelmeerküche'),
('Guatemala',   2500.00, 3, '🇬🇹', 'Maya-Ruinen im Dschungel und bunte Märkte'),
('Guyana',      2800.00, 3, '🇬🇾', 'Unberührter Regenwald und karibisches Flair'),
('Indien',      2200.00, 2, '🇮🇳', 'Taj Mahal, Bollywood und die Gewürzküste'),
('Kanada',      2000.00, 2, '🇨🇦', 'Niagara Falls, Ahornsirup und endlose Wälder'),
('Mauritius',   1800.00, 2, '🇲🇺', 'Inselparadies im Indischen Ozean'),
('Mexiko',      2200.00, 3, '🇲🇽', 'Aztekenerbe, Tequila und Chichen Itza'),
('Niederlande',  300.00, 1, '🇳🇱', 'Tulpen, Windmühlen und die Amsterdamer Grachten'),
('Österreich',   300.00, 1, '🇦🇹', 'Mozart, Schnitzel und die Wiener Kaffeehauskultur'),
('Pakistan',    1800.00, 2, '🇵🇰', 'Karakorum, Seidenstraße und uralte Zivilisationen'),
('Peru',        2600.00, 3, '🇵🇪', 'Machu Picchu, Inkas und Amazonas-Quellen'),
('Russland',    1200.00, 2, '🇷🇺', 'Roter Platz, Zaren-Paläste und sibirische Wildnis'),
('Spanien',      800.00, 1, '🇪🇸', 'Flamenco, Tapas und die Sagrada Família'),
('Südafrika',   1600.00, 2, '🇿🇦', 'Safari, Kap der Guten Hoffnung und Buntheit'),
('Tibet',       2800.00, 3, '🏔️', 'Dach der Welt — buddhistische Klöster im Himalaya'),
('Tschechien',   400.00, 1, '🇨🇿', 'Prag, Böhmisches Bier und gotische Altstadt'),
('Türkei',       900.00, 1, '🇹🇷', 'Bosporus, Hagia Sophia und türkische Köstlichkeiten')
ON CONFLICT (name) DO NOTHING;
