package com.ubs.prettylogs.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "log")
public class LogRulesConfig {

    private List<Rule> rules;

    @Data
    public static class Rule {
        private String name;
        private String description;
        private boolean enabled;
        private String type;
        private List<String> requiredPatterns;
        private List<String> forbiddenPatterns;
        private List<String> allowedLevels;
        private Integer maxOccurrences;

    }

    @PostConstruct
    public void logConfig() {
        System.out.println("Loaded log rules:");
        if (rules != null) {
            rules.forEach(rule -> System.out.println(rule.getName() + ": " + rule.getDescription()));
        } else {
            System.out.println("No rules loaded");
        }
    }


}
