package com.example.projet_restaurants.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRestaurantDto (@NotBlank @Size(max = 90) String nom, @NotBlank @Size(max = 255) String adresse) {
}
