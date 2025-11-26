package com.mallowlink.auth.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/")
public class StatusController {

    @GetMapping("/status")
    public Mono<ResponseEntity<String>> getStatus() {
        log.info("Status endpoint called");
        return Mono.just(ResponseEntity.ok("Auth service is running"));
    }
}
