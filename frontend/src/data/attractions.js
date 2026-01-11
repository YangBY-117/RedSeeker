// 景点数据（从数据库导出）
// 使用脚本 scripts/extractAttractions.js 从 red_tourism.db 导出
// 运行: cd database && node ../frontend/scripts/extractAttractions.js

// 类别映射
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

// 动态加载景点数据
let attractions = []
let attractionsData = []

// 初始化函数
async function initAttractions() {
  try {
    // Vite支持直接import JSON
    const data = await import('./attractions.json')
    attractionsData = Array.isArray(data.default) ? data.default : []
  } catch (e) {
    console.warn('attractions.json 文件不存在或为空，请先运行数据提取脚本: cd database && node ../frontend/scripts/extractAttractions.js')
    attractionsData = []
  }
  
  // 确保每个景点都有类别名称
  attractions = (attractionsData || []).map(attr => ({
    ...attr,
    categoryName: attr.categoryName || categoryMap[attr.category] || '其他',
    // 如果没有评分数据，使用默认值
    average_rating: attr.average_rating || 4.5,
    total_ratings: attr.total_ratings || 100,
    heat_score: attr.heat_score || 100,
    recommend_score: attr.recommend_score || 80.0
  }))
}

// 立即初始化（异步）
initAttractions()

// 排序函数
const sortAttractions = (attractions, sortBy) => {
  const sorted = [...attractions]
  switch (sortBy) {
    case 'recommend':
      sorted.sort((a, b) => b.recommend_score - a.recommend_score)
      break
    case 'heat':
      sorted.sort((a, b) => b.heat_score - a.heat_score)
      break
    case 'rating':
      sorted.sort((a, b) => b.average_rating - a.average_rating)
      break
    default:
      sorted.sort((a, b) => b.recommend_score - a.recommend_score)
  }
  return sorted
}

// 获取推荐景点
export const getRecommendations = async (category = null, sortBy = 'recommend', page = 1, pageSize = 6) => {
  // 确保数据已加载
  if (attractions.length === 0) {
    await initAttractions()
  }
  
  return new Promise((resolve) => {
    setTimeout(() => {
      let result = [...attractions]
      
      // 类别筛选
      if (category) {
        result = result.filter(attraction => attraction.category === category)
      }
      
      // 排序
      result = sortAttractions(result, sortBy)
      
      // 分页
      const total = result.length
      const start = (page - 1) * pageSize
      const end = start + pageSize
      result = result.slice(start, end)
      
      resolve({
        success: true,
        data: {
          attractions: result,
          total: total,
          page: page,
          pageSize: pageSize,
          totalPages: Math.ceil(total / pageSize)
        }
      })
    }, 300)
  })
}

// 搜索景点
export const searchAttractions = async (keyword, category = null, sortBy = 'heat', page = 1, pageSize = 6) => {
  // 确保数据已加载
  if (attractions.length === 0) {
    await initAttractions()
  }
  
  return new Promise((resolve) => {
    setTimeout(() => {
      let result = attractions.filter(attraction => {
        const matchKeyword = !keyword || 
          attraction.name.includes(keyword) ||
          (attraction.brief_intro && attraction.brief_intro.includes(keyword)) ||
          (attraction.historical_background && attraction.historical_background.includes(keyword))
        const matchCategory = !category || attraction.category === category
        return matchKeyword && matchCategory
      })
      
      // 排序
      result = sortAttractions(result, sortBy)
      
      // 分页
      const total = result.length
      const start = (page - 1) * pageSize
      const end = start + pageSize
      result = result.slice(start, end)
      
      resolve({
        success: true,
        data: {
          attractions: result,
          total: total,
          page: page,
          pageSize: pageSize,
          totalPages: Math.ceil(total / pageSize)
        }
      })
    }, 300)
  })
}
