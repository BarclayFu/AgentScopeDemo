<script setup>
defineOptions({
  name: 'CategoryTree'
})

const props = defineProps({
  categories: {
    type: Array,
    default: () => []
  },
  selectedId: {
    type: String,
    default: null
  },
  expandedIds: {
    type: Set,
    default: () => new Set()
  },
  uncategorizedCount: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['select', 'toggle-expand', 'add-category'])

const isExpanded = (id) => props.expandedIds.has(id)

const handleSelect = (category) => {
  emit('select', category)
}

const handleToggleExpand = (category) => {
  emit('toggle-expand', category)
}

const handleAddCategory = (parentId = null) => {
  emit('add-category', parentId)
}
</script>

<template>
  <div class="category-tree">
    <div
      v-for="category in categories"
      :key="category.id"
      class="category-item"
    >
      <div
        class="category-row"
        :class="{ active: category.id === selectedId }"
        :style="{ paddingLeft: `${category.level * 16 + 8}px` }"
        @click="handleSelect(category)"
      >
        <span
          class="expand-icon"
          @click.stop="handleToggleExpand(category)"
        >
          <template v-if="category.children && category.children.length > 0">
            {{ isExpanded(category.id) ? '▼' : '▶' }}
          </template>
        </span>
        <span class="category-name">{{ category.name }}</span>
        <span class="entry-count">({{ category.entryCount }})</span>
      </div>
      <template v-if="category.children && category.children.length > 0 && isExpanded(category.id)">
        <CategoryTree
          :categories="category.children"
          :selected-id="selectedId"
          :expanded-ids="expandedIds"
          :uncategorized-count="0"
          @select="emit('select', $event)"
          @toggle-expand="emit('toggle-expand', $event)"
          @add-category="emit('add-category', $event)"
        />
      </template>
    </div>
    <div
      v-if="uncategorizedCount > 0"
      class="category-item uncategorized"
    >
      <div
        class="category-row"
        :class="{ active: selectedId === 'uncategorized' }"
        style="padding-left: 8px"
        @click="handleSelect({ id: 'uncategorized', name: '未分类' })"
      >
        <span class="expand-icon"></span>
        <span class="category-name">未分类</span>
        <span class="entry-count">({{ uncategorizedCount }})</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.category-tree {
  font-size: 14px;
}

.category-item {
  user-select: none;
}

.category-row {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.15s;
}

.category-row:hover {
  background-color: #f5f5f5;
}

.category-row.active {
  background-color: #e3f2fd;
  color: #1976d2;
}

.expand-icon {
  width: 16px;
  margin-right: 4px;
  font-size: 10px;
  color: #666;
}

.category-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-count {
  margin-left: 8px;
  color: #999;
  font-size: 12px;
}

.uncategorized {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #eee;
}
</style>
