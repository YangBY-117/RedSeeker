<template>
  <div v-if="visible" class="ai-assistant-panel">
    <div class="panel-header">
      <h3 class="panel-title">🤖 AI助手</h3>
      <button class="panel-close" @click="close">×</button>
    </div>

    <div class="panel-tabs">
      <button
        :class="['tab-btn', { active: activeTab === 'write' }]"
        @click="activeTab = 'write'"
      >
        ✍️ 写日记
      </button>
      <button
        :class="['tab-btn', { active: activeTab === 'text2image' }]"
        @click="activeTab = 'text2image'"
      >
        🎨 文生图
      </button>
      <button
        :class="['tab-btn', { active: activeTab === 'image2video' }]"
        @click="activeTab = 'image2video'"
      >
        🎬 图生动画
      </button>
    </div>

    <div class="panel-content">
      <!-- AI写日记 -->
      <div v-if="activeTab === 'write'" class="tab-content">
        <div class="input-group">
          <label class="input-label">描述您的红色之旅</label>
          <textarea
            v-model="writePrompt"
            placeholder="例如：今天参观了中共一大会址，感受到了革命先烈的伟大精神..."
            rows="4"
            class="input-textarea"
          ></textarea>
        </div>
        <button
          class="btn btn-primary btn-generate"
          :disabled="generatingWrite"
          @click="handleGenerateDiary"
        >
          {{ generatingWrite ? '生成中...' : '生成日记内容' }}
        </button>
        <div v-if="generatedContent" class="result-box">
          <div class="result-header">
            <span>生成的内容：</span>
            <div class="result-actions">
              <button class="btn-copy" @click="copyToClipboard(generatedContent)">复制</button>
              <button class="btn-use" @click="handleUseContent">使用</button>
            </div>
          </div>
          <div class="result-content">{{ generatedContent }}</div>
        </div>
      </div>

      <!-- 文生图 -->
      <div v-if="activeTab === 'text2image'" class="tab-content">
        <div class="input-group">
          <label class="input-label">描述您想要的图片</label>
          <textarea
            v-model="imagePrompt"
            placeholder="例如：红色革命纪念馆，庄严肃穆，阳光洒在纪念碑上..."
            rows="4"
            class="input-textarea"
          ></textarea>
        </div>
        <button
          class="btn btn-primary btn-generate"
          :disabled="generatingImage"
          @click="handleGenerateImage"
        >
          {{ generatingImage ? '生成中...' : '生成图片' }}
        </button>
        <div v-if="generatedImageUrl" class="result-box">
          <div class="result-header">
            <span>生成的图片：</span>
            <button class="btn-copy" @click="handleUseImage">使用此图片</button>
          </div>
          <div class="image-result">
            <img :src="generatedImageUrl" alt="生成的图片" />
          </div>
        </div>
      </div>

      <!-- 图生动画 -->
      <div v-if="activeTab === 'image2video'" class="tab-content">
        <div class="input-group">
          <label class="input-label">选择图片</label>
          <div v-if="selectedImages.length === 0" class="empty-hint">
            请先在左侧选择或上传图片
          </div>
          <div v-else class="selected-images">
            <div
              v-for="(img, index) in selectedImages"
              :key="index"
              class="selected-image-item"
            >
              <img :src="getImagePreview(img)" alt="图片" />
              <span class="image-index">{{ index + 1 }}</span>
            </div>
          </div>
        </div>
        <div class="input-group">
          <label class="input-label">描述（可选）</label>
          <textarea
            v-model="videoDescription"
            placeholder="描述您想要的动画效果..."
            rows="3"
            class="input-textarea"
          ></textarea>
        </div>
        <button
          class="btn btn-primary btn-generate"
          :disabled="generatingVideo || selectedImages.length === 0"
          @click="handleGenerateVideo"
        >
          {{ generatingVideo ? '生成中...' : '生成动画' }}
        </button>
        <div v-if="videoTaskId" class="result-box">
          <div class="result-header">
            <span>生成状态：</span>
            <span :class="['status-badge', getStatusClass(videoStatus)]">
              {{ getStatusText(videoStatus) }}
            </span>
          </div>
          <div v-if="videoStatus === 'processing'" class="progress-info">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: '60%' }"></div>
            </div>
            <p class="progress-text">正在生成动画，请稍候...</p>
          </div>
          <div v-if="videoStatus === 'completed' && videoUrl" class="video-result">
            <video :src="videoUrl" controls></video>
            <button class="btn btn-primary" @click="handleUseVideo">使用此视频</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { generateDiaryContent, generateImageFromText, generateAnimationFromImages } from '../services/aiService'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  selectedImages: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'useContent', 'useImage', 'useVideo'])

// 标签页
const activeTab = ref('write')

// 写日记相关
const writePrompt = ref('')
const generatingWrite = ref(false)
const generatedContent = ref('')

// 文生图相关
const imagePrompt = ref('')
const generatingImage = ref(false)
const generatedImageUrl = ref('')

// 图生动画相关
const videoDescription = ref('')
const generatingVideo = ref(false)
const videoTaskId = ref('')
const videoStatus = ref('')
const videoUrl = ref('')

// 关闭面板
const close = () => {
  emit('close')
}

// 生成日记内容
const handleGenerateDiary = async () => {
  if (!writePrompt.value.trim()) {
    alert('请输入描述')
    return
  }

  generatingWrite.value = true
  generatedContent.value = ''

  try {
    const result = await generateDiaryContent({
      prompt: writePrompt.value,
      destination: '',
      travel_date: ''
    })
    generatedContent.value = result.content
  } catch (error) {
    console.error('生成日记内容失败:', error)
    alert(error.response?.data?.message || '生成失败，请稍后重试')
  } finally {
    generatingWrite.value = false
  }
}

// 生成图片
const handleGenerateImage = async () => {
  if (!imagePrompt.value.trim()) {
    alert('请输入图片描述')
    return
  }

  generatingImage.value = true
  generatedImageUrl.value = ''

  try {
    const result = await generateImageFromText({
      prompt: imagePrompt.value
    })
    generatedImageUrl.value = result.image_url
  } catch (error) {
    console.error('生成图片失败:', error)
    alert(error.response?.data?.message || '生成失败，请稍后重试')
  } finally {
    generatingImage.value = false
  }
}

// 生成动画
const handleGenerateVideo = async () => {
  if (props.selectedImages.length === 0) {
    alert('请先选择图片')
    return
  }

  generatingVideo.value = true
  videoTaskId.value = ''
  videoStatus.value = ''
  videoUrl.value = ''

  try {
    // 将图片转换为base64或URL
    // 如果是File对象，需要转换为base64或先上传到服务器
    const imageData = await Promise.all(
      props.selectedImages.map(async (img) => {
        if (img instanceof File) {
          // 转换为base64
          return new Promise((resolve, reject) => {
            const reader = new FileReader()
            reader.onload = () => resolve(reader.result)
            reader.onerror = reject
            reader.readAsDataURL(img)
          })
        }
        return img
      })
    )

    const result = await generateAnimationFromImages({
      images: imageData,
      description: videoDescription.value
    })

    videoTaskId.value = result.task_id
    videoStatus.value = result.status

    if (result.status === 'completed' && result.video_url) {
      videoUrl.value = result.video_url
    } else if (result.status === 'processing') {
      // 开始轮询状态
      checkVideoStatus()
    }
  } catch (error) {
    console.error('生成动画失败:', error)
    alert(error.response?.data?.message || '生成失败，请稍后重试')
  } finally {
    generatingVideo.value = false
  }
}

// 检查视频生成状态
const checkVideoStatus = async () => {
  if (!videoTaskId.value) return

  try {
    const { getAnimationStatus } = await import('../services/diaryService')
    const result = await getAnimationStatus(videoTaskId.value)
    videoStatus.value = result.status

    if (result.status === 'completed' && result.video_url) {
      videoUrl.value = result.video_url
    } else if (result.status === 'processing' || result.status === 'waiting') {
      setTimeout(() => {
        checkVideoStatus()
      }, 3000)
    }
  } catch (error) {
    console.error('查询动画状态失败:', error)
  }
}

// 使用生成的内容
const handleUseContent = () => {
  if (generatedContent.value) {
    emit('useContent', generatedContent.value)
  }
}

// 使用生成的图片
const handleUseImage = () => {
  if (generatedImageUrl.value) {
    emit('useImage', generatedImageUrl.value)
  }
}

// 使用生成的视频
const handleUseVideo = () => {
  if (videoUrl.value) {
    emit('useVideo', videoUrl.value)
  }
}

// 复制到剪贴板
const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    alert('已复制到剪贴板')
  }).catch(() => {
    alert('复制失败')
  })
}

// 获取图片预览
const getImagePreview = (img) => {
  if (img instanceof File) {
    return URL.createObjectURL(img)
  }
  return img
}

// 获取状态样式
const getStatusClass = (status) => {
  const statusMap = {
    'processing': 'status-processing',
    'completed': 'status-completed',
    'failed': 'status-failed'
  }
  return statusMap[status] || ''
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'processing': '生成中',
    'completed': '已完成',
    'failed': '生成失败'
  }
  return statusMap[status] || '未知'
}

// 监听生成的内容，自动使用
watch(generatedContent, (newVal) => {
  if (newVal) {
    // 可以自动填充，或者显示使用按钮
  }
})
</script>

<style scoped>
.ai-assistant-panel {
  position: fixed;
  right: 20px;
  bottom: 20px;
  width: 400px;
  max-height: 600px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 2000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-4);
  background: linear-gradient(135deg, #c62828, #8e0000);
  color: white;
}

.panel-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  margin: 0;
}

.panel-close {
  background: none;
  border: none;
  color: white;
  font-size: var(--font-size-2xl);
  cursor: pointer;
  padding: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  transition: background 0.2s;
}

.panel-close:hover {
  background: rgba(255, 255, 255, 0.2);
}

.panel-tabs {
  display: flex;
  border-bottom: 1px solid var(--color-border);
  background: #f5f5f5;
}

.tab-btn {
  flex: 1;
  padding: var(--spacing-3);
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: var(--font-size-sm);
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
}

.tab-btn:hover {
  background: rgba(198, 40, 40, 0.1);
}

.tab-btn.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  background: white;
  font-weight: 600;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-4);
}

.tab-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.input-label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text);
}

.input-textarea {
  width: 100%;
  padding: var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  font-family: inherit;
  resize: vertical;
  min-height: 80px;
}

.input-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

.btn {
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: var(--radius-md);
  font-weight: 500;
  font-size: var(--font-size-base);
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.btn-primary {
  background: var(--color-primary);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(198, 40, 40, 0.3);
}

.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-generate {
  width: 100%;
}

.result-box {
  margin-top: var(--spacing-4);
  padding: var(--spacing-3);
  background: #f9f9f9;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-2);
  font-size: var(--font-size-sm);
  font-weight: 500;
}

.result-actions {
  display: flex;
  gap: var(--spacing-2);
}

.btn-copy,
.btn-use {
  padding: var(--spacing-1) var(--spacing-3);
  background: var(--color-primary);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  cursor: pointer;
  transition: background 0.2s;
}

.btn-copy:hover,
.btn-use:hover {
  background: var(--color-primary-dark);
}

.result-content {
  padding: var(--spacing-2);
  background: white;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  line-height: 1.6;
  white-space: pre-wrap;
  max-height: 200px;
  overflow-y: auto;
}

.image-result {
  margin-top: var(--spacing-2);
}

.image-result img {
  width: 100%;
  border-radius: var(--radius-sm);
}

.selected-images {
  display: flex;
  gap: var(--spacing-2);
  flex-wrap: wrap;
}

.selected-image-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 2px solid var(--color-primary);
}

.selected-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-index {
  position: absolute;
  top: 4px;
  left: 4px;
  background: var(--color-primary);
  color: white;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.empty-hint {
  padding: var(--spacing-4);
  text-align: center;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  background: #f5f5f5;
  border-radius: var(--radius-md);
}

.status-badge {
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 500;
}

.status-processing {
  background: #fff3cd;
  color: #856404;
}

.status-completed {
  background: #d4edda;
  color: #155724;
}

.status-failed {
  background: #f8d7da;
  color: #721c24;
}

.progress-info {
  margin-top: var(--spacing-2);
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #e0e0e0;
  border-radius: var(--radius-full);
  overflow: hidden;
  margin-bottom: var(--spacing-2);
}

.progress-fill {
  height: 100%;
  background: var(--color-primary);
  transition: width 0.3s;
}

.progress-text {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  text-align: center;
}

.video-result {
  margin-top: var(--spacing-2);
}

.video-result video {
  width: 100%;
  border-radius: var(--radius-sm);
  margin-bottom: var(--spacing-2);
}

@media (max-width: 768px) {
  .ai-assistant-panel {
    right: 10px;
    bottom: 10px;
    width: calc(100vw - 20px);
    max-width: 400px;
  }
}
</style>
