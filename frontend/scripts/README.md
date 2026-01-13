# 数据提取脚本

## 提取景点数据

从数据库 `red_tourism.db` 中提取景点数据并生成 JSON 文件供前端使用。

### 运行方式

在项目根目录下运行：

```bash
# 方式1：使用database目录下的node环境（推荐）
cd database
node ../frontend/scripts/extractAttractions.cjs

# 方式2：在frontend目录下运行
cd frontend
npm run extract-data
```

### 输出

脚本会在 `frontend/src/data/attractions.json` 生成景点数据文件。

### 注意事项

- 确保 `database/red_tourism.db` 文件存在
- 确保 `frontend/node_modules/sql.js` 已安装（运行 `cd frontend && npm install`）
- 使用 `sql.js` 纯 JavaScript 实现，无需原生编译，可在任何平台运行
