package com.learningplatform.repository;

import com.learningplatform.entity.AssessmentResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, Long> {

    List<AssessmentResult> findByUsername(String username);

    List<AssessmentResult> findByUsernameAndTopic(String username, String topic);
}