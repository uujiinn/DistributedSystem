package com.konkukcs.YujinServer.service;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.konkukcs.YujinServer.handler.SocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class FileService {

    private final SocketHandler socketHandler;

    private final Map<String, String> fileUUIDMap = new HashMap<>();
    private final AmazonS3 amazonS3; // AmazonS3 클라이언트

    @Value("${cloud.aws.s3.bucket}") // application.properties에서 설정한 S3 버킷 이름
    private String s3BucketName;

    public List<String> uploadFiles(String host, List<MultipartFile> files, String remoteAddr) throws IOException {
        List<String> s3Keys = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String filename = file.getOriginalFilename();
                String fileUUID = filename + String.valueOf(UUID.nameUUIDFromBytes(filename.getBytes()));
                fileUUIDMap.put(filename, filename);

                String fullHost = host;

                //client
                String s3KeyForClient = "client" + "/" + fullHost + "/" + filename;

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(file.getSize());

                // S3 client에 파일 업로드
                amazonS3.putObject(new PutObjectRequest(s3BucketName, s3KeyForClient, file.getInputStream(), metadata));

                //server
                String s3KeyForServer = "server" + "/" + fullHost + "/" + filename;

                ObjectMetadata metadataForServer = new ObjectMetadata();
                metadataForServer.setContentLength(file.getSize());

                // S3 server에 파일 업로드
                amazonS3.putObject(new PutObjectRequest(s3BucketName, s3KeyForServer, file.getInputStream(), metadataForServer));

                CompletableFuture.runAsync(() -> {
                    try {
                        socketHandler.emitFileList(remoteAddr, host, filename, true);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    s3Keys.add(s3KeyForClient);
                    s3Keys.add(s3KeyForServer);
                });
            }
        }
        return s3Keys;
    }

        public boolean deleteFiles (String remoteAddr, String host, String fileName) throws IOException {
            String fullHost = host;
            String fileUUID = fileUUIDMap.get(fileName);
            if (fileUUID != null) {
                String s3KeyForClient = "client" + "/" + fullHost + "/" + fileUUID;

                // S3에서 파일 삭제
                amazonS3.deleteObject(s3BucketName, s3KeyForClient);

                CompletableFuture.runAsync(() -> {
                    try {
                        socketHandler.emitFileList(remoteAddr, host, fileName, false);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                String s3KeyForServer = "server" + "/" + fullHost + "/" + fileUUID;
                amazonS3.deleteObject(s3BucketName, s3KeyForServer);

                fileUUIDMap.remove(fileName);

                return true;
            }

            return false;
        }

    }