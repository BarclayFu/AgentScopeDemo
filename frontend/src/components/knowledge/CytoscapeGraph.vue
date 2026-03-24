<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import cytoscape from 'cytoscape'
import dagre from 'cytoscape-dagre'

// Register dagre layout extension
cytoscape.use(dagre)

const props = defineProps({
  nodes: {
    type: Array,
    default: () => []
  },
  edges: {
    type: Array,
    default: () => []
  },
  height: {
    type: String,
    default: '300px'
  }
})

const containerRef = ref(null)
let cy = null

onMounted(() => {
  initCytoscape()
})

onUnmounted(() => {
  if (cy) cy.destroy()
})

watch([() => props.nodes, () => props.edges], () => {
  if (cy) updateGraph()
}, { deep: true })

function initCytoscape() {
  cy = cytoscape({
    container: containerRef.value,
    style: [
      {
        selector: 'node',
        style: {
          'label': 'data(name)',
          'background-color': '#667eea',
          'width': 60,
          'height': 60,
          'font-size': 11,
          'text-valign': 'bottom',
          'text-margin-y': 6,
          'color': '#333',
          'border-width': 3,
          'border-color': '#5a6fd6',
          'text-background-color': '#ffffff',
          'text-background-opacity': 1,
          'text-background-padding': '3px'
        }
      },
      {
        selector: 'node[type = "Concept"]',
        style: {
          'background-color': '#ff6b6b',
          'border-color': '#ee5a5a',
          'shape': 'round-rectangle'
        }
      },
      {
        selector: 'node[type = "Entity"]',
        style: {
          'background-color': '#4ecdc4',
          'border-color': '#3dbdb5'
        }
      },
      {
        selector: 'node[type = "Document"]',
        style: {
          'background-color': '#ffe66d',
          'border-color': '#ffd93d',
          'shape': 'round-rectangle'
        }
      },
      {
        selector: 'edge',
        style: {
          'width': 2,
          'line-color': '#94a3b8',
          'target-arrow-color': '#94a3b8',
          'target-arrow-shape': 'triangle',
          'curve-style': 'bezier',
          'label': 'data(relation)',
          'font-size': 9,
          'text-rotation': 'autorotate',
          'text-margin-y': -8,
          'edge-text-padding': '3px'
        }
      },
      {
        selector: '.highlighted-node',
        style: {
          'background-color': '#ffd93d',
          'border-color': '#f59e0b',
          'border-width': 4
        }
      },
      {
        selector: '.highlighted-edge',
        style: {
          'line-color': '#f59e0b',
          'target-arrow-color': '#f59e0b',
          'width': 3
        }
      },
      {
        selector: '.faded',
        style: {
          'opacity': 0.3
        }
      }
    ],
    wheelSensitivity: 0.3,
    minZoom: 0.5,
    maxZoom: 3
  })

  cy.on('tap', 'node', (evt) => {
    const node = evt.target

    // Reset all elements
    cy.elements().removeClass('highlighted-node highlighted-edge faded')

    // Highlight selected node and its neighborhood
    node.addClass('highlighted-node')
    node.connectedEdges().addClass('highlighted-edge')
    node.neighborhood('node').addClass('highlighted-node')
    node.neighborhood('edge').addClass('highlighted-edge')

    // Fade everything else
    cy.elements().not(node).not(node.connectedEdges()).not(node.neighborhood()).addClass('faded')
  })

  cy.on('tap', (evt) => {
    if (evt.target === cy) {
      cy.elements().removeClass('highlighted-node highlighted-edge faded')
    }
  })

  updateGraph()
}

function updateGraph() {
  if (!cy) return

  cy.elements().remove()

  if (props.nodes.length === 0) return

  // Build elements array
  const elements = []

  // Add nodes
  for (const node of props.nodes) {
    const id = node.id || node.name || `node-${Math.random().toString(36).substr(2, 9)}`
    elements.push({
      data: {
        id: String(id),
        name: node.name || node.id || '',
        type: node.type || 'default'
      },
      group: 'nodes'
    })
  }

  // Add edges - source/target must match node ids
  for (const edge of props.edges) {
    const sourceId = String(edge.source)
    const targetId = String(edge.target)

    // Only add edge if both source and target nodes exist
    const sourceNode = elements.find(e => e.data.id === sourceId)
    const targetNode = elements.find(e => e.data.id === targetId)

    if (sourceNode && targetNode) {
      const edgeId = edge.id || `${sourceId}-${targetId}`
      elements.push({
        data: {
          id: String(edgeId),
          source: sourceId,
          target: targetId,
          relation: edge.relation || edge.type || ''
        },
        group: 'edges'
      })
    }
  }

  if (elements.length > 0) {
    cy.add(elements)

    // Use a better layout
    const layout = cy.layout({
      name: 'dagre',
      rankDir: 'TB',
      nodeSep: 80,
      rankSep: 100,
      animate: true,
      animationDuration: 500,
      animationEasing: 'ease-out-cubic'
    })

    layout.run()
  }
}
</script>

<template>
  <div class="cytoscape-graph" :style="{ height }">
    <div ref="containerRef" class="graph-container"></div>
    <div v-if="nodes.length === 0" class="empty-state">
      <div class="empty-icon">🔗</div>
      <div class="empty-text">暂无关联数据</div>
      <div class="empty-hint">该条目尚未关联知识图谱节点</div>
    </div>
  </div>
</template>

<style scoped>
.cytoscape-graph {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 250px;
  border-radius: 8px;
  overflow: hidden;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.graph-container {
  width: 100%;
  height: 100%;
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.6;
}

.empty-text {
  color: #64748b;
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 4px;
}

.empty-hint {
  color: #94a3b8;
  font-size: 13px;
}
</style>
