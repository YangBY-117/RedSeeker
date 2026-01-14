import { api } from './api'

/**
 * 推荐服务
 * 调用后端推荐接口获取真实数据
 */

/**
 * 获取推荐景点列表
 * @param {Object} params - 查询参数
 * @param {string} params.city - 城市名称（必填）
 * @param {number} params.userId - 用户ID（可选，用于个性化推荐）
 * @param {Array<string>} params.preferences - 偏好标签（可选）
 * @param {string} params.travelStyle - 旅行风格（可选）
 * @param {number} params.days - 出行天数（可选）
 * @param {number} params.category - 类别ID（可选，1-9）
 * @param {string} params.sortBy - 排序方式（可选，'recommend' | 'heat' | 'rating'）
 * @param {number} params.page - 页码（可选）
 * @param {number} params.pageSize - 每页数量（可选）
 * @returns {Promise<Object>} 推荐列表
 */
export async function getRecommendations(params = {}) {
  const {
    city = '上海', // 默认城市
    userId,
    preferences = [],
    travelStyle,
    days,
    category,
    sortBy = 'recommend',
    page = 1,
    pageSize = 12
  } = params

  // 构建请求体
  const requestBody = {
    city,
    preferences: preferences.length > 0 ? preferences : undefined,
    travelStyle,
    days,
    userId: userId ? Number(userId) : undefined
  }

  // 移除 undefined 字段
  Object.keys(requestBody).forEach(key => {
    if (requestBody[key] === undefined) {
      delete requestBody[key]
    }
  })

  try {
    const response = await api.post('/recommend/list', requestBody)
    let attractions = response.data.data || []

    // 如果有类别筛选，在前端进行筛选（如果后端不支持）
    if (category) {
      attractions = attractions.filter(attr => {
        // 根据 category 字段或 tags 进行筛选
        return attr.category === category || 
               (attr.tags && attr.tags.some(tag => {
                 const categoryMap = {
                   1: '纪念馆', 2: '烈士陵园', 3: '会议旧址', 4: '战役遗址',
                   5: '名人故居', 6: '革命根据地', 7: '纪念碑塔', 8: '博物馆', 9: '其他纪念地'
                 }
                 return tag.includes(categoryMap[category])
               }))
      })
    }

    // 前端排序（如果后端已排序，这里可以跳过）
    if (sortBy === 'heat') {
      attractions.sort((a, b) => (b.heat_score || 0) - (a.heat_score || 0))
    } else if (sortBy === 'rating') {
      attractions.sort((a, b) => (b.average_rating || 0) - (a.average_rating || 0))
    } else {
      // 推荐排序（按 score）
      attractions.sort((a, b) => (b.score || 0) - (a.score || 0))
    }

    // 分页
    const total = attractions.length
    const start = (page - 1) * pageSize
    const end = start + pageSize
    const paginatedAttractions = attractions.slice(start, end)

    // 将英文类别翻译成中文
    const categoryMap = {
      'Revolutionary Site': '革命遗址',
      'Celebrity Residence': '名人故居',
      'Memorial Hall': '纪念馆',
      'Martyr Cemetery': '烈士陵园',
      'Patriotic Education Base': '爱国主义教育基地',
      'Category-1': '革命遗址',
      'Category-2': '名人故居',
      'Category-3': '纪念馆',
      'Category-4': '烈士陵园',
      'Category-5': '爱国主义教育基地'
    }

    // 处理图片路径和数据格式转换
    const processedAttractions = paginatedAttractions.map(attr => {
      // 将英文类别翻译成中文
      const categoryName = categoryMap[attr.category] || attr.category || '其他'
      
      // 将 score (0.0-1.0) 转换为 recommend_score (0-100)
      const recommendScore = attr.score ? Math.round(attr.score * 100) : 0
      
      return {
        ...attr,
        // 基本信息
        id: String(attr.id), // 确保 id 是字符串
        name: attr.name || '',
        // 类别（中文）
        categoryName: categoryName,
        category: categoryName, // 兼容性
        // 图片路径
        image_url: attr.image_url || getAttractionImageUrl(attr.name),
        // 简介和历史
        brief_intro: attr.history || attr.brief_intro || attr.reason || '',
        history: attr.history || '',
        reason: attr.reason || '',
        // 推荐分数
        recommend_score: recommendScore,
        score: attr.score || 0,
        // 标签
        tags: attr.tags || [],
        // 使用后端返回的真实数据
        address: attr.address || '',
        business_hours: attr.business_hours || '全天开放',
        per_capita_consumption: attr.per_capita_consumption || 0,
        // 使用后端返回的评分和热度数据（从数据库计算）
        average_rating: attr.averageRating !== null && attr.averageRating !== undefined ? attr.averageRating : (attr.average_rating || 4.5),
        total_ratings: attr.totalRatings !== null && attr.totalRatings !== undefined ? attr.totalRatings : (attr.total_ratings || 0),
        heat_score: attr.heatScore !== null && attr.heatScore !== undefined ? attr.heatScore : (attr.heat_score || 0)
      }
    })

    return {
      success: true,
      data: {
        attractions: processedAttractions,
        total,
        page,
        pageSize,
        totalPages: Math.ceil(total / pageSize)
      }
    }
  } catch (error) {
    console.error('获取推荐列表失败:', error)
    throw error
  }
}

/**
 * 搜索景点
 * @param {Object} params - 搜索参数
 * @param {string} params.keyword - 搜索关键词
 * @param {number} params.category - 类别筛选
 * @param {string} params.sortBy - 排序方式
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 * @returns {Promise<Object>} 搜索结果
 */
export async function searchAttractions(params = {}) {
  const {
    keyword,
    category,
    sortBy = 'heat',
    page = 1,
    pageSize = 12
  } = params

  if (!keyword) {
    // 如果没有关键词，调用推荐接口
    return getRecommendations({ category, sortBy, page, pageSize })
  }

  // 构建请求体，使用关键词作为偏好
  const requestBody = {
    city: '全国', // 搜索时使用全国
    preferences: [keyword],
    userId: undefined
  }

  try {
    const response = await api.post('/recommend/list', requestBody)
    let attractions = response.data.data || []

    // 进一步筛选包含关键词的景点
    attractions = attractions.filter(attr => {
      const nameMatch = attr.name && attr.name.includes(keyword)
      const historyMatch = attr.history && attr.history.includes(keyword)
      const tagsMatch = attr.tags && attr.tags.some(tag => tag.includes(keyword))
      return nameMatch || historyMatch || tagsMatch
    })

    // 类别筛选
    if (category) {
      attractions = attractions.filter(attr => {
        return attr.category === category || 
               (attr.tags && attr.tags.some(tag => {
                 const categoryMap = {
                   1: '纪念馆', 2: '烈士陵园', 3: '会议旧址', 4: '战役遗址',
                   5: '名人故居', 6: '革命根据地', 7: '纪念碑塔', 8: '博物馆', 9: '其他纪念地'
                 }
                 return tag.includes(categoryMap[category])
               }))
      })
    }

    // 排序
    if (sortBy === 'heat') {
      attractions.sort((a, b) => (b.heat_score || 0) - (a.heat_score || 0))
    } else if (sortBy === 'rating') {
      attractions.sort((a, b) => (b.average_rating || 0) - (a.average_rating || 0))
    }

    // 分页
    const total = attractions.length
    const start = (page - 1) * pageSize
    const end = start + pageSize
    const paginatedAttractions = attractions.slice(start, end)

    // 将英文类别翻译成中文
    const categoryMap = {
      'Revolutionary Site': '革命遗址',
      'Celebrity Residence': '名人故居',
      'Memorial Hall': '纪念馆',
      'Martyr Cemetery': '烈士陵园',
      'Patriotic Education Base': '爱国主义教育基地',
      'Category-1': '革命遗址',
      'Category-2': '名人故居',
      'Category-3': '纪念馆',
      'Category-4': '烈士陵园',
      'Category-5': '爱国主义教育基地'
    }

    // 处理图片路径和数据格式转换
    const processedAttractions = paginatedAttractions.map(attr => {
      // 将英文类别翻译成中文
      const categoryName = categoryMap[attr.category] || attr.category || '其他'
      
      // 将 score (0.0-1.0) 转换为 recommend_score (0-100)
      const recommendScore = attr.score ? Math.round(attr.score * 100) : 0
      
      return {
        ...attr,
        // 基本信息
        id: String(attr.id || attr.attractionId || ''),
        name: attr.name || attr.attractionName || '',
        // 类别（中文）
        categoryName: categoryName,
        category: categoryName, // 兼容性
        // 图片路径
        image_url: attr.image_url || attr.imageUrl || getAttractionImageUrl(attr.name || attr.attractionName),
        // 简介和历史
        brief_intro: attr.history || attr.brief_intro || attr.reason || '',
        history: attr.history || '',
        reason: attr.reason || '',
        // 推荐分数
        recommend_score: recommendScore,
        score: attr.score || 0,
        // 标签
        tags: attr.tags || [],
        // 使用后端返回的真实数据
        address: attr.address || '',
        business_hours: attr.business_hours || '全天开放',
        per_capita_consumption: attr.per_capita_consumption || 0,
        // 使用后端返回的评分和热度数据（从数据库计算）
        average_rating: attr.averageRating !== null && attr.averageRating !== undefined ? attr.averageRating : (attr.average_rating || attr.rating || 4.5),
        total_ratings: attr.totalRatings !== null && attr.totalRatings !== undefined ? attr.totalRatings : (attr.total_ratings || attr.ratingCount || 0),
        heat_score: attr.heatScore !== null && attr.heatScore !== undefined ? attr.heatScore : (attr.heat_score || 0)
      }
    })

    return {
      success: true,
      data: {
        attractions: processedAttractions,
        total,
        page,
        pageSize,
        totalPages: Math.ceil(total / pageSize)
      }
    }
  } catch (error) {
    console.error('搜索景点失败:', error)
    throw error
  }
}

/**
 * 获取景点图片URL
 * @param {string} attractionName - 景点名称
 * @returns {string} 图片URL
 */
function getAttractionImageUrl(attractionName) {
  if (!attractionName) return null
  
  // 构建图片路径：后端需要提供静态资源服务
  // 图片存储在 database/attraction_images/ 目录
  // 后端应该配置静态资源映射：/images/attractions/{filename}
  // 图片文件名格式：{景点名称}.jpg
  const imageName = encodeURIComponent(attractionName + '.jpg')
  
  // 获取基础URL，移除 /api 后缀（如果存在）
  let baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  if (baseURL.endsWith('/api')) {
    baseURL = baseURL.slice(0, -4) // 移除 '/api'
  }
  
  // 静态资源路径不在 /api 下
  return `${baseURL}/images/attractions/${imageName}`
}

/**
 * 记录用户浏览历史
 * @param {string|number} attractionId - 景点ID
 * @returns {Promise<void>}
 */
export async function recordBrowse(attractionId) {
  try {
    // 后端期望的字段名是 attraction_id（通过 @JsonProperty 注解）
    // 但需要转换为数字类型
    const id = typeof attractionId === 'string' ? parseInt(attractionId, 10) : attractionId
    await api.post('/recommend/browse', { attraction_id: id })
  } catch (error) {
    console.error('记录浏览历史失败:', error)
    // 不抛出错误，避免影响用户体验
  }
}
