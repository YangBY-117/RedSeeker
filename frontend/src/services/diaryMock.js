// 简单的前端 Mock 服务，用于本地开发和测试
let nextId = 101
import { getImagesForAttraction } from '../data/images.js'

const mockDiaries = [
  {
    id: 1,
    title: '在井冈山的红色记忆',
    content: '这是一次难忘的红色之旅，感受到了革命先烈的精神。',
    destination: '井冈山',
    travel_date: '2024-08-10',
    author: { id: 1, username: '游客A' },
    view_count: 123,
    average_rating: 4.5,
    total_ratings: 10,
    media: getImagesForAttraction(11).map((u, i) => ({ id: `m1-${i}`, media_type: 'image', file_path: u })),
    attractions: [{ id: 11, name: '井冈山革命根据地', address: '江西', images: getImagesForAttraction(11) }]
  },
  {
    id: 2,
    title: '延安阳光下的回忆',
    content: '延安地下生活的历史让我深受触动。',
    destination: '延安',
    travel_date: '2023-05-01',
    author: { id: 2, username: '游客B' },
    view_count: 78,
    average_rating: 4.2,
    total_ratings: 6,
    media: getImagesForAttraction(21).map((u, i) => ({ id: `m2-${i}`, media_type: 'image', file_path: u })),
    attractions: [{ id: 21, name: '延安革命纪念馆', address: '陕西', images: getImagesForAttraction(21) }]
  }
]

// 模拟延迟
const delay = (ms = 200) => new Promise((r) => setTimeout(r, ms))

export async function getDiaryList({ sortBy = 'heat', userId, page = 1, pageSize = 12 } = {}) {
  await delay()
  // 简单排序：按热度(view_count)、评分(average_rating)、时间(travel_date)
  const items = [...mockDiaries]
  if (sortBy === 'heat') items.sort((a, b) => (b.view_count || 0) - (a.view_count || 0))
  if (sortBy === 'rating') items.sort((a, b) => (b.average_rating || 0) - (a.average_rating || 0))
  if (sortBy === 'time') items.sort((a, b) => new Date(b.travel_date || b.created_at) - new Date(a.travel_date || a.created_at))

  const total = items.length
  const totalPages = Math.max(1, Math.ceil(total / pageSize))
  const start = (page - 1) * pageSize
  const diaries = items.slice(start, start + pageSize)
  return { diaries, total, page, pageSize, totalPages }
}

export async function searchDiaryByDestination({ destination = '', sortBy = 'heat', page = 1, pageSize = 12 } = {}) {
  await delay()
  const filtered = mockDiaries.filter(d => (d.destination || '').toLowerCase().includes(destination.toLowerCase()))
  const total = filtered.length
  const totalPages = Math.max(1, Math.ceil(total / pageSize))
  const start = (page - 1) * pageSize
  const diaries = filtered.slice(start, start + pageSize)
  return { diaries, total, page, pageSize, totalPages }
}

export async function searchDiaryByName(title) {
  await delay()
  const found = mockDiaries.find(d => d.title === title)
  return found || null
}

export async function fulltextSearch({ keyword = '', page = 1, pageSize = 12 } = {}) {
  await delay()
  const filtered = mockDiaries.filter(d => ((d.title + ' ' + d.content) || '').toLowerCase().includes(keyword.toLowerCase()))
  const total = filtered.length
  const totalPages = Math.max(1, Math.ceil(total / pageSize))
  const start = (page - 1) * pageSize
  const diaries = filtered.slice(start, start + pageSize)
  return { diaries, total, page, pageSize, totalPages }
}

export async function getDiaryDetail(id) {
  await delay()
  return mockDiaries.find(d => d.id === Number(id)) || null
}

export async function createDiary(formData) {
  await delay()
  const obj = { id: nextId++, media: [], attractions: [], view_count: 0, average_rating: 0, total_ratings: 0 }
  // formData may be FormData or plain object in tests
  if (formData instanceof FormData) {
    obj.title = formData.get('title') || '无标题'
    obj.content = formData.get('content') || ''
    obj.destination = formData.get('destination') || ''
    obj.travel_date = formData.get('travel_date') || ''
    const aid = formData.get('attraction_ids')
    if (aid) {
      try {
        const ids = JSON.parse(aid)
        obj.attractions = Array.isArray(ids) ? ids.map(id => ({ id, name: `景点 ${id}`, address: '', images: getImagesForAttraction(id) })) : []
      } catch (e) {
        obj.attractions = []
      }
    }
    // 处理 images 和 videos： FormData 的同名字段可能存在多个
    const images = []
    for (const pair of formData.entries()) {
      if (pair[0] === 'images') {
        const file = pair[1]
        try {
          const url = typeof file === 'string' ? file : URL.createObjectURL(file)
          images.push({ id: `img-${Math.random().toString(36).slice(2,8)}`, media_type: 'image', file_path: url })
        } catch (e) {
          // ignore
        }
      }
      if (pair[0] === 'videos') {
        const file = pair[1]
        try {
          const url = typeof file === 'string' ? file : URL.createObjectURL(file)
          obj.media.push({ id: `vid-${Math.random().toString(36).slice(2,8)}`, media_type: 'video', file_path: url })
        } catch (e) {}
      }
    }
    obj.media = [...images, ...obj.media]
  } else if (typeof formData === 'object') {
    obj.title = formData.title || '无标题'
    obj.content = formData.content || ''
    obj.destination = formData.destination || ''
    obj.travel_date = formData.travel_date || ''
    if (Array.isArray(formData.attraction_ids)) {
      obj.attractions = formData.attraction_ids.map(id => ({ id, name: `景点 ${id}`, address: '', images: getImagesForAttraction(id) }))
    }
  }
  obj.author = { id: 999, username: '本地测试用户' }
  obj.created_at = new Date().toISOString()
  mockDiaries.unshift(obj)
  return obj
}

export async function updateDiary(id, formData) {
  await delay()
  const idx = mockDiaries.findIndex(d => d.id === Number(id))
  if (idx === -1) throw new Error('Not found')
  const target = mockDiaries[idx]
  if (formData instanceof FormData) {
    if (formData.get('title')) target.title = formData.get('title')
    if (formData.get('content')) target.content = formData.get('content')
  }
  return target
}

export async function deleteDiary(id) {
  await delay()
  const idx = mockDiaries.findIndex(d => d.id === Number(id))
  if (idx !== -1) mockDiaries.splice(idx, 1)
}

export async function rateDiary(id, rating) {
  await delay()
  const diary = mockDiaries.find(d => d.id === Number(id))
  if (!diary) throw new Error('Not found')
  diary.total_ratings = (diary.total_ratings || 0) + 1
  diary.average_rating = ((diary.average_rating || 0) * (diary.total_ratings - 1) + rating) / diary.total_ratings
  return { average_rating: diary.average_rating, total_ratings: diary.total_ratings }
}

export async function generateAnimation(id, params) {
  await delay()
  // 返回一个 mock 任务ID
  return { taskId: `task-${Math.random().toString(36).slice(2,8)}` }
}

export async function getAnimationStatus(taskId) {
  await delay()
  // 随机返回状态
  const states = ['waiting', 'processing', 'completed']
  const state = states[Math.floor(Math.random() * states.length)]
  if (state === 'completed') {
    return { status: 'completed', videoUrl: 'https://www.w3schools.com/html/mov_bbb.mp4' }
  }
  return { status: state }
}

export async function getUserById(id) {
  await delay()
  return { id, username: `用户${id}` }
}

export default {
  getDiaryList,
  searchDiaryByDestination,
  searchDiaryByName,
  fulltextSearch,
  getDiaryDetail,
  createDiary,
  updateDiary,
  deleteDiary,
  rateDiary,
  generateAnimation,
  getAnimationStatus,
  getUserById
}
