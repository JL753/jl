export const openTutorSSE = ({ question, context = '', answerMode = '' }, handlers = {}) => {
  const token = localStorage.getItem('sp_token') || ''
  const params = new URLSearchParams({ question, context, answerMode })
  const url = `/api/tutor/stream?${params.toString()}`
  const controller = new AbortController()
  const timeoutMs = handlers.timeoutMs ?? 60000
  const timeoutId = setTimeout(() => controller.abort('SSE_TIMEOUT'), timeoutMs)

  const cleanup = () => clearTimeout(timeoutId)

  const promise = fetch(url, {
    method: 'GET',
    signal: controller.signal,
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'text/event-stream'
    }
  })
    .then(async (response) => {
      if (!response.ok || !response.body) {
        const text = await response.text().catch(() => '')
        throw new Error(text || 'SSE 连接失败')
      }
      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const events = buffer.split('\n\n')
        buffer = events.pop() || ''

        for (const raw of events) {
          const lines = raw.split('\n')
          let event = 'message'
          const data = []
          for (const line of lines) {
            if (line.startsWith('event:')) event = line.slice(6).trim()
            if (line.startsWith('data:')) data.push(line.slice(5).trim())
          }
          const payload = data.join('\n')
          if (event === 'delta' && handlers.onDelta) handlers.onDelta(payload)
          if (event === 'meta' && handlers.onMeta) handlers.onMeta(payload)
          if (event === 'done' && handlers.onDone) handlers.onDone(payload)
          if (event === 'error' && handlers.onError) handlers.onError(new Error(payload))
          if (event === 'citations' && handlers.onCitations) { try { handlers.onCitations(JSON.parse(payload)) } catch(e) { handlers.onCitations(payload) } }
        }
      }
      handlers.onClose && handlers.onClose()
    })
    .catch((err) => {
      if (controller.signal.aborted) {
        handlers.onAbort && handlers.onAbort(err)
        return
      }
      handlers.onError && handlers.onError(err)
    })
    .finally(cleanup)

  return {
    close(reason = 'MANUAL_CLOSE') {
      if (!controller.signal.aborted) controller.abort(reason)
    },
    promise
  }
}
