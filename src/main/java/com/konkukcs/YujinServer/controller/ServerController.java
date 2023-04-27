package com.konkukcs.YujinServer.controller;

import com.konkukcs.YujinServer.handler.SocketHandler;
import com.konkukcs.YujinServer.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class ServerController {

    private final SocketHandler socketHandler;
    private final FileService fileService;

    @GetMapping("/")
    public String clientHomePage(){
        return "index";
    }

    @PostMapping("/file")
    public String uploadFile(HttpServletRequest request,
                             @RequestParam("input_file") MultipartFile file,
                             @RequestParam("client_name") String client_name ) throws IOException {

        fileService.uploadFile(client_name, file, request.getRemoteAddr());
        return "index";
    }

    @GetMapping("/deleteFile")
    @ResponseBody
    public String deleteFile(HttpServletRequest request,
                             @RequestParam("host") String host,
                             @RequestParam("filename") String fileName) throws IOException {
        System.out.println("OK, host=" + host +",fn =" + fileName);
        if (fileService.deleteFile(request.getRemoteAddr(), host, fileName)) {
            return "index";
        }
        return null;
    }
}
