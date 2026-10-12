package com.learningplatform.repository;

import com.learningplatform.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByTopic(String topic);

    List<QuizQuestion> findTop5ByTopicOrderByIdAsc(String topic);

    List<QuizQuestion> findByMaterialIdOrderByIdAsc(Long materialId);

    List<QuizQuestion> findTop5ByMaterialIdOrderByIdAsc(Long materialId);

    List<QuizQuestion> findByMaterialIdAndTopicOrderByIdAsc(
            Long materialId, String topic);

    List<QuizQuestion> findTop5ByMaterialIdAndTopicOrderByIdAsc(
            Long materialId, String topic);
}
