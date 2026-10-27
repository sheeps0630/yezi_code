/**
 * 前端应用入口：创建 Vue 应用、挂载路由与全局样式。
 */
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'

// 创建并挂载 Vue 应用到 index.html 的 #app 节点
createApp(App).use(router).mount('#app')
