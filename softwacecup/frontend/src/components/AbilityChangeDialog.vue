<template>
  <el-dialog
    v-model="visible"
    title="🎉 能力提升报告"
    width="700px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="ability-dialog-content">
      <!-- 总体评价 -->
      <div class="overall-section">
        <div class="congratulations">
          <span class="icon">🎊</span>
          <h3>恭喜！您的能力有所提升</h3>
          <p>本次测验表现优异，多个维度能力得到增强</p>
        </div>
      </div>

      <!-- 能力变化详情 -->
      <div class="changes-section">
        <h4>📊 能力维度变化</h4>
        <div class="changes-list">
          <div
            v-for="(change, dimension) in abilityChanges"
            :key="dimension"
            class="change-item"
          >
            <div class="change-header">
              <span class="dimension-name">{{ change.dimension }}</span>
              <span
                class="change-badge"
                :class="{ positive: change.delta > 0, negative: change.delta < 0 }"
              >
                {{ change.delta > 0 ? '+' : '' }}{{ change.delta }}
              </span>
            </div>
            <div class="change-progress">
              <div class="progress-bar">
                <div class="progress-before" :style="{ width: change.before + '%' }">
                  <span class="progress-label">{{ change.before }}</span>
                </div>
                <div
                  class="progress-after"
                  :style="{ width: change.after + '%' }"
                  :class="{ grow: change.delta > 0 }"
                >
                  <span class="progress-label">{{ change.after }}</span>
                </div>
              </div>
            </div>
            <div class="change-reason">
              <span class="reason-icon">💡</span>
              <span class="reason-text">{{ change.reason }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 雷达图对比 -->
      <div class="radar-section">
        <h4>📈 能力雷达图对比</h4>
        <div ref="radarChart" class="radar-chart"></div>
      </div>

      <!-- 鼓励语 -->
      <div class="encouragement-section">
        <p class="encouragement-text">
          {{ encouragementText }}
        </p>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">稍后查看</el-button>
      <el-button type="primary" @click="viewFullReport">查看完整报告</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  abilityChanges: {
    type: Object,
    default: () => ({})
  },
  beforeData: {
    type: Object,
    default: () => ({})
  },
  afterData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'viewReport'])

const visible = ref(props.modelValue)
const radarChart = ref(null)
let chartInstance = null

// 鼓励语
const encouragementText = ref('继续保持这样的学习状态，你一定能够取得更大的进步！')

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    nextTick(() => {
      initRadarChart()
      generateEncouragement()
    })
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 初始化雷达图
const initRadarChart = () => {
  if (!radarChart.value) return

  chartInstance = echarts.init(radarChart.value)

  // 构建雷达图数据
  const dimensions = Object.keys(props.abilityChanges)
  const indicator = dimensions.map(dim => ({
    name: props.abilityChanges[dim].dimension,
    max: 100
  }))

  const beforeValues = dimensions.map(dim => props.abilityChanges[dim].before)
  const afterValues = dimensions.map(dim => props.abilityChanges[dim].after)

  const option = {
    radar: {
      indicator: indicator,
      shape: 'polygon',
      splitNumber: 5,
      axisName: {
        color: '#666',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: '#e5e7eb'
        }
      },
      splitArea: {
        areaStyle: {
          color: ['#f9fafb', '#f3f4f6']
        }
      }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: beforeValues,
            name: '提升前',
            lineStyle: {
              color: '#94a3b8',
              width: 2
            },
            areaStyle: {
              color: 'rgba(148, 163, 184, 0.2)'
            },
            symbol: 'circle',
            symbolSize: 6
          },
          {
            value: afterValues,
            name: '提升后',
            lineStyle: {
              color: '#3b82f6',
              width: 3
            },
            areaStyle: {
              color: 'rgba(59, 130, 246, 0.3)'
            },
            symbol: 'circle',
            symbolSize: 8
          }
        ]
      }
    ],
    legend: {
      bottom: 10,
      data: ['提升前', '提升后']
    }
  }

  chartInstance.setOption(option)

  // 响应式调整
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
}

// 生成鼓励语
const generateEncouragement = () => {
  const totalDelta = Object.values(props.abilityChanges).reduce(
    (sum, change) => sum + change.delta,
    0
  )

  if (totalDelta >= 50) {
    encouragementText.value = '太棒了！您的能力有了显著提升，继续保持这样的学习节奏！'
  } else if (totalDelta >= 20) {
    encouragementText.value = '很好！您的学习效果明显，继续努力，相信你会越来越优秀！'
  } else if (totalDelta > 0) {
    encouragementText.value = '不错！虽然进步不大，但每一步都很重要，坚持下去！'
  } else {
    encouragementText.value = '没关系，学习是一个循序渐进的过程，继续加油！'
  }
}

// 关闭弹窗
const handleClose = () => {
  visible.value = false
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

// 查看完整报告
const viewFullReport = () => {
  emit('viewReport')
  handleClose()
}
</script>

<style scoped>
.ability-dialog-content {
  padding: 10px;
}

.overall-section {
  margin-bottom: 24px;
}

.congratulations {
  text-align: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
}

.congratulations .icon {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.congratulations h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
}

.congratulations p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.changes-section {
  margin-bottom: 24px;
}

.changes-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.changes-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.change-item {
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.change-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.dimension-name {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.change-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
}

.change-badge.positive {
  background: #dcfce7;
  color: #16a34a;
}

.change-badge.negative {
  background: #fee2e2;
  color: #dc2626;
}

.change-progress {
  margin-bottom: 12px;
}

.progress-bar {
  position: relative;
  height: 32px;
  background: #e5e7eb;
  border-radius: 16px;
  overflow: hidden;
}

.progress-before,
.progress-after {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 12px;
  transition: all 0.6s ease;
}

.progress-before {
  background: #94a3b8;
  opacity: 0.5;
}

.progress-after {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
}

.progress-after.grow {
  animation: growProgress 1s ease-out;
}

@keyframes growProgress {
  from {
    width: 0;
  }
}

.progress-label {
  color: white;
  font-size: 12px;
  font-weight: 600;
}

.change-reason {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.reason-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.reason-text {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

.radar-section {
  margin-bottom: 24px;
}

.radar-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.radar-chart {
  width: 100%;
  height: 300px;
}

.encouragement-section {
  padding: 16px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: 8px;
  text-align: center;
}

.encouragement-text {
  margin: 0;
  font-size: 14px;
  color: #92400e;
  line-height: 1.6;
}
</style>
