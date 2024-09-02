package com.ubs.prettylogs;

import com.ubs.prettylogs.config.LogRulesConfig;
import com.ubs.prettylogs.service.CodeAnalysisService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LogRulesConfig.class)
public class PrettylogsApplication {

	@Autowired
	private CodeAnalysisService codeAnalysisService;

	public static void main(String[] args) {
		SpringApplication.run(PrettylogsApplication.class, args);
	}

	@PostConstruct
	public void init() {
		codeAnalysisService.analyzeCode();
	}

}
