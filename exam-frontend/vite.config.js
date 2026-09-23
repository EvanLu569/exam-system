import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 对接后端时，把 /api 转发到 Spring Boot 服务
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
