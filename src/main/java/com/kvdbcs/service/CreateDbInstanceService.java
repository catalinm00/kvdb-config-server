package com.kvdbcs.service;

import com.kvdbcs.model.DbInstance;
import com.kvdbcs.model.InstancesRecoveryFile;
import com.kvdbcs.repository.DbInstanceRepository;
import com.kvdbcs.service.command.CreateDbInstanceCommand;
import jakarta.inject.Singleton;

import java.io.IOException;

@Singleton
public class CreateDbInstanceService {
    private final DbInstanceRepository dbInstanceRepository;

    public CreateDbInstanceService(DbInstanceRepository dbInstanceRepository) {
        this.dbInstanceRepository = dbInstanceRepository;
    }

    public DbInstance execute(CreateDbInstanceCommand command) throws IOException {
        if (command.host() == null || command.host().isEmpty()) {
            throw new IllegalArgumentException("host is required");
        }
        if (command.port() < 0) {
            throw new IllegalArgumentException("valid port is required");
        }
        var instance = dbInstanceRepository.save(new DbInstance(command.host(), command.port()));
        var recoveryFile = new InstancesRecoveryFile(InstancesRecoveryFile.DEFAULT_DIR, InstancesRecoveryFile.DEFAULT_NAME);
        recoveryFile.writeInstance(instance);
        return instance;
    }
}
