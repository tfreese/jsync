/**
 * Created: 22.10.2016
 */

package de.freese.jsync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
public class TestJSyncGenerator extends AbstractJSyncTest
{
    /**
     * @author Thomas Freese
     */
    public static class TestGenerator extends DefaultGenerator
    {
        /**
         * Erzeugt eine neue Instanz von {@link DefaultGenerator}.
         *
         * @param options {@link Options}
         * @param base {@link Path}
         */
        public TestGenerator(final Options options, final Path base)
        {
            super(options, base);
        }

        /**
         * @see de.freese.jsync.generator.DefaultGenerator#toItem(java.nio.file.Path, java.nio.file.LinkOption[])
         */
        @Override
        protected SyncItem toItem(final Path path, final LinkOption[] linkOption)
        {
            return super.toItem(path, linkOption);
        }
    };

    /**
     * Erstellt ein neues {@link TestJSyncGenerator} Object.
     */
    public TestJSyncGenerator()
    {
        super();
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    public void test010GeneratorQuelle() throws Exception
    {
        System.out.println();

        Path base = PATH_QUELLE;
        System.out.printf("Quelle: %s", base);

        Options options = new Options();
        options.setChecksum(false);

        Generator generator = new DefaultGenerator(options, base);

        Callable<Map<String, SyncItem>> callable = generator.createSyncItemTasks(GENERATORLISTENER);

        Map<String, SyncItem> fileMap = callable.call();

        assertNotNull(fileMap);
        assertEquals(5, fileMap.size());

        fileMap.forEach((key, value) -> System.out.printf("%s%n", key));
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    public void test020GeneratorZiel() throws Exception
    {
        System.out.println();

        Path base = PATH_ZIEL;
        System.out.printf("Ziel: %s", base);

        Options options = new Options();
        options.setChecksum(false);

        Generator generator = new DefaultGenerator(options, base);

        Callable<Map<String, SyncItem>> callable = generator.createSyncItemTasks(GENERATORLISTENER);

        Map<String, SyncItem> fileMap = callable.call();

        assertNotNull(fileMap);
        assertEquals(4, fileMap.size());

        fileMap.forEach((key, value) -> System.out.printf("%s%n", key));
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    public void test030FileAttributes() throws Exception
    {
        Path base = Paths.get(System.getProperty("user.dir"));

        Options options = new Options();

        TestGenerator generator = new TestGenerator(options, base);

        LinkOption[] linkOption = options.isFollowSymLinks() ? Generator.LINKOPTION_WITH_SYMLINKS : Generator.LINKOPTION_NO_SYMLINKS;

        FileSyncItem fileSyncItem = (FileSyncItem) generator.toItem(base.resolve("pom.xml"), linkOption);

        assertTrue(fileSyncItem.getLastModifiedTime() > 0);
        assertTrue(fileSyncItem.getSize() > 0);

        if (Options.IS_LINUX)
        {
            assertNotNull(fileSyncItem.getPermissions());

            assertNotNull(fileSyncItem.getGroup());
            assertNotNull(fileSyncItem.getGroup().getName());
            assertTrue(fileSyncItem.getGroup().getGid() > (Group.ROOT.getGid() - 1));
            assertTrue(fileSyncItem.getGroup().getGid() < (Group.ID_MAX + 1));
            assertEquals(fileSyncItem.getGroup().getName(), "tommy");
            assertEquals(fileSyncItem.getGroup().getGid(), 1000); // tommy

            assertNotNull(fileSyncItem.getUser());
            assertNotNull(fileSyncItem.getUser().getName());
            assertTrue(fileSyncItem.getUser().getUid() > (User.ROOT.getUid() - 1));
            assertTrue(fileSyncItem.getUser().getUid() < (User.ID_MAX + 1));
            assertEquals(fileSyncItem.getUser().getName(), "tommy");
            assertEquals(fileSyncItem.getUser().getUid(), 1000); // tommy
        }
    }
}
