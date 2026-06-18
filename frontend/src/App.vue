<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="header-content">
        <div class="logo">
          <el-icon :size="32" color="#409eff">
            <Van />
          </el-icon>
          <h1>火箭燃料推进系统参数仿真平台</h1>
        </div>
        <div class="header-nav">
          <el-button 
            type="primary" 
            @click="activeTab = 'simulation'"
            :type="activeTab === 'simulation' ? 'primary' : 'default'"
          >
            <el-icon><Operation /></el-icon>
            <span>仿真计算</span>
          </el-button>
          <el-button 
            @click="activeTab = 'recipes'"
            :type="activeTab === 'recipes' ? 'primary' : 'default'"
          >
            <el-icon><Document /></el-icon>
            <span>配方管理</span>
          </el-button>
        </div>
      </div>
    </el-header>

    <el-main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>

    <el-footer class="app-footer">
      <p>© 2026 火箭燃料推进系统参数仿真平台 | Powered by Spring Boot & Vue 3</p>
    </el-footer>
  </el-container>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Operation, Document, Van } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const activeTab = ref(route.name || 'simulation')

watch(activeTab, (newVal) => {
  router.push({ name: newVal })
})

watch(() => route.name, (newVal) => {
  activeTab.value = newVal || 'simulation'
})
</script>

<style lang="scss">
.app-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.app-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  height: 70px;
  padding: 0;

  .header-content {
    max-width: 1600px;
    margin: 0 auto;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;

    .logo {
      display: flex;
      align-items: center;
      gap: 12px;

      h1 {
        margin: 0;
        font-size: 22px;
        font-weight: 600;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }
    }

    .header-nav {
      display: flex;
      gap: 12px;
    }
  }
}

.app-main {
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
  padding: 24px;
  box-sizing: border-box;
}

.app-footer {
  background: rgba(255, 255, 255, 0.9);
  text-align: center;
  color: #666;
  font-size: 13px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;

  p {
    margin: 0;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
