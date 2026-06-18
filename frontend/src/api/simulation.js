import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

service.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    console.error('响应错误:', error)
    if (error.response) {
      const message = error.response.data?.message || '请求失败'
      ElMessage.error(message)
    } else {
      ElMessage.error('网络连接失败，请检查后端服务是否启动')
    }
    return Promise.reject(error)
  }
)

export const simulationApi = {
  getAllRecipes(keyword) {
    return service.get('/simulation/recipes', { params: { keyword } })
  },

  getRecipeById(id) {
    return service.get(`/simulation/recipes/${id}`)
  },

  createRecipe(data) {
    return service.post('/simulation/recipes', data)
  },

  updateRecipe(id, data) {
    return service.put(`/simulation/recipes/${id}`, data)
  },

  deleteRecipe(id) {
    return service.delete(`/simulation/recipes/${id}`)
  },

  runSimulation(data) {
    return service.post('/simulation/run', data)
  },

  runSimulationById(id) {
    return service.post(`/simulation/run/${id}`)
  },

  previewSimulation(data) {
    return service.post('/simulation/preview', data)
  }
}

export default service
