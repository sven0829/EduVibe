USE eduvibe;
SHOW TABLES LIKE 'reports';
DESCRIBE reports;

USE eduvibe;

SELECT *
FROM reports
ORDER BY created_at DESC;

SELECT * FROM reports ORDER BY created_at DESC;

USE eduvibe;

SELECT id, title, moderation_status, flagged_reason
FROM posts
ORDER BY id DESC;