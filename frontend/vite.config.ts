// Vite 构建配置
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 导出 Vite 配置：启用 Vue 单文件组件支持，并配置开发代理转发到后端
export default defineConfig({
  // Vue 3 单文件组件编译插件
  plugins: [vue()],
  // 开发服务器配置
  server: {
    // 本地开发端口
    port: 5173,
    // 后端接口代理：前端请求 /api 开头的路径时转发到 Spring Boot 服务
    proxy: {
      '/api': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
    },
  },
})
