package com.konkukcs.YujinServer.controller;

import com.konkukcs.YujinServer.handler.SocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ServerController {

    private final SocketHandler socketHandler;
    @GetMapping("/")
    public String clientHomePage(){
        return "index";
    }
}
