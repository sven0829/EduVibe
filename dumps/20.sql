

CREATE DATABASE IF NOT EXISTS eduvibe CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eduvibe;

-- ---------------------------------------------------------------- users
CREATE TABLE IF NOT EXISTS users (
    id                          INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name                   VARCHAR(100) NOT NULL,
    username                    VARCHAR(50)  NOT NULL,
    email                       VARCHAR(120) NOT NULL,
    password_hash               VARCHAR(255) NOT NULL,          -- BCrypt hash, never the password
    role                        VARCHAR(20)  NOT NULL DEFAULT 'student',   -- 'admin' for administrators
    status                      VARCHAR(20)  NOT NULL DEFAULT 'active',    -- active / suspended / banned
    points                      INT          NOT NULL DEFAULT 0,           -- reward points, no cash value
    bio                         TEXT NULL,
    profile_pic                 VARCHAR(255) NULL,
    entertainment_limit_minutes INT NULL,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- posts (reels are posts with is_reel = 1)
CREATE TABLE IF NOT EXISTS posts (
    id                INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id           INT NOT NULL,
    category          VARCHAR(20)  NOT NULL,                    -- academic / entertainment
    title             VARCHAR(255) NOT NULL,
    description       TEXT NULL,
    media_type        VARCHAR(20)  NULL,                        -- image / video / none
    media_path        VARCHAR(255) NULL,                        -- e.g. uploads/<file>
    is_reel           TINYINT(1)   NOT NULL DEFAULT 0,
    moderation_status VARCHAR(20)  NOT NULL DEFAULT 'clean',    -- clean / approved / flagged / removed
    flagged_reason    VARCHAR(255) NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_posts_user (user_id),
    KEY idx_posts_created (created_at),
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- comments
CREATE TABLE IF NOT EXISTS comments (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    post_id    INT NOT NULL,
    user_id    INT NOT NULL,
    content    TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_comments_post (post_id),
    KEY idx_comments_user (user_id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS comment_replies (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    comment_id INT NOT NULL,
    user_id    INT NOT NULL,
    content    TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_comment_replies_comment (comment_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS comment_likes (
    comment_id INT NOT NULL,
    user_id    INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, user_id),
    KEY idx_comment_likes_user (user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- likes, follows, saved posts
CREATE TABLE IF NOT EXISTS post_likes (
    post_id    INT NOT NULL,
    user_id    INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, user_id),
    KEY idx_post_likes_user (user_id),
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS follows (
    follower_id  INT NOT NULL,
    following_id INT NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, following_id),
    KEY idx_follows_following (following_id),
    CONSTRAINT fk_follows_follower  FOREIGN KEY (follower_id)  REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_following FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS saved_posts (
    user_id  INT NOT NULL,
    post_id  INT NOT NULL,
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id),
    KEY idx_saved_posts_post (post_id),
    CONSTRAINT fk_saved_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- notifications
CREATE TABLE IF NOT EXISTS notifications (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    type       VARCHAR(30)  NULL,                               -- notification kind, e.g. like, comment
    message    VARCHAR(500) NOT NULL,
    is_read    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notifications_user (user_id, is_read),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- moderation
CREATE TABLE IF NOT EXISTS reports (
    id          INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    post_id     INT NOT NULL,
    reporter_id INT NULL,
    source      VARCHAR(20)  NOT NULL DEFAULT 'user',           -- user / system
    reason      VARCHAR(500) NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'pending',        -- pending / reviewed / dismissed ...
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_reports_post (post_id),
    KEY idx_reports_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS moderation_logs (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    admin_id   INT NULL,
    post_id    INT NULL,
    action     VARCHAR(50)  NOT NULL,
    note       VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_moderation_logs_created (created_at)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- points and digital rewards (no monetary value)
CREATE TABLE IF NOT EXISTS point_transactions (
    id          INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    points      INT NOT NULL,
    action_type VARCHAR(50) NOT NULL,                           -- e.g. daily_login, rng_spin
    ref_key     VARCHAR(100) NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_point_tx_user (user_id, created_at),
    CONSTRAINT fk_point_tx_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rewards (
    id          INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    reward_type VARCHAR(30)  NOT NULL,                          -- avatar_frame / banner / theme / badge
    rarity      VARCHAR(20)  NOT NULL DEFAULT 'common',         -- common / uncommon / rare / epic / legendary
    point_cost  INT NOT NULL DEFAULT 0,
    is_active   TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_rewards (
    id           INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    reward_id    INT NOT NULL,
    obtained_via VARCHAR(30) NULL,                              -- e.g. 'purchase'
    KEY idx_user_rewards_user (user_id),
    CONSTRAINT fk_user_rewards_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE,
    CONSTRAINT fk_user_rewards_reward FOREIGN KEY (reward_id) REFERENCES rewards (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_equipped (
    user_id   INT NOT NULL,
    slot      VARCHAR(20) NOT NULL,
    reward_id INT NOT NULL,
    PRIMARY KEY (user_id, slot)
) ENGINE=InnoDB;

-- Quick check after running:  SHOW TABLES;   (you should see 15 tables)
