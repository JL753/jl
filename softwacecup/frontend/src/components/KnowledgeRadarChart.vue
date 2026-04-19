<template>
  <div class="knowledge-radar-chart">
    <div class="chart-header">
      <h4>{{ title }}</h4>
      <div class="chart-actions">
        <el-button size="small" @click="refreshData">刷新</el-button>
        <el-button size="small" @click="exportData">导出</el-button>
      </div>
    </div>
    <div class="chart-container" ref="chartRef"></div>
    <div class="chart-legend">
      <div v-for="(item, index) in legendData" :key="index" class="legend-item">
        <span class="legend-color" :style="{ background: item.color }"></span>
        <span class="legend-label">{{ item.name }}</span>
        <span class="legend-value">{{ item.value }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

const props = defineProps({
  title: {
    type: String,
    default: '知识掌握雷达图'
  },
  data: {
    type: Object,
    default: () => ({
      dimensions: ['基础概念', '算法理解', '编程实践', '理论推导', '应用场景', '综合能力'],
      current: [85, 72, 90, 65, 78, 80],
      target: [90, 85, 95, 80, 85, 90],
      average: [75, 70, 75, 60, 70, 72]
    })
  },
  height: {
    type: String,
    default: '400px'
  }
})

const emit = defineEmits(['refresh', 'export'])

const chartRef = ref(null)
let chartInstance = null

const legendData = ref([])

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#4c8dff',
      borderWidth: 1,
      textStyle: {
        color: '#fff',
        fontSize: 13
      },
      formatter: (params) => {
        const { name, value, seriesName } = params
        return `
          <div style="padding: 8px;">
            <div style="font-weight: 600; margin-bottom: 6px;">${name}</div>
            <div style="color: #4c8dff;">${seriesName}: ${value}%</div>
          </div>
        `
      }
    },
    legend: {
      show: false
    },
    radar: {
      indicator: props.data.dimensions.map(name => ({
        name,
        max: 100,
        axisLabel: {
          show: true,
          fontSize: 11,
          color: '#666',
          showMaxLabel: false,
          showMinLabel: false
        }
      })),
      shape: 'polygon',
      center: ['50%', '50%'],
      radius: '65%',
      splitNumber: 5,
      name: {
        textStyle: {
          color: '#2c3e50',
          fontSize: 13,
          fontWeight: 600
        }
      },
      splitLine: {
        lineStyle: {
          color: '#e2e8f0',
          width: 1
        }
      },
      splitArea: {
        show: true,
        areaStyle: {
          color: ['rgba(76, 141, 255, 0.05)', 'rgba(76, 141, 255, 0.1)']
        }
      },
      axisLine: {
        lineStyle: {
          color: '#cbd5e0'
        }
      }
    },
    series: [
      {
        name: '当前水平',
        type: 'radar',
        symbol: 'circle',
        symbolSize: 6,
        data: [
          {
            value: props.data.current,
            name: '当前水平',
            areaStyle: {
              color: 'rgba(76, 141, 255, 0.3)'
            },
            lineStyle: {
              color: '#4c8dff',
              width: 2
            },
            itemStyle: {
              color: '#4c8dff',
              borderColor: '#fff',
              borderWidth: 2
            }
          }
        ]
      },
      {
        name: '目标水平',
        type: 'radar',
        symbol: 'circle',
        symbolSize: 6,
        data: [
          {
            value: props.data.target,
            name: '目标水平',
            areaStyle: {
              color: 'rgba(16, 185, 129, 0.2)'
            },
            lineStyle: {
              color: '#10b981',
              width: 2,
              type: 'dashed'
            },
            itemStyle: {
              color: '#10b981',
              borderColor: '#fff',
              borderWidth: 2
            }
          }
        ]
      },
      {
        name: '班级平均',
        type: 'radar',
        symbol: 'circle',
        symbolSize: 5,
        data: [
          {
            value: props.data.average,
            name: '班级平均',
            areaStyle: {
              color: 'rgba(251, 146, 60, 0.15)'
            },
            lineStyle: {
              color: '#fb923c',
              width: 2,
              type: 'dotted'
            },
            itemStyle: {
              color: '#fb923c',
              borderColor: '#fff',
              borderWidth: 2
            }
          }
        ]
      }
    ],
    animation: true,
    animationDuration: 1000,
    animationEasing: 'cubicOut'
  }

  chartInstance.setOption(option)

  // 更新图例数据
  updateLegendData()
}

// 更新图例数据
const updateLegendData = () => {
  const avgCurrent = props.data.current.reduce((a, b) => a + b, 0) / props.data.current.length
  const avgTarget = props.data.target.reduce((a, b) => a + b, 0) / props.data.target.length
  const avgAverage = props.data.average.reduce((a, b) => a + b, 0) / props.data.average.length

  legendData.value = [
    { name: '当前水平', color: '#4c8dff', value: avgCurrent.toFixed(1) },
    { name: '目标水平', color: '#10b981', value: avgTarget.toFixed(1) },
    { name: '班级平均', color: '#fb923c', value: avgAverage.toFixed(1) }
  ]
}

// 刷新数据
const refreshData = () => {
  emit('refresh')
  ElMessage.success('数据已刷新')
}

// 导出数据
const exportData = () => {
  emit('export')
  ElMessage.success('数据导出成功')
}

// 响应式调整
const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

// 监听数据变化
watch(() => props.data, () => {
  if (chartInstance) {
    initChart()
  }
}, { deep: true })

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.knowledge-radar-chart {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f1f5f9;
}

.chart-header h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a202c;
}

.chart-actions {
  display: flex;
  gap: 8px;
}

.chart-container {
  width: 100%;
  height: v-bind(height);
  min-height: 300px;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  flex-shrink: 0;
}

.legend-label {
  color: #4a5568;
  font-weight: 500;
}

.legend-value {
  color: #1a202c;
  font-weight: 600;
  font-size: 14px;
}

@media (max-width: 768px) {
  .chart-legend {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}
</style>
