USE eduvibe;

UPDATE users
SET role = 'admin'
WHERE username = 'stvn';

SELECT id, full_name, username, role
FROM users
WHERE username = 'stvn';

USE eduvibe;

SELECT id, full_name, username, role, status
FROM users
ORDER BY id;