package com.acme.training.acmehr.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return """
                AcmeHR Training
                Status: Application is running
                """;
    }

    @GetMapping("/protected")
    public String protectedPage() {
        return """
                AcmeHR Training
                Protected page placeholder
                Security is not enabled yet.
                """;
    }
}
