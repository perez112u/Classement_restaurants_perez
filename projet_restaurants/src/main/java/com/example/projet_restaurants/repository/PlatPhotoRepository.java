package com.example.projet_restaurants.repository;

import com.example.projet_restaurants.entity.PlatPhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlatPhotoRepository extends JpaRepository<PlatPhotoEntity,Long> {

    List<PlatPhotoEntity> findByEvaluationId(Long evaluationId);

}
