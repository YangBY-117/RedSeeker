-- 创建用户表、用户浏览历史表、景点评价表（SQLite）
PRAGMA foreign_keys = ON;

-- users 表
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT (datetime('now')),
    last_login DATETIME
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- user_browse_history 表
CREATE TABLE IF NOT EXISTS user_browse_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    attraction_id INTEGER NOT NULL,
    browse_time DATETIME NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (attraction_id) REFERENCES attractions(id)
);

CREATE INDEX IF NOT EXISTS idx_user_browse_user_id ON user_browse_history(user_id);
CREATE INDEX IF NOT EXISTS idx_user_browse_attraction_id ON user_browse_history(attraction_id);
CREATE INDEX IF NOT EXISTS idx_user_browse_time ON user_browse_history(browse_time);

-- attraction_ratings 表
CREATE TABLE IF NOT EXISTS attraction_ratings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    attraction_id INTEGER NOT NULL,
    user_id INTEGER,
    rating INTEGER NOT NULL CHECK(rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at DATETIME NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (attraction_id) REFERENCES attractions(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_rating_attraction_id ON attraction_ratings(attraction_id);
CREATE INDEX IF NOT EXISTS idx_rating_user_id ON attraction_ratings(user_id);

-- 注意：
-- 1) 本文件假设项目还存在 `attractions` 表（由 red_tourism.db 或其他迁移提供）。
-- 2) 不插入测试数据；使用脚本 `../scripts/create_test_users.js` 创建测试用户并生成 bcrypt 哈希密码。
