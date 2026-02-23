package it.baddy.uni.utils;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class FileUtils {

    private FileUtils() {
        // utility class
    }

    // 1. Conta file (non ricorsivo)
    public static long countFiles(Path dir) {
        try (Stream<Path> paths = Files.list(dir)) {
            return paths.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            throw new RuntimeException("Error counting files in " + dir, e);
        }
    }

    // 2. Conta file (ricorsivo)
    public static long countFilesRecursive(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            throw new RuntimeException("Error counting files recursively in " + dir, e);
        }
    }

    // 3. Legge file
    public static byte[] readFile(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file " + file, e);
        }
    }

    // 4. Scrive file (overwrite)
    public static void writeFile(Path file, byte[] content) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(file, content, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Error writing file " + file, e);
        }
    }

    public static boolean updateFile(Path file, byte[] content){
        boolean created = false;
        if (Files.exists(file)) {
            System.out.println("File exists, updating: " + file);
        } else {
            System.out.println("File does not exist, creating: " + file);
            created = true;
        }
        writeFile(file, content);
        return created;
    }

    // 5. Cancella file
    public static boolean deleteFile(Path file) {
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Error deleting file " + file, e);
        }
    }

    // 7.HASHING / CHORD -> il filename viene trasformato in KeyID (SHA-1)
    public static BigInteger hashFilename(String filename) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(filename.getBytes());
            return new BigInteger(1, hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available", e);
        }
    }

    // 8. Indicizza file come (KeyID -> Path)
    public static Map<BigInteger, Path> indexFiles(Path dir) {
        Map<BigInteger, Path> index = new HashMap<>();

        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(Files::isRegularFile)
                    .forEach(file -> {
                        BigInteger key = hashFilename(file.getFileName().toString());
                        index.put(key, file);
                    });
        } catch (IOException e) {
            throw new RuntimeException("Error indexing files in " + dir, e);
        }

        return index;
    }
}
