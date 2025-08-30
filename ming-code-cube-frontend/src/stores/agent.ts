import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 工作流开关状态管理
 */
export const useAgentStore = defineStore('agent', () => {
  // 工作流开关状态，默认为false（关闭）
  const isAgentEnabled = ref<boolean>(false)

  // 设置工作流状态
  function setAgentEnabled(enabled: boolean) {
    isAgentEnabled.value = enabled
  }

  // 重置工作流状态（用于创建新对话时）
  function resetAgentState() {
    isAgentEnabled.value = false
  }

  return { 
    isAgentEnabled, 
    setAgentEnabled, 
    resetAgentState 
  }
})
