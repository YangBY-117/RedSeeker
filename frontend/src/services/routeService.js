import { api } from './api'

/**
 * 路线规划服务
 */

/**
 * 获取当前位置
 * @returns {Promise<Object>} 当前位置信息 {longitude, latitude, address}
 */
export async function getCurrentLocation() {
  // 优先使用浏览器定位API获取真实位置
  try {
    return await getBrowserLocation()
  } catch (error) {
    console.warn('浏览器定位失败，尝试后端接口:', error.message)
    // 如果浏览器定位失败，尝试后端接口
    try {
      const response = await api.get('/route/current-location')
      return response.data.data
    } catch (backendError) {
      // 如果后端也失败，使用默认位置（北京邮电大学西土城校区）
      console.warn('后端获取位置也失败，使用默认位置:', backendError.message)
      return {
        longitude: 116.3574,
        latitude: 39.9612,
        address: '北京邮电大学西土城校区（默认位置）'
      }
    }
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
        const longitude = position.coords.longitude
        const latitude = position.coords.latitude
        
        console.log('浏览器定位成功:', { longitude, latitude })
        
        // 尝试通过逆地理编码获取地址（可选）
        const address = await reverseGeocode(longitude, latitude).catch(() => null)
        
        resolve({
          longitude,
          latitude,
          address: address || `当前位置（经度: ${longitude.toFixed(6)}, 纬度: ${latitude.toFixed(6)}）`
        })
      },
      (error) => {
        // 提供更友好的错误信息
        let errorMsg = '获取位置失败'
        if (error.code === 1) {
          errorMsg = '用户拒绝了位置请求，请在浏览器设置中允许位置访问'
        } else if (error.code === 2) {
          errorMsg = '位置信息不可用'
        } else if (error.code === 3) {
          errorMsg = '获取位置超时'
        } else {
          errorMsg = '获取位置失败：' + error.message
        }
        reject(new Error(errorMsg))
      },
      {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 60000 // 允许使用1分钟内的缓存位置
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
    attractionIds: params.attractionIds,
    startLocation: params.startLocation,
    endLocation: params.endLocation,
    transportMode: params.transportMode || 'driving',
    strategy: params.strategy || 'history_first'
  })
  return response.data.data
}
