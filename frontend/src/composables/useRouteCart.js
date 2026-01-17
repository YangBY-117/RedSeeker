import { ref, computed } from 'vue'

// 选中的景点列表（类似购物车）
const selectedAttractions = ref([])

/**
 * 路线购物车管理
 */
export function useRouteCart() {
  /**
   * 添加景点到路线
   * @param {Object} attraction - 景点对象 {id, name, address, longitude, latitude}
   */
  const addAttraction = (attraction) => {
    // 检查是否已存在
    const exists = selectedAttractions.value.find(item => item.id === attraction.id)
    if (!exists) {
      // 支持多种坐标字段名
      const lng = attraction.longitude ?? attraction.lng ?? attraction.lon
      const lat = attraction.latitude ?? attraction.lat
      
      selectedAttractions.value.push({
        id: attraction.id,
        name: attraction.name,
        address: attraction.address,
        longitude: lng,
        latitude: lat,
        category: attraction.category,
        categoryName: attraction.categoryName,
        stageStart: attraction.stage_start ?? attraction.stageStart,
        stageEnd: attraction.stage_end ?? attraction.stageEnd,
        stageName: attraction.stage_name ?? attraction.stageName
      })
    }
  }

  /**
   * 从路线中移除景点
   * @param {number} attractionId - 景点ID
   */
  const removeAttraction = (attractionId) => {
    const index = selectedAttractions.value.findIndex(item => item.id === attractionId)
    if (index > -1) {
      selectedAttractions.value.splice(index, 1)
    }
  }

  /**
   * 清空路线
   */
  const clearAttractions = () => {
    selectedAttractions.value = []
  }

  /**
   * 检查景点是否已选中
   * @param {number} attractionId - 景点ID
   * @returns {boolean}
   */
  const isSelected = (attractionId) => {
    return selectedAttractions.value.some(item => item.id === attractionId)
  }

  /**
   * 更新景点顺序
   * @param {number} fromIndex - 原索引
   * @param {number} toIndex - 目标索引
   */
  const reorderAttractions = (fromIndex, toIndex) => {
    const item = selectedAttractions.value.splice(fromIndex, 1)[0]
    selectedAttractions.value.splice(toIndex, 0, item)
  }

  return {
    selectedAttractions,
    addAttraction,
    removeAttraction,
    clearAttractions,
    isSelected,
    reorderAttractions,
    count: computed(() => selectedAttractions.value.length)
  }
}
