<template>
  <div class="user-center-view">
    <div class="user-header">
      <h1 class="page-title">用户中心</h1>
      <div class="user-info-card">
        <div class="user-avatar-large" :style="headerAvatarStyle"></div>
        <div class="user-details">
          <h2 class="user-name">{{ user?.username || '游客' }}</h2>
          <p class="user-meta">ID: {{ user?.id || '-' }}</p>
        </div>
      </div>
    </div>

    <!-- 标签页 -->
    <div class="tabs">
      <button
        :class="['tab-btn', { active: activeTab === 'settings' }]"
        @click="activeTab = 'settings'"
      >
        ⚙️ 个人设置
      </button>
      <button
        v-if="user?.isAdmin"
        :class="['tab-btn', { active: activeTab === 'admin' }]"
        @click="activeTab = 'admin'"
      >
        🔧 管理员
      </button>
    </div>

    <!-- 个人设置 -->
    <div v-if="activeTab === 'settings'" class="tab-content">
      <div class="content-header">
        <h2 class="section-title">个人设置</h2>
      </div>

      <div class="settings-form">
        <div class="form-group">
          <label class="form-label">头像</label>
          <div class="avatar-upload">
            <div class="avatar-preview" :style="avatarStyle">
            </div>
            <div class="avatar-upload-controls">
              <input
                type="file"
                ref="avatarInput"
                @change="handleAvatarChange"
                accept="image/*"
                class="avatar-file-input"
                id="avatar-upload"
              />
              <label for="avatar-upload" class="btn btn-secondary btn-sm">
                {{ avatarFile ? '更换头像' : '选择头像' }}
              </label>
              <span v-if="avatarFile" class="avatar-file-name">{{ avatarFile.name }}</span>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">用户名</label>
          <input
            type="text"
            v-model="editForm.username"
            :placeholder="user?.username || ''"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label class="form-label">新密码</label>
          <input
            type="password"
            v-model="editForm.password"
            placeholder="留空则不修改密码"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label class="form-label">确认密码</label>
          <input
            type="password"
            v-model="editForm.confirmPassword"
            placeholder="再次输入新密码"
            class="form-input"
          />
        </div>

        <div class="form-actions">
          <button class="btn btn-primary" @click="saveProfile" :disabled="saving">
            {{ saving ? '保存中...' : '保存修改' }}
          </button>
          <button class="btn btn-secondary" @click="resetForm">重置</button>
        </div>

        <div v-if="saveMessage" :class="['message', saveMessageType]">
          {{ saveMessage }}
        </div>
      </div>
    </div>

    <!-- 管理员功能 -->
    <div v-if="activeTab === 'admin' && user?.isAdmin" class="tab-content">
      <div class="content-header">
        <h2 class="section-title">管理员功能</h2>
      </div>

      <div class="admin-panel">
        <div class="admin-warning">
          <p>⚠️ 危险操作：以下操作不可恢复</p>
        </div>

        <div class="admin-actions">
          <button class="btn btn-danger" @click="confirmDeleteAllDiaries" :disabled="deleting">
            {{ deleting ? '删除中...' : '🗑️ 删除所有日记' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { getDiaryList } from '../services/diaryService'
import { updateProfile, deleteAllDiaries, uploadAvatar } from '../services/userService'
import DiaryCard from '../components/DiaryCard.vue'

const router = useRouter()
const { user, isAuthenticated, getCurrentUser } = useAuth()

const activeTab = ref('settings')

// 日记相关
const myDiaries = ref([])
const diariesLoading = ref(false)
const diaryPage = ref(1)
const diaryPageSize = ref(12)
const diaryTotalPages = ref(1)

// 路线相关
const historyRoutes = ref([])
const routesLoading = ref(false)

// 个人设置相关
const editForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  avatar: ''
})
const avatarInput = ref(null)
const avatarFile = ref(null)
const avatarPreview = ref(null)
const saving = ref(false)
const saveMessage = ref('')
const saveMessageType = ref('')

// 管理员相关
const deleting = ref(false)

// 默认头像路径
const DEFAULT_AVATAR = '/生成系统头像.png'

// 获取用户首字母
const getUserInitial = () => {
  if (!user.value || !user.value.username) return '👤'
  return user.value.username.charAt(0).toUpperCase()
}

// 头像样式（用于设置表单）
const avatarStyle = computed(() => {
  let avatarUrl = DEFAULT_AVATAR
  if (avatarPreview.value) {
    avatarUrl = avatarPreview.value
  } else if (user.value?.avatar) {
    // 如果是相对路径，需要加上API基础URL
    if (user.value.avatar.startsWith('/uploads/')) {
      const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
      avatarUrl = apiBase.replace(/\/api\/?$/, '') + user.value.avatar
    } else if (user.value.avatar.startsWith('http://') || user.value.avatar.startsWith('https://')) {
      avatarUrl = user.value.avatar
    } else {
      // 其他情况，尝试作为相对路径处理
      avatarUrl = user.value.avatar
    }
  }
  return {
    backgroundImage: `url(${avatarUrl})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    backgroundColor: '#f0f0f0' // 添加背景色，避免白色背景时看不到
  }
})

// 处理头像文件选择
const handleAvatarChange = (event) => {
  const file = event.target.files?.[0]
  if (file) {
    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      saveMessage.value = '请选择图片文件'
      saveMessageType.value = 'error'
      return
    }
    // 验证文件大小（限制为5MB）
    if (file.size > 5 * 1024 * 1024) {
      saveMessage.value = '图片大小不能超过5MB'
      saveMessageType.value = 'error'
      return
    }
    avatarFile.value = file
    // 创建预览
    const reader = new FileReader()
    reader.onload = (e) => {
      avatarPreview.value = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

// 头像样式（用于头部显示）
const headerAvatarStyle = computed(() => {
  let avatarUrl = DEFAULT_AVATAR
  if (user.value?.avatar) {
    // 如果是相对路径，需要加上API基础URL
    if (user.value.avatar.startsWith('/uploads/')) {
      const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
      avatarUrl = apiBase.replace(/\/api\/?$/, '') + user.value.avatar
    } else if (user.value.avatar.startsWith('http://') || user.value.avatar.startsWith('https://')) {
      avatarUrl = user.value.avatar
    } else {
      // 其他情况，尝试作为相对路径处理
      avatarUrl = user.value.avatar
    }
  }
  return {
    backgroundImage: `url(${avatarUrl})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    backgroundColor: '#f0f0f0' // 添加背景色，避免白色背景时看不到
  }
})

// 重置表单
const resetForm = () => {
  editForm.value = {
    username: user.value?.username || '',
    password: '',
    confirmPassword: '',
    avatar: user.value?.avatar || ''
  }
  avatarFile.value = null
  avatarPreview.value = null
  if (avatarInput.value) {
    avatarInput.value.value = ''
  }
  saveMessage.value = ''
}

// 保存个人资料
const saveProfile = async () => {
  if (editForm.value.password && editForm.value.password !== editForm.value.confirmPassword) {
    saveMessage.value = '两次输入的密码不一致'
    saveMessageType.value = 'error'
    return
  }

  saving.value = true
  saveMessage.value = ''
  try {
    let avatarUrl = user.value?.avatar
    
    // 如果有新头像文件，先上传
    if (avatarFile.value) {
      avatarUrl = await uploadAvatar(user.value.id, avatarFile.value)
    }
    
    // 更新用户信息
    const updateData = {
      userId: user.value.id
    }
    if (editForm.value.username && editForm.value.username !== user.value.username) {
      updateData.username = editForm.value.username
    }
    if (editForm.value.password) {
      updateData.password = editForm.value.password
    }
    if (avatarUrl) {
      updateData.avatar = avatarUrl
    }

    const updatedUser = await updateProfile(updateData)
    // 更新用户信息，确保头像URL正确
    user.value = {
      ...user.value,
      ...updatedUser,
      avatar: avatarUrl || updatedUser.avatar || user.value.avatar // 确保使用最新的头像URL
    }
    // 强制刷新用户信息，确保头像更新
    try {
      await getCurrentUser()
    } catch (e) {
      console.warn('刷新用户信息失败:', e)
    }
    saveMessage.value = '保存成功！'
    saveMessageType.value = 'success'
    
    // 清空表单
    editForm.value.password = ''
    editForm.value.confirmPassword = ''
    avatarFile.value = null
    avatarPreview.value = null
    if (avatarInput.value) {
      avatarInput.value.value = ''
    }
  } catch (error) {
    console.error('保存失败:', error)
    saveMessage.value = error.response?.data?.message || error.message || '保存失败，请重试'
    saveMessageType.value = 'error'
  } finally {
    saving.value = false
  }
}

// 确认删除所有日记
const confirmDeleteAllDiaries = () => {
  if (confirm('⚠️ 警告：此操作将删除所有用户的日记，且不可恢复！\n\n确定要继续吗？')) {
    handleDeleteAllDiaries()
  }
}

// 删除所有日记
const handleDeleteAllDiaries = async () => {
  deleting.value = true
  try {
    await deleteAllDiaries()
    alert('所有日记已删除')
    // 刷新日记列表
    if (activeTab.value === 'diaries') {
      loadMyDiaries(diaryPage.value)
    }
  } catch (error) {
    console.error('删除失败:', error)
    alert(error.response?.data?.message || '删除失败，请重试')
  } finally {
    deleting.value = false
  }
}

// 加载我的日记
const loadMyDiaries = async (page = 1) => {
  if (!isAuthenticated.value || !user.value) return

  diariesLoading.value = true
  try {
    // 如果是管理员，不传 userId 可以查看所有日记
    // 如果是普通用户，传 userId 只能查看自己的日记
    const queryParams = {
      page,
      pageSize: diaryPageSize.value,
      sortBy: 'time'
    }
    
    // 只有管理员可以查看所有日记，普通用户只能查看自己的
    if (!user.value.isAdmin) {
      queryParams.userId = user.value.id
    }
    
    const response = await getDiaryList(queryParams)

    myDiaries.value = response.diaries || []
    diaryTotalPages.value = response.totalPages || 1
    diaryPage.value = response.page || page
  } catch (error) {
    console.error('加载我的日记失败:', error)
    myDiaries.value = []
  } finally {
    diariesLoading.value = false
  }
}

// 加载历史路线
const loadHistoryRoutes = async () => {
  if (!isAuthenticated.value || !user.value) return

  routesLoading.value = true
  try {
    // TODO: 调用后端接口获取历史路线
    // const response = await api.get(`/route/history?userId=${user.value.id}`)
    // historyRoutes.value = response.data.data || []
    
    // 暂时使用空数组，等待后端接口实现
    historyRoutes.value = []
  } catch (error) {
    console.error('加载历史路线失败:', error)
    historyRoutes.value = []
  } finally {
    routesLoading.value = false
  }
}

// 跳转到创建日记
const goToCreateDiary = () => {
  router.push('/diary')
  // 触发创建日记弹窗（需要在 DiaryView 中处理）
  setTimeout(() => {
    window.dispatchEvent(new CustomEvent('open-create-diary'))
  }, 100)
}

// 跳转到路线规划
const goToRoutePlanning = () => {
  router.push('/route')
}

// 查看日记详情
const viewDiary = (id) => {
  router.push(`/diary/${id}`)
}

// 查看路线详情
const viewRoute = (id) => {
  router.push(`/route?routeId=${id}`)
}

// 分页
const goToDiaryPage = (page) => {
  if (page >= 1 && page <= diaryTotalPages.value) {
    loadMyDiaries(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const handleDiaryDeleted = (deletedId) => {
  myDiaries.value = myDiaries.value.filter(diary => diary.id !== deletedId)
  if (myDiaries.value.length === 0 && diaryPage.value > 1) {
    goToDiaryPage(diaryPage.value - 1)
  }
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

// 格式化距离
const formatDistance = (meters) => {
  if (!meters) return '0 米'
  if (meters < 1000) return `${meters} 米`
  return `${(meters / 1000).toFixed(1)} 公里`
}

// 格式化时长
const formatDuration = (seconds) => {
  if (!seconds) return '0 分钟'
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  if (hours > 0) {
    return `${hours} 小时 ${minutes % 60} 分钟`
  }
  return `${minutes} 分钟`
}

onMounted(() => {
  if (isAuthenticated.value && user.value) {
    resetForm()
  } else {
    router.push('/')
  }
})
</script>

<style scoped>
.user-center-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-6);
  min-height: 100vh;
}

.user-header {
  margin-bottom: var(--spacing-6);
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-primary);
  margin: 0 0 var(--spacing-4) 0;
}

.user-info-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  background: white;
  padding: var(--spacing-5);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.user-avatar-large {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: var(--font-size-3xl);
  font-weight: bold;
  flex-shrink: 0;
}

.user-details {
  flex: 1;
}

.user-name {
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--spacing-1) 0;
}

.user-meta {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

/* 标签页 */
.tabs {
  display: flex;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-6);
  border-bottom: 2px solid var(--color-border);
}

.tab-btn {
  padding: var(--spacing-3) var(--spacing-5);
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: -2px;
}

.tab-btn:hover {
  color: var(--color-primary);
}

.tab-btn.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

/* 内容区域 */
.tab-content {
  background: white;
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-md);
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-5);
}

.section-title {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: var(--spacing-8);
  color: var(--color-text-secondary);
}

.empty-state button {
  margin-top: var(--spacing-3);
}

.diaries-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-4);
}

/* 路线列表 */
.routes-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.route-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  cursor: pointer;
  transition: all 0.2s;
}

.route-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.route-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-3);
}

.route-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.route-date {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.route-info {
  display: flex;
  gap: var(--spacing-4);
  flex-wrap: wrap;
}

.info-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.info-icon {
  font-size: var(--font-size-base);
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-4);
  margin-top: var(--spacing-6);
  padding-top: var(--spacing-4);
  border-top: 1px solid var(--color-border);
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
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 按钮样式 */
.btn {
  padding: var(--spacing-3) var(--spacing-5);
  border-radius: var(--radius-md);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  font-size: var(--font-size-base);
}

.btn-primary {
  background: linear-gradient(to right, #dc2626, #b91c1c);
  color: white;
}

.btn-primary:hover {
  background: linear-gradient(to right, #b91c1c, #991b1b);
  transform: translateY(-1px);
}

.btn-secondary {
  background: var(--color-bg);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover {
  background: var(--color-surface);
}

.btn-danger {
  background: #dc2626;
  color: white;
}

.btn-danger:hover {
  background: #b91c1c;
}

.btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 设置表单 */
.settings-form {
  max-width: 600px;
}

.form-group {
  margin-bottom: var(--spacing-5);
}

.form-label {
  display: block;
  font-weight: 500;
  margin-bottom: var(--spacing-2);
  color: var(--color-text);
}

.form-input {
  width: 100%;
  padding: var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
}

.avatar-upload-controls {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
  flex: 1;
}

.avatar-file-input {
  display: none;
}

.btn-sm {
  padding: var(--spacing-2) var(--spacing-4);
  font-size: var(--font-size-sm);
}

.avatar-file-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-left: var(--spacing-2);
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: var(--font-size-2xl);
  font-weight: bold;
  flex-shrink: 0;
}

.form-actions {
  display: flex;
  gap: var(--spacing-3);
  margin-top: var(--spacing-6);
}

.message {
  margin-top: var(--spacing-4);
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
}

.message.success {
  background: #d1fae5;
  color: #065f46;
}

.message.error {
  background: #fee2e2;
  color: #991b1b;
}

/* 管理员面板 */
.admin-panel {
  max-width: 600px;
}

.admin-warning {
  background: #fef3c7;
  border: 1px solid #fbbf24;
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  margin-bottom: var(--spacing-5);
}

.admin-warning p {
  margin: 0;
  color: #92400e;
  font-weight: 500;
}

.admin-actions {
  display: flex;
  gap: var(--spacing-3);
}

/* 响应式 */
@media (max-width: 768px) {
  .user-center-view {
    padding: var(--spacing-4);
  }

  .user-info-card {
    flex-direction: column;
    text-align: center;
  }

  .content-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-3);
  }

  .diaries-grid {
    grid-template-columns: 1fr;
  }
}
</style>
