import { api } from './api'

/**
 * 路线规划服务
 */

/**
 * 获取当前位置
 * @returns {Promise<Object>} 当前位置信息 {longitude, latitude, address}
 */
export async function getCurrentLocation() {
  try {
    const response = await api.get('/route/current-location')
    return response.data.data
  } catch (error) {
    // 如果后端接口未实现，使用浏览器定位API
    return getBrowserLocation()
  }
}

/**
 * 使用浏览器定位API获取当前位置
 * @returns {Promise<Object>} 当前位置信息
 */
function getBrowserLocation() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('浏览器不支持定位功能'))
      return
    }

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const { longitude, latitude } = {
          longitude: position.coords.longitude,
          latitude: position.coords.latitude
        }
        
        // 尝试通过逆地理编码获取地址（可选）
        const address = await reverseGeocode(longitude, latitude).catch(() => null)
        
        resolve({
          longitude,
          latitude,
          address: address || `经度: ${longitude}, 纬度: ${latitude}`
        })
      },
      (error) => {
        reject(new Error('获取位置失败：' + error.message))
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    )
  })
}

/**
 * 逆地理编码（通过坐标获取地址）
 * 注意：这里可以调用高德地图API，但需要前端配置API Key
 * 目前先返回null，由后端处理
 */
async function reverseGeocode(longitude, latitude) {
  // TODO: 如果需要前端直接调用高德地图API，可以在这里实现
  // 目前先返回null，地址由后端通过API获取
  return null
}

/**
 * 规划单景点路线
 * @param {Object} params - 路线规划参数
 * @param {number} params.attraction_id - 景点ID
 * @param {Object} params.start_location - 起始位置 {longitude, latitude, address?}
 * @param {string} params.transport_mode - 交通方式: 'driving' | 'walking' | 'transit'
 * @returns {Promise<Object>} 路线规划结果
 */
export async function planSingleRoute(params) {
  const response = await api.post('/route/single', {
    attraction_id: params.attraction_id,
    start_location: params.start_location,
    transport_mode: params.transport_mode || 'driving'
  })
  return response.data.data
}

/**
 * 规划多景点路线
 * @param {Object} params - 路线规划参数
 * @param {Array<number>} params.attraction_ids - 景点ID数组
 * @param {Object} params.start_location - 起始位置 {longitude, latitude, address?}
 * @param {Object} params.end_location - 结束位置（可选）{longitude, latitude, address?}
 * @param {string} params.transport_mode - 交通方式: 'driving' | 'walking' | 'transit'
 * @param {string} params.strategy - 排序策略: 'history_first' | 'shortest'
 * @returns {Promise<Object>} 路线规划结果
 */
export async function planMultipleRoute(params) {
  const response = await api.post('/route/multiple', {
    attraction_ids: params.attraction_ids,
    start_location: params.start_location,
    end_location: params.end_location,
    transport_mode: params.transport_mode || 'driving',
    strategy: params.strategy || 'history_first'
  })
  return response.data.data
}
