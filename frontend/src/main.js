import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createRouter } from 'vue-router'
import App from './App.vue'
import router from './router/index'

// 字体导入（知域星图宇宙主题）
import '@fontsource/inter/400.css'
import '@fontsource/inter/600.css'
import '@fontsource/inter/700.css'
import '@fontsource/inter/800.css'

// Element Plus (业务页面用)
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 全局样式
import './styles/design-tokens.css'
import './styles/global.scss'
import './styles/markdown.css'

const pinia = createPinia()

const app = createApp(App)
app.config.warnHandler = () => {}

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 设置英文字体变量
document.documentElement.style.setProperty('--font-heading', '"Inter", sans-serif')

// 初始化背景
import { useBackgroundStore } from './stores/background'
app.mount('#app')

// 挂载后初始化背景
const bgStore = useBackgroundStore()
bgStore.init()
