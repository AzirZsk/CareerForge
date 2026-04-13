// =====================================================
// LandIt Vite 配置
// @author Azir
// =====================================================
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';
export default defineConfig(function (_a) {
    var mode = _a.mode;
    var env = loadEnv(mode, process.cwd(), '');
    var apiTarget = env.VITE_API_TARGET || 'http://localhost:8080';
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
                    additionalData: "@use \"@/assets/styles/variables.scss\" as *;",
                    api: 'modern-compiler'
                }
            }
        },
        server: {
            proxy: {
                '/landit': {
                    target: apiTarget,
                    changeOrigin: true,
                    ws: true // 启用 WebSocket 代理
                }
            }
        }
    };
});
