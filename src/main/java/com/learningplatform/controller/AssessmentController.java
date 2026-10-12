package com.learningplatform.controller;

import com.learningplatform.dto.AssessmentRequest;
import com.learningplatform.entity.AssessmentResult;
import com.learningplatform.entity.QuizQuestion;
import com.learningplatform.repository.AssessmentResultRepository;
import com.learningplatform.repository.QuizQuestionRepository;
import com.learningplatform.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequestMapping("/assessment")
public class AssessmentController {

    private final AssessmentResultRepository assessmentResultRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final RecommendationService recommendationService;

    public AssessmentController(
            AssessmentResultRepository assessmentResultRepository,
            QuizQuestionRepository quizQuestionRepository,
            RecommendationService recommendationService) {
        this.assessmentResultRepository = assessmentResultRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.recommendationService = recommendationService;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitAssessment(
            @RequestBody AssessmentRequest request) {

        if (request.getUsername() == null
                || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("Username is required");
        }

        if (request.getMaterialId() == null
                || request.getMaterialId() <= 0) {
            return ResponseEntity.badRequest().body("Valid materialId is required");
        }

        if (request.getTopic() == null || request.getTopic().isBlank()) {
            return ResponseEntity.badRequest().body("Topic is required");
        }

        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            return ResponseEntity.badRequest().body("Answers are required");
        }

        List<QuizQuestion> questions =
                quizQuestionRepository.findTop5ByMaterialIdOrderByIdAsc(
                        request.getMaterialId());

        if (questions.isEmpty()) {
            return ResponseEntity.status(404)
                    .body("No quiz questions found for this material. Generate its quiz first.");
        }

        int correctAnswers = 0;

        for (QuizQuestion question : questions) {
            String selectedAnswer =
                    request.getAnswers().get(question.getId());

            if (selectedAnswer != null
                    && selectedAnswer.equalsIgnoreCase(
                            question.getCorrectAnswer())) {
                correctAnswers++;
            }
        }

        int totalQuestions = questions.size();

        double percentage = Math.round(
                (double) correctAnswers * 10000 / totalQuestions) / 100.0;

        String performance = percentage >= 80
                ? "STRONG"
                : percentage >= 60
                ? "MODERATE"
                : "NEEDS_IMPROVEMENT";

        AssessmentResult result = new AssessmentResult();
        result.setUsername(request.getUsername());
        result.setTopic(request.getTopic());
        result.setTotalQuestions(totalQuestions);
        result.setCorrectAnswers(correctAnswers);
        result.setPercentage(percentage);
        result.setPerformance(performance);
        result.setAttemptedAt(LocalDateTime.now());

        AssessmentResult savedResult =
                assessmentResultRepository.save(result);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Assessment submitted successfully");
        response.put("resultId", savedResult.getId());
        response.put("username", savedResult.getUsername());
        response.put("materialId", request.getMaterialId());
        response.put("topic", savedResult.getTopic());
        response.put("totalQuestions", totalQuestions);
        response.put("correctAnswers", correctAnswers);
        response.put("percentage", percentage);
        response.put("performance", performance);
        response.put("attemptedAt", savedResult.getAttemptedAt());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/performance/{username}")
    public ResponseEntity<?> getPerformance(
            @PathVariable String username) {

        List<AssessmentResult> results =
                assessmentResultRepository.findByUsername(username);

        if (results.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        return ResponseEntity.ok(results);
    }

    @GetMapping("/competency/{username}")
    public ResponseEntity<?> getCompetency(
            @PathVariable String username) {

        List<AssessmentResult> results =
                assessmentResultRepository.findByUsername(username);

        Map<String, AssessmentResult> latestResults = new HashMap<>();

        for (AssessmentResult result : results) {
            AssessmentResult existing = latestResults.get(result.getTopic());

            if (existing == null
                    || (result.getAttemptedAt() != null
                    && (existing.getAttemptedAt() == null
                    || result.getAttemptedAt().isAfter(
                            existing.getAttemptedAt())))) {
                latestResults.put(result.getTopic(), result);
            }
        }

        List<Map<String, Object>> competency = new ArrayList<>();

        for (AssessmentResult result : latestResults.values()) {
            Map<String, Object> data = new HashMap<>();
            data.put("topic", result.getTopic());
            data.put("percentage", result.getPercentage());
            data.put("performance", result.getPerformance());
            competency.add(data);
        }

        return ResponseEntity.ok(competency);
    }

    @GetMapping("/improvement/{username}/{topic}")
    public ResponseEntity<?> getImprovement(
            @PathVariable String username,
            @PathVariable String topic) {

        List<AssessmentResult> results =
                assessmentResultRepository.findByUsernameAndTopic(
                        username, topic);

        if (results.size() < 2) {
            return ResponseEntity.ok(
                    "At least two attempts are required to calculate improvement");
        }

        results.sort((a, b) -> {
            LocalDateTime dateA = a.getAttemptedAt();
            LocalDateTime dateB = b.getAttemptedAt();

            if (dateA == null && dateB == null) {
                return 0;
            }
            if (dateA == null) {
                return 1;
            }
            if (dateB == null) {
                return -1;
            }
            return dateA.compareTo(dateB);
        });

        AssessmentResult previous = results.get(results.size() - 2);
        AssessmentResult latest = results.get(results.size() - 1);

        double improvement = Math.round(
                (latest.getPercentage() - previous.getPercentage()) * 100.0)
                / 100.0;

        String status = improvement > 0
                ? "IMPROVED"
                : improvement < 0
                ? "DECLINED"
                : "NO_CHANGE";

        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("topic", topic);
        response.put("previousScore", previous.getPercentage());
        response.put("latestScore", latest.getPercentage());
        response.put("improvement", improvement);
        response.put("status", status);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendations/{username}")
    public ResponseEntity<?> getRecommendations(
            @PathVariable String username) {

        return ResponseEntity.ok(
                recommendationService.getRecommendations(username));
    }
}