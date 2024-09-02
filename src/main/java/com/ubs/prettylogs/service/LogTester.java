package com.ubs.prettylogs.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogTester {

    @PostConstruct
    public void testLogs(){
        log.info("Info log is running!");
        log.debug("Debug log is running!");
        log.error("Error! {}", new Exception().getMessage());
        log.info("");
    }
}
