
package com.learningplatform.controller;

import com.learningplatform.entity.AssessmentResult;
import com.learningplatform.entity.LearningMaterial;
import com.learningplatform.repository.AssessmentResultRepository;
import com.learningplatform.repository.LearningMaterialRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequestMapping("/recommendations")
public class RecommendationController {

    private final AssessmentResultRepository assessmentResultRepository;
    private final LearningMaterialRepository learningMaterialRepository;

    public RecommendationController(
            AssessmentResultRepository assessmentResultRepository,
            LearningMaterialRepository learningMaterialRepository) {
        this.assessmentResultRepository = assessmentResultRepository;
        this.learningMaterialRepository = learningMaterialRepository;
    }

    private String normalizeTopic(String topic) {
        if (topic == null) {
            return "";
        }

        String normalized = topic.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");

        normalized = normalized
                .replaceAll("\\boperating systems\\b", "operating system")
                .replaceAll("\\bdata structures\\b", "data structure")
                .replaceAll("\\balgorithms\\b", "algorithm");

        return normalized;
    }

    private boolean topicsMatch(String assessmentTopic, String materialTopic) {
        String assessment = normalizeTopic(assessmentTopic);
        String material = normalizeTopic(materialTopic);

        if (assessment.isEmpty() || material.isEmpty()) {
            return false;
        }

        if (assessment.equals(material)) {
            return true;
        }

        Set<String> assessmentWords = new HashSet<>(
                Arrays.asList(assessment.split(" "))
        );

        Set<String> materialWords = new HashSet<>(
                Arrays.asList(material.split(" "))
        );

        assessmentWords.removeIf(word ->
                word.equals("and") || word.equals("the") ||
                word.equals("of") || word.equals("in"));

        materialWords.removeIf(word ->
                word.equals("and") || word.equals("the") ||
                word.equals("of") || word.equals("in"));

        if (assessmentWords.isEmpty() || materialWords.isEmpty()) {
            return false;
        }

        Set<String> commonWords = new HashSet<>(assessmentWords);
        commonWords.retainAll(materialWords);

        return commonWords.size() >= 2 ||
                (assessmentWords.size() == 1 &&
                 materialWords.size() == 1 &&
                 commonWords.size() == 1);
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getRecommendations(
            @PathVariable String username) {

        List<AssessmentResult> results =
                assessmentResultRepository.findByUsername(username);

        if (results.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        Map<String, AssessmentResult> latestResults = new HashMap<>();

        for (AssessmentResult result : results) {
            String key = normalizeTopic(result.getTopic());

            AssessmentResult existing = latestResults.get(key);

            if (existing == null ||
                    (result.getAttemptedAt() != null &&
                     (existing.getAttemptedAt() == null ||
                      result.getAttemptedAt().isAfter(
                              existing.getAttemptedAt())))) {
                latestResults.put(key, result);
            }
        }

        List<LearningMaterial> allMaterials =
                learningMaterialRepository.findAll();

        List<Map<String, Object>> recommendations = new ArrayList<>();
        Set<Long> addedMaterialIds = new HashSet<>();

        for (AssessmentResult result : latestResults.values()) {
            if (!"NEEDS_IMPROVEMENT".equalsIgnoreCase(
                    result.getPerformance())) {
                continue;
            }

            for (LearningMaterial material : allMaterials) {
                if (!topicsMatch(result.getTopic(), material.getTopic())) {
                    continue;
                }

                if (!addedMaterialIds.add(material.getId())) {
                    continue;
                }

                Map<String, Object> recommendation = new HashMap<>();

                recommendation.put("topic", result.getTopic());
                recommendation.put("percentage", result.getPercentage());
                recommendation.put("performance", result.getPerformance());
                recommendation.put("materialId", material.getId());
                recommendation.put("title", material.getTitle());
                recommendation.put("fileName", material.getFileName());
                recommendation.put("fileType", material.getFileType());
                recommendation.put("materialAvailable", true);

                double percentage = result.getPercentage();

                recommendation.put(
                        "priority",
                        percentage < 40 ? "HIGH" :
                                percentage < 60 ? "MEDIUM" : "LOW"
                );

                recommendation.put(
                        "message",
                        percentage < 40
                                ? "Revise this topic carefully and take the quiz again."
                                : "Review this material and practise the topic again."
                );

                recommendations.add(recommendation);
            }
        }

        return ResponseEntity.ok(recommendations);
    }
}