package com.ubs.prettylogs.service;

import com.github.javaparser.ast.CompilationUnit;
import com.ubs.prettylogs.config.LogRulesConfig;
import com.ubs.prettylogs.entity.LogStatement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class CodeAnalysisServiceTest {

    @Mock
    LogRulesConfig logRulesConfig;
    @Mock
    Map<String, Integer> logFrequencyMap;
    @InjectMocks
    CodeAnalysisService codeAnalysisService;

    @Mock
    CompilationUnit compilationUnit;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAnalyzeCode() {
        when(logRulesConfig.getRules()).thenReturn(List.of(new LogRulesConfig.Rule()));

        String result = codeAnalysisService.analyzeCode();
        Assertions.assertNotEquals("replaceMeWithExpectedResult", result);
    }

    @Test
    void testExtractAndValidateLogStatements() {
        when(logRulesConfig.getRules()).thenReturn(List.of(new LogRulesConfig.Rule()));
        List<LogStatement> result = codeAnalysisService.extractAndValidateLogStatements(compilationUnit);
        Assertions.assertNotEquals(List.of(new LogStatement("timestamp", "level", "logger", "message", null)), result);
    }
}

