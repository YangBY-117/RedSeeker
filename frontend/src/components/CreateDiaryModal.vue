<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h2 class="modal-title">撰写红色旅游日记</h2>
        <button class="modal-close" @click="emit('close')">×</button>
      </div>

      <form @submit.prevent="handleSubmit" class="diary-form">
        <!-- 标题 -->
        <div class="form-group">
          <label class="form-label">标题 *</label>
          <input
            v-model="formData.title"
            type="text"
            placeholder="请输入日记标题（如：参观中共一大会址有感）"
            required
            class="form-input"
          />
        </div>

        <!-- 内容 -->
        <div class="form-group">
          <label class="form-label">内容 *</label>
          <textarea
            v-model="formData.content"
            placeholder="记录您的红色之旅感悟，革命历史学习心得..."
            required
            rows="8"
            class="form-textarea"
          ></textarea>
        </div>

        <!-- 目的地 -->
        <div class="form-group">
          <label class="form-label">目的地</label>
          <input
            v-model="formData.destination"
            type="text"
            placeholder="如：上海市中共一大会址、井冈山革命根据地"
            class="form-input"
          />
        </div>

        <!-- 旅游日期 -->
        <div class="form-group">
          <label class="form-label">旅游日期</label>
          <input
            v-model="formData.travel_date"
            type="date"
            class="form-input"
          />
        </div>

        <!-- 关联景点 -->
        <div class="form-group">
          <label class="form-label">关联红色景点</label>
          <select
            v-model="formData.attraction_ids"
            multiple
            class="form-select"
            size="4"
          >
            <option
              v-for="attraction in attractionsList"
              :key="attraction.id"
              :value="attraction.id"
            >
              {{ attraction.name }}
            </option>
          </select>
          <p class="form-hint">按住Ctrl/Cmd键可多选红色景点</p>
        </div>

        <!-- 图片上传 -->
        <div class="form-group">
          <label class="form-label">红色印记图片</label>
          <div class="file-upload-area">
            <input
              ref="imageInput"
              type="file"
              accept="image/*"
              multiple
              @change="handleImageChange"
              class="file-input"
            />
            <button
              type="button"
              @click="$refs.imageInput.click()"
              class="btn btn-outline"
            >
              选择图片
            </button>
            <span class="file-count">{{ formData.images.length }} 张图片</span>
          </div>
          <div v-if="formData.images.length > 0" class="image-preview-grid">
            <div
              v-for="(image, index) in formData.images"
              :key="index"
              class="image-preview-item"
            >
              <img :src="getImageUrl(image)" :alt="`预览 ${index + 1}`" />
              <button
                type="button"
                @click="removeImage(index)"
                class="remove-btn"
              >
                ×
              </button>
            </div>
          </div>
        </div>

        <!-- 视频上传 -->
        <div class="form-group">
          <label class="form-label">红色记忆视频</label>
          <div class="file-upload-area">
            <input
              ref="videoInput"
              type="file"
              accept="video/*"
              multiple
              @change="handleVideoChange"
              class="file-input"
            />
            <button
              type="button"
              @click="$refs.videoInput.click()"
              class="btn btn-outline"
            >
              选择视频
            </button>
            <span class="file-count">{{ formData.videos.length }} 个视频</span>
          </div>
        </div>

        <!-- 错误提示 -->
        <div v-if="error" class="error-message">{{ error }}</div>

        <!-- 提交按钮 -->
        <div class="form-actions">
          <button
            type="button"
            @click="emit('close')"
            class="btn btn-outline"
          >
            取消
          </button>
          <button
            type="submit"
            :disabled="submitting"
            class="btn btn-primary"
          >
            {{ submitting ? '提交中...' : '发布红色日记' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { createDiary } from '../services/diaryService'
import { getRecommendations } from '../services/recommendService.js'

const emit = defineEmits(['close', 'created'])

const formData = ref({
  title: '',
  content: '',
  destination: '',
  travel_date: '',
  attraction_ids: [],
  images: [],
  videos: []
})

const attractionsList = ref([])
const submitting = ref(false)
const error = ref('')
const imageInput = ref(null)
const videoInput = ref(null)

// 加载景点列表
const loadAttractions = async () => {
  try {
    const result = await getRecommendations({
      page: 1,
      pageSize: 100
    })
    attractionsList.value = result.data.attractions || []
  } catch (err) {
    console.error('加载景点列表失败:', err)
  }
}

// 处理图片选择
const handleImageChange = (event) => {
  const files = Array.from(event.target.files)
  formData.value.images.push(...files)
  event.target.value = '' // 清空input，允许重复选择同一文件
}

// 处理视频选择
const handleVideoChange = (event) => {
  const files = Array.from(event.target.files)
  formData.value.videos.push(...files)
  event.target.value = ''
}

// 获取图片预览URL
const getImageUrl = (file) => {
  if (typeof file === 'string') return file
  return URL.createObjectURL(file)
}

// 移除图片
const removeImage = (index) => {
  const image = formData.value.images[index]
  if (image instanceof File) {
    URL.revokeObjectURL(URL.createObjectURL(image))
  }
  formData.value.images.splice(index, 1)
}

// 提交表单
const handleSubmit = async () => {
  if (!formData.value.title.trim() || !formData.value.content.trim()) {
    error.value = '请填写标题和内容'
    return
  }

  submitting.value = true
  error.value = ''

  try {
    // 构建FormData
    const formDataToSend = new FormData()
    formDataToSend.append('title', formData.value.title)
    formDataToSend.append('content', formData.value.content)
    if (formData.value.destination) {
      formDataToSend.append('destination', formData.value.destination)
    }
    if (formData.value.travel_date) {
      formDataToSend.append('travel_date', formData.value.travel_date)
    }
    if (formData.value.attraction_ids.length > 0) {
      formDataToSend.append('attraction_ids', JSON.stringify(formData.value.attraction_ids))
    }

    // 添加图片
    formData.value.images.forEach((image) => {
      formDataToSend.append('images', image)
    })

    // 添加视频
    formData.value.videos.forEach((video) => {
      formDataToSend.append('videos', video)
    })

    await createDiary(formDataToSend)
    emit('created')
  } catch (err) {
    console.error('创建日记失败:', err)
    error.value = err.response?.data?.message || '创建失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}


onMounted(() => {
  loadAttractions()
  // 设置默认日期为今天
  const today = new Date().toISOString().split('T')[0]
  formData.value.travel_date = today
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: var(--spacing-4);
}

.modal-content {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  width: 100%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: var(--shadow-xl);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-5);
  border-bottom: 1px solid var(--color-border);
  background: linear-gradient(to right, #dc2626, #b91c1c);
  color: white;
}

.modal-title {
  font-size: var(--font-size-xl);
  font-weight: 600;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: var(--font-size-2xl);
  color: white;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.modal-close:hover {
  background: rgba(255, 255, 255, 0.2);
}

.diary-form {
  padding: var(--spacing-5);
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
.form-textarea,
.form-select {
  width: 100%;
  padding: var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  font-family: inherit;
  transition: border-color 0.2s;
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  outline: none;
  border-color: var(--color-primary);
}

.form-textarea {
  resize: vertical;
  min-height: 150px;
}

.form-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-top: var(--spacing-1);
}

.file-upload-area {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.file-input {
  display: none;
}

.file-count {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.image-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: var(--spacing-2);
  margin-top: var(--spacing-3);
}

.image-preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.image-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-lg);
  line-height: 1;
}

.error-message {
  padding: var(--spacing-3);
  background: #fee;
  color: #c33;
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-4);
  font-size: var(--font-size-sm);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-3);
  margin-top: var(--spacing-6);
  padding-top: var(--spacing-4);
  border-top: 1px solid var(--color-border);
}
</style>