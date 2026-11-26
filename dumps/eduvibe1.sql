USE eduvibe;

SELECT
    id,
    user_id,
    type,
    message,
    is_read,
    created_at
FROM notifications
ORDER BY created_at DESC;