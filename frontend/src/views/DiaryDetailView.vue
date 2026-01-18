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
            <span class="meta-text">{{ formatDate(diary.travelDate || diary.createdAt) }}</span>
          </div>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="diary-stats">
        <div class="stat-item">
          <span class="stat-icon">👁️</span>
          <span class="stat-value">{{ diary.viewCount || 0 }}</span>
          <span class="stat-label">浏览量</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">⭐</span>
          <span class="stat-value">{{ getRatingText() }}</span>
          <span v-if="diary.totalRatings > 0" class="stat-label">({{ diary.totalRatings }}人评分)</span>
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
              v-if="media.mediaType === 'image'"
              :src="resolveMediaUrl(media.filePath)"
              :alt="`图片 ${index + 1}`"
              @click="showImageViewer(resolveMediaUrl(media.filePath))"
              @error="handleImageError"
            />
            <video
              v-else-if="media.mediaType === 'video'"
              :src="resolveMediaUrl(media.filePath)"
              :poster="resolveMediaUrl(media.thumbnailPath)"
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

      <!-- 评论区域 -->
      <div class="comments-section">
        <h3 class="section-title">评论 ({{ comments.length }})</h3>
        <div v-if="isAuthenticated" class="comment-form">
          <textarea
            v-model="newComment"
            placeholder="写下您的评论..."
            rows="3"
            class="comment-input"
          ></textarea>
          <button
            type="button"
            class="btn btn-primary"
            :disabled="!newComment.trim() || submittingComment"
            @click="handleSubmitComment"
          >
            {{ submittingComment ? '提交中...' : '发表评论' }}
          </button>
        </div>
        <div v-else class="comment-login-hint">
          <p>请先登录后发表评论</p>
        </div>
        <div v-if="loadingComments" class="loading-comments">加载评论中...</div>
        <div v-else-if="comments.length === 0" class="no-comments">暂无评论</div>
        <div v-else class="comments-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <span class="comment-author">{{ comment.username || '匿名用户' }}</span>
              <span class="comment-time">{{ formatCommentTime(comment.createdAt) }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
          </div>
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
  addComment,
  getComments
} from '../services/diaryService'

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const mediaBase = apiBase.replace(/\/api\/?$/, '')

const route = useRoute()
const router = useRouter()
const { user, isAuthenticated } = useAuth()

const diary = ref(null)
const loading = ref(true)
const error = ref('')
const userRating = ref(0)
const hoverRating = ref(0)
const comments = ref([])
const newComment = ref('')
const submittingComment = ref(false)
const loadingComments = ref(false)

const getRatingText = () => {
  const rating = diary.value?.averageRating
  if (!rating || rating === 0 || isNaN(rating)) return '暂无评分'
  return rating.toFixed(1)
}

// 格式化内容（支持Markdown）
const formatContent = (content) => {
  if (!content) return ''
  
  // 简单的Markdown解析
  let html = content
    // 标题
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    // 粗体
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // 斜体
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    // 代码块
    .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    // 行内代码
    .replace(/`(.*?)`/g, '<code>$1</code>')
    // 链接
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
    // 换行
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>')
  
  // 包装段落
  if (!html.startsWith('<h') && !html.startsWith('<p') && !html.startsWith('<pre')) {
    html = '<p>' + html + '</p>'
  }
  
  return html
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

const resolveMediaUrl = (path) => {
  if (!path) return ''
  
  // 如果是完整的HTTP/HTTPS URL，直接返回
  if (/^https?:\/\//i.test(path)) return path
  
  // 如果是data URL，直接返回
  if (/^data:/i.test(path)) return path
  
  // 标准化路径
  const normalized = path.replace(/\\/g, '/').trim()
  
  // 如果已经是完整路径（包含协议），直接返回
  if (normalized.startsWith('http://') || normalized.startsWith('https://')) {
    return normalized
  }
  
  // 如果路径看起来像是OSS URL（包含oss-或aliyuncs.com），直接返回
  if (normalized.includes('oss-') || normalized.includes('aliyuncs.com')) {
    // 如果OSS URL没有协议，添加https://
    if (!normalized.startsWith('http://') && !normalized.startsWith('https://')) {
      return 'https://' + normalized
    }
    return normalized
  }
  
  // 如果以/uploads/开头，直接拼接mediaBase
  if (normalized.startsWith('/uploads/')) {
    return `${mediaBase}${normalized}`
  }
  
  // 如果包含/uploads/，提取该部分
  const uploadsIndex = normalized.indexOf('/uploads/')
  if (uploadsIndex !== -1) {
    return `${mediaBase}${normalized.slice(uploadsIndex)}`
  }
  
  // 如果以/开头但不是/uploads/，尝试拼接
  if (normalized.startsWith('/')) {
    return `${mediaBase}${normalized}`
  }
  
  // 否则添加/uploads/前缀（假设是相对路径）
  return `${mediaBase}/uploads/${normalized}`
}

// 图片查看器（简单实现）
const showImageViewer = (imageUrl) => {
  // 可以在这里实现图片查看器
  window.open(imageUrl, '_blank')
}

// 处理图片加载错误
const handleImageError = (event) => {
  console.warn('图片加载失败:', event.target.src)
  // 可以设置一个默认图片或隐藏该元素
  event.target.style.display = 'none'
}

const normalizeDiaryDetail = (data) => {
  if (!data) return null
  return {
    ...data,
    travelDate: data.travelDate ?? data.travel_date ?? '',
    createdAt: data.createdAt ?? data.created_at ?? '',
    viewCount: data.viewCount ?? data.view_count ?? 0,
    averageRating: data.averageRating ?? data.average_rating ?? 0,
    totalRatings: data.totalRatings ?? data.total_ratings ?? 0,
    userRating: data.userRating ?? data.user_rating ?? 0,
    media: Array.isArray(data.media)
      ? data.media.map((m) => ({
          ...m,
          mediaType: m.mediaType ?? m.media_type,
          filePath: m.filePath ?? m.file_path,
          thumbnailPath: m.thumbnailPath ?? m.thumbnail_path
        }))
      : []
  }
}

// 加载日记详情
const loadDiary = async () => {
  loading.value = true
  error.value = ''
  try {
    const diaryId = route.params.id
    const data = await getDiaryDetail(diaryId)
    diary.value = normalizeDiaryDetail(data)
    // 如果有用户评分，设置评分
    if (diary.value?.userRating) {
      userRating.value = diary.value.userRating
    }
    // 加载评论
    await loadComments()
  } catch (err) {
    console.error('加载日记失败:', err)
    error.value = '加载日记失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 加载评论
const loadComments = async () => {
  if (!diary.value?.id) return
  loadingComments.value = true
  try {
    const data = await getComments(diary.value.id)
    comments.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('加载评论失败:', err)
    comments.value = []
  } finally {
    loadingComments.value = false
  }
}

// 提交评论
const handleSubmitComment = async () => {
  if (!newComment.value.trim() || !diary.value) return
  
  submittingComment.value = true
  const commentText = newComment.value.trim()
  newComment.value = '' // 先清空输入框，提供即时反馈
  
  try {
    await addComment(diary.value.id, commentText)
    // 重新加载评论列表
    await loadComments()
  } catch (err) {
    // 即使请求失败，也尝试重新加载评论，因为评论可能已经成功提交
    console.error('提交评论请求失败:', err)
    // 恢复输入框内容
    newComment.value = commentText
    // 尝试重新加载评论，如果评论已成功，会显示出来
    setTimeout(async () => {
      await loadComments()
    }, 1000)
  } finally {
    submittingComment.value = false
  }
}

// 格式化评论时间
const formatCommentTime = (timeString) => {
  if (!timeString) return ''
  const date = new Date(timeString)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
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
  margin-bottom: var(--spacing-6);
}

.diary-text :deep(h1) {
  font-size: var(--font-size-2xl);
  margin: var(--spacing-4) 0;
  color: var(--color-primary);
}

.diary-text :deep(h2) {
  font-size: var(--font-size-xl);
  margin: var(--spacing-3) 0;
  color: var(--color-text);
}

.diary-text :deep(h3) {
  font-size: var(--font-size-lg);
  margin: var(--spacing-2) 0;
}

.diary-text :deep(p) {
  margin: var(--spacing-2) 0;
}

.diary-text :deep(strong) {
  font-weight: 600;
  color: var(--color-primary);
}

.diary-text :deep(code) {
  background: var(--color-bg);
  padding: 2px 4px;
  border-radius: 3px;
  font-family: monospace;
}

.diary-text :deep(pre) {
  background: var(--color-bg);
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
  overflow-x: auto;
}

.diary-text :deep(a) {
  color: var(--color-primary);
  text-decoration: none;
}

.diary-text :deep(a:hover) {
  text-decoration: underline;
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
  max-height: 500px;
  object-fit: contain;
  cursor: pointer;
  transition: transform 0.3s;
  display: block;
}

.media-item img:hover {
  transform: scale(1.05);
}

.media-item img[src=""],
.media-item img:not([src]) {
  display: none;
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


/* 评论区域样式 */
.comments-section {
  margin-top: var(--spacing-6);
  padding-top: var(--spacing-6);
  border-top: 1px solid var(--color-border);
}

.comment-form {
  margin-bottom: var(--spacing-5);
}

.comment-input {
  width: 100%;
  padding: var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  font-family: inherit;
  resize: vertical;
  margin-bottom: var(--spacing-3);
}

.comment-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.comment-login-hint {
  padding: var(--spacing-4);
  text-align: center;
  color: var(--color-text-secondary);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-5);
}

.loading-comments,
.no-comments {
  padding: var(--spacing-4);
  text-align: center;
  color: var(--color-text-secondary);
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.comment-item {
  padding: var(--spacing-4);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-2);
}

.comment-author {
  font-weight: 600;
  color: var(--color-text);
}

.comment-time {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.comment-content {
  color: var(--color-text);
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
