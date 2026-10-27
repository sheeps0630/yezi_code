<!-- 吃什么页：默认展示推荐美食，用户选择「出去吃 / 在家吃」后展示对应内容 -->
<script setup lang="ts">
// 组合式 API 所需的响应式工具
import { ref } from 'vue'
// 出去吃 / 在家吃两个子面板
import EatOutPanel from '../components/EatOutPanel.vue'
import CookAtHomePanel from '../components/CookAtHomePanel.vue'

// 当前子场景：out-出去吃 / home-在家做饭吃（默认展示出去吃推荐）
const eatMode = ref<'out' | 'home'>('out')
</script>

<template>
  <div>
    <!-- 页面标题 -->
    <h2 style="font-size: 20px; margin-bottom: 12px">今天吃什么？</h2>
    <!-- 场景切换：出去吃 / 在家吃 -->
    <div class="card">
      <span class="chip" :class="{ selected: eatMode === 'out' }" @click="eatMode = 'out'">出去吃</span>
      <span class="chip" :class="{ selected: eatMode === 'home' }" @click="eatMode = 'home'">在家做饭吃</span>
    </div>
    <!-- 对应场景的面板 -->
    <EatOutPanel v-if="eatMode === 'out'" />
    <CookAtHomePanel v-else />
  </div>
</template>
