SELECT *
FROM notifications
WHERE type = 'reward'
ORDER BY created_at DESC
LIMIT 20;