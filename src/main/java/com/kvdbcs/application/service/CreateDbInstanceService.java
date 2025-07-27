package com.kvdbcs.application.service;

import com.kvdbcs.domain.model.DbInstance;
import com.kvdbcs.domain.model.InstancesRecoveryFile;
import com.kvdbcs.domain.repository.DbInstanceRepository;
import com.kvdbcs.application.service.command.CreateDbInstanceCommand;
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
        var recoveryFile = new InstancesRecoveryFile(InstancesRecoveryFile.DEFAULT_PATH  );
        recoveryFile.writeInstance(instance);
        return instance;
    }
}
