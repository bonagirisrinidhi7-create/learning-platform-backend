package com.learningplatform.dto;

public class MaterialResponse {

    private Long id;
    private String title;
    private String topic;
    private String fileName;
    private String fileType;

    public MaterialResponse() {
    }

    public MaterialResponse(Long id, String title, String topic, String fileName, String fileType) {
        this.id = id;
        this.title = title;
        this.topic = topic;
        this.fileName = fileName;
        this.fileType = fileType;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTopic() {
        return topic;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }
}