<script setup>
defineOptions({
  name: 'EntryDetailModal'
})

import { ref, watch, computed } from 'vue'
import TagInput from './TagInput.vue'
import CategoryTree from './CategoryTree.vue'
import CytoscapeGraph from './CytoscapeGraph.vue'
import { getEntryGraph, createTag } from '@/api'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  entry: {
    type: Object,
    default: null
  },
  categoryTree: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'save', 'delete'])

// Local editing state
const editedTitle = ref('')
const editedContent = ref('')
const editedTags = ref([])
const editedCategoryId = ref(null)

// Category selector modal
const showCategorySelector = ref(false)
const categoryExpandedIds = ref(new Set())

// Graph data
const graphData = ref(null)
const graphLoading = ref(false)

// Functions (defined before watches that use them)
const loadGraphData = async (entryId) => {
  if (!entryId) return

  graphLoading.value = true
  graphData.value = null

  try {
    const data = await getEntryGraph(entryId)
    graphData.value = data
  } catch (error) {
    console.error('Failed to load entry graph:', error)
    graphData.value = { nodes: [], edges: [] }
  } finally {
    graphLoading.value = false
  }
}

const resetState = () => {
  editedTitle.value = ''
  editedContent.value = ''
  editedTags.value = []
  editedCategoryId.value = null
  graphData.value = null
  graphLoading.value = false
  showCategorySelector.value = false
  categoryExpandedIds.value = new Set()
}

// Watch entry prop changes to initialize editing state and load graph
watch(
  () => props.entry,
  (newEntry) => {
    if (newEntry) {
      editedTitle.value = newEntry.title || ''
      editedContent.value = newEntry.content || ''
      editedTags.value = Array.isArray(newEntry.tags) ? [...newEntry.tags] : []
      editedCategoryId.value = newEntry.categoryId || null

      // Load graph data when entry changes
      loadGraphData(newEntry.id)
    } else {
      resetState()
    }
  },
  { immediate: true }
)

// Also load graph when show becomes true
watch(
  () => props.show,
  (newShow) => {
    if (newShow && props.entry) {
      loadGraphData(props.entry.entryId)
    }
  }
)

// Computed properties
const selectedCategoryName = computed(() => {
  if (!props.categoryTree || !editedCategoryId.value) return '未分类'

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

  const category = findCategory(props.categoryTree, editedCategoryId.value)
  return category?.name || '未分类'
})

// Tag handling
const handleAddTag = async (tagName) => {
  try {
    const newTag = await createTag(tagName)
    editedTags.value.push(newTag)
  } catch (error) {
    console.error('Failed to create tag:', error)
    // Still add the tag locally if API fails
    editedTags.value.push({ id: null, name: tagName })
  }
}

const handleRemoveTag = (tag) => {
  editedTags.value = editedTags.value.filter(t => t.id !== tag.id && t.name !== tag.name)
}

// Category handling
const handleOpenCategorySelector = () => {
  showCategorySelector.value = true
}

const handleCloseCategorySelector = () => {
  showCategorySelector.value = false
}

const handleSelectCategory = (category) => {
  editedCategoryId.value = category.id === 'uncategorized' ? null : category.id
  showCategorySelector.value = false
}

const handleToggleCategoryExpand = (category) => {
  if (categoryExpandedIds.value.has(category.id)) {
    categoryExpandedIds.value.delete(category.id)
  } else {
    categoryExpandedIds.value.add(category.id)
  }
  // Trigger reactivity
  categoryExpandedIds.value = new Set(categoryExpandedIds.value)
}

// Actions
const handleClose = () => {
  emit('close')
}

const handleSave = () => {
  const updatedEntry = {
    ...props.entry,
    title: editedTitle.value,
    content: editedContent.value,
    tags: editedTags.value,
    categoryId: editedCategoryId.value
  }
  emit('save', updatedEntry)
}

const handleDelete = () => {
  if (confirm('确定要删除这个条目吗？此操作不可撤销。')) {
    emit('delete', props.entry.entryId)
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="show" class="modal-overlay" @click.self="handleClose">
      <div class="modal-container">
        <div class="modal-header">
          <h2 class="modal-title">条目详情</h2>
          <button class="close-button" @click="handleClose">&times;</button>
        </div>

        <div class="modal-body">
          <!-- Left Panel: Edit Form -->
          <div class="left-panel">
            <!-- Title -->
            <div class="form-group">
              <label class="form-label">标题</label>
              <input
                v-model="editedTitle"
                type="text"
                class="form-input"
                placeholder="输入标题"
              />
            </div>

            <!-- Tags -->
            <div class="form-group">
              <label class="form-label">标签</label>
              <TagInput
                :tags="editedTags"
                @add="handleAddTag"
                @remove="handleRemoveTag"
              />
            </div>

            <!-- Category -->
            <div class="form-group">
              <label class="form-label">分类</label>
              <button
                class="category-selector-button"
                @click="handleOpenCategorySelector"
              >
                {{ selectedCategoryName }}
                <span class="selector-arrow">▼</span>
              </button>
            </div>

            <!-- Content -->
            <div class="form-group content-group">
              <label class="form-label">内容</label>
              <textarea
                v-model="editedContent"
                class="form-textarea"
                placeholder="输入内容"
                rows="10"
              ></textarea>
            </div>

            <!-- Action Buttons -->
            <div class="action-buttons">
              <button class="btn btn-primary" @click="handleSave">
                保存
              </button>
              <button class="btn btn-danger" @click="handleDelete">
                删除
              </button>
            </div>
          </div>

          <!-- Right Panel: Knowledge Graph -->
          <div class="right-panel">
            <div class="graph-header">
              <h3 class="graph-title">知识图谱</h3>
              <span v-if="graphData" class="graph-stats">
                {{ graphData.nodes?.length || 0 }} 节点 / {{ graphData.edges?.length || 0 }} 边
              </span>
            </div>
            <div class="graph-container">
              <!-- Loading State -->
              <div v-if="graphLoading" class="graph-loading">
                <div class="spinner"></div>
                <span>加载图谱数据...</span>
              </div>

              <!-- Graph with Cytoscape -->
              <CytoscapeGraph
                v-else-if="graphData"
                :nodes="graphData.nodes || []"
                :edges="graphData.edges || []"
                height="100%"
              />

              <!-- No Entry Selected -->
              <div v-else class="graph-placeholder">
                <div class="graph-empty">选择一个条目查看知识图谱</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Category Selector Modal -->
        <div v-if="showCategorySelector" class="category-modal-overlay" @click.self="handleCloseCategorySelector">
          <div class="category-modal">
            <div class="category-modal-header">
              <h3>选择分类</h3>
              <button class="close-button" @click="handleCloseCategorySelector">&times;</button>
            </div>
            <div class="category-modal-body">
              <CategoryTree
                :categories="categoryTree"
                :selected-id="editedCategoryId"
                :expanded-ids="categoryExpandedIds"
                @select="handleSelectCategory"
                @toggle-expand="handleToggleCategoryExpand"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-container {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 1000px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e0e0e0;
}

.modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.close-button:hover {
  color: #333;
}

.modal-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.left-panel {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  border-right: 1px solid #e0e0e0;
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow: hidden;
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s;
}

.form-input:focus {
  border-color: #4caf50;
}

.content-group {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.form-textarea {
  width: 100%;
  flex: 1;
  min-height: 200px;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  resize: vertical;
  transition: border-color 0.15s;
  font-family: inherit;
}

.form-textarea:focus {
  border-color: #4caf50;
}

.category-selector-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  font-size: 14px;
  cursor: pointer;
  transition: border-color 0.15s;
}

.category-selector-button:hover {
  border-color: #4caf50;
}

.selector-arrow {
  font-size: 10px;
  color: #666;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-primary {
  background-color: #4caf50;
  color: white;
}

.btn-primary:hover {
  background-color: #43a047;
}

.btn-danger {
  background-color: #f44336;
  color: white;
}

.btn-danger:hover {
  background-color: #e53935;
}

/* Graph Panel Styles */
.graph-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.graph-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.graph-stats {
  font-size: 12px;
  color: #666;
}

.graph-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  min-height: 300px;
  overflow: hidden;
}

.graph-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #666;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e0e0e0;
  border-top-color: #4caf50;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.graph-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 100%;
}

.graph-empty {
  color: #999;
  font-size: 14px;
}

/* Category Modal Styles */
.category-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
}

.category-modal {
  background: white;
  border-radius: 8px;
  width: 320px;
  max-height: 400px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.category-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #e0e0e0;
}

.category-modal-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.category-modal-body {
  padding: 12px;
  overflow-y: auto;
  max-height: 340px;
}
</style>
