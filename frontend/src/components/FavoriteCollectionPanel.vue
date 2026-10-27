<!-- 场景收藏列表：置顶项优先并按收藏时间排序，删除前二次确认 -->
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listFavorites, removeFavorite, setFavoritePinned } from '../api/modules'
import type { FavoriteItem } from '../api/types'

const props = defineProps<{
  targetTypes: string[]
}>()
const emit = defineEmits<{
  open: [favorite: FavoriteItem]
}>()
const items = ref<FavoriteItem[]>([])
const errorMessage = ref('')
const isLoading = ref(false)

/**
 * 加载当前场景类型的收藏。
 */
async function loadFavorites(): Promise<void> {
  isLoading.value = true
  try {
    const allFavorites = await listFavorites()
    items.value = allFavorites.filter((item) => props.targetTypes.includes(item.targetType))
    errorMessage.value = ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载收藏失败'
  } finally {
    isLoading.value = false
  }
}

/**
 * 切换一条收藏的置顶状态。
 *
 * @param item 收藏条目
 */
async function togglePinned(item: FavoriteItem): Promise<void> {
  try {
    await setFavoritePinned(item.id, !item.pinned)
    await loadFavorites()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '更新置顶状态失败'
  }
}

/**
 * 二次确认后删除收藏，并刷新列表。
 *
 * @param item 收藏条目
 */
async function confirmRemove(item: FavoriteItem): Promise<void> {
  if (!window.confirm(`确定删除收藏“${item.title}”吗？此操作无法撤销。`)) return
  try {
    await removeFavorite(item.id)
    await loadFavorites()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除收藏失败'
  }
}

/**
 * 以中文格式展示收藏时间。
 *
 * @param value ISO 时间文本
 * @returns 本地化时间
 */
function formatTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString('zh-CN', { dateStyle: 'short', timeStyle: 'short' })
}

onMounted(loadFavorites)
</script>

<template>
  <section class="card favorite-collection">
    <div class="section-title">我的收藏</div>
    <div v-if="errorMessage" class="error-tip inline-error">{{ errorMessage }}</div>
    <div v-if="isLoading" class="hint">正在加载收藏…</div>
    <div v-else-if="items.length === 0" class="hint">还没有收藏内容。推荐结果旁可以点“收藏”保存。</div>
    <div v-for="item in items" :key="item.id" class="favorite-row">
      <button
        class="favorite-open"
        :class="{ clickable: item.targetType === 'drink-recipe' }"
        type="button"
        @click="emit('open', item)"
      >
        <span class="favorite-title">
          <span v-if="item.pinned" class="favorite-pin-mark" aria-label="已置顶">置顶 · </span>
          {{ item.title }}
        </span>
        <span class="hint">
          {{ item.pinned && item.pinnedAt ? `置顶于 ${formatTime(item.pinnedAt)}` : `收藏于 ${formatTime(item.createdAt)}` }}
        </span>
      </button>
      <div class="favorite-actions">
        <button class="btn btn-outline compact-button" @click="togglePinned(item)">
          {{ item.pinned ? '取消置顶' : '置顶' }}
        </button>
        <button class="btn btn-danger compact-button" @click="confirmRemove(item)">删除</button>
      </div>
    </div>
  </section>
</template>
