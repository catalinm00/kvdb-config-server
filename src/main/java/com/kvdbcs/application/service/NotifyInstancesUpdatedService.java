package com.kvdbcs.application.service;

import com.kvdbcs.domain.repository.DbInstanceRepository;
import com.kvdbcs.infrastructure.client.DbInstanceClient;
import jakarta.inject.Singleton;

@Singleton
public class NotifyInstancesUpdatedService {
    private final DbInstanceRepository dbInstanceRepository;
    private final DbInstanceClient dbInstanceClient;

    public NotifyInstancesUpdatedService(DbInstanceRepository dbInstanceRepository, DbInstanceClient dbInstanceClient) {
        this.dbInstanceRepository = dbInstanceRepository;
        this.dbInstanceClient = dbInstanceClient;
    }

    public void Execute() {
        var instances = dbInstanceRepository.findAll();
        for (var instance : instances) {
            dbInstanceClient.updateInstances(instance, instances);
        }
    }
}
