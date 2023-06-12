package com.konkukcs.YujinServer.controller;

import com.konkukcs.YujinServer.handler.SocketHandler;
import com.konkukcs.YujinServer.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


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
                             @RequestParam("input_file") List<MultipartFile> files,
                             @RequestParam("client_name") String clientName,
                             Model model) throws IOException {
        List<String> s3Keys = fileService.uploadFiles(clientName, files, request.getRemoteAddr());
        model.addAttribute("s3Keys", s3Keys);
        return "index";
    }

    @GetMapping("/deleteFile")
    @ResponseBody
    public String deleteFile(HttpServletRequest request,
                             @RequestParam("host") String host,
                             @RequestParam("filename") String fileName) throws IOException {
        if (fileService.deleteFiles(request.getRemoteAddr(), host, fileName)) {
            return "File deleted successfully.";
        }
        return "File deletion failed.";
    }
}