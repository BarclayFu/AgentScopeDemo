<script setup>
defineOptions({
  name: 'TagInput'
})

import { ref } from 'vue'

const props = defineProps({
  tags: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['add', 'remove'])

const newTagName = ref('')

const handleKeydown = (event) => {
  if (event.key === 'Enter' && newTagName.value.trim()) {
    event.preventDefault()
    emit('add', newTagName.value.trim())
    newTagName.value = ''
  }
}

const handleRemove = (tag) => {
  emit('remove', tag)
}
</script>

<template>
  <div class="tag-input">
    <div class="tags-container">
      <span
        v-for="tag in tags"
        :key="tag.id"
        class="tag-chip"
      >
        <span class="tag-name">{{ tag.name }}</span>
        <button
          type="button"
          class="tag-remove"
          @click="handleRemove(tag)"
        >
          &times;
        </button>
      </span>
    </div>
    <input
      v-model="newTagName"
      type="text"
      class="tag-input-field"
      placeholder="输入标签后按 Enter 添加"
      @keydown="handleKeydown"
    />
  </div>
</template>

<style scoped>
.tag-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 32px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background-color: #e8f5e9;
  color: #2e7d32;
  border-radius: 16px;
  font-size: 13px;
  transition: background-color 0.15s;
}

.tag-chip:hover {
  background-color: #c8e6c9;
}

.tag-name {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  padding: 0;
  background: none;
  border: none;
  border-radius: 50%;
  color: #2e7d32;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  transition: background-color 0.15s;
}

.tag-remove:hover {
  background-color: #a5d6a7;
}

.tag-input-field {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s;
}

.tag-input-field:focus {
  border-color: #4caf50;
}

.tag-input-field::placeholder {
  color: #999;
}
</style>
