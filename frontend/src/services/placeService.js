import { api } from './api'

/**
 * 场所查询服务
 */

/**
 * 场所周边搜索（由后端代理高德API）
 * @param {Object} params - 搜索参数
 * @param {number} params.longitude - 经度
 * @param {number} params.latitude - 纬度
 * @param {string} params.keywords - 关键词（如：超市、卫生间）
 * @param {string} params.types - 类型代码（可选）
 * @param {number} params.radius - 搜索半径（米），默认3000
 * @param {number} params.page - 页码，默认1
 * @param {number} params.pageSize - 每页数量，默认20
 * @returns {Promise<Object>} 搜索结果
 */
export async function searchNearbyPlaces(params) {
  try {
    // 直接请求后端自定义接口
    const response = await api.post('/place/around', params)
    
    // 检查响应格式
    if (response.data && response.data.success !== undefined) {
      // 后端返回格式: { success: true, data: {...} }
      return response.data
    } else if (response.data && response.data.data) {
      // 如果已经是 ApiResponse 格式
      return response.data
    } else {
      // 兼容其他格式
      return {
        success: true,
        data: response.data
      }
    }
  } catch (error) {
    console.error('搜索周边场所失败:', error)
    throw error
  }
}

/**
 * 获取实际路径距离并排序（调用后端API）
 * @param {Object} params - 参数
 * @param {number} params.longitude - 起点经度
 * @param {number} params.latitude - 起点纬度
 * @param {Array} params.places - 场所列表（包含location字段）
 * @param {string} params.transport_mode - 交通方式: 'driving' | 'walking' | 'transit'
 * @returns {Promise<Array>} 排序后的场所列表（包含实际距离）
 */
export async function getRealDistanceAndSort(params) {
  try {
    const response = await api.post('/place/distance-sort', {
      origin: {
        longitude: params.longitude,
        latitude: params.latitude
      },
      places: params.places,
      transport_mode: params.transport_mode || 'walking'
    })
    
    // 检查响应格式
    if (response.data && response.data.data) {
      // ApiResponse 格式: { success: true, data: [...] }
      return response.data.data
    } else if (Array.isArray(response.data)) {
      // 直接返回数组
      return response.data
    } else {
      console.warn('意外的响应格式:', response.data)
      return []
    }
  } catch (error) {
    console.error('获取实际距离失败:', error)
    throw error
  }
}

/**
 * 获取常用设施类别列表
 */
export function getCommonPlaceTypes() {
  return [
    { value: '', label: '全部', icon: '📍' },
    { value: '060000', label: '餐饮服务', icon: '🍽️' },
    { value: '070000', label: '购物服务', icon: '🛒' },
    { value: '080000', label: '生活服务', icon: '🏪' },
    { value: '100000', label: '风景名胜', icon: '🏛️' },
    { value: '110000', label: '商务住宅', icon: '🏢' },
    { value: '120000', label: '政府机构', icon: '🏛️' },
    { value: '130000', label: '科教文化', icon: '📚' },
    { value: '140000', label: '交通设施', icon: '🚇' },
    { value: '150000', label: '金融保险', icon: '🏦' },
    { value: '160000', label: '公司企业', icon: '🏢' },
    { value: '170000', label: '道路附属设施', icon: '🛣️' },
    { value: '180000', label: '地名地址', icon: '📍' },
    { value: '190000', label: '公共设施', icon: '🚻' }
  ]
}

/**
 * 根据关键词获取类型代码
 * @param {string} keyword - 关键词（如：超市、卫生间、餐厅）
 * @returns {string} 类型代码
 */
export function getTypeByKeyword(keyword) {
  const keywordMap = {
    '超市': '060100',
    '便利店': '060100',
    '商场': '060100',
    '餐厅': '050000',
    '饭店': '050000',
    '卫生间': '190000',
    '公厕': '190000',
    '厕所': '190000',
    '加油站': '030000',
    '银行': '150100',
    'ATM': '150100',
    '医院': '090000',
    '药店': '090000',
    '酒店': '100000',
    '宾馆': '100000',
    '停车场': '150600',
    '地铁站': '150500',
    '公交站': '150500'
  }

  // 精确匹配
  if (keywordMap[keyword]) {
    return keywordMap[keyword]
  }

  // 模糊匹配
  for (const [key, value] of Object.entries(keywordMap)) {
    if (keyword.includes(key) || key.includes(keyword)) {
      return value
    }
  }

  return ''
}
