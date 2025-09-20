<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
        <a-tag v-if="appInfo?.codeGenType" color="blue" class="code-gen-type-tag">
          {{ formatCodeGenType(appInfo.codeGenType) }}
        </a-tag>
        <!-- 工作流状态显示 -->
        <a-tag 
          v-if="ENABLE_WORKFLOW"
          :color="agentEnabled ? 'green' : 'default'" 
          class="agent-status-tag"
        >
          <template #icon>
            <span>⚙️</span>
          </template>
          工作流: {{ agentEnabled ? '开启' : '关闭' }}
        </a-tag>
      </div>
      <div class="header-right">
        <a-button type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button
            type="primary"
            ghost
            @click="downloadCode"
            :loading="downloading"
            :disabled="!isOwner"
        >
          <template #icon>
            <DownloadOutlined />
          </template>
          下载代码
        </a-button>
        <a-button type="primary" @click="deployApp" :loading="deploying">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
              加载更多历史消息
            </a-button>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar" />
              </div>
              <div class="message-content">
                <MarkdownRenderer v-if="message.content" :content="message.content" />
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <a-alert
            v-if="selectedElementInfo"
            class="selected-element-alert"
            type="info"
            closable
            @close="clearSelectedElement"
        >
          <template #message>
            <div class="selected-element-info">
              <div class="element-header">
                <span class="element-tag">
                  选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
                </span>
                <span v-if="selectedElementInfo.id" class="element-id">
                  #{{ selectedElementInfo.id }}
                </span>
                <span v-if="selectedElementInfo.className" class="element-class">
                  .{{ selectedElementInfo.className.split(' ').join('.') }}
                </span>
              </div>
              <div class="element-details">
                <div v-if="selectedElementInfo.textContent" class="element-item">
                  内容: {{ selectedElementInfo.textContent.substring(0, 50) }}
                  {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                </div>
                <div v-if="selectedElementInfo.pagePath" class="element-item">
                  页面路径: {{ selectedElementInfo.pagePath }}
                </div>
                <div class="element-item">
                  选择器:
                  <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                </div>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
              <a-textarea
                  v-model:value="userInput"
                  :placeholder="getInputPlaceholder()"
                  :rows="4"
                  :maxlength="1000"
                  @keydown.enter.prevent="sendMessage"
                  :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
                v-else
                v-model:value="userInput"
                :placeholder="getInputPlaceholder()"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating"
            />
            <div class="input-actions">
              <a-button
                  type="primary"
                  @click="sendMessage"
                  :loading="isGenerating"
                  :disabled="!isOwner"
              >
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <div class="preview-title">
            <h3>生成后的网页展示</h3>
            <div v-if="vueProjectBuilding" class="building-indicator">
              <a-spin size="small" />
              <span class="building-text">构建中...</span>
            </div>
          </div>
          <div class="preview-actions">
            <a-button
                v-if="isOwner && previewUrl"
                type="link"
                :danger="isEditMode"
                @click="toggleEditMode"
                :class="{ 'edit-mode-active': isEditMode }"
                style="padding: 0; height: auto; margin-right: 12px"
            >
              <template #icon>
                <EditOutlined />
              </template>
              {{ isEditMode ? '退出编辑' : '编辑模式' }}
            </a-button>
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              新窗口打开
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="buildFailure" class="preview-error">
            <div class="error-icon">❌</div>
            <h3>Vue项目构建失败</h3>
            <p class="error-message">{{ buildFailureMessage }}</p>
            <p class="error-tip">请检查代码是否正确，或重新生成项目</p>
          </div>
          <div v-else-if="!previewUrl && !isGenerating && !vueProjectBuilding" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>网站文件生成完成后将在这里展示</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成网站...</p>
          </div>
          <div v-else-if="vueProjectBuilding" class="preview-loading">
            <a-spin size="large" />
            <p>Vue项目正在构建中，请稍候...</p>
          </div>
          <iframe
              v-else
              :src="previewUrl"
              class="preview-iframe"
              frameborder="0"
              @load="onIframeLoad"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
        v-model:open="appDetailVisible"
        :app="appInfo"
        :show-actions="isOwner || isAdmin"
        @edit="editApp"
        @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
        v-model:open="deployModalVisible"
        :deploy-url="deployUrl"
        @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { useAgentStore } from '@/stores/agent'
import {
  getAppVoById,
  deployApp as deployAppApi,
  deleteApp as deleteAppApi,
} from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/aiAvatar.png'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'

import {
  CloudUploadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const agentStore = useAgentStore()

// 工作流功能开关 - 设置为 false 可隐藏工作流相关功能
const ENABLE_WORKFLOW = false

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<any>()

// Agent状态 - 在进入对话页面时固定，从URL参数获取
const agentEnabled = ref<boolean>(false)

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)
const vueProjectBuilding = ref(false)
const buildStatusEmitter = ref<EventSource | null>(null)
const buildFailure = ref(false)
const buildFailureMessage = ref('')
const buildTimeoutTimer = ref<number | null>(null)
const buildCompleted = ref(false)

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

// 权限相关
// 计算属性
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 应用详情相关
const appDetailVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  
  // 如果正在生成代码，避免覆盖当前对话
  if (isGenerating.value && !isLoadMore) {
    console.warn('正在生成代码，跳过历史消息加载以避免覆盖当前对话')
    return
  }
  
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        const historyMessages: Message[] = chatHistories
            .map((chat) => ({
              type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
              content: chat.message || '',
              createTime: chat.createTime,
            }))
            .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 先加载对话历史
      await loadChatHistory()
      // 如果有至少2条对话记录，展示对应的网站
      if (messages.value.length >= 2) {
        updatePreview()
      }
      
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (
          appInfo.value.initPrompt &&
          isOwner.value &&
          messages.value.length === 0 &&
          historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  // 清除之前的构建失败状态
  buildFailure.value = false
  buildFailureMessage.value = ''
  buildCompleted.value = false
  
  // 关闭之前的构建状态监听连接
  closeBuildStatusListener()

  let message = userInput.value.trim()
  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    message += elementContext
  }
  userInput.value = ''
  // 添加用户消息（包含元素信息）
  messages.value.push({
    type: 'user',
    content: message,
  })

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(message, aiMessageIndex)
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false

  try {
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
      agent: agentEnabled.value ? 'true' : 'false',
    })

    const url = `${baseURL}/app/chat/gen/code?${params}`

    // 创建 EventSource 连接
    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    let fullContent = ''

    // 处理接收到的消息
    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        // 解析JSON包装的数据
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        // 拼接内容
        if (content !== undefined && content !== null) {
          fullContent += content
          messages.value[aiMessageIndex].content = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    // 处理done事件
    eventSource.addEventListener('done', function () {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      eventSource?.close()

      // 如果是Vue项目，启动构建状态监听
      if (appInfo.value?.codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
        // 创建构建状态监听连接，实时接收构建状态推送
        createBuildStatusListener()
      } else {
        // 非Vue项目，正常更新预览
        setTimeout(async () => {
          updatePreview(true) // 强制刷新以显示最新修改
        }, 1000)
      }
    })

    // 处理business-error事件（后端限流等错误）
    eventSource.addEventListener('business-error', function (event: MessageEvent) {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        console.error('SSE业务错误事件:', errorData)

        // 显示具体的错误信息
        const errorMessage = errorData.message || '生成过程中出现错误'
        messages.value[aiMessageIndex].content = `❌ ${errorMessage}`
        messages.value[aiMessageIndex].loading = false
        message.error(errorMessage)

        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()
      } catch (parseError) {
        console.error('解析错误事件失败:', parseError, '原始数据:', event.data)
        handleError(new Error('服务器返回错误'), aiMessageIndex)
      }
    })

    // 处理错误
    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // 检查是否是正常的连接关闭
      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()

        // 只更新预览，不重新加载对话历史，避免覆盖当前正在生成的消息
        setTimeout(async () => {
          updatePreview(true) // 强制刷新以显示最新修改
        }, 1000)
      } else {
        handleError(new Error('SSE连接错误'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
}

// 更新预览
const updatePreview = (forceRefresh = false) => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    let newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    
    // 如果需要强制刷新，添加时间戳参数绕过缓存
    if (forceRefresh) {
      const timestamp = Date.now()
      const separator = newPreviewUrl.includes('?') ? '&' : '?'
      newPreviewUrl = `${newPreviewUrl}${separator}_t=${timestamp}`
    }
    
    previewUrl.value = newPreviewUrl
    previewReady.value = true
  }
}

// 刷新预览页面（用于显示最新修改）
const refreshPreview = () => {
  console.log('刷新预览以显示最新修改')
  updatePreview(true)
}

// 创建构建状态监听连接
const createBuildStatusListener = () => {
  if (!appId.value || buildStatusEmitter.value) return
  
  try {
    const baseURL = request.defaults.baseURL || API_BASE_URL
    const url = `${baseURL}/app/build-status/${appId.value}`
    
    buildStatusEmitter.value = new EventSource(url, {
      withCredentials: true,
    })
    
    // 监听构建开始事件
    buildStatusEmitter.value.addEventListener('build-started', function () {
      console.log('收到构建开始事件')
      vueProjectBuilding.value = true
      buildFailure.value = false
      buildFailureMessage.value = ''
      buildCompleted.value = false
      
      // 清除之前的超时定时器
      if (buildTimeoutTimer.value) {
        clearTimeout(buildTimeoutTimer.value)
      }
      
      // 设置超时检测（10分钟）
      buildTimeoutTimer.value = setTimeout(() => {
        if (vueProjectBuilding.value && !buildCompleted.value) {
          console.log('构建超时')
          vueProjectBuilding.value = false
          buildFailure.value = true
          buildFailureMessage.value = '构建超时（超过10分钟），请检查代码是否正确'
          buildCompleted.value = true
          closeBuildStatusListener()
        }
      }, 10 * 60 * 1000) // 10分钟超时
    })
    
    // 监听构建成功事件
    buildStatusEmitter.value.addEventListener('build-success', function () {
      console.log('收到构建成功事件，准备刷新预览')
      vueProjectBuilding.value = false
      buildFailure.value = false
      buildFailureMessage.value = ''
      buildCompleted.value = true
      
      // 清除超时定时器
      if (buildTimeoutTimer.value) {
        clearTimeout(buildTimeoutTimer.value)
        buildTimeoutTimer.value = null
      }
      
      // 显示成功通知
      message.success('Vue项目构建完成！', 3)
      
      // 延迟1秒后刷新预览，确保文件已经完全写入
      setTimeout(() => {
        refreshPreview()
      }, 1000)
    })
    
    // 监听构建失败事件
    buildStatusEmitter.value.addEventListener('build-failure', function (event: MessageEvent) {
      console.log('收到构建失败事件:', event.data)
      vueProjectBuilding.value = false
      buildFailure.value = true
      buildCompleted.value = true
      
      // 清除超时定时器
      if (buildTimeoutTimer.value) {
        clearTimeout(buildTimeoutTimer.value)
        buildTimeoutTimer.value = null
      }
      
      try {
        const eventData = JSON.parse(event.data)
        buildFailureMessage.value = eventData.message || '构建失败'
      } catch (e) {
        buildFailureMessage.value = '构建失败'
      }
      
      console.log('设置构建失败状态:', buildFailureMessage.value)
    })
    
    // 处理连接错误
    buildStatusEmitter.value.onerror = function () {
      console.log('构建状态SSE连接错误或关闭')
      
      // 只有在构建未完成时才设置错误状态
      if (!buildCompleted.value && vueProjectBuilding.value) {
        console.log('构建未完成，设置连接异常状态')
        vueProjectBuilding.value = false
        buildFailure.value = true
        buildFailureMessage.value = '构建状态连接异常，请刷新页面重试'
      } else {
        console.log('构建已完成，忽略连接关闭事件')
      }
      
      buildStatusEmitter.value?.close()
      buildStatusEmitter.value = null
    }
    
    console.log('构建状态监听连接已创建')
  } catch (error) {
    console.error('创建构建状态监听失败:', error)
  }
}

// 关闭构建状态监听连接
const closeBuildStatusListener = () => {
  if (buildStatusEmitter.value) {
    buildStatusEmitter.value.close()
    buildStatusEmitter.value = null
    console.log('构建状态监听连接已关闭')
  }
  
  // 清除超时定时器
  if (buildTimeoutTimer.value) {
    clearTimeout(buildTimeoutTimer.value)
    buildTimeoutTimer.value = null
  }
}

// 检查Vue项目预览是否可用
const checkVueProjectPreview = async () => {
  if (!previewUrl.value) return
  
  let retryCount = 0
  const maxRetries = 5
  const retryInterval = 2000
  
  const checkPreview = async (): Promise<boolean> => {
    try {
      const response = await fetch(previewUrl.value, { 
        method: 'HEAD',
        cache: 'no-cache'
      })
      return response.ok
    } catch (error) {
      console.log('预览检查失败:', error)
      return false
    }
  }
  
  const retryCheck = async () => {
    retryCount++
    console.log(`检查Vue项目预览 (${retryCount}/${maxRetries}):`, previewUrl.value)
    
    const isAvailable = await checkPreview()
    if (isAvailable) {
      console.log('Vue项目预览已就绪')
      // 强制刷新预览以显示最新版本
      updatePreview(true)
      return
    }
    
    if (retryCount < maxRetries) {
      console.log(`预览未就绪，${retryInterval/1000}秒后重试...`)
      setTimeout(retryCheck, retryInterval)
    } else {
      console.warn('Vue项目预览检查超时，可能构建失败或需要更长时间')
      message.warning('预览加载较慢，请稍后手动刷新页面')
    }
  }
  
  retryCheck()
}

// 检查Vue构建状态（新方法）
const checkVueBuildStatus = async () => {
  let retryCount = 0
  const maxRetries = 30 // 最多检查5分钟
  const retryInterval = 10000 // 每10秒检查一次
  const startTime = Date.now()
  
  const checkBuild = async (): Promise<{ success: boolean; hasDist: boolean; hasPackageJson: boolean }> => {
    try {
      // 构建预览URL，强制刷新以检测最新版本
      updatePreview(true)
      if (!previewUrl.value) return { success: false, hasDist: false, hasPackageJson: false }
      
      // 检查预览是否可访问
      const previewResponse = await fetch(previewUrl.value, { 
        method: 'HEAD',
        cache: 'no-cache'
      })
      
      // 如果预览可访问，说明构建成功
      if (previewResponse.ok) {
        return { success: true, hasDist: true, hasPackageJson: true }
      }
      
      // 如果预览不可访问，检查项目目录是否存在（用于判断是否是构建失败）
      const sourceUrl = `${API_BASE_URL}/static/vue_project_${appId.value}/package.json`
      const sourceResponse = await fetch(sourceUrl, { 
        method: 'HEAD',
        cache: 'no-cache'
      })
      
      return { 
        success: false, 
        hasDist: false, 
        hasPackageJson: sourceResponse.ok 
      }
    } catch (error) {
      return { success: false, hasDist: false, hasPackageJson: false }
    }
  }
  
  const retryCheck = async () => {
    retryCount++
    const elapsed = Math.round((Date.now() - startTime) / 1000)
    console.log(`检查Vue项目构建状态 (${retryCount}/${maxRetries}) - 已等待${elapsed}秒`)
    
    const { success: isBuilt, hasPackageJson } = await checkBuild()
    
    if (isBuilt) {
      console.log('Vue项目构建完成，预览已就绪')
      vueProjectBuilding.value = false
      
      // 只显示成功通知，不需要管理构建中的通知
      message.success('Vue项目构建完成！', 3)
      
      // 强制刷新预览URL以绕过缓存
      updatePreview(true)
      
      return
    }
    
    // 检查是否是构建失败（有源文件但长时间没有dist）
    if (hasPackageJson && elapsed > 120) { // 2分钟后开始判断可能是构建失败
      console.warn('检测到可能的构建失败：有源文件但构建时间过长')
      vueProjectBuilding.value = false
      return
    }
    
    if (retryCount < maxRetries) {
      console.log(`构建中...${retryInterval/1000}秒后再次检查`)
      setTimeout(retryCheck, retryInterval)
    } else {
      console.warn('Vue项目构建检查超时')
      vueProjectBuilding.value = false
    }
  }
  
  retryCheck()
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning('请等待页面加载完成')
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  return '请描述你想生成的网站，越详细效果越好哦'
}

// 页面加载时获取应用信息
onMounted(() => {
  // 从URL参数获取agent状态
  const agentParam = route.query.agent as string
  if (agentParam === 'true') {
    agentEnabled.value = true
  } else if (agentParam === 'false') {
    agentEnabled.value = false
  }
  
  fetchAppInfo()

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

// 清理资源
onUnmounted(() => {
  // 关闭构建状态监听连接
  closeBuildStatusListener()
  // EventSource 会在组件卸载时自动清理
})
</script>

<style scoped>
#appChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: #fdfdfd;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-gen-type-tag {
  font-size: 12px;
}

.agent-status-tag {
  font-size: 12px;
  margin-left: 8px;
}

.app-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.header-right {
  display: flex;
  gap: 12px;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 8px;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.messages-container {
  flex: 0.9;
  padding: 16px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 12px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 8px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
}

.user-message .message-content {
  background: #1890ff;
  color: white;
}

.ai-message .message-content {
  background: #f5f5f5;
  color: #1a1a1a;
  padding: 8px 12px;
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

/* 加载更多按钮 */
.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 16px;
}

/* 输入区域 */
.input-container {
  padding: 16px;
  background: white;
}

.input-wrapper {
  position: relative;
}

.input-wrapper .ant-input {
  padding-right: 50px;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

/* 右侧预览区域 */
.preview-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.preview-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.building-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #1890ff;
  font-size: 14px;
}

.building-text {
  color: #666;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.preview-loading p {
  margin-top: 16px;
}

.preview-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #ff4d4f;
  text-align: center;
  padding: 20px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.preview-error h3 {
  color: #ff4d4f;
  margin-bottom: 12px;
  font-size: 18px;
}

.error-message {
  color: #666;
  margin-bottom: 8px;
  font-size: 14px;
}

.error-tip {
  color: #999;
  font-size: 12px;
  font-style: italic;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.selected-element-alert {
  margin: 0 16px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .preview-section {
    flex: none;
    height: 50vh;
  }
}

@media (max-width: 768px) {
  .header-bar {
    padding: 12px 16px;
  }

  .app-name {
    font-size: 16px;
  }

  .main-content {
    padding: 8px;
    gap: 8px;
  }

  .message-content {
    max-width: 85%;
  }

  /* 选中元素信息样式 */
  .selected-element-alert {
    margin: 0 16px;
  }

  .selected-element-info {
    line-height: 1.4;
  }

  .element-header {
    margin-bottom: 8px;
  }

  .element-details {
    margin-top: 8px;
  }

  .element-item {
    margin-bottom: 4px;
    font-size: 13px;
  }

  .element-item:last-child {
    margin-bottom: 0;
  }

  .element-tag {
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 14px;
    font-weight: 600;
    color: #007bff;
  }

  .element-id {
    color: #28a745;
    margin-left: 4px;
  }

  .element-class {
    color: #ffc107;
    margin-left: 4px;
  }

  .element-selector-code {
    font-family: 'Monaco', 'Menlo', monospace;
    background: #f6f8fa;
    padding: 2px 4px;
    border-radius: 3px;
    font-size: 12px;
    color: #d73a49;
    border: 1px solid #e1e4e8;
  }

  /* 编辑模式按钮样式 */
  .edit-mode-active {
    background-color: #52c41a !important;
    border-color: #52c41a !important;
    color: white !important;
  }

  .edit-mode-active:hover {
    background-color: #73d13d !important;
    border-color: #73d13d !important;
  }
}
</style>
