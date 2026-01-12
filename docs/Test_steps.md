# 项目功能测试步骤与状态报告

本文档记录了 RedSeeker 项目的功能测试流程、验证方法及当前各模块的开发状态。

## 1. 测试概览

- **测试日期**: 2026-01-12
- **测试环境**: Windows / JDK 17 / Maven 3.9.6 / Node.js 20.10.0
- **数据库**: SQLite (`database/red_tourism.db`)

## 2. 后端功能测试

后端核心推荐模块已完成与 SQLite 数据库的对接，并通过自动化集成测试。

### 2.1 自动化测试

运行以下命令执行 JUnit 测试套件：

```powershell
# 确保已加载环境 (. .\setup_tools.ps1)
cd backend
mvn test
```

**测试覆盖范围**:
- `RecommendControllerTest`: 测试 API 接口层，验证 HTTP 状态码与 JSON 结构。
- `ZhipuAiIntegrationTest`: 测试与智谱 AI 的外部接口连通性。

**最新测试结果**:
- [x] `testGetRecommendations_ContentBased`: **通过** (正确返回 "革命旧址" 等中文分类)
- [x] `testZhipuAiIntegration`: **通过** (API Key 有效，响应正常)

### 2.2 手动接口测试 (API)

在后端启动 (`mvn spring-boot:run`) 的状态下，可使用 Curl 或 Postman 进行验证。

#### 场景 1: 获取推荐列表 (基于内容与用户协同)

**请求**:
```bash
POST http://localhost:8080/api/recommend/list
Content-Type: application/json

{
  "userId": 101,
  "city": "Shanghai",
  "preferences": ["革命旧址", "纪念馆"]
}
```

**预期响应**:
- 返回状态 `200 OK`
- `data` 数组包含推荐景点。
- `data[0].category` 显示中文名称（如 "革命旧址"）。
- `data[0].reason` 包含推荐理由（如 "Matches your interests..."）。

#### 场景 2: 生成 AI 行程方案

**请求**:
```bash
POST http://localhost:8080/api/recommend/ai-plan
Content-Type: application/json

{
  "city": "Shanghai",
  "days": 2,
  "prompt": "我想带孩子去参观，希望轻松一点"
}
```

**预期响应**:
- 返回 AI 生成的文本描述行程。

## 3. 前端功能测试

前端目前处于基础架构搭建阶段，且编译通过。

### 3.1 构建测试

验证前端代码的构建完整性：

```powershell
cd frontend
npm install
npm run build
```

**结果**:
- [x] 构建成功 (生成 `dist/` 目录)
- [ ] 页面功能 (UI 目前为静态占位符，尚未对接后端 API)

## 4. 遗留问题与下一步计划

1.  **前端对接**: `RecommendView.vue` 等视图目前仅包含静态文本，需编写 API 调用逻辑以展示后端数据。
2.  **数据完善**: 数据库中目前的测试数据已通过后端验证，后续需根据实际需求扩充。
