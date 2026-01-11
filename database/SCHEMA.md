# 数据库格式说明（red_tourism.db）

该文档列出仓库中用于本地测试的单一 SQLite 数据库 `database/red_tourism.db` 的表结构摘要、用途与存储建议。

概览
- 数据库文件：`database/red_tourism.db`
- 主要用途：存放景点主数据、历史事件、用户与行为数据、评价，以及旅行日记与图片引用，供本地集成测试使用。

主要表（摘要）
- `attractions`：景点主表
  - 字段：`id` (PK), `name`, `address`, `longitude`, `latitude`, `category`, `brief_intro`, `historical_background`, `per_capita_consumption`, `business_hours`, `created_at`, `updated_at`
  - 用途：景点元数据与定位信息。

- `historical_events`：历史事件表
  - 字段：`id` (PK), `event_name`, `start_year`, `end_year`, `description`, `period`

- `attraction_events`：景点-事件关联表
  - 字段：`id` (PK), `attraction_id` (FK -> `attractions.id`), `event_id` (FK -> `historical_events.id`)

- `users`：用户表
  - 字段：`id` (PK), `username` (unique), `password` (bcrypt hash), `created_at`, `last_login`

- `user_browse_history`：用户浏览历史
  - 字段：`id` (PK), `user_id` (FK -> `users.id`), `attraction_id` (FK -> `attractions.id`), `browse_time`

- `attraction_ratings`：景点评价
  - 字段：`id` (PK), `attraction_id` (FK -> `attractions.id`), `user_id` (FK -> `users.id`), `rating` (1-5), `comment`, `created_at`

- `diaries`：旅行日记
  - 字段：`id` (PK), `user_id` (FK -> `users.id`), `attraction_id` (FK -> `attractions.id`, 可空), `title`, `content` (长文本), `is_public`, `created_at`, `updated_at`
  - 用途：用户发布的行程/游记，支持关联某个景点。

- `diary_images`：日记图片
  - 字段：`id` (PK), `diary_id` (FK -> `diaries.id`), `file_path` (建议相对路径), `image_blob` (可选二进制), `caption`, `display_order`, `created_at`
  - 建议：为避免数据库膨胀，推荐把图片保存在文件系统（仓库外或 `database/uploads/`），在 `file_path` 中保存相对路径；仅在特殊场景下使用 `image_blob`。

索引与完整性
- 数据库包含若干索引（如 `idx_users_username`, `idx_user_browse_user_id` 等）以加速查询。迁移脚本中已启用 `PRAGMA foreign_keys = ON;`，建议在应用时确保外键检查开启以获得完整性保护。

迁移与脚本
- 迁移文件：位于 `database/migrations/`，按版本命名（例如 `002_users_and_ratings.sql`, `003_diary.sql`）。
- 初始化脚本：`database/init_local.ps1` 会按名称顺序应用 `migrations/*.sql` 到 `red_tourism.db` 并调用 `scripts/create_test_users.js` 生成示例用户。
- 合并脚本：`database/migrate_users_to_red_tourism.js` 可把历史 `users.db` 的数据导入 `red_tourism.db`（如果存在）。

当前数据状况（运行时样例）
- attractions: ~240 条（示例景点数据）
- users: 有示例用户（10 条）
- user_browse_history / attraction_ratings / diaries / diary_images: 可以为空（视是否生成测试数据）

使用建议
- 本地测试时使用 `red_tourism.db` 作为单一数据源以保证外键与联表查询行为一致。
- 图片推荐保存在文件系统，数据库中只保存路径。
- 若需要在 CI 中验证迁移，可在工作流中运行 `database/init_local.ps1` 或等效 shell 命令来完成构建数据库并运行简单断言。

如需我把这份说明合并进 `docs/`（例如 `docs/DB_SCHEMA.md`）或生成更详细的 ER 图，请告诉我。
