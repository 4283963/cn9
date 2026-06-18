<template>
  <div class="thrust-chart-wrapper">
    <div ref="chartRef" class="thrust-chart"></div>
    <div class="chart-controls">
      <el-space>
        <el-button-group>
          <el-button size="small" @click="chartType = 'line'" :type="chartType === 'line' ? 'primary' : 'default'">
            折线图
          </el-button>
          <el-button size="small" @click="chartType = 'bar'" :type="chartType === 'bar' ? 'primary' : 'default'">
            柱状图
          </el-button>
          <el-button size="small" @click="chartType = 'area'" :type="chartType === 'area' ? 'primary' : 'default'">
            面积图
          </el-button>
        </el-button-group>
        <el-button size="small" @click="downloadChart">
          <el-icon><Download /></el-icon>
          <span>导出图片</span>
        </el-button>
        <el-button size="small" @click="toggleSmooth">
          <el-icon><CircleCheck v-if="isSmooth" /><CircleClose v-else /></el-icon>
          <span>{{ isSmooth ? '平滑曲线' : '原始曲线' }}</span>
        </el-button>
      </el-space>
    </div>

    <div v-if="violations && violations.length > 0" class="legend-bar">
      <div class="legend-title">
        <el-icon color="#f56c6c"><Warning /></el-icon>
        <span>安全违规时段 (共 {{ violations.length }} 处)</span>
      </div>
      <div class="legend-items">
        <div class="legend-item">
          <span class="legend-color legend-critical"></span>
          <span>严重 (CRITICAL)</span>
        </div>
        <div class="legend-item">
          <span class="legend-color legend-danger"></span>
          <span>危险 (DANGER)</span>
        </div>
        <div class="legend-item">
          <span class="legend-color legend-warning"></span>
          <span>警告 (WARNING)</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { Download, CircleCheck, CircleClose, Warning } from '@element-plus/icons-vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  violations: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chartInstance = null
const chartType = ref('line')
const isSmooth = ref(true)

const violationColorMap = {
  CRITICAL: {
    area: 'rgba(255, 77, 79, 0.45)',
    border: '#ff4d4f'
  },
  DANGER: {
    area: 'rgba(255, 140, 0, 0.4)',
    border: '#fa8c16'
  },
  WARNING: {
    area: 'rgba(230, 162, 60, 0.35)',
    border: '#e6a23c'
  }
}

const defaultViolationColor = {
  area: 'rgba(245, 108, 108, 0.4)',
  border: '#f56c6c'
}

const getViolationColor = (severity) => {
  return violationColorMap[severity] || defaultViolationColor
}

const buildMarkAreaData = computed(() => {
  if (!props.violations || props.violations.length === 0) {
    return []
  }
  const result = []
  const times = props.data.map(p => p.time)

  const findClosestIndex = (targetTime) => {
    let closest = 0
    let minDiff = Infinity
    for (let i = 0; i < times.length; i++) {
      const diff = Math.abs(Number(times[i]) - Number(targetTime))
      if (diff < minDiff) {
        minDiff = diff
        closest = i
      }
    }
    return closest
  }

  props.violations.forEach(v => {
    const startIdx = findClosestIndex(v.startTime)
    const endIdx = findClosestIndex(v.endTime)
    const color = getViolationColor(v.severity)
    result.push([
      {
        name: v.description || v.violationType,
        xAxis: startIdx,
        itemStyle: {
          color: color.area,
          borderColor: color.border,
          borderWidth: 1,
          borderType: 'dashed'
        }
      },
      {
        xAxis: endIdx
      }
    ])
  })

  return result
})

const initChart = () => {
  if (chartRef.value) {
    chartInstance = echarts.init(chartRef.value, null, { renderer: 'canvas' })
    updateChart()
    window.addEventListener('resize', handleResize)
  }
}

const handleResize = () => {
  chartInstance?.resize()
}

const getOption = () => {
  const times = props.data.map(p => p.time)
  const thrusts = props.data.map(p => p.thrust)

  const series = {
    name: '推力',
    type: chartType.value === 'bar' ? 'bar' : 'line',
    data: thrusts,
    smooth: isSmooth.value && chartType.value !== 'bar',
    showSymbol: false,
    sampling: 'lttb',
    markArea: {
      silent: false,
      label: {
        show: true,
        position: 'top',
        formatter: () => '',
        fontSize: 10,
        color: '#303133'
      },
      data: buildMarkAreaData.value
    },
    lineStyle: {
      width: 2,
      color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
        { offset: 0, color: '#667eea' },
        { offset: 1, color: '#764ba2' }
      ])
    },
    itemStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: '#667eea' },
        { offset: 1, color: '#764ba2' }
      ])
    }
  }

  if (chartType.value === 'area') {
    series.areaStyle = {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(102, 126, 234, 0.5)' },
        { offset: 1, color: 'rgba(118, 75, 162, 0.1)' }
      ])
    }
  }

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: {
        color: '#303133'
      },
      formatter: (params) => {
        let html = ''
        if (Array.isArray(params)) {
          const mainParam = params.find(p => p.seriesName === '推力') || params[0]
          if (mainParam) {
            const timeVal = mainParam.axisValue
            const thrustVal = Number(mainParam.value).toLocaleString()
            html += `<div style="font-weight: 600; margin-bottom: 4px;">时间: ${timeVal} s</div>`
            html += `<div style="color: #667eea;">推力: ${thrustVal} kN</div>`
            const idx = mainParam.dataIndex
            if (props.data[idx]) {
              const temp = props.data[idx].temperature
              if (temp != null) {
                html += `<div style="color: #e6a23c; margin-top: 2px;">温度: ${Number(temp).toLocaleString()} K</div>`
              }
            }
          }
          const markAreaParams = params.filter(p => p.componentType === 'markArea')
          if (markAreaParams.length > 0) {
            markAreaParams.forEach(p => {
              if (p.name) {
                html += `<div style="color: #f56c6c; margin-top: 6px; border-top: 1px dashed #f56c6c; padding-top: 4px;">⚠ ${p.name}</div>`
              }
            })
          }
        }
        return html
      }
    },
    grid: {
      left: '3%',
      right: '3%',
      top: '10%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      name: '时间 (s)',
      nameLocation: 'middle',
      nameGap: 30,
      nameTextStyle: {
        fontSize: 13,
        fontWeight: 500,
        color: '#606266'
      },
      data: times,
      axisLabel: {
        rotate: 0,
        formatter: (value) => Number(value).toFixed(1),
        color: '#606266'
      },
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: '#f0f2f5',
          type: 'dashed'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '推力 (kN)',
      nameLocation: 'middle',
      nameGap: 45,
      nameTextStyle: {
        fontSize: 13,
        fontWeight: 500,
        color: '#606266'
      },
      axisLabel: {
        formatter: (value) => value.toLocaleString(),
        color: '#606266'
      },
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#f0f2f5',
          type: 'dashed'
        }
      },
      scale: true
    },
    dataZoom: [
      {
        type: 'inside',
        start: 0,
        end: 100,
        zoomOnMouseWheel: true,
        moveOnMouseMove: true
      },
      {
        type: 'slider',
        start: 0,
        end: 100,
        height: 20,
        bottom: 5,
        borderColor: '#e4e7ed',
        fillerColor: 'rgba(102, 126, 234, 0.2)',
        handleStyle: {
          color: '#667eea'
        },
        textStyle: {
          color: '#606266'
        }
      }
    ],
    series: [series]
  }
}

const updateChart = () => {
  if (chartInstance && props.data?.length > 0) {
    chartInstance.setOption(getOption(), true)
  }
}

watch([() => props.data, () => props.violations, chartType, isSmooth], () => {
  nextTick(updateChart)
}, { deep: true })

const downloadChart = () => {
  if (chartInstance) {
    const url = chartInstance.getDataURL({
      type: 'png',
      pixelRatio: 2,
      backgroundColor: '#fff'
    })
    const link = document.createElement('a')
    link.download = `thrust-curve-${Date.now()}.png`
    link.href = url
    link.click()
  }
}

const toggleSmooth = () => {
  isSmooth.value = !isSmooth.value
}

onMounted(() => {
  nextTick(initChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style lang="scss" scoped>
.thrust-chart-wrapper {
  width: 100%;

  .thrust-chart {
    width: 100%;
    height: 400px;
    background: #fafafa;
    border-radius: 8px;
    border: 1px solid #ebeef5;
  }

  .chart-controls {
    margin-top: 16px;
    display: flex;
    justify-content: center;
  }

  .legend-bar {
    margin-top: 16px;
    padding: 12px 16px;
    background: linear-gradient(135deg, #fff1f0 0%, #fff7e6 100%);
    border-radius: 8px;
    border: 1px solid #fbc4c4;
  }

  .legend-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    color: #cf1322;
    margin-bottom: 10px;
  }

  .legend-items {
    display: flex;
    gap: 24px;
    flex-wrap: wrap;
  }

  .legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #595959;
  }

  .legend-color {
    display: inline-block;
    width: 24px;
    height: 16px;
    border-radius: 3px;
  }

  .legend-critical {
    background: rgba(255, 77, 79, 0.45);
    border: 1px solid #ff4d4f;
  }

  .legend-danger {
    background: rgba(255, 140, 0, 0.4);
    border: 1px solid #fa8c16;
  }

  .legend-warning {
    background: rgba(230, 162, 60, 0.35);
    border: 1px solid #e6a23c;
  }
}
</style>
