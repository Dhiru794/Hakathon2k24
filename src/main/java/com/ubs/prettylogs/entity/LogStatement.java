package com.ubs.prettylogs.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogStatement {
    private static int idCounter = 1;

    @JsonProperty("id")
    private final int id;

    @JsonProperty("timestamp")
    private final String timestamp;

    @JsonProperty("level")
    private final String level;

    @JsonProperty("logger")
    private final String logger;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("suggession")
    private final Suggestion suggestion;

    public LogStatement(String timestamp, String level, String logger, String message, Suggestion suggestion) {
        this.id = idCounter++;
        this.timestamp = timestamp;
        this.level = level;
        this.logger = logger;
        this.message = message;
        this.suggestion = suggestion;
    }

    // Nested Suggestion class
    public static class Suggestion {
        @JsonProperty("errorCode")
        private final String errorCode;

        @JsonProperty("goodLog")
        private final String goodLog;

        @JsonProperty("description")
        private final String description;

        @JsonProperty("regixFormat")
        private final String regexFormat;

        public Suggestion(String errorCode, String goodLog, String description, String regexFormat) {
            this.errorCode = errorCode;
            this.goodLog = goodLog;
            this.description = description;
            this.regexFormat = regexFormat;
        }
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getLevel() {
        return level;
    }

    public String getLogger() {
        return logger;
    }

    public String getMessage() {
        return message;
    }

    public Suggestion getSuggestion() {
        return suggestion;
    }
}
