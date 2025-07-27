package com.kvdbcs.domain.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InstancesRecoveryFile {
    public static final String VERSION_SEPARATOR = ">>";
    private static String DEFAULT_DIR = "logs/recovery/";
    private static String DEFAULT_NAME = "recovery.log";
    public static String DEFAULT_PATH = Paths.get(DEFAULT_DIR, DEFAULT_NAME).toAbsolutePath().toString();
    private final Path filePath;
    ObjectMapper mapper;

    public static boolean exists(String path) {
        return Files.exists(Paths.get(path));
    }

    public InstancesRecoveryFile(String path) throws IOException {
        this.filePath = Paths.get(path);
        this.mapper = new ObjectMapper();

        if (!Files.exists(filePath)) {
            createFile();
        }
    }

    private void createFile() throws IOException {
        Files.createDirectories(filePath.getParent());

        Files.createFile(filePath);
    }

    public List<DbInstance> readInstances() {
        Map<Long, DbInstance> instances = new HashMap<>();
        Map<Long, Instant> latestVersions = new HashMap<>();
        try {
            var lines = Files.readAllLines(filePath);
            for (String line : lines) {
                if (line.isEmpty()) continue;
                if (!line.contains(VERSION_SEPARATOR)) continue;
                try {
                    var separatedLine = line.split(VERSION_SEPARATOR);
                    var version = Instant.ofEpochMilli(Long.parseLong(separatedLine[0]));
                    var instanceText = separatedLine[1];
                    var json = mapper.readTree(instanceText);
                    if (!json.has("host")) continue;
                    var instance = new DbInstance(
                            json.findValue("id").asLong(),
                            json.findValue("host").asText(),
                            json.findValue("port").asInt(),
                            json.findValue("deleted").asBoolean());
                    var lastVersion = latestVersions.getOrDefault(instance.getId(), Instant.ofEpochMilli(0));
                    if (lastVersion.isAfter(version)) continue;
                    instances.put(instance.getId(), instance);
                    latestVersions.put(instance.getId(), version);
                } catch (JsonProcessingException e) {
                    // SKIP TO NEXT ELEMENT
                }
            }
            return instances.values().stream().filter(i -> !i.isDeleted()).toList();
        } catch (FileNotFoundException | JsonProcessingException e) {
            return List.of();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeInstance(DbInstance instance) {
        try {
            String mappedInstance = mapper.writeValueAsString(instance) + System.lineSeparator();
            String line = Instant.now().toEpochMilli() + VERSION_SEPARATOR + mappedInstance;
            Files.writeString(filePath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            return;
        }
    }

    public void deleteInstance(DbInstance instance) {
        var instances = readInstances();
        var mayBeInstance = instances.stream()
                .filter(inst -> Objects.equals(inst.getId(), instance.getId()))
                .findFirst();
        if (mayBeInstance.isEmpty()) return;

        var instanceToDelete = mayBeInstance.get();
        instanceToDelete.delete();
        writeInstance(instanceToDelete);
    }

    public boolean delete() {
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
}
