package com.kvdbcs.domain.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class DbInstance {
    private final String host;
    private final int port;
    private long id;
    private boolean deleted;

    public DbInstance(long id, String host, int port, boolean deleted) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.deleted = deleted;
    }

    public DbInstance(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public long getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void delete() {
        deleted = true;
    }

    public String toString() {
        return "[id=%d, host=%s, port=%d, deleted=%s]".formatted(id, host, port, deleted);
    }
}
