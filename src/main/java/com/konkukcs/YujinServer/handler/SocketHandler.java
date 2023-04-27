package com.konkukcs.YujinServer.handler;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Log4j2
public class SocketHandler extends TextWebSocketHandler {

    private static Map<WebSocketSession, String> sockets = new ConcurrentHashMap<>();
    private static Map<String, WebSocketSession> names = new ConcurrentHashMap<>();
    private static final String FILE_DIR = "/Users/yujin.iris/downloads";
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String remoteAddress = "";
        for (String s : session.getRemoteAddress().toString().split(":")) {
            remoteAddress += s;
            if (s.indexOf("]") == -1) {
                remoteAddress += ":";
            } else {
                break;
            }
        }
        String username = session.getUri().getQuery().split("=")[1];
        String code = remoteAddress + "_" + username;

        sockets.put(session, code);
        names.put(code, session);

        String list = "CLIENT_LIST$";
        for (String value : sockets.values()) {
            list += value.split("_")[1] + "$";
        }
        for (WebSocketSession sess : sockets.keySet()) {
            sess.sendMessage(new TextMessage(list));
        }
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
        String list = "CLIENT_LIST$";
        for (String value : sockets.values()) {
            list += value.split("_")[1] + "$";
        }
        for (WebSocketSession sess : sockets.keySet()) {
            sess.sendMessage(new TextMessage(list));
        }
    }
    public void emitFileList(Map<String, String> originalName,
                             String remoteAddr,
                             String username,
                             String _filename,
                             boolean flag) throws IOException {
        WebSocketSession sess = names.get("/" + "[" + remoteAddr + "]" + "_" + username);
        String code = sockets.get(sess).replace("[", "").replace("]", "");
        String dir = FILE_DIR + code;
        String data = "";
        List<String> fileList = Stream.of(new File(dir).listFiles())
                .filter(file -> !file.isDirectory())
                .map(file -> originalName.get(file.getName()))
                .filter(filename -> !filename.equals("null"))
                .collect(Collectors.toList());
        for (String s : fileList) {
            data += s + "$";
        }
        data = "FILE_LIST$" + data;
        sess.sendMessage(new TextMessage(data));
        if(flag) sess.sendMessage(new TextMessage(_filename + " : 업로드 완료"));
        else sess.sendMessage(new TextMessage(_filename + " : 삭제 완료"));
    }
}