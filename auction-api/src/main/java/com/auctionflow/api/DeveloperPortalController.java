package com.auctionflow.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/developer-portal")
public class DeveloperPortalController {

    @GetMapping
    public String index() {
        return "developer-portal/index";
    }

    @GetMapping("/api-docs")
    public String apiDocs() {
        return "redirect:/swagger-ui/index.html";
    }

    @GetMapping("/rate-limits")
    public String rateLimits() {
        return "developer-portal/rate-limits";
    }

    @GetMapping("/webhooks")
    public String webhooks() {
        return "developer-portal/webhooks";
    }

    @GetMapping("/code-examples")
    public String codeExamples() {
        return "developer-portal/code-examples";
    }
}