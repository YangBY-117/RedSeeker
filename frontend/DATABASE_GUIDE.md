# 数据库开发指南

## 概述

RedSeeker 系统的数据库部分，使用 SQLite 数据库。**登录功能所需的表结构已就绪**，后端可以直接操作数据库实现登录功能。

## 数据库表结构

### 1. 用户表 (users) ✅ 已就绪

**表结构**（已创建在 `users.db` 或 `red_tourism.db` 中）：

```sql
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login DATETIME
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
```

**字段说明：**
- `id`: 用户ID，自增主键
- `username`: 用户名，唯一，用于登录
- `password`: 加密后的密码（bcrypt hash，已使用 bcryptjs 加密）
- `created_at`: 创建时间
- `last_login`: 最后登录时间

**测试用户**：已创建 10 个测试用户（`user1` 到 `user10`），密码统一为 `password123`

### 2. 景点表 (attractions)

**注意**：此表已由景点数据库（red_tourism.db）提供，数据库开发者需要确保可以访问以下字段：

- `id`: 景点ID
- `name`: 景点名称
- `address`: 地址
- `longitude`: 经度
- `latitude`: 纬度
- `category`: 类别（1-纪念馆, 2-烈士陵园, 3-会议旧址, 4-战役遗址, 5-名人故居, 6-革命根据地, 7-纪念碑塔, 8-博物馆, 9-其他纪念地）
- `brief_intro`: 简介
- `historical_background`: 历史背景
- `per_capita_consumption`: 人均消费
- `business_hours`: 营业时间
- `created_at`: 创建时间
- `updated_at`: 更新时间

### 2.1 历史事件表 (historical_events)

**注意**：此表已由景点数据库（red_tourism.db）提供，用于路线规划的历史阶段排序：

- `id`: 事件ID
- `event_name`: 事件名称
- `start_year`: 开始年份
- `end_year`: 结束年份
- `description`: 事件描述
- `period`: 历史时期（如：建党初期、土地革命战争时期等）

### 2.2 景点-事件关联表 (attraction_events)

**注意**：此表已由景点数据库（red_tourism.db）提供，用于关联景点和历史事件：

- `id`: 关联ID
- `attraction_id`: 景点ID
- `event_id`: 事件ID

**用途**：通过此表可以查询景点关联的历史事件，用于路线规划时按历史阶段排序

### 3. 用户浏览历史表 (user_browse_history)

```sql
CREATE TABLE IF NOT EXISTS user_browse_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    attraction_id INTEGER NOT NULL,
    browse_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (attraction_id) REFERENCES attractions(id)
);

CREATE INDEX IF NOT EXISTS idx_user_browse_user_id ON user_browse_history(user_id);
CREATE INDEX IF NOT EXISTS idx_user_browse_attraction_id ON user_browse_history(attraction_id);
CREATE INDEX IF NOT EXISTS idx_user_browse_time ON user_browse_history(browse_time);
```

**字段说明：**
- `id`: 记录ID
- `user_id`: 用户ID
- `attraction_id`: 景点ID
- `browse_time`: 浏览时间

**用途**：记录用户浏览历史，用于个性化推荐

### 4. 景点评价表 (attraction_ratings)

```sql
CREATE TABLE IF NOT EXISTS attraction_ratings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    attraction_id INTEGER NOT NULL,
    user_id INTEGER,
    rating INTEGER NOT NULL CHECK(rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (attraction_id) REFERENCES attractions(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_rating_attraction_id ON attraction_ratings(attraction_id);
CREATE INDEX IF NOT EXISTS idx_rating_user_id ON attraction_ratings(user_id);
```

**字段说明：**
- `id`: 评价ID
- `attraction_id`: 景点ID
- `user_id`: 用户ID（可为空，支持匿名评价）
- `rating`: 评分（1-5分）
- `comment`: 评价内容
- `created_at`: 创建时间

**用途**：存储用户评价，计算景点平均评分和热度

### 5. 旅游日记表 (travel_diaries)

```sql
CREATE TABLE IF NOT EXISTS travel_diaries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    content_compressed BLOB,  -- 压缩后的内容（可选，用于大数据量优化）
    destination TEXT,  -- 旅游目的地
    travel_date DATE,  -- 旅游日期
    view_count INTEGER NOT NULL DEFAULT 0,  -- 浏览量（热度）
    average_rating REAL DEFAULT 0,  -- 平均评分
    total_ratings INTEGER DEFAULT 0,  -- 评分总数
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_diary_user_id ON travel_diaries(user_id);
CREATE INDEX IF NOT EXISTS idx_diary_destination ON travel_diaries(destination);
CREATE INDEX IF NOT EXISTS idx_diary_created_at ON travel_diaries(created_at);
CREATE INDEX IF NOT EXISTS idx_diary_view_count ON travel_diaries(view_count);
CREATE INDEX IF NOT EXISTS idx_diary_average_rating ON travel_diaries(average_rating);
-- 全文检索索引（SQLite FTS5）
CREATE VIRTUAL TABLE IF NOT EXISTS travel_diaries_fts USING fts5(
    title, content, destination, content=travel_diaries, content_rowid=id
);
```

**字段说明：**
- `id`: 日记ID，自增主键
- `user_id`: 作者用户ID
- `title`: 日记标题
- `content`: 日记正文内容（未压缩）
- `content_compressed`: 压缩后的内容（使用无损压缩算法，如gzip）
- `destination`: 旅游目的地（用于按目的地搜索）
- `travel_date`: 旅游日期
- `view_count`: 浏览量（用于计算热度）
- `average_rating`: 平均评分（1-5分）
- `total_ratings`: 评分总数
- `created_at`: 创建时间
- `updated_at`: 更新时间

**用途**：存储用户撰写的旅游日记，支持文字、图片、视频等多媒体内容

### 6. 日记媒体文件表 (diary_media)

```sql
CREATE TABLE IF NOT EXISTS diary_media (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    diary_id INTEGER NOT NULL,
    media_type TEXT NOT NULL CHECK(media_type IN ('image', 'video')),
    file_path TEXT NOT NULL,  -- 文件存储路径
    file_size INTEGER,  -- 文件大小（字节）
    file_compressed BLOB,  -- 压缩后的文件（可选）
    thumbnail_path TEXT,  -- 缩略图路径（视频需要）
    display_order INTEGER DEFAULT 0,  -- 显示顺序
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (diary_id) REFERENCES travel_diaries(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_media_diary_id ON diary_media(diary_id);
CREATE INDEX IF NOT EXISTS idx_media_type ON diary_media(media_type);
```

**字段说明：**
- `id`: 媒体文件ID
- `diary_id`: 所属日记ID
- `media_type`: 媒体类型（'image' 或 'video'）
- `file_path`: 文件存储路径
- `file_size`: 文件大小
- `file_compressed`: 压缩后的文件（可选）
- `thumbnail_path`: 缩略图路径（视频需要）
- `display_order`: 显示顺序
- `created_at`: 创建时间

**用途**：存储日记关联的图片和视频文件

### 7. 日记关联景点表 (diary_attractions)

```sql
CREATE TABLE IF NOT EXISTS diary_attractions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    diary_id INTEGER NOT NULL,
    attraction_id INTEGER NOT NULL,
    FOREIGN KEY (diary_id) REFERENCES travel_diaries(id) ON DELETE CASCADE,
    FOREIGN KEY (attraction_id) REFERENCES attractions(id)
);

CREATE INDEX IF NOT EXISTS idx_diary_attr_diary_id ON diary_attractions(diary_id);
CREATE INDEX IF NOT EXISTS idx_diary_attr_attraction_id ON diary_attractions(attraction_id);
```

**字段说明：**
- `id`: 关联ID
- `diary_id`: 日记ID
- `attraction_id`: 景点ID

**用途**：关联日记和景点，用于按景点推荐日记

### 8. 日记评分表 (diary_ratings)

```sql
CREATE TABLE IF NOT EXISTS diary_ratings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    diary_id INTEGER NOT NULL,
    user_id INTEGER,
    rating INTEGER NOT NULL CHECK(rating >= 1 AND rating <= 5),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (diary_id) REFERENCES travel_diaries(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE(diary_id, user_id)  -- 每个用户对每篇日记只能评分一次
);

CREATE INDEX IF NOT EXISTS idx_diary_rating_diary_id ON diary_ratings(diary_id);
CREATE INDEX IF NOT EXISTS idx_diary_rating_user_id ON diary_ratings(user_id);
```

**字段说明：**
- `id`: 评分ID
- `diary_id`: 日记ID
- `user_id`: 用户ID（可为空，支持匿名评分）
- `rating`: 评分（1-5分）
- `created_at`: 创建时间

**用途**：存储用户对日记的评分，计算平均评分

### 9. 日记浏览记录表 (diary_browse_history)

```sql
CREATE TABLE IF NOT EXISTS diary_browse_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    diary_id INTEGER NOT NULL,
    user_id INTEGER,  -- 可为空，支持匿名浏览
    browse_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (diary_id) REFERENCES travel_diaries(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_diary_browse_diary_id ON diary_browse_history(diary_id);
CREATE INDEX IF NOT EXISTS idx_diary_browse_user_id ON diary_browse_history(user_id);
CREATE INDEX IF NOT EXISTS idx_diary_browse_time ON diary_browse_history(browse_time);
```

**字段说明：**
- `id`: 记录ID
- `diary_id`: 日记ID
- `user_id`: 用户ID（可为空，支持匿名浏览）
- `browse_time`: 浏览时间

**用途**：记录日记浏览历史，用于统计浏览量（热度）和个性化推荐

## 数据库操作说明

### 登录功能相关操作

**注意**：数据库表结构已就绪，后端需要直接使用 SQL 操作数据库，无需数据库层提供接口。

**后端需要实现的 SQL 操作**：

1. **根据用户名查找用户**（用于登录验证）：
```sql
SELECT id, username, password, created_at, last_login 
FROM users 
WHERE username = ?;
```

2. **根据ID查找用户**（不包含密码，用于获取用户信息）：
```sql
SELECT id, username, created_at, last_login 
FROM users 
WHERE id = ?;
```

3. **更新最后登录时间**：
```sql
UPDATE users 
SET last_login = datetime('now') 
WHERE id = ?;
```

### 景点相关操作（推荐功能）

#### 4. 获取景点列表

**SQL 示例**：
```sql
-- 按类别筛选
SELECT * FROM attractions WHERE category = ? LIMIT ? OFFSET ?;

-- 获取所有景点
SELECT * FROM attractions LIMIT ? OFFSET ?;
```

#### 5. 搜索景点

**SQL 示例**：
```sql
SELECT * FROM attractions 
WHERE (name LIKE ? OR brief_intro LIKE ? OR historical_background LIKE ?)
AND (? IS NULL OR category = ?);
```

#### 6. 获取景点详情

**SQL 示例**：
```sql
SELECT * FROM attractions WHERE id = ?;
```

### 评价相关操作（推荐功能）

#### 7. 获取景点评价统计

**SQL 示例**：
```sql
SELECT 
    attraction_id,
    ROUND(AVG(rating), 2) as average_rating,
    COUNT(*) as total_ratings,
    COUNT(*) as heat_score
FROM attraction_ratings
WHERE attraction_id = ?
GROUP BY attraction_id;
```

#### 8. 批量获取评价统计

**SQL 示例**：
```sql
SELECT 
    attraction_id,
    ROUND(AVG(rating), 2) as average_rating,
    COUNT(*) as total_ratings,
    COUNT(*) as heat_score
FROM attraction_ratings
WHERE attraction_id IN (?, ?, ...)
GROUP BY attraction_id;
```

### 用户行为操作（个性化推荐）

#### 9. 获取用户浏览历史

**SQL 示例**：
```sql
SELECT attraction_id, browse_time 
FROM user_browse_history 
WHERE user_id = ? 
ORDER BY browse_time DESC 
LIMIT ?;
```

#### 10. 记录用户浏览历史

**SQL 示例**：
```sql
INSERT INTO user_browse_history (user_id, attraction_id, browse_time)
VALUES (?, ?, datetime('now'));
```

### 路线规划相关操作

#### 11. 获取景点历史事件信息

**SQL 示例**：
```sql
SELECT 
    e.id as event_id,
    e.event_name,
    e.start_year,
    e.end_year,
    e.period
FROM attraction_events ae
JOIN historical_events e ON ae.event_id = e.id
WHERE ae.attraction_id = ?;
```

#### 12. 批量获取景点历史事件信息

**SQL 示例**：
```sql
SELECT 
    ae.attraction_id,
    e.id as event_id,
    e.event_name,
    e.start_year,
    e.end_year,
    e.period
FROM attraction_events ae
JOIN historical_events e ON ae.event_id = e.id
WHERE ae.attraction_id IN (?, ?, ...);
```

### 旅游日记相关操作

#### 13. 创建旅游日记

**SQL 示例**：
```sql
INSERT INTO travel_diaries (user_id, title, content, destination, travel_date)
VALUES (?, ?, ?, ?, ?);
```

#### 14. 更新日记内容（支持压缩存储）

**SQL 示例**：
```sql
-- 更新未压缩内容
UPDATE travel_diaries 
SET content = ?, updated_at = datetime('now') 
WHERE id = ?;

-- 更新压缩内容（使用gzip等无损压缩）
UPDATE travel_diaries 
SET content_compressed = ?, updated_at = datetime('now') 
WHERE id = ?;
```

#### 15. 获取日记列表（按热度排序）

**SQL 示例**：
```sql
SELECT 
    d.*,
    u.username as author_name,
    COUNT(DISTINCT db.id) as view_count,
    COALESCE(AVG(dr.rating), 0) as average_rating,
    COUNT(DISTINCT dr.id) as total_ratings
FROM travel_diaries d
LEFT JOIN users u ON d.user_id = u.id
LEFT JOIN diary_browse_history db ON d.id = db.diary_id
LEFT JOIN diary_ratings dr ON d.id = dr.diary_id
GROUP BY d.id
ORDER BY view_count DESC, average_rating DESC
LIMIT ? OFFSET ?;
```

#### 16. 获取日记列表（按评分排序）

**SQL 示例**：
```sql
SELECT 
    d.*,
    u.username as author_name,
    COUNT(DISTINCT db.id) as view_count,
    COALESCE(AVG(dr.rating), 0) as average_rating,
    COUNT(DISTINCT dr.id) as total_ratings
FROM travel_diaries d
LEFT JOIN users u ON d.user_id = u.id
LEFT JOIN diary_browse_history db ON d.id = db.diary_id
LEFT JOIN diary_ratings dr ON d.id = dr.diary_id
GROUP BY d.id
ORDER BY average_rating DESC, view_count DESC
LIMIT ? OFFSET ?;
```

#### 17. 按目的地搜索日记（查找+排序）

**SQL 示例**：
```sql
-- 按目的地搜索并按热度排序
SELECT 
    d.*,
    u.username as author_name,
    COUNT(DISTINCT db.id) as view_count,
    COALESCE(AVG(dr.rating), 0) as average_rating
FROM travel_diaries d
LEFT JOIN users u ON d.user_id = u.id
LEFT JOIN diary_browse_history db ON d.id = db.diary_id
LEFT JOIN diary_ratings dr ON d.id = dr.diary_id
WHERE d.destination LIKE ?
GROUP BY d.id
ORDER BY view_count DESC, average_rating DESC
LIMIT ? OFFSET ?;
```

#### 18. 按名称精确查询（高效查找，考虑大数据量）

**SQL 示例**：
```sql
-- 使用索引优化精确查询
SELECT * FROM travel_diaries 
WHERE title = ? 
LIMIT 1;

-- 或使用LIKE进行模糊查询（较慢，但支持部分匹配）
SELECT * FROM travel_diaries 
WHERE title LIKE ? 
LIMIT ? OFFSET ?;
```

#### 19. 全文检索（使用FTS5）

**SQL 示例**：
```sql
-- 全文检索日记内容
SELECT 
    d.*,
    rank
FROM travel_diaries d
JOIN travel_diaries_fts fts ON d.id = fts.rowid
WHERE travel_diaries_fts MATCH ?
ORDER BY rank
LIMIT ? OFFSET ?;
```

#### 20. 记录日记浏览（增加热度）

**SQL 示例**：
```sql
INSERT INTO diary_browse_history (diary_id, user_id, browse_time)
VALUES (?, ?, datetime('now'));

-- 更新日记浏览量
UPDATE travel_diaries 
SET view_count = view_count + 1 
WHERE id = ?;
```

#### 21. 添加日记评分

**SQL 示例**：
```sql
-- 插入或更新评分（使用INSERT OR REPLACE）
INSERT OR REPLACE INTO diary_ratings (diary_id, user_id, rating)
VALUES (?, ?, ?);

-- 更新日记的平均评分和评分总数
UPDATE travel_diaries 
SET 
    average_rating = (
        SELECT COALESCE(AVG(rating), 0) 
        FROM diary_ratings 
        WHERE diary_id = ?
    ),
    total_ratings = (
        SELECT COUNT(*) 
        FROM diary_ratings 
        WHERE diary_id = ?
    )
WHERE id = ?;
```

#### 22. 获取日记详情（包含媒体文件）

**SQL 示例**：
```sql
-- 获取日记基本信息
SELECT 
    d.*,
    u.username as author_name
FROM travel_diaries d
LEFT JOIN users u ON d.user_id = u.id
WHERE d.id = ?;

-- 获取日记关联的媒体文件
SELECT * FROM diary_media 
WHERE diary_id = ? 
ORDER BY display_order;

-- 获取日记关联的景点
SELECT a.* 
FROM attractions a
JOIN diary_attractions da ON a.id = da.attraction_id
WHERE da.diary_id = ?;
```

#### 23. 个性化推荐（基于用户浏览历史）

**SQL 示例**：
```sql
-- 获取用户浏览过的日记的关联景点
SELECT DISTINCT da.attraction_id
FROM diary_attractions da
JOIN diary_browse_history dbh ON da.diary_id = dbh.diary_id
WHERE dbh.user_id = ?;

-- 推荐包含相似景点的日记
SELECT 
    d.*,
    COUNT(DISTINCT db.id) as view_count,
    COALESCE(AVG(dr.rating), 0) as average_rating
FROM travel_diaries d
JOIN diary_attractions da ON d.id = da.diary_id
LEFT JOIN diary_browse_history db ON d.id = db.diary_id
LEFT JOIN diary_ratings dr ON d.id = dr.diary_id
WHERE da.attraction_id IN (
    SELECT DISTINCT da2.attraction_id
    FROM diary_attractions da2
    JOIN diary_browse_history dbh ON da2.diary_id = dbh.diary_id
    WHERE dbh.user_id = ?
)
AND d.id NOT IN (
    SELECT diary_id FROM diary_browse_history WHERE user_id = ?
)
GROUP BY d.id
ORDER BY average_rating DESC, view_count DESC
LIMIT ?;
```

#### 24. 压缩存储操作

**SQL 示例**：
```sql
-- 存储压缩内容（后端使用gzip等算法压缩）
UPDATE travel_diaries 
SET content_compressed = ? 
WHERE id = ?;

-- 读取压缩内容（后端解压）
SELECT content_compressed FROM travel_diaries WHERE id = ?;
```

**注意**：
- 压缩算法：建议使用gzip（Java: `java.util.zip.GZIPOutputStream`）
- 压缩比：通常文本内容可压缩到原大小的30-50%
- 解压：读取时如果存在压缩内容，优先使用压缩内容，否则使用未压缩内容

## 数据库文件位置

- **用户数据库**：`database/users.db` 或合并到 `database/red_tourism.db`
- **景点数据库**：`database/red_tourism.db`

## 注意事项

1. **密码加密**：用户密码已使用 bcrypt 加密存储（bcryptjs，10轮加密）
2. **唯一性约束**：username 字段有唯一性约束
3. **外键约束**：`user_browse_history` 和 `attraction_ratings` 表有外键约束，需要确保 `attractions` 表存在
4. **数据库连接**：后端需要配置 SQLite JDBC 连接
5. **字段命名**：字段名必须与后端 API 响应格式一致

## 测试数据

✅ **用户数据已就绪**：
- 10 个测试用户：`user1` 到 `user10`
- 密码统一为：`password123`（已 bcrypt 加密）
- 使用脚本创建：`node scripts/create_test_users.js --db red_tourism.db --count 10`

## 验证

1. ✅ 数据库表结构已创建
2. ✅ 测试用户已创建
3. ⏳ 等待后端实现登录接口进行验证