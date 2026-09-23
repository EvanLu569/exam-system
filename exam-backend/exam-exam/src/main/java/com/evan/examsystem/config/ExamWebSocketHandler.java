package com.evan.examsystem.config;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExamWebSocketHandler extends TextWebSocketHandler {

    // recordId -> (sessionId -> session)
    private static final Map<Long, Map<String, WebSocketSession>> recordSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从 URL 参数里拿 recordId
        Long recordId = getRecordId(session);
        if (recordId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        recordSessions.computeIfAbsent(recordId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        System.out.println("WebSocket 连接建立: recordId=" + recordId + ", sessionId=" + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equals(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long recordId = getRecordId(session);
        if (recordId != null && recordSessions.containsKey(recordId)) {
            recordSessions.get(recordId).remove(session.getId());
        }
        System.out.println("WebSocket 连接关闭: " + session.getId());
    }

    private Long getRecordId(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();  // "recordId=1&token=xxx"
            if (query == null) return null;
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "recordId".equals(kv[0])) {
                    return Long.valueOf(kv[1]);
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /** 向某场考试的所有连接广播 */
    public static void broadcast(Long recordId, String jsonMessage) {
        Map<String, WebSocketSession> sessions = recordSessions.get(recordId);
        if (sessions == null) return;
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonMessage));
                }
            } catch (Exception e) {
                System.err.println("推送失败: " + e.getMessage());
            }
        });
    }

    /** 关闭某场考试的所有连接 */
    public static void closeAll(Long recordId) {
        Map<String, WebSocketSession> sessions = recordSessions.remove(recordId);
        if (sessions == null) return;
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) session.close();
            } catch (Exception ignored) {}
        });
    }

    public static java.util.Set<Long> getActiveRecordIds() {
        return recordSessions.keySet();
    }

}