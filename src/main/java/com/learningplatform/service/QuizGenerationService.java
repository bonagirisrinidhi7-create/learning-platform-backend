package com.learningplatform.service;

import com.learningplatform.entity.LearningMaterial;
import com.learningplatform.entity.QuizQuestion;
import com.learningplatform.repository.LearningMaterialRepository;
import com.learningplatform.repository.QuizQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuizGenerationService {

    private static final int MAX_QUESTIONS = 5;

    private final LearningMaterialRepository materialRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final OpenAIService openAIService;

    public QuizGenerationService(
            LearningMaterialRepository materialRepository,
            QuizQuestionRepository quizQuestionRepository,
            PdfTextExtractorService pdfTextExtractorService,
            OpenAIService openAIService) {
        this.materialRepository = materialRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.pdfTextExtractorService = pdfTextExtractorService;
        this.openAIService = openAIService;
    }

    @Transactional
    public int generateQuiz(Long materialId) throws Exception {
        LearningMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() ->
                        new Exception("Learning material not found: " + materialId));

        String topic = material.getTopic();

        if (topic == null || topic.isBlank()) {
            throw new Exception("Learning material has no topic.");
        }

        String text = pdfTextExtractorService.extractText(
                material.getFileData());

        if (text == null || text.isBlank()) {
            throw new Exception("No readable text found in the PDF.");
        }

        String aiResponse = openAIService.generateQuestions(topic, text);

        if (aiResponse == null || aiResponse.isBlank()) {
            throw new Exception("Ollama returned an empty response.");
        }

        System.out.println("AI RESPONSE:\n" + aiResponse);

        int count = saveQuestions(materialId, topic, aiResponse);

        if (count == 0) {
            throw new Exception(
                    "No questions could be parsed. Check the AI response in the backend terminal.");
        }

        return count;
    }

    private int saveQuestions(
            Long materialId,
            String topic,
            String aiResponse) {

        String response = aiResponse
                .replace("\r\n", "\n")
                .replace("**", "")
                .replace("__", "");

        Pattern questionPattern = Pattern.compile(
                "(?im)^\\s*(?:#{1,6}\\s*)?(?:QUESTION\\s*)?"
                        + "(?:Q\\s*)?\\d+\\s*[.):]\\s*(.+)$"
                        + "|^\\s*QUESTION\\s*\\d*\\s*:\\s*(.+)$"
                        + "|^\\s*Question\\s*:\\s*(.+)$");

        Matcher matcher = questionPattern.matcher(response);

        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        List<String> questionTexts = new ArrayList<>();

        while (matcher.find()) {
            String questionText = firstNonBlank(
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3));

            if (questionText != null && !questionText.isBlank()) {
                starts.add(matcher.start());
                ends.add(matcher.end());
                questionTexts.add(questionText);
            }
        }

        int count = 0;

        for (int i = 0;
             i < questionTexts.size() && count < MAX_QUESTIONS;
             i++) {

            int blockStart = ends.get(i);
            int blockEnd = i + 1 < starts.size()
                    ? starts.get(i + 1)
                    : response.length();

            String block = response.substring(blockStart, blockEnd);

            String optionA = findOption(block, 'A');
            String optionB = findOption(block, 'B');
            String optionC = findOption(block, 'C');
            String optionD = findOption(block, 'D');
            String answer = findAnswer(block);

            if (optionA == null || optionB == null
                    || optionC == null || optionD == null
                    || answer == null) {
                continue;
            }

            saveQuestion(
                    materialId,
                    topic,
                    questionTexts.get(i),
                    optionA,
                    optionB,
                    optionC,
                    optionD,
                    answer,
                    findDifficulty(block));

            count++;
        }

        if (count == 0) {
            count = saveUnnumberedQuestions(materialId, topic, response);
        }

        return count;
    }

    private int saveUnnumberedQuestions(
            Long materialId,
            String topic,
            String response) {

        Pattern pattern = Pattern.compile(
                "(?ims)^\\s*Question\\s*:\\s*(.+?)\\s*\\n"
                        + "(.*?)(?=^\\s*Question\\s*:|\\z)");

        Matcher matcher = pattern.matcher(response);
        int count = 0;

        while (matcher.find() && count < MAX_QUESTIONS) {
            String questionText = clean(matcher.group(1));
            String block = matcher.group(2);

            String optionA = findOption(block, 'A');
            String optionB = findOption(block, 'B');
            String optionC = findOption(block, 'C');
            String optionD = findOption(block, 'D');
            String answer = findAnswer(block);

            if (optionA == null || optionB == null
                    || optionC == null || optionD == null
                    || answer == null) {
                continue;
            }

            saveQuestion(
                    materialId,
                    topic,
                    questionText,
                    optionA,
                    optionB,
                    optionC,
                    optionD,
                    answer,
                    findDifficulty(block));

            count++;
        }

        return count;
    }

    private void saveQuestion(
            Long materialId,
            String topic,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String answer,
            String difficulty) {

        QuizQuestion question = new QuizQuestion();
        question.setMaterialId(materialId);
        question.setTopic(topic);
        question.setQuestion(clean(questionText));
        question.setOptionA(clean(optionA));
        question.setOptionB(clean(optionB));
        question.setOptionC(clean(optionC));
        question.setOptionD(clean(optionD));
        question.setCorrectAnswer(answer);
        question.setDifficulty(difficulty);

        quizQuestionRepository.save(question);
    }

    private String findOption(String block, char letter) {
        Pattern pattern = Pattern.compile(
                "(?im)^\\s*(?:[-*]\\s*)?"
                        + letter + "\\s*[).:]\\s*(.+?)\\s*$");

        Matcher matcher = pattern.matcher(block);
        return matcher.find() ? clean(matcher.group(1)) : null;
    }

    private String findAnswer(String block) {
        Pattern pattern = Pattern.compile(
                "(?im)^\\s*(?:Correct\\s*Answer|Answer)"
                        + "\\s*:\\s*\\*?\\s*([A-D])"
                        + "(?:\\s*[).:-].*)?\\s*\\*?\\s*$");

        Matcher matcher = pattern.matcher(block);

        return matcher.find()
                ? matcher.group(1).toUpperCase()
                : null;
    }

    private String findDifficulty(String block) {
        Pattern pattern = Pattern.compile(
                "(?im)^\\s*Difficulty\\s*:\\s*"
                        + "(EASY|MEDIUM|DIFFICULT|HARD)\\s*$");

        Matcher matcher = pattern.matcher(block);

        if (matcher.find()) {
            String difficulty = matcher.group(1).toUpperCase();
            return difficulty.equals("HARD") ? "DIFFICULT" : difficulty;
        }

        return "MEDIUM";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return null;
    }

    private String clean(String value) {
        return value == null
                ? ""
                : value.replace("**", "").replace("__", "").trim();
    }
}