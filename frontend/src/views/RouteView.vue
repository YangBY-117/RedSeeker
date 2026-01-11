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
                v-model="startLocation.address"
                type="text"
                placeholder="请输入起点地址"
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
            <label class="form-label">交通方式</label>
            <div class="transport-options">
              <button
                v-for="mode in transportModes"
                :key="mode.value"
                :class="['transport-btn', { active: transportMode === mode.value }]"
                @click="transportMode = mode.value"
              >
                <span class="transport-icon">{{ mode.icon }}</span>
                <span class="transport-text">{{ mode.label }}</span>
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
  longitude: 121.4737, // 默认上海坐标（测试用）
  latitude: 31.2208,
  address: '上海市黄浦区'
})
const transportMode = ref('driving')
const strategy = ref('history_first')
const gettingLocation = ref(false)
const planning = ref(false)
const routeResult = ref(null)

// 交通方式选项
const transportModes = [
  { value: 'driving', label: '驾车', icon: '🚗' },
  { value: 'walking', label: '步行', icon: '🚶' },
  { value: 'transit', label: '公交', icon: '🚌' }
]

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

  // 创建地图实例
  map = new AMap.Map(mapContainer.value, {
    zoom: 13,
    center: [startLocation.value.longitude, startLocation.value.latitude],
    viewMode: '3D'
  })

  // 添加起点标记
  addStartMarker()

  // 绘制测试路线（硬编码）
  drawTestRoute()
}

// 添加起点标记
const addStartMarker = () => {
  if (!map) return

  const marker = new AMap.Marker({
    position: [startLocation.value.longitude, startLocation.value.latitude],
    title: '起点',
    icon: new AMap.Icon({
      size: new AMap.Size(32, 32),
      image: 'https://webapi.amap.com/theme/v1.3/markers/n/start.png',
      imageSize: new AMap.Size(32, 32)
    })
  })
  marker.setMap(map)
  markers.push(marker)
}

// 绘制测试路线（硬编码）
const drawTestRoute = () => {
  if (!map) return

  // 测试路线坐标点（从上海到嘉兴的路线）
  const testPath = [
    [121.4737, 31.2208], // 起点：上海
    [121.4800, 31.2300],
    [121.4900, 31.2400],
    [121.5000, 31.2500],
    [120.7575, 30.7536]  // 终点：嘉兴南湖
  ]

  // 创建折线
  polyline = new AMap.Polyline({
    path: testPath,
    isOutline: true,
    outlineColor: '#ffeeff',
    borderWeight: 3,
    strokeColor: '#3366FF',
    strokeOpacity: 1,
    strokeWeight: 6,
    strokeStyle: 'solid',
    lineJoin: 'round',
    lineCap: 'round',
    zIndex: 50
  })

  polyline.setMap(map)

  // 添加终点标记
  const endMarker = new AMap.Marker({
    position: testPath[testPath.length - 1],
    title: '终点：南湖革命纪念馆',
    icon: new AMap.Icon({
      size: new AMap.Size(32, 32),
      image: 'https://webapi.amap.com/theme/v1.3/markers/n/end.png',
      imageSize: new AMap.Size(32, 32)
    })
  })
  endMarker.setMap(map)
  markers.push(endMarker)

  // 设置地图视野
  map.setFitView([polyline], false, [50, 50, 50, 50])

  // 设置测试数据
  routeResult.value = {
    total_distance: 125000, // 125公里
    total_duration: 7200 // 2小时
  }
}

// 清除地图上的标记和路线
const clearMap = () => {
  if (markers.length > 0) {
    markers.forEach(marker => {
      marker.setMap(null)
    })
    markers = []
  }
  if (polyline) {
    polyline.setMap(null)
    polyline = null
  }
}

// 绘制路线
const drawRoute = (path) => {
  clearMap()

  if (!map || !path || path.length < 2) return

  // 绘制路线
  polyline = new AMap.Polyline({
    path: path,
    isOutline: true,
    outlineColor: '#ffeeff',
    borderWeight: 3,
    strokeColor: '#3366FF',
    strokeOpacity: 1,
    strokeWeight: 6,
    strokeStyle: 'solid',
    lineJoin: 'round',
    lineCap: 'round',
    zIndex: 50
  })

  polyline.setMap(map)

  // 添加起点标记
  const startMarker = new AMap.Marker({
    position: path[0],
    title: '起点',
    icon: new AMap.Icon({
      size: new AMap.Size(32, 32),
      image: 'https://webapi.amap.com/theme/v1.3/markers/n/start.png',
      imageSize: new AMap.Size(32, 32)
    })
  })
  startMarker.setMap(map)
  markers.push(startMarker)

  // 添加景点标记
  selectedAttractions.value.forEach((attraction, index) => {
    const marker = new AMap.Marker({
      position: [attraction.longitude, attraction.latitude],
      title: `${index + 1}. ${attraction.name}`,
      icon: new AMap.Icon({
        size: new AMap.Size(28, 28),
        image: 'https://webapi.amap.com/theme/v1.3/markers/n/mid.png',
        imageSize: new AMap.Size(28, 28)
      }),
      label: {
        content: `${index + 1}`,
        direction: 'right',
        offset: new AMap.Pixel(10, 0)
      }
    })
    marker.setMap(map)
    markers.push(marker)
  })

  // 添加终点标记
  const endMarker = new AMap.Marker({
    position: path[path.length - 1],
    title: '终点',
    icon: new AMap.Icon({
      size: new AMap.Size(32, 32),
      image: 'https://webapi.amap.com/theme/v1.3/markers/n/end.png',
      imageSize: new AMap.Size(32, 32)
    })
  })
  endMarker.setMap(map)
  markers.push(endMarker)

  // 设置地图视野
  map.setFitView([polyline, ...markers], false, [50, 50, 50, 50])
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
      clearMap()
      addStartMarker()
      drawTestRoute()
    }
  } catch (err) {
    console.error('获取当前位置失败:', err)
    alert('获取当前位置失败，请手动输入地址')
  } finally {
    gettingLocation.value = false
  }
}

// 规划路线
const planRoute = async () => {
  planning.value = true
  routeResult.value = null

  try {
    // TODO: 调用后端API规划路线
    // 目前先使用测试数据
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 构建路线路径（测试用）
    const path = [
      [startLocation.value.longitude, startLocation.value.latitude],
      ...selectedAttractions.value.map(attr => [attr.longitude, attr.latitude])
    ]

    // 绘制路线
    drawRoute(path)

    // 设置测试结果
    routeResult.value = {
      total_distance: 125000,
      total_duration: 7200
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
  } else {
    // 如果API还未加载，等待加载完成
    const checkAMap = setInterval(() => {
      if (window.AMap) {
        clearInterval(checkAMap)
        initMap()
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
