package com.example.projet_restaurants.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity (name = "evaluation_photo")
@Data
public class PlatPhotoEntity {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "evaluation_id")
    private EvaluationEntity evaluation;

    @Column(name = "image_key", nullable = true)
    private String imageKey;


}
