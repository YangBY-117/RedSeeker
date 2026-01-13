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

    // 处理图片路径
    const processedAttractions = paginatedAttractions.map(attr => ({
      ...attr,
      // 构建图片路径：如果后端返回了图片路径，使用它；否则根据名称查找
      image_url: attr.image_url || getAttractionImageUrl(attr.name),
      // 确保有必要的字段
      average_rating: attr.average_rating || 0,
      total_ratings: attr.total_ratings || 0,
      heat_score: attr.heat_score || 0,
      recommend_score: attr.score ? attr.score * 100 : 0
    }))

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

    // 处理图片路径和数据格式
    const processedAttractions = paginatedAttractions.map(attr => ({
      ...attr,
      image_url: attr.image_url || attr.imageUrl || getAttractionImageUrl(attr.name),
      id: attr.id || attr.attractionId,
      name: attr.name || attr.attractionName,
      categoryName: attr.categoryName || attr.category || '其他',
      average_rating: attr.average_rating || attr.rating || 0,
      total_ratings: attr.total_ratings || attr.ratingCount || 0,
      heat_score: attr.heat_score || attr.heatScore || attr.total_ratings || 0,
      recommend_score: attr.score ? attr.score * 100 : (attr.recommend_score || 0),
      address: attr.address || '',
      brief_intro: attr.brief_intro || attr.history || '',
      business_hours: attr.business_hours || '',
      per_capita_consumption: attr.per_capita_consumption || 0
    }))

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
  // 后端应该配置静态资源映射：/api/images/attractions/{filename}
  // 图片文件名格式：{景点名称}.jpg
  const imageName = encodeURIComponent(attractionName + '.jpg')
  const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  // 注意：静态资源路径通常不在 /api 下，而是直接在根路径下
  return `${baseURL}/images/attractions/${imageName}`
}

/**
 * 记录用户浏览历史
 * @param {number} attractionId - 景点ID
 * @returns {Promise<void>}
 */
export async function recordBrowse(attractionId) {
  try {
    await api.post('/recommend/browse', { attraction_id: attractionId })
  } catch (error) {
    console.error('记录浏览历史失败:', error)
    // 不抛出错误，避免影响用户体验
  }
}
