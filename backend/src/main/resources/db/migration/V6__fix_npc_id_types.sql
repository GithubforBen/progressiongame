-- V6: Fix npcs and player_relationships id columns to BIGINT
-- V5 used SERIAL (INTEGER), but the Hibernate entities map id to Long (BIGINT).

ALTER TABLE player_relationships ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE player_relationships_id_seq AS BIGINT;

ALTER TABLE npcs ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE npcs_id_seq AS BIGINT;
