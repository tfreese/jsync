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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    private static final Path PATH_DEST = createDestPath(TestJSyncGenerator.class);
    private static final Path PATH_SOURCE = createSourcePath(TestJSyncGenerator.class);

    @AfterEach
    void afterEach() throws Exception {
        deletePaths(PATH_SOURCE, PATH_DEST);
    }

    @BeforeEach
    void beforeEach() throws Exception {
        createSourceStructure(PATH_SOURCE);
    }

    @Test
    void testFileAttributes() throws Exception {
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
    void testFilter() throws Exception {
        PathFilter filter = new PathFilterEndsWith(Set.of("src", "target", ".settings"), Set.of(".classpath", ".project"));

        // @formatter:off
        Map<String, SyncItem> map = new DefaultGenerator().generateItems(System.getProperty("user.dir"), false, filter)
                .collectMap(SyncItem::getRelativePath)
                .block()
                ;
        // @formatter:on

        assertTrue(map.size() >= 1);

        assertTrue(map.keySet().stream().noneMatch(path -> path.endsWith(".classpath")));
        assertTrue(map.keySet().stream().noneMatch(path -> path.endsWith(".project")));
        assertTrue(map.keySet().stream().noneMatch(path -> path.contains("src/")));
        assertTrue(map.keySet().stream().noneMatch(path -> path.contains("target/")));
        assertTrue(map.keySet().stream().noneMatch(path -> path.contains(".settings")));
    }

    @Test
    void testGenerator() throws Exception {
        List<SyncItem> syncItems = new ArrayList<>();

        new DefaultGenerator().generateItems(PATH_SOURCE.toString(), false, PathFilterNoOp.INSTANCE, syncItems::add);

        assertEquals(7, syncItems.size());
    }
}
