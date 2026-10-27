/**
 * 路由配置：三个主 Tab——吃什么 / 喝什么 / 我的。
 */
import { createRouter, createWebHistory } from 'vue-router'
import WhatToEatView from '../views/WhatToEatView.vue'
import WhatToDrinkView from '../views/WhatToDrinkView.vue'
import ProfileView from '../views/ProfileView.vue'
import RecipeDetailView from '../views/RecipeDetailView.vue'

// 创建路由实例：使用 history 模式
const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 吃什么：默认首页（出去吃 / 在家吃）
    { path: '/', name: 'eat', component: WhatToEatView },
    // 喝什么
    { path: '/drink', name: 'drink', component: WhatToDrinkView },
    // 我的：个人信息/收藏夹/家庭菜谱/家庭成员
    { path: '/profile', name: 'profile', component: ProfileView },
    // 菜谱详情：支持从收藏夹、家庭菜谱进入并返回来源页
    { path: '/recipes/:dishId', name: 'recipe-detail', component: RecipeDetailView },
  ],
})

export default router
