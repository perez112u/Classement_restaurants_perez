package com.example.projet_restaurants.repository;

import com.example.projet_restaurants.entity.EvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<EvaluationEntity, Long> {

    // methode dérivée automatiquement par Spring à partir du nom
    List<EvaluationEntity> findByRestaurantId(Long restoId);

    // requête JPQL qui calcule la moyenne des notes pour un restaurant donné
    @Query("SELECT AVG(e.note) FROM evaluation e WHERE e.restaurant.id = :restaurantId")
    Double averageNoteByRestaurantId(@Param("restaurantId") Long restaurantId);
}
