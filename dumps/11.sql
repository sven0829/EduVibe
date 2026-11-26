SELECT
    id,
    post_id,
    admin_id,
    action,
    note,
    created_at
FROM moderation_logs
ORDER BY id DESC;
SELECT COUNT(*) AS total_logs
FROM moderation_logs;