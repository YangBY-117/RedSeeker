// 快速数据提取 - 使用database目录下的sqlite3（已安装）
const fs = require('fs')
const path = require('path')

// 动态查找sqlite3模块
function findSqlite3() {
  const scriptDir = __dirname
  const databaseDir = path.join(scriptDir, '../../database')
  const sqlite3Path = path.join(databaseDir, 'node_modules', 'sqlite3')
  
  if (fs.existsSync(sqlite3Path)) {
    return require(sqlite3Path).verbose()
  }
  
  // 尝试直接require
  try {
    return require('sqlite3').verbose()
  } catch (e) {
    throw new Error(`找不到sqlite3模块。请确保database目录已运行: cd database && npm install`)
  }
}

const sqlite3 = findSqlite3()

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
function extractAttractions() {
  const dbPath = path.join(__dirname, '../../database/red_tourism.db')
  const outputPath = path.join(__dirname, '../src/data/attractions.json')

  console.log('📂 数据库路径:', dbPath)
  console.log('📁 输出路径:', outputPath)
  
  if (!fs.existsSync(dbPath)) {
    console.error('❌ 数据库文件不存在:', dbPath)
    process.exit(1)
  }

  const db = new sqlite3.Database(dbPath, (err) => {
    if (err) {
      console.error('❌ 打开数据库失败:', err.message)
      process.exit(1)
    }
    console.log('✅ 已连接到数据库\n')
  })

  // 查询景点数据
  db.all(`
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
  `, (err, rows) => {
    if (err) {
      console.error('❌ 查询失败:', err.message)
      db.close()
      process.exit(1)
    }

    if (!rows || rows.length === 0) {
      console.error('❌ 查询失败：没有返回数据')
      db.close()
      process.exit(1)
    }

    console.log(`📊 查询到 ${rows.length} 个景点\n`)

    // 转换数据格式，添加类别名称和默认评分
    const attractions = rows.map(row => ({
      id: row.id,
      name: row.name || '',
      address: row.address || '',
      longitude: parseFloat(row.longitude) || 0,
      latitude: parseFloat(row.latitude) || 0,
      category: row.category,
      categoryName: categoryMap[row.category] || '其他',
      brief_intro: row.brief_intro || '',
      historical_background: row.historical_background || '',
      per_capita_consumption: row.per_capita_consumption || 0,
      business_hours: row.business_hours || '09:00-17:00',
      // 默认评分数据（后续可以从attraction_ratings表获取真实数据）
      average_rating: 4.5,
      total_ratings: 100,
      heat_score: 100,
      recommend_score: 80.0
    }))

    // 确保输出目录存在
    const outputDir = path.dirname(outputPath)
    if (!fs.existsSync(outputDir)) {
      fs.mkdirSync(outputDir, { recursive: true })
    }

    // 写入JSON文件
    fs.writeFileSync(outputPath, JSON.stringify(attractions, null, 2), 'utf8')
    console.log(`✅ 成功导出 ${attractions.length} 个景点`)
    console.log(`📄 文件已保存到: ${outputPath}`)
    console.log(`\n📋 数据预览:`)
    console.log(`  - 第一个: ${attractions[0]?.name || 'N/A'} (ID: ${attractions[0]?.id || 'N/A'})`)
    console.log(`  - 最后一个: ${attractions[attractions.length - 1]?.name || 'N/A'} (ID: ${attractions[attractions.length - 1]?.id || 'N/A'})`)
    console.log(`  - 类别分布:`)
    const categoryCount = {}
    attractions.forEach(attr => {
      categoryCount[attr.categoryName] = (categoryCount[attr.categoryName] || 0) + 1
    })
    Object.entries(categoryCount).forEach(([name, count]) => {
      console.log(`    ${name}: ${count}个`)
    })

    db.close()
  })
}

extractAttractions()
