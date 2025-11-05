package com.example.projet_restaurants.client;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    public MinioService(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.accessKey}") String accesKey,
            @Value("${minio.secretKey}") String secretKey
    ) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accesKey, secretKey)
                .build();

        try {
            log.info("Bucked coursm2 exists ? {}", this.minioClient.bucketExists(BucketExistsArgs.builder().bucket("coursm2").build()));

            var x = this.minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .expiry(1000)
                    .bucket("coursm2")
                    .object("test.txt")
                    .method(Method.POST)
                    .build());

            log.info("Presigned URL: {}", x);
        } catch (Exception e) {
            log.error("MinIO connectivity check failed", e);
        }
    }

    // Url déstiné pour l'upload de l'image
    public String getUpdateUrl(String objectKey) {
        try {
            return this.minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs
                            .builder()
                            .bucket("coursm2")
                            .expiry(60)
                            .method(Method.PUT)
                            .object(objectKey)
                            .build());
        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        } catch (ServerException e) {
            throw new RuntimeException(e);
        }
    }

    // donne url pour download l image (GET)
    public String getUrl(String objectKey) {
        try {
            return this.minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs
                            .builder()
                            .bucket("coursm2")
                            .expiry(60)
                            .method(Method.GET)
                            .object(objectKey)
                            .build());
        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        } catch (ServerException e) {
            throw new RuntimeException(e);
        }
    }

    public String getRestaurantKey (Long id) {
        return "restaurant_" + id + "_image.jpg";
    }

    public String getPlatKey (Long restoId, Long evalId, Long platImageId) {
        return "restaurant_" + restoId + "_evaluation_" + evalId + "_plat_" + platImageId;
    }


    public void delete(String objectKey) {
        try {
            this.minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("coursm2")
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression MinIO pour key=" + objectKey, e);
        }
    }
}
