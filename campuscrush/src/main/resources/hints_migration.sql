-- Per-conversation reveal kits (hints can no longer correlate a sender across inboxes)
--
-- STEP 1 — run BEFORE deploying the per-conversation hints backend:
ALTER TABLE reveal_kits ADD COLUMN confession_id BIGINT UNIQUE REFERENCES confessions(id);
ALTER TABLE reveal_kits ALTER COLUMN user_id DROP NOT NULL;

-- Copy each sender's existing kit into every thread they sent, so live
-- guessing panels keep working. New threads start with no kit.
INSERT INTO reveal_kits (confession_id, hint1, hint2, hint3, guessing_enabled, created_at, updated_at)
SELECT c.id, k.hint1, k.hint2, k.hint3, k.guessing_enabled, k.created_at, k.updated_at
FROM reveal_kits k
JOIN confessions c ON c.sender_id = k.user_id
WHERE k.confession_id IS NULL;

-- Remove the old per-user rows (their data now lives on per-thread rows)
DELETE FROM reveal_kits WHERE confession_id IS NULL;

-- STEP 2 — run AFTER the new backend is live:
ALTER TABLE reveal_kits DROP COLUMN user_id;
