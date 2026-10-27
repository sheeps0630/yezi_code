<!-- 喝什么：先选外出觅饮或在家自制，再进入对应的转盘、附近、收藏或配方功能 -->
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  addFavorite,
  listDrinkCategories,
  listFavorites,
  recommendDrink,
} from '../api/modules'
import FavoriteCollectionPanel from '../components/FavoriteCollectionPanel.vue'
import { drinkRecipeCategoryNames, drinkRecipes } from '../api/drinkRecipes'
import type { DrinkRecipe } from '../api/drinkRecipes'
import type { DrinkProduct, FavoriteItem } from '../api/types'

interface WheelEntry {
  id: string
  name: string
  weight: number
}

interface WheelPreset {
  id: string
  name: string
  entries: WheelEntry[]
}

interface WheelStore {
  activePresetId: string
  presets: WheelPreset[]
}

interface WheelSegment {
  path: string
  fill: string
  label: string
  labelX: number
  labelY: number
}

const wheelStorageKey = 'yechijia.drink-wheel.v1'
const wheelColors = ['#76a96b', '#e3b56c', '#9c7257', '#d98968', '#8caeb0', '#c4a0b6', '#d1cb82']
const scene = ref<'out' | 'home'>('out')
const outMode = ref<'wheel' | 'nearby' | 'favorites'>('nearby')
const drinkCategories = ref<string[]>([])
const selectedCategory = ref('')
const recommendation = ref<DrinkProduct | null>(null)
const seenProducts = ref<DrinkProduct[]>([])
const favoriteProductIds = ref<string[]>([])
const showSeenProducts = ref(false)
const errorMessage = ref('')
const isBusy = ref(false)
const selectedRecipeCategories = ref<string[]>([])
const selectedRecipe = ref<DrinkRecipe | null>(null)
const favoriteRecipeIds = ref<string[]>([])
const recipeToast = ref('')
const wheelStore = ref<WheelStore | null>(null)
const wheelRotation = ref(0)
const wheelSelection = ref('')
const spinning = ref(false)
const newEntryName = ref('')
const newEntryWeight = ref(1)
const newPresetName = ref('')
const wheelError = ref('')

const categoryNameMap: Record<string, string> = {
  'milk-tea': '奶茶',
  coffee: '咖啡',
  others: '其他饮品',
}

const activePreset = computed(() =>
  wheelStore.value?.presets.find((preset) => preset.id === wheelStore.value?.activePresetId) ?? null,
)

const wheelSegments = computed<WheelSegment[]>(() => {
  const entries = activePreset.value?.entries ?? []
  if (entries.length === 0) return []
  const totalWeight = entries.reduce((sum, entry) => sum + entry.weight, 0)
  let accumulatedWeight = 0
  return entries.map((entry, index) => {
    const startAngle = -90 + accumulatedWeight / totalWeight * 360
    accumulatedWeight += entry.weight
    const endAngle = -90 + accumulatedWeight / totalWeight * 360
    const middleAngle = (startAngle + endAngle) / 2 * Math.PI / 180
    const radius = entries.length > 8 ? 68 : 61
    const labelX = 120 + Math.cos(middleAngle) * radius
    const labelY = 120 + Math.sin(middleAngle) * radius
    const startX = 120 + Math.cos(startAngle * Math.PI / 180) * 100
    const startY = 120 + Math.sin(startAngle * Math.PI / 180) * 100
    const endX = 120 + Math.cos(endAngle * Math.PI / 180) * 100
    const endY = 120 + Math.sin(endAngle * Math.PI / 180) * 100
    const largeArc = endAngle - startAngle > 180 ? 1 : 0
    return {
      path: entries.length === 1
        ? ''
        : `M 120 120 L ${startX} ${startY} A 100 100 0 ${largeArc} 1 ${endX} ${endY} Z`,
      fill: wheelColors[index % wheelColors.length],
      label: entry.name,
      labelX,
      labelY,
    }
  })
})

const visibleDrinkRecipes = computed(() => selectedRecipeCategories.value.length === 0
  ? drinkRecipes
  : drinkRecipes.filter((recipe) => selectedRecipeCategories.value.includes(recipe.category)))

const drinkRecipeCategories = computed(() => Object.keys(drinkRecipeCategoryNames))

/**
 * 加载外出饮品类型和已收藏状态。
 */
async function loadOptions(): Promise<void> {
  try {
    const [categories, favorites] = await Promise.all([listDrinkCategories(), listFavorites()])
    drinkCategories.value = categories
    favoriteProductIds.value = favorites
      .filter((favorite) => favorite.targetType === 'drink')
      .map((favorite) => favorite.targetId)
    favoriteRecipeIds.value = favorites
      .filter((favorite) => favorite.targetType === 'drink-recipe')
      .map((favorite) => favorite.targetId)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载饮品选项失败'
  }
}

/**
 * 校验本地饮品转盘方案。
 *
 * @param value 待校验数据
 * @returns 有效转盘数据或 null
 */
function parseWheelStore(value: unknown): WheelStore | null {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return null
  const candidate = value as Partial<WheelStore>
  if (
    typeof candidate.activePresetId !== 'string'
    || !Array.isArray(candidate.presets)
    || candidate.presets.length === 0
  ) return null
  const validPresets = candidate.presets.every((preset) =>
    typeof preset.id === 'string'
    && typeof preset.name === 'string'
    && preset.name.trim().length > 0
    && Array.isArray(preset.entries)
    && preset.entries.length > 0
    && preset.entries.every((entry) =>
      typeof entry.id === 'string'
      && typeof entry.name === 'string'
      && entry.name.trim().length > 0
      && Number.isInteger(entry.weight)
      && entry.weight >= 1,
    ),
  )
  return validPresets && candidate.presets.some((preset) => preset.id === candidate.activePresetId)
    ? candidate as WheelStore
    : null
}

/**
 * 初始化并读取本地饮品转盘方案。
 */
function loadWheel(): void {
  try {
    const stored = window.localStorage.getItem(wheelStorageKey)
    if (stored) {
      const parsed = parseWheelStore(JSON.parse(stored) as unknown)
      if (!parsed) throw new Error('饮品转盘数据格式异常，请清理浏览器缓存后重试')
      wheelStore.value = parsed
      return
    }
    const firstPreset: WheelPreset = {
      id: `drink-preset-${Date.now()}`,
      name: '我的饮品转盘',
      entries: ['奶茶', '咖啡', '果茶', '气泡水'].map((name, index) => ({
        id: `drink-entry-${Date.now()}-${index}`,
        name,
        weight: 1,
      })),
    }
    wheelStore.value = { activePresetId: firstPreset.id, presets: [firstPreset] }
    persistWheel()
  } catch (error) {
    wheelError.value = error instanceof Error ? error.message : '读取饮品转盘失败'
  }
}

/**
 * 持久化饮品转盘。
 */
function persistWheel(): void {
  if (!wheelStore.value) return
  try {
    window.localStorage.setItem(wheelStorageKey, JSON.stringify(wheelStore.value))
    wheelError.value = ''
  } catch {
    wheelError.value = '无法保存饮品转盘，请检查浏览器存储设置'
  }
}

/**
 * 新增饮品转盘选项。
 */
function addWheelEntry(): void {
  const name = newEntryName.value.trim()
  const preset = activePreset.value
  if (!name) {
    wheelError.value = '请填写饮品名称'
    return
  }
  if (!preset || preset.entries.some((entry) => entry.name.toLowerCase() === name.toLowerCase())) {
    wheelError.value = '该饮品已存在或转盘尚未加载'
    return
  }
  preset.entries.push({
    id: `drink-entry-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    name,
    weight: Math.max(1, Math.floor(newEntryWeight.value || 1)),
  })
  newEntryName.value = ''
  newEntryWeight.value = 1
  wheelSelection.value = ''
  persistWheel()
}

/**
 * 保存一个饮品权重。
 *
 * @param entry 转盘项
 */
function saveWheelWeight(entry: WheelEntry): void {
  entry.weight = Number.isFinite(entry.weight) ? Math.max(1, Math.floor(entry.weight)) : 1
  wheelSelection.value = ''
  persistWheel()
}

/**
 * 删除一个饮品转盘选项，至少保留一个可抽选内容。
 *
 * @param entryId 选项标识
 */
function removeWheelEntry(entryId: string): void {
  const preset = activePreset.value
  if (!preset || preset.entries.length <= 1) {
    wheelError.value = '转盘至少需要保留一个选项'
    return
  }
  preset.entries = preset.entries.filter((entry) => entry.id !== entryId)
  wheelSelection.value = ''
  persistWheel()
}

/**
 * 切换当前饮品转盘方案。
 *
 * @param presetId 方案标识
 */
function selectWheelPreset(presetId: string): void {
  if (!wheelStore.value?.presets.some((preset) => preset.id === presetId)) {
    wheelError.value = '找不到这个转盘方案'
    return
  }
  wheelStore.value.activePresetId = presetId
  wheelSelection.value = ''
  persistWheel()
}

/**
 * 删除当前饮品方案并切换到剩余方案。
 */
function deleteWheelPreset(): void {
  if (!wheelStore.value || wheelStore.value.presets.length <= 1) {
    wheelError.value = '至少保留一个转盘方案'
    return
  }
  wheelStore.value.presets = wheelStore.value.presets.filter(
    (preset) => preset.id !== wheelStore.value?.activePresetId,
  )
  wheelStore.value.activePresetId = wheelStore.value.presets[0].id
  wheelSelection.value = ''
  persistWheel()
}

/**
 * 按权重抽取饮品并旋转转盘。
 */
function spinWheel(): void {
  const entries = activePreset.value?.entries ?? []
  if (spinning.value || entries.length === 0) return
  const totalWeight = entries.reduce((sum, entry) => sum + entry.weight, 0)
  let point = Math.random() * totalWeight
  let selectedIndex = 0
  for (let index = 0; index < entries.length; index += 1) {
    point -= entries[index].weight
    if (point < 0) {
      selectedIndex = index
      break
    }
  }
  const before = entries.slice(0, selectedIndex).reduce((sum, entry) => sum + entry.weight, 0)
  const selectedCenter = (before + entries[selectedIndex].weight / 2) / totalWeight * 360
  wheelRotation.value += 1440 + (360 - selectedCenter) % 360
  wheelSelection.value = ''
  spinning.value = true
  window.setTimeout(() => {
    wheelSelection.value = entries[selectedIndex].name
    spinning.value = false
  }, 2100)
}

/**
 * 保存当前饮品转盘为一个新方案。
 */
function saveWheelPreset(): void {
  const name = newPresetName.value.trim()
  const preset = activePreset.value
  if (!name || !preset || !wheelStore.value) {
    wheelError.value = '请填写方案名称'
    return
  }
  if (wheelStore.value.presets.some((item) => item.name.toLowerCase() === name.toLowerCase())) {
    wheelError.value = '已有同名方案，请换一个名称'
    return
  }
  const newPreset: WheelPreset = {
    id: `drink-preset-${Date.now()}`,
    name,
    entries: preset.entries.map((entry) => ({
      ...entry,
      id: `drink-entry-${Date.now()}-${entry.id}`,
    })),
  }
  wheelStore.value.presets.push(newPreset)
  wheelStore.value.activePresetId = newPreset.id
  newPresetName.value = ''
  persistWheel()
}

/**
 * 推荐一款外出饮品，排除本轮已看内容。
 */
async function handleRecommend(): Promise<void> {
  if (isBusy.value) return
  isBusy.value = true
  errorMessage.value = ''
  try {
    const result = await recommendDrink({
      useNearby: true,
      category: selectedCategory.value || undefined,
      latitude: 39.9,
      longitude: 116.4,
      excludedProductIds: seenProducts.value.map((product) => product.id),
    })
    recommendation.value = result
    seenProducts.value.push(result)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '推荐失败，请稍后再试'
  } finally {
    isBusy.value = false
  }
}

/**
 * 收藏当前外出饮品。
 */
async function favoriteProduct(): Promise<void> {
  if (!recommendation.value || favoriteProductIds.value.includes(recommendation.value.id)) return
  try {
    const favorite = await addFavorite(
      'drink',
      recommendation.value.id,
      `${recommendation.value.productName} · ${recommendation.value.brand}`,
    )
    favoriteProductIds.value.push(favorite.targetId)
    errorMessage.value = ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '收藏饮品失败'
  }
}

/**
 * 切换自制饮品类别筛选。
 *
 * @param category 饮品分类
 */
function toggleRecipeCategory(category: string): void {
  const index = selectedRecipeCategories.value.indexOf(category)
  if (index >= 0) selectedRecipeCategories.value.splice(index, 1)
  else selectedRecipeCategories.value.push(category)
}

/**
 * 从当前筛选范围中随机挑选一款自制饮品。
 */
function chooseDrinkRecipe(): void {
  const candidates = visibleDrinkRecipes.value.filter((recipe) => recipe.id !== selectedRecipe.value?.id)
  const pool = candidates.length > 0 ? candidates : visibleDrinkRecipes.value
  if (pool.length === 0) return
  const selected = pool[Math.floor(Math.random() * pool.length)]
  selectedRecipe.value = selected
  recipeToast.value = ''
}

/**
 * 收藏当前自制饮品配方。
 *
 * @param recipe 配方
 */
async function favoriteRecipe(recipe: DrinkRecipe): Promise<void> {
  if (favoriteRecipeIds.value.includes(recipe.id)) return
  try {
    const favorite = await addFavorite('drink-recipe', recipe.id, recipe.name)
    favoriteRecipeIds.value.push(favorite.targetId)
    recipeToast.value = `已收藏「${recipe.name}」`
  } catch (error) {
    recipeToast.value = error instanceof Error ? error.message : '收藏配方失败'
  }
}

/**
 * 从收藏列表打开本机固定配方详情。
 *
 * @param favorite 收藏条目
 */
function openFavoriteRecipe(favorite: FavoriteItem): void {
  if (favorite.targetType !== 'drink-recipe') return
  const recipe = drinkRecipes.find((item) => item.id === favorite.targetId)
  if (recipe) {
    scene.value = 'home'
    selectedRecipe.value = recipe
  }
}

/**
 * 切换外出推荐类别。
 *
 * @param category 饮品类别
 */
function toggleCategory(category: string): void {
  selectedCategory.value = selectedCategory.value === category ? '' : category
}

/**
 * 切换场景时清除前一场景提示。
 *
 * @param nextScene 目标场景
 */
function selectScene(nextScene: 'out' | 'home'): void {
  if (scene.value !== nextScene && scene.value === 'out') {
    seenProducts.value = []
    recommendation.value = null
    selectedCategory.value = ''
  }
  scene.value = nextScene
  errorMessage.value = ''
  recipeToast.value = ''
}

onMounted(() => {
  void loadOptions()
  loadWheel()
})
</script>

<template>
  <div>
    <h2 class="page-title">今天喝什么？</h2>
    <p class="page-subtitle">从外出觅饮到在家调一杯，椰子陪你选。</p>

    <section class="card">
      <div class="section-title">先选喝饮场景</div>
      <div class="choice-row">
        <button class="chip" :class="{ selected: scene === 'out' }" @click="selectScene('out')">外出觅饮</button>
        <button class="chip" :class="{ selected: scene === 'home' }" @click="selectScene('home')">在家自制</button>
      </div>
    </section>

    <template v-if="scene === 'out'">
      <section class="card">
        <div class="section-title">外出觅饮</div>
        <div class="choice-row">
          <button class="chip" :class="{ selected: outMode === 'wheel' }" @click="outMode = 'wheel'">饮品转盘</button>
          <button class="chip" :class="{ selected: outMode === 'nearby' }" @click="outMode = 'nearby'">附近饮品</button>
          <button class="chip" :class="{ selected: outMode === 'favorites' }" @click="outMode = 'favorites'">收藏</button>
        </div>
        <div v-if="errorMessage && outMode !== 'favorites'" class="error-tip inline-error">{{ errorMessage }}</div>
      </section>

      <template v-if="outMode === 'wheel'">
        <section class="card">
          <div class="section-title">我的饮品转盘</div>
          <div v-if="activePreset" class="preset-controls">
            <label class="field-label" for="drink-wheel-preset">当前方案</label>
            <select
              id="drink-wheel-preset"
              class="input preset-select"
              :value="activePreset.id"
              @change="selectWheelPreset(($event.target as HTMLSelectElement).value)"
            >
              <option v-for="preset in wheelStore?.presets ?? []" :key="preset.id" :value="preset.id">
                {{ preset.name }}
              </option>
            </select>
            <button
              class="btn btn-danger"
              :disabled="(wheelStore?.presets.length ?? 0) <= 1"
              @click="deleteWheelPreset"
            >
              删除方案
            </button>
          </div>
          <div class="form-row">
            <input v-model="newPresetName" class="input" placeholder="另存方案名称" aria-label="新饮品方案名称" />
            <button class="btn btn-outline" @click="saveWheelPreset">保存方案</button>
          </div>
          <div v-if="wheelError" class="error-tip inline-error">{{ wheelError }}</div>
          <div class="wheel-layout">
            <div class="wheel-wrap">
              <div class="wheel-pointer" aria-hidden="true"></div>
              <div class="wheel-disc" :style="{ transform: `rotate(${wheelRotation}deg)` }">
                <svg class="wheel-svg" viewBox="0 0 240 240" role="img" aria-label="按权重分区的饮品转盘">
                  <circle v-if="wheelSegments.length === 1" cx="120" cy="120" r="100" :fill="wheelSegments[0].fill" />
                  <path
                    v-for="(segment, index) in wheelSegments"
                    v-else
                    :key="`${segment.label}-${index}`"
                    :d="segment.path"
                    :fill="segment.fill"
                    stroke="#fff8e9"
                    stroke-width="2"
                  />
                  <text
                    v-for="(segment, index) in wheelSegments"
                    :key="`label-${segment.label}-${index}`"
                    :x="segment.labelX"
                    :y="segment.labelY"
                    class="wheel-label"
                    :transform="spinning ? undefined : `rotate(${-wheelRotation} ${segment.labelX} ${segment.labelY})`"
                  >
                    {{ segment.label.length > 6 ? `${segment.label.slice(0, 6)}…` : segment.label }}
                  </text>
                </svg>
                <div class="wheel-center" aria-hidden="true">椰</div>
              </div>
            </div>
            <div class="wheel-result" aria-live="polite">
              <span class="hint">{{ spinning ? '正在挑选一杯…' : '转一下，看看喝什么' }}</span>
              <strong v-if="wheelSelection">{{ wheelSelection }}</strong>
              <button class="btn wheel-spin-button" :disabled="spinning" @click="spinWheel">
                {{ spinning ? '转动中…' : '开始转盘' }}
              </button>
            </div>
          </div>
        </section>
        <section class="card">
          <div class="section-title">转盘内容与权重</div>
          <div v-for="entry in activePreset?.entries ?? []" :key="entry.id" class="wheel-entry">
            <span class="wheel-entry-name">{{ entry.name }}</span>
            <label class="weight-input-label">
              权重
              <input
                v-model.number="entry.weight"
                class="input weight-input"
                type="number"
                min="1"
                step="1"
                :aria-label="`${entry.name}的权重`"
                @change="saveWheelWeight(entry)"
              />
            </label>
            <button
              class="btn btn-danger"
              :disabled="(activePreset?.entries.length ?? 0) <= 1"
              :aria-label="`删除${entry.name}`"
              @click="removeWheelEntry(entry.id)"
            >
              删除
            </button>
          </div>
          <div class="form-row entry-form">
            <input
              v-model="newEntryName"
              class="input"
              placeholder="填入想喝的饮品"
              aria-label="饮品转盘选项"
              @keyup.enter="addWheelEntry"
            />
            <input
              v-model.number="newEntryWeight"
              class="input weight-input"
              type="number"
              min="1"
              step="1"
              aria-label="新饮品权重"
            />
            <button class="btn" @click="addWheelEntry">添加</button>
          </div>
        </section>
      </template>

      <template v-else-if="outMode === 'nearby'">
        <section class="card">
          <div class="section-title">附近 3km 的饮品</div>
          <div class="hint">可按饮品类型缩小范围；附近门店和在售饮品为演示示例数据。</div>
          <div class="choice-row" style="margin-top: 10px">
            <button
              v-for="category in drinkCategories"
              :key="category"
              class="chip"
              :class="{ selected: selectedCategory === category }"
              @click="toggleCategory(category)"
            >
              {{ categoryNameMap[category] ?? category }}
            </button>
          </div>
          <div class="nearby-actions">
            <button class="btn" :disabled="isBusy" @click="handleRecommend">
              {{ isBusy ? '正在推荐…' : recommendation ? '换一款' : '给我推荐一款' }}
            </button>
            <button v-if="seenProducts.length" class="btn btn-outline" @click="showSeenProducts = true">
              已看（{{ seenProducts.length }}）
            </button>
          </div>
          <div v-if="errorMessage" class="error-tip inline-error">{{ errorMessage }}</div>
        </section>
        <section v-if="recommendation" class="result-card">
          <div class="result-title">{{ recommendation.brand }}</div>
          <div class="result-sub">
            {{ recommendation.productName }} · 约 {{ recommendation.priceYuan }} 元
            <br />
            {{ recommendation.note }}
          </div>
          <div class="result-sub result-footnote">* 以门店实际供应为准；当前展示演示数据。</div>
          <button
            class="btn btn-outline result-favorite-button"
            :disabled="favoriteProductIds.includes(recommendation.id)"
            @click="favoriteProduct"
          >
            {{ favoriteProductIds.includes(recommendation.id) ? '已收藏' : '收藏这款' }}
          </button>
        </section>
      </template>

      <FavoriteCollectionPanel
        v-else
        :target-types="['drink', 'drink-recipe']"
        @open="openFavoriteRecipe"
      />
    </template>

    <template v-else>
      <section class="card">
        <div class="section-title">在家自制什么？</div>
        <p class="hint">分类可多选；不选类别时“帮我选一款”会从全部配方中挑选。</p>
        <div class="recipe-category-list">
          <button
            v-for="category in drinkRecipeCategories"
            :key="category"
            class="chip"
            :class="{ selected: selectedRecipeCategories.includes(category) }"
            :aria-pressed="selectedRecipeCategories.includes(category)"
            @click="toggleRecipeCategory(category)"
          >
            {{ drinkRecipeCategoryNames[category] }}
          </button>
        </div>
        <button class="btn" @click="chooseDrinkRecipe">帮我选一款</button>
      </section>

      <div v-if="recipeToast" class="success-tip">{{ recipeToast }}</div>
      <section v-for="category in drinkRecipeCategories" :key="category" class="card">
        <template v-if="visibleDrinkRecipes.some((recipe) => recipe.category === category)">
          <div class="section-title">{{ drinkRecipeCategoryNames[category] }}</div>
          <button
            v-for="recipe in visibleDrinkRecipes.filter((item) => item.category === category)"
            :key="recipe.id"
            class="recipe-choice"
            :class="{ 'recipe-choice-active': selectedRecipe?.id === recipe.id }"
            @click="selectedRecipe = recipe"
          >
            <span>{{ recipe.name }}</span>
            <span class="hint">{{ recipe.preparationMinutes }} 分钟 · {{ recipe.containsAlcohol ? '含酒精' : '无酒精' }}</span>
          </button>
        </template>
      </section>

      <section v-if="selectedRecipe" class="card drink-recipe-detail">
        <div class="recipe-kicker">一人份 · 约 {{ selectedRecipe.preparationMinutes }} 分钟</div>
        <div class="section-title">
          {{ selectedRecipe.name }}
          <span v-if="selectedRecipe.containsAlcohol" class="alcohol-badge">含酒精</span>
        </div>
        <div v-if="selectedRecipe.containsAlcohol" class="alcohol-warning">
          未成年人请勿饮酒，饮酒后请勿开车。
        </div>
        <div class="recipe-section-title">配料</div>
        <ul class="ingredient-list">
          <li v-for="ingredient in selectedRecipe.ingredients" :key="ingredient">{{ ingredient }}</li>
        </ul>
        <div class="recipe-section-title">制作步骤</div>
        <ol class="recipe-step-list">
          <li v-for="step in selectedRecipe.steps" :key="step">{{ step }}</li>
        </ol>
        <div v-if="selectedRecipe.tips" class="recipe-tip">{{ selectedRecipe.tips }}</div>
        <button
          class="btn"
          :disabled="favoriteRecipeIds.includes(selectedRecipe.id)"
          style="margin-top: 14px"
          @click="favoriteRecipe(selectedRecipe)"
        >
          {{ favoriteRecipeIds.includes(selectedRecipe.id) ? '已收藏' : '收藏这款配方' }}
        </button>
      </section>
    </template>

    <div v-if="showSeenProducts" class="modal-backdrop" role="presentation" @click.self="showSeenProducts = false">
      <section class="card seen-dialog" role="dialog" aria-modal="true" aria-labelledby="seen-drink-title">
        <div class="dialog-heading">
          <h3 id="seen-drink-title">本轮已看饮品</h3>
          <button class="dialog-close" aria-label="关闭" @click="showSeenProducts = false">×</button>
        </div>
        <p v-if="seenProducts.length === 0" class="hint">本轮还没有看过饮品。</p>
        <div v-for="product in seenProducts" :key="product.id" class="seen-row">
          <strong>{{ product.productName }} · {{ product.brand }}</strong>
          <span class="hint">{{ product.priceYuan }} 元 · {{ product.note }}</span>
        </div>
      </section>
    </div>
  </div>
</template>
