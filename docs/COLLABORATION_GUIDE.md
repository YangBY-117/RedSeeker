# 协作成员与分支验收指南

本文档用于指导新增协作成员、创建个人分支、提交验收指标，以及将可运行分支合并到 `main`。

## 一、在 GitHub 远程仓库添加协作成员

> 需仓库管理员/拥有者权限

1. 打开 GitHub 仓库主页。
2. 进入 **Settings** → **Collaborators and teams**。
3. 点击 **Add people**，输入对方 GitHub 用户名或邮箱。
4. 选择权限（建议：普通协作者使用 **Write** 权限）。
5. 发送邀请，对方接受即可加入。

## 二、协作者创建个人分支（以本地 Git 为例）

```bash
# 1) 克隆仓库（首次）
git clone <repo-url>
cd RedSeeker

# 2) 基于 main 创建个人分支（推荐：姓名/模块）
git checkout main
git pull

git checkout -b <name>/<module>

# 3) 提交改动并推送
git add .
git commit -m "<module>: 简要描述"

git push -u origin <name>/<module>
```

> 分支命名建议：`姓名/模块`，例如 `yang/recommend`、`niu/frontend`。

## 三、模块验收指标（合并前必须满足）

下列指标用于验收每个模块的可运行性与交付质量。建议在 PR 描述中逐条勾选。

### 1) 前端模块（`frontend/`）

- 本地可安装依赖且运行无报错。
- 启动命令：
  ```bash
  npm install
  npm run dev
  ```
- 所有新增页面/组件可正常渲染，路由可达。
- 无明显控制台报错或未处理 Promise 异常。

### 2) 后端模块（`backend/`）

- Spring Boot 服务可正常启动，无编译报错。
- 启动命令：
  ```bash
  mvn spring-boot:run
  ```
- 新增接口/功能具备基本自测（示例请求与返回）。
- 变更不影响其他模块（模块边界清晰）。

### 3) 数据库模块（`database/`）

- 迁移脚本命名规范、可重复执行（或明确说明执行顺序）。
- 脚本与后端字段保持一致（必要时在 PR 中说明变更影响）。
- 若涉及结构变更，附上简要说明（字段含义、默认值等）。

## 四、合并到 `main` 的流程

1. 协作者在自己的分支上提交代码并推送。
2. 发起 Pull Request（目标分支：`main`）。
3. 由负责人或指定 reviewer 根据 **验收指标** 进行检查。
4. 确认可运行后执行合并：
   - 推荐使用 **Squash and merge** 或 **Merge commit**。
5. 合并后删除远程分支（保持仓库整洁）。

---

如需额外标准（测试覆盖率、代码风格等），可在后续迭代中补充到本指南。
