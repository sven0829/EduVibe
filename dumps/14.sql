ALTER TABLE moderation_logs
MODIFY COLUMN action ENUM(
    'flagged',
    'approved',
    'removed',
    'edited_by_owner',
    'deleted_by_owner',
    'user_suspended',
    'user_banned',
    'role_promoted',
    'role_demoted',
    'report_reviewed',
    'report_dismissed'
) NOT NULL;