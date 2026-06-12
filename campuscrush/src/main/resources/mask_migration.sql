-- Per-conversation mask migration
--
-- STEP 1 — run BEFORE deploying the mask backend (old code keeps working):
ALTER TABLE confessions ADD COLUMN sender_alias VARCHAR(60);
ALTER TABLE users ALTER COLUMN display_alias DROP NOT NULL;

-- Then deploy. MaskBackfillRunner assigns inbox-unique masks to all existing
-- confessions on startup (self-healing, no-op once done). Recipients' existing
-- threads get renamed once; masks are stable afterwards.

-- STEP 2 — run AFTER the new backend is live (entity no longer maps it;
-- Hibernate validate ignores extra DB columns, so timing is flexible):
ALTER TABLE users DROP COLUMN display_alias;
