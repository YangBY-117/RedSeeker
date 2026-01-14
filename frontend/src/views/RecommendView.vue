<template>
  <div class="recommend-view">
    <!-- 搜索栏 -->
    <div class="search-section">
      <input
        v-model="searchKeyword"
        type="text"
        placeholder="搜索景点名称、简介..."
        class="search-input"
        @input="handleSearch"
      />
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <!-- 分类栏 -->
      <div class="category-section">
        <button
          v-for="cat in categories"
          :key="cat.id"
          :class="['category-btn', { active: selectedCategory === cat.id }]"
          @click="selectCategory(cat.id)"
        >
          {{ cat.name }}
        </button>
      </div>

      <!-- 排序选择 -->
      <div class="sort-section">
        <label class="sort-label">排序：</label>
        <select v-model="sortBy" @change="handleSortChange" class="sort-select">
          <option value="recommend">推荐排序</option>
          <option value="heat">按热度</option>
          <option value="rating">按评价</option>
        </select>
      </div>
    </div>

    <!-- 推荐列表 -->
    <div class="attractions-section">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="attractions.length === 0" class="empty">暂无推荐景点</div>
      <div v-else class="attractions-grid">
        <AttractionCard
          v-for="attraction in attractions"
          :key="attraction.id"
          :attraction="attraction"
        />
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="!loading && attractions.length > 0" class="pagination">
      <button
        :disabled="currentPage === 1"
        @click="goToPage(currentPage - 1)"
        class="page-btn"
      >
        上一页
      </button>
      <div class="page-info">
        <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
        <span class="total-info">共 {{ total }} 个景点</span>
      </div>
      <button
        :disabled="currentPage === totalPages"
        @click="goToPage(currentPage + 1)"
        class="page-btn"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../composables/useAuth'
import AttractionCard from '../components/AttractionCard.vue'
import { getRecommendations, searchAttractions, recordBrowse } from '../services/recommendService.js'

const searchKeyword = ref('')
const selectedCategory = ref(null)
const sortBy = ref('recommend')
const attractions = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(6)
const total = ref(0)
const totalPages = ref(1)

const categories = [
  { id: null, name: '推荐' },
  { id: 1, name: '纪念馆' },
  { id: 2, name: '烈士陵园' },
  { id: 3, name: '会议旧址' },
  { id: 4, name: '战役遗址' },
  { id: 5, name: '名人故居' },
  { id: 6, name: '革命根据地' },
  { id: 7, name: '纪念碑塔' },
  { id: 8, name: '博物馆' },
  { id: 9, name: '其他纪念地' }
]

const { user } = useAuth()

const loadAttractions = async (page = 1) => {
  loading.value = true
  try {
    let response
    if (searchKeyword.value.trim()) {
      response = await searchAttractions({
        keyword: searchKeyword.value.trim(),
        category: selectedCategory.value,
        sortBy: sortBy.value,
        page,
        pageSize: pageSize.value
      })
    } else {
      response = await getRecommendations({
        category: selectedCategory.value,
        sortBy: sortBy.value,
        page,
        pageSize: pageSize.value,
        userId: user.value?.id
      })
    }
    
    if (response.success) {
      attractions.value = response.data.attractions
      total.value = response.data.total
      totalPages.value = response.data.totalPages
      currentPage.value = response.data.page
      
      // 记录用户浏览历史（如果已登录）
      if (user.value && attractions.value.length > 0) {
        attractions.value.forEach(attr => {
          recordBrowse(attr.id)
        })
      }
    }
  } catch (error) {
    console.error('加载失败:', error)
    attractions.value = []
    total.value = 0
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

const selectCategory = (categoryId) => {
  selectedCategory.value = categoryId
  currentPage.value = 1
  loadAttractions(1)
}

const handleSortChange = () => {
  currentPage.value = 1
  loadAttractions(1)
}

const handleSearch = () => {
  currentPage.value = 1
  loadAttractions(1)
}

const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    loadAttractions(page)
    // 滚动到顶部
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

onMounted(() => {
  loadAttractions(1)
})
</script>

<style scoped>
.recommend-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.search-section {
  margin-bottom: 24px;
}

.search-input {
  width: 100%;
  padding: 14px 20px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.3s;
  box-sizing: border-box;
}

.search-input:focus {
  outline: none;
  border-color: var(--color-primary, #c62828);
  box-shadow: 0 0 0 4px rgba(198, 40, 40, 0.1);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 20px;
  flex-wrap: wrap;
}

.category-section {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  flex: 1;
}

.category-btn {
  padding: 10px 20px;
  border: 2px solid #e0e0e0;
  background: white;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
}

.category-btn:hover {
  border-color: var(--color-primary, #c62828);
  color: var(--color-primary, #c62828);
  transform: translateY(-2px);
}

.category-btn.active {
  background: var(--color-primary, #c62828);
  color: white;
  border-color: var(--color-primary, #c62828);
  box-shadow: 0 4px 12px rgba(198, 40, 40, 0.2);
}

.sort-section {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.sort-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.sort-select {
  padding: 8px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
}

.sort-select:focus {
  outline: none;
  border-color: var(--color-primary, #c62828);
}

.attractions-section {
  min-height: 400px;
  margin-bottom: 32px;
}

.loading,
.empty {
  text-align: center;
  padding: 80px 20px;
  color: #999;
  font-size: 16px;
}

.attractions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  padding: 24px 0;
  border-top: 1px solid #f0f0f0;
}

.page-btn {
  padding: 10px 24px;
  border: 2px solid var(--color-primary, #c62828);
  background: white;
  color: var(--color-primary, #c62828);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  background: var(--color-primary, #c62828);
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(198, 40, 40, 0.2);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 14px;
}

.total-info {
  font-size: 12px;
  color: #999;
}

@media (max-width: 768px) {
  .recommend-view {
    padding: 16px;
  }
  
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .category-section {
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 8px;
    -webkit-overflow-scrolling: touch;
  }
  
  .sort-section {
    justify-content: space-between;
  }
  
  .attractions-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .pagination {
    flex-direction: column;
    gap: 12px;
  }
  
  .page-btn {
    width: 100%;
  }
}
</style>