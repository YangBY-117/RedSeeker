# User 模块使用说明

本文档说明 `user` 模块的接口、鉴权方式与请求示例。

## 鉴权说明

- 登录成功后会返回 `token`。
- 除注册/登录外，其它接口都需要在请求头里带上 `X-Auth-Token`。
- `X-Auth-Token` 必须与请求参数中的 `userId` 匹配。

## 接口列表

基础路径：`/api/user`

### 1. 用户注册

`POST /api/user/register`

请求体：

```json
{
  "username": "user11",
  "password": "password123"
}
```

### 2. 用户登录

`POST /api/user/login`

请求体：

```json
{
  "username": "user1",
  "password": "password123"
}
```

响应体（示例）：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "token": "uuid-token",
    "user": {
      "id": 1,
      "username": "user1",
      "createdAt": "2026-01-10 18:02:07",
      "lastLogin": "2026-01-12 09:30:12"
    }
  }
}
```

### 3. 用户信息查询

`GET /api/user/profile?userId=1`

请求头：

```
X-Auth-Token: <token>
```

### 4. 用户信息更新

`PUT /api/user/profile`

请求头：

```
X-Auth-Token: <token>
```

请求体（用户名或密码二选一即可）：

```json
{
  "userId": 1,
  "username": "new_user",
  "password": "new_password"
}
```

### 5. 浏览历史新增

`POST /api/user/browse`

请求头：

```
X-Auth-Token: <token>
```

请求体：

```json
{
  "userId": 1,
  "attractionId": 10
}
```

### 6. 浏览历史查询

`GET /api/user/browse?userId=1`

请求头：

```
X-Auth-Token: <token>
```

### 7. 浏览历史更新

`PUT /api/user/browse`

请求头：

```
X-Auth-Token: <token>
```

请求体：

```json
{
  "id": 5,
  "userId": 1,
  "attractionId": 12,
  "browseTime": "2026-01-12 10:12:00"
}
```

### 8. 浏览历史删除

`DELETE /api/user/browse/5?userId=1`

请求头：

```
X-Auth-Token: <token>
```

### 9. 景点评分新增

`POST /api/user/ratings`

请求头：

```
X-Auth-Token: <token>
```

请求体：

```json
{
  "userId": 1,
  "attractionId": 10,
  "rating": 5,
  "comment": "非常推荐"
}
```

### 10. 景点评分查询

`GET /api/user/ratings?userId=1`

请求头：

```
X-Auth-Token: <token>
```

### 11. 景点评分更新

`PUT /api/user/ratings`

请求头：

```
X-Auth-Token: <token>
```

请求体：

```json
{
  "id": 7,
  "userId": 1,
  "rating": 4,
  "comment": "二次评价"
}
```

### 12. 景点评分删除

`DELETE /api/user/ratings/7?userId=1`

请求头：

```
X-Auth-Token: <token>
```

## 数据库说明

- 默认读取 `database/red_tourism.db`。
- 可通过环境变量 `REDSEEKER_DB_PATH` 指定数据库路径。
