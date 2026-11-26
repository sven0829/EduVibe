-- Optional: the app creates this table by itself the first time it is needed. Safe to run by hand too.
USE eduvibe;
CREATE TABLE IF NOT EXISTS post_download_blocked (
    post_id INT NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
