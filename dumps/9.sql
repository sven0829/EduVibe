USE eduvibe;

SELECT post_id, COUNT(*) AS pending_reports
FROM reports
WHERE status = 'pending'
  AND source = 'user'
GROUP BY post_id;

SELECT id, title, moderation_status, flagged_reason
FROM posts;