
import { api } from './api'
import images from '../data/images.js'
import attrNameData from '../data/attr_name.json'


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
 * @param {number} params.userLongitude - 用户经度（可选）
 * @param {number} params.userLatitude - 用户纬度（可选）
 * @param {string} params.visitTime - 访问时间（可选，ISO字符串）
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
    userLongitude,
    userLatitude,
    visitTime,
    category,
    page = 1,
    pageSize = 12
  } = params

  // 构建请求体
  const requestBody = {
    city,
    preferences: preferences.length > 0 ? preferences : undefined,
    travelStyle,
    days,
    userId: userId ? Number(userId) : undefined,
    userLongitude: typeof userLongitude === 'number' ? userLongitude : undefined,
    userLatitude: typeof userLatitude === 'number' ? userLatitude : undefined,
    visitTime: visitTime || undefined
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

    // 类别映射：前端类别ID -> 后端英文类别名
    const categoryIdToEnglish = {
      1: 'Memorial Hall',            // 纪念馆
      2: 'Martyr Cemetery',          // 烈士陵园
      3: 'Memorial Hall',            // 会议旧址（也属于纪念馆类）
      4: 'Revolutionary Site',       // 战役遗址
      5: 'Celebrity Residence',      // 名人故居
      6: 'Revolutionary Site',       // 革命根据地
      7: 'Martyr Cemetery',          // 纪念碑塔（也属于烈士陵园类）
      8: 'Memorial Hall',            // 博物馆（也属于纪念馆类）
      9: 'Patriotic Education Base'  // 其他纪念地
    }

    // 如果有类别筛选，在前端进行筛选
    if (category) {
      const englishCategory = categoryIdToEnglish[category]
      if (englishCategory) {
        attractions = attractions.filter(attr => attr.category === englishCategory)
      }
    }

    // 保存所有数据的总数（用于分页计算）
    const total = attractions.length
    
    // 后端已经按score排序，直接进行分页
    const start = (page - 1) * pageSize
    const end = start + pageSize
    const paginatedAttractions = attractions.slice(start, end)

    // 根据后端返回的category推断原始categoryId，然后映射到数据库的类别名
    // 后端formatCategory映射：1->Revolutionary Site, 2->Celebrity Residence, 3->Memorial Hall, 4->Martyr Cemetery, 5->Patriotic Education Base, 其他->Category-X
    // 数据库category映射：1->纪念馆, 2->烈士陵园, 3->会议旧址, 4->战役遗址, 5->名人故居, 6->革命根据地, 7->纪念碑塔, 8->博物馆, 9->其他纪念地
    function getCategoryNameFromBackendCategory(backendCategory) {
      // 如果是Category-X格式，直接提取数字
      if (backendCategory && backendCategory.startsWith('Category-')) {
        const categoryId = parseInt(backendCategory.replace('Category-', ''), 10)
        const dbCategoryMap = {
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
        return dbCategoryMap[categoryId] || '其他'
      }
      
      // 如果是英文类别名，需要反向映射到categoryId，然后再映射到数据库类别名
      const backendToCategoryId = {
        'Revolutionary Site': 1,      // 后端认为1是Revolutionary Site，但数据库1是纪念馆
        'Celebrity Residence': 5,     // 后端认为2是Celebrity Residence，但数据库5是名人故居
        'Memorial Hall': 3,           // 后端认为3是Memorial Hall，但数据库3是会议旧址
        'Martyr Cemetery': 2,         // 后端认为4是Martyr Cemetery，但数据库2是烈士陵园
        'Patriotic Education Base': 9 // 后端认为5是Patriotic Education Base，但数据库9是其他纪念地
      }
      
      const categoryId = backendToCategoryId[backendCategory]
      if (categoryId) {
        const dbCategoryMap = {
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
        return dbCategoryMap[categoryId] || '其他'
      }
      
      return '其他'
    }

    // 处理图片路径和数据格式转换
    // 构建id到评分/热度的映射
    const idToAttrName = {}
    if (Array.isArray(attrNameData)) {
      attrNameData.forEach(item => {
        idToAttrName[String(item.id)] = item
      })
    }

    const processedAttractions = paginatedAttractions.map(attr => {
      const idStr = String(attr.id)
      const dbItem = idToAttrName[idStr] || {}
      // 根据后端返回的category获取数据库的类别名
      const categoryName = getCategoryNameFromBackendCategory(attr.category)
      // 将 score (0.0-1.0) 转换为 recommend_score (0-100)
      const recommendScore = attr.score ? Math.round(attr.score * 100) : 0

      // 用户评价分归一化到0-5区间
      const normalizedScore = attr.score ? (attr.score * 5) : (dbItem['评分'] || 0)

      // 图片优先级：后端字段 > images.js > getAttractionImageUrl
      let imageUrl = ''
      if (attr.image_url) {
        imageUrl = attr.image_url
      } else if (images && images[attr.id] && images[attr.id][0]) {
        imageUrl = images[attr.id][0]
      } else if (typeof getAttractionImageUrl === 'function') {
        imageUrl = getAttractionImageUrl(attr.name)
      } else {
        imageUrl = ''
      }

      return {
        ...attr,
        // 基本信息
        id: idStr,
        name: attr.name || '',
        // 坐标信息（确保包含longitude和latitude）
        longitude: attr.longitude ?? attr.lng ?? attr.lon,
        latitude: attr.latitude ?? attr.lat,
        // 类别（中文，使用数据库的类别映射）
        categoryName: categoryName,
        category: categoryName, // 兼容性
        // 图片路径
        image_url: imageUrl,
        // 简介和历史
        brief_intro: attr.history || attr.brief_intro || attr.reason || '',
        history: attr.history || '',
        reason: attr.reason || '',
        // 推荐分数
        recommend_score: recommendScore,
        score: normalizedScore,
        // 标签
        tags: attr.tags || [],
        // 使用后端返回的真实数据
        address: attr.address || '',
        business_hours: attr.business_hours || '全天开放',
        per_capita_consumption: attr.per_capita_consumption || 0,
        // 评分和热度优先级：后端 > db > 0
        average_rating:
          attr.averageRating !== null && attr.averageRating !== undefined
            ? attr.averageRating
            : (dbItem['评分'] || (attr.score ? attr.score * 5 : attr.average_rating) || null),
        total_ratings: attr.totalRatings !== null && attr.totalRatings !== undefined ? attr.totalRatings : (attr.total_ratings || 0),
        heat_score:
          attr.heatScore !== null && attr.heatScore !== undefined
            ? attr.heatScore
            : (dbItem['热度'] || attr.heat_score || null),
        stage_start: attr.stageStart,
        stage_end: attr.stageEnd,
        stage_name: attr.stageName
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
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 * @returns {Promise<Object>} 搜索结果
 */
export async function searchAttractions(params = {}) {
  const {
    keyword,
    category,
    page = 1,
    pageSize = 12
  } = params

  if (!keyword) {
    // 如果没有关键词，调用推荐接口
    return getRecommendations({ category, page, pageSize })
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

    // 类别映射：前端类别ID -> 后端英文类别名
    const categoryIdToEnglish = {
      1: 'Memorial Hall',            // 纪念馆
      2: 'Martyr Cemetery',          // 烈士陵园
      3: 'Memorial Hall',            // 会议旧址（也属于纪念馆类）
      4: 'Revolutionary Site',       // 战役遗址
      5: 'Celebrity Residence',      // 名人故居
      6: 'Revolutionary Site',       // 革命根据地
      7: 'Martyr Cemetery',          // 纪念碑塔（也属于烈士陵园类）
      8: 'Memorial Hall',            // 博物馆（也属于纪念馆类）
      9: 'Patriotic Education Base'  // 其他纪念地
    }

    // 类别筛选
    if (category) {
      const englishCategory = categoryIdToEnglish[category]
      if (englishCategory) {
        attractions = attractions.filter(attr => attr.category === englishCategory)
      }
    }

    // 后端已经按score排序，直接进行分页
    const total = attractions.length
    const start = (page - 1) * pageSize
    const end = start + pageSize
    const paginatedAttractions = attractions.slice(start, end)

    // 根据后端返回的category推断原始categoryId，然后映射到数据库的类别名
    function getCategoryNameFromBackendCategory(backendCategory) {
      // 如果是Category-X格式，直接提取数字
      if (backendCategory && backendCategory.startsWith('Category-')) {
        const categoryId = parseInt(backendCategory.replace('Category-', ''), 10)
        const dbCategoryMap = {
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
        return dbCategoryMap[categoryId] || '其他'
      }
      
      // 如果是英文类别名，需要反向映射到categoryId，然后再映射到数据库类别名
      const backendToCategoryId = {
        'Revolutionary Site': 1,
        'Celebrity Residence': 5,
        'Memorial Hall': 3,
        'Martyr Cemetery': 2,
        'Patriotic Education Base': 9
      }
      
      const categoryId = backendToCategoryId[backendCategory]
      if (categoryId) {
        const dbCategoryMap = {
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
        return dbCategoryMap[categoryId] || '其他'
      }
      
      return '其他'
    }

    // 处理图片路径和数据格式转换
    const processedAttractions = paginatedAttractions.map(attr => {
      // 根据后端返回的category获取数据库的类别名
      const categoryName = getCategoryNameFromBackendCategory(attr.category)
      
      // 将 score (0.0-1.0) 转换为 recommend_score (0-100)
      const recommendScore = attr.score ? Math.round(attr.score * 100) : 0
      
      return {
        ...attr,
        // 基本信息
        id: String(attr.id || attr.attractionId || ''),
        name: attr.name || attr.attractionName || '',
        // 类别（中文，使用数据库的类别映射）
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
        // 优先使用后端返回的评分和热度数据
        average_rating: attr.averageRating !== null && attr.averageRating !== undefined ? attr.averageRating : (attr.average_rating || attr.rating || null),
        total_ratings: attr.totalRatings !== null && attr.totalRatings !== undefined ? attr.totalRatings : (attr.total_ratings || attr.ratingCount || 0),
        heat_score: attr.heatScore !== null && attr.heatScore !== undefined ? attr.heatScore : (attr.heat_score || null),
        stage_start: attr.stageStart,
        stage_end: attr.stageEnd,
        stage_name: attr.stageName
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
 * 堆排序实现（用于第一页排序）
 * 使用最小堆来维护topN个最大元素
 * @param {Array} arr - 要排序的数组
 * @param {Function} compareFn - 比较函数，返回正数表示第一个元素更大
 * @param {number} topN - 只排序前N个元素
 * @returns {Array} 排序后的数组（降序）
 */
function heapSortTopN(arr, compareFn, topN) {
  if (arr.length <= topN) {
    // 如果数组长度小于等于topN，直接排序全部
    return [...arr].sort(compareFn)
  }

  // 使用最小堆来维护topN个最大元素
  // 堆顶是最小的元素，这样我们可以快速判断新元素是否应该进入堆
  const heap = []
  
  for (let i = 0; i < arr.length; i++) {
    if (heap.length < topN) {
      heap.push(arr[i])
      // 向上调整堆（维护最小堆性质）
      let idx = heap.length - 1
      while (idx > 0) {
        const parentIdx = Math.floor((idx - 1) / 2)
        // 如果父节点比子节点小（或相等），说明堆性质满足，退出
        // compareFn返回正数表示第一个元素更大，所以如果父节点更大，需要交换
        if (compareFn(heap[parentIdx], heap[idx]) > 0) {
          [heap[parentIdx], heap[idx]] = [heap[idx], heap[parentIdx]]
          idx = parentIdx
        } else {
          break
        }
      }
    } else {
      // 如果当前元素比堆顶元素大，替换堆顶
      // compareFn(arr[i], heap[0]) > 0 表示 arr[i] 比 heap[0] 大
      if (compareFn(arr[i], heap[0]) > 0) {
        heap[0] = arr[i]
        // 向下调整堆（维护最小堆性质）
        let idx = 0
        while (true) {
          let minIdx = idx
          const left = 2 * idx + 1
          const right = 2 * idx + 2
          
          // 找到最小的子节点
          if (left < heap.length && compareFn(heap[left], heap[minIdx]) < 0) {
            minIdx = left
          }
          if (right < heap.length && compareFn(heap[right], heap[minIdx]) < 0) {
            minIdx = right
          }
          
          if (minIdx === idx) break
          [heap[idx], heap[minIdx]] = [heap[minIdx], heap[idx]]
          idx = minIdx
        }
      }
    }
  }
  
  // 将堆中的元素按降序排序后返回
  return heap.sort(compareFn)
}

/**
 * 获取景点图片URL（使用前端public目录）
 * @param {string} attractionName - 景点名称
 * @returns {string} 图片URL
 */
function getAttractionImageUrl(attractionName) {
  if (!attractionName) return null
  
  // 使用前端public目录中的图片
  // 图片文件名格式：{景点名称}.jpg
  // 对景点名称进行URL编码，确保特殊字符正确处理
  const encodedName = encodeURIComponent(attractionName)
  return `/attraction_images/${encodedName}.jpg`
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
