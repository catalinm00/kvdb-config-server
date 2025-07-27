package com.kvdbcs.application.service;

import com.kvdbcs.domain.model.InstancesRecoveryFile;
import com.kvdbcs.domain.repository.DbInstanceRepository;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.kvdbcs.domain.model.InstancesRecoveryFile.DEFAULT_PATH;

@Singleton
public class RecoverInstancesFromLogService {
    private static final Logger logger = LoggerFactory.getLogger(RecoverInstancesFromLogService.class);
    private final DbInstanceRepository dbInstanceRepository;

    public RecoverInstancesFromLogService(DbInstanceRepository dbInstanceRepository) {
        this.dbInstanceRepository = dbInstanceRepository;
    }

    @EventListener
    public void listen(ServerStartupEvent event) {
        logger.info("Starting instance recovery process on application startup");
        try {
            execute();
            logger.info("Instance recovery process completed successfully");
        } catch (IOException e) {
            logger.warn("Error during instance recovery on startup", e);
        }
    }

    public void execute() throws IOException {
        logger.info("Searching for recovery file");
        if (!InstancesRecoveryFile.exists(DEFAULT_PATH)) {
            logger.info("No recovery file found");
            return;
        }
        logger.info("Recovery file: FOUND");
        var file = new InstancesRecoveryFile(DEFAULT_PATH);
        var instances = file.readInstances();
        instances.forEach(instance -> {
            logger.info("Recovering instance {}", instance);
            dbInstanceRepository.save(instance);
        });
        logger.info("Recovery completed.");
    }

}
