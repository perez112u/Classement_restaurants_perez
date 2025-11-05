package com.example.projet_restaurants.repository;

import com.example.projet_restaurants.entity.EvaluationEntity;
import com.example.projet_restaurants.entity.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<RestaurantEntity, Long> {
}
