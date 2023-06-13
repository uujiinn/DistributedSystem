package com.konkukcs.YujinServer.handler;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
@RequiredArgsConstructor
public class SocketHandler extends TextWebSocketHandler {

    private final AmazonS3 amazonS3; // AmazonS3 클라이언트
    @Value("${cloud.aws.s3.bucket}") // application.properties에서 설정한 S3 버킷 이름
    private String s3BucketName;
    private static Map<WebSocketSession, String> sockets = new ConcurrentHashMap<>();
    private static Map<String, WebSocketSession> names = new ConcurrentHashMap<>();

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
        String code = username;

        sockets.put(session, code);
        names.put(code, session);

        String list = "CLIENT_LIST$";
        for (String value : sockets.values()) {
            list += value + "$";
        }
        for (WebSocketSession sess : sockets.keySet()) {
            sess.sendMessage(new TextMessage(list));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // const message = `SHARE_FILE:${sender}:${selectedFile}:${selectedClient}`
        // 보내는사람 클라이언트에서 받는 사람 둘 다로
        String messageText = message.getPayload();
        if (messageText.startsWith("SHARE_FILE:")) {
            String[] parts = messageText.split(":");
            if (parts.length == 4) {
                String sender = parts[1];
                String filename = parts[2];
                String receiver = parts[3];

                handleFileSharing(sender, filename, receiver);
            }
        } else if (messageText.equals("REQ_EMIT")) {
            emitFileList("", sockets.get(session), null, false);
        } else {
            for (WebSocketSession sess : sockets.keySet()) {
                sess.sendMessage(message);
            }
        }
    }

    private void handleFileSharing(String sender, String filename, String receiver) {
        String sourceFilePath = "client" + "/" + sender + "/" + filename;
        String receiverFolder = "client" + "/" + receiver + "/" + filename;
        String receiverFolder2 = "server" + "/" + receiver + "/" + filename;

        try {
            // s3에서 파일 복사
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(s3BucketName, sourceFilePath, s3BucketName, receiverFolder);
            amazonS3.copyObject(copyObjectRequest);
            CopyObjectRequest copyObjectRequest2 = new CopyObjectRequest(s3BucketName, sourceFilePath, s3BucketName, receiverFolder2);
            amazonS3.copyObject(copyObjectRequest2);

            // Send a success message to the sender
            sendMessageToClient(sender,  filename + " shared successfully with " + receiver);

            // Send a success message to the receiver
            sendMessageToClient(receiver, "You received "+ filename + " from " + sender);
        } catch (AmazonS3Exception e) {
            // Handle exception if file upload fails
            e.printStackTrace();
            sendMessageToClient(sender, "Failed to share the file with " + receiver);
        }
    }

    private void sendMessageToClient(String username, String message) {
        WebSocketSession session = sockets.entrySet().stream()
                .filter(entry -> entry.getValue().equals(username))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                // Handle exception if sending message fails
                e.printStackTrace();
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sockets.remove(session);
        String list = "CLIENT_LIST$";
        for (String value : sockets.values()) {
            list += value + "$";
        }
        for (WebSocketSession sess : sockets.keySet()) {
            sess.sendMessage(new TextMessage(list));
        }
    }

    public void emitFileList(
            String remoteAddr,
            String username,
            String _filename,
            boolean flag) throws IOException {
        WebSocketSession sess = names.get(username);
        String fullHost = username;

        String directoryPath_client = "client" + "/" + fullHost;
        String directoryPath_server = "server" + "/" + fullHost;

        // client 파일 리스트 emit
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(s3BucketName)
                .withPrefix(directoryPath_client);


        ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);
        List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();

        String data = "";
        String val;
        List<String> fileList = new ArrayList<>();
        for (S3ObjectSummary objectSummary : objectSummaries) {
            val = objectSummary.getKey().split("/")[2];
            if (val != null) fileList.add(val);
        }

        for (String s : fileList) {
            data += s + "$";
        }
        data = "CLIENT_FILE_LIST$" + data;

        data += "SERVER_FILE_LIST$";

        // server 파일 리스트 emit
        ListObjectsRequest listObjectsRequest2 = new ListObjectsRequest()
                .withBucketName(s3BucketName)
                .withPrefix(directoryPath_server);


        ObjectListing objectListing2 = amazonS3.listObjects(listObjectsRequest2);
        List<S3ObjectSummary> objectSummaries2 = objectListing2.getObjectSummaries();

        String val2;
        List<String> fileList2 = new ArrayList<>();
        for (S3ObjectSummary objectSummary : objectSummaries2) {
            val2 = objectSummary.getKey().split("/")[2];
            if (val2 != null) fileList2.add(val2);
        }

        for (String s : fileList2) {
            data += s + "$";
        }

        sess.sendMessage(new TextMessage(data));

        if (_filename != null) {
            if (flag) sess.sendMessage(new TextMessage(_filename + " : 업로드 완료"));
            else sess.sendMessage(new TextMessage(_filename + " : 삭제 완료"));
        }
    }
}