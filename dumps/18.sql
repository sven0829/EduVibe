-- OPTIONAL. EduVibe creates these three NEW tables by itself the first time they are needed.
-- Run this file only if your MySQL user is not allowed to CREATE tables.
-- It does not change or delete anything that already exists.
USE eduvibe;

CREATE TABLE IF NOT EXISTS comment_likes (
    comment_id INT NOT NULL,
    user_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, user_id),
    KEY idx_comment_likes_user (user_id)
);

CREATE TABLE IF NOT EXISTS comment_replies (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    comment_id INT NOT NULL,
    user_id INT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_comment_replies_comment (comment_id)
);

CREATE TABLE IF NOT EXISTS user_equipped (
    user_id INT NOT NULL,
    slot VARCHAR(20) NOT NULL,
    reward_id INT NOT NULL,
    PRIMARY KEY (user_id, slot)
);
