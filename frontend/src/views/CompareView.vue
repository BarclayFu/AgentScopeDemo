<template>
  <div class="compare-view">
    <header class="compare-header">
      <h1>GraphRAG vs VectorRAG 对比实验</h1>
    </header>

    <div class="query-input">
      <input v-model="query" placeholder="输入问题进行对比..." @keyup.enter="runComparison" />
      <button @click="runComparison" :disabled="isLoading">开始对比</button>
    </div>

    <div class="comparison-results" v-if="result">
      <div class="result-panel vector-panel">
        <h2>Vector RAG</h2>
        <div class="answer">
          <h3>答案:</h3>
          <p>{{ result.vectorResult.answer || '无结果' }}</p>
        </div>
        <div class="retrieved">
          <h3>检索到的知识片段:</h3>
          <ul>
            <li v-for="(chunk, i) in result.vectorResult.retrievedChunks" :key="i">
              {{ chunk.content }} (score: {{ chunk.score?.toFixed(2) }})
            </li>
          </ul>
        </div>
      </div>

      <div class="result-panel graph-panel">
        <h2>Graph RAG</h2>
        <div class="answer">
          <h3>答案:</h3>
          <p>{{ result.graphResult.answer || '无结果' }}</p>
        </div>
        <div class="retrieved">
          <h3>检索到的实体:</h3>
          <ul>
            <li v-for="(entity, i) in result.graphResult.retrievedEntities" :key="i">
              <strong>{{ entity.entityName }}</strong> ({{ entity.entityType }})
              <span v-for="rel in entity.relations" :key="rel.path">
                - {{ rel.path }}
              </span>
            </li>
          </ul>
        </div>
        <div class="mini-graph" ref="miniGraphContainer"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import cytoscape from 'cytoscape'
import { compareSearch } from '@/api'

const query = ref('')
const isLoading = ref(false)
const result = ref(null)
const miniGraphContainer = ref(null)
let miniCy = null

async function runComparison() {
  if (!query.value.trim()) return
  isLoading.value = true
  try {
    result.value = await compareSearch(query.value, 5)
    updateMiniGraph()
  } finally {
    isLoading.value = false
  }
}

function updateMiniGraph() {
  if (!result.value?.graphResult?.subgraphNodes) return

  if (miniCy) miniCy.destroy()

  const elements = [
    ...result.value.graphResult.subgraphNodes.map(n => ({
      data: { id: n.id, name: n.name }
    })),
    ...result.value.graphResult.subgraphEdges.map(e => ({
      data: { source: e.source, target: e.target }
    }))
  ]

  if (elements.length === 0) return

  miniCy = cytoscape({
    container: miniGraphContainer.value,
    elements,
    style: [
      { selector: 'node', style: { 'label': 'data(name)', 'background-color': '#4a90e2' } },
      { selector: 'edge', style: { 'line-color': '#ccc' } }
    ],
    layout: { name: 'circle' }
  })
}
</script>

<style scoped>
.compare-view {
  padding: 1rem;
}

.query-input {
  display: flex;
  gap: 1rem;
  margin: 1rem 0;
}

.query-input input {
  flex: 1;
  padding: 0.5rem;
  font-size: 1rem;
}

.comparison-results {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-top: 1rem;
}

.result-panel {
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.mini-graph {
  height: 200px;
  border: 1px solid #eee;
  margin-top: 1rem;
}
</style>
