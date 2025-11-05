package com.example.projet_restaurants.service;

import com.example.projet_restaurants.client.MinioService;
import com.example.projet_restaurants.dto.CreateEvaluationDto;
import com.example.projet_restaurants.entity.EvaluationEntity;
import com.example.projet_restaurants.entity.PlatPhotoEntity;
import com.example.projet_restaurants.entity.RestaurantEntity;
import com.example.projet_restaurants.repository.EvaluationRepository;
import com.example.projet_restaurants.repository.PlatPhotoRepository;
import com.example.projet_restaurants.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private PlatPhotoRepository platPhotoRepository;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private EvaluationService evaluationService;

    private RestaurantEntity sampleResto;
    private EvaluationEntity sampleEval;

    @BeforeEach
    void generateRestaurantMock() {
        sampleResto = new RestaurantEntity();
        sampleResto.setId(1L);
        sampleResto.setNom("Chez Test");
        sampleResto.setAdresse("5 rue du test, Nancy");

        sampleEval = new EvaluationEntity();
        sampleEval.setId(10L);
        sampleEval.setRestaurant(sampleResto);
        sampleEval.setCommentaire("Bon");
        sampleEval.setNote(3);
        sampleEval.setEvaluateur("un client");
    }

    // verifie que la methode recupere bien la liste des evaluations d'un restaurant
    @Test
    void getEvaluationsByRestoId_ok() {
        when(evaluationRepository.findByRestaurantId(1L)).thenReturn(List.of(sampleEval));

        var list = evaluationService.getEvaluationsByRestoId(1L);

        assertEquals(1, list.size());
        assertSame(sampleEval, list.get(0));
        verify(evaluationRepository, times(1)).findByRestaurantId(1L);
    }

    // verifie la creation d'une evaluation
    @Test
    void createEvaluation_ok() {
        CreateEvaluationDto dto = new CreateEvaluationDto("Tres bon", 3);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sampleResto));
        EvaluationEntity saved = new EvaluationEntity();
        saved.setId(100L);
        when(evaluationRepository.save(any(EvaluationEntity.class))).thenReturn(saved);

        var created = evaluationService.createEvaluation(1L, dto, "toto");

        assertEquals(100L, created.getId());
        verify(restaurantRepository, times(1)).findById(1L);
        verify(evaluationRepository, times(1)).save(any(EvaluationEntity.class));
    }

    // verifie qu'une exception est levee si le restaurant n'existe pas lors de la creation
    @Test
    void createEvaluation_notFoundRestaurant() {
        when(restaurantRepository.findById(2L)).thenReturn(Optional.empty());

        CreateEvaluationDto dto = new CreateEvaluationDto("OK", 1);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> evaluationService.createEvaluation(2L, dto, "toto"));
        assertTrue(ex.getMessage().contains("Restaurant introuvable: 2"));
        verify(restaurantRepository, times(1)).findById(2L);
        verify(evaluationRepository, never()).save(any());
    }

    // verifie la recuperation d'une evaluation avec le restaurant
    @Test
    void getEvaluationByRestaurant_ok() {
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(sampleEval));

        var res = evaluationService.getEvaluationByRestaurant(1L, 10L);
        assertSame(sampleEval, res);
        verify(evaluationRepository, times(1)).findById(10L);
    }

    // verifie qu'une exception est levee si l'evaluation n'existe pas
    @Test
    void getEvaluationByRestaurant_evalNotFound() {
        when(evaluationRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> evaluationService.getEvaluationByRestaurant(1L, 99L));
        assertTrue(ex.getMessage().contains("Evaluation introuvable: 99"));
        verify(evaluationRepository, times(1)).findById(99L);
    }

    // verifie qu'une exception est levee si l'evaluation n'appartient pas au restaurant
    @Test
    void getEvaluationByRestaurant_wrongRestaurant() {
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(sampleEval));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> evaluationService.getEvaluationByRestaurant(2L, 10L));
        assertTrue(ex.getMessage().contains("Restaurant introuvable: 2"));
        verify(evaluationRepository, times(1)).findById(10L);
    }

    // verifie la generation de 3 url d'update
    @Test
    void getUpdatePlatsImageUrls_ok() {
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(sampleEval));


        // pour que le plat ait un id
        when(platPhotoRepository.save(any(PlatPhotoEntity.class)))
                .thenAnswer(inv -> {
                    PlatPhotoEntity e = inv.getArgument(0);
                    e.setId(123L);
                    return e;
                });

        when(minioService.getPlatKey(anyLong(), anyLong(), anyLong())).thenReturn("key");
        when(minioService.getUpdateUrl("key")).thenReturn("url");

        var urls = evaluationService.getUpdatePlatsImageUrls(1L, 10L, 3);

        assertEquals(3, urls.size());
        assertEquals("url", urls.get(0));
        assertEquals("url", urls.get(1));
        assertEquals("url", urls.get(2));

        verify(evaluationRepository, times(1)).findById(10L);
        verify(platPhotoRepository, times(6)).save(any(PlatPhotoEntity.class));
        verify(minioService, times(3)).getPlatKey(anyLong(), anyLong(), anyLong());
        verify(minioService, times(3)).getUpdateUrl(anyString());
    }

    // verifie qu'une exception est levee si l'évaluation n'existe pas
    @Test
    void getUpdatePlatsImageUrls_evalNotFound() {
        when(evaluationRepository.findById(77L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> evaluationService.getUpdatePlatsImageUrls(1L, 77L, 1));
        assertTrue(ex.getMessage().contains("Evaluation introuvable: 77"));
        verify(evaluationRepository, times(1)).findById(77L);
        verify(platPhotoRepository, never()).save(any());
    }


    // verifie simplement la generation des URLs GET (sans details sur les cles)
    @Test
    void getPlatsImageUrls_ok() {
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(sampleEval));

        PlatPhotoEntity p1 = new PlatPhotoEntity();
        p1.setImageKey("k");
        p1.setEvaluation(sampleEval);
        PlatPhotoEntity p2 = new PlatPhotoEntity();
        p2.setImageKey("k");
        p2.setEvaluation(sampleEval);
        when(platPhotoRepository.findByEvaluationId(10L)).thenReturn(List.of(p1, p2));

        when(minioService.getUrl(anyString())).thenReturn("url");

        var urls = evaluationService.getPlatsImageUrls(1L, 10L);

        assertEquals(2, urls.size());
        assertEquals("url", urls.get(0));
        assertEquals("url", urls.get(1));
        verify(evaluationRepository, times(1)).findById(10L);
        verify(platPhotoRepository, times(1)).findByEvaluationId(10L);
        verify(minioService, times(2)).getUrl(anyString());
    }

    // verifie qu'une exception 404 est levee si l'evaluation n'existe pas (URLs GET)
    @Test
    void getPlatsImageUrls_evalNotFound() {
        when(evaluationRepository.findById(55L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> evaluationService.getPlatsImageUrls(1L, 55L));
        assertTrue(ex.getMessage().contains("Evaluation introuvable: 55"));
        verify(evaluationRepository, times(1)).findById(55L);
        verify(platPhotoRepository, never()).findByEvaluationId(anyLong());
        verify(minioService, never()).getUrl(anyString());
    }


    // verifie qu'une exception est levee si l'evaluation n'existe pas a la suppression
    @Test
    void deleteEvaluation_evalNotFound() {
        when(evaluationRepository.findById(88L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> evaluationService.deleteEvaluation(1L, 88L));
        assertTrue(ex.getMessage().contains("Evaluation introuvable: 88"));
        verify(evaluationRepository, times(1)).findById(88L);
        verify(evaluationRepository, never()).delete(any());
    }

    // verifie qu'une exception est levee si l'evaluation n'appartient pas au restaurant a la suppression
    @Test
    void deleteEvaluation_wrongRestaurant() {
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(sampleEval));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> evaluationService.deleteEvaluation(2L, 10L));
        assertTrue(ex.getMessage().contains("Restaurant introuvable: 2"));
        verify(evaluationRepository, times(1)).findById(10L);
        verify(evaluationRepository, never()).delete(any());
        verify(minioService, never()).delete(anyString());
    }
}
