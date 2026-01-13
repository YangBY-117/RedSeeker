// 快速测试数据库连接和数据提取
const fs = require('fs')
const path = require('path')

console.log('=== 数据库连接测试 ===')

// 检查数据库文件
const dbPath = path.join(__dirname, '../../database/red_tourism.db')
console.log('数据库路径:', dbPath)
console.log('数据库文件存在:', fs.existsSync(dbPath))

if (fs.existsSync(dbPath) {
  const stats = fs.statSync(dbPath)
  console.log('数据库文件大小:', (stats.size / 1024).toFixed(2), 'KB')
}

// 检查sql.js模块
const frontendDir = path.join(__dirname, '..')
const sqlJsPath = path.join(frontendDir, 'node_modules', 'sql.js')
console.log('\nsql.js路径:', sqlJsPath)
console.log('sql.js存在:', fs.existsSync(sqlJsPath))

// 尝试加载sql.js
try {
  const initSqlJs = require(sqlJsPath)
  console.log('sql.js加载成功')
  
  // 测试数据库读取
  if (fs.existsSync(dbPath)) {
    console.log('\n尝试读取数据库...')
    initSqlJs().then(SQL => {
      const buffer = fs.readFileSync(dbPath)
      const db = new SQL.Database(buffer)
      
      // 查询表结构
      const tables = db.exec("SELECT name FROM sqlite_master WHERE type='table'")
      console.log('数据库中的表:')
      if (tables.length > 0) {
        tables[0].values.forEach(row => {
          console.log('  -', row[0])
        })
      }
      
      // 查询景点数量
      const count = db.exec("SELECT COUNT(*) as count FROM attractions")
      if (count.length > 0 && count[0].values.length > 0) {
        console.log('\n景点总数:', count[0].values[0][0])
      }
      
      // 查询前3个景点
      const sample = db.exec("SELECT id, name, category FROM attractions LIMIT 3")
      if (sample.length > 0) {
        console.log('\n前3个景点示例:')
        const columns = sample[0].columns
        sample[0].values.forEach(row => {
          const obj = {}
          columns.forEach((col, i) => {
            obj[col] = row[i]
          })
          console.log('  -', obj)
        })
      }
      
      db.close()
      console.log('\n✅ 数据库连接测试成功！')
    }).catch(err => {
      console.error('❌ 数据库读取失败:', err.message)
    })
  }
} catch (err) {
  console.error('❌ sql.js加载失败:', err.message)
  console.log('\n请先运行: cd frontend && npm install')
}
