<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
    <div class="glass-card import-modal">
      <div class="im-header">
        <h3>B站视频导入</h3>
        <button class="im-close" @click="$emit('close')">&times;</button>
      </div>

      <div class="im-tabs">
        <button class="im-tab" :class="{ active: tab === 'link' }" @click="tab = 'link'">粘贴链接</button>
        <button class="im-tab" :class="{ active: tab === 'search' }" @click="tab = 'search'">搜索视频</button>
        <button class="im-tab" :class="{ active: tab === 'playlist' }" @click="tab = 'playlist'">合集导入</button>
      </div>

      <!-- Tab 1: Link paste -->
      <div v-if="tab === 'link'" class="im-body">
        <p class="im-hint">粘贴一个或多个B站视频链接，每行一个</p>
        <textarea v-model="linkText" class="im-textarea" rows="4" placeholder="https://www.bilibili.com/video/BV..."></textarea>
        <button class="im-btn primary" @click="parseLinks" :disabled="parsing">{{ parsing ? '解析中...' : '解析' }}</button>

        <div v-if="parsedVideos.length > 0" class="im-results">
          <div class="im-subtitle">解析结果 ({{ parsedVideos.length }} 个)</div>
          <div v-for="v in parsedVideos" :key="v.bvid" class="im-video-row">
            <div class="im-cover-wrap">
              <img v-if="v.coverUrl" :src="fixUrl(v.coverUrl)" class="im-cover" @error="onImgError" />
              <div v-else class="im-cover-placeholder"></div>
            </div>
            <div class="im-video-info">
              <div class="im-video-title">{{ v.title }}</div>
              <div class="im-video-meta">{{ v.bvid }} . {{ formatDuration(v.duration) }} . {{ v.authorName }}</div>
            </div>
          </div>
          <button class="im-btn primary" @click="doImport" :disabled="importing">
            {{ importing ? '导入中...' : '一键导入 ' + parsedVideos.length + ' 个视频' }}
          </button>
        </div>
      </div>

      <!-- Tab 2: Search -->
      <div v-if="tab === 'search'" class="im-body">
        <div class="im-search-row">
          <input v-model="searchKeyword" class="im-search-input" placeholder="输入关键词搜索B站视频..." @keydown.enter="doSearch" />
          <button class="im-btn primary" @click="doSearch" :disabled="searching">{{ searching ? '搜索中...' : '搜索' }}</button>
        </div>

        <div v-if="searchItems.length > 0" class="im-results">
          <div class="im-subtitle">搜索结果 . 共 {{ searchTotal }} 个</div>
          <div v-for="v in searchItems" :key="v.bvid" class="im-video-row">
            <input type="checkbox" :value="v.bvid" v-model="selectedBvids" class="im-checkbox" />
            <div class="im-cover-wrap">
              <img v-if="v.coverUrl" :src="fixUrl(v.coverUrl)" class="im-cover" @error="onImgError" />
              <div v-else class="im-cover-placeholder"></div>
            </div>
            <div class="im-video-info">
              <div class="im-video-title">{{ v.title }}</div>
              <div class="im-video-meta">{{ formatDuration(v.duration) }} . {{ formatPlayCount(v.playCount) }}播放 . {{ v.authorName }}</div>
            </div>
          </div>
          <button class="im-btn primary" @click="doImportSelected" :disabled="importing || selectedBvids.length === 0">
            {{ importing ? '导入中...' : '导入选中视频（' + selectedBvids.length + '）' }}
          </button>
        </div>
      </div>

      <!-- Tab 3: Playlist -->
      <div v-if="tab === 'playlist'" class="im-body">
        <p class="im-hint">粘贴B站合集/播放列表链接</p>
        <div class="im-search-row">
          <input v-model="playlistUrl" class="im-search-input" placeholder="https://space.bilibili.com/xxx/channel/seriesdetail?sid=xxx" @keydown.enter="parsePlaylist" />
          <button class="im-btn primary" @click="parsePlaylist" :disabled="parsingPlaylist">{{ parsingPlaylist ? '解析中...' : '解析' }}</button>
        </div>

        <div v-if="playlistItems.length > 0" class="im-results">
          <div class="im-subtitle">共 {{ playlistItems.length }} 个视频</div>
          <div v-for="v in playlistItems" :key="v.bvid" class="im-video-row">
            <input type="checkbox" :value="v.bvid" v-model="selectedBvids" class="im-checkbox" />
            <div class="im-cover-wrap">
              <img v-if="v.coverUrl" :src="fixUrl(v.coverUrl)" class="im-cover" @error="onImgError" />
              <div v-else class="im-cover-placeholder"></div>
            </div>
            <div class="im-video-info">
              <div class="im-video-title">{{ v.title }}</div>
              <div class="im-video-meta">{{ formatDuration(v.duration) }}</div>
            </div>
          </div>
          <div v-if="isPlaylistMode" class="im-course-name-row">
            <label class="im-label">课程名称（将作为合集名称创建一门新课）</label>
            <input v-model="playlistCourseName" class="im-course-input" placeholder="输入课程名称..." />
          </div>
          <button class="im-btn primary" @click="doImportSelected" :disabled="importing || selectedBvids.length === 0">
            {{ importing ? '导入中...' : '导入为课程 · ' + selectedBvids.length + ' 个视频' }}
          </button>
        </div>
      </div>

      <!-- Result summary -->
      <div v-if="importResults.length > 0" class="im-results im-import-summary">
        <div class="im-subtitle">导入完成</div>
        <div v-for="r in importResults" :key="r.bvid" class="im-result-row" :class="{ error: r.error }">
          <span class="im-result-icon">{{ r.error ? 'X' : 'OK' }}</span>
          <div class="im-video-info">
            <div>{{ r.lessonName || r.bvid }}</div>
            <div class="im-video-meta" v-if="!r.error">{{ r.subjectName }} / {{ r.unitName }}</div>
            <div class="im-video-meta err" v-else>{{ r.error }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { apiBilibiliParse, apiBilibiliSearch, apiBilibiliPlaylist, apiBilibiliImport, apiBilibiliImportPlaylist } from '../api'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close'])

const tab = ref('link')

// Tab 1
const linkText = ref('')
const parsing = ref(false)
const parsedVideos = ref([])

// Tab 2
const searchKeyword = ref('')
const searching = ref(false)
const searchItems = ref([])
const searchTotal = ref(0)

// Tab 3
const playlistUrl = ref('')
const parsingPlaylist = ref(false)
const playlistItems = ref([])
const playlistCourseName = ref('')
const isPlaylistMode = ref(false)

// Shared
const selectedBvids = ref([])
const importing = ref(false)
const importResults = ref([])

async function parseLinks() {
  const urls = linkText.value.split('\n').filter(l => l.trim())
  if (urls.length === 0) return
  parsing.value = true
  parsedVideos.value = []
  importResults.value = []
  selectedBvids.value = []

  const results = await Promise.allSettled(
    urls.map(url => apiBilibiliParse(url.trim()))
  )

  for (const r of results) {
    if (r.status === 'fulfilled' && r.value.data && r.value.data.bvid) {
      const v = r.value.data
      parsedVideos.value.push(v)
      selectedBvids.value.push(v.bvid)
    }
  }
  parsing.value = false
}

async function doSearch() {
  if (!searchKeyword.value.trim()) return
  searching.value = true
  searchItems.value = []
  importResults.value = []
  selectedBvids.value = []

  try {
    const res = await apiBilibiliSearch(searchKeyword.value.trim(), 1, 10)
    searchItems.value = res.data?.items || []
    searchTotal.value = res.data?.total || 0
    selectedBvids.value = searchItems.value.map(v => v.bvid)
  } catch (e) { /* ignore */ }
  searching.value = false
}

async function parsePlaylist() {
  if (!playlistUrl.value.trim()) return
  parsingPlaylist.value = true
  playlistItems.value = []
  importResults.value = []
  selectedBvids.value = []
  playlistCourseName.value = ''
  isPlaylistMode.value = false

  try {
    const res = await apiBilibiliPlaylist(playlistUrl.value.trim())
    playlistItems.value = res.data || []
    if (playlistItems.value.length > 0) {
      isPlaylistMode.value = true
      playlistCourseName.value = playlistItems.value[0].title || ''
      selectedBvids.value = playlistItems.value.map(v => v.bvid)
    }
  } catch (e) { /* ignore */ }
  parsingPlaylist.value = false
}

async function doImport() {
  if (selectedBvids.value.length === 0) return
  importing.value = true
  try {
    let res
    if (isPlaylistMode.value && playlistCourseName.value.trim()) {
      res = await apiBilibiliImportPlaylist(selectedBvids.value, playlistCourseName.value.trim(), true)
    } else {
      res = await apiBilibiliImport(selectedBvids.value, true)
    }
    importResults.value = res.data?.results || []
  } catch (e) { /* ignore */ }
  importing.value = false
}

async function doImportSelected() {
  await doImport()
}

function formatDuration(seconds) {
  if (!seconds) return '--:--'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

function formatPlayCount(n) {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

function fixUrl(url) {
  if (!url) return ''
  if (url.startsWith('//')) return 'https:' + url
  return url
}

function onImgError(e) {
  e.target.style.display = 'none'
}
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.6); z-index: 300;
  display: flex; align-items: center; justify-content: center;
}
.import-modal {
  width: 680px; max-height: 85vh; overflow-y: auto; padding: 0; border-radius: 14px;
}
.im-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid rgba(255,255,255,0.06);
}
.im-header h3 { font-size: 16px; font-weight: 600; margin: 0; color: #f1f5f9; }
.im-close { background: none; border: none; color: rgba(255,255,255,0.4); font-size: 20px; cursor: pointer; }
.im-tabs { display: flex; border-bottom: 1px solid rgba(255,255,255,0.06); padding: 0 20px; }
.im-tab {
  padding: 10px 16px; font-size: 12px; color: rgba(255,255,255,0.4);
  background: none; border: none; border-bottom: 2px solid transparent; cursor: pointer; font-family: inherit;
}
.im-tab.active { color: #60d9fa; border-bottom-color: #60d9fa; font-weight: 600; }
.im-body { padding: 20px; }
.im-hint { font-size: 11px; color: rgba(255,255,255,0.4); margin: 0 0 8px; }
.im-textarea {
  width: 100%; padding: 10px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.04); color: #e2e8f0; font-size: 12px; font-family: inherit;
  resize: vertical; margin-bottom: 10px; outline: none; box-sizing: border-box;
}
.im-btn {
  padding: 8px 20px; border-radius: 8px; border: none; font-size: 12px; cursor: pointer; font-family: inherit;
}
.im-btn.primary { background: linear-gradient(135deg,#3b82f6,#2563eb); color: #fff; }
.im-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.im-search-row { display: flex; gap: 8px; margin-bottom: 16px; }
.im-course-name-row { margin-bottom: 16px; }
.im-label { display: block; font-size: 11px; color: rgba(255,255,255,0.4); margin-bottom: 6px; }
.im-course-input {
  width: 100%; padding: 10px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.04); color: #e2e8f0; font-size: 13px; font-family: inherit;
  outline: none; box-sizing: border-box;
}
.im-course-input:focus { border-color: rgba(96,217,250,0.3); }
.im-search-input {
  flex: 1; padding: 10px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.04); color: #e2e8f0; font-size: 12px; font-family: inherit; outline: none;
}
.im-subtitle {
  font-size: 10px; text-transform: uppercase; letter-spacing: 1px;
  color: rgba(255,255,255,0.3); margin-bottom: 10px;
}
.im-results { margin-top: 16px; display: flex; flex-direction: column; gap: 8px; }
.im-video-row {
  display: flex; align-items: center; gap: 10px; padding: 8px;
  background: rgba(255,255,255,0.03); border-radius: 8px; border: 1px solid rgba(255,255,255,0.06);
}
.im-cover-wrap { width: 80px; min-width: 80px; height: 46px; border-radius: 4px; overflow: hidden; }
.im-cover { width: 100%; height: 100%; object-fit: cover; display: block; }
.im-cover-placeholder { width: 100%; height: 100%; background: rgba(255,255,255,0.05); }
.im-video-info { flex: 1; min-width: 0; }
.im-video-title { font-size: 12px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #f1f5f9; }
.im-video-meta { font-size: 10px; color: rgba(255,255,255,0.4); margin-top: 2px; }
.im-video-meta.err { color: #f87171; }
.im-checkbox { accent-color: #3b82f6; width: 16px; height: 16px; flex-shrink: 0; margin: 0; }
.im-import-summary { border-top: 1px solid rgba(255,255,255,0.06); padding-top: 12px; margin-top: 12px; }
.im-result-row { display: flex; align-items: center; gap: 8px; padding: 4px 0; }
.im-result-row.error { opacity: 0.6; }
.im-result-icon { font-size: 10px; width: 20px; text-align: center; color: rgba(255,255,255,0.5); flex-shrink: 0; }

.glass-card {
  background: rgba(8, 13, 31, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(16px);
}
</style>
