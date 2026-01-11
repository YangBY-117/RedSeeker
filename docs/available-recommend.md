# Recommend 模块可用接口说明

本文档基于当前已实现的 `backend` recommend 模块代码，列出可直接使用的接口与请求方式。

**最新更新 (2026-01-11)**:
1.  **推荐算法升级**: 引入并实现了 **混合推荐系统**，结合了 *基于内容的推荐 (Content-Based)* 和 *基于用户的协同过滤 (User-Based CF)*。
2.  **AI 能力接入**: 集成了 **智谱 AI (GLM-4)** SDK，提供真实的个性化行程生成能力。

## 基础说明

- 基础路径前缀：`/api/recommend`
- 响应统一封装：`ApiResponse`
- 参数校验：`@Valid` + `@NotBlank`，缺失必填字段会返回 400

## 接口 1：获取推荐列表

- 路径：`POST /api/recommend/list`
- 说明：返回红色旅游推荐列表。支持根据用户 ID 进行协同过滤推荐。

请求体（JSON）：

```json
{
  "city": "上海",
  "preferences": ["革命旧址", "建党", "历史"],
  "travelStyle": "研学",
  "days": 2,
  "userId": 101
}
```

字段说明：

- `city`：必填，城市名称
- `preferences`：可选，偏好分类或标签数组（匹配 `category` 或 `tags`，如“革命旧址”“建党”等）
- `userId`：**[新增]** 可选，Long 类型。如果提供，将触发协同过滤算法，参考相似用户的喜好。
    - *测试可用 ID*: `101` (偏好一大会址), `102` (偏好纪念馆), `103` (高分用户)
- `travelStyle`：可选，旅行风格（暂未深度集成）
- `days`：可选，出行天数

**算法逻辑说明**：
- 采用混合加权评分：`Score = (基础分 * 0.2) + (内容匹配分 * 0.4) + (协同过滤分 * 0.4)`
- 若未提供 `userId`，则降级为：`Score = (基础分 * 0.4) + (内容匹配分 * 0.6)`
- **内容匹配**：检测 `category` 和 `tags` 与 `preferences` 的重合度。
- **协同过滤**：计算用户评分矩阵的余弦相似度。

响应示例（简化）：

```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": "1",
      "name": "中国共产党第一次全国代表大会会址",
      "category": "革命旧址",
      "tags": ["建党", "上海", "历史", "旧址"],
      "score": 0.98,
      "history": "中国共产党的诞生地...",
      "reason": "根据相似用户的喜好为您精选。"
    }
  ],
  "timestamp": "2026-01-11T00:00:00Z"
}
```

调用示例：

```bash
curl -X POST http://localhost:8080/api/recommend/list \
  -H "Content-Type: application/json" \
  -d "{\"city\":\"上海\",\"userId\":101,\"preferences\":[\"革命旧址\"]}"
```

## 接口 2：生成 AI 行程 (真实 AI)

- 路径：`POST /api/recommend/ai-plan`
- 说明：调用 **智谱 AI (GLM-4)** 大模型，根据自然语言需求实时生成定制化行程。

**⚠️ 前置配置**:
需要在 `backend/src/main/resources/application.yml` 中配置有效的 `zhipu.api.key` 才能正常使用，否则将返回错误提示。

请求体（JSON）：

```json
{
  "prompt": "想要两天的红色研学行程，希望了解建党历史",
  "city": "上海",
  "days": 2
}
```

字段说明：

- `prompt`：必填，自然语言需求描述
- `city`：可选，作为上下文传给 AI
- `days`：可选，作为上下文传给 AI

响应示例（简化）：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "summary": "AI为您生成的个性化方案：",
    "plans": [
      {
        "day": 1,
        "title": "AI定制行程",
        "description": "第一天：上午抵达上海，前往中共一大会址参观，了解建党历史...\n第二天：前往...",
        "activities": null
      }
    ]
  },
  "timestamp": "2026-01-11T00:00:00Z"
}
```

调用示例：

```bash
curl -X POST http://localhost:8080/api/recommend/ai-plan \
  -H "Content-Type: application/json" \
  -d "{\"prompt\":\"想要两天的红色研学行程\",\"city\":\"上海\",\"days\":2}"
```

## 错误响应示例

API Key 未配置或调用失败时，AI 接口可能返回：

```json
{
    ...
    "data": {
        "plans": [
            { "description": "Error: API Key not configured..." }
        ]
    }
}
```

## 已知限制

- **数据源**：推荐列表的景点数据 (`CATALOG`) 和用户评分矩阵 (`MOCK_USER_RATINGS`) 目前存储在内存中（仅作演示和算法验证），未连接 MySQL 数据库。
- **协同过滤范围**：仅内置了 id 为 101-103 的用户数据，其他用户 ID 将作为新用户处理（仅依赖内容推荐）。
