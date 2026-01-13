// 简化版数据提取脚本 - 使用database目录下的sqlite3（如果可用）
// 或者提供手动提取的说明
const fs = require('fs')
const path = require('path')

console.log('=== 景点数据提取工具 ===\n')

const dbPath = path.join(__dirname, '../../database/red_tourism.db')
const outputPath = path.join(__dirname, '../src/data/attractions.json')

// 检查数据库文件
if (!fs.existsSync(dbPath)) {
  console.error('❌ 数据库文件不存在:', dbPath)
  process.exit(1)
}

console.log('✅ 数据库文件存在:', dbPath)
console.log('📁 输出路径:', outputPath)

// 尝试使用sql.js
const frontendDir = path.join(__dirname, '..')
const sqlJsPath = path.join(frontendDir, 'node_modules', 'sql.js')

if (!fs.existsSync(sqlJsPath)) {
  console.error('\n❌ sql.js未安装')
  console.log('请先运行: cd frontend && npm install')
  console.log('\n或者使用database目录下的Node.js环境:')
  console.log('  cd database')
  console.log('  node -e "const sqlite3=require(\'./node_modules/sqlite3\').verbose();const db=new sqlite3.Database(\'./red_tourism.db\');db.all(\'SELECT * FROM attractions\',(e,r)=>{if(e)console.error(e);else{const fs=require(\'fs\');fs.writeFileSync(\'../frontend/src/data/attractions.json\',JSON.stringify(r.map(x=>({id:x.id,name:x.name,address:x.address||\'\',longitude:parseFloat(x.longitude)||0,latitude:parseFloat(x.latitude)||0,category:x.category,categoryName:[\'纪念馆\',\'烈士陵园\',\'会议旧址\',\'战役遗址\',\'名人故居\',\'革命根据地\',\'纪念碑塔\',\'博物馆\',\'其他纪念地\'][x.category-1]||\'其他\',brief_intro:x.brief_intro||\'\',historical_background:x.historical_background||\'\',per_capita_consumption:x.per_capita_consumption||0,business_hours:x.business_hours||\'09:00-17:00\',average_rating:4.5,total_ratings:100,heat_score:100,recommend_score:80.0})),null,2));console.log(\'✅ 成功导出\',r.length,\'个景点\')}});db.close()"')
  process.exit(1)
}

// 使用sql.js提取数据
async function extract() {
  try {
    const initSqlJs = require(sqlJsPath)
    const SQL = await initSqlJs()
    
    const buffer = fs.readFileSync(dbPath)
    const db = new SQL.Database(buffer)
    
    console.log('✅ 已连接到数据库\n')
    
    // 查询景点数据
    const result = db.exec(`
      SELECT 
        id, name, address, longitude, latitude, category,
        brief_intro, historical_background, per_capita_consumption, business_hours
      FROM attractions
      ORDER BY id
    `)
    
    if (!result || result.length === 0) {
      console.error('❌ 查询失败：没有返回数据')
      db.close()
      process.exit(1)
    }
    
    const rows = result[0].values
    const columns = result[0].columns
    
    const categoryMap = {
      1: '纪念馆', 2: '烈士陵园', 3: '会议旧址', 4: '战役遗址',
      5: '名人故居', 6: '革命根据地', 7: '纪念碑塔', 8: '博物馆', 9: '其他纪念地'
    }
    
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
    console.log(`✅ 成功导出 ${attractions.length} 个景点到 ${outputPath}`)
    console.log(`\n📊 数据预览:`)
    console.log(`  - 第一个景点: ${attractions[0]?.name || 'N/A'}`)
    console.log(`  - 最后一个景点: ${attractions[attractions.length - 1]?.name || 'N/A'}`)
    
    db.close()
  } catch (error) {
    console.error('❌ 提取数据失败:', error.message)
    console.error(error.stack)
    process.exit(1)
  }
}

extract()
