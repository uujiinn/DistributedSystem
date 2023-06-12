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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        // const message = `SHARE_FILE:${selectedFile}:${selectedClient}:${username}`;
        // 저 형식에서 filename, sender, receiver
        // 파싱을 해서
        // 1. username/file 여기서 파일을 꺼내옵니다
        // 2. 이거를 receiver/file 로 저장
        // 새롭게 함수를 만듬
        // 기존 uploadFile을
        String messageText = message.getPayload();
        if (messageText.startsWith("SHARE_FILE:")) {
            String[] parts = messageText.split(":");
            if (parts.length == 4) {
                String sender = parts[1];
                String filename = parts[2];
                String receiver = parts[3];
                // Call a new function to handle file sharing
                handleFileSharing(sender, filename, receiver);
            }
        } else {
            for (WebSocketSession sess : sockets.keySet()) {
                sess.sendMessage(message);
            }
        }
    }

    private void handleFileSharing(String sender, String filename, String receiver) {
        String sourceFilePath = sender +"/"+filename;
        String receiverFolder = receiver+ "/"+filename; // Change this to the receiver's folder path in the S3 bucket
        String s3Key = receiverFolder;

        try {
            File file = new File(sourceFilePath);
            PutObjectRequest putObjectRequest = new PutObjectRequest(s3BucketName, s3Key, file);
            amazonS3.putObject(putObjectRequest);

            // Send a success message to the sender
            sendMessageToClient(sender, "File shared successfully with " + receiver);

            // Send a success message to the receiver
            sendMessageToClient(receiver, "You received a file from " + sender);
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
            list += value.split("_")[1] + "$";
        }
        for (WebSocketSession sess : sockets.keySet()) {
            sess.sendMessage(new TextMessage(list));
        }
    }

    public void emitFileList(Map<String, String> originalName,  //client+host
                             String remoteAddr,
                             String username,
                             String _filename,
                             boolean flag) throws IOException {
        WebSocketSession sess = names.get("/" + "[" + remoteAddr + "]" + "_" + username);
        String fullHost = remoteAddr + "_" + username;

        String directoryPath = fullHost;

        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(s3BucketName)
                .withPrefix(directoryPath);


        ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);
        List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();

        String data = "";
        String val, fn = null;
        List<String> fileList = new ArrayList<>();
        for (S3ObjectSummary objectSummary : objectSummaries) {
            val = objectSummary.getKey().split("/")[1];
            for (Map.Entry<String, String> entry : originalName.entrySet()) {
                if (entry.getValue().equals(val)) {
                    fn = entry.getKey();
                    break;
                }
            }
            if (fn != null) fileList.add(fn);
        }

        for (String s : fileList) {
            data += s + "$";
        }
        data = "FILE_LIST$" + data;
        sess.sendMessage(new TextMessage(data));
        if (flag) sess.sendMessage(new TextMessage(_filename + " : 업로드 완료"));
        else sess.sendMessage(new TextMessage(_filename + " : 삭제 완료"));
    }
}