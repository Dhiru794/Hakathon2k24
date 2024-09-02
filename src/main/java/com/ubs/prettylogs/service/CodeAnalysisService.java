package com.ubs.prettylogs.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.utils.SourceRoot;
import com.ubs.prettylogs.config.LogRulesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CodeAnalysisService {

    private final JavaParser javaParser;
    private final LogRulesConfig logRulesConfig;

    @Autowired
    public CodeAnalysisService(LogRulesConfig logRulesConfig) {
        this.javaParser = new JavaParser();
        this.logRulesConfig = logRulesConfig;
    }

    public void analyzeCode() {
        try {
            SourceRoot sourceRoot = new SourceRoot(Paths.get("src/main/java/com/ubs/prettylogs"));
            List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse();

            for (ParseResult<CompilationUnit> parseResult : parseResults) {
                parseResult.ifSuccessful(cu -> {
                    System.out.println("Analyzing class: " + cu.getPrimaryTypeName().orElse("Unknown"));

                    // Extract and validate log statements
                    extractAndValidateLogStatements(cu);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void extractAndValidateLogStatements(CompilationUnit cu) {
        cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
            String methodName = methodCall.getNameAsString();
            if (isLogLevel(methodName)) {
                methodCall.getScope().ifPresent(scope -> {
                    String scopeName = scope.toString();
                    if (isLogger(scopeName)) {
                        String logMessage = methodCall.getArguments().get(0).toString();
                        System.out.println("Extracted log statement: " + scopeName + "." + methodName + "(" + logMessage + ") at line " + methodCall.getBegin().get().line);

                        // Validate the log statement using external rules
                        validateLogStatement(methodCall, logMessage);
                    }
                });
            }
        });
    }

    private void validateLogStatement(MethodCallExpr methodCall, String logMessage) {
        for (LogRulesConfig.Rule rule : logRulesConfig.getRules()) {
            if (rule.isEnabled()) {
                switch (rule.getType()) {
                    case "string" -> validateStringRule(rule, methodCall, logMessage);
                    case "level" -> validateLevelRule(rule, methodCall);
                    case "frequency" -> validateFrequencyRule(rule, methodCall);
                    case "exception" -> validateExceptionRule(rule, methodCall);
                }
            }
        }
    }

    private void validateStringRule(LogRulesConfig.Rule rule, MethodCallExpr methodCall, String logMessage) {
        // Validate required patterns
        if (rule.getRequiredPatterns() != null) {
            for (String pattern : rule.getRequiredPatterns()) {
                if (!logMessage.contains(pattern)) {
                    System.out.println("Validation failed for rule: " + rule.getName() + " at line " + methodCall.getBegin().get().line + ": Required pattern not found: " + pattern);
                }
            }
        }

        // Validate forbidden patterns
        if (rule.getForbiddenPatterns() != null) {
            for (String pattern : rule.getForbiddenPatterns()) {
                if (Pattern.matches(pattern, logMessage)) {
                    System.out.println("Validation failed for rule: " + rule.getName() + " at line " + methodCall.getBegin().get().line + ": Forbidden pattern found: " + pattern);
                }
            }
        }
    }

    private void validateLevelRule(LogRulesConfig.Rule rule, MethodCallExpr methodCall) {
        // Validate log levels
        if (rule.getAllowedLevels() != null && !rule.getAllowedLevels().contains(methodCall.getNameAsString())) {
            System.out.println("Validation failed for rule: " + rule.getName() + " at line " + methodCall.getBegin().get().line + ": Log level not allowed: " + methodCall.getNameAsString());
        }
    }

    private void validateFrequencyRule(LogRulesConfig.Rule rule, MethodCallExpr methodCall) {
        // Track frequency of log statements (example logic)
        // This requires additional implementation to count occurrences
        // and ensure they do not exceed the maximum allowed occurrences.
        // For simplicity, this part is left as a placeholder.
        System.out.println("Frequency Rule!!");
    }

    private void validateExceptionRule(LogRulesConfig.Rule rule, MethodCallExpr methodCall) {
        // Example logic to check if exceptions are logged with stack traces
        // This requires additional parsing to detect exception handling patterns.
        // For simplicity, this part is left as a placeholder.
        System.out.println("Exception Rule!!");
    }

    private boolean isLogLevel(String methodName) {
        return methodName.equals("info") || methodName.equals("debug") ||
                methodName.equals("warn") || methodName.equals("error") ||
                methodName.equals("trace") || methodName.equals("fatal");
    }

    private boolean isLogger(String scopeName) {
        return scopeName.equals("log") || scopeName.equals("logger");
    }
}
