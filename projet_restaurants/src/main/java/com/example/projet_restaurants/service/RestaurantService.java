package com.example.projet_restaurants.service;

import com.example.projet_restaurants.client.MinioService;
import com.example.projet_restaurants.dto.CreateRestaurantDto;
import com.example.projet_restaurants.dto.UpdateRestaurantDto;
import com.example.projet_restaurants.entity.RestaurantEntity;
import com.example.projet_restaurants.repository.EvaluationRepository;
import com.example.projet_restaurants.repository.RestaurantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Slf4j
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    // utilisé pour avoir acces aux notes (pour la moyenne)
    private final EvaluationRepository evaluationRepository;

    private final MinioService minioService;

    public RestaurantService(RestaurantRepository restaurantRepository, EvaluationRepository evaluationRepository, MinioService minioService) {
        this.restaurantRepository = restaurantRepository;
        this.evaluationRepository = evaluationRepository;
        this.minioService = minioService;
    }

    public List<RestaurantEntity> getRestaurants() {
        return this.restaurantRepository.findAll();
    }

    public RestaurantEntity getRestaurantById(Long id) {
        return this.restaurantRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant introuvable: " + id));
    }

    public RestaurantEntity createRestaurant(@Valid CreateRestaurantDto createRestaurantDto) {
        RestaurantEntity restaurant = RestaurantEntity.buildFromDto(createRestaurantDto);
        return this.restaurantRepository.save(restaurant);
    }

    public RestaurantEntity updateRestaurant(Long id, @Valid UpdateRestaurantDto updateRestaurantDto) {
        RestaurantEntity restaurant = this.restaurantRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant introuvable: " + id));
        restaurant.setNom(updateRestaurantDto.nom());
        restaurant.setAdresse(updateRestaurantDto.adresse());
        return this.restaurantRepository.save(restaurant);
    }

    // moyenne pour un resto
    public double averageFor(Long restaurantId) {
        Double avg = evaluationRepository.averageNoteByRestaurantId(restaurantId);
        return (avg == null) ? -1.0 : avg;
    }


    public String getUpdateRestaurantImageUrl(@Positive Long id) {
        // on teste si le restaurant existe bien
        RestaurantEntity rest = this.restaurantRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant introuvable: " + id));

        // on stocke la clé
        String imageKey = minioService.getRestaurantKey(id);

        rest.setImageKey(imageKey);
        restaurantRepository.save(rest);

        return minioService.getUpdateUrl(imageKey);
    }

    public String getRestaurantImageUrl(@Positive Long id) {
        // on teste si le restaurant existe bien
        RestaurantEntity rest = this.restaurantRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant introuvable: " + id));

        // on devinne la clé
        String imageKey = rest.getImageKey();

        if (imageKey == null || imageKey.isEmpty()) {
            // pas d'image donc pas d'url
            return null;
        }

        return minioService.getUrl(imageKey);
    }
}
