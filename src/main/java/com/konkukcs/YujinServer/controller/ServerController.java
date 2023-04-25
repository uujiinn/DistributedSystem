package com.konkukcs.YujinServer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ServerController {

    @GetMapping("/")
    public String clientHomePage(){
        return "index";
    }
}
