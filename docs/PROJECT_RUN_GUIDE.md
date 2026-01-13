# 项目运行指南 (基于本地工具链)

本文档根据实际运行测试生成，指导如何使用项目内置的工具链 (`tools_setup` 目录) 快速启动前后端服务。

## 1. 环境准备

本项目在 `tools_setup` 目录下内置了特定版本的 Maven 和 Node.js，无需全局安装。

### 必修项
- **Java JDK**: 需要预先安装 JDK 17+ 并配置好 `JAVA_HOME` (测试环境使用的是 JDK 23)。

### 内置工具路径
- **Maven**: `tools_setup/apache-maven-3.9.6`
- **Node.js**: `tools_setup/node-v20.10.0-win-x64`

## 2. 启动步骤 (PowerShell)

为了顺利运行，建议在 PowerShell 终端中临时将内置工具添加到 PATH 环境变量中。

### 第一步：设置临时环境变量

在项目根目录下打开 PowerShell，执行以下命令(一次性设置当前终端会话)：

```powershell
$env:PATH = "$PWD\tools_setup\node-v20.10.0-win-x64;$PWD\tools_setup\apache-maven-3.9.6\bin;" + $env:PATH
```

验证环境是否生效：
```powershell
mvn -v
node -v
npm -v
```

### 第二步：启动后端 (Backend)

在一个终端窗口中（确保已执行第一步及其环境变量设置）：

```powershell
cd backend
mvn spring-boot:run
```

- 如果是首次运行，Maven 会自动下载依赖，可能需要几分钟。
- 启动成功后，控制台会显示 `Tomcat started on port 8080`。

### 第三步：启动前端 (Frontend)

打开一个新的终端窗口（**同样需要先执行第一步设置环境变量**）：

```powershell
cd frontend
# 首次运行需要安装依赖（包括 axios 等）
npm install

# 启动开发服务器
npm run dev
```

- 启动成功后，访问终端显示的地址（通常是 `http://localhost:5173` 或 `5174`）。

## 3. 常见问题处理

1.  **npm install 报错**:
    - 确保已经在当前终端执行了环境变量设置命令。
    - 如果依赖解析失败，可以尝试删除 `frontend/node_modules` 和 `package-lock.json` 后重试。

2.  **端口占用**:
    - 后端默认 `8080`，前端默认 `5173`。如果被占用，前端 Vite 会自动切换到 `5174` 等端口，后端需要修改 `application.yml`。

3.  **axios 缺失**:
    - 如果启动前端看到 `Error: The following dependencies are imported but could not be resolved: axios`，请务必先运行 `npm install`。
