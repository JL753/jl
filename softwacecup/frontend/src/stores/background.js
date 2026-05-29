import { defineStore } from 'pinia'

const STORAGE_KEY = 'leleo_bg'

// 本地壁纸预设
// 静态壁纸：书房夜晚、安逸舒适、海洋女孩、浅色背景
// 动态壁纸：向往航天的女孩、尼尔：机械纪元 团队
const enc = (path) => path ? encodeURI(path) : ''

const WALLPAPERS = [
  { id: 'default', title: '星图默认', type: 'static', url: '', attribution: '知域' },
  { id: 'light-bg', title: '浅色背景', type: 'static', url: enc('/img/wallpaper/static/浅色背景/img.png'), preview: enc('/img/wallpaper/static/浅色背景/img.png'), attribution: '本地' },
  { id: 'study-night', title: '书房夜晚', type: 'static', url: enc('/img/wallpaper/static/书房夜晚/image.png'), preview: enc('/img/wallpaper/static/书房夜晚/image-pre.webp'), attribution: '本地' },
  { id: 'cozy', title: '安逸舒适', type: 'static', url: enc('/img/wallpaper/static/安逸舒适/image.png'), preview: enc('/img/wallpaper/static/安逸舒适/image-pre.webp'), attribution: '本地' },
  { id: 'ocean-girl', title: '海洋女孩', type: 'static', url: enc('/img/wallpaper/static/海洋女孩/image.png'), preview: enc('/img/wallpaper/static/海洋女孩/image-pre.webp'), attribution: '本地' },
  { id: 'aerospace-girl', title: '向往航天的女孩', type: 'dynamic', url: enc('/img/wallpaper/dynamic/向往航天的女孩/Toy-Aeroplane.webm'), preview: enc('/img/wallpaper/dynamic/向往航天的女孩/Toy-Aeroplane-pre.webm'), attribution: '本地' },
  { id: 'nier-team', title: '尼尔：机械纪元 团队', type: 'dynamic', url: enc('/img/wallpaper/dynamic/尼尔：机械纪元 团队/Nier-Automata-Team.webm'), preview: enc('/img/wallpaper/dynamic/尼尔：机械纪元 团队/Nier-Automata-Team-pre.webm'), attribution: '本地' },
]

const DEFAULT = WALLPAPERS[0]

export const useBackgroundStore = defineStore('background', {
  state: () => {
    const saved = (() => {
      try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') } catch { return null }
    })()
    return {
      current: saved || { ...DEFAULT },
      brightness: 85,
      blur: 5,
      textContrast: 30
    }
  },

  getters: {
    wallpapers: () => WALLPAPERS,
    isDynamic: (state) => {
      const wp = WALLPAPERS.find(w => w.id === state.current.id)
      return wp?.type === 'dynamic'
    }
  },

  actions: {
    setBackground(bg) {
      this.current = { id: bg.id, title: bg.title, url: bg.url, type: bg.type || 'static', preview: bg.preview || bg.url }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.current))
      this.applyToDOM()
    },

    setBrightness(val) {
      this.brightness = val
      // 动态背景也更新亮度（通过 CSS filter）
      this.applyToDOM()
    },

    setBlur(val) {
      this.blur = val
      this.applyToDOM()
    },

    setTextContrast(val) {
      this.textContrast = val
      this.applyToDOM()
    },

    applyToDOM() {
      const root = document.documentElement
      const wp = WALLPAPERS.find(w => w.id === this.current?.id)
      if (this.current?.id === 'default' || !wp) {
        root.style.setProperty('--leleo-bg-image', 'none')
        root.style.setProperty('--leleo-bg-type', 'none')
      } else if (wp.type === 'dynamic') {
        root.style.setProperty('--leleo-bg-image', `url("${wp.preview || wp.url}")`)
        root.style.setProperty('--leleo-bg-type', 'dynamic')
        root.style.setProperty('--leleo-bg-video', `url("${wp.url}")`)
      } else {
        root.style.setProperty('--leleo-bg-image', `url("${wp.url}")`)
        root.style.setProperty('--leleo-bg-type', 'static')
        root.style.setProperty('--leleo-bg-video', 'none')
      }
      root.style.setProperty('--leleo-brightness', `${this.brightness}%`)
      root.style.setProperty('--leleo-blur', `${this.blur}px`)
      root.style.setProperty('--leleo-text-contrast', `${this.textContrast / 100}`)
    },

    init() {
      this.applyToDOM()
    }
  }
})
