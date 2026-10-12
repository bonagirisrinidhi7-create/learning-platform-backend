package com.learningplatform.dto;

public class QuizQuestionResponse {

    private Long id;
    private String topic;
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String difficulty;

    public QuizQuestionResponse() {
    }

    public QuizQuestionResponse(
            Long id,
            String topic,
            String question,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String difficulty) {

        this.id = id;
        this.topic = topic;
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.difficulty = difficulty;
    }

    public Long getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getQuestion() {
        return question;
    }

    public String getOptionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public String getDifficulty() {
        return difficulty;
    }
}
