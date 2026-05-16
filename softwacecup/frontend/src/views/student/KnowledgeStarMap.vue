<template>
  <div class="knowledge-star-map">
    <!-- Header -->
    <div class="map-header">
      <h2 class="page-title">知识星图</h2>
      <div class="map-legend">
        <span class="legend-item"><span class="dot dot-gray"></span>未学习</span>
        <span class="legend-item"><span class="dot dot-blue"></span>学习中</span>
        <span class="legend-item"><span class="dot dot-green"></span>已掌握</span>
        <span class="legend-item"><span class="dot dot-gold"></span>精通</span>
      </div>
    </div>

    <!-- Chart Container -->
    <div ref="chartRef" class="chart-container"></div>

    <!-- Node Detail Popup -->
    <transition name="popup-fade">
      <div v-if="showDetail && selectedNode" class="detail-popup">
        <button class="popup-close" @click="showDetail = false">&times;</button>
        <div class="popup-content">
          <h3 class="popup-title">{{ selectedNode.name || selectedNode.label || '知识点' }}</h3>

          <div class="popup-section">
            <span class="popup-label">掌握度</span>
            <div class="mastery-bar-wrap">
              <div class="mastery-bar">
                <div
                  class="mastery-fill"
                  :style="{ width: (selectedNode.mastery || 0) + '%', background: masteryColor(selectedNode.mastery) }"
                ></div>
              </div>
              <span class="mastery-text">{{ selectedNode.mastery || 0 }}%</span>
            </div>
          </div>

          <div class="popup-section" v-if="selectedNode.练习记录 || selectedNode.practiceCount">
            <span class="popup-label">练习记录</span>
            <span class="popup-value">{{ selectedNode.练习记录 || selectedNode.practiceCount || 0 }} 次练习</span>
          </div>

          <button
            class="popup-btn"
            @click="goToLesson(selectedNode)"
          >
            去学习
          </button>
        </div>
      </div>
    </transition>

    <!-- Empty State -->
    <div v-if="!loading && nodes.length === 0" class="empty-state">
      <div class="empty-icon">🗺️</div>
      <p class="empty-text">暂无知识图谱数据</p>
      <p class="empty-hint">开始学习课程后，知识星图将自动生成</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { apiKnowledgeGraphFull, apiKnowledgeGraphProgress } from '../../api/index.js'

const router = useRouter()
const chartRef = ref(null)
const chartRefs = chartRef
let chartInstance = null
const selectedNode = ref(null)
const showDetail = ref(false)
const loading = ref(true)
const nodes = ref([])

function masteryColor(mastery) {
  if (mastery >= 80) return '#fbbf24'
  if (mastery >= 60) return '#10b981'
  if (mastery > 0) return '#3b82f6'
  return 'rgba(255,255,255,0.15)'
}

onMounted(async () => {
  try {
    const [graphRes, progressRes] = await Promise.all([
      apiKnowledgeGraphFull(),
      apiKnowledgeGraphProgress(),
    ])

    const rawNodes = (graphRes.data?.data?.nodes || graphRes.data?.nodes || [])
    const rawEdges = (graphRes.data?.data?.edges || graphRes.data?.edges || [])
    const progressList = (progressRes.data?.data || progressRes.data || [])

    nodes.value = rawNodes.map(node => {
      const prog = progressList.find(p => {
        const pid = p.knowledgePointId || p.knowledgePoint_id || p.kpId || p.id
        return String(pid) === String(node.id)
      })
      const mastery = prog?.mastery !== undefined ? prog.mastery : 0
      let itemStyle = { color: 'rgba(255,255,255,0.15)' } // gray = not started
      if (mastery >= 80) {
        itemStyle = { color: '#fbbf24', borderColor: '#f59e0b', borderWidth: 2 }
      } else if (mastery >= 60) {
        itemStyle = { color: '#10b981', borderColor: '#34d399', borderWidth: 2 }
      } else if (mastery > 0) {
        itemStyle = { color: '#3b82f6', borderColor: '#60a5fa', borderWidth: 2 }
      }
      return {
        ...node,
        name: node.name || node.label || node.title || '知识点',
        mastery,
        itemStyle,
        symbolSize: Math.max(20, (node.weight || node.importance || 1) * 10),
      }
    })

    const edges = rawEdges.map(e => ({
      source: e.source || e.from,
      target: e.target || e.to,
    }))

    if (nodes.value.length === 0) {
      loading.value = false
      return
    }

    await nextTick()
    if (!chartRef.value) {
      loading.value = false
      return
    }

    chartInstance = echarts.init(chartRef.value)
    chartInstance.setOption({
      backgroundColor: 'transparent',
      series: [{
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        data: nodes.value,
        edges: edges,
        force: {
          repulsion: 300,
          edgeLength: 120,
          friction: 0.1,
          gravity: 0.05,
        },
        label: {
          show: true,
          position: 'bottom',
          color: 'rgba(255,255,255,0.6)',
          fontSize: 11,
        },
        lineStyle: {
          color: 'rgba(255,255,255,0.12)',
          width: 1,
          curveness: 0.3,
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 2 },
        },
        edgeSymbol: ['none', 'none'],
        edgeLabel: { show: false },
      }],
    })

    chartInstance.on('click', (params) => {
      if (params.dataType === 'node') {
        const node = nodes.value.find(n => String(n.id) === String(params.data.id))
        if (node) {
          selectedNode.value = node
          showDetail.value = true
        }
      }
    })

    window.addEventListener('resize', handleResize)
  } catch (e) {
    console.warn('Failed to load knowledge graph', e)
  } finally {
    loading.value = false
  }
})

function handleResize() {
  chartInstance?.resize()
}

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})

function goToLesson(node) {
  const lessonId = node.lessonId || node.lesson_id || node.lesson
  if (lessonId) {
    router.push(`/student/lessons/${lessonId}`)
  } else {
    router.push('/student/courses')
  }
  showDetail.value = false
}
</script>

<style scoped>
.knowledge-star-map {
  position: relative;
  width: 100%;
  min-height: calc(100vh - 116px);
  background: transparent;
  overflow: hidden;
}

.map-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px 12px;
  flex-wrap: wrap;
  gap: 10px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #e6edf3;
  margin: 0;
}

.map-legend {
  display: flex;
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-gray {
  background: rgba(255, 255, 255, 0.15);
}

.dot-blue {
  background: #3b82f6;
  border: 1.5px solid #60a5fa;
}

.dot-green {
  background: #10b981;
  border: 1.5px solid #34d399;
}

.dot-gold {
  background: #fbbf24;
  border: 1.5px solid #f59e0b;
}

.chart-container {
  width: 100%;
  height: calc(100vh - 176px);
  min-height: 400px;
}

/* Detail Popup */
.detail-popup {
  position: absolute;
  top: 50%;
  right: 24px;
  transform: translateY(-50%);
  width: 280px;
  background: rgba(18, 22, 30, 0.92);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.4);
  z-index: 10;
}

.popup-close {
  position: absolute;
  top: 8px;
  right: 12px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.4);
  font-size: 22px;
  cursor: pointer;
  line-height: 1;
  padding: 4px;
  transition: color 0.2s;
}

.popup-close:hover {
  color: rgba(255, 255, 255, 0.8);
}

.popup-content {
  display: grid;
  gap: 16px;
}

.popup-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #e6edf3;
  padding-right: 20px;
}

.popup-section {
  display: grid;
  gap: 6px;
}

.popup-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.popup-value {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.mastery-bar-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mastery-bar {
  flex: 1;
  height: 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.mastery-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.mastery-text {
  font-size: 14px;
  font-weight: 600;
  color: #e6edf3;
  min-width: 36px;
  text-align: right;
}

.popup-btn {
  width: 100%;
  padding: 10px 16px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 4px;
}

.popup-btn:hover {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.3);
}

/* Transitions */
.popup-fade-enter-active,
.popup-fade-leave-active {
  transition: all 0.25s ease;
}

.popup-fade-enter-from,
.popup-fade-leave-to {
  opacity: 0;
  transform: translateY(-50%) translateX(10px);
}

/* Empty State */
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
}

.empty-text {
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
  margin: 0 0 6px;
}

.empty-hint {
  color: rgba(255, 255, 255, 0.3);
  font-size: 13px;
  margin: 0;
}
</style>
