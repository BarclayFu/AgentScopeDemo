<template>
  <div class="graph-view">
    <header class="graph-header">
      <h1>知识图谱</h1>
      <div class="graph-stats">
        <span>节点: {{ graphStore.stats.nodeCount }}</span>
        <span>边: {{ graphStore.stats.edgeCount }}</span>
        <button @click="graphStore.fetchGraphData" :disabled="graphStore.isLoading">
          {{ graphStore.isLoading ? '加载中...' : '刷新' }}
        </button>
      </div>
    </header>

    <div class="graph-container" ref="graphContainer"></div>

    <aside class="node-detail" v-if="selectedNode">
      <h3>{{ selectedNode.name }}</h3>
      <p>类型: {{ selectedNode.type }}</p>
      <div v-for="(value, key) in selectedNode.properties" :key="key">
        <strong>{{ key }}:</strong> {{ value }}
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import cytoscape from 'cytoscape'
import { useGraphStore } from '@/stores/graph'

const graphStore = useGraphStore()
const graphContainer = ref(null)
let cy = null

const selectedNode = computed(() => {
  if (!graphStore.selectedNodeId) return null
  return graphStore.nodes.find(n => n.id === graphStore.selectedNodeId)
})

onMounted(async () => {
  await graphStore.fetchGraphData()
  initCytoscape()
})

onUnmounted(() => {
  if (cy) cy.destroy()
})

watch([() => graphStore.nodes, () => graphStore.edges], () => {
  if (cy) updateGraph()
}, { deep: true })

function initCytoscape() {
  cy = cytoscape({
    container: graphContainer.value,
    style: [
      {
        selector: 'node',
        style: {
          'label': 'data(name)',
          'background-color': '#666',
          'width': 40,
          'height': 40
        }
      },
      {
        selector: 'edge',
        style: {
          'width': 2,
          'line-color': '#ccc',
          'target-arrow-color': '#ccc',
          'target-arrow-shape': 'triangle'
        }
      },
      {
        selector: '.highlighted',
        style: {
          'background-color': '#ff6b6b',
          'line-color': '#ff6b6b',
          'target-arrow-color': '#ff6b6b',
          'width': 4
        }
      }
    ],
    layout: { name: 'circle' }
  })

  cy.on('tap', 'node', (evt) => {
    graphStore.selectNode(evt.target.id())
  })

  updateGraph()
}

function updateGraph() {
  if (!cy) return

  cy.elements().remove()

  const elements = [
    ...graphStore.nodes.map(n => ({
      data: { id: n.id, name: n.name, type: n.type },
      group: 'nodes'
    })),
    ...graphStore.edges.map(e => ({
      data: { id: e.id, source: e.source, target: e.target, relation: e.relation },
      group: 'edges'
    }))
  ]

  cy.add(elements)
  cy.layout({ name: 'circle' }).run()
}
</script>

<style scoped>
.graph-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 1rem;
}

.graph-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.graph-stats {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.graph-container {
  flex: 1;
  min-height: 400px;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.node-detail {
  position: absolute;
  right: 1rem;
  top: 100px;
  width: 250px;
  padding: 1rem;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
</style>
