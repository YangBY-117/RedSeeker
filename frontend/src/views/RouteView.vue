<template>
  <div class="route-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">路线规划</h1>
      <p class="page-subtitle">从推荐中选择景点，规划您的红色旅游路线</p>
    </div>

    <div class="route-content">
      <!-- 左侧：已选景点列表 -->
      <div class="selected-section">
        <div class="section-card">
          <div class="card-header">
            <h2 class="card-title">已选景点</h2>
            <span class="attraction-count">{{ selectedCount }} 个</span>
          </div>

          <div v-if="selectedCount === 0" class="empty-state">
            <p class="empty-text">暂无景点</p>
            <p class="empty-hint">前往推荐页面选择景点加入路线</p>
          </div>

          <div v-else class="attraction-list">
            <div
              v-for="(attraction, index) in selectedAttractions"
              :key="attraction.id"
              class="attraction-item"
            >
              <div class="item-order">{{ index + 1 }}</div>
              <div class="item-content">
                <h3 class="item-name">{{ attraction.name }}</h3>
                <p class="item-address">{{ attraction.address }}</p>
                <span class="item-category">{{ attraction.categoryName }}</span>
              </div>
              <button
                class="btn-remove"
                @click="removeAttraction(attraction.id)"
                title="移除"
              >
                ×
              </button>
            </div>
          </div>

          <div v-if="selectedCount > 0" class="card-actions">
            <button
              class="btn btn-primary"
              :disabled="!canPlan || planning"
              @click="planRoute"
            >
              {{ planning ? '规划中...' : '开始规划路线' }}
            </button>
            <button
              class="btn btn-outline"
              @click="clearAttractions"
            >
              清空
            </button>
          </div>
        </div>

        <!-- 规划设置 -->
        <div v-if="selectedCount > 0" class="section-card">
          <h2 class="card-title">规划设置</h2>
          

          <div class="form-group">
            <label class="form-label">起点位置</label>
            <div class="input-group">
              <input
                id="start-input"
                v-model="startLocation.address"
                type="text"
                placeholder="请输入起点地址（支持自动补全）"
                class="form-input"
              />
              <button
                class="btn btn-outline btn-sm"
                :disabled="gettingLocation"
                @click="handleGetCurrentLocation"
              >
                {{ gettingLocation ? '获取中...' : '当前位置' }}
              </button>
            </div>
          </div>


          <div class="form-group">
            <label class="form-label">规划策略</label>
            <div class="strategy-options">
              <label class="strategy-option">
                <input
                  v-model="strategy"
                  type="radio"
                  value="history_first"
                  class="radio-input"
                />
                <div class="strategy-content">
                  <span class="strategy-title">历史优先</span>
                  <span class="strategy-desc">按历史阶段排序</span>
                </div>
              </label>
              <label class="strategy-option">
                <input
                  v-model="strategy"
                  type="radio"
                  value="shortest"
                  class="radio-input"
                />
                <div class="strategy-content">
                  <span class="strategy-title">最短路径</span>
                  <span class="strategy-desc">优化路线距离</span>
                </div>
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：地图展示 -->
      <div class="map-section">
        <div class="map-container" ref="mapContainer"></div>
        <div v-if="routeResult" class="route-info">
          <div class="info-item">
            <span class="info-label">总距离</span>
            <span class="info-value">{{ formatDistance(routeResult.total_distance) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">预计时间</span>
            <span class="info-value">{{ formatDuration(routeResult.total_duration) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouteCart } from '../composables/useRouteCart'
import { getCurrentLocation } from '../services/routeService'

// 高德地图相关
const mapContainer = ref(null)
let map = null
let markers = []
let polyline = null

// 路线购物车
const {
  selectedAttractions,
  removeAttraction,
  clearAttractions,
  count: selectedCount
} = useRouteCart()

// 规划设置
const startLocation = ref({
  longitude: null, // 初始为空，等待用户输入或获取当前位置
  latitude: null,
  address: ''
})
// 已删除终点功能

// 使用PlaceSearch搜索地点并在地图上标注
function searchAndMarkLocation(keyword, locationRef, markerType = 'normal') {
  return new Promise((resolve, reject) => {
    if (!window.AMap) {
      reject(new Error('高德地图API未加载'))
      return
    }
    
    window.AMap.plugin('AMap.PlaceSearch', () => {
      if (!window.AMap.PlaceSearch) {
        reject(new Error('PlaceSearch插件加载失败'))
        return
      }
      
      const placeSearch = new window.AMap.PlaceSearch({
        pageSize: 1, // 只取第一个结果
        pageIndex: 1,
        city: '全国', // 支持全国搜索
        citylimit: false,
        map: null, // 不自动在地图上显示
        panel: null, // 不使用默认面板
        autoFitView: false,
        type: '风景名胜|历史建筑' // 限定搜索类型，提高准确性
      })
      
      placeSearch.search(keyword, (status, result) => {
        if (status === 'complete' && result.poiList && result.poiList.pois.length > 0) {
          const poi = result.poiList.pois[0]
          
          // 检查 locationRef 是否有效
          if (!locationRef || !locationRef.value) {
            reject(new Error('位置引用无效'))
            return
          }
          
          // 更新位置信息
          locationRef.value.address = poi.name
          locationRef.value.longitude = poi.location.lng
          locationRef.value.latitude = poi.location.lat
          
          // 确保地图已初始化后再创建标记
          if (map) {
            createMarker([poi.location.lng, poi.location.lat], poi.name, markerType)
          }
          
          resolve({
            name: poi.name,
            lng: poi.location.lng,
            lat: poi.location.lat
          })
        } else if (status === 'no_data') {
          reject(new Error(`未找到相关地点: ${keyword}`))
        } else {
          // status === 'error' 或其他错误
          console.error('PlaceSearch错误详情:', { status, result, keyword })
          reject(new Error(`搜索失败: ${status}，关键词: ${keyword}`))
        }
      })
    })
  })
}

// 创建自定义标记
function createMarker(position, title, markerType = 'normal') {
  if (!map) return null
  
  let markerContent = ''
  let offset = new AMap.Pixel(-13, -30)
  
  if (markerType === 'start') {
    // 起点标记
    markerContent = `<div class="custom-content-marker">
      <img src="//a.amap.com/jsapi_demos/static/demo-center/icons/dir-marker.png">
      <div class="marker-label">起</div>
    </div>`
  } else if (markerType === 'end') {
    // 终点标记（最后一个景点）
    markerContent = `<div class="custom-content-marker">
      <img src="//a.amap.com/jsapi_demos/static/demo-center/icons/dir-marker.png">
      <div class="marker-label">终</div>
    </div>`
  } else if (typeof markerType === 'number') {
    // 途经点标记（带编号）
    markerContent = `<div class="custom-content-marker">
      <img src="//a.amap.com/jsapi_demos/static/demo-center/icons/dir-marker.png">
      <div class="marker-label">${markerType}</div>
    </div>`
  } else {
    // 普通标记
    markerContent = `<div class="custom-content-marker">
      <img src="//a.amap.com/jsapi_demos/static/demo-center/icons/dir-marker.png">
    </div>`
  }
  
  const marker = new AMap.Marker({
    position: position,
    title: title,
    content: markerContent,
    offset: offset,
    zIndex: 100
  })
  
  marker.setMap(map)
  markers.push(marker)
  
  return marker
}

// 初始化起点搜索（使用Autocomplete实现自动补全）
function initStartLocationSearch(inputId, locationRef) {
  if (!window.AMap) {
    setTimeout(() => initStartLocationSearch(inputId, locationRef), 100)
    return
  }
  
  window.AMap.plugin('AMap.Autocomplete', () => {
    if (!window.AMap.Autocomplete) {
      console.warn('Autocomplete插件加载失败')
      return
    }
    
    const inputElement = document.getElementById(inputId)
    if (!inputElement) {
      console.warn(`找不到输入框: ${inputId}`)
      return
    }
    
    // 创建自动补全实例
    const autocomplete = new window.AMap.Autocomplete({
      input: inputId,
      city: '全国' // 支持全国搜索
    })
    
    // 监听选择事件
    autocomplete.on('select', async function(e) {
      if (e.poi && e.poi.location) {
        // 直接使用POI的坐标
        if (locationRef && locationRef.value) {
          locationRef.value.address = e.poi.name
          locationRef.value.longitude = e.poi.location.lng
          locationRef.value.latitude = e.poi.location.lat
          
          // 创建起点标记
          if (map) {
            createMarker([e.poi.location.lng, e.poi.location.lat], e.poi.name, 'start')
            map.setCenter([e.poi.location.lng, e.poi.location.lat])
          }
        }
      } else if (e.poi && e.poi.name) {
        // 如果没有location，使用PlaceSearch查询
        try {
          await searchAndMarkLocation(e.poi.name, locationRef, 'start')
          if (map && locationRef.value && locationRef.value.longitude && locationRef.value.latitude) {
            map.setCenter([locationRef.value.longitude, locationRef.value.latitude])
          }
        } catch (err) {
          console.error('搜索地点失败:', err)
        }
      }
    })
  })
}

// 初始化搜索功能
const initSearchFunctions = () => {
  initStartLocationSearch('start-input', startLocation)
}

// 这个onMounted已合并到下面的onMounted中

// 已删除终点功能
const transportMode = ref('driving')
const strategy = ref('history_first')
const gettingLocation = ref(false)
const planning = ref(false)
const routeResult = ref(null)

// 交通方式选项
// 交通方式：只支持驾车

// 计算属性
const canPlan = computed(() => {
  return selectedCount.value > 0 && startLocation.value.longitude && startLocation.value.latitude
})

// 初始化地图
const initMap = () => {
  if (!window.AMap || !mapContainer.value) {
    console.error('高德地图API未加载或容器未找到')
    return
  }

  // 创建地图实例（使用默认中心点：北京）
  map = new AMap.Map(mapContainer.value, {
    zoom: 11,
    center: [116.397428, 39.90923], // 默认北京
    viewMode: '2D' // 使用2D模式
  })

  // 不再自动添加起点标记和测试路线
}

// 添加起点标记
const addStartMarker = () => {
  if (!map) return

  const marker = new AMap.Marker({
    position: [startLocation.value.longitude, startLocation.value.latitude],
    title: '起点',
    icon: new AMap.Icon({
      size: new AMap.Size(32, 40),
      image: 'data:image/svg+xml;base64,' + btoa(`
        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="40" viewBox="0 0 32 40">
          <path d="M0 0 L20 0 L16 8 L20 16 L0 16 Z" fill="#c62828"/>
          <rect x="0" y="16" width="4" height="24" fill="#8e0000"/>
        </svg>
      `),
      imageSize: new AMap.Size(32, 40)
    })
  })
  marker.setMap(map)
  markers.push(marker)
}

// 不再绘制测试路线
const drawTestRoute = () => {
  // 已删除测试路线功能
}

// 清除地图上的标记和路线
const clearMap = () => {
  if (markers.length > 0) {
    markers.forEach(marker => {
      marker.setMap(null)
    })
    markers = []
  }
  // 清除高德Driving绘制的路线
  if (map && map.getAllOverlays) {
    const polylines = map.getAllOverlays('polyline')
    polylines.forEach(p => {
      map.remove(p)
    })
  }
  // 清除自定义polyline（如果有）
  if (polyline) {
    polyline.setMap(null)
    polyline = null
  }
}

// 绘制路线
const drawRoute = (path) => {
  clearMap()

  if (!map || !path || path.length < 2) {
    console.warn('路径数据无效:', path)
    return
  }

  // 验证并过滤无效坐标
  const validPath = path.filter(point => {
    if (!Array.isArray(point) || point.length < 2) {
      return false
    }
    const lng = point[0]
    const lat = point[1]
    return typeof lng === 'number' && typeof lat === 'number' && 
           !isNaN(lng) && !isNaN(lat) && 
           isFinite(lng) && isFinite(lat)
  })

  if (validPath.length < 2) {
    console.warn('有效路径点不足:', validPath)
    return
  }

  // 使用高德地图路线规划服务绘制平滑曲线
  // 根据文档，应该让Driving插件自动绘制路线，然后自定义样式
  if (window.AMap) {
    window.AMap.plugin('AMap.Driving', () => {
      // 创建路线规划服务，让高德自动绘制路线（使用默认绿色）
      const driving = new AMap.Driving({
        map: map,
        panel: null,
        hideMarkers: true // 隐藏默认标记，使用自定义标记
        // 不设置polylineOptions，使用高德默认的绿色路线
      })
    
      // 构建路径点：起点 -> 途经点 -> 终点（最后一个景点）
      // validPath格式：[起点, 景点1, 景点2, ..., 景点N]
      // 应该调用：起点 -> [景点1, 景点2, ..., 景点N-1] -> 景点N（终点）
      const waypoints = []
      if (validPath.length > 2) {
        // 有多个点，中间的点是途经点
        for (let i = 1; i < validPath.length - 1; i++) {
          waypoints.push(validPath[i])
        }
      }
      
      // 调用路线规划
      const searchCallback = (status, result) => {
        if (status === 'complete') {
          // 路线规划成功，高德会自动绘制路线
          if (result.routes && result.routes.length > 0) {
            const route = result.routes[0]
            
            // 从路线结果中获取总距离和总时间
            const totalDistance = route.distance || 0 // 单位：米
            const totalDuration = route.time || 0 // 单位：秒
            
            console.log('路线规划成功:', {
              distance: totalDistance,
              duration: totalDuration,
              routeCount: result.routes.length
            })
            
            // 更新总距离和总时间
            routeResult.value = {
              total_distance: totalDistance,
              total_duration: totalDuration
            }
            
            // 设置地图视野，包含路线和标记
            setTimeout(() => {
              const allOverlays = []
              // 获取Driving绘制的路线
              if (map && map.getAllOverlays) {
                const overlays = map.getAllOverlays('polyline')
                allOverlays.push(...overlays)
              }
              if (markers.length > 0) {
                allOverlays.push(...markers)
              }
              if (allOverlays.length > 0) {
                map.setFitView(allOverlays, false, [50, 50, 50, 50])
              }
            }, 200)
          } else {
            console.warn('路线规划结果中没有路线')
          }
        } else {
          console.warn('路线规划失败:', status, result)
        }
      }
      
      // 根据路径点数量选择不同的调用方式
      if (validPath.length === 2) {
        // 只有起点和终点（1个景点）
        driving.search(
          validPath[0], // 起点
          validPath[1], // 终点（唯一的景点）
          searchCallback
        )
      } else if (validPath.length > 2) {
        // 有多个点，最后一个作为终点，中间的是途经点
        const endPoint = validPath[validPath.length - 1]
        if (waypoints.length > 0) {
          driving.search(
            validPath[0], // 起点
            endPoint, // 终点（最后一个景点）
            {
              waypoints: waypoints // 途经点
            },
            searchCallback
          )
        } else {
          // 只有起点和终点
          driving.search(
            validPath[0], // 起点
            endPoint, // 终点
            searchCallback
          )
        }
      } else {
        console.warn('路径点不足，无法规划路线')
      }
    })
  }
  
  // 不再使用降级方案，只使用高德自动绘制的路线

  // 标记已经在planRoute中通过searchAndMarkLocation创建
  // 这里不再重复创建，避免重复标记

  // 设置地图视野（延迟执行，确保所有标记都已添加）
  setTimeout(() => {
    const allOverlays = []
    if (polyline) {
      allOverlays.push(polyline)
    }
    if (markers.length > 0) {
      allOverlays.push(...markers)
    }
    if (allOverlays.length > 0) {
      map.setFitView(allOverlays, false, [50, 50, 50, 50])
    }
  }, 200)
}

// 获取当前位置
const handleGetCurrentLocation = async () => {
  gettingLocation.value = true
  try {
    const location = await getCurrentLocation()
    startLocation.value = {
      longitude: location.longitude,
      latitude: location.latitude,
      address: location.address || ''
    }
    // 更新地图中心
    if (map) {
      map.setCenter([location.longitude, location.latitude])
      // 更新起点标记
      if (map) {
        clearMap()
        createMarker([location.longitude, location.latitude], '起点', 'start')
      }
    }
  } catch (err) {
    console.error('获取当前位置失败:', err)
    alert('获取当前位置失败，请手动输入地址')
  } finally {
    gettingLocation.value = false
  }
}

// 规划路线
import { planMultipleRoute } from '../services/routeService'

const planRoute = async () => {
  planning.value = true
  routeResult.value = null

  try {
    // 第一步：清除地图上的所有标记
    clearMap()
    
    // 第二步：搜索并标注起点
    if (startLocation.value.address) {
      try {
        await searchAndMarkLocation(startLocation.value.address, startLocation, 'start')
      } catch (err) {
        console.warn('起点搜索失败，使用已有坐标:', err)
        if (startLocation.value.longitude && startLocation.value.latitude) {
          createMarker(
            [startLocation.value.longitude, startLocation.value.latitude],
            '起点',
            'start'
          )
        }
      }
    }
    
    // 第三步：标注所有景点（使用景点已有的坐标，不搜索）
    // 因为景点坐标已经在数据库中，直接使用坐标创建标记
    if (map && selectedAttractions.value && selectedAttractions.value.length > 0) {
      selectedAttractions.value.forEach((attraction, index) => {
        if (attraction && (attraction.longitude || attraction.lng) && (attraction.latitude || attraction.lat)) {
          const lng = attraction.longitude ?? attraction.lng
          const lat = attraction.latitude ?? attraction.lat
          
          // 景点标记：显示编号（1, 2, 3...），最后一个景点显示"终"
          const markerType = (index === selectedAttractions.value.length - 1) ? 'end' : (index + 1)
          createMarker([lng, lat], attraction.name || `景点${index + 1}`, markerType)
        } else {
          console.warn(`景点 ${attraction?.name || '未知'} 没有坐标信息`)
        }
      })
    }
    
    // 第五步：构建后端参数
    const params = {
      attractionIds: selectedAttractions.value.map(attr => Number(attr.id)),
      startLocation: {
        longitude: startLocation.value.longitude,
        latitude: startLocation.value.latitude,
        address: startLocation.value.address || ''
      },
      // 不设置终点，后端会自动将最后一个景点作为终点
      endLocation: null,
      transportMode: transportMode.value,
      strategy: strategy.value
    }
    
    // 第六步：调用后端多景点路线规划接口
    const result = await planMultipleRoute(params)

    console.log('路线规划结果:', result)
    console.log('路径数据详情:', JSON.stringify(result.path))
    console.log('路径数组长度:', result.path?.length)

    // 路径点格式：[[lng, lat], ...]
    if (result && result.path && Array.isArray(result.path)) {
      // 验证并转换路径数据（确保坐标是数字类型）
      const validPath = result.path
        .map((point, index) => {
          if (!Array.isArray(point) || point.length < 2) {
            console.warn(`路径点 ${index} 格式无效:`, point)
            return null
          }
          // 转换为数字类型（支持字符串和数字）
          let lng = point[0]
          let lat = point[1]
          
          if (typeof lng === 'string') {
            lng = parseFloat(lng)
          }
          if (typeof lat === 'string') {
            lat = parseFloat(lat)
          }
          
          // 验证坐标有效性
          if (typeof lng === 'number' && typeof lat === 'number' && 
              !isNaN(lng) && !isNaN(lat) && 
              isFinite(lng) && isFinite(lat) &&
              lng >= -180 && lng <= 180 &&
              lat >= -90 && lat <= 90) {
            return [lng, lat]
          }
          console.warn(`路径点 ${index} 坐标无效:`, [lng, lat])
          return null
        })
        .filter(point => point !== null)
      
      console.log('验证后的路径:', validPath)
      console.log('有效路径点数量:', validPath.length)
      
      if (validPath.length < 2) {
        console.error('有效坐标点不足，原始路径:', result.path)
        console.error('选中的景点数量:', selectedAttractions.value.length)
        throw new Error(`后端返回的路径数据无效，有效坐标点不足（${validPath.length}个有效点，需要至少2个）。请确保已选择景点且景点坐标有效。`)
      }
      
      drawRoute(validPath)
      
      // 使用后端返回的总距离和总时间
      routeResult.value = {
        total_distance: result.total_distance || 0,
        total_duration: result.total_duration || 0
      }
      
      console.log('路线规划完成:', {
        pathPoints: validPath.length,
        totalDistance: routeResult.value.total_distance,
        totalDuration: routeResult.value.total_duration
      })
    } else {
      console.error('后端返回数据格式错误:', result)
      throw new Error('后端未返回有效路径')
    }
  } catch (err) {
    console.error('路线规划失败:', err)
    alert('路线规划失败，请稍后重试')
  } finally {
    planning.value = false
  }
}

// 格式化距离
const formatDistance = (meters) => {
  if (!meters) return '0米'
  if (meters < 1000) {
    return `${Math.round(meters)}米`
  }
  return `${(meters / 1000).toFixed(2)}公里`
}

// 格式化时间
const formatDuration = (seconds) => {
  if (!seconds) return '0分钟'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  }
  return `${minutes}分钟`
}

// 监听选中景点变化，更新地图
watch(selectedAttractions, () => {
  if (selectedAttractions.value.length > 0 && map) {
    // 清除旧标记
    clearMap()
    // 重新绘制测试路线（后续会改为实际路线）
    drawTestRoute()
  }
}, { deep: true })

// 生命周期
onMounted(() => {
  // 等待高德地图API加载完成
  if (window.AMap) {
    initMap()
    // 地图初始化后再初始化搜索功能
    setTimeout(() => {
      initSearchFunctions()
    }, 500)
  } else {
    // 如果API还未加载，等待加载完成
    const checkAMap = setInterval(() => {
      if (window.AMap) {
        clearInterval(checkAMap)
        initMap()
        // 地图初始化后再初始化搜索功能
        setTimeout(() => {
          initSearchFunctions()
        }, 500)
      }
    }, 100)
    
    // 10秒后超时
    setTimeout(() => {
      clearInterval(checkAMap)
      if (!window.AMap) {
        console.error('高德地图API加载超时')
      }
    }, 10000)
  }
})

onUnmounted(() => {
  clearMap()
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<style scoped>
.route-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--spacing-6);
}

/* 页面标题 */
.page-header {
  margin-bottom: var(--spacing-6);
  text-align: center;
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: var(--spacing-2);
}

.page-subtitle {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
}

/* 内容区域 */
.route-content {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: var(--spacing-6);
  height: calc(100vh - 200px);
  min-height: 600px;
}

/* 左侧：已选景点 */
.selected-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
  overflow-y: auto;
}

.section-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: var(--spacing-5);
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-4);
}

.card-title {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text-primary);
}

.attraction-count {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  background: #f5f5f5;
  padding: var(--spacing-1) var(--spacing-3);
  border-radius: var(--radius-sm);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: var(--spacing-8) var(--spacing-4);
}

.empty-text {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-2);
}

.empty-hint {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 景点列表 */
.attraction-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-4);
}

.attraction-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-3);
  padding: var(--spacing-3);
  background: #f9f9f9;
  border-radius: var(--radius-md);
  border-left: 3px solid var(--color-primary);
}

.item-order {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  color: white;
  border-radius: 50%;
  font-weight: 600;
  font-size: var(--font-size-sm);
  flex-shrink: 0;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-address {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-category {
  display: inline-block;
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  background: rgba(198, 40, 40, 0.1);
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--radius-sm);
}

.btn-remove {
  width: 28px;
  height: 28px;
  border: none;
  background: #f5f5f5;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xl);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-remove:hover {
  background: var(--color-error);
  color: white;
}

/* 卡片操作 */
.card-actions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
  margin-top: var(--spacing-4);
  padding-top: var(--spacing-4);
  border-top: 1px solid #e0e0e0;
}

/* 表单组 */
.form-group {
  margin-bottom: var(--spacing-5);
}

.form-label {
  display: block;
  margin-bottom: var(--spacing-2);
  font-weight: 500;
  color: var(--color-text-primary);
}

.input-group {
  display: flex;
  gap: var(--spacing-2);
}

.form-input {
  flex: 1;
  padding: var(--spacing-3) var(--spacing-4);
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  transition: border-color var(--transition-fast);
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 交通方式 */
.transport-options {
  display: flex;
  gap: var(--spacing-2);
}

.transport-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-1);
  padding: var(--spacing-3);
  border: 2px solid #e0e0e0;
  background: white;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.transport-btn:hover {
  border-color: var(--color-primary);
}

.transport-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.transport-icon {
  font-size: var(--font-size-xl);
}

.transport-text {
  font-size: var(--font-size-xs);
  font-weight: 500;
}

/* 规划策略 */
.strategy-options {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.strategy-option {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding: var(--spacing-3);
  border: 2px solid #e0e0e0;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.strategy-option:hover {
  border-color: var(--color-primary);
}

.radio-input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.strategy-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-1);
}

.strategy-title {
  font-weight: 600;
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
}

.strategy-desc {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* 按钮 */
.btn {
  padding: var(--spacing-3) var(--spacing-5);
  border-radius: var(--radius-md);
  font-weight: 500;
  font-size: var(--font-size-base);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: none;
}

.btn-primary {
  background: var(--color-primary);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-dark);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-outline {
  background: white;
  border: 2px solid var(--color-primary);
  color: var(--color-primary);
}

.btn-outline:hover {
  background: var(--color-primary);
  color: white;
}

.btn-sm {
  padding: var(--spacing-2) var(--spacing-4);
  font-size: var(--font-size-sm);
}

/* 右侧：地图 */
.map-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.map-container {
  flex: 1;
  min-height: 0;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

/* 路线信息 */
.route-info {
  display: flex;
  gap: var(--spacing-4);
  padding: var(--spacing-4);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-1);
  flex: 1;
}

.info-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.info-value {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-primary);
}

/* 自定义标记样式 */
.custom-content-marker {
  position: relative;
  width: 25px;
  height: 34px;
}

.custom-content-marker img {
  width: 100%;
  height: 100%;
}

.custom-content-marker .marker-label {
  position: absolute;
  top: 2px;
  left: 50%;
  transform: translateX(-50%);
  color: white;
  font-size: 12px;
  font-weight: bold;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .route-content {
    grid-template-columns: 1fr;
    height: auto;
  }

  .selected-section {
    max-height: 400px;
  }

  .map-container {
    height: 500px;
  }
}
</style>
