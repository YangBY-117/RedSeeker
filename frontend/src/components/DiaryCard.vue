<template>
  <div class="diary-card" @click="$emit('click')">
    <div v-if="diary.cover_image" class="card-cover">
      <img :src="diary.cover_image" :alt="diary.title" />
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
          <span class="stat-value">{{ diary.view_count || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">⭐</span>
          <span class="stat-value">{{ diary.average_rating?.toFixed(1) || '0.0' }}</span>
          <span class="stat-label">({{ diary.total_ratings || 0 }})</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">📅</span>
          <span class="stat-value">{{ formatDate(diary.created_at) }}</span>
        </div>
      </div>
    </div>
    <div v-if="diary.travel_date" class="red-tourism-tag">
      红色之旅
    </div>
  </div>
</template>

<script setup>
defineProps({
  diary: {
    type: Object,
    required: true
  }
})

defineEmits(['click'])

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
</style>