package com.kvdbcs.application.service;

import com.kvdbcs.domain.event.UpdatedInstanceEvent;
import com.kvdbcs.infrastructure.client.DbInstanceClient;
import com.kvdbcs.domain.model.DbInstance;
import com.kvdbcs.domain.model.InstancesRecoveryFile;
import com.kvdbcs.domain.repository.DbInstanceRepository;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class CheckInstanceHealthService {
    private static final Logger logger = LoggerFactory.getLogger(CheckInstanceHealthService.class);
    private final DbInstanceRepository repository;
    private final DbInstanceClient dbInstanceClient;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<Long, Integer> retries = new HashMap<>();

    public CheckInstanceHealthService(DbInstanceRepository repository, DbInstanceClient dbInstanceClient, ApplicationEventPublisher eventPublisher) {
        this.dbInstanceClient = dbInstanceClient;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(initialDelay = "15s", fixedDelay = "10s")
    public void execute() throws IOException {
        logger.info("Starting health check for all instances");

        for (DbInstance dbInstance : repository.findAll()) {
            long instanceId = dbInstance.getId();

            if (dbInstanceClient.isHealthy(dbInstance)) {
                retries.remove(instanceId); // Limpia el contador si está sano
                continue;
            }

            int attempt = retries.getOrDefault(instanceId, 0) + 1;
            retries.put(instanceId, attempt);

            if (attempt < 3) {
                logger.warn("Instance {} failed health check (attempt {})", instanceId, attempt);
                continue;
            }

            // Eliminar instancia después de 3 intentos fallidos
            retries.remove(instanceId);
            repository.delete(dbInstance);

            InstancesRecoveryFile file = new InstancesRecoveryFile(InstancesRecoveryFile.DEFAULT_PATH);
            file.deleteInstance(dbInstance);

            logger.info("Instance {} DELETED after {} failed health checks", instanceId, attempt);
            eventPublisher.publishEvent(new UpdatedInstanceEvent(dbInstance));
        }

        logger.info("Finished health check for all instances");
    }

}
