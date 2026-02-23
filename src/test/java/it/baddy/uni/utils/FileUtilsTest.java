package it.baddy.uni.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void countFilesNonRecursive() throws Exception {
        Files.createFile(tempDir.resolve("a.txt"));
        Files.createFile(tempDir.resolve("b.txt"));
        Files.createDirectory(tempDir.resolve("sub"));

        assertEquals(2, FileUtils.countFiles(tempDir));
    }

    @Test
    void countFilesRecursive() throws Exception {
        Files.createFile(tempDir.resolve("a.txt"));
        Path sub = Files.createDirectory(tempDir.resolve("sub"));
        Files.createFile(sub.resolve("b.txt"));

        assertEquals(2, FileUtils.countFilesRecursive(tempDir));
    }

    @Test
    void writeAndReadFile() {
        Path file = tempDir.resolve("test.txt");
        byte[] content = "hello chord".getBytes();

        FileUtils.writeFile(file, content);
        byte[] read = FileUtils.readFile(file);

        assertArrayEquals(content, read);
    }

    @Test
    void updateAndReadFile() {
        Path file = tempDir.resolve("test.txt");
        byte[] content = "hello chord".getBytes();

        FileUtils.writeFile(file, content);
        //modifica file
        byte[] newContent = "hello chord updated".getBytes();
        boolean created = FileUtils.updateFile(file, newContent);

        byte[] read = FileUtils.readFile(file);
        assertFalse(created);
        assertArrayEquals(newContent, read);
    }

    @Test
    void deleteFileWorks() throws Exception {
        Path file = tempDir.resolve("toDelete.txt");
        Files.createFile(file);

        assertTrue(FileUtils.deleteFile(file));
        assertFalse(Files.exists(file));
    }

    @Test
    void hashFilenameIsDeterministic() {
        BigInteger h1 = FileUtils.hashFilename("file.txt");
        BigInteger h2 = FileUtils.hashFilename("file.txt");
        System.out.println("hash: "+ h1);
        assertEquals(h1, h2);
    }

    @Test
    void indexFilesWorks() throws Exception {
        Files.createFile(tempDir.resolve("a.txt"));
        Files.createFile(tempDir.resolve("b.txt"));

        Map<BigInteger, Path> index = FileUtils.indexFiles(tempDir);

        assertEquals(2, index.size());
    }
}

