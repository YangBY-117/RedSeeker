<template>
  <div class="user-center-view">
    <div class="user-header">
      <h1 class="page-title">用户中心</h1>
      <div class="user-info-card">
        <div class="user-avatar-large">{{ getUserInitial() }}</div>
        <div class="user-details">
          <h2 class="user-name">{{ user?.username || '游客' }}</h2>
          <p class="user-meta">ID: {{ user?.id || '-' }}</p>
        </div>
      </div>
    </div>

    <!-- 标签页 -->
    <div class="tabs">
      <button
        :class="['tab-btn', { active: activeTab === 'diaries' }]"
        @click="activeTab = 'diaries'"
      >
        📝 我的日记
      </button>
      <button
        :class="['tab-btn', { active: activeTab === 'routes' }]"
        @click="activeTab = 'routes'"
      >
        🗺️ 历史路线
      </button>
    </div>

    <!-- 我的日记 -->
    <div v-if="activeTab === 'diaries'" class="tab-content">
      <div class="content-header">
        <h2 class="section-title">我的红色旅游日记</h2>
        <button class="btn btn-primary" @click="goToCreateDiary">
          ✏️ 撰写新日记
        </button>
      </div>

      <div v-if="diariesLoading" class="loading-state">加载中...</div>
      <div v-else-if="myDiaries.length === 0" class="empty-state">
        <p>您还没有撰写过日记</p>
        <button class="btn btn-primary" @click="goToCreateDiary">
          撰写第一篇日记
        </button>
      </div>
      <div v-else class="diaries-grid">
        <DiaryCard
          v-for="diary in myDiaries"
          :key="diary.id"
          :diary="diary"
          @click="viewDiary(diary.id)"
          @deleted="handleDiaryDeleted"
        />
      </div>

      <!-- 分页 -->
      <div v-if="myDiaries.length > 0" class="pagination">
        <button
          :disabled="diaryPage === 1"
          @click="goToDiaryPage(diaryPage - 1)"
          class="page-btn"
        >
          上一页
        </button>
        <div class="page-info">
          <span>第 {{ diaryPage }} / {{ diaryTotalPages }} 页</span>
        </div>
        <button
          :disabled="diaryPage === diaryTotalPages"
          @click="goToDiaryPage(diaryPage + 1)"
          class="page-btn"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 历史路线 -->
    <div v-if="activeTab === 'routes'" class="tab-content">
      <div class="content-header">
        <h2 class="section-title">历史路线规划</h2>
      </div>

      <div v-if="routesLoading" class="loading-state">加载中...</div>
      <div v-else-if="historyRoutes.length === 0" class="empty-state">
        <p>您还没有规划过路线</p>
        <button class="btn btn-primary" @click="goToRoutePlanning">
          开始规划路线
        </button>
      </div>
      <div v-else class="routes-list">
        <div
          v-for="route in historyRoutes"
          :key="route.id"
          class="route-card"
          @click="viewRoute(route.id)"
        >
          <div class="route-header">
            <h3 class="route-title">{{ route.name || `路线 ${route.id}` }}</h3>
            <span class="route-date">{{ formatDate(route.created_at) }}</span>
          </div>
          <div class="route-info">
            <div class="info-item">
              <span class="info-icon">📍</span>
              <span class="info-text">{{ route.attraction_count || 0 }} 个景点</span>
            </div>
            <div class="info-item">
              <span class="info-icon">📏</span>
              <span class="info-text">{{ formatDistance(route.total_distance) }}</span>
            </div>
            <div class="info-item">
              <span class="info-icon">⏱️</span>
              <span class="info-text">{{ formatDuration(route.total_duration) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { getDiaryList } from '../services/diaryService'
import DiaryCard from '../components/DiaryCard.vue'

const router = useRouter()
const { user, isAuthenticated } = useAuth()

const activeTab = ref('diaries')

// 日记相关
const myDiaries = ref([])
const diariesLoading = ref(false)
const diaryPage = ref(1)
const diaryPageSize = ref(12)
const diaryTotalPages = ref(1)

// 路线相关
const historyRoutes = ref([])
const routesLoading = ref(false)

// 获取用户首字母
const getUserInitial = () => {
  if (!user.value || !user.value.username) return '👤'
  return user.value.username.charAt(0).toUpperCase()
}

// 加载我的日记
const loadMyDiaries = async (page = 1) => {
  if (!isAuthenticated.value || !user.value) return

  diariesLoading.value = true
  try {
    // 调用后端接口获取当前用户的日记
    // 注意：后端需要支持按 userId 筛选
    const response = await getDiaryList({
      userId: user.value.id,
      page,
      pageSize: diaryPageSize.value,
      sortBy: 'time'
    })

    myDiaries.value = response.diaries || []
    diaryTotalPages.value = response.totalPages || 1
    diaryPage.value = response.page || page
  } catch (error) {
    console.error('加载我的日记失败:', error)
    myDiaries.value = []
  } finally {
    diariesLoading.value = false
  }
}

// 加载历史路线
const loadHistoryRoutes = async () => {
  if (!isAuthenticated.value || !user.value) return

  routesLoading.value = true
  try {
    // TODO: 调用后端接口获取历史路线
    // const response = await api.get(`/route/history?userId=${user.value.id}`)
    // historyRoutes.value = response.data.data || []
    
    // 暂时使用空数组，等待后端接口实现
    historyRoutes.value = []
  } catch (error) {
    console.error('加载历史路线失败:', error)
    historyRoutes.value = []
  } finally {
    routesLoading.value = false
  }
}

// 跳转到创建日记
const goToCreateDiary = () => {
  router.push('/diary')
  // 触发创建日记弹窗（需要在 DiaryView 中处理）
  setTimeout(() => {
    window.dispatchEvent(new CustomEvent('open-create-diary'))
  }, 100)
}

// 跳转到路线规划
const goToRoutePlanning = () => {
  router.push('/route')
}

// 查看日记详情
const viewDiary = (id) => {
  router.push(`/diary/${id}`)
}

// 查看路线详情
const viewRoute = (id) => {
  router.push(`/route?routeId=${id}`)
}

// 分页
const goToDiaryPage = (page) => {
  if (page >= 1 && page <= diaryTotalPages.value) {
    loadMyDiaries(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const handleDiaryDeleted = (deletedId) => {
  myDiaries.value = myDiaries.value.filter(diary => diary.id !== deletedId)
  if (myDiaries.value.length === 0 && diaryPage.value > 1) {
    goToDiaryPage(diaryPage.value - 1)
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '未知'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

// 格式化距离
const formatDistance = (meters) => {
  if (!meters) return '0 米'
  if (meters < 1000) return `${meters} 米`
  return `${(meters / 1000).toFixed(1)} 公里`
}

// 格式化时长
const formatDuration = (seconds) => {
  if (!seconds) return '0 分钟'
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  if (hours > 0) {
    return `${hours} 小时 ${minutes % 60} 分钟`
  }
  return `${minutes} 分钟`
}

onMounted(() => {
  if (isAuthenticated.value && user.value) {
    loadMyDiaries()
    loadHistoryRoutes()
  } else {
    router.push('/')
  }
})
</script>

<style scoped>
.user-center-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-6);
  min-height: 100vh;
}

.user-header {
  margin-bottom: var(--spacing-6);
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-primary);
  margin: 0 0 var(--spacing-4) 0;
}

.user-info-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  background: white;
  padding: var(--spacing-5);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.user-avatar-large {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: var(--font-size-3xl);
  font-weight: bold;
}

.user-details {
  flex: 1;
}

.user-name {
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--spacing-1) 0;
}

.user-meta {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

/* 标签页 */
.tabs {
  display: flex;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-6);
  border-bottom: 2px solid var(--color-border);
}

.tab-btn {
  padding: var(--spacing-3) var(--spacing-5);
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: -2px;
}

.tab-btn:hover {
  color: var(--color-primary);
}

.tab-btn.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

/* 内容区域 */
.tab-content {
  background: white;
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-md);
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-5);
}

.section-title {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: var(--spacing-8);
  color: var(--color-text-secondary);
}

.empty-state button {
  margin-top: var(--spacing-3);
}

.diaries-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-4);
}

/* 路线列表 */
.routes-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.route-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  cursor: pointer;
  transition: all 0.2s;
}

.route-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.route-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-3);
}

.route-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.route-date {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.route-info {
  display: flex;
  gap: var(--spacing-4);
  flex-wrap: wrap;
}

.info-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.info-icon {
  font-size: var(--font-size-base);
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-4);
  margin-top: var(--spacing-6);
  padding-top: var(--spacing-4);
  border-top: 1px solid var(--color-border);
}

.page-btn {
  padding: var(--spacing-2) var(--spacing-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 按钮样式 */
.btn {
  padding: var(--spacing-3) var(--spacing-5);
  border-radius: var(--radius-md);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  font-size: var(--font-size-base);
}

.btn-primary {
  background: linear-gradient(to right, #dc2626, #b91c1c);
  color: white;
}

.btn-primary:hover {
  background: linear-gradient(to right, #b91c1c, #991b1b);
  transform: translateY(-1px);
}

/* 响应式 */
@media (max-width: 768px) {
  .user-center-view {
    padding: var(--spacing-4);
  }

  .user-info-card {
    flex-direction: column;
    text-align: center;
  }

  .content-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-3);
  }

  .diaries-grid {
    grid-template-columns: 1fr;
  }
}
</style>
