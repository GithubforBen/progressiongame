-- Tracks remaining months of Finanzamt audit (disables Steuerhinterziehung)
ALTER TABLE characters ADD COLUMN finanzamt_audit_months_remaining INT NOT NULL DEFAULT 0;
