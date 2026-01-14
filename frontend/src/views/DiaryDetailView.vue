<template>
  <div class="diary-detail-view">
    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <div v-else-if="diary" class="diary-content">
      <!-- 返回按钮 -->
      <button class="btn-back" @click="$router.go(-1)">← 返回</button>

      <!-- 日记头部 -->
      <div class="diary-header">
        <h1 class="diary-title">{{ diary.title }}</h1>
        <div class="diary-meta">
          <div class="meta-item">
            <span class="meta-icon">👤</span>
            <span class="meta-text">{{ diary.author?.username || '匿名' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-icon">📍</span>
            <span class="meta-text">{{ diary.destination || '未知' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-icon">📅</span>
            <span class="meta-text">{{ formatDate(diary.travel_date || diary.created_at) }}</span>
          </div>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="diary-stats">
        <div class="stat-item">
          <span class="stat-icon">👁️</span>
          <span class="stat-value">{{ diary.view_count || 0 }}</span>
          <span class="stat-label">浏览量</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">⭐</span>
          <span class="stat-value">{{ diary.average_rating?.toFixed(1) || '0.0' }}</span>
          <span class="stat-label">评分 ({{ diary.total_ratings || 0 }})</span>
        </div>
      </div>

      <!-- 评分区域 -->
      <div v-if="isAuthenticated" class="rating-section">
        <label class="rating-label">我的评分：</label>
        <div class="rating-stars">
          <button
            v-for="star in 5"
            :key="star"
            :class="['star-btn', { active: userRating >= star, hover: hoverRating >= star }]"
            @click="handleRate(star)"
            @mouseenter="hoverRating = star"
            @mouseleave="hoverRating = 0"
          >
            ⭐
          </button>
        </div>
        <span v-if="userRating > 0" class="rating-text">{{ userRating }} 分</span>
      </div>

      <!-- 日记内容 -->
      <div class="diary-body">
        <div class="diary-text" v-html="formatContent(diary.content)"></div>

        <!-- 媒体文件 -->
        <div v-if="diary.media && diary.media.length > 0" class="diary-media">
          <div
            v-for="(media, index) in diary.media"
            :key="media.id"
            class="media-item"
          >
            <img
              v-if="media.media_type === 'image'"
              :src="media.file_path"
              :alt="`图片 ${index + 1}`"
              @click="showImageViewer(media.file_path)"
            />
            <video
              v-else-if="media.media_type === 'video'"
              :src="media.file_path"
              :poster="media.thumbnail_path"
              controls
            ></video>
          </div>
        </div>

        <!-- 关联景点 -->
        <div v-if="diary.attractions && diary.attractions.length > 0" class="diary-attractions">
          <h3 class="section-title">相关红色景点</h3>
          <div class="attractions-list">
            <div
              v-for="attraction in diary.attractions"
              :key="attraction.id"
              class="attraction-item"
            >
              <span class="attraction-name">{{ attraction.name }}</span>
              <span class="attraction-address">{{ attraction.address }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AIGC动画生成 -->
      <div v-if="isAuthenticated" class="aigc-section">
        <h3 class="section-title">AI红色记忆动画</h3>
        <p class="section-desc">基于您的红色旅游照片和文字描述，生成专属的红色记忆动画视频</p>
        <button
          class="btn btn-primary"
          :disabled="generatingAnimation || animationVideoUrl"
          @click="handleGenerateAnimation"
        >
          {{ generatingAnimation ? '生成中...' : (animationVideoUrl ? '已生成' : '生成红色记忆动画') }}
        </button>
        <div v-if="animationTaskId" class="animation-status">
          <p>任务ID: {{ animationTaskId }}</p>
          <p>状态: {{ animationStatusText }}</p>
          <div v-if="['processing', 'waiting'].includes(animationTaskStatus)" class="progress-bar-container">
            <div class="progress-bar" :class="getProgressStatusClass">
              <div class="progress-bar-fill" :style="{ width: getProgressPercentage + '%' }"></div>
            </div>
            <span class="progress-text">{{ getProgressPercentage }}%</span>
          </div>
          <button
            class="btn btn-outline"
            @click="checkAnimationStatus"
          >
            刷新状态
          </button>
        </div>
        <div v-if="animationVideoUrl" class="animation-result">
          <h4>生成的红色记忆动画</h4>
          <video :src="animationVideoUrl" controls></video>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import {
  getDiaryDetail,
  rateDiary,
  generateAnimation,
  getAnimationStatus
} from '../services/diaryService'

const route = useRoute()
const router = useRouter()
const { user, isAuthenticated } = useAuth()

const diary = ref(null)
const loading = ref(true)
const error = ref('')
const userRating = ref(0)
const hoverRating = ref(0)
const generatingAnimation = ref(false)
const animationTaskId = ref('')
const animationVideoUrl = ref('')
const animationTaskStatus = ref('')

// 计算属性
const getProgressPercentage = computed(() => {
  if (!animationTaskStatus.value) return 0
  if (animationTaskStatus.value === 'processing') return 50
  if (animationTaskStatus.value === 'completed') return 100
  return 0
})

const getProgressStatusClass = computed(() => {
  if (!animationTaskStatus.value) return ''
  if (animationTaskStatus.value === 'completed') return 'progress-success'
  if (animationTaskStatus.value === 'failed') return 'progress-error'
  return 'progress-warning'
})

const animationStatusText = computed(() => {
  if (!animationTaskStatus.value) return ''
  switch (animationTaskStatus.value) {
    case 'processing': return '动画生成中...'
    case 'completed': return '动画生成完成'
    case 'failed': return '动画生成失败'
    default: return '等待中...'
  }
})

// 格式化内容（支持换行）
const formatContent = (content) => {
  if (!content) return ''
  return content.replace(/\n/g, '<br>')
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

// 图片查看器（简单实现）
const showImageViewer = (imageUrl) => {
  // 可以在这里实现图片查看器
  window.open(imageUrl, '_blank')
}

// 加载日记详情
const loadDiary = async () => {
  loading.value = true
  error.value = ''
  try {
    const diaryId = route.params.id
    const data = await getDiaryDetail(diaryId)
    diary.value = data
    // 如果有用户评分，设置评分
    if (data.user_rating) {
      userRating.value = data.user_rating
    }
  } catch (err) {
    console.error('加载日记失败:', err)
    error.value = '加载日记失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 评分处理
const handleRate = async (rating) => {
  if (!isAuthenticated.value) {
    alert('请先登录')
    return
  }
  try {
    await rateDiary(diary.value.id, rating)
    userRating.value = rating
    // 更新日记的评分信息
    if (diary.value) {
      // 重新加载日记以获取最新评分
      await loadDiary()
    }
  } catch (err) {
    console.error('评分失败:', err)
    alert('评分失败，请稍后重试')
  }
}

// 生成动画
const handleGenerateAnimation = async () => {
  if (!isAuthenticated.value) {
    alert('请先登录')
    return
  }
  generatingAnimation.value = true
  try {
    const images = diary.value.media
      ?.filter(m => m.media_type === 'image')
      .map(m => m.file_path) || []
    const description = diary.value.content?.substring(0, 500) || ''
    
    const result = await generateAnimation(diary.value.id, {
      images,
      description
    })
    animationTaskId.value = result.task_id
    animationTaskStatus.value = result.status
    
    // 如果已完成，直接显示视频
    if (result.status === 'completed' && result.video_url) {
      animationVideoUrl.value = result.video_url
    } else {
      // 开始轮询状态
      checkAnimationStatus()
    }
  } catch (err) {
    console.error('生成动画失败:', err)
    alert('生成动画失败，请稍后重试')
  } finally {
    generatingAnimation.value = false
  }
}

// 检查动画状态
const checkAnimationStatus = async () => {
  if (!animationTaskId.value) return
  
  try {
    const result = await getAnimationStatus(animationTaskId.value)
    animationTaskStatus.value = result.status
    
    if (result.status === 'completed' && result.video_url) {
      animationVideoUrl.value = result.video_url
    } else if (result.status === 'processing' || result.status === 'waiting') {
      // 继续轮询
      setTimeout(() => {
        checkAnimationStatus()
      }, 3000)
    }
  } catch (err) {
    console.error('查询动画状态失败:', err)
  }
}

onMounted(() => {
  loadDiary()
})
</script>

<style scoped>
.diary-detail-view {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--spacing-6);
}

.loading-state,
.error-state {
  text-align: center;
  padding: var(--spacing-8);
  font-size: var(--font-size-lg);
}

.error-state {
  color: #c33;
}

.btn-back {
  padding: var(--spacing-2) var(--spacing-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: pointer;
  margin-bottom: var(--spacing-4);
  transition: all 0.2s;
}

.btn-back:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.diary-content {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-md);
}

.diary-header {
  margin-bottom: var(--spacing-5);
  padding-bottom: var(--spacing-5);
  border-bottom: 1px solid var(--color-border);
}

.diary-title {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 var(--spacing-4) 0;
}

.diary-meta {
  display: flex;
  gap: var(--spacing-4);
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.meta-icon {
  font-size: var(--font-size-base);
}

.diary-stats {
  display: flex;
  gap: var(--spacing-6);
  margin-bottom: var(--spacing-5);
  padding: var(--spacing-4);
  background: var(--color-bg);
  border-radius: var(--radius-md);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.stat-icon {
  font-size: var(--font-size-lg);
}

.stat-value {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.rating-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-5);
  padding: var(--spacing-4);
  background: var(--color-bg);
  border-radius: var(--radius-md);
}

.rating-label {
  font-size: var(--font-size-sm);
  color: var(--color-text);
}

.rating-stars {
  display: flex;
  gap: var(--spacing-1);
}

.star-btn {
  background: none;
  border: none;
  font-size: var(--font-size-2xl);
  cursor: pointer;
  opacity: 0.3;
  transition: all 0.2s;
  padding: 0;
  line-height: 1;
}

.star-btn.active,
.star-btn.hover {
  opacity: 1;
  transform: scale(1.2);
}

.rating-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.diary-body {
  margin-bottom: var(--spacing-6);
}

.diary-text {
  font-size: var(--font-size-base);
  line-height: 1.8;
  color: var(--color-text);
  white-space: pre-wrap;
  margin-bottom: var(--spacing-6);
}

.diary-media {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-6);
}

.media-item {
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-bg);
}

.media-item img {
  width: 100%;
  height: auto;
  cursor: pointer;
  transition: transform 0.3s;
}

.media-item img:hover {
  transform: scale(1.05);
}

.media-item video {
  width: 100%;
  height: auto;
}

.diary-attractions {
  margin-top: var(--spacing-6);
  padding-top: var(--spacing-6);
  border-top: 1px solid var(--color-border);
}

.section-title {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--spacing-4) 0;
}

.attractions-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.attraction-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-3);
  background: var(--color-bg);
  border-radius: var(--radius-md);
}

.attraction-name {
  font-weight: 500;
  color: var(--color-text);
}

.attraction-address {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.aigc-section {
  margin-top: var(--spacing-6);
  padding-top: var(--spacing-6);
  border-top: 1px solid var(--color-border);
  background: linear-gradient(to right, #fff5f5, #fef2f2);
  border-radius: var(--radius-md);
  padding: var(--spacing-5);
}

.section-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: var(--spacing-2) 0 var(--spacing-4) 0;
}

.animation-status {
  margin-top: var(--spacing-4);
  padding: var(--spacing-3);
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.animation-result {
  margin-top: var(--spacing-4);
}

.animation-result h4 {
  font-size: var(--font-size-lg);
  color: var(--color-text);
  margin: 0 0 var(--spacing-3) 0;
  text-align: center;
}

.animation-result video {
  width: 100%;
  border-radius: var(--radius-md);
  max-width: 600px;
}

/* 进度条样式 */
.progress-bar-container {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  margin: var(--spacing-3) 0;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: var(--color-bg);
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(to right, #dc2626, #b91c1c);
  transition: width 0.3s ease;
  border-radius: 4px;
}

.progress-bar.progress-success .progress-bar-fill {
  background: linear-gradient(to right, #10b981, #059669);
}

.progress-bar.progress-error .progress-bar-fill {
  background: linear-gradient(to right, #ef4444, #dc2626);
}

.progress-bar.progress-warning .progress-bar-fill {
  background: linear-gradient(to right, #f59e0b, #d97706);
}

.progress-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  min-width: 50px;
  text-align: right;
}
</style>