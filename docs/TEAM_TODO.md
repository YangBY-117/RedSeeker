# 开发人员待办清单（严格分工）

> 说明：以下列表明确每位成员应编写与无需修改的文件，避免互相干扰。

## 杨博宇（组长 / 总体设计 + 旅游推荐模块）

**需要编写/维护**
- `docs/PROJECT_STRUCTURE.md`（总体设计与结构说明补充）
- `backend/src/main/java/com/redseeker/recommend/` 下的所有实现类（推荐算法、AI 行程接口）
- `backend/src/main/java/com/redseeker/common/` 下的通用返回体、异常封装等

**不要修改**
- `frontend/` 目录全部文件
- `backend/src/main/java/com/redseeker/route/`、`place/`、`diary/`、`user/` 目录
- `database/` 目录

## 牛浩凯（前端界面实现与交互设计）

**需要编写/维护**
- `frontend/src/` 下所有页面与组件文件
- `frontend/src/router/index.js`
- `frontend/src/styles/` 中样式文件

**不要修改**
- `backend/` 目录全部文件
- `database/` 目录全部文件
- `docs/TEAM_TODO.md`（仅由组长维护分工）

## 胡杨（后端逻辑与算法实现）

**需要编写/维护**
- `backend/src/main/java/com/redseeker/route/` 下路径规划算法相关类
- `backend/src/main/java/com/redseeker/place/` 场所查询模块
- `backend/src/main/java/com/redseeker/diary/` 日记管理模块

**不要修改**
- `frontend/` 目录全部文件
- `backend/src/main/java/com/redseeker/recommend/` 与 `user/`
- `database/` 目录全部文件

## 李正（数据库设计与管理 + 测试与文档）

**需要编写/维护**
- `database/migrations/` 下的 SQL 迁移脚本
- `database/README.md`（数据库说明与初始化步骤）
- `docs/` 中测试与部署相关文档

**不要修改**
- `frontend/` 目录全部文件
- `backend/` 目录全部文件（除非与数据库对接字段协商）
