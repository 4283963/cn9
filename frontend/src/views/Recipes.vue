<template>
  <div class="recipes-page">
    <div class="page-container">
      <div class="page-header">
        <h2 class="section-title">
          <el-icon><Document /></el-icon>
          <span>仿真配方管理</span>
        </h2>
        <div class="header-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索配方名称..."
            style="width: 250px"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            <span>刷新</span>
          </el-button>
        </div>
      </div>

      <el-table
        :data="store.recipes"
        v-loading="store.loading"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="配方名称" min-width="150">
          <template #default="{ row }">
            <el-tag type="primary" size="small">{{ row.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="燃料配比 (O/F)" width="140" align="center">
          <template #default="{ row }">
            <span class="of-ratio">
              {{ (row.loxRatio / row.keroseneRatio).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="chamberPressure" label="燃烧室压力" width="120" align="center">
          <template #default="{ row }">
            {{ row.chamberPressure }} MPa
          </template>
        </el-table-column>
        <el-table-column prop="initialTemperature" label="初始温度" width="100" align="center">
          <template #default="{ row }">
            {{ row.initialTemperature }} K
          </template>
        </el-table-column>
        <el-table-column prop="burnDuration" label="燃烧时长" width="100" align="center">
          <template #default="{ row }">
            {{ row.burnDuration }} s
          </template>
        </el-table-column>
        <el-table-column prop="nozzleAreaRatio" label="面积比" width="80" align="center">
          <template #default="{ row }">
            {{ row.nozzleAreaRatio }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleRunSimulation(row)">
              <el-icon><VideoPlay /></el-icon>
              <span>运行</span>
            </el-button>
            <el-button type="success" link size="small" @click="handleLoadToSimulator(row)">
              <el-icon><Edit /></el-icon>
              <span>加载</span>
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              <span>删除</span>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="store.recipes.length > 0"
        style="margin-top: 20px; justify-content: center"
        layout="total, sizes, prev, pager, next, jumper"
        :total="store.recipes.length"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        v-model:current-page="currentPage"
      />
    </div>

    <el-dialog
      v-model="detailDialogVisible"
      title="配方详情"
      width="600px"
    >
      <el-descriptions :column="2" border v-if="selectedRecipe">
        <el-descriptions-item label="配方名称">
          {{ selectedRecipe.name }}
        </el-descriptions-item>
        <el-descriptions-item label="氧燃比">
          {{ (selectedRecipe.loxRatio / selectedRecipe.keroseneRatio).toFixed(3) }}
        </el-descriptions-item>
        <el-descriptions-item label="液氧比例">
          {{ selectedRecipe.loxRatio }}
        </el-descriptions-item>
        <el-descriptions-item label="煤油比例">
          {{ selectedRecipe.keroseneRatio }}
        </el-descriptions-item>
        <el-descriptions-item label="燃烧室压力">
          {{ selectedRecipe.chamberPressure }} MPa
        </el-descriptions-item>
        <el-descriptions-item label="初始温度">
          {{ selectedRecipe.initialTemperature }} K
        </el-descriptions-item>
        <el-descriptions-item label="燃烧时长">
          {{ selectedRecipe.burnDuration }} s
        </el-descriptions-item>
        <el-descriptions-item label="喷管面积比">
          {{ selectedRecipe.nozzleAreaRatio }}
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          {{ selectedRecipe.description || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">
          {{ formatDate(selectedRecipe.createdAt) }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleRunSimulation(selectedRecipe)">运行仿真</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSimulationStore } from '@/stores/simulation'
import {
  Document, Search, Refresh, VideoPlay, Edit, Delete
} from '@element-plus/icons-vue'

const router = useRouter()
const store = useSimulationStore()

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const detailDialogVisible = ref(false)
const selectedRecipe = ref(null)

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const handleSearch = () => {
  store.fetchRecipes(searchKeyword.value.trim())
}

const handleRefresh = () => {
  searchKeyword.value = ''
  store.fetchRecipes()
}

const handleRunSimulation = async (recipe) => {
  try {
    await store.runSimulationById(recipe.id)
    ElMessage.success(`配方 [${recipe.name}] 仿真计算完成`)
    router.push({ name: 'simulation' })
  } catch (e) {
    console.error(e)
  }
}

const handleLoadToSimulator = (recipe) => {
  ElMessage.success(`配方 [${recipe.name}] 已加载到仿真器，请在仿真页面查看`)
  router.push({ name: 'simulation' })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除配方 [${row.name}] 吗？此操作不可恢复。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    await store.deleteRecipe(row.id)
    ElMessage.success('配方已删除')
  }).catch(() => {})
}

onMounted(() => {
  store.fetchRecipes()
})
</script>

<style lang="scss" scoped>
.recipes-page {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.of-ratio {
  font-weight: 600;
  color: #409eff;
  background: #ecf5ff;
  padding: 4px 12px;
  border-radius: 4px;
}
</style>
