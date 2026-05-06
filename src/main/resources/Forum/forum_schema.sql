-- ============================================================
-- Shadow Dimensions — Forum Module Database Schema
-- Run this script against the shadow_dimensions database.
-- ============================================================

CREATE TABLE IF NOT EXISTS forum_post (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(300)  NOT NULL,
    content     TEXT          NOT NULL,
    category    VARCHAR(100)  NOT NULL DEFAULT 'General',
    image_url   VARCHAR(500)  NULL,
    votes       INT           NOT NULL DEFAULT 0,
    author_id   INT           NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS forum_commentaire (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    content     TEXT          NOT NULL,
    post_id     INT           NOT NULL,
    author_id   INT           NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES forum_post(id) ON DELETE CASCADE
);
