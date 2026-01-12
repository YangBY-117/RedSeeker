# Red Tourism Database Schema Documentation

## 概述

本文档详细介绍了 `red_tourism.db` 的数据库结构。该数据库采用 SQLite 存储，包含了红色旅游景点信息、用户信息、历史事件、旅游日记及相关互动数据（评分、浏览历史等）。

**数据库文件路径**: `database/red_tourism.db` (请根据实际部署调整)

## 表结构概览

数据库包含以下主要数据表：

1.  **attractions**: 景点基础信息表
2.  **historical_events**: 历史事件表
3.  **attraction_events**: 景点与历史事件关联表
4.  **users**: 用户表
5.  **user_browse_history**: 用户浏览景点历史表
6.  **attraction_ratings**: 景点评分表
7.  **travel_diaries**: 旅游日记表
8.  **diary_media**: 日记多媒体资源表
9.  **diary_attractions**: 日记与景点关联表
10. **diary_ratings**: 日记评分表
11. **diary_browse_history**: 日记浏览历史表
12. **travel_diaries_fts**: 日记全文检索引擎表

---

## 详细表结构

### 1. 景点表 (attractions)

存储所有红色旅游景点的基本信息。

| 字段名 | 类型 | 描述 | 备注 |
|---|---|---|---|
| id | INTEGER | 主键ID | 自增 |
| old_id | INTEGER | 旧系统ID | 迁移用途 |
| name | TEXT | 景点名称 | |
| address | TEXT | 地址 | |
| longitude | REAL | 经度 | |
| latitude | REAL | 纬度 | |
| category | INTEGER | 类别ID | |
| brief_intro | TEXT | 简介 | |
| historical_background | TEXT | 历史背景 | |
| per_capita_consumption | REAL | 人均消费 | |
| business_hours | TEXT | 营业时间 | |
| created_at | TEXT | 创建时间 | |
| updated_at | TEXT | 更新时间 | |
| images | TEXT | 图片路径列表 | JSON 格式存储 |
| popularity | INTEGER | 热度 | |
| score | REAL | 评分 | |

### 2. 历史事件表 (historical_events)

存储与红色旅游相关的历史事件。

| 字段名 | 类型 | 描述 | 备注 |
|---|---|---|---|
| id | INTEGER | 主键ID | |
| event_name | TEXT | 事件名称 | |
| start_year | INTEGER | 开始年份 | |
| end_year | INTEGER | 结束年份 | |
| description | TEXT | 事件描述 | |
| period | TEXT | 历史时期 | 如：土地革命时期 |

### 3. 景点-事件关联表 (attraction_events)

建立景点与历史事件的多对多关系。

| 字段名 | 类型 | 描述 | 备注 |
|---|---|---|---|
| id | INTEGER | 主键ID | 自增 |
| attraction_id | INTEGER | 景点ID | 关联 attractions.id |
| event_id | INTEGER | 事件ID | 关联 historical_events.id |

### 4. 用户表 (users)

存储注册用户信息。

| 字段名 | 类型 | 描述 | 备注 |
|---|---|---|---|
| id | INTEGER | 用户ID | 自增主键 |
| username | TEXT | 用户名 | 唯一 |
| password | TEXT | 密码 | bcrypt 加密 |
| created_at | DATETIME | 注册时间 | 默认为当前时间 |
| last_login | DATETIME | 最后登录时间 | |

**预置测试用户**:
*   用户名: `user1` ~ `user10`
*   密码: `password123` (已加密)

### 5. 用户浏览历史表 (user_browse_history)

记录用户浏览景点的历史，用于推荐算法。

| 字段名 | 类型 | 描述 | 备注 |
|---|---|---|---|
| id | INTEGER | 记录ID | 自增 |
| user_id | INTEGER | 用户ID | 外键 |
| attraction_id | INTEGER | 景点ID | 外键 |
| browse_time | DATETIME | 浏览时间 | 默认为当前时间 |

### 6. 景点评价表 (attraction_ratings)

存储用户对景点的评分和评论。

| 字段名 | 类型 | 描述 | 备注 |
|---|---|---|---|
| id | INTEGER | 评价ID | 自增 |
| attraction_id | INTEGER | 景点ID | 外键 |
| user_id | INTEGER | 用户ID | 外键 |
| rating | INTEGER | 评分 | 1-5分 |
| comment | TEXT | 评论内容 | |
| created_at | DATETIME | 创建时间 | |

### 7. 旅游日记表 (travel_diaries)

存储用户发布的游记。支持全文检索。

| 字段名 | 类型 | 描述 | 备注 |
|---|---|---|---|
| id | INTEGER | 日记ID | 自增 |
| user_id | INTEGER | 作者ID | 外键 |
| title | TEXT | 标题 | |
| content | TEXT | 正文 | FTS5 索引 |
| content_compressed | BLOB | 压缩正文 | 可选，gzip压缩 |
| destination | TEXT | 目的地 | |
| travel_date | DATE | 旅游日期 | |
| view_count | INTEGER | 浏览量 | 默认为0 |
| average_rating | REAL | 平均评分 | |
| total_ratings | INTEGER | 总评分数 | |
| created_at | DATETIME | 发布时间 | |
| updated_at | DATETIME | 更新时间 | |

**全文检索 (FTS5)**:
表 `travel_diaries_fts` 提供了对 `title`, `content`, `destination` 的全文检索功能。
数据库包含触发器，当 `travel_diaries` 更新时自动同步 FTS 索引。

### 8. 日记媒体文件表 (diary_media)

存储日记关联的图片或视频文件信息。

| 字段名 | 类型 | 描述 | 备注 |
|---|---|---|---|
| id | INTEGER | 媒体ID | 自增 |
| diary_id | INTEGER | 日记ID | 外键 |
| media_type | TEXT | 类型 | 'image' 或 'video' |
| file_path | TEXT | 文件路径 | |
| file_size | INTEGER | 文件大小 | 字节 |
| file_compressed | BLOB | 压缩文件 | 可选 |
| thumbnail_path | TEXT | 缩略图路径 | 视频专用 |
| display_order | INTEGER | 显示顺序 | |
| created_at | DATETIME | 上传时间 | |

### 9. 日记关联景点表 (diary_attractions)

关联日记中提及的景点。

| 字段名 | 类型 | 描述 | 备注 |
|---|---|---|---|
| id | INTEGER | 记录ID | 自增 |
| diary_id | INTEGER | 日记ID | 外键 |
| attraction_id | INTEGER | 景点ID | 外键 |

### 10. 日记评分表 (diary_ratings)

用户对日记的评分。

| 字段名 | 类型 | 描述 | 备注 |
|---|---|---|---|
| id | INTEGER | 评分ID | 自增 |
| diary_id | INTEGER | 日记ID | 外键 |
| user_id | INTEGER | 用户ID | 外键 |
| rating | INTEGER | 评分 | 1-5分 |
| created_at | DATETIME | 评分时间 | |

### 11. 日记浏览记录表 (diary_browse_history)

记录日记的被浏览情况。

| 字段名 | 类型 | 描述 | 备注 |
|---|---|---|---|
| id | INTEGER | 记录ID | 自增 |
| diary_id | INTEGER | 日记ID | 外键 |
| user_id | INTEGER | 用户ID | 可为空(匿名) |
| browse_time | DATETIME | 浏览时间 | |

## 索引优化

为了提高查询效率，已在以下字段建立索引：

*   `users`: username
*   `user_browse_history`: user_id, attraction_id, browse_time
*   `attraction_ratings`: attraction_id, user_id
*   `travel_diaries`: user_id, destination, created_at, view_count, average_rating
*   `diary_media`: diary_id, media_type
*   `diary_attractions`: diary_id, attraction_id
*   `diary_ratings`: diary_id, user_id
*   `diary_browse_history`: diary_id, user_id, browse_time

## 注意事项

1.  **FTS5 全文检索**: 日记搜索功能依赖 SQLite 的 FTS5 扩展。
2.  **表名变更**: 原 `red_tourism` 已更名为 `attractions`，原 `red_tourism_events` 已更名为 `attraction_events`。
3.  **安全性**: 用户密码均经过 bcrypt 哈希处理。
