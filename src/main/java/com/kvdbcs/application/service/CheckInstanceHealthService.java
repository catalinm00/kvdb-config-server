package com.kvdbcs.application.service;

import com.kvdbcs.infrastructure.client.DbInstanceClient;
import com.kvdbcs.domain.model.DbInstance;
import com.kvdbcs.domain.model.InstancesRecoveryFile;
import com.kvdbcs.domain.repository.DbInstanceRepository;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Singleton
public class CheckInstanceHealthService {
    private static final Logger logger = LoggerFactory.getLogger(CheckInstanceHealthService.class);
    private final DbInstanceRepository repository;
    private final DbInstanceClient dbInstanceClient;

    public CheckInstanceHealthService(DbInstanceRepository repository, DbInstanceClient dbInstanceClient) {
        this.dbInstanceClient = dbInstanceClient;
        this.repository = repository;
    }

    @Scheduled(initialDelay = "15s", fixedDelay = "30s")
    public void execute() throws IOException {
        logger.info("Starting health check for all instances");
        for (DbInstance dbInstance : repository.findAll()) {
            if (dbInstanceClient.isHealthy(dbInstance)) { continue; }

            repository.delete(dbInstance);
            var file = new InstancesRecoveryFile(InstancesRecoveryFile.DEFAULT_PATH);
            file.deleteInstance(dbInstance);
            logger.info("Instance {} DELETED", dbInstance.getHost());
        }
        logger.info("Finished health check for all instances");
    }
}
