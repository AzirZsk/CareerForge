// =====================================================
// CareerForge 应用入口
// @author Azir
// =====================================================

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/styles/global.scss'

// Font Awesome 图标库
import { FontAwesomeIcon } from './plugins/fontawesome'

const app = createApp(App)
const pinia = createPinia()

// 注册全局组件
app.component('FontAwesomeIcon', FontAwesomeIcon)

app.use(pinia)
app.use(router)
app.mount('#app')
