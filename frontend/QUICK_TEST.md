# 快速测试数据库对接

## 方法1：使用database目录下的sqlite3（推荐，最快）

```bash
# 在项目根目录下运行
cd database
node ../frontend/scripts/extractAttractions-quick.cjs
```

这个脚本会：
- 自动使用 `database/node_modules/sqlite3`（已安装）
- 从 `database/red_tourism.db` 读取景点数据
- 导出到 `frontend/src/data/attractions.json`

## 方法2：在frontend目录下运行

```bash
cd frontend
npm run extract-data
```

（需要先确保 `database/node_modules/sqlite3` 存在）

## 验证数据

运行脚本后，检查：

1. **文件是否生成**：
   ```bash
   ls frontend/src/data/attractions.json
   ```

2. **数据是否正确**：
   - 文件应该包含景点数组
   - 每个景点应该有：id, name, address, longitude, latitude, category等字段

3. **前端是否能加载**：
   - 启动前端：`cd frontend && npm run dev`
   - 访问推荐页面，应该能看到景点列表

## 如果遇到问题

1. **找不到sqlite3模块**：
   ```bash
   cd database
   npm install
   ```

2. **数据库文件不存在**：
   - 确保 `database/red_tourism.db` 文件存在
   - 如果不存在，运行数据库初始化脚本

3. **数据为空**：
   - 检查数据库中是否有 `attractions` 表
   - 检查表中是否有数据
