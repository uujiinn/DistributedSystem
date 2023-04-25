package com.konkukcs.YujinServer.handler;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Component
@Log4j2
public class SocketHandler extends TextWebSocketHandler {

    private static Map<WebSocketSession,String> sockets = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String remoteAddress = session.getRemoteAddress().toString();
        String username = session.getUri().getQuery().split("=")[1];

        String code = remoteAddress + "$" + username;

        sockets.put(session, code);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        for (WebSocketSession sess : sockets.keySet()) {
            sess.sendMessage(message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sockets.remove(session);
        for (WebSocketSession webSocketSession : sockets.keySet()) {
            System.out.println(sockets.get(webSocketSession));
        }
    }
}