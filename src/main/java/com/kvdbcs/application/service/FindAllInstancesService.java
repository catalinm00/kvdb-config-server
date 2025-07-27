package com.kvdbcs.application.service;

import com.kvdbcs.domain.model.DbInstance;
import com.kvdbcs.domain.repository.DbInstanceRepository;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class FindAllInstancesService {
    private final DbInstanceRepository repository;

    public FindAllInstancesService(DbInstanceRepository repository) {
        this.repository = repository;
    }

    public List<DbInstance> execute() {
        return repository.findAll().stream()
                .filter(inst -> !inst.isDeleted())
                .toList();
    }
}
