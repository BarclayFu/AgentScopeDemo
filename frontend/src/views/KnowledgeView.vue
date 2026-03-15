<template>
  <div class="knowledge-view">
    <div class="page-header">
      <div>
        <h1>📚 知识库管理</h1>
        <p class="subtitle">管理知识条目、查看状态并手动刷新知识库索引</p>
      </div>
      <div class="header-actions">
        <button class="secondary-btn" :disabled="isRebuilding" @click="loadKnowledgeData">
          {{ isLoading ? '加载中...' : '刷新列表' }}
        </button>
        <button class="primary-btn" :disabled="isRebuilding" @click="rebuildIndex">
          {{ isRebuilding ? '刷新中...' : '重建索引' }}
        </button>
      </div>
    </div>

    <div class="status-grid">
      <div class="status-card">
        <div class="status-label">知识库状态</div>
        <div class="status-value">{{ status.initialized ? '已初始化' : '未初始化' }}</div>
      </div>
      <div class="status-card">
        <div class="status-label">知识条目数</div>
        <div class="status-value">{{ entryList.total }}</div>
      </div>
      <div class="status-card">
        <div class="status-label">最近更新时间</div>
        <div class="status-value small">{{ formatDate(status.lastUpdatedAt) }}</div>
      </div>
      <div class="status-card">
        <div class="status-label">最近索引刷新</div>
        <div class="status-value small">{{ formatDate(status.lastRebuildAt) }}</div>
      </div>
    </div>

    <p v-if="feedbackMessage" class="feedback-message">{{ feedbackMessage }}</p>
    <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

    <div class="content-grid">
      <section class="panel">
        <div class="panel-header">
          <h2>新增知识条目</h2>
          <p>当前 MVP 先支持标题 + 文本内容的手工录入。</p>
        </div>

        <div class="form-field">
          <label>标题</label>
          <input v-model="form.title" type="text" class="text-input" placeholder="例如：发票开具说明" />
        </div>

        <div class="form-field">
          <label>内容</label>
          <textarea
            v-model="form.content"
            class="text-area"
            rows="8"
            placeholder="输入将写入知识库的文本内容"
          ></textarea>
        </div>

        <div class="panel-actions">
          <button
            class="primary-btn"
            :disabled="isSubmitting || !form.title.trim() || !form.content.trim()"
            @click="submitEntry"
          >
            {{ isSubmitting ? '提交中...' : '新增知识' }}
          </button>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <h2>当前知识条目</h2>
          <p>{{ status.lastOperationMessage || '这里会展示知识库最近一次管理操作结果。' }}</p>
        </div>

        <div v-if="isLoading && entryList.entries.length === 0" class="empty-state">
          正在加载知识条目...
        </div>
        <div v-else-if="entryList.entries.length === 0" class="empty-state">
          当前没有可管理的知识条目，请先新增一条知识。
        </div>
        <div v-else class="entry-list">
          <article v-for="entry in entryList.entries" :key="entry.entryId" class="entry-card">
            <div class="entry-header">
              <div>
                <h3>{{ entry.title }}</h3>
                <p>{{ entry.source }} / {{ entry.type }}</p>
              </div>
              <button class="danger-btn" @click="removeEntry(entry.entryId)">删除</button>
            </div>
            <p class="entry-preview">{{ entry.contentPreview }}</p>
            <div class="entry-meta">
              <span>创建于 {{ formatDate(entry.createdAt) }}</span>
              <span>更新于 {{ formatDate(entry.updatedAt) }}</span>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  createKnowledgeEntry,
  deleteKnowledgeEntry,
  getKnowledgeEntries,
  getKnowledgeStatus,
  rebuildKnowledgeBase
} from '@/api'

const entryList = ref({
  entries: [],
  total: 0,
  checkedAt: null
})
const status = ref({
  initialized: false,
  totalEntries: 0,
  lastUpdatedAt: null,
  lastRebuildAt: null,
  lastOperationMessage: ''
})
const form = reactive({
  title: '',
  content: ''
})
const isLoading = ref(false)
const isSubmitting = ref(false)
const isRebuilding = ref(false)
const feedbackMessage = ref('')
const errorMessage = ref('')

async function loadKnowledgeData() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const [entriesData, statusData] = await Promise.all([
      getKnowledgeEntries(),
      getKnowledgeStatus()
    ])
    entryList.value = entriesData
    status.value = statusData
  } catch (error) {
    console.error('Failed to load knowledge data:', error)
    errorMessage.value = error?.message || '知识库数据加载失败，请稍后重试。'
  } finally {
    isLoading.value = false
  }
}

async function submitEntry() {
  isSubmitting.value = true
  errorMessage.value = ''
  feedbackMessage.value = ''

  try {
    const response = await createKnowledgeEntry({
      title: form.title,
      content: form.content
    })

    feedbackMessage.value = response.message || '知识条目已创建。'
    form.title = ''
    form.content = ''
    await loadKnowledgeData()
  } catch (error) {
    console.error('Failed to create knowledge entry:', error)
    errorMessage.value = error?.response?.data?.message || error?.message || '知识条目创建失败。'
  } finally {
    isSubmitting.value = false
  }
}

async function removeEntry(entryId) {
  errorMessage.value = ''
  feedbackMessage.value = ''

  try {
    const response = await deleteKnowledgeEntry(entryId)
    feedbackMessage.value = response.message || '知识条目已删除。'
    await loadKnowledgeData()
  } catch (error) {
    console.error('Failed to delete knowledge entry:', error)
    errorMessage.value = error?.response?.data?.message || error?.message || '知识条目删除失败。'
  }
}

async function rebuildIndex() {
  isRebuilding.value = true
  errorMessage.value = ''
  feedbackMessage.value = ''

  try {
    const response = await rebuildKnowledgeBase()
    feedbackMessage.value = response.message || '知识库刷新完成。'
    await loadKnowledgeData()
  } catch (error) {
    console.error('Failed to rebuild knowledge base:', error)
    errorMessage.value = error?.response?.data?.message || error?.message || '知识库刷新失败。'
  } finally {
    isRebuilding.value = false
  }
}

function formatDate(timestamp) {
  if (!timestamp) {
    return '暂无'
  }

  return new Date(timestamp).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadKnowledgeData()
})
</script>

<style scoped>
.knowledge-view {
  padding: 30px;
  overflow-y: auto;
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  flex-wrap: wrap;
}

.page-header h1 {
  font-size: 28px;
  color: #333;
  margin: 0 0 10px;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

.header-actions,
.panel-actions {
  display: flex;
  gap: 12px;
}

.status-grid,
.content-grid {
  display: grid;
  gap: 20px;
}

.status-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  margin-bottom: 20px;
}

.content-grid {
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
}

.status-card,
.panel,
.entry-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.status-card {
  padding: 20px;
}

.status-label {
  color: #98a2b3;
  font-size: 13px;
  margin-bottom: 8px;
}

.status-value {
  color: #344054;
  font-size: 28px;
  font-weight: 600;
}

.status-value.small {
  font-size: 18px;
}

.panel {
  padding: 24px;
}

.panel-header {
  margin-bottom: 20px;
}

.panel-header h2 {
  margin: 0 0 8px;
  color: #344054;
  font-size: 20px;
}

.panel-header p {
  color: #667085;
  font-size: 14px;
}

.form-field {
  margin-bottom: 16px;
}

.form-field label {
  display: block;
  margin-bottom: 8px;
  color: #475467;
  font-size: 14px;
}

.text-input,
.text-area {
  width: 100%;
  border: 2px solid #e4e7ec;
  border-radius: 12px;
  padding: 12px 14px;
  font-size: 14px;
  font-family: inherit;
  outline: none;
}

.text-input:focus,
.text-area:focus {
  border-color: #667eea;
}

.text-area {
  resize: vertical;
}

.entry-list {
  display: grid;
  gap: 16px;
}

.entry-card {
  padding: 18px 18px 16px;
  border: 1px solid #eef2f6;
}

.entry-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.entry-header h3 {
  margin: 0 0 4px;
  color: #344054;
}

.entry-header p,
.entry-meta {
  color: #667085;
  font-size: 13px;
}

.entry-preview {
  color: #475467;
  line-height: 1.7;
  margin-bottom: 12px;
}

.entry-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.empty-state,
.feedback-message,
.error-message {
  padding: 16px 18px;
  border-radius: 12px;
  margin-bottom: 16px;
  font-size: 14px;
}

.empty-state {
  background: #f8fafc;
  color: #667085;
}

.feedback-message {
  background: #ecfdf3;
  color: #027a48;
}

.error-message {
  background: #fef3f2;
  color: #b42318;
}

.primary-btn,
.secondary-btn,
.danger-btn {
  border: none;
  border-radius: 10px;
  padding: 11px 16px;
  font-size: 14px;
  cursor: pointer;
}

.primary-btn {
  background: #667eea;
  color: white;
}

.secondary-btn {
  background: #eef2ff;
  color: #4052b5;
}

.danger-btn {
  background: #fef3f2;
  color: #b42318;
}

.primary-btn:disabled,
.secondary-btn:disabled,
.danger-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
