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

确保已安装 JDK 17+，并已在当前终端运行了环境配置脚本。

### 3.2 本地运行

进入 `backend/` 目录：

```powershell
# 编译并安装依赖
mvn clean install

# 启动应用
mvn spring-boot:run
```

> **特别说明**: 由于数据库选型未定，后端已配置为排除 `DataSourceAutoConfiguration`，启动时**不需要**连接数据库。

## 4. 前端（Vue 3 + Vite）环境配置

### 4.1 启动准备

进入 `frontend/` 目录（确保已运行环境配置脚本）：

```powershell
# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

## 5. 数据库环境配置

数据库与初始化（本地测试）
- 仓库采用“单一 DB”本地测试方案：`database/red_tourism.db` 包含景点与用户相关表（推荐）。
-- 迁移脚本：`database/migrations/` 下的版本化 SQL（例如 `002_users_and_ratings.sql`、`003_diary.sql` 等）。
-- 自动初始化脚本（推荐）：`database/init_local.ps1` — 按名称顺序在 `red_tourism.db` 上依次应用 `migrations/*.sql` 并生成测试用户。

**运行所需依赖（快速参考）**

- 系统工具：
    - `sqlite3` 命令行客户端（用于执行 `.read` 迁移脚本）。Windows 可从 SQLite 官网下载预编译二进制或使用 `winget`/`scoop` 安装。
    - `Node.js`（>=20）与 `npm`（已在本仓库 setup_tools 中提供可选便携版本）。
    - `Python`（>=3.8） — 用于运行示例/迁移辅助脚本（`001_init_example.py` 使用标准库 `sqlite3`，无需额外 pip 包）。

- Node 脚本依赖（项目内，位于 `database/package.json`）：
    - `bcryptjs`（用于生成 bcrypt 哈希）
    - `sqlite3`（Node SQLite 驱动，用于脚本写入 DB）

- 后端（可选，本地集成时需要）：
    - JDK 17+
    - Maven 3.9+
    - 若希望后端直接访问仓库内 SQLite：在 `backend/pom.xml` 添加 `org.xerial:sqlite-jdbc:3.40.0.0` 依赖（示例已在 `database/README.md` 中给出）。

安装与准备示例：

```powershell
# 安装 Node 依赖（在 database 目录）
cd database
npm install

# 在 red_tourism.db 上运行迁移并生成测试用户
sqlite3 red_tourism.db ".read migrations/002_users_and_ratings.sql"
node scripts/create_test_users.js --db red_tourism.db --count 10
```

说明：`001_init_example.py` 为 Python 示例脚本，仅使用标准库（`sqlite3`, `json`, `enum` 等），不需要额外 pip 包；直接用 `python 001_init_example.py` 运行即可（确保 Python 版本 >=3.8）。

附加说明（跨库与脚本行为）:

- `database/migrations/*` 中的迁移通常假定目标数据库包含 `attractions` 表（若迁移引用该表的外键）。建议将迁移运行到包含 `attractions` 的数据库（即 `red_tourism.db`）。

如果你选择把用户数据与景点数据放在不同的文件（`users.db` 与 `red_tourism.db`），有两种常见做法：
    - 推荐（确保外键约束生效）：将迁移运行到 `red_tourism.db`（合并方案），即在 `red_tourism.db` 上执行 `.read migrations/002_users_and_ratings.sql`，这样 `FOREIGN KEY (attraction_id) REFERENCES attractions(id)` 能被正确解析并受约束。
    - 可选（数据隔离，需注意限制）：把迁移运行到 `users.db`，并在查询时使用 `ATTACH 'red_tourism.db' AS tourism;` 以便跨库查询（使用 `tourism.attractions`）。**注意**：SQLite 对跨附加数据库（attached DB）的外键完整性支持有限或不可保证，跨库的 FOREIGN KEY 通常不会被强制执行，因此如果需要依赖数据库层面强制外键，请使用合并到单一 DB 的方案。

- `database/scripts/create_test_users.js`：
    - 默认目标 DB 为 `red_tourism.db`（仓库已采用合并方案），可通过 `--db` 指定为其他路径。
    - 脚本在目标 DB 上会确保 `users` 表存在（会执行 `CREATE TABLE IF NOT EXISTS users ...`），并用 `INSERT OR IGNORE` 写入用户名与 bcrypt 哈希密码（默认密码 `password123`，可用 `--password` 覆盖）。
    - 示例：

```powershell
cd database
npm install
# 在 red_tourism.db 中生成 10 个测试用户（推荐用于本地集成测试）
node scripts/create_test_users.js --db red_tourism.db --count 10
```

## 6. 常见问题排查

- **端口冲突**：确认后端/前端默认端口未被占用，必要时修改配置
- **依赖安装失败**：尝试清理缓存后重装（`npm cache clean --force`）
- **数据库连接失败**：检查数据库是否启动、账号密码与连接地址是否正确

---

如有新的环境要求或工具链变更，请及时更新本文档。
