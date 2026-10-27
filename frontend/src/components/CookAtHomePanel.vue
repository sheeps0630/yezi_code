<!-- 在家做饭：选择菜类与食材偏好，生成固定菜谱中可匹配的菜品 -->
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  addFavorite,
  addFamilyRecipe,
  generateMenu,
  getRecipe,
  listCookCategories,
  listCookIngredientTypes,
} from '../api/modules'
import type { Dish, MenuResponse, Recipe } from '../api/types'

const categoryNameMap: Record<string, string> = {
  vegetable: '素菜',
  meat: '肉菜',
  dessert: '甜品',
  seafood: '海鲜',
  soup: '汤羹',
}

const cookCategories = ref<string[]>([])
const ingredientTypes = ref<Record<string, string[]>>({})
const selectedCategories = ref<string[]>([])
const ingredientFilters = ref<Record<string, string[]>>({})
const expandedCategories = ref<string[]>([])
const searchQuery = ref('')
const menu = ref<MenuResponse | null>(null)
const currentRecipe = ref<Recipe | null>(null)
const toast = ref('')
const selectionError = ref('')
const actionError = ref('')
const recipeError = ref('')
const isLoadingOptions = ref(false)

const visibleIngredientGroups = computed(() => {
  const query = searchQuery.value.trim().toLocaleLowerCase()
  return selectedCategories.value
    .map((category) => {
      const options = ingredientTypes.value[category] ?? []
      const categoryMatches = query.length > 0
        && (categoryNameMap[category] ?? category).toLocaleLowerCase().includes(query)
      return {
        category,
        options: categoryMatches || !query
          ? options
          : options.filter((option) => option.toLocaleLowerCase().includes(query)),
      }
    })
    .filter((group) => group.options.length > 0)
})

const totalSelectedIngredients = computed(() =>
  Object.values(ingredientFilters.value).reduce((sum, values) => sum + values.length, 0),
)

/**
 * 加载做饭大类和每类的食材标签。
 */
async function loadOptions(): Promise<void> {
  isLoadingOptions.value = true
  try {
    const [categories, ingredients] = await Promise.all([
      listCookCategories(),
      listCookIngredientTypes(),
    ])
    cookCategories.value = categories
    ingredientTypes.value = ingredients
  } catch (error) {
    selectionError.value = error instanceof Error ? error.message : '加载菜类失败'
  } finally {
    isLoadingOptions.value = false
  }
}

/**
 * 切换第一步的菜类。
 *
 * @param category 菜类标识
 */
function toggleCategory(category: string): void {
  const index = selectedCategories.value.indexOf(category)
  if (index >= 0) {
    selectedCategories.value.splice(index, 1)
    delete ingredientFilters.value[category]
    expandedCategories.value = expandedCategories.value.filter((item) => item !== category)
  } else {
    selectedCategories.value.push(category)
    if (!expandedCategories.value.includes(category)) expandedCategories.value.push(category)
  }
  selectionError.value = ''
}

/**
 * 切换某个菜类中的食材类型。
 *
 * @param category 菜类标识
 * @param ingredient 食材标签
 */
function toggleIngredient(category: string, ingredient: string): void {
  const selected = ingredientFilters.value[category] ?? []
  const index = selected.indexOf(ingredient)
  ingredientFilters.value[category] = index >= 0
    ? selected.filter((item) => item !== ingredient)
    : [...selected, ingredient]
}

/**
 * 展开或收起一个食材分组。
 *
 * @param category 菜类标识
 */
function toggleIngredientGroup(category: string): void {
  const index = expandedCategories.value.indexOf(category)
  if (index >= 0) {
    expandedCategories.value.splice(index, 1)
  } else {
    expandedCategories.value.push(category)
  }
}

/**
 * 判断搜索时某个食材组是否应自动展开。
 *
 * @param category 菜类标识
 * @returns 是否包含搜索匹配结果
 */
function isSearchMatch(category: string): boolean {
  return searchQuery.value.trim().length > 0
    && visibleIngredientGroups.value.some((group) => group.category === category)
}

/**
 * 按所选菜类与食材偏好生成菜单。
 */
async function handleGenerateMenu(): Promise<void> {
  if (selectedCategories.value.length === 0) {
    selectionError.value = '请至少选择一个菜类'
    return
  }
  actionError.value = ''
  selectionError.value = ''
  currentRecipe.value = null
  try {
    const filters = Object.fromEntries(
      selectedCategories.value
        .filter((category) => (ingredientFilters.value[category]?.length ?? 0) > 0)
        .map((category) => [category, ingredientFilters.value[category]]),
    )
    menu.value = await generateMenu({
      categories: selectedCategories.value,
      ingredientFilters: filters,
    })
  } catch (error) {
    actionError.value = error instanceof Error ? error.message : '生成菜单失败'
  }
}

/**
 * 查看一道菜的静态菜谱。
 *
 * @param dish 菜品
 */
async function openRecipe(dish: Dish): Promise<void> {
  recipeError.value = ''
  try {
    currentRecipe.value = await getRecipe(dish.id)
  } catch (error) {
    recipeError.value = error instanceof Error ? error.message : '加载菜谱失败'
  }
}

/**
 * 把当前菜谱对应的菜加入收藏。
 */
async function handleAddFavorite(): Promise<void> {
  if (!currentRecipe.value) return
  try {
    await addFavorite('dish', currentRecipe.value.dishId, currentRecipe.value.dishName)
    toast.value = `已收藏「${currentRecipe.value.dishName}」`
    recipeError.value = ''
  } catch (error) {
    recipeError.value = error instanceof Error ? error.message : '收藏失败'
  }
}

/**
 * 把当前菜谱对应的菜加入家庭菜谱。
 */
async function handleAddFamilyRecipe(): Promise<void> {
  if (!currentRecipe.value) return
  try {
    await addFamilyRecipe(currentRecipe.value.dishId)
    toast.value = `已把「${currentRecipe.value.dishName}」加入家庭菜谱`
    recipeError.value = ''
  } catch (error) {
    recipeError.value = error instanceof Error ? error.message : '加入家庭菜谱失败'
  }
}

onMounted(loadOptions)
</script>

<template>
  <div>
    <div v-if="toast" class="success-tip">{{ toast }}</div>

    <section class="card">
      <div class="section-title">第一步 · 选想吃的菜类（可多选）</div>
      <div v-if="isLoadingOptions" class="hint">正在加载菜类…</div>
      <button
        v-for="category in cookCategories"
        :key="category"
        class="chip"
        :class="{ selected: selectedCategories.includes(category) }"
        :aria-pressed="selectedCategories.includes(category)"
        @click="toggleCategory(category)"
      >
        {{ categoryNameMap[category] ?? category }}
      </button>
      <div v-if="selectionError" class="error-tip inline-error">{{ selectionError }}</div>
    </section>

    <section class="card">
      <div class="cook-step-heading">
        <div>
          <div class="section-title">第二步 · 选食材方向（可多选）</div>
          <p class="hint">同一菜类选多个食材时，生成结果匹配其中任意一种；未选食材的菜类不额外筛选。</p>
        </div>
        <span v-if="totalSelectedIngredients" class="selected-count">已选 {{ totalSelectedIngredients }}</span>
      </div>
      <div v-if="selectedCategories.length === 0" class="hint">先在第一步选择菜类，再搜索和勾选食材。</div>
      <template v-else>
        <label class="field-label" for="ingredient-search">搜索食材</label>
        <input
          id="ingredient-search"
          v-model="searchQuery"
          class="input"
          type="search"
          placeholder="例如：西兰花、牛肉、虾"
        />
        <div v-if="visibleIngredientGroups.length === 0" class="hint">没有匹配的食材选项。</div>
        <div
          v-for="group in visibleIngredientGroups"
          :key="group.category"
          class="ingredient-group"
        >
          <button
            class="ingredient-group-toggle"
            :aria-expanded="expandedCategories.includes(group.category) || isSearchMatch(group.category)"
            @click="toggleIngredientGroup(group.category)"
          >
            <span>
              {{ categoryNameMap[group.category] ?? group.category }}
              <span class="ingredient-count">
                {{ ingredientFilters[group.category]?.length ?? 0 }}/{{ ingredientTypes[group.category]?.length ?? 0 }}
              </span>
            </span>
            <span aria-hidden="true">{{ expandedCategories.includes(group.category) || isSearchMatch(group.category) ? '−' : '+' }}</span>
          </button>
          <div
            v-if="expandedCategories.includes(group.category) || isSearchMatch(group.category)"
            class="ingredient-options"
          >
            <button
              v-for="ingredient in group.options"
              :key="ingredient"
              class="chip ingredient-chip"
              :class="{ selected: ingredientFilters[group.category]?.includes(ingredient) }"
              :aria-pressed="ingredientFilters[group.category]?.includes(ingredient) ?? false"
              @click="toggleIngredient(group.category, ingredient)"
            >
              {{ ingredient }}
            </button>
          </div>
        </div>
      </template>
    </section>

    <section class="card action-card">
      <button class="btn" :disabled="isLoadingOptions" @click="handleGenerateMenu">生成今天这一餐</button>
      <div v-if="actionError" class="error-tip inline-error">{{ actionError }}</div>
    </section>

    <section v-if="menu" class="card">
      <div class="section-title">今天这一餐</div>
      <div
        v-for="dish in menu.dishes"
        :key="dish.id"
        class="list-row dish-result-row"
        role="button"
        tabindex="0"
        @click="openRecipe(dish)"
        @keydown.enter="openRecipe(dish)"
      >
        <span class="row-title">{{ dish.name }}</span>
        <span class="hint">
          {{ categoryNameMap[dish.category] ?? dish.category }} · 约{{ dish.cookMinutes }}分钟
        </span>
      </div>
      <div class="hint">点击菜名查看菜谱。</div>
    </section>

    <section v-if="currentRecipe" class="card">
      <div v-if="recipeError" class="error-tip inline-error">{{ recipeError }}</div>
      <div class="section-title">{{ currentRecipe.dishName }} · 菜谱</div>
      <div class="recipe-section-title">食材</div>
      <div v-for="item in currentRecipe.ingredients" :key="item" class="recipe-line">· {{ item }}</div>
      <div class="recipe-section-title">做法</div>
      <div v-for="(step, index) in currentRecipe.steps" :key="`${index}-${step}`" class="recipe-line">
        {{ index + 1 }}. {{ step }}
      </div>
      <div v-if="currentRecipe.tips" class="recipe-tip">{{ currentRecipe.tips }}</div>
      <div class="recipe-actions">
        <button class="btn" @click="handleAddFavorite">加入收藏</button>
        <button class="btn btn-outline" @click="handleAddFamilyRecipe">加入家庭菜谱</button>
      </div>
    </section>
    <div v-else-if="recipeError" class="error-tip inline-error">{{ recipeError }}</div>
  </div>
</template>
