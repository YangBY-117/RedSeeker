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