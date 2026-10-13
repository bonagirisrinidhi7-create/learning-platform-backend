package com.learningplatform.controller;

import com.learningplatform.dto.QuizQuestionResponse;
import com.learningplatform.entity.QuizQuestion;
import com.learningplatform.repository.QuizQuestionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;


@RestController
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:3000",
    "https://learning-platform-frontend-beta.vercel.app",
    "https://learning-platform-frontend-6bo1kg6xa-bonagirisrinidhi7-4236.vercel.app"
})
@RequestMapping("/quiz")

public class QuizQuestionController {

    private final QuizQuestionRepository quizQuestionRepository;

    public QuizQuestionController(
            QuizQuestionRepository quizQuestionRepository) {
        this.quizQuestionRepository = quizQuestionRepository;
    }

    private String normalizeTopic(String topic) {
        if (topic == null) {
            return "";
        }

        return topic.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("\\b([a-z]+)s\\b", "$1");
    }

    private QuizQuestionResponse toResponse(QuizQuestion question) {
        return new QuizQuestionResponse(
                question.getId(),
                question.getTopic(),
                question.getQuestion(),
                question.getOptionA(),
                question.getOptionB(),
                question.getOptionC(),
                question.getOptionD(),
                question.getDifficulty()
        );
    }

    @PostMapping("/add")
    public ResponseEntity<?> addQuestion(
            @RequestBody QuizQuestion question) {

        if (question.getTopic() == null ||
                question.getTopic().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Topic is required");
        }

        if (question.getQuestion() == null ||
                question.getQuestion().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Question is required");
        }

        if (question.getCorrectAnswer() == null ||
                !question.getCorrectAnswer().matches("(?i)[A-D]")) {
            return ResponseEntity.badRequest()
                    .body("Correct answer must be A, B, C or D");
        }

        question.setTopic(question.getTopic().trim());
        question.setCorrectAnswer(
                question.getCorrectAnswer().toUpperCase(Locale.ROOT));

        QuizQuestion saved = quizQuestionRepository.save(question);

        return ResponseEntity.ok(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<QuizQuestionResponse>> getAllQuestions() {
        List<QuizQuestionResponse> questions =
                quizQuestionRepository.findAll()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(questions);
    }

    @GetMapping("/topic/{topic}")
    public ResponseEntity<List<QuizQuestionResponse>> getQuestionsByTopic(
            @PathVariable String topic) {

        String normalized = normalizeTopic(topic);

        List<QuizQuestionResponse> questions =
                quizQuestionRepository.findAll()
                        .stream()
                        .filter(q -> normalizeTopic(q.getTopic()).equals(normalized))
                        .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                        .limit(5)
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(questions);
    }

    @GetMapping("/learner/topic/{topic}")
    public ResponseEntity<List<QuizQuestionResponse>> getLearnerQuestions(
            @PathVariable String topic) {

        return getQuestionsByTopic(topic);
    }

    @GetMapping("/material/{materialId}")
    public ResponseEntity<List<QuizQuestionResponse>> getQuestionsByMaterial(
            @PathVariable Long materialId) {

        List<QuizQuestionResponse> questions =
                quizQuestionRepository
                        .findTop5ByMaterialIdOrderByIdAsc(materialId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(questions);
    }
}
