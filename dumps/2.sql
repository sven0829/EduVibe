SELECT
    id,
    user_id,
    points,
    action_type,
    ref_key,
    created_at
FROM point_transactions
WHERE action_type = 'daily_login'
ORDER BY created_at DESC
LIMIT 20;