package com.example.projet_restaurants.service;

import com.example.projet_restaurants.client.MinioService;
import com.example.projet_restaurants.dto.CreateRestaurantDto;
import com.example.projet_restaurants.dto.UpdateRestaurantDto;
import com.example.projet_restaurants.entity.RestaurantEntity;
import com.example.projet_restaurants.repository.EvaluationRepository;
import com.example.projet_restaurants.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private RestaurantService restaurantService;

    private RestaurantEntity sample;

    @BeforeEach
    void generateRestaurantMock() {
        sample = new RestaurantEntity();
        sample.setId(1L);
        sample.setNom("Chez Test");
        sample.setAdresse("5 rue du test, Nancy");
    }

    // on vérifie que la méthode récupère bien la liste complète des restaurants
    @Test
    void getRestaurants_ok() {
        when(restaurantRepository.findAll()).thenReturn(List.of(sample));

        var result = restaurantService.getRestaurants();

        assertEquals(1, result.size());
        RestaurantEntity r = result.get(0);
        assertEquals(1L, r.getId());
        assertEquals("Chez Test", r.getNom());
        verify(restaurantRepository, times(1)).findAll();
    }

    // on vérifie la récupération d'un restaurant existant avec l id
    @Test
    void getRestaurantById_found() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sample));

        var result = restaurantService.getRestaurantById(1L);

        assertSame(sample, result);
        verify(restaurantRepository, times(1)).findById(1L);
    }

    // on vérifie l'exception si l'id n'existe pas
    @Test
    void getRestaurantById_notFound() {
        when(restaurantRepository.findById(2L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> restaurantService.getRestaurantById(2L));
        assertTrue(ex.getMessage().contains("Restaurant introuvable: 2"));
        verify(restaurantRepository, times(1)).findById(2L);
    }

    // on vérifie la création d'un restaurant
    @Test
    void createRestaurant_ok() {
        CreateRestaurantDto dto = new CreateRestaurantDto("Nouveau", "Adresse");

        RestaurantEntity saved = new RestaurantEntity();
        saved.setId(10L);
        saved.setNom("Nouveau");
        saved.setAdresse("Adresse");
        when(restaurantRepository.save(any(RestaurantEntity.class))).thenReturn(saved);

        var created = restaurantService.createRestaurant(dto);

        assertEquals(10L, created.getId());
        assertEquals("Nouveau", created.getNom());
        assertEquals("Adresse", created.getAdresse());
        verify(restaurantRepository, times(1)).save(any(RestaurantEntity.class));
    }

    // on vérifie la mise à jour des champs
    @Test
    void updateRestaurant_found() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sample));
        when(restaurantRepository.save(any(RestaurantEntity.class))).thenReturn(sample);

        UpdateRestaurantDto dto = new UpdateRestaurantDto("Maj", "Nouvelle adresse");
        var updated = restaurantService.updateRestaurant(1L, dto);

        assertEquals("Maj", updated.getNom());
        assertEquals("Nouvelle adresse", updated.getAdresse());
        verify(restaurantRepository, times(1)).findById(1L);
        verify(restaurantRepository, times(1)).save(sample);
    }

    // on vérifie l'exception si on met à jour un id inexistant
    @Test
    void updateRestaurant_notFound() {
        when(restaurantRepository.findById(9L)).thenReturn(Optional.empty());

        UpdateRestaurantDto dto = new UpdateRestaurantDto("Maj", "Adresse");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> restaurantService.updateRestaurant(9L, dto));
        assertTrue(ex.getMessage().contains("Restaurant introuvable: 9"));
        verify(restaurantRepository, times(1)).findById(9L);
        verify(restaurantRepository, never()).save(any());
    }

    // on vérifie que la moyenne retournée par le repo est renvoyée
    @Test
    void averageFor_value() {
        when(evaluationRepository.averageNoteByRestaurantId(1L)).thenReturn(4.5);

        double avg = restaurantService.averageFor(1L);
        assertEquals(4.5, avg);
        verify(evaluationRepository, times(1)).averageNoteByRestaurantId(1L);
    }

    // on vérifie que -1.0 est renvoyé quand aucune note
    @Test
    void averageFor_null() {
        when(evaluationRepository.averageNoteByRestaurantId(1L)).thenReturn(null);

        double avg = restaurantService.averageFor(1L);
        assertEquals(-1.0, avg);
        verify(evaluationRepository, times(1)).averageNoteByRestaurantId(1L);
    }

    // on vérifie la génération de la clé, l'enregistrement de l'imageKey et le retour de l'URL PUT
    @Test
    void getUpdateRestaurantImageUrl_ok() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sample));
        when(minioService.getRestaurantKey(1L)).thenReturn("restaurant_1_image.jpg");
        when(minioService.getUpdateUrl("restaurant_1_image.jpg")).thenReturn("http://minio/update/url");

        var url = restaurantService.getUpdateRestaurantImageUrl(1L);

        assertEquals("restaurant_1_image.jpg", sample.getImageKey());
        assertEquals("http://minio/update/url", url);
        verify(restaurantRepository, times(1)).findById(1L);
        verify(restaurantRepository, times(1)).save(sample);
        verify(minioService, times(1)).getRestaurantKey(1L);
        verify(minioService, times(1)).getUpdateUrl("restaurant_1_image.jpg");
    }

    // lance une exception si on demande une url d'upload pour un id inexistant
    @Test
    void getUpdateRestaurantImageUrl_notFound() {
        when(restaurantRepository.findById(7L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> restaurantService.getUpdateRestaurantImageUrl(7L));
        assertTrue(ex.getMessage().contains("Restaurant introuvable: 7"));
        verify(restaurantRepository, times(1)).findById(7L);
        verify(minioService, never()).getRestaurantKey(any());
        verify(minioService, never()).getUpdateUrl(any());
        verify(restaurantRepository, never()).save(any());
    }

    // on vérifie que l'url est null lorsque imageKey est null ou vide
    @Test
    void getRestaurantImageUrl_noImageKey() {
        // imageKey null
        sample.setImageKey(null);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sample));

        var url1 = restaurantService.getRestaurantImageUrl(1L);
        assertNull(url1);
        verify(restaurantRepository, times(1)).findById(1L);

        // imageKey vide
        reset(restaurantRepository, minioService);
        sample.setImageKey("");
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sample));

        var url2 = restaurantService.getRestaurantImageUrl(1L);
        assertNull(url2);
        verify(restaurantRepository, times(1)).findById(1L);
        verify(minioService, never()).getUrl(any());
    }

    // on vérifie le retour d'une url get quand imageKey est présent
    @Test
    void getRestaurantImageUrl_ok() {
        sample.setImageKey("restaurant_1_image.jpg");
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sample));
        when(minioService.getUrl("restaurant_1_image.jpg")).thenReturn("http://minio/get/url");

        var url = restaurantService.getRestaurantImageUrl(1L);

        assertEquals("http://minio/get/url", url);
        verify(restaurantRepository, times(1)).findById(1L);
        verify(minioService, times(1)).getUrl(eq("restaurant_1_image.jpg"));
    }
}
