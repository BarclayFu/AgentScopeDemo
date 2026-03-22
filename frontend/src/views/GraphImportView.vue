<template>
  <div class="graph-import-view">
    <header class="page-header">
      <div>
        <h1>◉ 知识图谱导入</h1>
        <p class="subtitle">输入知识条目，预览并导入到知识图谱</p>
      </div>
      <div class="header-actions">
        <button class="secondary-btn" @click="goToGraphView">
          查看图谱
        </button>
      </div>
    </header>

    <!-- 提示信息 -->
    <div class="tips-panel">
      <h3>📋 格式说明</h3>
      <p>支持以下格式，系统会自动识别其中的实体和关系：</p>
      <ul>
        <li><strong>产品：</strong>产品名称 → 如"产品：智能手表"</li>
        <li><strong>问答：</strong>问？\n答： → 如"智能手表如何保修？\n答案：联系客服"</li>
        <li><strong>服务：</strong>保修、质保、维修、退换、退款 等关键词</li>
      </ul>
    </div>

    <div class="content-grid">
      <!-- 输入面板 -->
      <section class="panel input-panel">
        <div class="panel-header">
          <h2>输入知识条目</h2>
        </div>

        <div class="form-field">
          <label>标题</label>
          <input
            v-model="form.title"
            type="text"
            class="text-input"
            placeholder="例如：智能手表保修说明"
          />
        </div>

        <div class="form-field">
          <label>内容</label>
          <textarea
            v-model="form.content"
            class="text-area"
            rows="10"
            placeholder="粘贴知识内容，支持以下格式：

产品：智能手表
保修：两年质保

智能手表如何保修？
答案：请联系客服处理。

退换货政策：收货7天内可申请退换。"
          ></textarea>
        </div>

        <div class="form-actions">
          <button
            class="secondary-btn"
            :disabled="isPreviewing || !canPreview"
            @click="previewExtract"
          >
            {{ isPreviewing ? '预览中...' : '预览抽取结果' }}
          </button>
          <button
            class="primary-btn"
            :disabled="isSubmitting || !canSubmit"
            @click="submitEntry"
          >
            {{ isSubmitting ? '导入中...' : '导入到图谱' }}
          </button>
        </div>
      </section>

      <!-- 预览面板 -->
      <section class="panel preview-panel">
        <div class="panel-header">
          <h2>抽取预览</h2>
          <span v-if="previewResult" class="result-count">
            共 {{ previewResult.totalCount }} 个三元组
          </span>
        </div>

        <div v-if="!previewResult && !errorMessage" class="empty-state">
          点击"预览抽取结果"查看将提取的三元组
        </div>

        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>

        <div v-if="previewResult" class="preview-results">
          <!-- 预处理实体信息 -->
          <div class="preview-section" v-if="hasPreprocessedEntities">
            <h3>🔍 识别的实体</h3>
            <div class="entity-tags">
              <span
                v-for="(value, key) in previewResult.preprocessedEntities"
                :key="key"
                class="entity-tag"
              >
                <strong>{{ key }}:</strong>
                <template v-if="Array.isArray(value)">
                  <span
                    v-for="(item, idx) in value"
                    :key="idx"
                    class="tag-item"
                    :class="key"
                  >
                    {{ typeof item === 'object' ? item.question + '→' + item.answer : item }}
                  </span>
                </template>
              </span>
            </div>
          </div>

          <!-- 规则抽取结果 -->
          <div class="preview-section" v-if="previewResult.ruleTriples.length > 0">
            <h3>📌 规则抽取 ({{ previewResult.ruleTriples.length }})</h3>
            <div class="triple-list">
              <div
                v-for="(triple, idx) in previewResult.ruleTriples"
                :key="'rule-' + idx"
                class="triple-item"
              >
                <span class="subject">{{ triple.subject }}</span>
                <span class="relation">-[{{ triple.relation }}]-></span>
                <span class="object">{{ triple.object }}</span>
              </div>
            </div>
          </div>

          <!-- LLM抽取结果 -->
          <div class="preview-section" v-if="previewResult.llmTriples.length > 0">
            <h3>🤖 LLM抽取 ({{ previewResult.llmTriples.length }})</h3>
            <div class="triple-list">
              <div
                v-for="(triple, idx) in previewResult.llmTriples"
                :key="'llm-' + idx"
                class="triple-item llm"
              >
                <span class="subject">{{ triple.subject }}</span>
                <span class="relation">-[{{ triple.relation }}]-></span>
                <span class="object">{{ triple.object }}</span>
              </div>
            </div>
          </div>

          <!-- 无LLM结果提示 -->
          <div v-if="previewResult.llmTriples.length === 0 && previewResult.ruleTriples.length > 0" class="llm-hint">
            <p>💡 LLM抽取未返回结果（可能需要配置MiniMax API Key）</p>
          </div>
        </div>
      </section>
    </div>

    <!-- 成功提示 -->
    <div v-if="successMessage" class="success-toast">
      {{ successMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { previewTripleExtraction, createKnowledgeEntry } from '@/api'

const router = useRouter()

const form = reactive({
  title: '',
  content: ''
})

const previewResult = ref(null)
const isPreviewing = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const canPreview = computed(() => {
  return form.content.trim().length > 0
})

const canSubmit = computed(() => {
  return form.title.trim().length > 0 && form.content.trim().length > 0
})

const hasPreprocessedEntities = computed(() => {
  if (!previewResult.value || !previewResult.value.preprocessedEntities) return false
  const entities = previewResult.value.preprocessedEntities
  return entities.products?.length > 0 ||
         entities.services?.length > 0 ||
         entities.orders?.length > 0 ||
         entities.qas?.length > 0
})

async function previewExtract() {
  if (!canPreview.value) return

  isPreviewing.value = true
  errorMessage.value = ''

  try {
    previewResult.value = await previewTripleExtraction(form.title, form.content)
  } catch (error) {
    errorMessage.value = error?.message || '预览失败，请稍后重试'
    previewResult.value = null
  } finally {
    isPreviewing.value = false
  }
}

async function submitEntry() {
  if (!canSubmit.value) return

  isSubmitting.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await createKnowledgeEntry({
      title: form.title,
      content: form.content
    })

    successMessage.value = '知识条目已导入到图谱！'

    // 清空表单
    form.title = ''
    form.content = ''
    previewResult.value = null

    // 3秒后隐藏成功提示
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  } catch (error) {
    errorMessage.value = error?.message || '导入失败，请稍后重试'
  } finally {
    isSubmitting.value = false
  }
}

function goToGraphView() {
  router.push('/graph')
}
</script>

<style scoped>
.graph-import-view {
  padding: 30px;
  overflow-y: auto;
  height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 28px;
  color: #333;
  margin: 0 0 8px;
}

.subtitle {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.tips-panel {
  background: #f0f9ff;
  border: 1px solid #b3e0ff;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.tips-panel h3 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #0066cc;
}

.tips-panel p {
  margin: 0 0 8px;
  font-size: 13px;
  color: #555;
}

.tips-panel ul {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #555;
}

.tips-panel li {
  margin-bottom: 4px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.panel {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.panel-header h2 {
  margin: 0;
  font-size: 18px;
  color: #344054;
}

.result-count {
  font-size: 13px;
  color: #667eea;
  font-weight: 500;
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
  min-height: 200px;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.primary-btn,
.secondary-btn {
  border: none;
  border-radius: 10px;
  padding: 11px 20px;
  font-size: 14px;
  cursor: pointer;
}

.primary-btn {
  background: #667eea;
  color: white;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.secondary-btn {
  background: #eef2ff;
  color: #4052b5;
}

.secondary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #667085;
  font-size: 14px;
  background: #f8fafc;
  border-radius: 12px;
}

.error-message {
  padding: 16px;
  background: #fef3f2;
  color: #b42318;
  border-radius: 8px;
  font-size: 14px;
}

.preview-results {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.preview-section h3 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #344054;
}

.entity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.entity-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f0f9ff;
  border-radius: 16px;
  font-size: 13px;
}

.entity-tag strong {
  color: #0066cc;
}

.tag-item {
  color: #344054;
}

.tag-item.products {
  background: #dbeafe;
  padding: 2px 8px;
  border-radius: 8px;
}

.tag-item.services {
  background: #dcfce7;
  padding: 2px 8px;
  border-radius: 8px;
}

.tag-item.qas {
  background: #fef3c7;
  padding: 2px 8px;
  border-radius: 8px;
}

.triple-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.triple-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 13px;
}

.triple-item.llm {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.subject {
  color: #3b82f6;
  font-weight: 500;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relation {
  color: #64748b;
  font-family: monospace;
}

.object {
  color: #10b981;
  font-weight: 500;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.llm-hint {
  padding: 12px 16px;
  background: #fef9e6;
  border-radius: 8px;
  font-size: 13px;
  color: #92600a;
}

.success-toast {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  padding: 14px 28px;
  background: #059669;
  color: white;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 20px rgba(5, 150, 105, 0.3);
  z-index: 1000;
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
