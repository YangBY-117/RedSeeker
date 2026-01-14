<template>
  <div class="places-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">场所查询</h1>
      <p class="page-subtitle">查找附近的超市、卫生间、餐厅等设施</p>
    </div>

    <div class="places-content">
      <!-- 左侧：搜索和筛选 -->
      <div class="search-section">
        <div class="section-card">
          <h2 class="card-title">搜索设置</h2>

          <!-- 位置选择 -->
          <div class="form-group">
            <label class="form-label">搜索位置</label>
            <div class="location-selector">
              <button
                :class="['location-btn', { active: locationMode === 'current' }]"
                @click="handleUseCurrentLocation"
                :disabled="gettingLocation"
              >
                <span class="btn-icon">📍</span>
                <span class="btn-text">{{ gettingLocation ? '获取中...' : '当前位置' }}</span>
              </button>
              <button
                :class="['location-btn', { active: locationMode === 'attraction' }]"
                @click="showAttractionSelector = true"
              >
                <span class="btn-icon">🏛️</span>
                <span class="btn-text">选择景点</span>
              </button>
            </div>
            <div v-if="selectedLocation.address" class="location-info">
              <p class="location-text">{{ selectedLocation.address }}</p>
            </div>
          </div>

          <!-- 景点选择器 -->
          <div v-if="showAttractionSelector" class="attraction-selector">
            <label class="form-label">选择景点</label>
            <select
              v-model="selectedAttractionId"
              @change="handleAttractionChange"
              class="form-select"
            >
              <option value="">请选择景点</option>
              <option
                v-for="attraction in attractionsList"
                :key="attraction.id"
                :value="attraction.id"
              >
                {{ attraction.name }}
              </option>
            </select>
          </div>

          <!-- 搜索关键词 -->
          <div class="form-group">
            <label class="form-label">搜索关键词</label>
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="如：超市、卫生间、餐厅"
              class="form-input"
              @keyup.enter="handleSearch"
            />
          </div>

          <!-- 类别筛选 -->
          <div class="form-group">
            <label class="form-label">设施类别</label>
            <div class="category-grid">
              <button
                v-for="type in placeTypes"
                :key="type.value"
                :class="['category-btn', { active: selectedType === type.value }]"
                @click="selectedType = type.value"
              >
                <span class="category-icon">{{ type.icon }}</span>
                <span class="category-label">{{ type.label }}</span>
              </button>
            </div>
          </div>

          <!-- 搜索半径 -->
          <div class="form-group">
            <label class="form-label">搜索半径</label>
            <div class="radius-selector">
              <button
                v-for="radius in radiusOptions"
                :key="radius"
                :class="['radius-btn', { active: searchRadius === radius }]"
                @click="searchRadius = radius"
              >
                {{ radius / 1000 }}km
              </button>
            </div>
          </div>

          <!-- 搜索按钮 -->
          <button
            class="btn btn-primary btn-search"
            :disabled="!canSearch || searching"
            @click="handleSearch"
          >
            {{ searching ? '搜索中...' : '开始搜索' }}
          </button>

          <!-- 实际距离排序 -->
          <div v-if="places.length > 0" class="form-group">
            <label class="form-label">排序方式</label>
            <div class="sort-options">
              <button
                :class="['sort-btn', { active: sortBy === 'straight' }]"
                @click="sortBy = 'straight'"
              >
                直线距离
              </button>
              <button
                :class="['sort-btn', { active: sortBy === 'real' }]"
                @click="handleRealDistanceSort"
                :disabled="sortingRealDistance"
              >
                {{ sortingRealDistance ? '计算中...' : '实际距离' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：地图和结果 -->
      <div class="results-section">
        <!-- 地图容器 -->
        <div class="map-container">
          <div ref="mapContainer" class="map"></div>
        </div>

        <!-- 结果列表 -->
        <div class="results-list">
          <div class="results-header">
            <h3 class="results-title">搜索结果</h3>
            <span class="results-count">{{ places.length }} 个结果</span>
          </div>

          <div v-if="searching" class="loading-state">
            <p>搜索中...</p>
          </div>

          <div v-else-if="places.length === 0 && hasSearched" class="empty-state">
            <p>未找到相关场所</p>
          </div>

          <div v-else-if="places.length === 0" class="empty-state">
            <p>请设置搜索位置并输入关键词开始搜索</p>
          </div>

          <div v-else class="places-list">
            <div
              v-for="(place, index) in places"
              :key="place.id"
              :class="['place-item', { active: selectedPlaceIndex === index }]"
              @click="selectPlace(index)"
            >
              <div class="place-number">{{ index + 1 }}</div>
              <div class="place-content">
                <h4 class="place-name">{{ place.name }}</h4>
                <p class="place-address">{{ place.address || '地址未知' }}</p>
                <div class="place-meta">
                  <span class="place-distance">
                    {{ formatDistance(place.realDistance || place.distance) }}
                  </span>
                  <span v-if="place.tel" class="place-tel">📞 {{ place.tel }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { searchNearbyPlaces, getRealDistanceAndSort, getCommonPlaceTypes, getTypeByKeyword } from '../services/placeService'
import { getCurrentLocation } from '../services/routeService'
import { getRecommendations } from '../services/recommendService.js'

// 地图相关
const mapContainer = ref(null)
let map = null
let markers = []

// 位置相关
const locationMode = ref('current') // 'current' | 'attraction'
const selectedLocation = ref({
  longitude: null,
  latitude: null,
  address: ''
})
const gettingLocation = ref(false)
const showAttractionSelector = ref(false)
const selectedAttractionId = ref('')
const attractionsList = ref([])

// 搜索相关
const searchKeyword = ref('')
const selectedType = ref('')
const searchRadius = ref(3000) // 默认3km
const radiusOptions = [1000, 2000, 3000, 5000, 10000] // 1km, 2km, 3km, 5km, 10km
const searching = ref(false)
const hasSearched = ref(false)

// 结果相关
const places = ref([])
const selectedPlaceIndex = ref(-1)
const sortBy = ref('straight') // 'straight' | 'real'
const sortingRealDistance = ref(false)

// 设施类别
const placeTypes = getCommonPlaceTypes()

// 计算属性
const canSearch = computed(() => {
  return selectedLocation.value.longitude && selectedLocation.value.latitude && 
         (searchKeyword.value.trim() || selectedType.value)
})

// 初始化地图
const initMap = () => {
  if (!window.AMap || !mapContainer.value) {
    console.error('高德地图API未加载或容器未找到')
    return
  }

  // 默认中心点（上海）
  const center = selectedLocation.value.longitude && selectedLocation.value.latitude
    ? [selectedLocation.value.longitude, selectedLocation.value.latitude]
    : [121.4737, 31.2208]

  map = new AMap.Map(mapContainer.value, {
    zoom: 15,
    center: center,
    viewMode: '3D'
  })
}

// 清除地图标记
const clearMarkers = () => {
  if (markers.length > 0) {
    markers.forEach(marker => {
      marker.setMap(null)
    })
    markers = []
  }
}

// 在地图上显示场所
const showPlacesOnMap = () => {
  if (!map) return

  clearMarkers()

  // 添加中心点标记（搜索位置）
  if (selectedLocation.value.longitude && selectedLocation.value.latitude) {
    const centerMarker = new AMap.Marker({
      position: [selectedLocation.value.longitude, selectedLocation.value.latitude],
      title: '搜索位置',
      icon: new AMap.Icon({
        size: new AMap.Size(32, 32),
        image: 'https://webapi.amap.com/theme/v1.3/markers/n/start.png',
        imageSize: new AMap.Size(32, 32)
      })
    })
    centerMarker.setMap(map)
    markers.push(centerMarker)
  }

  // 添加场所标记
  places.value.forEach((place, index) => {
    const marker = new AMap.Marker({
      position: [place.location.longitude, place.location.latitude],
      title: `${index + 1}. ${place.name}`,
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

  // 调整地图视野
  if (markers.length > 0) {
    map.setFitView(markers, false, [50, 50, 50, 50])
  }
}

// 获取当前位置
const handleUseCurrentLocation = async () => {
  gettingLocation.value = true
  locationMode.value = 'current'
  showAttractionSelector.value = false
  selectedAttractionId.value = ''

  try {
    const location = await getCurrentLocation()
    selectedLocation.value = {
      longitude: location.longitude,
      latitude: location.latitude,
      address: location.address || `经度: ${location.longitude}, 纬度: ${location.latitude}`
    }

    // 更新地图中心
    if (map) {
      map.setCenter([location.longitude, location.latitude])
    }
  } catch (error) {
    console.error('获取当前位置失败:', error)
    alert('获取当前位置失败：' + error.message)
  } finally {
    gettingLocation.value = false
  }
}

// 加载景点列表
const loadAttractions = async () => {
  try {
    const result = await getRecommendations({
      page: 1,
      pageSize: 100
    })
    attractionsList.value = result.data.attractions || []
  } catch (error) {
    console.error('加载景点列表失败:', error)
  }
}

// 景点选择变化
const handleAttractionChange = () => {
  if (!selectedAttractionId.value) {
    selectedLocation.value = { longitude: null, latitude: null, address: '' }
    return
  }

  const attraction = attractionsList.value.find(a => a.id === parseInt(selectedAttractionId.value))
  if (attraction) {
    locationMode.value = 'attraction'
    selectedLocation.value = {
      longitude: attraction.longitude,
      latitude: attraction.latitude,
      address: `${attraction.name} - ${attraction.address}`
    }

    // 更新地图中心
    if (map) {
      map.setCenter([attraction.longitude, attraction.latitude])
    }
  }
}

// 搜索场所
const handleSearch = async () => {
  if (!canSearch.value) return

  searching.value = true
  hasSearched.value = true

  try {
    // 根据关键词获取类型代码
    let types = selectedType.value
    if (!types && searchKeyword.value.trim()) {
      types = getTypeByKeyword(searchKeyword.value.trim())
    }

    const result = await searchNearbyPlaces({
      longitude: selectedLocation.value.longitude,
      latitude: selectedLocation.value.latitude,
      keywords: searchKeyword.value.trim(),
      types: types,
      radius: searchRadius.value,
      page: 1,
      pageSize: 50
    })

    places.value = result.data.places || []
    selectedPlaceIndex.value = -1

    // 在地图上显示
    showPlacesOnMap()

    // 按直线距离排序
    sortBy.value = 'straight'
    places.value.sort((a, b) => a.distance - b.distance)
  } catch (error) {
    console.error('搜索失败:', error)
    alert('搜索失败：' + error.message)
    places.value = []
  } finally {
    searching.value = false
  }
}

// 实际距离排序
const handleRealDistanceSort = async () => {
  if (places.value.length === 0) return

  sortingRealDistance.value = true
  sortBy.value = 'real'

  try {
    const sortedPlaces = await getRealDistanceAndSort({
      longitude: selectedLocation.value.longitude,
      latitude: selectedLocation.value.latitude,
      places: places.value,
      transport_mode: 'walking'
    })

    places.value = sortedPlaces
    showPlacesOnMap()
  } catch (error) {
    console.error('计算实际距离失败:', error)
    alert('计算实际距离失败，请稍后重试')
    sortBy.value = 'straight'
  } finally {
    sortingRealDistance.value = false
  }
}

// 选择场所
const selectPlace = (index) => {
  selectedPlaceIndex.value = index
  const place = places.value[index]
  if (map && place) {
    map.setCenter([place.location.longitude, place.location.latitude])
    map.setZoom(16)
  }
}

// 格式化距离
const formatDistance = (distance) => {
  if (!distance) return '距离未知'
  if (distance < 1000) {
    return `${distance}米`
  } else {
    return `${(distance / 1000).toFixed(1)}公里`
  }
}

// 监听排序方式变化
watch(sortBy, (newVal) => {
  if (newVal === 'straight' && places.value.length > 0) {
    places.value.sort((a, b) => (a.realDistance || a.distance) - (b.realDistance || b.distance))
  }
})

// 生命周期
onMounted(async () => {
  // 加载景点列表
  await loadAttractions()

  // 初始化地图
  if (window.AMap) {
    initMap()
  } else {
    const checkAMap = setInterval(() => {
      if (window.AMap) {
        clearInterval(checkAMap)
        initMap()
      }
    }, 100)

    setTimeout(() => {
      clearInterval(checkAMap)
      if (!window.AMap) {
        console.error('高德地图API加载超时')
      }
    }, 10000)
  }

  // 默认获取当前位置
  handleUseCurrentLocation()
})

onUnmounted(() => {
  clearMarkers()
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<style scoped>
.places-view {
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
.places-content {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: var(--spacing-6);
  height: calc(100vh - 200px);
  min-height: 600px;
}

/* 左侧搜索区域 */
.search-section {
  overflow-y: auto;
}

.section-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: var(--spacing-5);
  box-shadow: var(--shadow-md);
}

.card-title {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--spacing-4);
}

.form-group {
  margin-bottom: var(--spacing-5);
}

.form-label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: var(--spacing-2);
}

.form-input,
.form-select {
  width: 100%;
  padding: var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  transition: border-color 0.2s;
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 位置选择 */
.location-selector {
  display: flex;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-2);
}

.location-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2);
  padding: var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: pointer;
  transition: all 0.2s;
}

.location-btn:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.location-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.location-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.location-info {
  margin-top: var(--spacing-2);
  padding: var(--spacing-2);
  background: var(--color-bg);
  border-radius: var(--radius-md);
}

.location-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 类别网格 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-2);
}

.category-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-2) var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: pointer;
  transition: all 0.2s;
  font-size: var(--font-size-sm);
}

.category-btn:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.category-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.category-icon {
  font-size: var(--font-size-lg);
}

/* 半径选择 */
.radius-selector {
  display: flex;
  gap: var(--spacing-2);
  flex-wrap: wrap;
}

.radius-btn {
  padding: var(--spacing-2) var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: pointer;
  transition: all 0.2s;
  font-size: var(--font-size-sm);
}

.radius-btn:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.radius-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
}

/* 搜索按钮 */
.btn-search {
  width: 100%;
  margin-top: var(--spacing-4);
  padding: var(--spacing-3) var(--spacing-4);
  font-size: var(--font-size-base);
  font-weight: 600;
}

/* 排序选项 */
.sort-options {
  display: flex;
  gap: var(--spacing-2);
}

.sort-btn {
  flex: 1;
  padding: var(--spacing-2) var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: pointer;
  transition: all 0.2s;
  font-size: var(--font-size-sm);
}

.sort-btn:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.sort-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.sort-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 右侧结果区域 */
.results-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.map-container {
  flex: 1;
  min-height: 300px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.map {
  width: 100%;
  height: 100%;
}

/* 结果列表 */
.results-list {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: var(--spacing-4);
  box-shadow: var(--shadow-md);
  max-height: 300px;
  overflow-y: auto;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-4);
  padding-bottom: var(--spacing-2);
  border-bottom: 1px solid var(--color-border);
}

.results-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
}

.results-count {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.loading-state,
.empty-state {
  text-align: center;
  padding: var(--spacing-6);
  color: var(--color-text-secondary);
}

.places-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.place-item {
  display: flex;
  gap: var(--spacing-3);
  padding: var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.place-item:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.place-item.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.place-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary);
  color: white;
  font-weight: 600;
  font-size: var(--font-size-sm);
  flex-shrink: 0;
}

.place-content {
  flex: 1;
  min-width: 0;
}

.place-name {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--spacing-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.place-address {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.place-meta {
  display: flex;
  gap: var(--spacing-3);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.place-distance {
  font-weight: 500;
  color: var(--color-primary);
}

.place-tel {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 响应式 */
@media (max-width: 1024px) {
  .places-content {
    grid-template-columns: 1fr;
    height: auto;
  }

  .search-section {
    max-height: 500px;
  }

  .map-container {
    min-height: 400px;
  }  .results-list {
    max-height: 400px;
  }
}
</style>
