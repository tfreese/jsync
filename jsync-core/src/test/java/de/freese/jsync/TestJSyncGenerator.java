/**
 * Created: 22.10.2016
 */

package de.freese.jsync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncItemMeta;
import de.freese.jsync.model.User;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class TestJSyncGenerator extends AbstractJSyncTest
{
    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void test010GeneratorQuelle() throws Exception
    {
        System.out.println();

        Path base = PATH_QUELLE;
        System.out.printf("Quelle: %s%n", base);

        Generator generator = new DefaultGenerator();
        List<SyncItem> syncItems = generator.generateItems(base.toString(), false, GENERATORLISTENER);

        assertNotNull(syncItems);
        assertEquals(5, syncItems.size());

        syncItems.forEach(syncItem -> System.out.printf("%s%n", syncItem));
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void test020GeneratorZiel() throws Exception
    {
        System.out.println();

        Path base = PATH_ZIEL;
        System.out.printf("Ziel: %s%n", base);

        Generator generator = new DefaultGenerator();
        List<SyncItem> syncItems = generator.generateItems(base.toString(), false, GENERATORLISTENER);

        assertNotNull(syncItems);
        assertEquals(4, syncItems.size());

        syncItems.forEach(syncItem -> System.out.printf("%s%n", syncItem));
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void test030FileAttributes() throws Exception
    {
        System.out.println();

        Path base = Paths.get(System.getProperty("user.dir"));

        Generator generator = new DefaultGenerator();
        SyncItemMeta meta = generator.generateMeta(base.toString(), base.resolve("pom.xml").toString(), false, false, GENERATORLISTENER);

        assertTrue(meta.getLastModifiedTime() > 0);
        assertTrue(meta.getSize() > 0);

        if (Options.IS_LINUX)
        {
            assertNotNull(meta.getPermissions());

            assertNotNull(meta.getGroup());
            assertNotNull(meta.getGroup().getName());
            assertTrue(meta.getGroup().getGid() > (Group.ROOT.getGid() - 1));
            assertTrue(meta.getGroup().getGid() < (Group.ID_MAX + 1));
            assertEquals("tommy", meta.getGroup().getName());
            assertEquals(1000, meta.getGroup().getGid()); // tommy

            assertNotNull(meta.getUser());
            assertNotNull(meta.getUser().getName());
            assertTrue(meta.getUser().getUid() > (User.ROOT.getUid() - 1));
            assertTrue(meta.getUser().getUid() < (User.ID_MAX + 1));
            assertEquals("tommy", meta.getUser().getName());
            assertEquals(1000, meta.getUser().getUid()); // tommy
        }
    }
}
