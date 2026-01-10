# 项目结构说明

本项目采用前后端分离，目录结构如下：

```
RedSeeker/
├── frontend/          # Vue 前端工程
├── backend/           # Spring Boot 后端工程
├── database/          # 数据库脚本与迁移
└── docs/              # 项目文档与协作说明
```

## 模块拆分原则

- 前端与后端完全隔离，互不改动对方目录。
- 后端按业务模块分包，减少多人协作冲突：
  - `com.redseeker.user`：用户管理
  - `com.redseeker.recommend`：旅游推荐
  - `com.redseeker.route`：路径规划
  - `com.redseeker.place`：场所查询
  - `com.redseeker.diary`：旅游日记
  - `com.redseeker.common`：通用基础设施

## 总体设计补充

- 架构形态：前端 Vue 应用通过 HTTP 调用后端 REST API；后端负责业务逻辑与算法；数据层由 MySQL 提供持久化存储。
- 接口约定：所有后端接口统一以 `/api` 为前缀，返回 `ApiResponse` 结构，异常统一由全局处理器转换为错误响应。
- 推荐模块：
  - `/api/recommend/list`：基于用户偏好、旅行风格输出红色旅游推荐列表。
    - 核心类：`RecommendServiceImpl`（当前使用 Mock 数据，包含了“中共一大会址”、“井冈山”、“延安”等经典红色景点）。
    - 数据模型：`RecommendItem` 包含历史背景 (`history`) 和推荐理由 (`reason`) 字段，支持个性化评分。
  - `/api/recommend/ai-plan`：接收自然语言需求，Simulate 生成行程摘要与分日计划。
    - 输入：`AiPlanRequest` (prompt, days, city)
    - 输出：`AiPlanResponse` (summary, List<ItineraryPlan>)
- 模块协作：用户管理提供身份信息；推荐、路径规划、查询、日记模块通过服务层封装业务逻辑；数据库表结构由数据库负责人维护迁移脚本。
- 代码约定：
  - `common` 包放置通用响应体、错误码、异常与全局处理。
  - 各业务模块目录仅由对应负责人维护，避免交叉修改。
