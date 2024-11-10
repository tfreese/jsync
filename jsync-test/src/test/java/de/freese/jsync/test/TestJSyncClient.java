// Created: 18.07.2021
package de.freese.jsync.test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
import de.freese.jsync.client.listener.EmptyClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filter.PathFilterNoOp;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;

/**
 * @author Thomas Freese
 */
class TestJSyncClient extends AbstractJSyncIoTest {
    private static final Path PATH_DEST = createDestPath(TestJSyncClient.class);
    private static final Path PATH_SOURCE = createSourcePath(TestJSyncClient.class);

    private static Options options;

    /**
     * @author Thomas Freese
     */
    private static final class TestClientListener extends EmptyClientListener {
        @Override
        public void error(final String message, final Throwable th) {
            assertNull(th);
        }
    }

    @BeforeAll
    static void beforeAll() {
        options = new Builder().delete(true).checksum(true).followSymLinks(false).dryRun(false).build();
    }

    @AfterEach
    void afterEach() throws Exception {
        deletePaths(PATH_SOURCE, PATH_DEST);
    }

    @BeforeEach
    void beforeEach() throws Exception {
        createSourceStructure(PATH_SOURCE);
    }

    @Test
    void testLocalToLocal() {
        final URI senderUri = PATH_SOURCE.toUri();
        final URI receiverUri = PATH_DEST.toUri();

        syncDirectories(options, senderUri, receiverUri);

        assertTrue(true);
    }

    private void syncDirectories(final Options options, final URI senderUri, final URI receiverUri) {
        final Client client = new DefaultClient(options, senderUri, receiverUri);
        client.connectFileSystems();

        final List<SyncItem> syncItemsSender = new ArrayList<>();
        client.generateSyncItems(EFileSystem.SENDER, PathFilterNoOp.INSTANCE, syncItem -> {
            syncItemsSender.add(syncItem);
            final String checksum = client.generateChecksum(EFileSystem.SENDER, syncItem, i -> {
                // getLogger().info("Sender Bytes read: {}", i);
            });
            syncItem.setChecksum(checksum);
        });

        final List<SyncItem> syncItemsReceiver = new ArrayList<>();
        client.generateSyncItems(EFileSystem.RECEIVER, PathFilterNoOp.INSTANCE, syncItem -> {
            syncItemsReceiver.add(syncItem);
            final String checksum = client.generateChecksum(EFileSystem.RECEIVER, syncItem, i -> {
                // getLogger().info("Receiver Bytes read: {}", i);
            });
            syncItem.setChecksum(checksum);
        });

        final List<SyncPair> syncPairs = client.mergeSyncItems(syncItemsSender, syncItemsReceiver);

        syncPairs.forEach(SyncPair::validateStatus);

        client.syncReceiver(syncPairs, new TestClientListener());

        client.disconnectFileSystems();
    }
}
