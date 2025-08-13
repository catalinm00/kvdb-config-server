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
            if (dbInstanceClient.isHealthy(dbInstance)) { continue; }
            retries.put(dbInstance.getId(), retries.getOrDefault(dbInstance.getId(), 0) + 1);
            if (retries.get(dbInstance.getId()) < 3) { continue; }
            retries.replace(dbInstance.getId(), null);
            repository.delete(dbInstance);
            var file = new InstancesRecoveryFile(InstancesRecoveryFile.DEFAULT_PATH);
            file.deleteInstance(dbInstance);
            logger.info("Instance {} DELETED", dbInstance.getId());
            eventPublisher.publishEvent(new UpdatedInstanceEvent(dbInstance));
        }
        logger.info("Finished health check for all instances");
    }
}
