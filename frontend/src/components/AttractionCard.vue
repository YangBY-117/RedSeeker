<template>
  <div class="attraction-card">
    <div class="card-image">
      <img
        v-if="attraction.image_url"
        :src="attraction.image_url"
        :alt="attraction.name"
        class="card-image-img"
        @error="handleImageError"
      />
      <div v-else class="image-placeholder">
        <span class="image-icon">🏛️</span>
      </div>
      <div class="card-category-badge">{{ attraction.categoryName || attraction.category }}</div>
    </div>
    
    <div class="card-content">
      <div class="card-header">
        <h3 class="card-title">{{ attraction.name }}</h3>
        <div class="card-score">
          <span class="score-label">推荐</span>
          <div class="score-container">
            <span class="score-value">{{ attraction.recommend_score }}</span>
            <div v-if="attraction.reason" class="reason-tooltip-wrapper">
              <span class="reason-icon">💡</span>
              <div class="reason-tooltip">
                <div class="tooltip-arrow"></div>
                <div class="tooltip-content">
                  <div class="tooltip-title">推荐理由</div>
                  <div class="tooltip-text">{{ attraction.reason }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <p class="card-intro">{{ attraction.brief_intro }}</p>
      
      <div class="card-info">
        <div class="info-row">
          <span class="info-icon">📍</span>
          <span class="info-text">{{ attraction.address }}</span>
        </div>
        <div class="info-row">
          <span class="info-icon">🕐</span>
          <span class="info-text">{{ attraction.business_hours }}</span>
        </div>
        <div v-if="attraction.per_capita_consumption > 0" class="info-row">
          <span class="info-icon">💰</span>
          <span class="info-text">人均消费 ¥{{ attraction.per_capita_consumption }}</span>
        </div>
        <div v-else class="info-row">
          <span class="info-icon">💰</span>
          <span class="info-text free">免费开放</span>
        </div>
      </div>
    </div>
    
    <div class="card-footer">
      <div class="rating-section">
        <div class="rating-stars">
          <span v-for="i in 5" :key="i" class="star" :class="{ filled: i <= Math.round(attraction.average_rating) }">
            ⭐
          </span>
        </div>
        <div class="rating-text">
          <span class="rating-value">{{ attraction.average_rating }}</span>
          <span class="rating-count">({{ attraction.total_ratings }})</span>
        </div>
      </div>
      <div class="card-actions">
        <div class="heat-section">
          <span class="heat-icon">🔥</span>
          <span class="heat-value">{{ attraction.heat_score }}</span>
        </div>
        <button
          :class="['add-route-btn', { added: isSelected }]"
          @click="handleToggleRoute"
        >
          {{ isSelected ? '✓ 已加入' : '+ 加入路线' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouteCart } from '../composables/useRouteCart'

const props = defineProps({
  attraction: {
    type: Object,
    required: true
  }
})

const { addAttraction, removeAttraction, isSelected: checkIsSelected } = useRouteCart()

const isSelected = computed(() => checkIsSelected(props.attraction.id))

const handleToggleRoute = () => {
  if (isSelected.value) {
    removeAttraction(props.attraction.id)
  } else {
    addAttraction(props.attraction)
  }
}

const handleImageError = (event) => {
  const img = event.target
  const attractionName = props.attraction.name
  
  // 如果当前src不是public路径，尝试使用public路径
  if (img.src && !img.src.includes('/attraction_images/')) {
    const publicImageUrl = `/attraction_images/${encodeURIComponent(attractionName)}.jpg`
    // 避免循环错误
    if (img.src !== publicImageUrl) {
      img.src = publicImageUrl
      return
    }
  }
  
  // 如果public路径也失败，显示占位符
  img.style.display = 'none'
  const placeholder = img.nextElementSibling
  if (placeholder && placeholder.classList.contains('image-placeholder')) {
    placeholder.style.display = 'flex'
  }
}
</script>

<style scoped>
.attraction-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.attraction-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 24px rgba(198, 40, 40, 0.15);
}

.card-image {
  position: relative;
  width: 100%;
  height: 180px;
  background: linear-gradient(135deg, #c62828 0%, #8e0000 100%);
  overflow: hidden;
}

.card-image-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(198, 40, 40, 0.9) 0%, rgba(142, 0, 0, 0.9) 100%);
}

.image-icon {
  font-size: 64px;
  opacity: 0.8;
}

.card-category-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(255, 255, 255, 0.95);
  color: var(--color-primary, #c62828);
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card-content {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 12px;
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin: 0;
  flex: 1;
  line-height: 1.4;
}

.card-score {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}

.score-label {
  font-size: 10px;
  color: #999;
  margin-bottom: 2px;
}

.score-container {
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
}

.score-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary, #c62828);
  line-height: 1;
}

.reason-tooltip-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.reason-icon {
  font-size: 16px;
  cursor: pointer;
  transition: transform 0.2s;
}

.reason-icon:hover {
  transform: scale(1.2);
}

.reason-tooltip {
  position: absolute;
  bottom: 100%;
  right: 0;
  margin-bottom: 8px;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.3s, visibility 0.3s;
  z-index: 1000;
  pointer-events: none;
}

.reason-tooltip-wrapper:hover .reason-tooltip {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
}

.tooltip-arrow {
  position: absolute;
  bottom: -6px;
  right: 20px;
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 6px solid rgba(0, 0, 0, 0.9);
}

.tooltip-content {
  background: rgba(0, 0, 0, 0.9);
  color: white;
  padding: 12px 16px;
  border-radius: 8px;
  min-width: 200px;
  max-width: 300px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.tooltip-title {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #ffd700;
}

.tooltip-text {
  font-size: 13px;
  line-height: 1.5;
  word-wrap: break-word;
}

.card-intro {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-info {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.info-text {
  color: #666;
  font-size: 13px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-text.free {
  color: var(--color-primary, #c62828);
  font-weight: 500;
}

.card-footer {
  padding: 16px 20px;
  background: #f8f9fa;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rating-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-stars {
  display: flex;
  gap: 2px;
}

.star {
  font-size: 14px;
  opacity: 0.3;
  transition: opacity 0.2s;
}

.star.filled {
  opacity: 1;
}

.rating-text {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.rating-value {
  color: #ff9800;
  font-weight: 600;
  font-size: 14px;
}

.rating-count {
  color: #999;
  font-size: 12px;
}

.heat-section {
  display: flex;
  align-items: center;
  gap: 4px;
}

.heat-icon {
  font-size: 16px;
}

.heat-value {
  color: #ff4444;
  font-weight: 600;
  font-size: 14px;
}

.add-route-btn {
  padding: 6px 16px;
  border: 2px solid var(--color-primary, #c62828);
  background: white;
  color: var(--color-primary, #c62828);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.add-route-btn:hover:not(:disabled) {
  background: var(--color-primary, #c62828);
  color: white;
  transform: translateY(-1px);
}

.add-route-btn.added {
  background: var(--color-primary, #c62828);
  color: white;
  border-color: var(--color-primary, #c62828);
}

.add-route-btn.added:hover {
  background: #a02020;
  border-color: #a02020;
}

@media (max-width: 768px) {
  .card-image {
    height: 160px;
  }
  
  .card-title {
    font-size: 18px;
  }
  
  .card-content {
    padding: 16px;
  }
  
  .card-footer {
    padding: 12px 16px;
  }
}
</style>