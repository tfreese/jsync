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
public abstract class AbstractJSyncIoTest
{
    /**
     * Paths.get(System.getProperty("java.io.tmpdir"), "jsync")<br>
     * Paths.get(System.getProperty("user.dir"), "target")
     */
    private static final Path PATH_BASE = Paths.get(System.getProperty("java.io.tmpdir"), "java");

    /**
     *
     */
    protected static final Path PATH_QUELLE = PATH_BASE.resolve("quelle");

    /**
     *
     */
    protected static final Path PATH_ZIEL = PATH_BASE.resolve("ziel");

    /**
     * Erzeugt eine neue Instanz von {@link AbstractJSyncIoTest}.
     */
    protected AbstractJSyncIoTest()
    {
        super();
    }

    /**
     * Verzeichnis-Struktur zum Testen löschen.
     *
     * @throws Exception Falls was schief geht.
     */
    @AfterEach
    public void afterEach() throws Exception
    {
        System.out.println("Delete Source and Target Paths...\n");
        JSyncUtils.delete(PATH_BASE, false);
    }

    /**
     * Verzeichnis-Struktur zum Testen aufbauen.
     *
     * @throws Exception Falls was schief geht.
     */
    @BeforeEach
    public void beforeEach() throws Exception
    {
        System.out.println("Prepare Source and Target Paths...\n");

        long delay = 1000L;

        // Quell-Dateien anlegen
        Path path = PATH_QUELLE;
        Path pathFile = path.resolve("file.txt");

        if (Files.notExists(pathFile))
        {
            Files.createDirectories(path);

            try (PrintWriter writer = new PrintWriter(new FileOutputStream(pathFile.toFile())))
            {
                writer.print("file.txt");
            }
        }

        TimeUnit.MILLISECONDS.sleep(delay);

        path = PATH_QUELLE.resolve("v1");
        pathFile = path.resolve("file.txt");

        if (Files.notExists(pathFile))
        {
            Files.createDirectories(path);

            try (PrintWriter writer = new PrintWriter(new FileOutputStream(pathFile.toFile())))
            {
                writer.print("file1.txt");
            }
        }

        TimeUnit.MILLISECONDS.sleep(delay);

        pathFile = PATH_QUELLE.resolve("largeFile.bin");

        if (Files.notExists(pathFile))
        {
            try (RandomAccessFile raf = new RandomAccessFile(pathFile.toFile(), "rw"))
            {
                // 32 MB und ein paar zerquetschte...
                raf.setLength((1024 * 1024 * 32) + 1024);
            }
        }

        TimeUnit.MILLISECONDS.sleep(delay);

        // Ziel-Dateien anlegen
        path = PATH_ZIEL.resolve("v2");
        pathFile = path.resolve("file.txt");

        if (Files.notExists(pathFile))
        {
            Files.createDirectories(path);

            try (PrintWriter writer = new PrintWriter(new FileOutputStream(pathFile.toFile())))
            {
                writer.print("file.txt");
            }
        }

        TimeUnit.MILLISECONDS.sleep(delay);

        pathFile = path.resolve("file2.txt");

        if (Files.notExists(pathFile))
        {
            try (PrintWriter writer = new PrintWriter(new FileOutputStream(pathFile.toFile())))
            {
                writer.print("file2.txt");
            }
        }
    }
}
