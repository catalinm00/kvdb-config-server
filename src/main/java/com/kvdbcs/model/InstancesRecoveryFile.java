package com.kvdbcs.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class InstancesRecoveryFile {
    public static String DEFAULT_DIR = "logs/recovery/";
    public static String DEFAULT_NAME = "recovery.log";
    private final String path;
    private final String name;
    private File file;
    ObjectMapper mapper;

    public static boolean exists(String path) {
        return new File(path).exists();
    }

    public InstancesRecoveryFile(String path, String name) throws IOException {
        this.path = path;
        this.name = name;
        if(!exists(path + name)) createFile(path, name);
        this.mapper = new ObjectMapper();
    }

    private void createFile(String path, String name) throws IOException {
        this.file = new File(path + File.separator + name);
        file.setReadable(true);
        file.setWritable(true);
        file.createNewFile();
    }

    public List<DbInstance> readInstances() {
        List<DbInstance> instances = new ArrayList<DbInstance>();
        try {
            Scanner scanner = new Scanner(new FileInputStream(file));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                DbInstance instance = mapper.readValue(line, DbInstance.class);
                if (!instance.isDeleted()) instances.add(instance);
            }
            return instances;
        } catch (FileNotFoundException | JsonProcessingException e) {
            return List.of();
        }
    }

    public void writeInstance(DbInstance instance) {
        try {
            String mappedInstance = mapper.writeValueAsString(instance);
            Files.writeString(file.toPath(), mappedInstance, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            return;
        }
    }

    public void deleteInstance(DbInstance instance) {
        var instances = readInstances();
        if (!instances.contains(instance)) return;

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
            this.file = null;
            Files.deleteIfExists(file.toPath());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
