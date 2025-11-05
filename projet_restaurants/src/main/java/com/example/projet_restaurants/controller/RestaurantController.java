package com.example.projet_restaurants.controller;

import com.example.projet_restaurants.dto.CreateRestaurantDto;
import com.example.projet_restaurants.dto.RestaurantDto;
import com.example.projet_restaurants.dto.UpdateRestaurantDto;
import com.example.projet_restaurants.dto.UpdateRestaurantImageDto;
import com.example.projet_restaurants.entity.RestaurantEntity;
import com.example.projet_restaurants.service.RestaurantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/restaurant")
@Slf4j
@Validated
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping
    public List<RestaurantDto> getRestaurants() {
        return this.restaurantService.getRestaurants()
                .stream()
                .map(resto ->  {
                    String imageUrl = this.restaurantService.getRestaurantImageUrl(resto.getId());
                    double moyenne = this.restaurantService.averageFor(resto.getId());
                    return RestaurantDto.buildFromEntity(resto, imageUrl, moyenne);
                })
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping ("/{id}")
    public RestaurantDto getRestaurantById(@PathVariable @Positive Long id) {
        RestaurantEntity res = this.restaurantService.getRestaurantById(id);
        // creation de l'url de l'image
        String imageUrl = this.restaurantService.getRestaurantImageUrl(id);
        // calcule de la moyenne du resto
        double moyenne = this.restaurantService.averageFor(res.getId());

        return RestaurantDto.buildFromEntity(res, imageUrl, moyenne);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<RestaurantDto> createRestaurant(@RequestBody @Valid CreateRestaurantDto createRestaurantDto) {
        RestaurantEntity rest = this.restaurantService.createRestaurant(createRestaurantDto);
        // moyenne de -1
        double moyenne = -1.0;

        // url null lors de la création (pas d image)
        RestaurantDto body = RestaurantDto.buildFromEntity(rest, null, moyenne);

        // enveloppe du resultat dans une reponse http
        return ResponseEntity.created(URI.create("/restaurant/" + rest.getId())).body(body);

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<RestaurantDto> updateRestaurant(@PathVariable @Positive Long id, @RequestBody @Valid UpdateRestaurantDto updateRestaurantDto) {
        RestaurantEntity rest = this.restaurantService.updateRestaurant(id, updateRestaurantDto);

        // creation de l'url de l'image
        String imageUrl = this.restaurantService.getRestaurantImageUrl(id);

        double moyenne = this.restaurantService.averageFor(rest.getId());

        RestaurantDto body = RestaurantDto.buildFromEntity(rest, imageUrl, moyenne);

        return ResponseEntity.ok().body(body);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/image")
    public UpdateRestaurantImageDto getUpdateRestaurantImageUrl (@PathVariable @Positive Long id) {
        return new UpdateRestaurantImageDto(this.restaurantService.getUpdateRestaurantImageUrl(id));
    }
}
