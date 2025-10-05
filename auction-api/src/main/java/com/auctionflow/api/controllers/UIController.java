package com.auctionflow.api.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ui")
public class UIController {
    
    @GetMapping("/{path:[^\\.]*}")
    public String forwardToIndex() {
        return "forward:/ui/index.html";
    }
}