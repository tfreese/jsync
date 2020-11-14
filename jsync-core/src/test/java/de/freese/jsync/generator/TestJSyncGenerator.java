/**
 * Created: 22.10.2016
 */

package de.freese.jsync.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import de.freese.jsync.AbstractJSyncTest;
import de.freese.jsync.Options;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
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
        List<SyncItem> syncItems = new ArrayList<>();
        generator.generateItems(base.toString(), false, syncItems::add);

        assertNotNull(syncItems);

        System.out.printf("Anzahl SyncItems: %d%n", syncItems.size());

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
        List<SyncItem> syncItems = new ArrayList<>();
        generator.generateItems(base.toString(), false, syncItems::add);

        assertNotNull(syncItems);

        System.out.printf("Anzahl SyncItems: %d%n", syncItems.size());

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

        Path path = Paths.get(System.getProperty("user.dir"), "pom.xml");

        DefaultGenerator generator = new DefaultGenerator();
        SyncItem syncItem = generator.generateItem(path, "pom.xml", new LinkOption[]
        {
                LinkOption.NOFOLLOW_LINKS
        });

        assertTrue(syncItem.getLastModifiedTime() > 0);
        assertTrue(syncItem.getSize() > 0);

        if (Options.IS_LINUX)
        {
            assertNotNull(syncItem.getPermissions());

            assertNotNull(syncItem.getGroup());
            assertNotNull(syncItem.getGroup().getName());
            assertTrue(syncItem.getGroup().getGid() > (Group.ROOT.getGid() - 1));
            assertTrue(syncItem.getGroup().getGid() < (Group.ID_MAX + 1));
            assertEquals("tommy", syncItem.getGroup().getName());
            assertEquals(1000, syncItem.getGroup().getGid()); // tommy

            assertNotNull(syncItem.getUser());
            assertNotNull(syncItem.getUser().getName());
            assertTrue(syncItem.getUser().getUid() > (User.ROOT.getUid() - 1));
            assertTrue(syncItem.getUser().getUid() < (User.ID_MAX + 1));
            assertEquals("tommy", syncItem.getUser().getName());
            assertEquals(1000, syncItem.getUser().getUid()); // tommy
        }
    }
}
