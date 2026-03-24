<template>
  <div class="knowledge-view">
    <!-- Page Header -->
    <div class="page-header">
      <div>
        <h1>📚 知识库管理</h1>
      </div>
      <div class="header-actions">
        <button class="secondary-btn" :disabled="isLoading" @click="handleRefresh">
          {{ isLoading ? '加载中...' : '刷新' }}
        </button>
        <button class="primary-btn" :disabled="isRebuilding" @click="handleRebuildIndex">
          {{ isRebuilding ? '重建中...' : '重建索引' }}
        </button>
      </div>
    </div>

    <!-- Main Content: Left Tree + Right List -->
    <div class="main-content">
      <!-- Left Panel: Category Tree -->
      <aside class="left-panel">
        <div class="panel-header">
          <h2>分类目录</h2>
        </div>
        <div class="category-tree-container">
          <CategoryTree
            :categories="categoryTree"
            :selected-id="selectedCategoryId === 'uncategorized' ? 'uncategorized' : selectedCategoryId"
            :expanded-ids="expandedIds"
            :uncategorized-count="uncategorizedCount"
            @select="handleCategorySelect"
            @toggle-expand="handleToggleExpand"
            @add-category="handleAddCategory"
          />
        </div>
      </aside>

      <!-- Right Panel: Entry List -->
      <main class="right-panel">
        <!-- List Header -->
        <div class="list-header">
          <div class="list-title">
            <h2>{{ currentCategoryName }}</h2>
            <span class="entry-count">共 {{ filteredEntries.length }} 条知识条目</span>
          </div>
          <div class="list-actions">
            <div class="search-box">
              <input
                v-model="searchQuery"
                type="text"
                class="search-input"
                placeholder="搜索知识条目..."
                @input="handleSearch"
              />
            </div>
            <button class="primary-btn" @click="handleAddEntry">
              + 新增
            </button>
          </div>
        </div>

        <!-- Entry Cards -->
        <div class="entry-list">
          <div v-if="isLoading && filteredEntries.length === 0" class="empty-state">
            正在加载知识条目...
          </div>
          <div v-else-if="filteredEntries.length === 0" class="empty-state">
            当前分类下没有知识条目
          </div>
          <article
            v-for="entry in filteredEntries"
            v-else
            :key="entry.entryId"
            class="entry-card"
            @click="handleEntryClick(entry)"
          >
            <div class="entry-header">
              <h3>{{ entry.title }}</h3>
              <div v-if="entry.tags && entry.tags.length > 0" class="entry-tags">
                <span v-for="tag in entry.tags" :key="tag.id" class="tag-badge">
                  {{ tag.name }}
                </span>
              </div>
            </div>
            <p class="entry-path">{{ formatCategoryPath(entry.categories) }}</p>
            <p class="entry-preview">{{ getEntryPreview(entry) }}</p>
            <div class="entry-meta">
              <span>创建于 {{ formatDate(entry.createdAt) }}</span>
              <span>更新于 {{ formatDate(entry.updatedAt) }}</span>
            </div>
          </article>
        </div>
      </main>
    </div>

    <!-- Entry Detail Modal -->
    <EntryDetailModal
      :show="showModal"
      :entry="selectedEntry"
      :category-tree="categoryTree"
      @close="handleCloseModal"
      @save="handleSave"
      @delete="handleDelete"
    />

    <!-- Messages -->
    <p v-if="feedbackMessage" class="feedback-message">{{ feedbackMessage }}</p>
    <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import CategoryTree from '@/components/knowledge/CategoryTree.vue'
import EntryDetailModal from '@/components/knowledge/EntryDetailModal.vue'
import {
  getCategoryTree,
  getKnowledgeEntries,
  createCategory,
  updateKnowledgeEntry,
  deleteKnowledgeEntry,
  rebuildKnowledgeBase
} from '@/api'

// State
const categoryTree = ref([])
const selectedCategoryId = ref(null)
const expandedIds = ref(new Set())
const filteredEntries = ref([])
const showModal = ref(false)
const selectedEntry = ref(null)
const searchQuery = ref('')
const isLoading = ref(false)
const isRebuilding = ref(false)
const feedbackMessage = ref('')
const errorMessage = ref('')
const uncategorizedCount = ref(0)

// Computed
const currentCategoryName = computed(() => {
  if (selectedCategoryId.value === 'uncategorized' || selectedCategoryId.value === null) {
    return '全部知识'
  }

  const findCategory = (categories, id) => {
    for (const cat of categories) {
      if (cat.id === id) return cat
      if (cat.children?.length) {
        const found = findCategory(cat.children, id)
        if (found) return found
      }
    }
    return null
  }

  const category = findCategory(categoryTree.value, selectedCategoryId.value)
  return category?.name || '全部知识'
})

// Functions
async function loadCategoryTree() {
  try {
    const data = await getCategoryTree()
    // API returns { categories: [...] } but axios unwraps to the object
    categoryTree.value = (data && data.categories) ? data.categories : []
  } catch (error) {
    console.error('Failed to load category tree:', error)
    errorMessage.value = '分类目录加载失败'
  }
}

async function loadEntries() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const params = {}

    // Filter by category
    if (selectedCategoryId.value === 'uncategorized') {
      params.categoryId = null
    } else if (selectedCategoryId.value) {
      params.categoryId = selectedCategoryId.value
    }

    // Filter by search query
    if (searchQuery.value.trim()) {
      params.search = searchQuery.value.trim()
    }

    const data = await getKnowledgeEntries(params)

    // Handle both array and object response formats
    if (Array.isArray(data)) {
      filteredEntries.value = data
    } else if (data && typeof data === 'object') {
      filteredEntries.value = data.entries || data.items || []
      if (data.uncategorizedCount !== undefined) {
        uncategorizedCount.value = data.uncategorizedCount
      }
    } else {
      filteredEntries.value = []
    }
  } catch (error) {
    console.error('Failed to load entries:', error)
    errorMessage.value = '知识条目加载失败'
    filteredEntries.value = []
  } finally {
    isLoading.value = false
  }
}

function handleCategorySelect(category) {
  selectedCategoryId.value = category.id === 'uncategorized' ? 'uncategorized' : category.id
  loadEntries()
}

function handleToggleExpand(category) {
  if (expandedIds.value.has(category.id)) {
    expandedIds.value.delete(category.id)
  } else {
    expandedIds.value.add(category.id)
  }
  // Trigger reactivity
  expandedIds.value = new Set(expandedIds.value)
}

async function handleAddCategory(parentId) {
  const name = prompt('请输入分类名称：')
  if (!name || !name.trim()) return

  try {
    await createCategory(name.trim(), parentId)
    feedbackMessage.value = '分类创建成功'
    await loadCategoryTree()
  } catch (error) {
    console.error('Failed to create category:', error)
    errorMessage.value = '分类创建失败'
  }
}

function handleEntryClick(entry) {
  selectedEntry.value = entry
  showModal.value = true
}

function handleAddEntry() {
  // Create a new empty entry for the modal
  selectedEntry.value = {
    entryId: null,
    title: '',
    content: '',
    tags: [],
    categories: [],
    categoryId: selectedCategoryId.value === 'uncategorized' ? null : selectedCategoryId.value
  }
  showModal.value = true
}

function handleCloseModal() {
  showModal.value = false
  selectedEntry.value = null
}

async function handleSave(entryData) {
  try {
    if (entryData.entryId) {
      // Update existing entry
      await updateKnowledgeEntry(entryData.entryId, {
        title: entryData.title,
        content: entryData.content,
        tagIds: entryData.tags?.map(t => t.id).filter(Boolean) || [],
        categoryIds: entryData.categoryId ? [entryData.categoryId] : []
      })
      feedbackMessage.value = '知识条目已更新'
    } else {
      // Create new entry - use the API directly
      const { createKnowledgeEntry } = await import('@/api')
      await createKnowledgeEntry({
        title: entryData.title,
        content: entryData.content,
        categoryId: entryData.categoryId,
        tagIds: entryData.tags?.map(t => t.id).filter(Boolean) || []
      })
      feedbackMessage.value = '知识条目已创建'
    }

    handleCloseModal()
    await loadEntries()
  } catch (error) {
    console.error('Failed to save entry:', error)
    errorMessage.value = error?.message || '保存失败'
  }
}

async function handleDelete(entryId) {
  try {
    await deleteKnowledgeEntry(entryId)
    feedbackMessage.value = '知识条目已删除'
    handleCloseModal()
    await loadEntries()
  } catch (error) {
    console.error('Failed to delete entry:', error)
    errorMessage.value = error?.message || '删除失败'
  }
}

function handleSearch() {
  loadEntries()
}

async function handleRefresh() {
  await loadCategoryTree()
  await loadEntries()
}

async function handleRebuildIndex() {
  isRebuilding.value = true
  errorMessage.value = ''
  feedbackMessage.value = ''

  try {
    const { rebuildKnowledgeBase } = await import('@/api')
    const response = await rebuildKnowledgeBase()
    feedbackMessage.value = response.message || '知识库刷新完成'
    await loadEntries()
  } catch (error) {
    console.error('Failed to rebuild knowledge base:', error)
    errorMessage.value = error?.response?.data?.message || error?.message || '知识库刷新失败'
  } finally {
    isRebuilding.value = false
  }
}

function formatDate(timestamp) {
  if (!timestamp) return '暂无'

  return new Date(timestamp).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatCategoryPath(categories) {
  if (!categories || categories.length === 0) return '未分类'

  // Build path from root to leaf
  const pathParts = []
  const buildPath = (cats) => {
    if (!cats) return
    for (const cat of cats) {
      pathParts.push(cat.name)
      if (cat.children?.length) {
        buildPath(cat.children)
      }
    }
  }

  // Find the category path
  const findPath = (cats, targetId, currentPath) => {
    if (!cats) return null
    for (const cat of cats) {
      const newPath = [...currentPath, cat.name]
      if (cat.id === targetId) return newPath
      if (cat.children?.length) {
        const result = findPath(cat.children, targetId, newPath)
        if (result) return result
      }
    }
    return null
  }

  if (selectedCategoryId.value && selectedCategoryId.value !== 'uncategorized') {
    const path = findPath(categoryTree.value, selectedCategoryId.value, [])
    if (path) return path.join(' / ')
  }

  return '未分类'
}

function getEntryPreview(entry) {
  if (!entry) return ''
  const content = entry.content || entry.contentPreview || ''
  // Truncate to ~100 characters
  return content.length > 100 ? content.substring(0, 100) + '...' : content
}

// Lifecycle
onMounted(async () => {
  await loadCategoryTree()
  await loadEntries()
})
</script>

<style scoped>
.knowledge-view {
  flex: 1;
  width: 100%;
  min-width: 0;
  padding: 30px;
  overflow-y: auto;
  background: #f8fafc;
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.page-header h1 {
  font-size: 28px;
  color: #333;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.main-content {
  display: flex;
  gap: 20px;
  min-height: calc(100vh - 180px);
}

/* Left Panel */
.left-panel {
  width: 240px;
  flex-shrink: 0;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

.left-panel .panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ec;
}

.left-panel .panel-header h2 {
  margin: 0;
  font-size: 16px;
  color: #344054;
}

.category-tree-container {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
}

/* Right Panel */
.right-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 16px;
}

.list-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.list-title h2 {
  margin: 0;
  font-size: 20px;
  color: #344054;
}

.entry-count {
  color: #667085;
  font-size: 14px;
}

.list-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-box {
  position: relative;
}

.search-input {
  padding: 10px 14px;
  border: 1px solid #e4e7ec;
  border-radius: 10px;
  font-size: 14px;
  width: 220px;
  outline: none;
  transition: border-color 0.15s;
}

.search-input:focus {
  border-color: #667eea;
}

/* Entry List */
.entry-list {
  display: grid;
  gap: 16px;
}

.entry-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  padding: 20px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.entry-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.entry-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}

.entry-header h3 {
  margin: 0;
  color: #344054;
  font-size: 16px;
}

.entry-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag-badge {
  background: #eef2ff;
  color: #4052b5;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.entry-path {
  color: #667085;
  font-size: 13px;
  margin: 0 0 12px;
}

.entry-preview {
  color: #475467;
  line-height: 1.6;
  margin-bottom: 12px;
  white-space: pre-wrap;
  overflow-wrap: break-word;
}

.entry-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #98a2b3;
  font-size: 13px;
}

/* Empty State */
.empty-state {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  padding: 60px 40px;
  text-align: center;
  color: #667085;
  font-size: 14px;
}

/* Messages */
.feedback-message,
.error-message {
  padding: 14px 18px;
  border-radius: 12px;
  margin-top: 16px;
  font-size: 14px;
}

.feedback-message {
  background: #ecfdf3;
  color: #027a48;
}

.error-message {
  background: #fef3f2;
  color: #b42318;
}

/* Buttons */
.primary-btn,
.secondary-btn {
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.primary-btn {
  background: #667eea;
  color: white;
}

.primary-btn:hover {
  background: #5a6fd6;
}

.secondary-btn {
  background: #eef2ff;
  color: #4052b5;
}

.secondary-btn:hover {
  background: #e0e7ff;
}

.primary-btn:disabled,
.secondary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Responsive */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }

  .left-panel {
    width: 100%;
  }
}
</style>
