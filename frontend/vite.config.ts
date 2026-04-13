// =====================================================
// CareerForge Vite 配置
// @author Azir
// =====================================================

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `@use "@/assets/styles/variables.scss" as *;`,
          api: 'modern-compiler'
        }
      }
    },
    server: {
      proxy: {
        '/careerforge': {
          target: apiTarget,
          changeOrigin: true,
          ws: true  // 启用 WebSocket 代理
        }
      }
    }
  }
})
