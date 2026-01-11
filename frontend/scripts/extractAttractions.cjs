// 使用纯JavaScript的SQLite库sql.js，无需原生编译
// 使用.cjs扩展名以支持CommonJS语法
const fs = require('fs')
const path = require('path')

// 动态解析sql.js模块路径（支持从不同目录运行）
function findSqlJs() {
  const scriptDir = __dirname
  const frontendDir = path.join(scriptDir, '..')
  const frontendNodeModules = path.join(frontendDir, 'node_modules', 'sql.js')
  
  // 尝试从frontend/node_modules加载
  if (fs.existsSync(frontendNodeModules)) {
    return require(frontendNodeModules)
  }
  
  // 尝试直接require（如果当前目录有node_modules）
  try {
    return require('sql.js')
  } catch (e) {
    throw new Error(`找不到sql.js模块。请先运行: cd frontend && npm install`)
  }
}

const initSqlJs = findSqlJs()

// 景点类别映射
const categoryMap = {
  1: '纪念馆',
  2: '烈士陵园',
  3: '会议旧址',
  4: '战役遗址',
  5: '名人故居',
  6: '革命根据地',
  7: '纪念碑塔',
  8: '博物馆',
  9: '其他纪念地'
}

// 读取数据库并导出JSON
async function extractAttractions() {
  try {
    const dbPath = path.join(__dirname, '../../database/red_tourism.db')
    const outputPath = path.join(__dirname, '../src/data/attractions.json')

    // 初始化sql.js
    const SQL = await initSqlJs()
    
    // 读取数据库文件
    const buffer = fs.readFileSync(dbPath)
    const db = new SQL.Database(buffer)
    
    console.log('已连接到数据库')

    // 查询景点数据
    const result = db.exec(`
      SELECT 
        id,
        name,
        address,
        longitude,
        latitude,
        category,
        brief_intro,
        historical_background,
        per_capita_consumption,
        business_hours
      FROM attractions
      ORDER BY id
    `)

    if (!result || result.length === 0) {
      console.error('查询失败：没有返回数据')
      db.close()
      process.exit(1)
    }

    const rows = result[0].values
    const columns = result[0].columns

    // 转换数据格式，添加类别名称和默认评分
    const attractions = rows.map(row => {
      const rowObj = {}
      columns.forEach((col, index) => {
        rowObj[col] = row[index]
      })
      
      return {
        id: rowObj.id,
        name: rowObj.name || '',
        address: rowObj.address || '',
        longitude: parseFloat(rowObj.longitude) || 0,
        latitude: parseFloat(rowObj.latitude) || 0,
        category: rowObj.category,
        categoryName: categoryMap[rowObj.category] || '其他',
        brief_intro: rowObj.brief_intro || '',
        historical_background: rowObj.historical_background || '',
        per_capita_consumption: rowObj.per_capita_consumption || 0,
        business_hours: rowObj.business_hours || '09:00-17:00',
        // 默认评分数据（后续可以从attraction_ratings表获取真实数据）
        average_rating: 4.5,
        total_ratings: 100,
        heat_score: 100,
        recommend_score: 80.0
      }
    })

    // 确保输出目录存在
    const outputDir = path.dirname(outputPath)
    if (!fs.existsSync(outputDir)) {
      fs.mkdirSync(outputDir, { recursive: true })
    }

    // 写入JSON文件
    fs.writeFileSync(outputPath, JSON.stringify(attractions, null, 2), 'utf8')
    console.log(`成功导出 ${attractions.length} 个景点到 ${outputPath}`)

    db.close()
  } catch (error) {
    console.error('提取数据失败:', error)
    process.exit(1)
  }
}

extractAttractions()
