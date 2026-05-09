<template>
  <div class="knowledge-map-container">
    <div class="map-header">
      <div class="header-left">
        <span class="icon">🧠</span>
        <h3>{{ title }}</h3>
      </div>
      <div class="header-actions">
        <el-button size="small" @click="expandAll">全部展开</el-button>
        <el-button size="small" @click="collapseAll">全部折叠</el-button>
        <el-button size="small" @click="resetView">重置视图</el-button>
        <el-button size="small" type="primary" @click="exportImage">导出图片</el-button>
      </div>
    </div>
    <div ref="chartContainer" class="chart-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  title: {
    type: String,
    default: '知识图谱'
  }
})

const chartContainer = ref(null)
let chartInstance = null

// 初始化图表
onMounted(() => {
  initChart()
})

// 监听数据变化
watch(() => props.data, () => {
  updateChart()
}, { deep: true })

// 初始化 ECharts
const initChart = () => {
  if (!chartContainer.value) return

  chartInstance = echarts.init(chartContainer.value)

  // 配置图表选项
  const option = {
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove',
      formatter: (params) => {
        const data = params.data
        let html = `<div style="padding: 8px;">
          <div style="font-weight: bold; font-size: 14px; margin-bottom: 6px;">${data.name}</div>`

        if (data.description) {
          html += `<div style="color: #666; font-size: 12px; margin-bottom: 4px;">${data.description}</div>`
        }

        if (data.importance) {
          html += `<div style="font-size: 12px;">
            <span style="color: #999;">重要度：</span>
            <span style="color: #f59e0b;">${'★'.repeat(data.importance)}${'☆'.repeat(5 - data.importance)}</span>
          </div>`
        }

        if (data.pages && data.pages.length > 0) {
          html += `<div style="font-size: 12px; color: #3b82f6; margin-top: 4px;">
            📄 页码：${data.pages.join(', ')}
          </div>`
        }

        html += '</div>'
        return html
      }
    },
    series: [
      {
        type: 'tree',
        data: [transformData(props.data)],
        top: '5%',
        left: '10%',
        bottom: '5%',
        right: '20%',
        symbolSize: 12,
        orient: 'LR', // 从左到右布局
        label: {
          position: 'left',
          verticalAlign: 'middle',
          align: 'right',
          fontSize: 13,
          color: '#333',
          formatter: (params) => {
            const maxLength = 20
            const name = params.name
            return name.length > maxLength ? name.substring(0, maxLength) + '...' : name
          }
        },
        leaves: {
          label: {
            position: 'right',
            verticalAlign: 'middle',
            align: 'left'
          }
        },
        emphasis: {
          focus: 'descendant',
          itemStyle: {
            borderColor: '#3b82f6',
            borderWidth: 2
          },
          label: {
            fontSize: 14,
            fontWeight: 'bold',
            color: '#3b82f6'
          }
        },
        expandAndCollapse: true,
        animationDuration: 550,
        animationDurationUpdate: 750,
        itemStyle: {
          color: (params) => {
            const colors = {
              root: '#8b5cf6',
              chapter: '#3b82f6',
              section: '#10b981',
              concept: '#f59e0b'
            }
            return colors[params.data.type] || '#6b7280'
          },
          borderColor: '#fff',
          borderWidth: 2
        },
        lineStyle: {
          color: '#cbd5e1',
          width: 2,
          curveness: 0.5
        }
      }
    ]
  }

  chartInstance.setOption(option)

  // 响应式调整
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })

  // 点击节点事件
  chartInstance.on('click', (params) => {
    if (params.data.pages && params.data.pages.length > 0) {
      ElMessage.success(`关联页码：${params.data.pages.join(', ')}`)
    }
  })
}

// 转换数据格式（后端格式 -> ECharts格式）
const transformData = (node) => {
  if (!node) return null

  const transformed = {
    name: node.name,
    type: node.type,
    description: node.description,
    importance: node.importance,
    pages: node.pages,
    value: node.importance || 1,
    children: []
  }

  if (node.children && node.children.length > 0) {
    transformed.children = node.children.map(child => transformData(child))
  }

  return transformed
}

// 更新图表
const updateChart = () => {
  if (!chartInstance) return

  const option = chartInstance.getOption()
  option.series[0].data = [transformData(props.data)]
  chartInstance.setOption(option, true)
}

// 全部展开
const expandAll = () => {
  if (!chartInstance) return

  const option = chartInstance.getOption()
  const expandNode = (node) => {
    if (node.children) {
      node.collapsed = false
      node.children.forEach(child => expandNode(child))
    }
  }

  option.series[0].data.forEach(node => expandNode(node))
  chartInstance.setOption(option, true)
  ElMessage.success('已全部展开')
}

// 全部折叠
const collapseAll = () => {
  if (!chartInstance) return

  const option = chartInstance.getOption()
  const collapseNode = (node, level = 0) => {
    if (node.children && level > 0) {
      node.collapsed = true
      node.children.forEach(child => collapseNode(child, level + 1))
    } else if (node.children) {
      node.children.forEach(child => collapseNode(child, level + 1))
    }
  }

  option.series[0].data.forEach(node => collapseNode(node))
  chartInstance.setOption(option, true)
  ElMessage.success('已全部折叠')
}

// 重置视图
const resetView = () => {
  if (!chartInstance) return
  chartInstance.clear()
  initChart()
  ElMessage.success('视图已重置')
}

// 导出图片
const exportImage = () => {
  if (!chartInstance) return

  const url = chartInstance.getDataURL({
    type: 'png',
    pixelRatio: 2,
    backgroundColor: '#fff'
  })

  const link = document.createElement('a')
  link.download = `知识图谱_${Date.now()}.png`
  link.href = url
  link.click()

  ElMessage.success('图片已导出')
}

// 组件卸载时销毁图表
import { onBeforeUnmount } from 'vue'
onBeforeUnmount(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  window.removeEventListener('resize', () => {})
})
</script>

<style scoped>
.knowledge-map-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom: 1px solid #e5e7eb;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left .icon {
  font-size: 28px;
  line-height: 1;
}

.header-left h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.chart-container {
  flex: 1;
  min-height: 500px;
  padding: 20px;
}

/* 响应式 */
@media (max-width: 768px) {
  .map-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .chart-container {
    min-height: 400px;
    padding: 10px;
  }
}
</style>
