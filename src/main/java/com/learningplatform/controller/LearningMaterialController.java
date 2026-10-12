
package com.learningplatform.controller;

import com.learningplatform.dto.MaterialResponse;
import com.learningplatform.entity.LearningMaterial;
import com.learningplatform.repository.LearningMaterialRepository;
import com.learningplatform.service.PdfTextExtractorService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/materials")
public class LearningMaterialController {

    private final LearningMaterialRepository materialRepository;
    private final PdfTextExtractorService pdfTextExtractorService;

    public LearningMaterialController(
            LearningMaterialRepository materialRepository,
            PdfTextExtractorService pdfTextExtractorService) {

        this.materialRepository = materialRepository;
        this.pdfTextExtractorService = pdfTextExtractorService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadMaterial(
            @RequestParam String title,
            @RequestParam String topic,
            @RequestParam MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("File is empty");
            }

            if (!"application/pdf".equalsIgnoreCase(
                    file.getContentType())) {

                return ResponseEntity.badRequest()
                        .body("Only PDF files are allowed");
            }

            LearningMaterial material = new LearningMaterial();

            material.setTitle(title);
            material.setTopic(topic);
            material.setFileName(file.getOriginalFilename());
            material.setFileType(file.getContentType());
            material.setFileData(file.getBytes());

            LearningMaterial savedMaterial =
                    materialRepository.save(material);

            return ResponseEntity.ok(savedMaterial.getId());

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("File upload failed");
        }
    }

    @GetMapping
    public ResponseEntity<List<MaterialResponse>> getAllMaterials() {

        List<MaterialResponse> materials =
                materialRepository.findAll()
                        .stream()
                        .map(material -> new MaterialResponse(
                                material.getId(),
                                material.getTitle(),
                                material.getTopic(),
                                material.getFileName(),
                                material.getFileType()
                        ))
                        .toList();

        return ResponseEntity.ok(materials);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMaterial(
            @PathVariable Long id) {

        LearningMaterial material =
                materialRepository.findById(id).orElse(null);

        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" +
                                material.getFileName() +
                                "\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                material.getFileType()
                        )
                )
                .body(material.getFileData());
    }

    @GetMapping("/{id}/text")
    public ResponseEntity<?> getPdfText(
            @PathVariable Long id) {

        LearningMaterial material =
                materialRepository.findById(id).orElse(null);

        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String text =
                    pdfTextExtractorService.extractText(
                            material.getFileData()
                    );

            return ResponseEntity.ok(text);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Unable to extract PDF text");
        }
    }

    @PutMapping("/{id}/topic")
    public ResponseEntity<?> updateMaterialTopic(
            @PathVariable Long id,
            @RequestParam String topic) {

        LearningMaterial material =
                materialRepository.findById(id).orElse(null);

        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        if (topic == null || topic.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Topic is required");
        }

        material.setTopic(topic.trim());

        LearningMaterial updated =
                materialRepository.save(material);

        return ResponseEntity.ok(
                new MaterialResponse(
                        updated.getId(),
                        updated.getTitle(),
                        updated.getTopic(),
                        updated.getFileName(),
                        updated.getFileType()
                )
        );
    }
}
