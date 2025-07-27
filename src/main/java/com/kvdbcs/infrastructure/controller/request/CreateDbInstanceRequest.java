package com.kvdbcs.infrastructure.controller.request;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record CreateDbInstanceRequest(String host, int port) {
}
