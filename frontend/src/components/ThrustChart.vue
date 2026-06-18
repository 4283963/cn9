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
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Download, CircleCheck, CircleClose } from '@element-plus/icons-vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chartInstance = null
const chartType = ref('line')
const isSmooth = ref(true)

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
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: {
        color: '#303133'
      },
      formatter: (params) => {
        const param = params[0]
        return `<div style="font-weight: 600; margin-bottom: 4px;">时间: ${param.axisValue} s</div>
                <div style="color: #667eea;">推力: ${param.value.toLocaleString()} kN</div>`
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

watch([() => props.data, chartType, isSmooth], () => {
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
}
</style>
