package com.learningplatform.controller;

import com.learningplatform.service.OpenAIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/ai")
public class AIController {

    private final OpenAIService openAIService;

    public AIController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @GetMapping("/test")
    public ResponseEntity<?> testAI(
            @RequestParam String prompt) {

        try {
            String response = openAIService.testAI(prompt);

            return ResponseEntity.ok(
                    Map.of(
                            "message", "AI response received",
                            "response", response
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(
                            Map.of(
                                    "message", "AI request failed",
                                    "error", e.getMessage()
                            )
                    );
        }
    }
}