// Created: 06.04.2018
package de.freese.jsync;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.utils.JSyncUtils;

/**
 * @author Thomas Freese
 */
public abstract class AbstractJSyncTest
{
    /**
     * @author Thomas Freese
     */
    private static class TestGeneratorListener implements GeneratorListener
    {
        /**
         *
         */
        private final String countLogFormat;

        /**
         *
         */
        private final String syncItemLogFormat;

        /**
         * Erstellt ein neues {@link TestGeneratorListener} Object.
         *
         * @param countLogFormat String; @see {@link String#format(String, Object...)}
         * @param syncItemLogFormat String; @see {@link String#format(String, Object...)}
         */
        public TestGeneratorListener(final String countLogFormat, final String syncItemLogFormat)
        {
            super();

            this.countLogFormat = Objects.requireNonNull(countLogFormat, "countLogFormat required");
            this.syncItemLogFormat = Objects.requireNonNull(syncItemLogFormat, "syncItemLogFormat required");
        }

        /**
         * @see de.freese.jsync.generator.listener.GeneratorListener#checksum(long, long)
         */
        @Override
        public void checksum(final long size, final long bytesRead)
        {
            // Empty
        }

        /**
         * @see de.freese.jsync.generator.listener.GeneratorListener#pathCount(java.nio.file.Path, int)
         */
        @Override
        public void pathCount(final Path path, final int pathCount)
        {
            System.out.printf(this.countLogFormat, path, pathCount);
        }

        /**
         * @see de.freese.jsync.generator.listener.GeneratorListener#processingSyncItem(de.freese.jsync.model.SyncItem)
         */
        @Override
        public void processingSyncItem(final SyncItem syncItem)
        {
            System.out.printf(this.syncItemLogFormat, syncItem);
        }
    }

    /**
    *
    */
    protected static final GeneratorListener GENERATORLISTENER = new TestGeneratorListener("Size of SyncItems in %s: %d%n", "Current SyncItem: %s%n");

    /**
     *
     */
    protected static final GeneratorListener GENERATORLISTENER_SOURCE =
            new TestGeneratorListener("Source size of SyncItems in %s: %d%n", "Source current SyncItem: %s%n");

    /**
     *
     */
    protected static final GeneratorListener GENERATORLISTENER_TARGET =
            new TestGeneratorListener("Target size of SyncItems in %s: %d%n", "Target current SyncItem: %s%n");

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
     * Verzeichnis-Struktur zum Testen löschen.
     *
     * @throws Exception Falls was schief geht.
     */
    @AfterAll
    public static void afterAll() throws Exception
    {
        JSyncUtils.deleteDirectoryRecursive(PATH_BASE);
    }

    /**
     * Verzeichnis-Struktur zum Testen aufbauen.
     *
     * @throws Exception Falls was schief geht.
     */
    @BeforeAll
    public static void beforeAll() throws Exception
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
                // 512 MB
                raf.setLength(1024 * 1024 * 512);
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

    /**
     * Erzeugt eine neue Instanz von {@link AbstractJSyncTest}.
     */
    protected AbstractJSyncTest()
    {
        super();
    }
}
