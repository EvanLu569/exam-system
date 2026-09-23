// 考试 WebSocket 封装：服务端权威倒计时 + 心跳 + 断线重连
// 消息协议见接口文档 7.2 / 7.3
//  - 服务端推送：{type:'countdown', remainingSeconds} / {type:'exam_ended'} / {type:'force_submit', reason}
//  - 客户端心跳：发送 'ping'，服务端回复 'pong'

const WS_BASE = import.meta.env.VITE_WS_URL || `ws://${location.hostname}:8080`

export function createExamSocket({ paperId, token, onCountdown, onEnded, onForceSubmit, onStatus }) {
  let ws = null
  let closed = false
  let heartbeatTimer = null
  let reconnectTimer = null
  let attempts = 0
  const MAX_ATTEMPTS = 8

  function connect() {
    const url = `${WS_BASE}/ws/exam?examId=${paperId}&token=${encodeURIComponent(token)}`
    try {
      ws = new WebSocket(url)
    } catch {
      scheduleReconnect()
      return
    }

    ws.onopen = () => {
      attempts = 0
      startHeartbeat()
      onStatus?.('open')
    }
    ws.onmessage = (ev) => {
      if (ev.data === 'pong') return
      let msg
      try {
        msg = JSON.parse(ev.data)
      } catch {
        return
      }
      if (msg.type === 'countdown') onCountdown?.(msg.remainingSeconds)
      else if (msg.type === 'exam_ended') onEnded?.()
      else if (msg.type === 'force_submit') onForceSubmit?.(msg.reason)
    }
    ws.onclose = () => {
      stopHeartbeat()
      onStatus?.('closed')
      scheduleReconnect()
    }
    ws.onerror = () => ws.close()
  }

  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      if (ws && ws.readyState === WebSocket.OPEN) ws.send('ping')
    }, 10000)
  }

  function stopHeartbeat() {
    if (heartbeatTimer) clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }

  function scheduleReconnect() {
    if (closed || attempts >= MAX_ATTEMPTS) return
    attempts += 1
    onStatus?.('reconnecting')
    reconnectTimer = setTimeout(connect, Math.min(1000 * attempts, 8000))
  }

  connect()

  return {
    close() {
      closed = true
      stopHeartbeat()
      if (reconnectTimer) clearTimeout(reconnectTimer)
      if (ws) ws.close()
    },
    isOpen() {
      return !!(ws && ws.readyState === WebSocket.OPEN)
    }
  }
}
