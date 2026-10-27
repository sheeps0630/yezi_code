<!-- 应用根组件：页面内容 + 底部主菜单 Tab 栏 -->
<script setup lang="ts">
// 组合式 API：当前路由，用于高亮所在 Tab
import { computed } from 'vue'
import { useRoute } from 'vue-router'

// 当前路由对象
const route = useRoute()

// 底部 Tab 的名称与路由，按需求主菜单：吃什么 / 喝什么 / 我的
const tabs = [
  { name: '吃什么', path: '/' },
  { name: '喝什么', path: '/drink' },
  { name: '我的', path: '/profile' },
]

// 计算当前激活的 Tab 路径，用于高亮样式
const activePath = computed(() => route.path)
</script>

<template>
  <div class="app-shell">
    <!-- 页面主体：由路由渲染 -->
    <main class="page-body">
      <router-view />
    </main>
    <!-- 底部主菜单 Tab 栏 -->
    <nav class="tab-bar">
      <router-link
        v-for="tab in tabs"
        :key="tab.path"
        :to="tab.path"
        class="tab-item"
        :class="{ active: activePath === tab.path }"
      >
        {{ tab.name }}
      </router-link>
    </nav>
  </div>
</template>
