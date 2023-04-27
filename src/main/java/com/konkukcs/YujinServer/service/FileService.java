package com.konkukcs.YujinServer.service;


import com.konkukcs.YujinServer.handler.SocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class FileService {

    private final SocketHandler socketHandler;
    private static Map<String, String> pathMap = new ConcurrentHashMap<>();
    private static Map<String, String> originalFilename = new ConcurrentHashMap<>();
    private static Map<String, String> fileUUIDMap = new ConcurrentHashMap<>();
    private static final String FILE_DIR = "/Users/yujin.iris/downloads";

    public String uploadFile(String host, MultipartFile file, String remoteAddr) throws IOException {
        if (!file.isEmpty()) {

            String filename = file.getOriginalFilename();
            String fileUUID = UUID.randomUUID().toString() + "_" + filename;

            originalFilename.put(fileUUID, filename);
            fileUUIDMap.put(filename, fileUUID);


            String fullHost = remoteAddr + "_" + host;
            String dir = FILE_DIR + File.separator + fullHost;

            if (!pathMap.containsKey(dir)) {
                pathMap.put(dir, fullHost);
            }

            File folder = new File(dir);
            if (!folder.exists()) {
                folder.mkdir();
            }

            String fullPath = dir + File.separator + fileUUID;
            Path absPath = Paths.get(fullPath).toAbsolutePath();
            file.transferTo(absPath.toFile());

            CompletableFuture.runAsync(() -> {
                try {
                    socketHandler.emitFileList(originalFilename, remoteAddr, host, filename, true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            return fullPath;
        } else return null;
    }

    public boolean deleteFile(String remoteAddr, String host, String fileName) throws IOException {
        String fullHost = remoteAddr + "_" + host;
        String dir = FILE_DIR + File.separator + fullHost;
        String path = dir + File.separator + fileUUIDMap.get(fileName);
        System.out.println(path);

        Path absPath = Paths.get(path).toAbsolutePath();
        File target = absPath.toFile();

        System.out.println(absPath);
        if (target.exists()) {
            if (target.delete()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        socketHandler.emitFileList(originalFilename, remoteAddr, host, fileName, false);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                return true;
            }
        }
        return false;
    }
}