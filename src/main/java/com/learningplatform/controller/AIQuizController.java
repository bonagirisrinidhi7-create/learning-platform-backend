package com.learningplatform.controller;

import com.learningplatform.service.QuizGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/ai")
public class AIQuizController {

    private final QuizGenerationService quizGenerationService;

    public AIQuizController(
            QuizGenerationService quizGenerationService) {

        this.quizGenerationService = quizGenerationService;
    }

    @PostMapping("/generate-quiz/{materialId}")
    public ResponseEntity<?> generateQuiz(
            @PathVariable Long materialId) {

        try {

            int count =
                    quizGenerationService.generateQuiz(materialId);

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "AI quiz generated successfully",
                            "questionsGenerated",
                            count
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(
                            Map.of(
                                    "message",
                                    "AI quiz generation failed",
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }
}