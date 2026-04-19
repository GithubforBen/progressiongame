-- V11: Extend stocks with advanced types (ETF, DIVIDEND, BOND, REIT, CRYPTO, LEVERAGE, WARRANT, SHORT, FUTURES)

-- Widen type column and drop the old 2-value check constraint
ALTER TABLE stocks ALTER COLUMN type TYPE VARCHAR(20);
ALTER TABLE stocks DROP CONSTRAINT stocks_type_check;

-- ETF (requires WEITERBILDUNG_BUCHHALTUNG_1)
INSERT INTO stocks (name, ticker, type, current_price, required_cert) VALUES
  ('iShares MSCI World ETF',   'MSCI', 'ETF', 85.00,  'WEITERBILDUNG_BUCHHALTUNG_1'),
  ('Vanguard FTSE All-World',  'VWRL', 'ETF', 110.00, 'WEITERBILDUNG_BUCHHALTUNG_1')
ON CONFLICT (ticker) DO NOTHING;

-- DIVIDEND_STOCK (requires WEITERBILDUNG_BUCHHALTUNG_2)
INSERT INTO stocks (name, ticker, type, current_price, required_cert) VALUES
  ('Allianz AG',         'ALVN', 'DIVIDEND_STOCK', 240.00, 'WEITERBILDUNG_BUCHHALTUNG_2'),
  ('Deutsche Telekom',   'DTLK', 'DIVIDEND_STOCK',  22.00, 'WEITERBILDUNG_BUCHHALTUNG_2')
ON CONFLICT (ticker) DO NOTHING;

-- BOND (requires WEITERBILDUNG_IMMOBILIEN_1)
INSERT INTO stocks (name, ticker, type, current_price, required_cert) VALUES
  ('Bundesanleihe 10J',      'BUND', 'BOND', 98.50,  'WEITERBILDUNG_IMMOBILIEN_1'),
  ('Unternehmensanleihe A',  'CORP', 'BOND', 102.00, 'WEITERBILDUNG_IMMOBILIEN_1')
ON CONFLICT (ticker) DO NOTHING;

-- REIT (requires WEITERBILDUNG_IMMOBILIEN_1)
INSERT INTO stocks (name, ticker, type, current_price, required_cert) VALUES
  ('DIC Asset AG',     'DICA', 'REIT', 9.50, 'WEITERBILDUNG_IMMOBILIEN_1'),
  ('Hamborner REIT',   'HMBR', 'REIT', 8.20, 'WEITERBILDUNG_IMMOBILIEN_1')
ON CONFLICT (ticker) DO NOTHING;

-- CRYPTO (requires WEITERBILDUNG_CRYPTO_1)
INSERT INTO stocks (name, ticker, type, current_price, required_cert) VALUES
  ('Bitcoin',   'BTC', 'CRYPTO', 42000.00, 'WEITERBILDUNG_CRYPTO_1'),
  ('Ethereum',  'ETH', 'CRYPTO',  2800.00, 'WEITERBILDUNG_CRYPTO_1')
ON CONFLICT (ticker) DO NOTHING;

-- LEVERAGE (requires WEITERBILDUNG_CRYPTO_2)
INSERT INTO stocks (name, ticker, type, current_price, required_cert) VALUES
  ('2x Tech Hebel',  'TCH2', 'LEVERAGE', 35.00, 'WEITERBILDUNG_CRYPTO_2'),
  ('3x DAX Bull',    'DAX3', 'LEVERAGE', 18.00, 'WEITERBILDUNG_CRYPTO_2')
ON CONFLICT (ticker) DO NOTHING;

-- WARRANT (requires WEITERBILDUNG_CRYPTO_2)
INSERT INTO stocks (name, ticker, type, current_price, required_cert) VALUES
  ('Call Option DAX',      'CDAX', 'WARRANT', 4.20, 'WEITERBILDUNG_CRYPTO_2'),
  ('Put Option TechCorp',  'PTCH', 'WARRANT', 2.80, 'WEITERBILDUNG_CRYPTO_2')
ON CONFLICT (ticker) DO NOTHING;

-- SHORT (requires WEITERBILDUNG_CRYPTO_3)
INSERT INTO stocks (name, ticker, type, current_price, required_cert) VALUES
  ('Short MSCI World',  'SMSC', 'SHORT', 52.00, 'WEITERBILDUNG_CRYPTO_3'),
  ('Bear DAX',          'BDAX', 'SHORT', 28.00, 'WEITERBILDUNG_CRYPTO_3')
ON CONFLICT (ticker) DO NOTHING;

-- FUTURES (requires WEITERBILDUNG_CRYPTO_3)
INSERT INTO stocks (name, ticker, type, current_price, required_cert) VALUES
  ('Gold Future',  'GOLDF', 'FUTURES', 1950.00, 'WEITERBILDUNG_CRYPTO_3'),
  ('Öl Future',    'OILF',  'FUTURES',   82.00, 'WEITERBILDUNG_CRYPTO_3')
ON CONFLICT (ticker) DO NOTHING;
