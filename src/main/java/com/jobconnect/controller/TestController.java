package com.jobconnect.controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class TestController {
    @GetMapping("/api/test")
    public String testApi() {
        return "Xin chao! Backend JobConnect da chay tren Localhost thanh cong!";
    }
}
