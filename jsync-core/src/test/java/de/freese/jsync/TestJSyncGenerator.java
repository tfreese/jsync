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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import de.freese.jsync.Options.Builder;
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
class TestJSyncGenerator extends AbstractJSyncTest
{
    /**
     * @author Thomas Freese
     */
    static class TestGenerator extends DefaultGenerator
    {
        /**
         * Erzeugt eine neue Instanz von {@link DefaultGenerator}.
         */
        TestGenerator()
        {
            super();
        }

        /**
         * @see de.freese.jsync.generator.DefaultGenerator#toItem(de.freese.jsync.Options, java.nio.file.Path, java.nio.file.Path, java.nio.file.LinkOption[])
         */
        @Override
        public SyncItem toItem(final Options options, final Path base, final Path path, final LinkOption[] linkOption)
        {
            return super.toItem(options, base, path, linkOption);
        }
    };

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void test010GeneratorQuelle() throws Exception
    {
        System.out.println();

        Path base = PATH_QUELLE;
        System.out.printf("Quelle: %s", base);

        Options options = new Builder().checksum(false).build();

        Generator generator = new DefaultGenerator();

        Map<String, SyncItem> fileMap = generator.createSyncItems(options, base, GENERATORLISTENER);

        assertNotNull(fileMap);
        assertEquals(5, fileMap.size());

        fileMap.forEach((key, value) -> System.out.printf("%s%n", key));
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void test020GeneratorZiel() throws Exception
    {
        System.out.println();

        Path base = PATH_ZIEL;
        System.out.printf("Ziel: %s", base);

        Options options = new Builder().checksum(false).build();

        Generator generator = new DefaultGenerator();

        Map<String, SyncItem> fileMap = generator.createSyncItems(options, base, GENERATORLISTENER);

        assertNotNull(fileMap);
        assertEquals(4, fileMap.size());

        fileMap.forEach((key, value) -> System.out.printf("%s%n", key));
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void test030FileAttributes() throws Exception
    {
        Path base = Paths.get(System.getProperty("user.dir"));

        Options options = new Builder().build();

        TestGenerator generator = new TestGenerator();

        LinkOption[] linkOption = options.isFollowSymLinks() ? Generator.LINKOPTION_WITH_SYMLINKS : Generator.LINKOPTION_NO_SYMLINKS;

        FileSyncItem fileSyncItem = (FileSyncItem) generator.toItem(options, base, base.resolve("pom.xml"), linkOption);

        assertTrue(fileSyncItem.getLastModifiedTime() > 0);
        assertTrue(fileSyncItem.getSize() > 0);

        if (Options.IS_LINUX)
        {
            assertNotNull(fileSyncItem.getPermissions());

            assertNotNull(fileSyncItem.getGroup());
            assertNotNull(fileSyncItem.getGroup().getName());
            assertTrue(fileSyncItem.getGroup().getGid() > (Group.ROOT.getGid() - 1));
            assertTrue(fileSyncItem.getGroup().getGid() < (Group.ID_MAX + 1));
            assertEquals("tommy", fileSyncItem.getGroup().getName());
            assertEquals(1000, fileSyncItem.getGroup().getGid()); // tommy

            assertNotNull(fileSyncItem.getUser());
            assertNotNull(fileSyncItem.getUser().getName());
            assertTrue(fileSyncItem.getUser().getUid() > (User.ROOT.getUid() - 1));
            assertTrue(fileSyncItem.getUser().getUid() < (User.ID_MAX + 1));
            assertEquals("tommy", fileSyncItem.getUser().getName());
            assertEquals(1000, fileSyncItem.getUser().getUid()); // tommy
        }
    }
}
