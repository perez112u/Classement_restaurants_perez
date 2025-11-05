package com.example.projet_restaurants.dto;

import com.example.projet_restaurants.entity.EvaluationEntity;
import jakarta.validation.constraints.*;

import java.util.List;

public record EvaluationDto(@Positive @NotNull long id, @Size(max = 50) @NotBlank String evaluateur, @Size(max = 255) @NotBlank String commentaire, @Min(0) @Max(3) Integer note, List<String> photosUrls) {

    public static EvaluationDto fromEntity(EvaluationEntity entity, List<String> platsImagesUrls) {
        return new EvaluationDto(entity.getId(), entity.getEvaluateur(), entity.getCommentaire(), entity.getNote(), platsImagesUrls);
    }
}
