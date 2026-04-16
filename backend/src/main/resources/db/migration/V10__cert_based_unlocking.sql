-- ============================================================
-- V10: Cert-based unlocking replaces investment level system
-- ============================================================

-- Drop the old level tracking table (replaced by cert checks)
DROP TABLE IF EXISTS player_investment_levels;

-- Real estate: replace level_required with required_cert (nullable)
ALTER TABLE real_estate_catalog ADD COLUMN required_cert VARCHAR(100);
ALTER TABLE real_estate_catalog DROP COLUMN level_required;

-- Stocks: add required_cert (nullable = free)
ALTER TABLE stocks ADD COLUMN required_cert VARCHAR(100);

-- Set required_cert per stock type
UPDATE stocks SET required_cert = 'WEITERBILDUNG_BUCHHALTUNG_1' WHERE type = 'ETF';
UPDATE stocks SET required_cert = 'WEITERBILDUNG_BUCHHALTUNG_2' WHERE type = 'DIVIDEND_STOCK';
UPDATE stocks SET required_cert = 'WEITERBILDUNG_IMMOBILIEN_1'  WHERE type = 'BOND';
UPDATE stocks SET required_cert = 'WEITERBILDUNG_IMMOBILIEN_1'  WHERE type = 'REIT';
UPDATE stocks SET required_cert = 'WEITERBILDUNG_CRYPTO_1'      WHERE type = 'CRYPTO';
UPDATE stocks SET required_cert = 'WEITERBILDUNG_CRYPTO_2'      WHERE type IN ('LEVERAGE', 'WARRANT');
UPDATE stocks SET required_cert = 'WEITERBILDUNG_CRYPTO_3'      WHERE type IN ('SHORT', 'FUTURES');
-- NORMAL and MEME stay NULL (free)

-- Collections: add required_cert (nullable = free)
ALTER TABLE collections ADD COLUMN required_cert VARCHAR(100);
