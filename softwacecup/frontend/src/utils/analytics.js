/**
 * 客户端埋点 SDK
 * 自动采集：页面停留、视频播放进度、答题行为、资源点击
 * 手动上报：trackEvent('event_type', { ...data })
 * 批量发送，debounce 2 秒
 */

const ANALYTICS_ENDPOINT = '/api/analytics/event'
const BATCH_INTERVAL = 2000 // 批量发送间隔 ms

// ---- state ----
const eventQueue = []
let flushTimer = null
let pageEntryTime = Date.now()
let currentPage = null
let videoWatchStart = null
let videoCurrentKp = null

// ---- public API ----

/** 手动上报事件 */
export function trackEvent(eventType, data = {}) {
  eventQueue.push({
    type: eventType,
    timestamp: Date.now(),
    page: currentPage || getPagePath(),
    ...data
  })
  scheduleFlush()
}

/** 页面进入（自动调用） */
export function trackPageEnter(pageName) {
  currentPage = pageName || getPagePath()
  pageEntryTime = Date.now()
  trackEvent('page_enter', { page: currentPage })
}

/** 页面离开（自动调用） */
export function trackPageLeave() {
  if (!currentPage) return
  const duration = Math.round((Date.now() - pageEntryTime) / 1000)
  trackEvent('page_leave', {
    page: currentPage,
    staySeconds: duration
  })
  currentPage = null
  pageEntryTime = 0
}

/** 视频播放开始 */
export function trackVideoStart(videoId, knowledgePointId) {
  videoWatchStart = Date.now()
  videoCurrentKp = knowledgePointId
  trackEvent('video_start', { videoId, knowledgePointId })
}

/** 视频播放进度（duration: 已观看秒数, total: 总时长秒数） */
export function trackVideoProgress(videoId, currentTime, totalDuration, knowledgePointId) {
  const percent = totalDuration > 0 ? Math.round((currentTime / totalDuration) * 100) : 0
  // 仅每10%上报一次
  if (percent % 10 === 0) {
    trackEvent('video_progress', {
      videoId,
      currentTime: Math.round(currentTime),
      totalDuration: Math.round(totalDuration),
      percent,
      knowledgePointId: knowledgePointId || videoCurrentKp
    })
  }
}

/** 视频播放结束 */
export function trackVideoEnd(videoId, knowledgePointId) {
  const watchDuration = videoWatchStart ? Math.round((Date.now() - videoWatchStart) / 1000) : 0
  trackEvent('video_end', {
    videoId,
    watchDuration,
    knowledgePointId: knowledgePointId || videoCurrentKp
  })
  videoWatchStart = null
  videoCurrentKp = null
}

/** 答题完成 */
export function trackQuizComplete(knowledgePointId, score, questionCount, correctCount) {
  trackEvent('quiz_complete', { knowledgePointId, score, questionCount, correctCount })
}

/** 资源点击 */
export function trackResourceClick(resourceId, resourceType, title) {
  trackEvent('resource_click', { resourceId, resourceType, title })
}

/** 立即刷新（页面卸载时调用） */
export function flushEvents() {
  if (eventQueue.length === 0) return
  const batch = [...eventQueue]
  eventQueue.length = 0
  clearTimeout(flushTimer)

  // 使用 sendBeacon 确保在页面卸载时也能发送
  const payload = JSON.stringify({ events: batch })
  if (navigator.sendBeacon) {
    navigator.sendBeacon(ANALYTICS_ENDPOINT, payload)
  } else {
    fetch(ANALYTICS_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: payload,
      keepalive: true
    }).catch(() => {})
  }
}

// ---- internal ----

function scheduleFlush() {
  if (flushTimer) return
  flushTimer = setTimeout(() => {
    flushEvents()
    flushTimer = null
  }, BATCH_INTERVAL)
}

function getPagePath() {
  try {
    return window.location.hash?.replace('#', '') || window.location.pathname || '/'
  } catch {
    return '/'
  }
}

// 页面卸载时刷新
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', flushEvents)
  window.addEventListener('pagehide', flushEvents)
  // 监听 hash 变化
  window.addEventListener('hashchange', () => {
    trackPageLeave()
    trackPageEnter()
  })
}
