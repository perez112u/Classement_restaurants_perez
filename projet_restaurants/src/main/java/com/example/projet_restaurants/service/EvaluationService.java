package com.example.projet_restaurants.service;

import com.example.projet_restaurants.client.MinioService;
import com.example.projet_restaurants.dto.CreateEvaluationDto;
import com.example.projet_restaurants.dto.EvaluationDto;
import com.example.projet_restaurants.entity.EvaluationEntity;
import com.example.projet_restaurants.entity.PlatPhotoEntity;
import com.example.projet_restaurants.entity.RestaurantEntity;
import com.example.projet_restaurants.repository.EvaluationRepository;
import com.example.projet_restaurants.repository.PlatPhotoRepository;
import com.example.projet_restaurants.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;

    private final RestaurantRepository restaurantRepository;

    private final PlatPhotoRepository platPhotoRepository;

    private final MinioService minioService;

    public EvaluationService(EvaluationRepository evaluationRepository, RestaurantRepository restaurantRepository, PlatPhotoRepository platPhotoRepository, MinioService minioService) {
        this.evaluationRepository = evaluationRepository;
        this.restaurantRepository = restaurantRepository;
        this.platPhotoRepository = platPhotoRepository;

        this.minioService = minioService;
    }

    public List<EvaluationEntity> getEvaluationsByRestoId (long resto_id) {
        return evaluationRepository.findByRestaurantId(resto_id);
    }

    public EvaluationEntity createEvaluation(long restaurantId, CreateEvaluationDto createEvaluationDto, String authorUsername) {
        // on récupère le restaurant attribué
        RestaurantEntity restaurant = this.restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant introuvable: " + restaurantId));

        EvaluationEntity res = EvaluationEntity.buildFromDto(createEvaluationDto, restaurant);
        // on ajout l utilisateur dans l entité
        if (authorUsername != null && !authorUsername.isBlank()) {
            res.setEvaluateur(authorUsername);
        }

        // on lie des deux coté pour la cohérence
        restaurant.addEvaluation(res);

        return evaluationRepository.save(res);
    }

    public EvaluationEntity getEvaluationByRestaurant(long restaurantId, long evaluationId) {
        EvaluationEntity eval = this.evaluationRepository.findById(evaluationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation introuvable: " + evaluationId));

        if (!eval.getRestaurant().getId().equals(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant introuvable: " + restaurantId);
        }
        return eval;
    }

    public List<String> getUpdatePlatsImageUrls(@Positive long restaurantId, @Positive long evaluationId, int nbImage) {
        // 404 si l'éval n'existe pas
        EvaluationEntity eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation introuvable: " + evaluationId));

        // on verifie l’appartenance au resto
        if (!eval.getRestaurant().getId().equals(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Restaurant introuvable: " + restaurantId);
        }

        List<String> uploadImageUrls = new ArrayList<>();
        for (int i = 0; i < nbImage; i++) {

            // on créé d abord l entity sans l url pour pouvoir recup l'id unique generé par spring
            PlatPhotoEntity p = new PlatPhotoEntity();
            p.setEvaluation(eval);
            this.platPhotoRepository.save(p);

            Long platImageId = p.getId();

            // on recupere la clé généré par notre minioService pour notre plat
            String platImageKey = this.minioService.getPlatKey(restaurantId, evaluationId, platImageId);

            // on ajoute dans la bdd la clé dans ce plat de l'evaluation courante
            p.setImageKey(platImageKey);
            platPhotoRepository.save(p);

            // on genere l'url update via la key
            String url = minioService.getUpdateUrl(platImageKey);
            uploadImageUrls.add(url);
        }

        return uploadImageUrls;
    }

    public List<String> getPlatsImageUrls(@Positive long restaurantId, @Positive long evaluationId) {
        // 404 si l'éval n'existe pas
        EvaluationEntity eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation introuvable: " + evaluationId));

        // on vérifie l’appartenance au resto
        if (!eval.getRestaurant().getId().equals(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant introuvable: " + restaurantId);
        }

        // recup les photos lié à l'evaluation
        List<PlatPhotoEntity> photos = platPhotoRepository.findByEvaluationId(evaluationId);

        // on genere l'url get pour chaque images des plats
        List<String> uploadImageUrls = new ArrayList<>();
        for (PlatPhotoEntity p : photos) {
            String key = p.getImageKey();
            String url = minioService.getUrl(key);
            uploadImageUrls.add(url);
        }

        return uploadImageUrls;
    }

    // pour mettre un verrou sur la suppression ou si une suppression echoue, on annule tout
    @Transactional
    public void deleteEvaluation(@Positive long restaurantId, @Positive long evaluationId) {
        EvaluationEntity eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Evaluation introuvable: " + evaluationId));

        if (!eval.getRestaurant().getId().equals(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Restaurant introuvable: " + restaurantId);
        }

        // on supprime les objets minio liés
        if (eval.getPlatsPhotos() != null) {
            for (PlatPhotoEntity p : eval.getPlatsPhotos()) {
                String key = p.getImageKey();
                if (key != null) {
                    try {
                        minioService.delete(key);
                    } catch (Exception e) {
                       log.warn("Echec suppression MinIO pour key={} (éval={}) : {}", key, evaluationId, e.getMessage());
                    }
                }
            }
        }

        // on supprime ensuite dans la bdd (cascade + orphanremoval)
        evaluationRepository.delete(eval);
    }
}
