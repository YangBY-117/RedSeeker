# 后端开发指南

## 概述

实现 RedSeeker 系统的后端 API 接口，使用 Spring Boot。后端需要提供用户登录和旅游推荐功能。

## 技术栈

- **Spring Boot**
- **JWT (JSON Web Token)** 用于身份认证
- **bcrypt** 用于密码加密
- **CORS** 配置支持跨域请求
- **堆排序算法** 用于推荐排序（Top10）
- **高德地图API** 用于路径规划和地图服务
- **硅基流动API (LangChain)** 用于AI辅助路线规划（可选）

## 必须实现的接口

### 用户认证接口

#### 1. 用户登录

**接口地址**: `POST /api/auth/login`

**请求体**:
```json
{
  "username": "string",
  "password": "string"
}
```

**成功响应 (200)**:
```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "token": "jwt-token-string",
    "user": {
      "id": 1,
      "username": "testuser",
      "created_at": "2024-01-01T00:00:00.000Z",
      "last_login": "2024-01-01T12:00:00.000Z"
    }
  }
}
```

**错误响应 (401)**:
```json
{
  "success": false,
  "message": "用户名或密码错误"
}
```

#### 2. 获取当前用户信息

**接口地址**: `GET /api/auth/me`

**请求头**:
```
Authorization: Bearer {token}
```

**成功响应 (200)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "testuser",
    "created_at": "2024-01-01T00:00:00.000Z",
    "last_login": "2024-01-01T12:00:00.000Z"
  }
}
```

**错误响应 (401)**:
```json
{
  "success": false,
  "message": "未授权，请先登录"
}
```

### 推荐接口

#### 3. 获取推荐景点

**接口地址**: `GET /api/recommend/attractions`

**请求参数**:
- `category` (integer, 可选): 类别ID（1-9），不传则推荐所有类别
- `userId` (integer, 可选): 用户ID，用于个性化推荐（需要登录）

**成功响应 (200)**:
```json
{
  "success": true,
  "data": {
    "attractions": [
      {
        "id": 1,
        "name": "景点名称",
        "address": "地址",
        "longitude": 121.4737,
        "latitude": 31.2208,
        "category": 1,
        "categoryName": "纪念馆",
        "brief_intro": "简介",
        "historical_background": "历史背景",
        "per_capita_consumption": 0,
        "business_hours": "09:00-17:00",
        "average_rating": 4.5,
        "total_ratings": 120,
        "heat_score": 120,
        "recommend_score": 85.5
      }
    ],
    "total": 10
  }
}
```

**推荐算法要求**：
- 使用**堆排序**获取 Top10 景点
- 推荐分数计算公式：`recommend_score = 历史脉络分数(30%) + 教育价值分数(20%) + 热度分数(30%) + 评价分数(20%)`
- 历史脉络分数：根据景点关联的历史事件时间顺序（越早越重要，可简单按ID或时间排序）
- 教育价值分数：根据历史背景长度和完整性（可简化为固定值或根据简介长度）
- 热度分数：根据 `heat_score`（评价总数）归一化
- 评价分数：根据 `average_rating` 归一化（1-5分转换为0-100分）
- 如果提供 `userId`，根据用户浏览历史调整分数（浏览过的景点降低分数，避免重复推荐）

#### 4. 搜索景点

**接口地址**: `GET /api/recommend/search`

**请求参数**:
- `keyword` (string, 必填): 搜索关键词
- `category` (integer, 可选): 类别筛选
- `sortBy` (string, 可选): 排序方式，可选值：`heat`（热度）、`rating`（评价），默认 `heat`
- `limit` (integer, 可选): 返回数量，默认10

**成功响应 (200)**:
```json
{
  "success": true,
  "data": {
    "attractions": [ /* 同推荐接口格式 */ ],
    "total": 10
  }
}
```

**功能要求**：
- 在景点名称、简介、历史背景中搜索关键词
- 支持按热度和评价排序
- 使用堆排序获取 Top N 结果

#### 5. 记录用户浏览

**接口地址**: `POST /api/recommend/browse`

**请求头**:
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "attraction_id": 1
}
```

**成功响应 (200)**:
```json
{
  "success": true,
  "message": "记录成功"
}
```

**功能**：记录用户浏览历史，用于个性化推荐

### 路线规划接口

#### 6. 规划单景点路线

**接口地址**: `POST /api/route/single`

**请求体**:
```json
{
  "attraction_id": 1,
  "start_location": {
    "longitude": 121.4737,
    "latitude": 31.2208,
    "address": "上海市黄浦区"
  },
  "transport_mode": "driving"
}
```

**请求参数说明**：
- `attraction_id` (integer, 必填): 目标景点ID
- `start_location` (object, 必填): 起始位置
  - `longitude` (number): 经度
  - `latitude` (number): 纬度
  - `address` (string, 可选): 地址描述
- `transport_mode` (string, 可选): 交通方式，可选值：`driving`（驾车）、`walking`（步行）、`transit`（公交），默认 `driving`

**成功响应 (200)**:
```json
{
  "success": true,
  "data": {
    "route": {
      "distance": 12500,
      "duration": 1800,
      "steps": [
        {
          "instruction": "从起点出发，沿XX路行驶",
          "road": "XX路",
          "distance": 500,
          "duration": 60,
          "polyline": "path坐标点串"
        }
      ],
      "polyline": "完整路径坐标点串"
    },
    "attraction": {
      "id": 1,
      "name": "景点名称",
      "address": "地址",
      "longitude": 121.4737,
      "latitude": 31.2208
    },
    "start_location": {
      "longitude": 121.4737,
      "latitude": 31.2208,
      "address": "上海市黄浦区"
    }
  }
}
```

**功能要求**：
- 调用高德地图API的路径规划接口
- 返回详细的路线信息，包括距离、时间、路径点坐标

#### 7. 规划多景点路线

**接口地址**: `POST /api/route/multiple`

**请求体**:
```json
{
  "attraction_ids": [1, 2, 3],
  "start_location": {
    "longitude": 121.4737,
    "latitude": 31.2208,
    "address": "上海市黄浦区"
  },
  "end_location": {
    "longitude": 120.7575,
    "latitude": 30.7536,
    "address": "浙江省嘉兴市"
  },
  "transport_mode": "driving",
  "strategy": "history_first"
}
```

**请求参数说明**：
- `attraction_ids` (array, 必填): 目标景点ID数组
- `start_location` (object, 必填): 起始位置
- `end_location` (object, 可选): 结束位置，不传则返回起始位置
- `transport_mode` (string, 可选): 交通方式，默认 `driving`
- `strategy` (string, 可选): 排序策略，可选值：
  - `history_first`: 先按历史阶段排序，同阶段按最短路径（推荐，符合红色旅游特色）
  - `shortest`: 纯最短路径
  - 默认 `history_first`

**成功响应 (200)**:
```json
{
  "success": true,
  "data": {
    "route_plan": {
      "total_distance": 125000,
      "total_duration": 18000,
      "stages": [
        {
          "stage_name": "建党初期",
          "stage_period": "1921-1927",
          "attractions": [
            {
              "id": 1,
              "name": "景点名称",
              "order": 1,
              "distance_from_previous": 0,
              "duration_from_previous": 0
            }
          ]
        }
      ],
      "segments": [
        {
          "from_attraction_id": null,
          "to_attraction_id": 1,
          "distance": 5000,
          "duration": 600,
          "polyline": "路径坐标点串",
          "steps": [ /* 同单景点路线格式 */ ]
        }
      ],
      "full_polyline": "完整路径坐标点串"
    },
    "attractions": [ /* 景点详细信息数组 */ ]
  }
}
```

**路线规划算法要求**：
1. **历史阶段排序**（strategy=history_first）：
   - 查询所有景点的历史事件信息
   - 按历史事件的 `start_year` 排序，将景点分组到不同历史阶段
   - 同一历史阶段内的景点，使用高德地图API的多点路径规划，计算最短路径顺序
   - 阶段之间按时间顺序连接

2. **最短路径排序**（strategy=shortest）：
   - 直接使用高德地图API的多点路径规划
   - 计算所有景点的最短访问路径

3. **路径计算**：
   - 起点 → 第一阶段景点（按最短路径） → 第二阶段景点（按最短路径） → ... → 终点
   - 每个阶段内部使用多点路径规划优化顺序
   - 阶段之间按历史时间顺序连接

#### 8. 获取当前位置

**接口地址**: `GET /api/route/current-location`

**功能**：获取用户当前位置（用于路线规划的起点）

**成功响应 (200)**:
```json
{
  "success": true,
  "data": {
    "longitude": 121.4737,
    "latitude": 31.2208,
    "address": "上海市黄浦区XX路XX号"
  }
}
```

**说明**：可以调用高德地图API的定位服务，或由前端通过浏览器定位API获取后传给后端

### 场所查询接口

#### 9. 计算实际路径距离并排序

**接口地址**: `POST /api/place/distance-sort`

**请求体**:
```json
{
  "origin": {
    "longitude": 121.4737,
    "latitude": 31.2208
  },
  "places": [
    {
      "id": "poi_id_1",
      "name": "场所名称",
      "location": {
        "longitude": 121.4800,
        "latitude": 31.2300
      },
      "distance": 500
    }
  ],
  "transport_mode": "walking"
}
```

**请求参数说明**：
- `origin` (object, 必填): 起点位置
  - `longitude` (number): 经度
  - `latitude` (number): 纬度
- `places` (array, 必填): 场所列表（前端已通过高德地图周边搜索API获取）
  - 每个场所包含 `id`, `name`, `location`（包含 `longitude` 和 `latitude`），以及初始的 `distance`（直线距离）
- `transport_mode` (string, 可选): 交通方式，可选值：`driving`（驾车）、`walking`（步行）、`transit`（公交），默认 `walking`

**成功响应 (200)**:
```json
{
  "success": true,
  "data": [
    {
      "id": "poi_id_1",
      "name": "场所名称",
      "address": "地址",
      "location": {
        "longitude": 121.4800,
        "latitude": 31.2300
      },
      "distance": 500,
      "realDistance": 650,
      "realDuration": 480,
      "type": "类型",
      "tel": "联系电话"
    }
  ]
}
```

**响应数据说明**：
- `realDistance`: 实际路径距离（米），通过高德地图路径规划API计算
- `realDuration`: 实际路径时间（秒）
- 返回的数组已按 `realDistance` 从小到大排序

**功能要求**：
- 对每个场所，调用高德地图路径规划API（`/v3/direction/walking` 或 `/v3/direction/driving`）计算从起点到该场所的实际路径距离
- 根据实际距离对场所列表进行排序
- 如果路径规划失败，保留原始直线距离，不进行排序调整

**API调用示例**：
```java
// 对每个场所计算实际距离
for (Place place : places) {
    String url = "https://restapi.amap.com/v3/direction/walking";
    Map<String, String> params = new HashMap<>();
    params.put("key", "bfa236c5b4ff2d954936faa864c1a490");
    params.put("origin", originLongitude + "," + originLatitude);
    params.put("destination", place.getLocation().getLongitude() + "," + place.getLocation().getLatitude());
    
    // 调用API获取路径信息
    // 解析返回的distance和duration
    place.setRealDistance(parsedDistance);
    place.setRealDuration(parsedDuration);
}

// 按实际距离排序
places.sort(Comparator.comparing(Place::getRealDistance));
```

**注意**：
- 前端会先调用高德地图周边搜索API获取场所列表（前端直接调用，无需后端）
- 后端只需要实现实际距离计算和排序功能
- 如果场所数量较多，可以考虑批量调用或异步处理以提高性能

### 旅游日记接口

#### 10. 获取日记列表（推荐）

**接口地址**: `GET /api/diary/list`

**请求参数**:
- `sortBy` (string, 可选): 排序方式，可选值：`heat`（热度）、`rating`（评分）、`time`（时间），默认 `heat`
- `destination` (string, 可选): 目的地筛选
- `userId` (integer, 可选): 用户ID，用于个性化推荐（需要登录）
- `page` (integer, 可选): 页码，默认1
- `pageSize` (integer, 可选): 每页数量，默认10

**成功响应 (200)**:
```json
{
  "success": true,
  "data": {
    "diaries": [
      {
        "id": 1,
        "title": "日记标题",
        "content": "日记内容（摘要）",
        "destination": "上海市",
        "travel_date": "2024-01-01",
        "view_count": 120,
        "average_rating": 4.5,
        "total_ratings": 20,
        "author": {
          "id": 1,
          "username": "user1"
        },
        "cover_image": "https://example.com/image.jpg",
        "created_at": "2024-01-01T00:00:00.000Z"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 10,
    "totalPages": 10
  }
}
```

**推荐算法要求**：
- 使用**排序算法**（快速排序或归并排序）对日记进行排序
- 推荐分数计算公式：`recommend_score = 热度分数(40%) + 评分分数(40%) + 个人兴趣分数(20%)`
- 热度分数：根据 `view_count` 归一化
- 评分分数：根据 `average_rating` 归一化（1-5分转换为0-100分）
- 个人兴趣分数：如果提供 `userId`，根据用户浏览历史计算（浏览过相似景点的日记提高分数）

#### 11. 按目的地搜索日记

**接口地址**: `GET /api/diary/search-by-destination`

**请求参数**:
- `destination` (string, 必填): 目的地关键词
- `sortBy` (string, 可选): 排序方式，可选值：`heat`（热度）、`rating`（评分），默认 `heat`
- `page` (integer, 可选): 页码，默认1
- `pageSize` (integer, 可选): 每页数量，默认10

**成功响应 (200)**:
```json
{
  "success": true,
  "data": {
    "diaries": [ /* 同列表接口格式 */ ],
    "total": 50,
    "page": 1,
    "pageSize": 10
  }
}
```

**功能要求**：
- 使用**查找算法**（字符串匹配）查找目的地相关的日记
- 使用**排序算法**（快速排序）按热度和评分排序
- 支持模糊匹配（LIKE查询）

#### 12. 按名称精确查询

**接口地址**: `GET /api/diary/search-by-name`

**请求参数**:
- `title` (string, 必填): 日记标题（精确匹配）

**成功响应 (200)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "日记标题",
    "content": "完整内容",
    "destination": "上海市",
    "travel_date": "2024-01-01",
    "view_count": 120,
    "average_rating": 4.5,
    "total_ratings": 20,
    "author": { /* 作者信息 */ },
    "media": [ /* 媒体文件列表 */ ],
    "attractions": [ /* 关联景点列表 */ ],
    "created_at": "2024-01-01T00:00:00.000Z"
  }
}
```

**功能要求**：
- 使用**查找算法**（哈希表或B树索引）进行高效查找
- 考虑大数据量情况，使用数据库索引优化查询性能
- 时间复杂度：O(log n) 或 O(1)

#### 13. 全文检索

**接口地址**: `GET /api/diary/fulltext-search`

**请求参数**:
- `keyword` (string, 必填): 搜索关键词
- `page` (integer, 可选): 页码，默认1
- `pageSize` (integer, 可选): 每页数量，默认10

**成功响应 (200)**:
```json
{
  "success": true,
  "data": {
    "diaries": [ /* 日记列表 */ ],
    "total": 30,
    "page": 1,
    "pageSize": 10
  }
}
```

**功能要求**：
- 使用**文本搜索算法**（SQLite FTS5全文检索）
- 在日记标题、内容、目的地中搜索关键词
- 按相关性排序

#### 14. 获取日记详情

**接口地址**: `GET /api/diary/{id}`

**请求头**（可选）:
```
Authorization: Bearer {token}  // 如果登录，记录浏览历史
```

**成功响应 (200)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "日记标题",
    "content": "完整内容（如果存储了压缩内容，需要解压）",
    "destination": "上海市",
    "travel_date": "2024-01-01",
    "view_count": 121,
    "average_rating": 4.5,
    "total_ratings": 20,
    "author": {
      "id": 1,
      "username": "user1"
    },
    "media": [
      {
        "id": 1,
        "media_type": "image",
        "file_path": "https://example.com/image.jpg",
        "thumbnail_path": null,
        "display_order": 0
      },
      {
        "id": 2,
        "media_type": "video",
        "file_path": "https://example.com/video.mp4",
        "thumbnail_path": "https://example.com/thumbnail.jpg",
        "display_order": 1
      }
    ],
    "attractions": [
      {
        "id": 1,
        "name": "景点名称",
        "address": "地址"
      }
    ],
    "user_rating": 4,  // 当前用户的评分（如果已登录且已评分）
    "created_at": "2024-01-01T00:00:00.000Z",
    "updated_at": "2024-01-01T12:00:00.000Z"
  }
}
```

**功能要求**：
- 如果用户已登录，记录浏览历史（增加热度）
- 如果内容存储为压缩格式，需要解压后返回
- 返回关联的媒体文件和景点信息

#### 15. 创建日记

**接口地址**: `POST /api/diary/create`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**请求体**:
```
title: 日记标题
content: 日记内容
destination: 目的地
travel_date: 2024-01-01
attraction_ids: [1, 2, 3]  // 关联的景点ID数组
images: [File, File, ...]  // 图片文件（可选）
videos: [File, File, ...]  // 视频文件（可选）
```

**成功响应 (200)**:
```json
{
  "success": true,
  "message": "日记创建成功",
  "data": {
    "id": 1,
    "title": "日记标题",
    "created_at": "2024-01-01T00:00:00.000Z"
  }
}
```

**功能要求**：
- 支持上传图片和视频文件
- 对内容进行**无损压缩**（gzip）存储（可选，用于大数据量优化）
- 生成视频缩略图
- 关联景点信息

#### 16. 更新日记

**接口地址**: `PUT /api/diary/{id}`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**请求体**: 同创建接口

**功能要求**：
- 只能更新自己创建的日记
- 支持更新内容、媒体文件等

#### 17. 删除日记

**接口地址**: `DELETE /api/diary/{id}`

**请求头**:
```
Authorization: Bearer {token}
```

**成功响应 (200)**:
```json
{
  "success": true,
  "message": "日记删除成功"
}
```

**功能要求**：
- 只能删除自己创建的日记
- 级联删除关联的媒体文件和评分记录

#### 18. 评分日记

**接口地址**: `POST /api/diary/{id}/rate`

**请求头**:
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "rating": 5
}
```

**成功响应 (200)**:
```json
{
  "success": true,
  "message": "评分成功",
  "data": {
    "average_rating": 4.6,
    "total_ratings": 21
  }
}
```

**功能要求**：
- 每个用户对每篇日记只能评分一次（更新已有评分）
- 更新日记的平均评分和评分总数

#### 19. 生成AIGC动画

**接口地址**: `POST /api/diary/{id}/generate-animation`

**请求头**:
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "images": ["https://example.com/image1.jpg", "https://example.com/image2.jpg"],
  "description": "文字描述（可选）"
}
```

**成功响应 (200)**:
```json
{
  "success": true,
  "message": "动画生成中",
  "data": {
    "task_id": "task_123456",
    "status": "processing",
    "estimated_time": 30  // 预计完成时间（秒）
  }
}
```

**功能要求**：
- 调用硅基流动API（LangChain）生成旅游动画
- 支持异步处理，返回任务ID
- 根据图片和文字描述生成动画视频

**API密钥**：`sk-isqxgxwcscmjjyaqyamsqgvqkxiqgtlgrorrnagmiqnuyunq`

**硅基流动API调用示例**：
```java
// 使用LangChain调用硅基流动API
// 构建提示词：根据图片和文字描述生成旅游动画
String prompt = "根据以下图片和描述生成一段旅游动画视频：" + description;
// 调用API并返回生成的视频文件URL
```

#### 20. 查询动画生成状态

**接口地址**: `GET /api/diary/animation-status/{task_id}`

**成功响应 (200)**:
```json
{
  "success": true,
  "data": {
    "status": "completed",  // processing | completed | failed
    "video_url": "https://example.com/animation.mp4",  // 完成时返回
    "error_message": null  // 失败时返回错误信息
  }
}
```

## 实现要点

### 1. 数据库操作

**数据库表结构已就绪**，后端需要直接使用 SQL 操作数据库。

**数据库配置**：
- 数据库文件：`database/red_tourism.db` 或 `database/users.db`
- 需要添加 SQLite JDBC 依赖

**用户相关 SQL 操作**：

```java
// 1. 根据用户名查找用户（用于登录验证）
String sql = "SELECT id, username, password, created_at, last_login FROM users WHERE username = ?";
// 使用 JdbcTemplate 或 PreparedStatement 执行

// 2. 根据ID查找用户（不包含密码）
String sql = "SELECT id, username, created_at, last_login FROM users WHERE id = ?";

// 3. 更新最后登录时间
String sql = "UPDATE users SET last_login = datetime('now') WHERE id = ?";
```

**景点相关 SQL 操作**：
```java
// 获取景点列表
String sql = "SELECT * FROM attractions WHERE (? IS NULL OR category = ?) LIMIT ? OFFSET ?";

// 搜索景点
String sql = "SELECT * FROM attractions WHERE (name LIKE ? OR brief_intro LIKE ? OR historical_background LIKE ?) AND (? IS NULL OR category = ?)";

// 获取景点详情
String sql = "SELECT * FROM attractions WHERE id = ?";
```

**评价相关 SQL 操作**：
```java
// 获取景点评价统计
String sql = "SELECT attraction_id, ROUND(AVG(rating), 2) as average_rating, COUNT(*) as total_ratings, COUNT(*) as heat_score FROM attraction_ratings WHERE attraction_id = ? GROUP BY attraction_id";

// 批量获取评价统计
String sql = "SELECT attraction_id, ROUND(AVG(rating), 2) as average_rating, COUNT(*) as total_ratings, COUNT(*) as heat_score FROM attraction_ratings WHERE attraction_id IN (?) GROUP BY attraction_id";
```

**用户行为相关 SQL 操作**：
```java
// 获取用户浏览历史
String sql = "SELECT attraction_id, browse_time FROM user_browse_history WHERE user_id = ? ORDER BY browse_time DESC LIMIT ?";

// 记录用户浏览历史
String sql = "INSERT INTO user_browse_history (user_id, attraction_id, browse_time) VALUES (?, ?, datetime('now'))";
```

**路线规划相关 SQL 操作**：
```java
// 获取景点历史事件信息
String sql = "SELECT e.id as event_id, e.event_name, e.start_year, e.end_year, e.period FROM attraction_events ae JOIN historical_events e ON ae.event_id = e.id WHERE ae.attraction_id = ?";

// 批量获取景点历史事件信息
String sql = "SELECT ae.attraction_id, e.id as event_id, e.event_name, e.start_year, e.end_year, e.period FROM attraction_events ae JOIN historical_events e ON ae.event_id = e.id WHERE ae.attraction_id IN (?)";
```

### 2. 推荐算法实现

**核心算法：堆排序（Top10）**

```java
// 伪代码示例
PriorityQueue<AttractionScore> heap = new PriorityQueue<>(10, Comparator.comparing(AttractionScore::getScore));

for (Attraction attraction : allAttractions) {
    double score = calculateRecommendScore(attraction, userId);
    if (heap.size() < 10) {
        heap.offer(new AttractionScore(attraction, score));
    } else if (score > heap.peek().getScore()) {
        heap.poll();
        heap.offer(new AttractionScore(attraction, score));
    }
}

// 转换为列表并排序
List<Attraction> top10 = heap.stream()
    .sorted(Comparator.comparing(AttractionScore::getScore).reversed())
    .map(AttractionScore::getAttraction)
    .collect(Collectors.toList());
```

**推荐分数计算**：
```java
double calculateRecommendScore(Attraction attraction, Integer userId) {
    // 历史脉络分数 (30%)
    double historyScore = calculateHistoryScore(attraction) * 0.3;
    
    // 教育价值分数 (20%)
    double educationScore = calculateEducationScore(attraction) * 0.2;
    
    // 热度分数 (30%) - 根据评价总数归一化
    double heatScore = normalizeHeatScore(attraction.getHeatScore()) * 0.3;
    
    // 评价分数 (20%) - 根据平均评分归一化
    double ratingScore = normalizeRatingScore(attraction.getAverageRating()) * 0.2;
    
    double totalScore = historyScore + educationScore + heatScore + ratingScore;
    
    // 如果提供userId，根据浏览历史调整
    if (userId != null) {
        if (hasUserBrowsed(userId, attraction.getId())) {
            totalScore *= 0.5; // 浏览过的景点降低分数
        }
    }
    
    return totalScore;
}
```

### 3. 密码验证

使用 bcrypt 验证密码：

```java
boolean isValid = BCrypt.checkpw(password, user.getPassword());
```

### 4. JWT Token 生成

登录成功后生成 JWT Token，包含用户ID和用户名，过期时间设置为 7 天。

### 5. 认证中间件

实现 JWT 认证中间件，用于保护需要登录的接口：
- 从请求头 `Authorization: Bearer {token}` 中提取 token
- 验证 token 有效性
- 将用户信息添加到请求上下文

### 6. 高德地图API集成

**API密钥配置**：
- Key: `bfa236c5b4ff2d954936faa864c1a490`
- 安全密钥: `332666a7065f99857bf73e6e0fbf2351`

**需要调用的API**：
1. **路径规划API** (`/v3/direction/driving` 或 `/v3/direction/walking` 或 `/v3/direction/transit`)
   - 单点路径规划：起点 → 终点
   - 多点路径规划：起点 → 途经点1 → 途经点2 → ... → 终点

2. **地理编码API** (`/v3/geocode/geo`)
   - 地址转坐标

3. **逆地理编码API** (`/v3/geocode/regeo`)
   - 坐标转地址

**API调用示例**：
```java
// 单点路径规划
String url = "https://restapi.amap.com/v3/direction/driving";
Map<String, String> params = new HashMap<>();
params.put("key", "bfa236c5b4ff2d954936faa864c1a490");
params.put("origin", startLongitude + "," + startLatitude);
params.put("destination", endLongitude + "," + endLatitude);
// 调用API并解析返回的路径信息
```

### 7. 多景点路线规划算法

**历史阶段优先策略（history_first）**：

```java
// 伪代码
1. 获取所有景点的历史事件信息
2. 按历史事件的start_year排序，将景点分组到不同阶段
3. 对每个阶段内的景点：
   a. 调用高德地图多点路径规划API
   b. 计算该阶段内的最短访问路径
4. 按阶段时间顺序连接：
   起点 → 第一阶段（优化后顺序） → 第二阶段（优化后顺序） → ... → 终点
```

**最短路径策略（shortest）**：
```java
// 直接调用高德地图多点路径规划API
// 起点 + 所有景点作为途经点 + 终点
```

### 8. 硅基流动API集成（可选）

**API密钥**：`sk-isqxgxwcscmjjyaqyamsqgvqkxiqgtlgrorrnagmiqnuyunq`

**用途**：使用LangChain进行AI辅助路线规划，可以提供：
- 路线说明和建议
- 景点介绍
- 游览时间建议

**说明**：这是可选功能，如果时间有限可以先不实现

## 响应格式

所有接口统一使用以下响应格式：

**成功响应**:
```json
{
  "success": true,
  "message": "操作成功",
  "data": { /* 数据对象 */ }
}
```

**错误响应**:
```json
{
  "success": false,
  "message": "错误信息"
}
```

## 注意事项

1. **密码安全**：必须使用 bcrypt 验证密码，不要存储明文
2. **JWT 密钥**：生产环境必须使用强随机密钥
3. **CORS 配置**：确保配置 CORS 允许前端域名访问（通常是 `http://localhost:5173`）
4. **错误处理**：不要向客户端暴露敏感信息
5. **输入验证**：所有用户输入必须验证
6. **算法效率**：推荐算法使用堆排序，时间复杂度 O(n log k)，其中 n 是景点总数，k=10
7. **数据动态性**：推荐分数需要实时计算，考虑数据动态变化

## 测试

使用 Postman 或 curl 测试接口：

```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"password123"}'

# 获取推荐（替换 {token} 为登录返回的 token）
curl -X GET "http://localhost:8080/api/recommend/attractions?category=1" \
  -H "Authorization: Bearer {token}"

# 搜索景点
curl -X GET "http://localhost:8080/api/recommend/search?keyword=纪念馆&sortBy=heat"

# 记录浏览
curl -X POST http://localhost:8080/api/recommend/browse \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"attraction_id":1}'
```

## 数据库状态

✅ **数据库表结构已就绪**：
- `users` 表已创建，包含登录所需字段
- `attractions` 表已存在（red_tourism.db）
- `user_browse_history` 表已创建
- `attraction_ratings` 表已创建
- `historical_events` 表已存在
- `attraction_events` 表已存在

✅ **测试数据已就绪**：
- 10 个测试用户：`user1` 到 `user10`，密码：`password123`
- 景点数据已存在于 `red_tourism.db`

**后端可以直接开始实现登录功能**，使用上述 SQL 语句操作数据库即可。