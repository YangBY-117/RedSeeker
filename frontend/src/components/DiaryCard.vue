<template>
  <div class="diary-card" @click="handleCardClick">
    <div v-if="canDelete" class="card-delete" @click.stop="handleDelete">
      🗑️
    </div>
    <div v-if="coverImage" class="card-cover">
      <img :src="coverImage" :alt="diary.title" />
    </div>
    <div class="card-content">
      <h3 class="card-title">{{ diary.title }}</h3>
      <p class="card-excerpt">{{ getExcerpt(diary.content) }}</p>
      <div class="card-meta">
        <div class="meta-item">
          <span class="meta-icon">📍</span>
          <span class="meta-text">{{ diary.destination || '未知' }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-icon">👤</span>
          <span class="meta-text">{{ diary.author?.username || '匿名' }}</span>
        </div>
      </div>
      <div class="card-stats">
        <div class="stat-item">
          <span class="stat-icon">👁️</span>
          <span class="stat-value">{{ viewCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">⭐</span>
          <span class="stat-value">{{ averageRatingText }}</span>
          <span v-if="totalRatings > 0" class="stat-label">({{ totalRatings }}人评分)</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">📅</span>
          <span class="stat-value">{{ formatDate(createdAt) }}</span>
        </div>
      </div>
    </div>
    <div v-if="travelDate" class="red-tourism-tag">
      红色之旅
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuth } from '../composables/useAuth'
import { deleteDiary } from '../services/diaryService'

const props = defineProps({
  diary: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click', 'deleted'])

const { user } = useAuth()

// 判断当前用户是否可以删除此日记
const canDelete = computed(() => {
  return user.value && user.value.id === props.diary.author?.id
})

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const mediaBase = apiBase.replace(/\/api\/?$/, '')

const resolveMediaUrl = (path) => {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  const normalized = path.replace(/\\/g, '/')
  const uploadsIndex = normalized.indexOf('/uploads/')
  if (uploadsIndex !== -1) {
    return `${mediaBase}${normalized.slice(uploadsIndex)}`
  }
  if (normalized.startsWith('/')) return `${mediaBase}${normalized}`
  return `${mediaBase}/${normalized}`
}

const coverImage = computed(() => {
  const img = props.diary.coverImage || props.diary.cover_image || ''
  return img ? resolveMediaUrl(img) : ''
})
const viewCount = computed(() => props.diary.viewCount ?? props.diary.view_count ?? 0)
const averageRating = computed(() => props.diary.averageRating ?? props.diary.average_rating ?? 0)
const averageRatingText = computed(() => {
  const rating = averageRating.value
  if (!rating || rating === 0 || isNaN(rating)) return '暂无评分'
  return Number(rating).toFixed(1)
})
const totalRatings = computed(() => props.diary.totalRatings ?? props.diary.total_ratings ?? 0)
const createdAt = computed(() => props.diary.createdAt || props.diary.created_at || '')
const travelDate = computed(() => props.diary.travelDate || props.diary.travel_date || '')

const handleCardClick = () => {
  emit('click')
}

const handleDelete = async (e) => {
  e.stopPropagation()
  if (!confirm('确定要删除这篇日记吗？此操作不可恢复。')) {
    return
  }
  
  try {
    await deleteDiary(props.diary.id)
    emit('deleted', props.diary.id)
  } catch (error) {
    console.error('删除日记失败:', error)
    const status = error.response?.status
    const message = error.response?.data?.message
    if (status === 401) {
      alert('登录已失效，请重新登录')
    } else if (status === 403) {
      alert('无权限删除该日记')
    } else {
      alert(message || '删除失败，请稍后重试')
    }
  }
}

const getExcerpt = (content) => {
  if (!content) return '暂无内容'
  const maxLength = 100
  if (content.length <= maxLength) return content
  return content.substring(0, maxLength) + '...'
}

const formatDate = (dateString) => {
  if (!dateString) return '未知'
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
</script>

<style scoped>
.diary-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
}

.diary-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.card-cover {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: var(--color-bg);
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-content {
  padding: var(--spacing-4);
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--spacing-2) 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-excerpt {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0 0 var(--spacing-3) 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.card-meta {
  display: flex;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-3);
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.meta-icon {
  font-size: var(--font-size-sm);
}

.card-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--spacing-3);
  border-top: 1px solid var(--color-border);
  flex-wrap: wrap;
  gap: var(--spacing-2);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.stat-icon {
  font-size: var(--font-size-sm);
}

.stat-value {
  font-weight: 500;
  color: var(--color-text);
}

.stat-label {
  color: var(--color-text-secondary);
}

.red-tourism-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  background: linear-gradient(45deg, #dc2626, #b91c1c);
  color: white;
  padding: 4px 10px;
  border-radius: 15px;
  font-size: 12px;
  font-weight: bold;
  z-index: 1;
}

.card-delete {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 2;
  transition: all 0.2s ease;
}

.card-delete:hover {
  background: rgba(220, 38, 38, 0.8);
  transform: scale(1.1);
}
</style>
