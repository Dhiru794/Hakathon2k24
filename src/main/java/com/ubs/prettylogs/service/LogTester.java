package com.ubs.prettylogs.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogTester {


    public void testLogs() {
        log.info("Info log is running!");
        log.debug("Debug log is running!");
        log.error("Error! {}", new Exception().getMessage());
        log.info("");
        log.info("Operation completed successfully, userId=123, requestId=abc123");
        log.error("test error");
        log.info("Operation completed successfully, userId=123");
    }
}
