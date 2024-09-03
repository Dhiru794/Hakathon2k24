package com.ubs.prettylogs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.utils.SourceRoot;
import com.ubs.prettylogs.config.LogRulesConfig;
import com.ubs.prettylogs.entity.LogStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodeAnalysisService {

    private final JavaParser javaParser;
    private final LogRulesConfig logRulesConfig;

    private final Map<String, Integer> logFrequencyMap = new HashMap<>();
    private static final int MAX_FREQUENCY = 5; // Maximum allowed frequency for any log message


    @Autowired
    public CodeAnalysisService(LogRulesConfig logRulesConfig) {
        this.javaParser = new JavaParser();
        this.logRulesConfig = logRulesConfig;
    }

    public String analyzeCode() {
        List<LogStatement> logStatements = new ArrayList<>();
        try {
            SourceRoot sourceRoot = new SourceRoot(Paths.get("src/main/java/com/ubs/prettylogs"));
            List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse();

            for (ParseResult<CompilationUnit> parseResult : parseResults) {
                parseResult.ifSuccessful(cu -> {
                    System.out.println("Analyzing class: " + cu.getPrimaryTypeName().orElse("Unknown"));

                    // Extract and validate log statements
                    logStatements.addAll(extractAndValidateLogStatements(cu));
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (logStatements.isEmpty()) {
            return "no logs found!!";
        } else {
            // Convert the list of LogStatement objects into a single JSON array string
            return generateJsonArrayFromLogs(logStatements);
        }
    }



    public List<LogStatement> extractAndValidateLogStatements(CompilationUnit cu) {
        List<LogStatement> logStatements = new ArrayList<>();

        cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
            String methodName = methodCall.getNameAsString();
            if (isLogLevel(methodName)) {
                methodCall.getScope().ifPresent(scope -> {
                    String scopeName = scope.toString();
                    if (isLogger(scopeName)) {
                        String logMessage = extractLogMessage(methodCall);
                        int line = methodCall.getBegin().map(pos -> pos.line).orElse(-1);

                        System.out.println("Extracted log statement: " + scopeName + "." + methodName + "(" + logMessage + ") at line " + line);

                        // Validate the log statement using external rules
                        validateLogStatement(methodCall, logMessage, methodName, logStatements);
                    }
                });
            }
        });

        return logStatements;
    }

    private void validateLogStatement(MethodCallExpr methodCall, String logMessage, String logLevel, List<LogStatement> logStatements) {
        String errorCode = "default";
        String description = "";
        String regexFormat = ".{0-10}"; // Example regex format

        for (LogRulesConfig.Rule rule : logRulesConfig.getRules()) {
            if (rule.isEnabled() && rule.getAllowedLevels().contains(logLevel)) {
                switch (rule.getType()) {
                    case "string" -> {
                        if (!validateStringRule(rule, methodCall, logMessage)) {
                            errorCode = "warning";
                            description = "Log message doesn't match required patterns.";
                        }
                    }
                    case "level" -> {
                        if (!validateLevelRule(rule, methodCall)) {
                            errorCode = "danger";
                            description = "Log level is not allowed.";
                        }
                    }
                    case "frequency" -> {
                        if (!validateFrequencyRule(rule, methodCall)) {
                            errorCode = "danger";
                            description = "Log frequency exceeds the allowed limit.";
                        }
                    }
                    case "exception" -> {
                        if (!validateExceptionRule(rule, methodCall)) {
                            errorCode = "warning";
                            description = "Log should include an exception or error.";
                        }
                    }
                }
            }
        }

        // Add the suggestion object to the log statement
        LogStatement.Suggestion suggestion = new LogStatement.Suggestion(errorCode, logMessage, description, regexFormat);
        logStatements.add(createLogStatement(logMessage, logLevel, getLoggerName(methodCall), suggestion));
    }

    private boolean validateStringRule(LogRulesConfig.Rule rule, MethodCallExpr methodCall, String logMessage) {
        boolean isValid = true;

        // Validate required patterns
        if (rule.getRequiredPatterns() != null) {
            for (String pattern : rule.getRequiredPatterns()) {
                Pattern regexPattern = Pattern.compile(pattern);
                Matcher matcher = regexPattern.matcher(logMessage);

                if (!matcher.find()) {
                    System.out.println("Validation failed for rule: " + rule.getName() + " at line " + methodCall.getBegin().get().line + ": Required pattern not found: " + pattern);
                    isValid = false;
                }
            }
        }

        // Validate forbidden patterns
        if (rule.getForbiddenPatterns() != null) {
            for (String pattern : rule.getForbiddenPatterns()) {
                if (Pattern.matches(pattern, logMessage)) {
                    System.out.println("Validation failed for rule: " + rule.getName() + " at line " + methodCall.getBegin().get().line + ": Forbidden pattern found: " + pattern);
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    private boolean validateLevelRule(LogRulesConfig.Rule rule, MethodCallExpr methodCall) {
        boolean isValid = true;
        // Validate log levels
        if (rule.getAllowedLevels() != null && !rule.getAllowedLevels().contains(methodCall.getNameAsString())) {
            System.out.println("Validation failed for rule: " + rule.getName() + " at line " + methodCall.getBegin().get().line + ": Log level not allowed: " + methodCall.getNameAsString());
            isValid = false;
        }
        return isValid;
   }

    private boolean validateFrequencyRule(LogRulesConfig.Rule rule, MethodCallExpr methodCall) {
        boolean isValid = true;
        // Get the log message
        String logMessage = methodCall.getArguments().get(0).toString();

        // Use the rule name or a combination of rule attributes to create a unique key

        // Update the frequency count
        logFrequencyMap.put(logMessage, logFrequencyMap.getOrDefault(logMessage, 0) + 1);

        // Check if the frequency exceeds the maximum allowed frequency
        if (logFrequencyMap.get(logMessage) > MAX_FREQUENCY) {
            System.out.println("Validation failed for rule: " + rule.getName() + " at line " + methodCall.getBegin().get().line + ": Frequency limit exceeded for log message: " + logMessage);
            isValid = false;
         }
        return isValid;
    }

    private boolean validateExceptionRule(LogRulesConfig.Rule rule, MethodCallExpr methodCall) {
        boolean isValid = true;
        // Get the log message
        String logMessage = methodCall.getArguments().get(0).toString();

        // Define a regex pattern to look for both "Exception" and "Error"
        String exceptionPattern = ".*(Exception|Error).*"; // Matches any occurrence of "Exception" or "Error"

        // Check if the log message contains an exception or error
        if (logMessage.matches(exceptionPattern)) {
            System.out.println("Validation successful for rule: " + rule.getName() + " at line " + methodCall.getBegin().get().line + ": Exception or Error logged.");
        } else {
            System.out.println("Validation failed for rule: " + rule.getName() + " at line " + methodCall.getBegin().get().line + ": Expected Exception or Error not found in log message.");
            isValid = false;
        }
        return isValid;
    }

    private boolean isLogLevel(String methodName) {
        return methodName.equals("info") || methodName.equals("debug") ||
                methodName.equals("warn") || methodName.equals("error") ||
                methodName.equals("trace") || methodName.equals("fatal");
    }

    private boolean isLogger(String scopeName) {
        return scopeName.equals("log") || scopeName.equals("logger");
    }

    private String extractLogMessage(MethodCallExpr methodCall) {
        return methodCall.getArguments()
                .stream()
                .findFirst()
                .map(arg -> arg.toString())
                .orElse("");
    }

    private LogStatement createLogStatement(String logMessage, String logLevel, String loggerName, LogStatement.Suggestion suggestion) {
        String timestamp = getCurrentTimestamp();
        return new LogStatement(timestamp, logLevel, loggerName, logMessage, suggestion);
    }


    private String getCurrentTimestamp() {
        // Return the current timestamp in ISO-8601 format
        return Instant.now().toString();
    }

    private String determineLogLevel(String methodName) {
        // Example mapping of method names to log levels
        switch (methodName) {
            case "error":
                return "ERROR";
            case "warn":
                return "WARN";
            case "info":
                return "INFO";
            case "debug":
                return "DEBUG";
            default:
                return "TRACE"; // Default level
        }
    }
    private String generateJsonArrayFromLogs(List<LogStatement> logStatements) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (LogStatement logStatement : logStatements) {
                arrayNode.add(objectMapper.valueToTree(logStatement));
            }
            // Convert the ArrayNode to a JSON array string
            return objectMapper.writeValueAsString(arrayNode);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating JSON array";
        }
    }
    private String getLoggerName(MethodCallExpr methodCall) {
        return methodCall.getScope().map(Object::toString).orElse("UnknownLogger");
    }

}
