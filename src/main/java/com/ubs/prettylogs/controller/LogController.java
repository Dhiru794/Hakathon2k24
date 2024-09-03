package com.ubs.prettylogs.controller;

import com.ubs.prettylogs.service.CodeAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final CodeAnalysisService codeAnalysisService;

    @Autowired
    public LogController(CodeAnalysisService codeAnalysisService) {
        this.codeAnalysisService = codeAnalysisService;
    }

    @GetMapping("/analyze")
    public String analyzeAndReturnLogs() {
        // Analyze code and return the generated JSON logs
        return codeAnalysisService.analyzeCode();
    }
}
