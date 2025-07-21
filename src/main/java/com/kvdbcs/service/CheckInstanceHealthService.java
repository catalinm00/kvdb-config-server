package com.kvdbcs.service;

import com.kvdbcs.model.DbInstance;
import com.kvdbcs.repository.DbInstanceRepository;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Singleton
public class CheckInstanceHealthService {
    private static final Logger logger = LoggerFactory.getLogger(CheckInstanceHealthService.class);
    private final HttpClient httpClient;
    private final static String HEALTH_ENDPOINT = "/health";
    private final static String PROTOCOL = "http";
    private final static int TIMEOUT_SECONDS = 5;
    private final DbInstanceRepository repository;

    public CheckInstanceHealthService(DbInstanceRepository repository) {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.repository = repository;
    }

    @Scheduled(initialDelay = "15s", fixedDelay = "30s")
    public void execute() {
        logger.info("Starting health check for all instances");
        for (DbInstance dbInstance : repository.findAll()) {
            if (isHealthy(dbInstance)) {continue;}
            repository.delete(dbInstance);
            logger.info("Instance {} DELETED", dbInstance.getHost());
        }
    }

    public boolean isHealthy(DbInstance instance) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(
                        "%s://%s:%d%s".formatted(PROTOCOL, instance.getHost(), instance.getPort(), HEALTH_ENDPOINT)))
                .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                logger.warn("Instance {}: ERROR {}", instance.getHost(), response.statusCode());
                return false;
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        logger.info("Instance {}: OK", instance.getHost());
        return true;
    }
}
