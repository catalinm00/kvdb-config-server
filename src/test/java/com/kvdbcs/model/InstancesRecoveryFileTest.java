package com.kvdbcs.model;

import com.kvdbcs.domain.model.DbInstance;
import com.kvdbcs.domain.model.InstancesRecoveryFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstancesRecoveryFileTest {
    
    @TempDir
    Path tempDir;
    
    private Path testFilePath;
    private InstancesRecoveryFile recoveryFile;
    
    @BeforeEach
    void setUp() throws IOException {
        testFilePath = tempDir.resolve("test-recovery.log");
        recoveryFile = new InstancesRecoveryFile(testFilePath.toString());
    }
    
    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(testFilePath)) {
            Files.delete(testFilePath);
        }
    }
    
    @Test
    void testConstructor_CreatesFileIfNotExists() throws IOException {
        // Arrange
        Path newFilePath = tempDir.resolve("new-recovery.log");
        
        // Act
        InstancesRecoveryFile newRecoveryFile = new InstancesRecoveryFile(newFilePath.toString());
        
        // Assert
        assertTrue(Files.exists(newFilePath));
        
        // Cleanup
        Files.delete(newFilePath);
    }
    
    @Test
    void testConstructor_DoesNotCreateIfExists() throws IOException {
        // Arrange
        Path existingFile = tempDir.resolve("existing-recovery.log");
        Files.createFile(existingFile);
        Files.writeString(existingFile, "existing content");
        
        // Act
        InstancesRecoveryFile newRecoveryFile = new InstancesRecoveryFile(existingFile.toString());
        
        // Assert
        assertEquals("existing content", Files.readString(existingFile));
        
        // Cleanup
        Files.delete(existingFile);
    }
    
    @Test
    void testExists_ReturnsTrueForExistingFile() {
        // Assert
        assertTrue(InstancesRecoveryFile.exists(testFilePath.toString()));
    }
    
    @Test
    void testExists_ReturnsFalseForNonExistingFile() {
        // Arrange
        String nonExistingPath = tempDir.resolve("non-existing.log").toString();
        
        // Assert
        assertFalse(InstancesRecoveryFile.exists(nonExistingPath));
    }
    
    @Test
    void testWriteInstance_WritesInstanceToFile() throws IOException {
        // Arrange
        DbInstance instance = createTestInstance(1L, "localhost", 8080, false);
        
        // Act
        recoveryFile.writeInstance(instance);
        
        // Assert
        String fileContent = Files.readString(testFilePath);
        assertTrue(fileContent.contains("\"id\":1"));
        assertTrue(fileContent.contains("\"host\":\"localhost\""));
        assertTrue(fileContent.contains("\"port\":8080"));
        assertTrue(fileContent.contains("\"deleted\":false"));
    }
    
    @Test
    void testWriteInstance_AppendsMultipleInstances() throws IOException {
        // Arrange
        DbInstance instance1 = createTestInstance(1L, "localhost", 8080, false);
        DbInstance instance2 = createTestInstance(2L, "127.0.0.1", 9090, false);
        
        // Act
        recoveryFile.writeInstance(instance1);
        recoveryFile.writeInstance(instance2);
        
        // Assert
        List<String> lines = Files.readAllLines(testFilePath);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("\"id\":1"));
        assertTrue(lines.get(1).contains("\"id\":2"));
    }
    
    @Test
    void testReadInstances_ReturnsEmptyListForEmptyFile() {
        // Act
        List<DbInstance> instances = recoveryFile.readInstances();
        
        // Assert
        assertTrue(instances.isEmpty());
    }
    
    @Test
    void testReadInstances_ReturnsInstancesFromFile() throws IOException {
        // Arrange
        DbInstance instance1 = createTestInstance(1L, "localhost", 8080, false);
        DbInstance instance2 = createTestInstance(2L, "127.0.0.1", 9090, false);
        recoveryFile.writeInstance(instance1);
        recoveryFile.writeInstance(instance2);
        
        // Act
        List<DbInstance> instances = recoveryFile.readInstances();
        
        // Assert
        assertEquals(2, instances.size());
        assertTrue(instances.stream().anyMatch(i -> i.getId() == 1L));
        assertTrue(instances.stream().anyMatch(i -> i.getId() == 2L));
    }
    
    @Test
    void testReadInstances_HandlesDeletedInstances() throws IOException {
        // Arrange
        DbInstance instance1 = createTestInstance(1L, "localhost", 8080, false);
        DbInstance instance2 = createTestInstance(2L, "127.0.0.1", 9090, true); // deleted
        recoveryFile.writeInstance(instance1);
        recoveryFile.writeInstance(instance2);
        
        // Act
        List<DbInstance> instances = recoveryFile.readInstances();
        
        // Assert
        assertEquals(1, instances.size());
        assertEquals(1L, instances.get(0).getId());
        assertFalse(instances.get(0).isDeleted());
    }
    
    @Test
    void testReadInstances_HandlesMultipleVersionsOfSameInstance() throws IOException {
        // Arrange
        DbInstance instance1 = createTestInstance(1L, "localhost", 8080, false);
        DbInstance instance1Updated = createTestInstance(1L, "localhost", 8090, false); // Different port
        DbInstance instance1Deleted = createTestInstance(1L, "localhost", 8080, true);
        
        recoveryFile.writeInstance(instance1);
        recoveryFile.writeInstance(instance1Updated);
        recoveryFile.writeInstance(instance1Deleted);
        
        // Act
        List<DbInstance> instances = recoveryFile.readInstances();
        
        // Assert
        assertTrue(instances.isEmpty()); // Porque la última versión está marcada como deleted
    }
    
    @Test
    void testReadInstances_ReturnsLatestVersionOfInstance() throws IOException {
        // Arrange
        DbInstance instance1 = createTestInstance(1L, "localhost", 8080, false);
        DbInstance instance1Updated = createTestInstance(1L, "localhost", 9090, false); // Different port
        
        recoveryFile.writeInstance(instance1);
        recoveryFile.writeInstance(instance1Updated);
        
        // Act
        List<DbInstance> instances = recoveryFile.readInstances();
        
        // Assert
        assertEquals(1, instances.size());
        assertEquals(9090, instances.get(0).getPort());
    }
    
    @Test
    void testReadInstances_HandlesInvalidJson() throws IOException {
        // Arrange
        Files.writeString(testFilePath, "invalid json line\n");
        Files.writeString(testFilePath, "1>>{\"id\":1,\"host\":\"localhost\",\"port\":8080,\"deleted\":false}\n",
                         java.nio.file.StandardOpenOption.APPEND);
        
        // Act
        List<DbInstance> instances = recoveryFile.readInstances();
        
        // Assert
        assertEquals(1, instances.size());
        assertEquals("localhost", instances.get(0).getHost());
    }
    
    @Test
    void testDeleteInstance_MarksInstanceAsDeleted() throws IOException {
        // Arrange
        DbInstance instance = createTestInstance(1L, "localhost", 8080, false);
        recoveryFile.writeInstance(instance);
        
        // Act
        recoveryFile.deleteInstance(instance);
        
        // Assert
        List<DbInstance> instances = recoveryFile.readInstances();
        assertTrue(instances.isEmpty()); // Porque se marcó como deleted
        
        // Verificar que se escribió la versión deleted
        List<String> lines = Files.readAllLines(testFilePath);
        assertEquals(2, lines.size()); // Original + deleted version
        assertTrue(lines.get(1).contains("\"deleted\":true"));
    }
    
    @Test
    void testDeleteInstance_DoesNothingForNonExistentInstance() throws IOException {
        // Arrange
        DbInstance existingInstance = createTestInstance(1L, "localhost", 8080, false);
        DbInstance nonExistentInstance = createTestInstance(2L, "127.0.0.1", 9090, false);
        recoveryFile.writeInstance(existingInstance);
        
        // Act
        recoveryFile.deleteInstance(nonExistentInstance);
        
        // Assert
        List<DbInstance> instances = recoveryFile.readInstances();
        assertEquals(1, instances.size());
        assertEquals(1L, instances.get(0).getId());
    }
    
    @Test
    void testDelete_RemovesFile() {
        // Act
        boolean result = recoveryFile.delete();
        
        // Assert
        assertTrue(result);
        assertFalse(Files.exists(testFilePath));
    }
    
    @Test
    void testDelete_ReturnsFalseForNonExistentFile() throws IOException {
        // Arrange
        Files.delete(testFilePath); // Delete the file first
        
        // Act
        boolean result = recoveryFile.delete();
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void testDefaultPath_IsCorrect() {
        // Act & Assert
        String expectedPath = Paths.get("logs/recovery/", "recovery.log").toAbsolutePath().toString();
        assertEquals(expectedPath, InstancesRecoveryFile.DEFAULT_PATH);
    }
    
    @Test
    void testCreateFile_CreatesDirectories() throws IOException {
        // Arrange
        Path nestedPath = tempDir.resolve("nested/directory/structure/recovery.log");
        
        // Act
        InstancesRecoveryFile nestedRecoveryFile = new InstancesRecoveryFile(nestedPath.toString());
        
        // Assert
        assertTrue(Files.exists(nestedPath));
        assertTrue(Files.isDirectory(nestedPath.getParent()));
        
        // Cleanup
        Files.delete(nestedPath);
        Files.delete(nestedPath.getParent());
        Files.delete(nestedPath.getParent().getParent());
        Files.delete(nestedPath.getParent().getParent().getParent());
    }
    
    @Test
    void testReadInstances_HandlesEmptyLines() throws IOException {
        // Arrange
        DbInstance instance = createTestInstance(1L, "localhost", 8080, false);
        Files.writeString(testFilePath, "\n"); // Empty line
        recoveryFile.writeInstance(instance);
        Files.writeString(testFilePath, "\n", java.nio.file.StandardOpenOption.APPEND); // Another empty line
        
        // Act
        List<DbInstance> instances = recoveryFile.readInstances();
        
        // Assert
        assertEquals(1, instances.size());
        assertEquals(1L, instances.get(0).getId());
    }
    
    // Helper method to create test instances
    private DbInstance createTestInstance(long id, String host, int port, boolean deleted) {
        return new DbInstance(id, host, port, deleted);
    }
    
    private DbInstance createTestInstance(long id, String host, int port) {
        DbInstance instance = new DbInstance(host, port);
        // Note: We need to use reflection to set the ID since there's no setter
        try {
            java.lang.reflect.Field idField = DbInstance.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(instance, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID for test instance", e);
        }
        return instance;
    }
}