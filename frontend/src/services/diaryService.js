import { api } from './api'

/**
 * 旅游日记服务
 */

/**
 * 获取日记列表（推荐）
 * @param {Object} params - 查询参数
 * @param {string} params.sortBy - 排序方式: 'heat' | 'rating' | 'time'
 * @param {string} params.destination - 目的地筛选
 * @param {number} params.userId - 用户ID（用于个性化推荐）
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 * @returns {Promise<Object>} 日记列表
 */
export async function getDiaryList(params = {}) {
  const { sortBy = 'heat', destination, userId, page = 1, pageSize = 10 } = params
  const queryParams = new URLSearchParams()
  if (sortBy) queryParams.append('sortBy', sortBy)
  if (destination) queryParams.append('destination', destination)
  if (userId) queryParams.append('userId', userId)
  queryParams.append('page', page)
  queryParams.append('pageSize', pageSize)

  const response = await api.get(`/diary/list?${queryParams}`)
  return response.data.data
}

/**
 * 按目的地搜索日记
 * @param {Object} params - 搜索参数
 * @param {string} params.destination - 目的地关键词
 * @param {string} params.sortBy - 排序方式: 'heat' | 'rating'
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 * @returns {Promise<Object>} 搜索结果
 */
export async function searchDiaryByDestination(params) {
  const { destination, sortBy = 'heat', page = 1, pageSize = 10 } = params
  const queryParams = new URLSearchParams()
  queryParams.append('destination', destination)
  queryParams.append('sortBy', sortBy)
  queryParams.append('page', page)
  queryParams.append('pageSize', pageSize)

  const response = await api.get(`/diary/search-by-destination?${queryParams}`)
  return response.data.data
}

/**
 * 按名称精确查询
 * @param {string} title - 日记标题
 * @returns {Promise<Object>} 日记详情
 */
export async function searchDiaryByName(title) {
  const response = await api.get(`/diary/search-by-name?title=${encodeURIComponent(title)}`)
  return response.data.data
}

/**
 * 全文检索
 * @param {Object} params - 搜索参数
 * @param {string} params.keyword - 搜索关键词
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 * @returns {Promise<Object>} 搜索结果
 */
export async function fulltextSearch(params) {
  const { keyword, page = 1, pageSize = 10 } = params
  const queryParams = new URLSearchParams()
  queryParams.append('keyword', keyword)
  queryParams.append('page', page)
  queryParams.append('pageSize', pageSize)

  const response = await api.get(`/diary/fulltext-search?${queryParams}`)
  return response.data.data
}

/**
 * 获取日记详情
 * @param {number} id - 日记ID
 * @returns {Promise<Object>} 日记详情
 */
export async function getDiaryDetail(id) {
  const response = await api.get(`/diary/${id}`)
  return response.data.data
}

/**
 * 创建日记
 * @param {FormData} formData - 表单数据（包含title, content, destination, travel_date, attraction_ids, images, videos）
 * @returns {Promise<Object>} 创建结果
 */
export async function createDiary(formData) {
  const response = await api.post('/diary/create', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  return response.data.data
}

/**
 * 更新日记
 * @param {number} id - 日记ID
 * @param {FormData} formData - 表单数据
 * @returns {Promise<Object>} 更新结果
 */
export async function updateDiary(id, formData) {
  const response = await api.put(`/diary/${id}`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  return response.data.data
}

/**
 * 删除日记
 * @param {number} id - 日记ID
 * @returns {Promise<void>}
 */
export async function deleteDiary(id) {
  await api.delete(`/diary/${id}`)
}

/**
 * 评分日记
 * @param {number} id - 日记ID
 * @param {number} rating - 评分（1-5）
 * @returns {Promise<Object>} 评分结果
 */
export async function rateDiary(id, rating) {
  const response = await api.post(`/diary/${id}/rate`, { rating })
  return response.data.data
}

/**
 * 生成AIGC动画
 * @param {number} id - 日记ID
 * @param {Object} params - 生成参数
 * @param {Array<string>} params.images - 图片URL数组
 * @param {string} params.description - 文字描述（可选）
 * @returns {Promise<Object>} 任务信息
 */
export async function generateAnimation(id, params) {
  const response = await api.post(`/diary/${id}/generate-animation`, params)
  return response.data.data
}

/**
 * 查询动画生成状态
 * @param {string} taskId - 任务ID
 * @returns {Promise<Object>} 任务状态
 */
export async function getAnimationStatus(taskId) {
  const response = await api.get(`/diary/animation-status/${taskId}`)
  return response.data.data
}

/**
 * 按ID获取用户信息
 * @param {number} id - 用户ID
 * @returns {Promise<Object>} 用户信息
 */
export async function getUserById(id) {
  const response = await api.get(`/auth/user/${id}`)
  return response.data.data
}