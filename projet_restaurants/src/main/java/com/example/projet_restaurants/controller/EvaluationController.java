package com.example.projet_restaurants.controller;

import com.example.projet_restaurants.dto.CreateEvaluationDto;
import com.example.projet_restaurants.dto.EvaluationDto;
import com.example.projet_restaurants.dto.UpdatePlatsImageDto;
import com.example.projet_restaurants.entity.EvaluationEntity;
import com.example.projet_restaurants.service.EvaluationIndexService;
import com.example.projet_restaurants.service.EvaluationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/restaurant/{restaurantId}/evaluation")
@Slf4j
@Validated
public class EvaluationController {

    private final EvaluationService evaluationService;

    private final EvaluationIndexService evaluationIndexService;

    public EvaluationController(EvaluationService evaluationService, EvaluationIndexService evaluationIndexService) {
        this.evaluationService = evaluationService;
        this.evaluationIndexService = evaluationIndexService;
    }

    @GetMapping
    public List<EvaluationDto> getEvaluationsByRestoId (@PathVariable @Positive long restaurantId) {
        return this.evaluationService.getEvaluationsByRestoId(restaurantId).stream()
                .map(eval -> {
                    List<String> platsImagesUrls = this.evaluationService.getPlatsImageUrls(restaurantId, eval.getId());
                    return EvaluationDto.fromEntity(eval, platsImagesUrls);
                })
                .toList();

    }

    @GetMapping("/{evaluationId}")
    public EvaluationDto getEvaluation(@PathVariable @Positive long restaurantId, @PathVariable @Positive long evaluationId) {
        EvaluationEntity eval = this.evaluationService.getEvaluationByRestaurant(restaurantId, evaluationId);
        List<String> platsImagesUrls = this.evaluationService.getPlatsImageUrls(restaurantId, eval.getId());
        return EvaluationDto.fromEntity(eval, platsImagesUrls);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping
    public ResponseEntity<EvaluationDto> createEvaluation (@PathVariable @Positive long restaurantId, @Valid @RequestBody CreateEvaluationDto createEvaluationDto, Authentication authentication) {
        String author = (authentication instanceof JwtAuthenticationToken jwtAuth)
                ? jwtAuth.getName()
                : null;

        EvaluationEntity eval = this.evaluationService.createEvaluation(restaurantId, createEvaluationDto, author);

        // inexation service lucene
        try {
            this.evaluationIndexService.indexEvaluation(eval.getId(), eval.getRestaurant().getId(), eval.getEvaluateur(), eval.getCommentaire(), eval.getNote());
        } catch (Exception e) {
            log.warn("Echec indexation Lucene pour evaluation id={} : {}",
                    eval.getId(), e.getMessage());
        }


        //liste d urls vide pour l instant
        EvaluationDto body = EvaluationDto.fromEntity(eval, List.of());

        return ResponseEntity.created(URI.create("/restaurant/" + restaurantId + "/evaluation/" + eval.getId())).body(body);

    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PutMapping("/{evaluationId}/upload-urls")
    public UpdatePlatsImageDto getUpdatePlatsImageUrls (
            @PathVariable @Positive long restaurantId,
            @PathVariable @Positive long evaluationId,
            @RequestParam(defaultValue = "1") int nbImage,
            Authentication authentication
    ) {
        EvaluationEntity eval = this.evaluationService.getEvaluationByRestaurant(restaurantId, evaluationId);

        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            // si role = USER alors on verifie que c est le user auteur de cet evalution -> sinon pas les droits
            String currentUser = (authentication instanceof JwtAuthenticationToken jwtAuth) ? jwtAuth.getName() : null;
            if (currentUser == null || !currentUser.equals(eval.getEvaluateur())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé: uniquement l'auteur ou un admin");
            }
        }

        return new UpdatePlatsImageDto(this.evaluationService.getUpdatePlatsImageUrls(restaurantId, evaluationId, nbImage));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/{evaluationId}")
    public ResponseEntity<Void> deleteEvaluation (
            @PathVariable @Positive long restaurantId,
            @PathVariable @Positive long evaluationId,
            Authentication authentication
    ) {
        EvaluationEntity eval = this.evaluationService.getEvaluationByRestaurant(restaurantId, evaluationId);

        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        log.info("admin ? ! " + isAdmin);
        if (!isAdmin) {
            // si user alors on verifie que c est le user auteur de cet evaluation -> sinon pas les droits
            String currentUser = (authentication instanceof JwtAuthenticationToken jwtAuth) ? jwtAuth.getName() : null;
            log.info("current user: " + currentUser);

            if (currentUser == null || !currentUser.equals(eval.getEvaluateur())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé: uniquement l'auteur ou un admin");
            }
        }

        evaluationService.deleteEvaluation(restaurantId, evaluationId);

        try {
            evaluationIndexService.deleteEvaluation(evaluationId);
        } catch (Exception e) {
            log.warn("Echec suppression index Lucene pour evaluation id={}: {}", evaluationId, e.getMessage());
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<EvaluationDto> searchEvaluation (@PathVariable @Positive long restaurantId, @RequestParam(name = "query") String query) {
        // on recupere les ids correspondant via l index
        List<String> ids = evaluationIndexService.searchByKeywords(query, restaurantId);

        return ids.stream().map(id_str -> {
            try {
                long id = Long.parseLong(id_str);
                EvaluationEntity eval = this.evaluationService.getEvaluationByRestaurant(restaurantId, id);

                List<String> platsImagesUrls = this.evaluationService.getPlatsImageUrls(restaurantId, eval.getId());

                return EvaluationDto.fromEntity(eval, platsImagesUrls);
            } catch (Exception e) {
                log.debug("Evaluation manquante ou non accessible pour id={} dans la recherche: {}", id_str, e.getMessage());
                return null;
            }
        }).toList();
    }
}
