<template>
  <div class="diary-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">红色旅游日记</h1>
      <p class="page-subtitle">记录您的红色之旅，传承革命精神</p>
      <button class="btn btn-primary btn-create" @click="handleCreateDiary">
        ✏️ 撰写红色日记
      </button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="search-toolbar">
      <div class="search-section">
        <div class="search-tabs">
          <button
            :class="['search-tab', { active: searchMode === 'destination' }]"
            @click="searchMode = 'destination'"
          >
            按目的地
          </button>
          <button
            :class="['search-tab', { active: searchMode === 'name' }]"
            @click="searchMode = 'name'"
          >
            按名称
          </button>
          <button
            :class="['search-tab', { active: searchMode === 'fulltext' }]"
            @click="searchMode = 'fulltext'"
          >
            全文检索
          </button>
        </div>
        <div class="search-input-group">
          <input
            v-model="searchKeyword"
            type="text"
            :placeholder="getSearchPlaceholder()"
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <button class="btn btn-primary btn-search" @click="handleSearch">搜索</button>
        </div>
      </div>

      <div class="sort-section">
        <label class="sort-label">排序：</label>
        <select v-model="sortBy" @change="handleSortChange" class="sort-select">
          <option value="heat">按热度</option>
          <option value="rating">按评分</option>
          <option value="time">按时间</option>
        </select>
      </div>
    </div>

    <!-- 日记列表 -->
    <div class="diaries-section">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="diaries.length === 0 && hasSearched" class="empty-state">
        <p>未找到相关红色旅游日记</p>
        <button v-if="isAuthenticated" class="btn btn-outline" @click="showCreateModal = true">
          撰写一篇新的红色日记
        </button>
      </div>
      <div v-else-if="diaries.length === 0" class="empty-state">
        <p>暂无红色旅游日记，快来记录您的红色之旅吧！</p>
        <button v-if="isAuthenticated" class="btn btn-primary" @click="showCreateModal = true">
          撰写红色日记
        </button>
      </div>
      <div v-else class="diaries-grid">
        <DiaryCard
          v-for="diary in diaries"
          :key="diary.id"
          :diary="diary"
          @click="viewDiary(diary.id)"
          @deleted="handleDiaryDeleted"
        />
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="!loading && diaries.length > 0" class="pagination">
      <button
        :disabled="currentPage === 1"
        @click="goToPage(currentPage - 1)"
        class="page-btn"
      >
        上一页
      </button>
      <div class="page-info">
        <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
        <span class="total-info">共 {{ total }} 篇红色日记</span>
      </div>
      <button
        :disabled="currentPage === totalPages"
        @click="goToPage(currentPage + 1)"
        class="page-btn"
      >
        下一页
      </button>
    </div>

    <!-- 创建日记弹窗 -->
    <CreateDiaryModal
      v-if="showCreateModal"
      @close="showCreateModal = false"
      @created="handleDiaryCreated"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import {
  getDiaryList,
  searchDiaryByDestination,
  searchDiaryByName,
  fulltextSearch
} from '../services/diaryService'
import DiaryCard from '../components/DiaryCard.vue'
import CreateDiaryModal from '../components/CreateDiaryModal.vue'

const router = useRouter()
const { user, isAuthenticated } = useAuth()

// 搜索相关
const searchMode = ref('destination') // 'destination' | 'name' | 'fulltext'
const searchKeyword = ref('')
const hasSearched = ref(false)

// 排序和分页
const sortBy = ref('heat')
const diaries = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const totalPages = ref(1)

// 弹窗
const showCreateModal = ref(false)

// 计算属性
const getSearchPlaceholder = () => {
  const placeholders = {
    destination: '输入红色旅游目的地，如：井冈山、延安、西柏坡...',
    name: '输入日记标题（精确匹配）',
    fulltext: '输入关键词，搜索红色记忆...'
  }
  return placeholders[searchMode.value]
}

// 加载日记列表
const loadDiaries = async (page = 1) => {
  loading.value = true
  try {
    let response

    if (searchKeyword.value.trim()) {
      // 有搜索关键词
      switch (searchMode.value) {
        case 'destination':
          response = await searchDiaryByDestination({
            destination: searchKeyword.value.trim(),
            sortBy: sortBy.value,
            page,
            pageSize: pageSize.value
          })
          break
        case 'name':
          // 精确查询，只返回一条结果
          const diary = await searchDiaryByName(searchKeyword.value.trim())
          response = {
            diaries: diary ? [diary] : [],
            total: diary ? 1 : 0,
            page: 1,
            pageSize: 1,
            totalPages: diary ? 1 : 0
          }
          break
        case 'fulltext':
          response = await fulltextSearch({
            keyword: searchKeyword.value.trim(),
            page,
            pageSize: pageSize.value
          })
          break
      }
      hasSearched.value = true
    } else {
      // 无搜索关键词，获取推荐列表（不传userId，显示所有用户的日记）
      response = await getDiaryList({
        sortBy: sortBy.value,
        // 不传userId，让后端返回所有用户的日记
        page,
        pageSize: pageSize.value
      })
      hasSearched.value = false
    }

    diaries.value = response.diaries || []
    total.value = response.total || 0
    totalPages.value = response.totalPages || 1
    currentPage.value = response.page || page
  } catch (error) {
    console.error('加载日记失败:', error)
    diaries.value = []
    total.value = 0
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadDiaries(1)
}

// 排序变化
const handleSortChange = () => {
  currentPage.value = 1
  loadDiaries(1)
}

// 分页
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    loadDiaries(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// 查看日记详情
const viewDiary = (id) => {
  router.push(`/diary/${id}`)
}

// 处理创建日记
const handleCreateDiary = () => {
  if (!isAuthenticated.value) {
    // 未登录，触发登录弹窗
    window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    return
  }
  showCreateModal.value = true
}

// 处理日记创建成功
const handleDiaryCreated = () => {
  // 关闭弹窗
  showCreateModal.value = false
  // 刷新列表（延迟一下确保后端已保存）
  setTimeout(() => {
    loadDiaries(currentPage.value)
  }, 500)
}

// 处理日记删除
const handleDiaryDeleted = (deletedId) => {
  diaries.value = diaries.value.filter(diary => diary.id !== deletedId)
  if (total.value > 0) {
    total.value -= 1
  }
  if (diaries.value.length === 0 && currentPage.value > 1) {
    goToPage(currentPage.value - 1)
    return
  }
  if (!searchKeyword.value.trim()) {
    loadDiaries(currentPage.value)
  }
}

// 监听打开创建日记事件（从用户中心跳转过来时）
const handleOpenCreateDiary = () => {
  if (isAuthenticated.value) {
    showCreateModal.value = true
  }
}

// 初始化
onMounted(() => {
  loadDiaries(1)
  // 监听打开创建日记事件
  window.addEventListener('open-create-diary', handleOpenCreateDiary)
})

// 清理
onUnmounted(() => {
  window.removeEventListener('open-create-diary', handleOpenCreateDiary)
})
</script>

<style scoped>
.diary-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--spacing-6);
  background: linear-gradient(to bottom, #fff5f5, #fef2f2);
  min-height: 100vh;
}

/* 页面标题 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-6);
  flex-wrap: wrap;
  gap: var(--spacing-4);
  background: white;
  padding: var(--spacing-6);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: #b91c1c;
  margin: 0;
}

.page-subtitle {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  margin: var(--spacing-1) 0 0 0;
}

.btn-create {
  padding: var(--spacing-3) var(--spacing-5);
  font-size: var(--font-size-base);
  font-weight: 600;
  background: linear-gradient(to right, #dc2626, #b91c1c);
  border: none;
}

/* 搜索工具栏 */
.search-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-6);
  flex-wrap: wrap;
  background: white;
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.search-section {
  flex: 1;
  min-width: 300px;
}

.search-tabs {
  display: flex;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-2);
}

.search-tab {
  padding: var(--spacing-2) var(--spacing-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: pointer;
  transition: all 0.2s;
  font-size: var(--font-size-sm);
}

.search-tab:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.search-tab.active {
  border-color: #dc2626;
  background: #dc2626;
  color: white;
}

.search-input-group {
  display: flex;
  gap: var(--spacing-2);
}

.search-input {
  flex: 1;
  padding: var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
}

.search-input:focus {
  outline: none;
  border-color: #dc2626;
}

.btn-search {
  padding: var(--spacing-3) var(--spacing-5);
  background: linear-gradient(to right, #dc2626, #b91c1c);
  border: none;
  white-space: nowrap;
}

.sort-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.sort-label {
  font-size: var(--font-size-sm);
  color: var(--color-text);
}

.sort-select {
  padding: var(--spacing-2) var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  cursor: pointer;
}

/* 日记列表 */
.diaries-section {
  margin-bottom: var(--spacing-6);
}

.loading-state,
.empty-state {
  text-align: center;
  padding: var(--spacing-8);
  color: var(--color-text-secondary);
  font-size: var(--font-size-lg);
  background: white;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  margin-bottom: var(--spacing-4);
}

.empty-state button {
  margin-top: var(--spacing-3);
}

.diaries-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-4);
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-4);
  margin-top: var(--spacing-6);
  background: white;
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
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
  border-color: #dc2626;
  background: #fef2f2;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-1);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.total-info {
  font-size: var(--font-size-xs);
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-toolbar {
    flex-direction: column;
  }

  .diaries-grid {
    grid-template-columns: 1fr;
  }
}
</style>
