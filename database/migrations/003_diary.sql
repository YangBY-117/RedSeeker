-- 003_diary.sql
-- 增加旅行日记表与图片表
PRAGMA foreign_keys = ON;

-- 日记主表：支持关联用户与景点，包含标题与长文内容
CREATE TABLE IF NOT EXISTS diaries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    attraction_id INTEGER,
    title TEXT NOT NULL,
    content TEXT,
    is_public INTEGER NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT (datetime('now')),
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (attraction_id) REFERENCES attractions(id)
);

CREATE INDEX IF NOT EXISTS idx_diaries_user_id ON diaries(user_id);
CREATE INDEX IF NOT EXISTS idx_diaries_attraction_id ON diaries(attraction_id);

-- 日记图片表：支持存储相对文件路径或二进制数据（image_blob），并按 display_order 排序
CREATE TABLE IF NOT EXISTS diary_images (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    diary_id INTEGER NOT NULL,
    file_path TEXT,
    image_blob BLOB,
    caption TEXT,
    display_order INTEGER DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (diary_id) REFERENCES diaries(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_diary_images_diary_id ON diary_images(diary_id);

-- 注意：推荐存储图片为文件并使用 `file_path` 字段保存相对路径，
-- 这样可以避免数据库文件过大。若需要可选地存储二进制数据，请使用 `image_blob` 字段。
