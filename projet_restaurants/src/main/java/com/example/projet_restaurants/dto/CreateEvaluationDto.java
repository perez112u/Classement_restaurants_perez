package com.example.projet_restaurants.dto;

import jakarta.validation.constraints.*;

public record CreateEvaluationDto(@Size(max = 255) @NotBlank String commentaire, @NotNull @Min(0) @Max(3) Integer note) {
}
