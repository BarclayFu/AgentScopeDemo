import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getGraphStats, getGraphNodes, getGraphEdges } from '@/api'

export const useGraphStore = defineStore('graph', () => {
  const nodes = ref([])
  const edges = ref([])
  const stats = ref({ nodeCount: 0, edgeCount: 0 })
  const selectedNodeId = ref(null)
  const highlightedPath = ref([])
  const isLoading = ref(false)

  async function fetchGraphData() {
    isLoading.value = true
    try {
      const [nodesRes, edgesRes, statsRes] = await Promise.all([
        getGraphNodes(1000, 0),
        getGraphEdges(1000, 0),
        getGraphStats()
      ])
      nodes.value = nodesRes
      edges.value = edgesRes
      stats.value = statsRes
    } finally {
      isLoading.value = false
    }
  }

  function selectNode(nodeId) {
    selectedNodeId.value = nodeId
  }

  function highlightPath(path) {
    highlightedPath.value = path
  }

  function clearHighlight() {
    highlightedPath.value = []
  }

  return {
    nodes,
    edges,
    stats,
    selectedNodeId,
    highlightedPath,
    isLoading,
    fetchGraphData,
    selectNode,
    highlightPath,
    clearHighlight
  }
})
