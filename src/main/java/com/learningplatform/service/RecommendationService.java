package com.learningplatform.service;

import com.learningplatform.entity.AssessmentResult;
import com.learningplatform.entity.LearningMaterial;
import com.learningplatform.repository.AssessmentResultRepository;
import com.learningplatform.repository.LearningMaterialRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RecommendationService {

    private final AssessmentResultRepository assessmentResultRepository;
    private final LearningMaterialRepository learningMaterialRepository;

    public RecommendationService(
            AssessmentResultRepository assessmentResultRepository,
            LearningMaterialRepository learningMaterialRepository) {
        this.assessmentResultRepository = assessmentResultRepository;
        this.learningMaterialRepository = learningMaterialRepository;
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

    public List<Map<String, Object>> getRecommendations(String username) {

        List<AssessmentResult> results =
                assessmentResultRepository.findByUsername(username);

        Map<String, AssessmentResult> latestResults = new HashMap<>();

        for (AssessmentResult result : results) {
            String normalizedTopic = normalizeTopic(result.getTopic());

            AssessmentResult existing = latestResults.get(normalizedTopic);
            LocalDateTime resultDate = result.getAttemptedAt();

            if (existing == null
                    || (resultDate != null
                    && (existing.getAttemptedAt() == null
                    || resultDate.isAfter(existing.getAttemptedAt())))) {
                latestResults.put(normalizedTopic, result);
            }
        }

        List<LearningMaterial> allMaterials =
                learningMaterialRepository.findAll();

        List<Map<String, Object>> recommendations = new ArrayList<>();

        for (AssessmentResult result : latestResults.values()) {
            double percentage = result.getPercentage();

            String priority;
            String message;

            if (percentage < 60) {
                priority = "HIGH";
                message = "Revise this topic and take the quiz again.";
            } else if (percentage < 80) {
                priority = "MEDIUM";
                message = "Practice more questions to improve your score.";
            } else {
                priority = "LOW";
                message = "Good performance. Try advanced questions.";
            }

            String normalizedTopic = normalizeTopic(result.getTopic());

            List<LearningMaterial> matchingMaterials =
                    allMaterials.stream()
                            .filter(material ->
                                    normalizeTopic(material.getTopic())
                                            .equals(normalizedTopic))
                            .toList();

            Map<String, Object> recommendation = new HashMap<>();

            recommendation.put("topic", result.getTopic());
            recommendation.put("percentage", percentage);
            recommendation.put("priority", priority);
            recommendation.put("message", message);

            if (matchingMaterials.isEmpty()) {
                recommendation.put("materialAvailable", false);
            } else {
                LearningMaterial material = matchingMaterials.stream()
                        .max((a, b) -> {
                            Long idA = a.getId();
                            Long idB = b.getId();

                            if (idA == null && idB == null) {
                                return 0;
                            }
                            if (idA == null) {
                                return -1;
                            }
                            if (idB == null) {
                                return 1;
                            }
                            return idA.compareTo(idB);
                        })
                        .orElseThrow();

                recommendation.put("materialAvailable", true);
                recommendation.put("materialId", material.getId());
                recommendation.put("title", material.getTitle());
                recommendation.put("fileName", material.getFileName());
            }

            recommendations.add(recommendation);
        }

        recommendations.sort((a, b) -> {
            Number percentageA = (Number) a.get("percentage");
            Number percentageB = (Number) b.get("percentage");

            double scoreA = percentageA == null
                    ? Double.MAX_VALUE
                    : percentageA.doubleValue();

            double scoreB = percentageB == null
                    ? Double.MAX_VALUE
                    : percentageB.doubleValue();

            return Double.compare(scoreA, scoreB);
        });

        return recommendations;
    }
}