import { api } from './api'

/**
 * 上传头像
 * @param {number} userId - 用户ID
 * @param {File} avatarFile - 头像文件
 * @returns {Promise<string>} 头像URL
 */
export async function uploadAvatar(userId, avatarFile) {
  const formData = new FormData()
  formData.append('userId', userId)
  formData.append('avatar', avatarFile)
  
  const response = await api.post('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  return response.data.data
}

/**
 * 更新用户资料
 * @param {Object} data - 用户资料数据 { userId, username?, password?, avatar? }
 * @returns {Promise<Object>} 更新后的用户信息
 */
export async function updateProfile(data) {
  const response = await api.put('/user/profile', data)
  return response.data.data
}

/**
 * 管理员删除所有日记
 * @returns {Promise<Boolean>}
 */
export async function deleteAllDiaries() {
  const response = await api.delete('/diary/admin/delete-all')
  return response.data.data
}
