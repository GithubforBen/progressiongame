ALTER TABLE characters
  ADD COLUMN tax_evasion_active         BOOLEAN        NOT NULL DEFAULT false,
  ADD COLUMN tax_evasion_caught_pending BOOLEAN        NOT NULL DEFAULT false,
  ADD COLUMN cumulative_evaded_taxes    NUMERIC(15,2)  NOT NULL DEFAULT 0,
  ADD COLUMN jail_months_remaining      INT            NOT NULL DEFAULT 0,
  ADD COLUMN exile_months_remaining     INT            NOT NULL DEFAULT 0;
