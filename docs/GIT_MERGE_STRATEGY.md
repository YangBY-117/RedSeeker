# Git 模块合并策略

## 当前情况

- **推荐模块**：已由用户完成并push到main分支 ✅
- **路线规划模块**：用户和另一个人都修改了，需要综合
- **场所查询模块**：另一个人完成，需要采用 ✅
- **日记模块**：另一个人完成，没问题 ✅

## 合并目标

1. **推荐模块**：使用main分支的代码（用户的版本）
2. **场所查询模块**：使用另一个人的代码
3. **路线规划模块**：先使用另一个人的代码，然后用户提供代码给另一个人综合修改

## 操作步骤

### 第一步：备份当前工作

```bash
# 1. 确保当前所有更改已提交或暂存
git status

# 2. 创建备份分支（以防万一）
git branch backup-$(date +%Y%m%d-%H%M%S)

# 3. 切换到main分支
git checkout main
git pull origin main
```

### 第二步：创建合并分支

```bash
# 创建新的合并分支
git checkout -b merge-modules

# 或者如果另一个人的代码在特定分支（如 nhaok-frontend）
git checkout -b merge-modules
```

### 第三步：合并另一个人的代码（场所和路线）

假设另一个人的代码在 `nhaok-frontend` 分支或 `dev` 分支：

```bash
# 方案A：如果另一个人的代码在独立分支
git checkout merge-modules
git merge nhaok-frontend --no-commit --no-ff

# 方案B：如果另一个人的代码在dev分支
git checkout merge-modules
git merge dev --no-commit --no-ff
```

### 第四步：选择性保留文件

#### 4.1 保留推荐模块（用户的代码）

```bash
# 恢复推荐模块相关文件到main分支的版本
git checkout main -- frontend/src/views/RecommendView.vue
git checkout main -- frontend/src/services/recommendService.js
git checkout main -- backend/src/main/java/com/redseeker/recommend/
```

#### 4.2 保留场所查询模块（另一个人的代码）

```bash
# 确保场所查询模块使用另一个人的版本
# 如果合并后已经是另一个人的版本，则不需要操作
# 如果需要恢复，使用：
# git checkout nhaok-frontend -- frontend/src/views/PlacesView.vue
# git checkout nhaok-frontend -- frontend/src/services/placeService.js
# git checkout nhaok-frontend -- backend/src/main/java/com/redseeker/place/
```

#### 4.3 保留路线规划模块（另一个人的代码，临时）

```bash
# 先使用另一个人的路线规划代码
# 如果合并后已经是另一个人的版本，则不需要操作
# 如果需要恢复，使用：
# git checkout nhaok-frontend -- frontend/src/views/RouteView.vue
# git checkout nhaok-frontend -- frontend/src/services/routeService.js
# git checkout nhaok-frontend -- backend/src/main/java/com/redseeker/route/
```

### 第五步：导出用户的路线代码给另一个人

```bash
# 1. 创建临时分支，包含用户的路线代码
git checkout main
git checkout -b user-route-code

# 2. 只保留路线相关文件
# 先删除其他文件（或创建新分支只包含路线文件）

# 3. 导出路线相关文件
# 方法A：创建补丁文件
git format-patch main -- frontend/src/views/RouteView.vue frontend/src/services/routeService.js backend/src/main/java/com/redseeker/route/

# 方法B：创建压缩包
# 手动复制以下文件到压缩包：
# - frontend/src/views/RouteView.vue
# - frontend/src/services/routeService.js
# - backend/src/main/java/com/redseeker/route/*.java
```

### 第六步：提交合并结果

```bash
# 检查合并状态
git status

# 添加所有更改
git add .

# 提交合并
git commit -m "合并模块：推荐用用户代码，场所和路线用另一个人的代码"

# 推送到远程（如果需要）
git push origin merge-modules
```

### 第七步：另一个人综合路线代码

另一个人需要：
1. 接收用户的路线代码文件
2. 在他的分支上综合两个版本的路线代码
3. 完成后提交并push
4. 然后用户再合并他的综合版本

## 推荐模块文件列表（用户的代码）

### 前端
- `frontend/src/views/RecommendView.vue`
- `frontend/src/services/recommendService.js`
- `frontend/src/components/AttractionCard.vue`（如果修改了）

### 后端
- `backend/src/main/java/com/redseeker/recommend/RecommendController.java`
- `backend/src/main/java/com/redseeker/recommend/RecommendServiceImpl.java`
- `backend/src/main/java/com/redseeker/recommend/RecommendService.java`
- `backend/src/main/java/com/redseeker/recommend/RecommendRequest.java`
- `backend/src/main/java/com/redseeker/recommend/RecommendItem.java`

## 场所查询模块文件列表（另一个人的代码）

### 前端
- `frontend/src/views/PlacesView.vue`
- `frontend/src/services/placeService.js`

### 后端
- `backend/src/main/java/com/redseeker/place/PlaceController.java`
- `backend/src/main/java/com/redseeker/place/PlaceServiceImpl.java`
- `backend/src/main/java/com/redseeker/place/PlaceService.java`
- `backend/src/main/java/com/redseeker/place/PlaceAroundRequest.java`
- `backend/src/main/java/com/redseeker/place/PlaceDistanceSortRequest.java`
- `backend/src/main/java/com/redseeker/place/PlaceCandidate.java`
- `backend/src/main/java/com/redseeker/place/PlaceLocation.java`

## 路线规划模块文件列表（需要综合）

### 前端
- `frontend/src/views/RouteView.vue`
- `frontend/src/services/routeService.js`

### 后端
- `backend/src/main/java/com/redseeker/route/RouteController.java`
- `backend/src/main/java/com/redseeker/route/RouteServiceImpl.java`
- `backend/src/main/java/com/redseeker/route/RouteService.java`
- `backend/src/main/java/com/redseeker/route/MultipleRouteRequest.java`
- `backend/src/main/java/com/redseeker/route/RoutePlanResult.java`
- `backend/src/main/java/com/redseeker/route/RouteLocation.java`
- `backend/src/main/java/com/redseeker/route/RouteInfo.java`
- `backend/src/main/java/com/redseeker/route/AttractionSummary.java`

## 注意事项

1. **数据库文件**：`database/red_tourism.db` 可能需要合并，建议使用数据库迁移工具
2. **配置文件**：检查 `backend/src/main/resources/application.yml` 是否有冲突
3. **依赖文件**：检查 `frontend/package.json` 和 `backend/pom.xml` 是否有冲突
4. **测试**：合并后务必测试所有模块功能

## 快速合并脚本

如果确定另一个人的代码在 `nhaok-frontend` 分支：

```bash
#!/bin/bash
# 快速合并脚本

# 1. 切换到main并更新
git checkout main
git pull origin main

# 2. 创建合并分支
git checkout -b merge-modules

# 3. 合并另一个人的分支
git merge nhaok-frontend --no-commit --no-ff

# 4. 恢复推荐模块到main版本
git checkout main -- frontend/src/views/RecommendView.vue
git checkout main -- frontend/src/services/recommendService.js
git checkout main -- backend/src/main/java/com/redseeker/recommend/

# 5. 检查状态
git status

# 6. 提交（手动检查后再提交）
# git add .
# git commit -m "合并模块：推荐用用户代码，场所和路线用另一个人的代码"
```
