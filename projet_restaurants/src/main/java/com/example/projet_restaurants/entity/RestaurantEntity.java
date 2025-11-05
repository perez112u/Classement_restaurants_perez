package com.example.projet_restaurants.entity;

import com.example.projet_restaurants.dto.CreateRestaurantDto;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity(name= "restaurant")
@Data
public class RestaurantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name= "nom", length = 90, nullable = false)
    private String nom;

    @Column(name = "adresse", length = 255, nullable = false)
    private String adresse;

    @OneToMany(mappedBy = "restaurant")
    private List<EvaluationEntity> evaluations = new ArrayList<>();

    @Column(name = "imageKey")
    private String imageKey;

    public static RestaurantEntity buildFromDto(CreateRestaurantDto createRestaurantDto) {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.nom = createRestaurantDto.nom();
        restaurant.adresse = createRestaurantDto.adresse();
        return restaurant;
    }

    public void addEvaluation (EvaluationEntity evaluationEntity) {
        this.evaluations.add(evaluationEntity);
    }
}
