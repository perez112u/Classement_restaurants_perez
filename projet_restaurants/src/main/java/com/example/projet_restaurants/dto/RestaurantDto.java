package com.example.projet_restaurants.dto;

import com.example.projet_restaurants.entity.RestaurantEntity;
import jakarta.validation.constraints.*;

public record RestaurantDto (@NotNull @Positive Long id, @NotBlank @Size(max = 90) String nom, @NotBlank @Size(max = 255) String adresse, String imageUrl, Double moyenne) {


    // moyenne non presente dans l'entité, oin la calcule depuis le service
    public static RestaurantDto buildFromEntity(RestaurantEntity restaurantEntity, String imageUrl, Double moyenne) {
        return  new RestaurantDto(restaurantEntity.getId(), restaurantEntity.getNom(), restaurantEntity.getAdresse(), imageUrl, moyenne);
    }
}
