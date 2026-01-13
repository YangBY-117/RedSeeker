import { api } from './api'

// 高德地图API Key
const AMAP_KEY = 'bfa236c5b4ff2d954936faa864c1a490'
const AMAP_BASE_URL = 'https://restapi.amap.com/v3'

/**
 * 场所查询服务
 */

/**
 * 高德地图周边搜索（前端直接调用）
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
  const {
    longitude,
    latitude,
    keywords = '',
    types = '',
    radius = 3000,
    page = 1,
    pageSize = 20
  } = params

  const url = `${AMAP_BASE_URL}/place/around`
  const queryParams = new URLSearchParams({
    key: AMAP_KEY,
    location: `${longitude},${latitude}`,
    keywords: keywords,
    types: types,
    radius: radius.toString(),
    page: page.toString(),
    offset: pageSize.toString(),
    extensions: 'all'
  })

  try {
    const response = await fetch(`${url}?${queryParams}`)
    const data = await response.json()

    if (data.status === '1' && data.pois) {
      // 转换数据格式
      const places = data.pois.map(poi => ({
        id: poi.id,
        name: poi.name,
        address: poi.address || '',
        location: {
          longitude: parseFloat(poi.location.split(',')[0]),
          latitude: parseFloat(poi.location.split(',')[1])
        },
        distance: parseInt(poi.distance) || 0, // 直线距离（米）
        type: poi.type || '',
        tel: poi.tel || '',
        business_area: poi.business_area || ''
      }))

      return {
        success: true,
        data: {
          places: places,
          total: parseInt(data.count) || places.length,
          page: page,
          pageSize: pageSize,
          totalPages: Math.ceil((parseInt(data.count) || places.length) / pageSize)
        }
      }
    } else {
      throw new Error(data.info || '搜索失败')
    }
  } catch (error) {
    console.error('周边搜索失败:', error)
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
  const response = await api.post('/place/distance-sort', {
    origin: {
      longitude: params.longitude,
      latitude: params.latitude
    },
    places: params.places,
    transport_mode: params.transport_mode || 'walking'
  })
  return response.data.data
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
