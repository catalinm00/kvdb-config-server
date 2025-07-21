package com.kvdbcs.service;

import com.kvdbcs.model.InstancesRecoveryFile;
import com.kvdbcs.repository.DbInstanceRepository;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Singleton
public class RecoverInstancesFromLogService {
    private static final String LOG_PATH = "logs/recovery/";
    private static final String RECOVERY_FILE = "recovery.log";
    private static final Logger logger = LoggerFactory.getLogger(RecoverInstancesFromLogService.class);
    private final DbInstanceRepository dbInstanceRepository;

    public RecoverInstancesFromLogService(DbInstanceRepository dbInstanceRepository) {
        this.dbInstanceRepository = dbInstanceRepository;
    }

    @PostConstruct
    public void execute() throws IOException {
        logger.info("Searching for recovery file");
        if (!InstancesRecoveryFile.exists(LOG_PATH + RECOVERY_FILE)) {
            logger.info("No recovery file found");
            return;
        }
        logger.info("Recovery file: FOUND");
        var file = new InstancesRecoveryFile(LOG_PATH, RECOVERY_FILE);
        var instances = file.readInstances();
        instances.forEach(instance -> {
            logger.info("Recovering instance {}", instance);
            dbInstanceRepository.save(instance);
        });
        logger.info("Recovery completed. Deleting recovery file");
        file.delete();
    }

}
