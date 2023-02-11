// Created: 22.10.2016
package de.freese.jsync.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.filter.PathFilterEndsWith;
import de.freese.jsync.filter.PathFilterNoOp;
import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
class TestJSyncGenerator extends AbstractJSyncIoTest {
    @Test
    @Order(40)
    void testFileAttributes() throws Exception {
        System.out.println();

        // @formatter:off
        SyncItem syncItem = new DefaultGenerator().generateItems(System.getProperty("user.dir"), false, PathFilterNoOp.INSTANCE)
                .filter(si -> si.getRelativePath().endsWith("pom.xml"))
                .blockFirst()
                ;
        // @formatter:on

        assertNotNull(syncItem);
        assertTrue(syncItem.getLastModifiedTime() > 0);
        assertTrue(syncItem.getSize() > 0);

        //        if (Options.IS_LINUX)
        //        {
        //            assertNotNull(syncItem.getPermissions());
        //
        //            assertNotNull(syncItem.getGroup());
        //            assertNotNull(syncItem.getGroup().getName());
        //            assertTrue(syncItem.getGroup().getGid() > (Group.ROOT.getGid() - 1));
        //            assertTrue(syncItem.getGroup().getGid() < (Group.ID_MAX + 1));
        //            assertEquals("tommy", syncItem.getGroup().getName());
        //            assertEquals(1000, syncItem.getGroup().getGid()); // tommy
        //
        //            assertNotNull(syncItem.getUser());
        //            assertNotNull(syncItem.getUser().getName());
        //            assertTrue(syncItem.getUser().getUid() > (User.ROOT.getUid() - 1));
        //            assertTrue(syncItem.getUser().getUid() < (User.ID_MAX + 1));
        //            assertEquals("tommy", syncItem.getUser().getName());
        //            assertEquals(1000, syncItem.getUser().getUid()); // tommy
        //        }
    }

    @Test
    @Order(30)
    void testFilter() throws Exception {
        System.out.println();

        PathFilter filter = new PathFilterEndsWith(Set.of("src", "target", ".settings"), Set.of(".classpath", ".project"));

        // @formatter:off
        Map<String, SyncItem> map = new DefaultGenerator().generateItems(System.getProperty("user.dir"), false, filter)
                .collectMap(SyncItem::getRelativePath)
                .block()
                ;
        // @formatter:on

        assertTrue(map.size() >= 1);

        map.keySet().stream().sorted().forEach(System.out::println);

        assertTrue(map.keySet().stream().noneMatch(path -> path.endsWith(".classpath")));
        assertTrue(map.keySet().stream().noneMatch(path -> path.endsWith(".project")));
        assertTrue(map.keySet().stream().noneMatch(path -> path.contains("src/")));
        assertTrue(map.keySet().stream().noneMatch(path -> path.contains("target/")));
        assertTrue(map.keySet().stream().noneMatch(path -> path.contains(".settings")));
    }

    @Test
    @Order(10)
    void testGeneratorQuelle() throws Exception {
        System.out.println();

        Path base = PATH_QUELLE;
        System.out.printf("Quelle: %s%n", base);

        List<SyncItem> syncItems = new ArrayList<>();

        new DefaultGenerator().generateItems(base.toString(), false, PathFilterNoOp.INSTANCE, syncItems::add);

        System.out.printf("Anzahl SyncItems: %d%n", syncItems.size());

        assertEquals(4, syncItems.size());

        syncItems.forEach(syncItem -> System.out.printf("%s%n", syncItem));
    }

    @Test
    @Order(20)
    void testGeneratorZiel() throws Exception {
        System.out.println();

        Path base = PATH_ZIEL;
        System.out.printf("Ziel: %s%n", base);

        List<SyncItem> syncItems = new ArrayList<>();

        new DefaultGenerator().generateItems(base.toString(), false, PathFilterNoOp.INSTANCE, syncItems::add);

        System.out.printf("Anzahl SyncItems: %d%n", syncItems.size());

        assertEquals(3, syncItems.size());

        syncItems.forEach(syncItem -> System.out.printf("%s%n", syncItem));
    }
}
