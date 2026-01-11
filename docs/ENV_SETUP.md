# 开发与运行环境配置指南

本文档用于指导团队成员完成本地开发环境与运行环境的配置。

## 1. 通用环境要求

- **Git**: 版本控制工具
- **Java 17+**: 必须手动安装并配置 `JAVA_HOME`
- **Maven & Node.js**:
    - **推荐**: 使用项目自带的 `setup_tools.ps1` 脚本自动配置（无需手动安装）
    - 自行安装要求: Maven 3.9+, Node.js 20+

## 2. 快速启动脚本 (环境自动配置)

项目根目录提供了 `setup_tools.ps1` 脚本，用于自动下载并配置 Portable 版的 Maven 和 Node.js 环境。

**使用方式 (PowerShell):**

```powershell
# 在当前终端会话中激活环境（注意开头的 "点" 和 "空格"）
. .\setup_tools.ps1
```

> **重要提示**: 每次打开新的 VS Code 终端准备开发时，请先运行此命令以加载 Maven 和 Node.js 环境变量。

## 3. 后端（Spring Boot）环境配置

### 3.1 启动准备

````markdown
# 本地环境与数据库要求（简洁）

仅保留本地运行与数据库相关的必要说明。

先决条件
- `Node.js` >= 20（含 `npm`）
- `sqlite3` 命令行客户端（用于执行迁移 SQL）
- `Python` >= 3.8（用于可选样例脚本 `001_init_example.py`）

数据库与初始化（本地测试）
- 仓库采用“单一 DB”本地测试方案：`database/red_tourism.db` 包含景点与用户相关表（推荐）。
- 迁移脚本：`database/migrations/002_users_and_ratings.sql`（将在目标 DB 中创建 `users`、`user_browse_history`、`attraction_ratings` 等表）。
- 自动初始化脚本（推荐）：`database/init_local.ps1` — 在 `red_tourism.db` 上应用迁移并生成测试用户。
- 如需从历史分离的 `users.db` 导入，请使用：`database/migrate_users_to_red_tourism.js`。

快速示例命令（PowerShell）
```powershell
cd database
npm install
# 在 red_tourism.db 上应用迁移并生成 10 个测试用户（推荐）
.\init_local.ps1 -Count 10
```

要点说明
- 请确保 `sqlite3` 可用以使 SQL 迁移生效；若缺失脚本会给出警告并跳过 `.read` 步骤。
- SQLite 对跨附加数据库（`ATTACH`）的外键完整性支持有限；若依赖 DB 层外键约束，请使用合并到单一 DB 的方案（即在 `red_tourism.db` 上运行迁移）。

````

