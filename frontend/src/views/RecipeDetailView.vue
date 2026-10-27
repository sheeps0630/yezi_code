<!-- 菜谱详情页：从收藏夹或家庭菜谱进入时提供明确的返回入口 -->
<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRecipe } from '../api/modules'
import type { Recipe } from '../api/types'

const route = useRoute()
const router = useRouter()
const recipe = ref<Recipe | null>(null)
const isLoading = ref(false)
const errorMessage = ref('')

/**
 * 加载当前路由对应的真实菜谱。
 */
async function loadRecipe(): Promise<void> {
  const dishId = route.params.dishId
  if (typeof dishId !== 'string' || !dishId) {
    errorMessage.value = '菜谱标识无效'
    recipe.value = null
    return
  }
  isLoading.value = true
  errorMessage.value = ''
  try {
    recipe.value = await getRecipe(dishId)
  } catch (error) {
    recipe.value = null
    errorMessage.value = error instanceof Error ? error.message : '加载菜谱失败'
  } finally {
    isLoading.value = false
  }
}

/**
 * 返回进入菜谱详情前所在的应用页面。
 */
function returnToSource(): void {
  const source = route.query.from
  if (typeof source === 'string' && source.startsWith('/') && !source.startsWith('//')) {
    void router.push(source)
    return
  }
  void router.push({ name: 'profile' })
}

watch(() => route.params.dishId, loadRecipe)
onMounted(loadRecipe)
</script>

<template>
  <div>
    <button class="btn btn-outline back-button" @click="returnToSource">← 返回</button>
    <div v-if="isLoading" class="card hint">正在加载菜谱…</div>
    <div v-else-if="errorMessage" class="error-tip">{{ errorMessage }}</div>
    <section v-else-if="recipe" class="card recipe-detail">
      <div class="recipe-kicker">椰子家的好味道</div>
      <h2 class="section-title">{{ recipe.dishName }}</h2>
      <div class="recipe-section-title">准备食材</div>
      <ul class="ingredient-list">
        <li v-for="ingredient in recipe.ingredients" :key="ingredient">{{ ingredient }}</li>
      </ul>
      <div class="recipe-section-title">烹饪步骤</div>
      <ol class="recipe-step-list">
        <li v-for="step in recipe.steps" :key="step">{{ step }}</li>
      </ol>
      <div v-if="recipe.tips" class="recipe-tip">{{ recipe.tips }}</div>
    </section>
  </div>
</template>
