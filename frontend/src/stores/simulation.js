import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { simulationApi } from '@/api/simulation'

export const useSimulationStore = defineStore('simulation', () => {
  const recipes = ref([])
  const currentResult = ref(null)
  const loading = ref(false)
  const simulating = ref(false)

  const recipeOptions = computed(() =>
    recipes.value.map(r => ({ label: r.name, value: r.id }))
  )

  async function fetchRecipes(keyword = '') {
    loading.value = true
    try {
      const data = await simulationApi.getAllRecipes(keyword)
      recipes.value = data
      return data
    } finally {
      loading.value = false
    }
  }

  async function createRecipe(formData) {
    const data = await simulationApi.createRecipe(formData)
    recipes.value.unshift(data)
    return data
  }

  async function updateRecipe(id, formData) {
    const data = await simulationApi.updateRecipe(id, formData)
    const index = recipes.value.findIndex(r => r.id === id)
    if (index !== -1) {
      recipes.value[index] = data
    }
    return data
  }

  async function deleteRecipe(id) {
    await simulationApi.deleteRecipe(id)
    recipes.value = recipes.value.filter(r => r.id !== id)
  }

  async function runSimulation(formData) {
    simulating.value = true
    try {
      const result = await simulationApi.runSimulation(formData)
      currentResult.value = result
      await fetchRecipes()
      return result
    } finally {
      simulating.value = false
    }
  }

  async function runSimulationById(id) {
    simulating.value = true
    try {
      const result = await simulationApi.runSimulationById(id)
      currentResult.value = result
      return result
    } finally {
      simulating.value = false
    }
  }

  async function previewSimulation(formData) {
    simulating.value = true
    try {
      const result = await simulationApi.previewSimulation(formData)
      currentResult.value = result
      return result
    } finally {
      simulating.value = false
    }
  }

  function loadRecipeToForm(recipe, form) {
    form.name = recipe.name
    form.description = recipe.description || ''
    form.loxRatio = recipe.loxRatio
    form.keroseneRatio = recipe.keroseneRatio
    form.chamberPressure = recipe.chamberPressure
    form.initialTemperature = recipe.initialTemperature
    form.burnDuration = recipe.burnDuration
    form.nozzleAreaRatio = recipe.nozzleAreaRatio
  }

  function clearResult() {
    currentResult.value = null
  }

  return {
    recipes,
    currentResult,
    loading,
    simulating,
    recipeOptions,
    fetchRecipes,
    createRecipe,
    updateRecipe,
    deleteRecipe,
    runSimulation,
    runSimulationById,
    previewSimulation,
    loadRecipeToForm,
    clearResult
  }
})
