-- EduVibe: classification diagnostics + safe repair. Nothing here deletes a row.
USE eduvibe;

-- 1) READ-ONLY: how many posts sit in each of the four destinations
SELECT category, is_reel, COUNT(*) AS posts
FROM posts GROUP BY category, is_reel ORDER BY category, is_reel;

-- 2) READ-ONLY: posts that will NOT appear in any public feed (bad / missing category)
SELECT id, user_id, title, category, is_reel, media_type, created_at
FROM posts
WHERE category IS NULL OR category NOT IN ('academic','entertainment');

-- 3) OPTIONAL repair, only for harmless spelling variants (case / spaces)
--    e.g. 'Academic ', 'ENTERTAINMENT'. Run the SELECT first to see what would change.
SELECT id, category, LOWER(TRIM(category)) AS fixed
FROM posts
WHERE category IS NOT NULL AND category NOT IN ('academic','entertainment')
  AND LOWER(TRIM(category)) IN ('academic','entertainment');

UPDATE posts
SET category = LOWER(TRIM(category))
WHERE category IS NOT NULL AND category NOT IN ('academic','entertainment')
  AND LOWER(TRIM(category)) IN ('academic','entertainment');

-- 4) Anything still returned by query 2 is unknown: do NOT guess. Open Admin > Post Moderation,
--    filter "Invalid / Missing", select the post and press "Set Category".

-- 5) READ-ONLY: videos that are regular posts vs reels (check nothing was mixed up by hand)
SELECT media_type, is_reel, COUNT(*) FROM posts GROUP BY media_type, is_reel;
