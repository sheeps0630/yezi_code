<!-- 出去吃面板：本地加权转盘与周边美食推荐 -->
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  addFavorite,
  listFavorites,
  listFoodCategories,
  listNearbyFoodTypes,
  recommendEatOut,
} from '../api/modules'
import FavoriteCollectionPanel from './FavoriteCollectionPanel.vue'
import type { FoodCategory, Restaurant } from '../api/types'

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

const storageKey = 'yechijia.food-wheel.v1'
const segmentColors = ['#76a96b', '#e3b56c', '#9c7257', '#d98968', '#8caeb0', '#c4a0b6', '#d1cb82', '#7f9eae']
const mode = ref<'custom' | 'nearby' | 'favorites'>('custom')
const foodCategories = ref<FoodCategory[]>([])
const nearbyFoodTypes = ref<string[]>([])
const wheelStore = ref<WheelStore | null>(null)
const recommendation = ref<Restaurant | null>(null)
const wheelSelection = ref('')
const seenRestaurants = ref<Restaurant[]>([])
const favoriteRestaurantIds = ref<string[]>([])
const preferredCategoryNames = ref<string[]>([])
const excludedCategoryNames = ref<string[]>([])
const showSeenRestaurants = ref(false)
const newEntryName = ref('')
const newEntryWeight = ref(1)
const newPresetName = ref('')
const wheelRotation = ref(0)
const spinning = ref(false)
const errorMessage = ref('')
const presetError = ref('')
const entryError = ref('')
const isBusy = ref(false)

const activePreset = computed(() =>
  wheelStore.value?.presets.find((preset) => preset.id === wheelStore.value?.activePresetId) ?? null,
)

const wheelSegments = computed<WheelSegment[]>(() => {
  const entries = activePreset.value?.entries ?? []
  if (entries.length === 0) return []
  const totalWeight = entries.reduce((sum, entry) => sum + Math.max(1, entry.weight), 0)
  let accumulatedWeight = 0

  return entries.map((entry, index) => {
    const startAngle = -90 + (accumulatedWeight / totalWeight) * 360
    accumulatedWeight += Math.max(1, entry.weight)
    const endAngle = -90 + (accumulatedWeight / totalWeight) * 360
    const middleAngle = ((startAngle + endAngle) / 2) * (Math.PI / 180)
    const labelRadius = entries.length > 8 ? 68 : 61
    const labelX = 120 + Math.cos(middleAngle) * labelRadius
    const labelY = 120 + Math.sin(middleAngle) * labelRadius
    const startX = 120 + 100 * Math.cos(startAngle * (Math.PI / 180))
    const startY = 120 + 100 * Math.sin(startAngle * (Math.PI / 180))
    const endX = 120 + 100 * Math.cos(endAngle * (Math.PI / 180))
    const endY = 120 + 100 * Math.sin(endAngle * (Math.PI / 180))
    const largeArc = endAngle - startAngle > 180 ? 1 : 0
    const path = entries.length === 1
      ? ''
      : `M 120 120 L ${startX} ${startY} A 100 100 0 ${largeArc} 1 ${endX} ${endY} Z`

    return {
      path,
      fill: segmentColors[index % segmentColors.length],
      label: entry.name,
      labelX,
      labelY,
    }
  })
})

/**
 * 持久化当前转盘方案到本机浏览器。
 */
function persistWheelStore(): void {
  if (!wheelStore.value) return
  try {
    window.localStorage.setItem(storageKey, JSON.stringify(wheelStore.value))
    presetError.value = ''
  } catch {
    presetError.value = '无法保存转盘方案，请检查浏览器存储空间或隐私设置'
  }
}

/**
 * 检查未知值是否为普通对象。
 *
 * @param value 待检查值
 * @returns 值是否为对象记录
 */
function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

/**
 * 检查本地转盘方案结构，避免损坏的数据进入页面状态。
 *
 * @param value 待校验的本地数据
 * @returns 数据结构有效时返回转盘方案
 */
function parseWheelStore(value: unknown): WheelStore | null {
  if (!isRecord(value) || typeof value.activePresetId !== 'string' || !Array.isArray(value.presets)) {
    return null
  }
  const presets: WheelPreset[] = []
  for (const rawPreset of value.presets) {
    if (
      !isRecord(rawPreset)
      || typeof rawPreset.id !== 'string'
      || typeof rawPreset.name !== 'string'
      || !rawPreset.name.trim()
      || !Array.isArray(rawPreset.entries)
      || rawPreset.entries.length === 0
    ) {
      return null
    }
    const entries: WheelEntry[] = []
    for (const rawEntry of rawPreset.entries) {
      if (
        !isRecord(rawEntry)
        || typeof rawEntry.id !== 'string'
        || typeof rawEntry.name !== 'string'
        || !rawEntry.name.trim()
        || typeof rawEntry.weight !== 'number'
        || !Number.isInteger(rawEntry.weight)
        || rawEntry.weight < 1
      ) {
        return null
      }
      entries.push({ id: rawEntry.id, name: rawEntry.name, weight: rawEntry.weight })
    }
    presets.push({ id: rawPreset.id, name: rawPreset.name, entries })
  }
  if (presets.length === 0 || !presets.some((preset) => preset.id === value.activePresetId)) return null
  return { activePresetId: value.activePresetId, presets }
}

/**
 * 加载附近推荐可选类型和本地保存的转盘方案。
 */
async function loadOptions(): Promise<void> {
  try {
    const [categories, nearbyTypes, favorites] = await Promise.all([
      listFoodCategories(),
      listNearbyFoodTypes(),
      listFavorites(),
    ])
    foodCategories.value = categories.sort((first, second) => first.name.localeCompare(second.name, 'zh-CN'))
    nearbyFoodTypes.value = nearbyTypes
    favoriteRestaurantIds.value = favorites
      .filter((favorite) => favorite.targetType === 'restaurant')
      .map((favorite) => favorite.targetId)
    const savedValue = window.localStorage.getItem(storageKey)
    if (savedValue !== null) {
      const parsed = parseWheelStore(JSON.parse(savedValue) as unknown)
      if (!parsed) {
        throw new Error('已保存的转盘方案格式异常，请清理浏览器缓存后重试')
      }
      wheelStore.value = parsed
      return
    }

    const initialPreset: WheelPreset = {
      id: `preset-${Date.now()}`,
      name: '我的美食转盘',
      entries: foodCategories.value.map((category) => ({
        id: category.id,
        name: category.name,
        weight: Math.max(1, category.weight),
      })),
    }
    if (initialPreset.entries.length === 0) {
      initialPreset.entries = [{ id: `entry-${Date.now()}`, name: '随便吃点', weight: 1 }]
    }
    wheelStore.value = { activePresetId: initialPreset.id, presets: [initialPreset] }
    persistWheelStore()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载转盘设置失败'
  }
}

/**
 * 添加一道自定义转盘选项。
 */
function addWheelEntry(): void {
  const name = newEntryName.value.trim()
  if (!name) {
    entryError.value = '请先填写想吃的内容'
    return
  }
  const preset = activePreset.value
  if (!preset || preset.entries.some((entry) => entry.name.toLowerCase() === name.toLowerCase())) {
    entryError.value = preset ? '这个选项已在转盘中' : '请先加载转盘方案'
    return
  }
  preset.entries.push({
    id: `entry-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    name,
    weight: Math.max(1, Math.floor(newEntryWeight.value || 1)),
  })
  wheelSelection.value = ''
  newEntryName.value = ''
  newEntryWeight.value = 1
  entryError.value = ''
  persistWheelStore()
}

/**
 * 保存选项权重的有效整数值。
 *
 * @param entry 被编辑的转盘选项
 */
function saveEntryWeight(entry: WheelEntry): void {
  entry.weight = Number.isFinite(entry.weight) ? Math.max(1, Math.floor(entry.weight)) : 1
  wheelSelection.value = ''
  persistWheelStore()
}

/**
 * 删除一个转盘选项，至少保留一个可抽选内容。
 *
 * @param entryId 选项标识
 */
function removeWheelEntry(entryId: string): void {
  const preset = activePreset.value
  if (!preset || preset.entries.length <= 1) {
    entryError.value = '转盘至少需要保留一个选项'
    return
  }
  preset.entries = preset.entries.filter((entry) => entry.id !== entryId)
  wheelSelection.value = ''
  entryError.value = ''
  persistWheelStore()
}

/**
 * 另存当前选项为一个新的转盘方案。
 */
function saveAsPreset(): void {
  const name = newPresetName.value.trim()
  const currentPreset = activePreset.value
  if (!name) {
    presetError.value = '请填写新转盘名称'
    return
  }
  if (!currentPreset || !wheelStore.value) {
    presetError.value = '转盘方案尚未加载'
    return
  }
  if (wheelStore.value.presets.some((preset) => preset.name.toLowerCase() === name.toLowerCase())) {
    presetError.value = '已有同名转盘，请换一个名称'
    return
  }
  const newPreset: WheelPreset = {
    id: `preset-${Date.now()}`,
    name,
    entries: currentPreset.entries.map((entry) => ({ ...entry, id: `entry-${Date.now()}-${entry.id}` })),
  }
  wheelStore.value.presets.push(newPreset)
  wheelStore.value.activePresetId = newPreset.id
  newPresetName.value = ''
  persistWheelStore()
}

/**
 * 切换当前使用的转盘方案。
 *
 * @param presetId 方案标识
 */
function selectPreset(presetId: string): void {
  if (!wheelStore.value?.presets.some((preset) => preset.id === presetId)) {
    presetError.value = '找不到这个转盘方案'
    return
  }
  wheelStore.value.activePresetId = presetId
  wheelSelection.value = ''
  persistWheelStore()
}

/**
 * 删除当前转盘方案，并切换到剩余方案。
 */
function deleteCurrentPreset(): void {
  if (!wheelStore.value || wheelStore.value.presets.length <= 1) {
    presetError.value = '至少保留一个转盘方案'
    return
  }
  const remainingPresets = wheelStore.value.presets.filter(
    (preset) => preset.id !== wheelStore.value?.activePresetId,
  )
  wheelStore.value.presets = remainingPresets
  wheelStore.value.activePresetId = remainingPresets[0].id
  wheelSelection.value = ''
  persistWheelStore()
}

/**
 * 根据权重抽取转盘结果并播放转动动画。
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
  const weightBeforeSelection = entries
    .slice(0, selectedIndex)
    .reduce((sum, entry) => sum + entry.weight, 0)
  const selectedCenter = ((weightBeforeSelection + entries[selectedIndex].weight / 2) / totalWeight) * 360
  wheelRotation.value += 1440 + ((360 - selectedCenter) % 360)
  spinning.value = true
  wheelSelection.value = ''
  window.setTimeout(() => {
    wheelSelection.value = entries[selectedIndex].name
    spinning.value = false
  }, 2100)
}

/**
 * 切换某个附近美食类型的心动或讨厌状态，两个集合互斥。
 *
 * @param categoryId 类型标识
 * @param preference 用户选择的偏好
 */
function toggleNearbyPreference(categoryName: string, preference: 'preferred' | 'excluded'): void {
  const selected = preference === 'preferred' ? preferredCategoryNames : excludedCategoryNames
  const opposite = preference === 'preferred' ? excludedCategoryNames : preferredCategoryNames
  const existingIndex = selected.value.indexOf(categoryName)
  if (existingIndex >= 0) {
    selected.value.splice(existingIndex, 1)
  } else {
    selected.value.push(categoryName)
    const oppositeIndex = opposite.value.indexOf(categoryName)
    if (oppositeIndex >= 0) opposite.value.splice(oppositeIndex, 1)
  }
}

/**
 * 推荐附近门店，心动类型优先，讨厌类型排除。
 */
async function recommendNearby(): Promise<void> {
  if (isBusy.value) return
  isBusy.value = true
  errorMessage.value = ''
  try {
    const result = await recommendEatOut({
      mode: 'nearby',
      latitude: 39.9,
      longitude: 116.4,
      excludedCategoryIds: [],
      preferredCategoryIds: [],
      excludedCategoryNames: excludedCategoryNames.value,
      preferredCategoryNames: preferredCategoryNames.value,
      excludedRestaurantIds: seenRestaurants.value.map((restaurant) => restaurant.id),
    })
    recommendation.value = result
    seenRestaurants.value.push(result)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '推荐失败，请稍后再试'
  } finally {
    isBusy.value = false
  }
}

/**
 * 收藏当前推荐门店。
 */
async function favoriteRestaurant(): Promise<void> {
  if (!recommendation.value || favoriteRestaurantIds.value.includes(recommendation.value.id)) return
  try {
    const favorite = await addFavorite(
      'restaurant',
      recommendation.value.id,
      `${recommendation.value.name} · ${recommendation.value.categoryName}`,
    )
    favoriteRestaurantIds.value.push(favorite.targetId)
    errorMessage.value = ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '收藏门店失败'
  }
}

/**
 * 加载页面所需选项。
 */
onMounted(loadOptions)
</script>

<template>
  <div>
    <div class="card">
      <div class="section-title">今天想怎么吃？</div>
      <div class="choice-row">
        <button class="chip" :class="{ selected: mode === 'custom' }" @click="mode = 'custom'">椰子美食转盘</button>
        <button class="chip" :class="{ selected: mode === 'nearby' }" @click="mode = 'nearby'">周边美食</button>
        <button class="chip" :class="{ selected: mode === 'favorites' }" @click="mode = 'favorites'">收藏</button>
      </div>
      <div class="hint">转盘内容和权重保存在当前浏览器；周边门店为演示示例数据。</div>
      <div v-if="errorMessage && mode === 'custom'" class="error-tip inline-error">{{ errorMessage }}</div>
    </div>

    <template v-if="mode === 'custom'">
      <div class="card">
        <div class="section-title">我的转盘</div>
        <div v-if="activePreset" class="preset-controls">
          <label class="field-label" for="wheel-preset">当前方案</label>
          <select
            id="wheel-preset"
            class="input preset-select"
            :value="activePreset.id"
            @change="selectPreset(($event.target as HTMLSelectElement).value)"
          >
            <option v-for="preset in wheelStore?.presets ?? []" :key="preset.id" :value="preset.id">
              {{ preset.name }}
            </option>
          </select>
          <button
            class="btn btn-danger"
            :disabled="(wheelStore?.presets.length ?? 0) <= 1"
            @click="deleteCurrentPreset"
          >
            删除方案
          </button>
        </div>
        <div class="form-row">
          <input
            v-model="newPresetName"
            class="input"
            placeholder="另存为新转盘，如：工作日"
            aria-label="新转盘名称"
            @keyup.enter="saveAsPreset"
          />
          <button class="btn btn-outline" @click="saveAsPreset">保存方案</button>
        </div>
        <div v-if="presetError" class="error-tip inline-error">{{ presetError }}</div>
        <div class="wheel-layout">
          <div class="wheel-wrap">
            <div class="wheel-pointer" aria-hidden="true"></div>
            <div class="wheel-disc" :class="{ spinning }" :style="{ transform: `rotate(${wheelRotation}deg)` }">
              <svg class="wheel-svg" viewBox="0 0 240 240" role="img" aria-label="按权重分区的美食转盘">
                <circle
                  v-if="wheelSegments.length === 1"
                  cx="120"
                  cy="120"
                  r="100"
                  :fill="wheelSegments[0].fill"
                />
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
            <span class="hint">{{ spinning ? '椰子正在认真挑选…' : '转一下，看看今天吃什么' }}</span>
            <strong v-if="wheelSelection">{{ wheelSelection }}</strong>
            <button class="btn wheel-spin-button" :disabled="spinning || !activePreset" @click="spinWheel">
              {{ spinning ? '转动中…' : '开始转盘' }}
            </button>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="section-title">转盘内容与权重</div>
        <p class="hint">权重越高，越容易转到；新内容默认权重为 1。</p>
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
              @change="saveEntryWeight(entry)"
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
            placeholder="填入想吃的内容，如：饺子"
            aria-label="转盘选项名称"
            @keyup.enter="addWheelEntry"
          />
          <input
            v-model.number="newEntryWeight"
            class="input weight-input"
            type="number"
            min="1"
            step="1"
            aria-label="新选项权重"
          />
          <button class="btn" @click="addWheelEntry">添加</button>
        </div>
        <div v-if="entryError" class="error-tip inline-error">{{ entryError }}</div>
      </div>
    </template>

    <FavoriteCollectionPanel v-else-if="mode === 'favorites'" :target-types="['restaurant']" />

    <template v-else>
      <div class="card">
        <div class="section-title">按口味挑选</div>
        <div class="hint">心动类型会优先推荐；讨厌类型会排除。两种选择互斥。</div>
        <div v-if="nearbyFoodTypes.length" class="taste-options">
          <div class="taste-row">
            <strong>喜欢</strong>
            <div class="taste-chips">
              <button
                v-for="type in nearbyFoodTypes"
                :key="`like-${type}`"
                class="preference-button"
                :class="{ preferred: preferredCategoryNames.includes(type) }"
                :aria-pressed="preferredCategoryNames.includes(type)"
                @click="toggleNearbyPreference(type, 'preferred')"
              >
                {{ type }}
              </button>
            </div>
          </div>
          <div class="taste-row">
            <strong>讨厌</strong>
            <div class="taste-chips">
              <button
                v-for="type in nearbyFoodTypes"
                :key="`hate-${type}`"
                class="preference-button"
                :class="{ excluded: excludedCategoryNames.includes(type) }"
                :aria-pressed="excludedCategoryNames.includes(type)"
                @click="toggleNearbyPreference(type, 'excluded')"
              >
                {{ type }}
              </button>
            </div>
          </div>
        </div>
        <div v-else class="hint">正在加载附近美食类型…</div>
        <div class="nearby-actions">
          <button class="btn" :disabled="isBusy" @click="recommendNearby">
            {{ isBusy ? '正在推荐…' : recommendation ? '换一家' : '给我推荐一家' }}
          </button>
          <button
            v-if="seenRestaurants.length"
            class="btn btn-outline"
            @click="showSeenRestaurants = true"
          >
            已看（{{ seenRestaurants.length }}）
          </button>
        </div>
      </div>
      <div v-if="errorMessage" class="error-tip inline-error">{{ errorMessage }}</div>
      <div v-if="recommendation" class="result-card">
        <div class="result-title">{{ recommendation.name }}</div>
        <div class="result-sub">
          {{ recommendation.categoryName }} · {{ (recommendation.distanceMeters / 1000).toFixed(1) }}km · 评分
          {{ recommendation.rating }}
          <br />
          {{ recommendation.address }}
        </div>
        <div class="result-sub result-footnote">
          * 演示门店数据；心动类型暂无可用门店时，会从其他未排除类型中推荐。
        </div>
        <button class="btn btn-outline result-favorite-button" :disabled="favoriteRestaurantIds.includes(recommendation.id)" @click="favoriteRestaurant">
          {{ favoriteRestaurantIds.includes(recommendation.id) ? '已收藏' : '收藏这家' }}
        </button>
      </div>
    </template>
    <div v-if="showSeenRestaurants" class="modal-backdrop" role="presentation" @click.self="showSeenRestaurants = false">
      <section class="card seen-dialog" role="dialog" aria-modal="true" aria-labelledby="seen-title">
        <div class="dialog-heading">
          <h3 id="seen-title">本轮已看门店</h3>
          <button class="dialog-close" aria-label="关闭" @click="showSeenRestaurants = false">×</button>
        </div>
        <p v-if="seenRestaurants.length === 0" class="hint">本轮还没有看过门店。</p>
        <div v-for="restaurant in seenRestaurants" :key="restaurant.id" class="seen-row">
          <strong>{{ restaurant.name }}</strong>
          <span class="hint">{{ restaurant.categoryName }} · {{ (restaurant.distanceMeters / 1000).toFixed(1) }}km · {{ restaurant.address }}</span>
        </div>
      </section>
    </div>
  </div>
</template>
