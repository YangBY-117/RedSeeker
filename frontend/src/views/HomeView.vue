<template>
  <div class="home">
    <!-- 封面图区域 -->
    <section class="cover-section">
      <div class="cover-overlay"></div>
      <div class="cover-content">
        <h1 class="cover-title">邮觅红途</h1>
        <p class="cover-subtitle">追寻红色足迹，传承革命精神</p>
        <p class="cover-description">
          基于个性化推荐的红色文化旅游服务平台<br>
          为您量身定制革命历史学习与旅游体验
        </p>
        <div class="cover-actions">
          <router-link to="/recommend" class="btn btn-primary btn-lg">
            <span class="btn-icon">🎯</span>
            开始个性化推荐
          </router-link>
          <button @click="openLoginModal" class="btn btn-outline btn-lg" type="button">
            <span class="btn-icon">👤</span>
            立即注册体验
          </button>
        </div>
      </div>
    </section>

    <!-- 特色功能 -->
    <section class="features-section">
      <div class="section-header">
        <h2>核心功能</h2>
        <p>为您提供全方位的红色旅游服务</p>
      </div>
      <div class="features-grid">
        <div class="feature-card" v-for="feature in features" :key="feature.id">
          <div class="feature-icon" :style="{ background: feature.color }">
            {{ feature.icon }}
          </div>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.description }}</p>
        </div>
      </div>
    </section>

    <!-- 热门红色景点 -->
    <section class="spots-section">
      <div class="section-header">
        <h2>热门红色景点</h2>
        <p>历史的见证，革命的精神</p>
      </div>
      <div class="spots-grid">
        <div class="spot-card" v-for="spot in hotSpots" :key="spot.id">
          <div class="spot-image">
            <img
              v-if="spot.image"
              class="spot-image-img"
              :src="spot.image"
              :alt="spot.name"
            />
            <div
              v-else
              class="spot-image-fallback"
              :style="{ background: spot.color }"
            ></div>
            <div class="spot-type">{{ spot.type }}</div>
          </div>
          <div class="spot-content">
            <h3>{{ spot.name }}</h3>
            <p class="spot-location">{{ spot.location }}</p>
            <p class="spot-desc">{{ spot.description }}</p>
            <div class="spot-tags">
              <span class="tag" v-for="tag in spot.tags" :key="tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 用户评价 -->
    <section class="testimonials-section">
      <div class="section-header">
        <h2>用户评价</h2>
        <p>真实用户的体验分享</p>
      </div>
      <div class="testimonials-grid">
        <div class="testimonial-card" v-for="testimonial in testimonials" :key="testimonial.id">
          <div class="testimonial-header">
            <div class="user-avatar">{{ testimonial.avatar }}</div>
            <div class="user-info">
              <h4>{{ testimonial.name }}</h4>
              <p>{{ testimonial.role }}</p>
            </div>
          </div>
          <div class="testimonial-content">
            <p>"{{ testimonial.text }}"</p>
          </div>
          <div class="testimonial-footer">
            <span class="testimonial-location">{{ testimonial.location }}</span>
            <span class="testimonial-rating">⭐ {{ testimonial.rating }}/5</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 系统优势 -->
    <section class="advantages-section">
      <div class="section-header">
        <h2>系统优势</h2>
        <p>为什么选择邮觅红途</p>
      </div>
      <div class="advantages-grid">
        <div class="advantage-card" v-for="advantage in advantages" :key="advantage.id">
          <h3>{{ advantage.title }}</h3>
          <p>{{ advantage.description }}</p>
        </div>
      </div>
    </section>
    
    <!-- 登录弹窗 -->
    <LoginModal v-model:visible="showLoginModal" @close="showLoginModal = false" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import LoginModal from '../components/LoginModal.vue'

const showLoginModal = ref(false)

const openLoginModal = () => {
  showLoginModal.value = true
}

// 特色功能数据
const features = ref([
  {
    id: 1,
    icon: '🎯',
    title: '智能推荐',
    description: '基于用户画像和历史行为，推荐最合适的红色旅游目的地',
    color: 'linear-gradient(135deg, var(--color-primary), var(--color-primary-dark))'
  },
  {
    id: 2,
    icon: '🗺️',
    title: '路线规划',
    description: '最优参观路线规划，包含交通、时长、讲解点',
    color: 'linear-gradient(135deg, var(--color-primary-light), var(--color-primary))'
  },
  {
    id: 3,
    icon: '🔍',
    title: '场所查询',
    description: '快速查找景点信息、开放时间、历史背景',
    color: 'linear-gradient(135deg, var(--color-primary-dark), var(--color-primary))'
  },
  {
    id: 4,
    icon: '📝',
    title: '旅游日记',
    description: '记录旅行点滴，图文并茂，支持分享和回顾',
    color: 'linear-gradient(135deg, var(--color-primary), var(--color-primary-light))'
  }
])

// 热门景点数据
const hotSpots = ref([
  {
    id: 1,
    name: '中共一大纪念馆',
    location: '上海',
    description: '中国共产党第一次全国代表大会会址，党的诞生地。',
    type: '纪念馆',
    image: '/attraction_images/中共一大纪念馆.jpg',
    color: 'linear-gradient(135deg, #ff6b6b, #c62828)',
    tags: ['建党历史', '红色教育']
  },
  {
    id: 2,
    name: '上海四行仓库抗战纪念馆',
    location: '上海',
    description: '四行仓库保卫战旧址，铭记抗战历史的重要场所。',
    type: '纪念馆',
    image: '/attraction_images/上海四行仓库抗战纪念馆.jpg',
    color: 'linear-gradient(135deg, #4caf50, #2e7d32)',
    tags: ['抗战历史', '城市记忆']
  },
  {
    id: 3,
    name: '西柏坡纪念馆',
    location: '河北石家庄',
    description: '解放战争时期党中央所在地，新中国从这里走来',
    type: '纪念馆',
    image: '/attraction_images/西柏坡纪念馆.jpg',
    color: 'linear-gradient(135deg, #2196f3, #0d47a1)',
    tags: ['党史教育', '会议旧址', '革命纪念地']
  },
  {
    id: 4,
    name: '遵义会议会址',
    location: '贵州遵义',
    description: '中国革命历史上的伟大转折点，具有重要历史意义',
    type: '会议会址',
    image: '/attraction_images/遵义会议纪念馆.jpg',
    color: 'linear-gradient(135deg, #9c27b0, #6a1b9a)',
    tags: ['历史转折', '会议旧址', '文物保护']
  }
])

// 用户评价数据
const testimonials = ref([
  {
    id: 1,
    name: '张老师',
    role: '历史教师',
    avatar: '👨‍🏫',
    rating: 5,
    text: '这个系统对我的教学工作帮助太大了！学生们通过个性化的路线规划，能更好地理解历史事件的关联性。',
    location: '北京'
  },
  {
    id: 2,
    name: '李同学',
    role: '大学生',
    avatar: '👨‍🎓',
    rating: 4.5,
    text: '作为00后，通过这个系统我对红色历史有了更深刻的认识。推荐算法真的很准，推荐的景点都很有意义。',
    location: '上海'
  },
  {
    id: 3,
    name: '王阿姨',
    role: '退休干部',
    avatar: '👵',
    rating: 4.8,
    text: '和子女一起用这个系统规划家庭旅行，操作简单，路线合理。旅游日记功能让我们能记录下珍贵的家庭回忆。',
    location: '西安'
  }
])

// 系统优势
const advantages = ref([
  {
    id: 1,
    title: '权威数据',
    description: '与各地红色旅游景点深度合作，提供最准确、最全面的景点信息'
  },
  {
    id: 2,
    title: '智能算法',
    description: '基于大数据和AI技术的个性化推荐，为您量身定制最佳旅游方案'
  },
  {
    id: 3,
    title: '多端同步',
    description: '支持PC、手机、平板多设备使用，数据实时同步，随时随地规划行程'
  },
  {
    id: 4,
    title: '安全保障',
    description: '严格的数据加密和隐私保护措施，确保您的个人信息安全'
  }
])
</script>

<style scoped>
.home {
  animation: fadeIn 0.8s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 封面图区域 */
.cover-section {
  position: relative;
  height: 500px;
  background: linear-gradient(rgba(0, 0, 0, 0.7), rgba(0, 0, 0, 0.5)),
              /* 这里替换为你的封面图URL */
              url('https://images.unsplash.com/photo-1513475382585-d06e58bcb0e0?ixlib=rb-1.2.1&auto=format&fit=crop&w=2070&q=80');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: white;
  margin-bottom: var(--spacing-12);
  border-radius: 0 0 var(--radius-2xl) var(--radius-2xl);
  overflow: hidden;
}

.cover-content {
  position: relative;
  z-index: 2;
  max-width: 800px;
  padding: var(--spacing-8);
}

.cover-title {
  font-size: 4rem;
  font-weight: 800;
  margin-bottom: var(--spacing-4);
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
}

.cover-subtitle {
  font-size: 1.8rem;
  margin-bottom: var(--spacing-6);
  color: rgba(255, 255, 255, 0.9);
}

.cover-description {
  font-size: 1.1rem;
  line-height: 1.8;
  margin-bottom: var(--spacing-8);
  color: rgba(255, 255, 255, 0.8);
}

.cover-actions {
  display: flex;
  gap: var(--spacing-4);
  justify-content: center;
  flex-wrap: wrap;
}

.btn-lg {
  padding: var(--spacing-4) var(--spacing-8);
  font-size: var(--font-size-lg);
}

.btn-outline {
  background: transparent;
  border: 2px solid white;
  color: white;
}

.btn-outline:hover {
  background: white;
  color: var(--color-primary);
}

.btn-icon {
  margin-right: var(--spacing-2);
}

/* 通用区块样式 */
.section-header {
  text-align: center;
  margin-bottom: var(--spacing-10);
}

.section-header h2 {
  font-size: var(--font-size-3xl);
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-3);
  position: relative;
  display: inline-block;
}

.section-header h2::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
}

.section-header p {
  color: var(--color-text-secondary);
  font-size: var(--font-size-lg);
}

/* 特色功能区域 */
.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-8);
  margin-bottom: var(--spacing-12);
}

.feature-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-8);
  text-align: center;
  transition: all var(--transition-normal);
  border: 1px solid rgba(198, 40, 40, 0.1);
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary-light);
}

.feature-icon {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  margin: 0 auto var(--spacing-6);
  color: white;
}

.feature-card h3 {
  font-size: var(--font-size-xl);
  margin-bottom: var(--spacing-3);
  color: var(--color-text-primary);
}

.feature-card p {
  color: var(--color-text-secondary);
  line-height: 1.6;
}

/* 热门景点区域 */
.spots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--spacing-8);
  margin-bottom: var(--spacing-12);
}

.spot-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  overflow: hidden;
  transition: all var(--transition-normal);
  border: 1px solid rgba(198, 40, 40, 0.1);
}

.spot-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary-light);
}

.spot-image {
  height: 200px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: white;
  overflow: hidden;
}

.spot-image-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.spot-image-fallback {
  width: 100%;
  height: 100%;
}

.spot-type {
  position: absolute;
  top: var(--spacing-4);
  right: var(--spacing-4);
  background: rgba(255, 255, 255, 0.9);
  color: var(--color-primary);
  padding: var(--spacing-1) var(--spacing-3);
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: 500;
}

.spot-content {
  padding: var(--spacing-6);
}

.spot-content h3 {
  font-size: var(--font-size-xl);
  margin-bottom: var(--spacing-2);
  color: var(--color-text-primary);
}

.spot-location {
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  margin-bottom: var(--spacing-3);
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
}

.spot-desc {
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-bottom: var(--spacing-4);
  font-size: var(--font-size-sm);
}

.spot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-2);
}

.tag {
  background: rgba(198, 40, 40, 0.1);
  color: var(--color-primary);
  padding: var(--spacing-1) var(--spacing-3);
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
}

/* 用户评价区域 */
.testimonials-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--spacing-8);
  margin-bottom: var(--spacing-12);
}

.testimonial-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  border: 1px solid rgba(198, 40, 40, 0.1);
}

.testimonial-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-4);
}

.user-avatar {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.user-info h4 {
  font-size: var(--font-size-base);
  margin-bottom: var(--spacing-1);
  color: var(--color-text-primary);
}

.user-info p {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.testimonial-content {
  margin-bottom: var(--spacing-4);
}

.testimonial-content p {
  color: var(--color-text-secondary);
  line-height: 1.6;
  font-style: italic;
}

.testimonial-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  padding-top: var(--spacing-4);
  font-size: var(--font-size-sm);
}

.testimonial-location {
  color: var(--color-primary);
}

.testimonial-rating {
  color: var(--color-accent);
  font-weight: 500;
}

/* 系统优势区域 */
.advantages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-8);
  margin-bottom: var(--spacing-12);
}

.advantage-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-8);
  text-align: center;
  border: 1px solid rgba(198, 40, 40, 0.1);
}

.advantage-card h3 {
  font-size: var(--font-size-lg);
  color: var(--color-primary);
  margin-bottom: var(--spacing-3);
}

.advantage-card p {
  color: var(--color-text-secondary);
  line-height: 1.6;
  font-size: var(--font-size-sm);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .cover-section {
    height: 400px;
    border-radius: 0 0 var(--radius-xl) var(--radius-xl);
  }
  
  .cover-title {
    font-size: 2.5rem;
  }
  
  .cover-subtitle {
    font-size: 1.2rem;
  }
  
  .cover-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .btn-lg {
    width: 100%;
    max-width: 300px;
  }
  
  .features-grid,
  .spots-grid,
  .testimonials-grid,
  .advantages-grid {
    grid-template-columns: 1fr;
  }
}
</style>