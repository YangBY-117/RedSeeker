# User 模块实现与原理说明

本文档描述 `com.redseeker.user` 的实现结构、数据流与核心原理，供协作者快速理解与维护。

## 模块范围

用户模块负责以下能力：

- 用户注册与登录（密码加密、登录时间更新）
- 用户信息查询与维护
- 简易鉴权与 token 管理
- 用户行为：浏览历史与景点评分的增删改查

## 目录结构

- `UserController`：REST 接口层
- `UserService` / `UserServiceImpl`：业务逻辑与数据访问
- `AuthTokenStore`：内存 token 存储
- `BCrypt`：密码加密与校验
- `*Request` / `*Response`：请求与响应 DTO

## 数据来源与表结构

模块使用 SQLite 数据库 `database/red_tourism.db`，关联以下表：

- `users`：账号与登录时间
- `user_browse_history`：浏览历史
- `attraction_ratings`：景点评分

数据库路径可通过环境变量 `REDSEEKER_DB_PATH` 覆盖，未设置时默认读取 `database/red_tourism.db`。

## 核心流程

### 1. 注册

1. 检查用户名是否已存在  
2. 使用 `BCrypt` 对密码加盐哈希  
3. 写入 `users` 表  
4. 返回用户基础信息

### 2. 登录

1. 根据用户名查询用户记录  
2. 使用 `BCrypt.checkpw` 校验密码  
3. 更新 `users.last_login`  
4. 生成 token 并返回

### 3. 鉴权

- 登录返回的 token 需放在请求头 `X-Auth-Token`
- `AuthTokenStore` 将 token 与 userId 映射
- 业务接口调用 `assertAuthorized` 校验 token 与 userId 是否匹配

### 4. 浏览历史与评分

浏览历史与评分均直接映射到表的 CRUD 操作：

- 新增：插入记录并返回完整记录
- 查询：按 userId 查询并按时间倒序
- 修改：更新指定记录
- 删除：按记录 id + userId 删除

## 设计说明

- **密码安全**：使用 bcrypt 加密存储，避免明文密码落库。
- **简易鉴权**：token 内存存储便于教学与演示，生产环境建议替换为 JWT 或持久化方案。
- **错误处理**：统一抛出 `ServiceException` 由全局异常处理器转换为标准响应。

## 可扩展点

如需完善权限系统或账号体系，可按以下方向扩展：

- 新增角色/权限表，扩展鉴权逻辑
- 增加 token 过期时间与刷新机制
- 引入登录设备信息与审计日志
