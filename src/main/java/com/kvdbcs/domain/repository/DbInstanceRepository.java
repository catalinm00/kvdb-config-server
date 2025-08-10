package com.kvdbcs.domain.repository;

import com.kvdbcs.domain.model.DbInstance;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class DbInstanceRepository {
    private final List<DbInstance> dbInstances;

    public DbInstanceRepository() {
        this.dbInstances = new ArrayList<>();
    }

    public Optional<DbInstance> findById(int id) {
        return Optional.ofNullable(dbInstances.get(id));
    }

    public DbInstance save(DbInstance dbInstance) {
        var instance = new DbInstance(generateId(), dbInstance.getHost(), dbInstance.getPort(), dbInstance.isDeleted());
        dbInstances.add(instance);
        return instance;
    }

    private long generateId() {
        long a = dbInstances.isEmpty()? 1 : dbInstances.getLast().getId() + 1L;
        return Math.max(dbInstances.size() + 1L, a);
    }

    public void delete(DbInstance dbInstance) {
        dbInstances.remove(dbInstance);
    }

    public List<DbInstance> findAll() {
        return new ArrayList<>(dbInstances);
    }

    public Optional<DbInstance> findByHost(String host) {
        return dbInstances.stream()
                .filter(dbInstance -> dbInstance.getHost().equals(host))
                .findFirst();
    }
}
