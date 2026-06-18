<template>
  <div class="simulation-page">
    <el-row :gutter="24">
      <el-col :span="8">
        <div class="page-container">
          <h2 class="section-title">
            <el-icon><Setting /></el-icon>
            <span>仿真参数配置</span>
          </h2>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="120px"
            label-position="left"
          >
            <el-form-item label="配方名称" prop="name">
              <el-input
                v-model="form.name"
                placeholder="请输入配方名称"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="配方描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="2"
                placeholder="请输入配方描述（可选）"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>

            <div class="form-section">
              <h3 class="section-title">
                <el-icon><Coin /></el-icon>
                <span>燃料配比</span>
              </h3>
              <el-form-item label="液氧 (LOX)" prop="loxRatio">
                <el-input-number
                  v-model="form.loxRatio"
                  :min="0.1"
                  :max="10.0"
                  :step="0.01"
                  :precision="2"
                  style="width: 100%"
                  controls-position="right"
                />
                <span class="unit">质量比</span>
              </el-form-item>
              <el-form-item label="煤油 (RP-1)" prop="keroseneRatio">
                <el-input-number
                  v-model="form.keroseneRatio"
                  :min="0.1"
                  :max="10.0"
                  :step="0.01"
                  :precision="2"
                  style="width: 100%"
                  controls-position="right"
                />
                <span class="unit">质量比</span>
              </el-form-item>
              <el-alert
                :title="`氧燃比 (O/F): ${(form.loxRatio / form.keroseneRatio).toFixed(3)}`"
                type="info"
                :closable="false"
                show-icon
                size="small"
              />
            </div>

            <div class="form-section">
              <h3 class="section-title">
                <el-icon><Odometer /></el-icon>
                <span>燃烧室参数</span>
              </h3>
              <el-form-item label="燃烧室压力" prop="chamberPressure">
                <el-input-number
                  v-model="form.chamberPressure"
                  :min="1.0"
                  :max="50.0"
                  :step="0.1"
                  :precision="1"
                  style="width: 100%"
                  controls-position="right"
                />
                <span class="unit">MPa</span>
              </el-form-item>
              <el-form-item label="初始温度" prop="initialTemperature">
                <el-input-number
                  v-model="form.initialTemperature"
                  :min="200.0"
                  :max="800.0"
                  :step="1.0"
                  :precision="1"
                  style="width: 100%"
                  controls-position="right"
                />
                <span class="unit">K</span>
              </el-form-item>
            </div>

            <div class="form-section">
              <h3 class="section-title">
                <el-icon><Timer /></el-icon>
                <span>燃烧与喷管</span>
              </h3>
              <el-form-item label="燃烧时长" prop="burnDuration">
                <el-input-number
                  v-model="form.burnDuration"
                  :min="1.0"
                  :max="600.0"
                  :step="1.0"
                  :precision="1"
                  style="width: 100%"
                  controls-position="right"
                />
                <span class="unit">秒</span>
              </el-form-item>
              <el-form-item label="喷管面积比" prop="nozzleAreaRatio">
                <el-input-number
                  v-model="form.nozzleAreaRatio"
                  :min="1.0"
                  :max="100.0"
                  :step="0.1"
                  :precision="1"
                  style="width: 100%"
                  controls-position="right"
                />
                <span class="unit">ε = Ae/At</span>
              </el-form-item>
            </div>

            <el-form-item>
              <el-space style="width: 100%" justify="center">
                <el-button
                  type="primary"
                  size="large"
                  :loading="store.simulating"
                  @click="handleRunSimulation"
                >
                  <el-icon><VideoPlay /></el-icon>
                  <span>开始仿真并保存</span>
                </el-button>
                <el-button
                  size="large"
                  :loading="store.simulating"
                  @click="handlePreview"
                >
                  <el-icon><View /></el-icon>
                  <span>仅预览结果</span>
                </el-button>
                <el-button size="large" @click="handleReset">
                  <el-icon><Refresh /></el-icon>
                  <span>重置参数</span>
                </el-button>
              </el-space>
            </el-form-item>
          </el-form>
        </div>
      </el-col>

      <el-col :span="16">
        <div class="page-container" v-if="store.currentResult">
          <h2 class="section-title">
            <el-icon><DataLine /></el-icon>
            <span>仿真结果 - {{ store.currentResult.recipeName || '预览模式' }}</span>
            <el-tag
              v-if="store.currentResult.safetyCompliant === true"
              type="success"
              effect="light"
              size="default"
              style="margin-left: 12px;"
            >
              <el-icon style="margin-right: 4px;"><CircleCheck /></el-icon>
              安全合规
            </el-tag>
            <el-tag
              v-else-if="store.currentResult.safetyCompliant === false"
              type="danger"
              effect="dark"
              size="default"
              style="margin-left: 12px;"
            >
              <el-icon style="margin-right: 4px;"><WarningFilled /></el-icon>
              发现安全风险
            </el-tag>
          </h2>

          <el-alert
            v-if="store.currentResult.safetyCompliant === false && store.currentResult.safetySummary"
            :title="safetyAlertTitle"
            :description="store.currentResult.safetySummary"
            type="error"
            :closable="false"
            show-icon
            style="margin-bottom: 20px;"
          >
            <template #icon>
              <el-icon :size="20"><WarningFilled /></el-icon>
            </template>
          </el-alert>

          <el-alert
            v-else-if="store.currentResult.safetyCompliant === true"
            title="安全评估：通过"
            description="所有参数均在行业标准安全红线范围内，可以进行点火试验。"
            type="success"
            :closable="false"
            show-icon
            style="margin-bottom: 20px;"
          >
            <template #icon>
              <el-icon :size="20"><CircleCheckFilled /></el-icon>
            </template>
          </el-alert>

          <el-row :gutter="16" style="margin-bottom: 20px">
            <el-col :span="6">
              <div class="result-card">
                <div class="result-label">最大推力</div>
                <div class="result-value">
                  {{ store.currentResult.maxThrust?.toLocaleString() }}
                  <span class="result-unit">kN</span>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="result-card" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
                <div class="result-label">平均推力</div>
                <div class="result-value">
                  {{ store.currentResult.averageThrust?.toLocaleString() }}
                  <span class="result-unit">kN</span>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="result-card" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
                <div class="result-label">排气速度</div>
                <div class="result-value">
                  {{ store.currentResult.exhaustVelocity?.toLocaleString() }}
                  <span class="result-unit">m/s</span>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="result-card" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
                <div class="result-label">比冲 Isp</div>
                <div class="result-value">
                  {{ store.currentResult.specificImpulse?.toLocaleString() }}
                  <span class="result-unit">s</span>
                </div>
              </div>
            </el-col>
          </el-row>

          <ThrustChart
            :data="store.currentResult.thrustCurve"
            :violations="store.currentResult.safetyViolations || []"
          />

          <el-collapse v-if="store.currentResult.safetyViolations?.length > 0" style="margin-top: 24px;">
            <el-collapse-item title="安全违规详细清单" name="violations">
              <el-table
                :data="store.currentResult.safetyViolations"
                stripe
                border
                size="small"
                style="width: 100%"
              >
                <el-table-column label="严重程度" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag
                      :type="severityTagType(row.severity)"
                      effect="dark"
                      size="small"
                    >
                      {{ severityLabel(row.severity) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="违规类型" width="170">
                  <template #default="{ row }">
                    {{ violationTypeLabel(row.violationType) }}
                  </template>
                </el-table-column>
                <el-table-column prop="startTime" label="开始时间 (s)" width="120" align="right" />
                <el-table-column prop="endTime" label="结束时间 (s)" width="120" align="right" />
                <el-table-column prop="duration" label="持续时间 (s)" width="120" align="right" />
                <el-table-column label="峰值" width="140" align="right">
                  <template #default="{ row }">
                    <span :style="{ color: '#f56c6c', fontWeight: 600 }">
                      {{ row.peakValue?.toLocaleString() }}
                    </span>
                    <span style="color: #909399; margin-left: 2px;">
                      {{ violationUnit(row.violationType) }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="红线阈值" width="140" align="right">
                  <template #default="{ row }">
                    <span style="color: #909399;">
                      {{ row.threshold?.toLocaleString() }}
                    </span>
                    <span style="color: #909399; margin-left: 2px;">
                      {{ violationUnit(row.violationType) }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="描述说明" min-width="260" />
              </el-table>
            </el-collapse-item>
          </el-collapse>

          <el-divider />

          <h3 class="section-title">
            <el-icon><Histogram /></el-icon>
            <span>详细参数</span>
          </h3>

          <el-row :gutter="16">
            <el-col :span="8">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="总冲量">
                  {{ store.currentResult.totalImpulse?.toLocaleString() }} kN·s
                </el-descriptions-item>
                <el-descriptions-item label="质量流率">
                  {{ store.currentResult.massFlowRate?.toLocaleString() }} kg/s
                </el-descriptions-item>
                <el-descriptions-item label="特征速度 C*">
                  {{ store.currentResult.characteristicVelocity?.toLocaleString() }} m/s
                </el-descriptions-item>
              </el-descriptions>
            </el-col>
            <el-col :span="8">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="推力系数 Cf">
                  {{ store.currentResult.thrustCoefficient?.toLocaleString() }}
                </el-descriptions-item>
                <el-descriptions-item label="出口马赫数">
                  {{ store.currentResult.exitMachNumber?.toLocaleString() }}
                </el-descriptions-item>
                <el-descriptions-item label="出口温度">
                  {{ store.currentResult.exitTemperature?.toLocaleString() }} K
                </el-descriptions-item>
              </el-descriptions>
            </el-col>
            <el-col :span="8">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="出口压力">
                  {{ store.currentResult.exitPressure?.toLocaleString() }} MPa
                </el-descriptions-item>
                <el-descriptions-item label="配方ID">
                  {{ store.currentResult.recipeId || '预览模式' }}
                </el-descriptions-item>
                <el-descriptions-item label="配方名称">
                  {{ store.currentResult.recipeName || '未保存' }}
                </el-descriptions-item>
              </el-descriptions>
            </el-col>
          </el-row>
        </div>

        <div class="page-container empty-state" v-else>
          <el-empty description="配置参数后点击'开始仿真'查看结果">
            <el-icon :size="80" color="#667eea"><Van /></el-icon>
          </el-empty>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useSimulationStore } from '@/stores/simulation'
import ThrustChart from '@/components/ThrustChart.vue'
import {
  Setting, Coin, Odometer, Timer, VideoPlay, View,
  Refresh, DataLine, Histogram, Van,
  Warning, WarningFilled, CircleCheck, CircleCheckFilled
} from '@element-plus/icons-vue'

const store = useSimulationStore()
const formRef = ref(null)

const form = reactive({
  name: '',
  description: '',
  loxRatio: 2.56,
  keroseneRatio: 1.0,
  chamberPressure: 10.0,
  initialTemperature: 300.0,
  burnDuration: 120.0,
  nozzleAreaRatio: 16.0
})

const rules = {
  name: [
    { required: true, message: '请输入配方名称', trigger: 'blur' },
    { min: 1, max: 100, message: '名称长度在1到100个字符', trigger: 'blur' }
  ],
  description: [
    { max: 500, message: '描述不能超过500个字符', trigger: 'blur' }
  ],
  loxRatio: [
    { required: true, message: '请输入液氧比例', trigger: 'blur' },
    { type: 'number', min: 0.1, max: 10.0, message: '范围: 0.1 - 10.0', trigger: 'blur' }
  ],
  keroseneRatio: [
    { required: true, message: '请输入煤油比例', trigger: 'blur' },
    { type: 'number', min: 0.1, max: 10.0, message: '范围: 0.1 - 10.0', trigger: 'blur' }
  ],
  chamberPressure: [
    { required: true, message: '请输入燃烧室压力', trigger: 'blur' },
    { type: 'number', min: 1.0, max: 50.0, message: '范围: 1 - 50 MPa', trigger: 'blur' }
  ],
  initialTemperature: [
    { required: true, message: '请输入初始温度', trigger: 'blur' },
    { type: 'number', min: 200.0, max: 800.0, message: '范围: 200 - 800 K', trigger: 'blur' }
  ],
  burnDuration: [
    { required: true, message: '请输入燃烧时长', trigger: 'blur' },
    { type: 'number', min: 1.0, max: 600.0, message: '范围: 1 - 600 秒', trigger: 'blur' }
  ],
  nozzleAreaRatio: [
    { required: true, message: '请输入喷管面积比', trigger: 'blur' },
    { type: 'number', min: 1.0, max: 100.0, message: '范围: 1 - 100', trigger: 'blur' }
  ]
}

const safetyAlertTitle = computed(() => {
  const vs = store.currentResult?.safetyViolations || []
  const critical = vs.filter(v => v.severity === 'CRITICAL').length
  const danger = vs.filter(v => v.severity === 'DANGER').length
  const warning = vs.filter(v => v.severity === 'WARNING').length
  const parts = []
  if (critical > 0) parts.push(`${critical} 项严重违规`)
  if (danger > 0) parts.push(`${danger} 项危险`)
  if (warning > 0) parts.push(`${warning} 项警告`)
  return `检测到安全风险：${parts.join('，')}！`
})

function severityTagType(severity) {
  switch (severity) {
    case 'CRITICAL': return 'danger'
    case 'DANGER': return 'warning'
    case 'WARNING': return 'info'
    default: return 'info'
  }
}

function severityLabel(severity) {
  switch (severity) {
    case 'CRITICAL': return '严重'
    case 'DANGER': return '危险'
    case 'WARNING': return '警告'
    default: return severity || '未知'
  }
}

function violationTypeLabel(type) {
  switch (type) {
    case 'TEMPERATURE_CONTINUOUS': return '燃烧室温度持续超标'
    case 'TEMPERATURE_PEAK': return '燃烧室温度峰值超标'
    case 'THRUST_FLUCTUATION': return '推力波动过大'
    default: return type || '未知违规'
  }
}

function violationUnit(type) {
  switch (type) {
    case 'TEMPERATURE_CONTINUOUS':
    case 'TEMPERATURE_PEAK':
      return 'K'
    case 'THRUST_FLUCTUATION':
      return 'kN'
    default:
      return ''
  }
}

async function handleRunSimulation() {
  if (!form.value.name?.trim()) {
    ElMessage.warning('请输入配方名称')
    return
  }
  try {
    await formRef.value.validate()
    await store.runSimulation({ ...form })
    ElMessage.success('仿真计算完成，配方已保存')
  } catch (e) {
    console.error(e)
  }
}

async function handlePreview() {
  try {
    await formRef.value.validate()
    await store.previewSimulation({ ...form })
    ElMessage.success('仿真计算完成（预览模式）')
  } catch (e) {
    console.error(e)
  }
}

function handleReset() {
  form.name = ''
  form.description = ''
  form.loxRatio = 2.56
  form.keroseneRatio = 1.0
  form.chamberPressure = 10.0
  form.initialTemperature = 300.0
  form.burnDuration = 120.0
  form.nozzleAreaRatio = 16.0
  store.clearResult()
  formRef.value?.resetFields()
}

onMounted(() => {
  store.fetchRecipes()
})
</script>

<style lang="scss" scoped>
.simulation-page {
  width: 100%;
}

.unit {
  display: inline-block;
  margin-left: 8px;
  color: #909399;
  font-size: 13px;
}

.empty-state {
  min-height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;

  .el-empty {
    flex: 1;
  }
}
</style>
