package com.kvdbcs.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvdbcs.domain.model.DbInstance;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;


@Singleton
public class DbInstanceClient {
    private static final Logger logger = LoggerFactory.getLogger(DbInstanceClient.class);
    private final static int TIMEOUT_SECONDS = 5;
    private final String HEALTH_ENDPOINT = "/health";
    private final String INSTANCE_ENDPOINT = "/instances";
    private HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public DbInstanceClient() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    public boolean isHealthy(DbInstance instance) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(
                        "%s://%s:%d%s".formatted("http", instance.getHost(), instance.getPort(), HEALTH_ENDPOINT)))
                .build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
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

    public void updateInstances(DbInstance instanceToUpdate ,List<DbInstance> instances) {
        try {
            var serializedInstances = mapper.writeValueAsString(instances);
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(serializedInstances))
                    .uri(URI.create(
                            "%s://%s:%d%s".formatted("http", instanceToUpdate.getHost(), instanceToUpdate.getPort(), INSTANCE_ENDPOINT)))
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                logger.warn("Error updating Instance {}: {}", instanceToUpdate.getHost(), response.statusCode());
                return;
            }
        } catch (IOException | InterruptedException e) {
            logger.warn(e.getMessage(), e);
            return;
        }
        logger.info("Instance {}: UPDATED WITH MOST RECENT INSTANCES", instanceToUpdate.getHost());
    }
}
