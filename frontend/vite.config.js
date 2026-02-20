// =====================================================
// LandIt Vite 配置
// @author Azir
// =====================================================
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    css: {
        preprocessorOptions: {
            scss: {
                additionalData: "@use \"@/assets/styles/variables.scss\" as *;",
                api: 'modern-compiler'
            }
        }
    }
});
