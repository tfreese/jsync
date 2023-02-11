// Created: 06.04.2018
package de.freese.jsync.test;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import de.freese.jsync.utils.JSyncUtils;

/**
 * @author Thomas Freese
 */
public abstract class AbstractJSyncIoTest {
    protected static final Path PATH_QUELLE = Paths.get(System.getProperty("java.io.tmpdir"), "java", "quelle");

    protected static final Path PATH_ZIEL = Paths.get(System.getProperty("java.io.tmpdir"), "java", "ziel");

    @AfterEach
    public void afterEach() throws Exception {
        System.out.println("Delete Source and Target Paths...\n");
        JSyncUtils.delete(PATH_QUELLE.getParent(), false);
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        System.out.println("Prepare Source and Target Paths...\n");

        long delay = 1000L;

        // Create Source-Files.
        Path path = PATH_QUELLE;
        Path pathFile = path.resolve("file.txt");

        if (Files.notExists(pathFile)) {
            Files.createDirectories(path);

            try (PrintWriter writer = new PrintWriter(new FileOutputStream(pathFile.toFile()))) {
                writer.print("file.txt");
            }
        }

        TimeUnit.MILLISECONDS.sleep(delay);

        path = PATH_QUELLE.resolve("v1");
        pathFile = path.resolve("file.txt");

        if (Files.notExists(pathFile)) {
            Files.createDirectories(path);

            try (PrintWriter writer = new PrintWriter(new FileOutputStream(pathFile.toFile()))) {
                writer.print("file1.txt");
            }
        }

        TimeUnit.MILLISECONDS.sleep(delay);

        pathFile = PATH_QUELLE.resolve("largeFile.bin");

        if (Files.notExists(pathFile)) {
            try (RandomAccessFile raf = new RandomAccessFile(pathFile.toFile(), "rw")) {
                // 32 MB and some Bytes...
                raf.setLength((1024 * 1024 * 32) + 1024);
            }
        }

        TimeUnit.MILLISECONDS.sleep(delay);

        // Create Target-Files.
        path = PATH_ZIEL.resolve("v2");
        pathFile = path.resolve("file.txt");

        if (Files.notExists(pathFile)) {
            Files.createDirectories(path);

            try (PrintWriter writer = new PrintWriter(new FileOutputStream(pathFile.toFile()))) {
                writer.print("file.txt");
            }
        }

        TimeUnit.MILLISECONDS.sleep(delay);

        pathFile = path.resolve("file2.txt");

        if (Files.notExists(pathFile)) {
            try (PrintWriter writer = new PrintWriter(new FileOutputStream(pathFile.toFile()))) {
                writer.print("file2.txt");
            }
        }
    }
}
