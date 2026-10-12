package com.learningplatform.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class OpenAIService {

    private final RestClient restClient;

    public OpenAIService() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    public String testAI(String prompt) {
        return generateResponse(prompt);
    }

    public String generateQuestions(String topic, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Learning material contains no readable text.");
        }

        String material = text.length() > 10000
                ? text.substring(0, 10000)
                : text;

        String prompt = """
                You are an exam question generator.
                Read the learning material and create exactly 5 multiple-choice questions.

                Topic: %s

                Follow this format EXACTLY for every question.
                Do not write an introduction, summary, outline, or conclusion.
                Do not use Markdown formatting.

                QUESTION 1: Write the question here
                A: First option
                B: Second option
                C: Third option
                D: Fourth option
                Correct Answer: A
                Difficulty: EASY

                QUESTION 2: Write the question here
                A: First option
                B: Second option
                C: Third option
                D: Fourth option
                Correct Answer: B
                Difficulty: MEDIUM

                Continue until QUESTION 5.
                Each correct answer must be exactly one letter: A, B, C, or D.
                Questions must be based on the provided material.
                Every question must have four distinct options.
                Use EASY, MEDIUM, or HARD for difficulty.

                LEARNING MATERIAL:
                %s
                """.formatted(topic, material);

        return generateResponse(prompt);
    }

    private String generateResponse(String prompt) {
        Map<?, ?> response = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", "llama3.1:8b",
                        "prompt", prompt,
                        "stream", false
                ))
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("response") == null) {
            throw new IllegalStateException(
                    "Ollama returned an empty response.");
        }

        return response.get("response").toString();
    }
}