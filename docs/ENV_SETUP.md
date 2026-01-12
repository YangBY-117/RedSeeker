# 开发与运行环境配置指南

本文档用于指导团队成员完成本地开发环境与运行环境的配置。

## 1. 通用环境要求

- **Git**: 版本控制工具
- **Java 17+**: 必须手动安装并配置 `JAVA_HOME`
- **Maven & Node.js**:
    - **推荐**: 使用项目自带的 `setup_tools.ps1` 脚本自动配置（无需手动安装）
    - 自行安装要求: Maven 3.9+, Node.js 20+
- **数据库**: **SQLite** (文件已包含在项目中，无需安装)

## 2. 快速启动脚本 (环境自动配置)

项目根目录提供了 `setup_tools.ps1` 脚本，用于自动下载并配置 Portable 版的 Maven 和 Node.js 环境。

**使用方式 (PowerShell):**

```powershell
# 在当前终端会话中激活环境（注意开头的 "点" 和 "空格"）
. .\setup_tools.ps1
```

> **重要提示**: 每次打开新的 VS Code 终端准备开发时，请先运行此命令以加载 Maven 和 Node.js 环境变量。

**验证安装:**
运行以下命令，应输出对应版本号：
```powershell
mvn -v   # 应显示 Maven 3.9.6
node -v  # 应显示 v20.10.0
```

## 3. 后端（Spring Boot）环境配置

### 3.1 启动准备

确保已安装 JDK 17+，并已在当前终端运行了环境配置脚本。

### 3.2 配置 AI 服务密钥

项目使用智谱 AI (Zhipu AI) 提供行程生成服务。
请打开 `backend/src/main/resources/application.yml` 文件，找到以下配置：

```yaml
zhipu:
  api:
    key: "your_api_key_here" # 请替换为真实有效的 API Key
```

> 申请地址: [智谱AI开放平台](https://open.bigmodel.cn/)

### 3.3 本地运行

进入 `backend/` 目录：

```powershell
# 编译并安装依赖
mvn clean install

# 启动应用
mvn spring-boot:run
```

> **特别说明**: 后端已配置为连接 SQLite 数据库，数据库文件位于 `../database/red_tourism.db`。请确保该文件存在。

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

**状态：已配置 (SQLite)**

- 项目使用 **SQLite** 数据库，无需安装额外的数据库服务器。
- 数据库文件位于项目根目录的 `database/red_tourism.db`。
- 后端已配置 JDBC 连接，开箱即用。

> 详细表结构与数据说明请参考: [README_DATABASE.md](./README_DATABASE.md)

## 6. 常见问题排查

- **端口冲突**：确认后端/前端默认端口未被占用，必要时修改配置
- **依赖安装失败**：尝试清理缓存后重装（`npm cache clean --force`）
- **数据库连接失败**：检查数据库是否启动、账号密码与连接地址是否正确

---

如有新的环境要求或工具链变更，请及时更新本文档。
