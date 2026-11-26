USE eduvibe;

SHOW TABLES LIKE '%follow%';

USE eduvibe;

CREATE TABLE IF NOT EXISTS follows (
    id INT AUTO_INCREMENT PRIMARY KEY,
    follower_id INT NOT NULL,
    following_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY unique_follow (follower_id, following_id),

    FOREIGN KEY (follower_id) REFERENCES users(id)
        ON DELETE CASCADE,

    FOREIGN KEY (following_id) REFERENCES users(id)
        ON DELETE CASCADE
);

SELECT
    follower_id,
    following_id,
    created_at
FROM follows
ORDER BY created_at DESC;