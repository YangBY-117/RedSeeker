import { api } from './api'

/**
 * AI服务
 * 提供AI写日记、文生图、图生动画等功能
 */

/**
 * AI生成日记内容
 * @param {Object} params - 生成参数
 * @param {string} params.prompt - 用户描述
 * @param {string} params.destination - 目的地（可选）
 * @param {string} params.travel_date - 旅游日期（可选）
 * @param {Array<number|string>} params.attraction_ids - 关联景点ID（可选）
 * @returns {Promise<Object>} 生成的日记内容 {title, content}
 */
export async function generateDiaryContent(params) {
  const response = await api.post('/ai/generate-diary', {
    prompt: params.prompt,
    destination: params.destination,
    travelDate: params.travel_date,
    attractionIds: params.attraction_ids
  }, {
    timeout: 120000 // AI生成日记内容可能需要更长时间，设置为120秒
  })
  return response.data?.data ?? response.data ?? null
}

/**
 * 文生图
 * @param {Object} params - 生成参数
 * @param {string} params.prompt - 图片描述
 * @returns {Promise<Object>} 生成的图片信息 {imageUrl, taskId}
 */
export async function generateImageFromText(params) {
  const response = await api.post('/ai/text-to-image', {
    prompt: params.prompt
  }, {
    timeout: 120000
  })
  return response.data?.data ?? response.data ?? null
}

/**
 * 图生动画
 * @param {Object} params - 生成参数
 * @param {Array<string>} params.images - 图片URL数组
 * @param {string} params.description - 文字描述（可选）
 * @returns {Promise<Object>} 任务信息 {taskId, status, videoUrl}
 */
export async function generateAnimationFromImages(params) {
  const response = await api.post('/ai/image-to-animation', {
    images: params.images,
    description: params.description
  }, {
    timeout: 180000
  })
  return response.data?.data ?? response.data ?? null
}
