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

    <!-- Tab Bar -->
    <div class="star-tabs">
      <button class="star-tab" :class="{ active: starTab === 'overview' }" @click="starTab = 'overview'">星图总览</button>
      <button class="star-tab" :class="{ active: starTab === 'path' }" @click="starTab = 'path'">学习路径</button>
      <button class="star-tab" :class="{ active: starTab === 'puzzle' }" @click="starTab = 'puzzle'">知识拼图</button>
    </div>

    <!-- Tab: 星图总览 -->
    <div v-show="starTab === 'overview'">
      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <p class="loading-text">加载中...</p>
      </div>

      <template v-else>
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
        <div v-if="nodes.length === 0" class="empty-state">
          <div class="empty-icon">🗺️</div>
          <p class="empty-text">暂无知识图谱数据</p>
          <p class="empty-hint">开始学习课程后，知识星图将自动生成</p>
        </div>
      </template>
    </div>

    <!-- Tab: 学习路径 -->
    <div v-show="starTab === 'path'" class="path-tab">
      <div v-if="pathLoading" class="loading-state"><p class="loading-text">加载中...</p></div>
      <div v-else class="path-content">
        <div class="path-chart" ref="pathChartRef"></div>
        <div v-if="nextRecommended.length > 0" class="path-recommend">
          <h3 class="section-title">AI 推荐下一步</h3>
          <div v-for="rec in nextRecommended" :key="rec.id" class="rec-item">
            <span class="rec-name">{{ rec.name }}</span>
            <span class="rec-reason">{{ rec.reason || '' }}</span>
            <button class="rec-btn" @click="goToLesson({ id: rec.lessonId || rec.id })">去学习</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab: 知识拼图 -->
    <div v-show="starTab === 'puzzle'" class="puzzle-tab">
      <div v-if="puzzleLoading" class="loading-state"><p class="loading-text">加载中...</p></div>
      <div v-else class="puzzle-grid">
        <div v-for="kp in puzzlePieces" :key="kp.id" class="puzzle-piece"
          :class="'mastery-' + Math.floor((kp.mastery || 0) / 25)"
          @click="goToLesson({ id: kp.lessonId || kp.id })">
          <span class="piece-name">{{ kp.name }}</span>
          <span class="piece-pct">{{ kp.mastery || 0 }}%</span>
        </div>
      </div>
      <div v-if="puzzlePieces.length === 0 && !puzzleLoading" class="empty-state">暂无知识点数据</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { apiKnowledgeGraphFull, apiKnowledgeGraphProgress, apiKnowledgeGraphNextRecommended } from '../../api/index.js'

const router = useRouter()
let echarts = null
const chartRef = ref(null)
let chartInstance = null
const selectedNode = ref(null)
const showDetail = ref(false)
const loading = ref(true)
const nodes = ref([])
const links = ref([])
const starTab = ref('overview')
const pathLoading = ref(false)
const nextRecommended = ref([])
const pathChartRef = ref(null)
const puzzleLoading = ref(false)
const puzzlePieces = ref([])

function masteryColor(mastery) {
  if (mastery >= 80) return '#fbbf24'
  if (mastery >= 60) return '#10b981'
  if (mastery > 0) return '#3b82f6'
  return 'rgba(255,255,255,0.15)'
}

async function ensureEcharts() {
  if (echarts && echarts.init) return true
  try {
    if (window.echarts) { echarts = window.echarts; return true }
    const mod = await import('echarts')
    echarts = mod.default || mod
    return !!(echarts && echarts.init)
  } catch { return false }
}

onMounted(async () => {
  try {
    loading.value = true
    const [graphRes, progressRes] = await Promise.all([
      apiKnowledgeGraphFull(),
      apiKnowledgeGraphProgress(),
    ])

    // API 响应已由http拦截器解包为 {success, data: {nodes, edges}}
    const payload = graphRes?.data || graphRes || {}
    const rawNodes = payload.nodes || []
    const rawEdges = payload.edges || []
    const progressList = progressRes?.data || progressRes || []

    nodes.value = rawNodes.map(node => {
      const prog = progressList.find(p => {
        const pid = p.knowledgePointId || p.knowledgePoint_id || p.kpId || p.id
        return String(pid) === String(node.id)
      })
      const mastery = prog?.mastery !== undefined ? prog.mastery : 0
      // color by mastery: 精通=金, 已掌握=绿, 学习中=蓝, 未学习=灰
      let color = '#334155' // gray = 未学习
      if (mastery >= 80) color = '#fbbf24'       // gold = 精通
      else if (mastery >= 60) color = '#10b981'   // green = 已掌握
      else if (mastery > 0) color = '#3b82f6'     // blue = 学习中
      return {
        name: node.name || node.label || node.title || '知识点',
        value: node.name,
        mastery,
        symbolSize: 28 + mastery * 0.2,
        itemStyle: { color },
        label: { show: true },
      }
    })

    const edges = rawEdges.map(e => ({
      source: e.source || e.from,
      target: e.target || e.to,
    }))
    links.value = edges

    // 先关闭loading让chart-container DOM渲染出来
    loading.value = false
    if (nodes.value.length === 0) return

    await nextTick()
    await new Promise(r => setTimeout(r, 100))
    if (!chartRef.value) {
      console.warn('chartRef still null after load')
      return
    }
    if (!(await ensureEcharts())) { loading.value = false; return }
    chartInstance = echarts.init(chartRef.value)
    chartInstance.resize()
    chartInstance.setOption({
      tooltip: { formatter: (p) => p.dataType === 'node' ? `<b>${p.data.name}</b><br/>掌握度: ${p.data.mastery || 0}%` : '' },
      series: [{
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        data: nodes.value,
        edges: edges,
        force: { repulsion: 400, edgeLength: 150, friction: 0.1, gravity: 0.03 },
        label: { show: true, position: 'bottom', color: '#cbd5e1', fontSize: 11, distance: 6 },
        lineStyle: { color: '#64748b', width: 1.2, curveness: 0.25, opacity: 1 },
        emphasis: { focus: 'adjacency', lineStyle: { width: 2, color: '#60d9fa' } },
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

watch(starTab, (tab) => {
  if (tab === 'path') loadPathTab()
  if (tab === 'puzzle') loadPuzzleTab()
})

async function loadPathTab() {
  if (nextRecommended.value.length > 0) return // cached
  pathLoading.value = true
  try {
    const res = await apiKnowledgeGraphNextRecommended()
    nextRecommended.value = res.data || []
    await nextTick()
    if (pathChartRef.value && nodes.value.length > 0) {
      initPathChart()
    }
  } catch {} finally { pathLoading.value = false }
}

async function initPathChart() {
  const nodeMap = {}
  nodes.value.forEach(n => { nodeMap[n.id] = { ...n, children: [] } })
  links.value.forEach(l => {
    if (nodeMap[l.source]) nodeMap[l.source].children.push(nodeMap[l.target])
  })
  const roots = nodes.value.filter(n => !links.value.some(l => l.target === n.id))

  await ensureEcharts()
  if (echarts && echarts.init && pathChartRef.value) {
    const chart = echarts.init(pathChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'tree',
        data: roots.map(r => ({ name: r.name, value: r.mastery, children: nodeMap[r.id]?.children || [] })),
        top: '5%', left: '10%', bottom: '5%', right: '20%',
        symbolSize: 8,
        label: { position: 'left', fontSize: 11, color: 'rgba(255,255,255,0.6)' },
        leaves: { label: { position: 'right', color: 'rgba(255,255,255,0.6)' } },
        roam: true
      }]
    })
  }
}

async function loadPuzzleTab() {
  if (puzzlePieces.value.length > 0) return // cached
  puzzleLoading.value = true
  try {
    const res = await apiKnowledgeGraphProgress()
    puzzlePieces.value = res.data || []
  } catch {} finally { puzzleLoading.value = false }
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
  background: #334155;
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
  height: 520px;
  min-height: 420px;
}
.path-chart {
  width: 100%;
  height: 360px;
  min-height: 300px;
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

/* Loading State */
.loading-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.loading-text {
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
  margin: 0;
}

.star-tabs {
  display: flex; gap: 0; margin-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.star-tab {
  padding: 10px 16px; font-size: 12px; color: rgba(255,255,255,0.4);
  background: none; border: none; border-bottom: 2px solid transparent; cursor: pointer;
  font-family: inherit;
}
.star-tab.active { color: #60d9fa; border-bottom-color: #60d9fa; font-weight: 600; }

.path-tab, .puzzle-tab { min-height: 300px; }
.path-content { padding: 0; }
.path-chart { width: 100%; height: 320px; }
.path-recommend { margin-top: 16px; }
.section-title { font-weight: 600; margin-bottom: 10px; }
.rec-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px;
  background: rgba(255,255,255,0.03); border-radius: 8px; margin-bottom: 6px;
}
.rec-name { font-weight: 600; font-size: 13px; }
.rec-reason { font-size: 11px; color: rgba(255,255,255,0.4); flex: 1; }
.rec-btn {
  padding: 4px 12px; border-radius: 6px; border: 1px solid rgba(59,130,246,0.3);
  background: rgba(59,130,246,0.1); color: #60d9fa; font-size: 11px; cursor: pointer;
  font-family: inherit;
}
.rec-btn:hover { background: rgba(59,130,246,0.2); }

.puzzle-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 10px; }
.puzzle-piece {
  padding: 16px 10px; border-radius: 10px; text-align: center; cursor: pointer;
  background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.06);
  transition: transform 0.15s;
}
.puzzle-piece:hover { transform: translateY(-2px); }
.puzzle-piece.mastery-0 { background: rgba(255,255,255,0.02); }
.puzzle-piece.mastery-1 { background: rgba(239,68,68,0.08); border-color: rgba(239,68,68,0.15); }
.puzzle-piece.mastery-2 { background: rgba(234,179,8,0.08); border-color: rgba(234,179,8,0.15); }
.puzzle-piece.mastery-3 { background: rgba(34,197,94,0.06); border-color: rgba(34,197,94,0.12); }
.puzzle-piece.mastery-4 { background: rgba(59,130,246,0.1); border-color: rgba(59,130,246,0.2); }
.piece-name { display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px; }
.piece-pct { font-size: 11px; color: rgba(255,255,255,0.4); }
</style>
