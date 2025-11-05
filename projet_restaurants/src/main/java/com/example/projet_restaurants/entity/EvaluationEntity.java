package com.example.projet_restaurants.entity;

import com.example.projet_restaurants.dto.CreateEvaluationDto;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "evaluation")
@Data
public class EvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "id")
    private Long id;

    @Column (name = "evaluateur", length = 50, nullable = false)
    private String evaluateur;

    @Column (name = "commentaire", length = 255, nullable = false)
    private String commentaire;

    @Column (name = "note", nullable = false)
    private Integer note;

    @ManyToOne
    @JoinColumn (name = "resto_id", nullable = false)
    private RestaurantEntity restaurant;

    // cascade et orphanRemoval permette de supprimer cette entité si l'evaluation attribué est supprimé
    @OneToMany (mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlatPhotoEntity> platsPhotos = new ArrayList<>();

    public static EvaluationEntity buildFromDto(CreateEvaluationDto createEvaluationDto, RestaurantEntity restaurant) {
        EvaluationEntity res = new EvaluationEntity();
        res.restaurant = restaurant;
        res.commentaire = createEvaluationDto.commentaire();
        res.note = createEvaluationDto.note();
        return res;
    }
}
