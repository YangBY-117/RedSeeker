# Database

该目录放置数据库建表脚本、迁移文件与本地测试数据库文件（以 `red_tourism.db` 为本地单一测试 DB）。

目录结构（说明）
- `migrations/`：SQL 迁移脚本（例如 `001_init.sql`, `002_users_and_ratings.sql`）
- `scripts/`：初始化与测试脚本（例如 `create_test_users.js`）
- `package.json`：脚本依赖（`bcryptjs`, `sqlite3`）
- `red_tourism.db`：景点数据库（包含 `attractions` 表，用于推荐与查询），仓库采用将用户数据合并到 `red_tourism.db` 的单一本地测试方案
*- `migrate_users_to_red_tourism.js`：用于把历史 `users.db` 文件的数据导入 `red_tourism.db`（如果你有历史 `users.db`，可使用此脚本）

本文档目的
- 详细说明如何使用 `users.db` 与 `red_tourism.db`、如何运行迁移、如何生成测试用户，以及如何在本地把数据库与后端进行验证性集成（仅供本地测试使用）。

主要概念
- `red_tourism.db`：景点主数据（仓库中已有，包含 `attractions` 等表），同时也作为本地用户数据的合并目标（`users` / `user_browse_history` / `attraction_ratings` 表）。
- `users.db`：历史可选的分离用户 DB（已从仓库移除），如需导入请参见 `migrate_users_to_red_tourism.js`。
- 迁移脚本位于 `migrations/`，请根据目标数据库（合并或分离）选择运行方式。

迁移与初始化策略（两种常见做法）

方案 A — 合并到单一 DB（推荐用于本地集成测试）
- 将 `red_tourism.db` 作为主数据库，在其上运行迁移以创建 `users` 等表。

示例：
```powershell
cd database
# 在 red_tourism.db 上运行迁移（会在该文件中创建 users 等表）
sqlite3 red_tourism.db ".read migrations/002_users_and_ratings.sql"

# 生成测试用户并写入 red_tourism.db
node scripts/create_test_users.js --db red_tourism.db --count 10
```

优点：外键引用（如 `attractions(id)`）直接生效，便于后端本地测试。

方案 B — 保持独立 DB（可选，适用于数据隔离或生产模拟）
- 把用户数据放在 `users.db`（历史做法），景点数据保留在 `red_tourism.db`；需要跨库查询时在 sqlite 会话中 `ATTACH`。

示例：
```powershell
cd database
# 在 users.db 上运行迁移，创建 users 与 ratings 表
sqlite3 users.db ".read migrations/002_users_and_ratings.sql"

# 在会话中附加 red_tourism.db 以便联合查询
sqlite3 users.db
sqlite> ATTACH 'red_tourism.db' AS tourism;
sqlite> SELECT u.username, t.name
         FROM users u
         JOIN tourism.attractions t ON t.id = 1
         LIMIT 10;
```

注意：迁移脚本中如果有外键引用 `attractions(id)`，在运行迁移之前要确保目标 DB 已含有 `attractions` 表，或将迁移运行到含有该表的 DB（通常是 `red_tourism.db`）。

生成测试用户
- 脚本位置：`scripts/create_test_users.js`。
- 依赖安装：
```powershell
cd database
npm install
```
- 运行脚本：
```powershell
# 在默认 red_tourism.db 生成 10 个用户（仓库已采用合并方案）
node scripts/create_test_users.js --count 10

# 若你有历史的 users.db 并想导入到 red_tourism.db，可先把文件放到该目录下，然后运行：
node migrate_users_to_red_tourism.js --from users.db --to red_tourism.db
```

脚本说明：
- 密码默认 `password123`，使用 bcrypt 哈希（`bcryptjs`）。
- 使用 `INSERT OR IGNORE`，重复运行不会新增同名用户。

数据库文件说明
- `database/red_tourism.db`：景点数据，包含 `attractions` 表与详细字段（`id`, `name`, `address`, `longitude`, `latitude`, `category`, `brief_intro`, 等）。
- 历史说明：`users.db` 曾被用作分离的用户 DB（可选），但已从仓库移除；推荐使用合并到 `red_tourism.db` 的方案以便在本地测试时外键完整性生效。

后端本地集成示例（Spring Boot）
- 在 `backend/pom.xml` 中添加 SQLite JDBC 依赖：
```xml
<dependency>
  <groupId>org.xerial</groupId>
  <artifactId>sqlite-jdbc</artifactId>
  <version>3.40.0.0</version>
</dependency>
```

- 在 `backend/src/main/resources/application.yml` 中配置（示例）：
```yaml
spring:
  datasource:
    url: jdbc:sqlite:${project.basedir}/database/red_tourism.db
    driver-class-name: org.sqlite.JDBC
  jdbc:
    initialize-schema: false
```

注意：SQLite 与 JPA/Hibernate 的兼容性有限，建议本地测试时使用 `JdbcTemplate` 或原生 JDBC；如需长期使用，请将生产环境数据库替换为 MySQL/Postgres 等。

验证与常用 SQL
- 列出用户：
```sql
SELECT id, username, created_at, last_login FROM users LIMIT 20;
```
- 计算景点评价统计：
```sql
SELECT attraction_id, COUNT(*) as total, ROUND(AVG(rating),2) as average_rating
FROM attraction_ratings GROUP BY attraction_id ORDER BY total DESC LIMIT 20;
```
- 查询景点详情（从 red_tourism.db）：
```sql
SELECT id, name, address, brief_intro FROM attractions WHERE id = 1;
```

迁移维护说明
- 所有迁移脚本统一放在 `migrations/`，请在每次修改表结构时新增版本化 SQL文件。
- 若希望由后端自动执行迁移，可使用 Flyway：
  - 在 `backend/pom.xml` 添加 `org.flywaydb:flyway-core` 依赖；
  - 在 `application.yml` 中设置 `spring.flyway.locations: filesystem:${project.basedir}/database/migrations`。

故障排查要点
- 外键错误（找不到 `attractions`）：说明迁移目标 DB 中没有 `attractions` 表，解决方法见“迁移与初始化策略”。
- 插入用户失败：请确认脚本的 `--db` 参数是否指向预期文件，或检查文件权限。

本次仓库变更（与数据库相关）
- 新增 `migrations/002_users_and_ratings.sql`（创建 `users`、`user_browse_history`、`attraction_ratings`）。
- 新增 `scripts/create_test_users.js` 与 `package.json`（用于生成测试用户并管理依赖）。


# Database

- 该目录放置数据库建表脚本和迁移文件，以及数据库db文件。
- 由数据库负责人维护。
