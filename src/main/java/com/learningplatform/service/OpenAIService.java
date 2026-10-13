
package com.learningplatform.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    private final RestClient restClient;

    public OpenAIService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
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
                Generate exactly 5 multiple-choice questions based on the material.

                Topic: %s

                Follow this format exactly:
                QUESTION 1: Write the question here
                A: First option
                B: Second option
                C: Third option
                D: Fourth option
                Correct Answer: A
                Difficulty: EASY

                Continue until QUESTION 5.
                Use four distinct options for every question.
                Correct Answer must be A, B, C, or D.
                Difficulty must be EASY, MEDIUM, or HARD.
                Do not include an introduction or conclusion.

                LEARNING MATERIAL:
                %s
                """.formatted(topic, material);

        return generateResponse(prompt);
    }

    private String generateResponse(String prompt) {
        String apiKey = System.getenv("GROQ_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GROQ_API_KEY environment variable is missing.");
        }

        Map<?, ?> response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", "openai/gpt-oss-120b",
                        "messages", List.of(
                                Map.of(
                                        "role", "user",
                                        "content", prompt
                                )
                        ),
                        "temperature", 0.2,
                        "stream", false
                ))
                .retrieve()
                .body(Map.class);

        if (response == null
                || !(response.get("choices") instanceof List<?> choices)
                || choices.isEmpty()
                || !(choices.get(0) instanceof Map<?, ?> choice)
                || !(choice.get("message") instanceof Map<?, ?> message)
                || message.get("content") == null) {
            throw new IllegalStateException(
                    "Groq returned an empty or invalid response.");
        }

        return message.get("content").toString();
    }
}
