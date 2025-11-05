package com.example.projet_restaurants.dto;

import jakarta.validation.constraints.*;

public record CreateRestaurantDto (@NotBlank @Size(max = 90) String nom, @NotBlank @Size(max = 255) String adresse) {


}
