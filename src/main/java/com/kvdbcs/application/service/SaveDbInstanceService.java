package com.kvdbcs.application.service;

import com.kvdbcs.domain.event.UpdatedInstanceEvent;
import com.kvdbcs.domain.model.DbInstance;
import com.kvdbcs.domain.model.InstancesRecoveryFile;
import com.kvdbcs.domain.repository.DbInstanceRepository;
import com.kvdbcs.application.service.command.SaveDbInstanceCommand;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Objects;

@Singleton
public class SaveDbInstanceService {
    private final DbInstanceRepository dbInstanceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SaveDbInstanceService(DbInstanceRepository dbInstanceRepository, ApplicationEventPublisher eventPublisher) {
        this.dbInstanceRepository = dbInstanceRepository;
        this.eventPublisher = eventPublisher;
    }

    public DbInstance execute(SaveDbInstanceCommand command) throws IOException {
        if (command.host() == null || command.host().isEmpty()) {
            throw new IllegalArgumentException("host is required");
        }
        if (command.port() < 0) {
            throw new IllegalArgumentException("valid port is required");
        }

        var found = this.dbInstanceRepository.findAll().stream()
                .filter(i -> Objects.equals(i.getHost(), command.host())
                        && Objects.equals(i.getPort(), command.port())).findFirst();
        if (found.isPresent()) { return found.get(); }

        var instance = dbInstanceRepository.save(new DbInstance(command.host(), command.port()));
        var recoveryFile = new InstancesRecoveryFile(InstancesRecoveryFile.DEFAULT_PATH  );
        recoveryFile.writeInstance(instance);

        eventPublisher.publishEvent(new UpdatedInstanceEvent(instance));
        return instance;
    }
}
