// Created: 16.08.2020
package de.freese.jsync.swing.controller;

import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.Options;
import de.freese.jsync.client.Client;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.swing.JSyncContext;
import de.freese.jsync.swing.view.SyncView;
import de.freese.jsync.utils.JSyncUtils;

/**
 * @param <T> Result-Type
 * @param <V> Publish-Type
 *
 * @author Thomas Freese
 */
public abstract class AbstractWorker<T, V> extends SwingWorker<T, V> {
    protected static ExecutorService getExecutorService() {
        return JSyncContext.getExecutorService();
    }

    protected static String getMessage(final String key) {
        return JSyncContext.getMessages().getString(key);
    }

    private final JSyncController controller;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Options options;

    private final boolean parallel;

    protected AbstractWorker(final JSyncController controller) {
        super();

        this.controller = controller;
        this.options = controller.getSyncView().getOptions();

        URI senderUri = controller.getSyncView().getUri(EFileSystem.SENDER);
        URI receiverUri = controller.getSyncView().getUri(EFileSystem.RECEIVER);

        controller.createNewClient(this.options, senderUri, receiverUri);

        // this.parallel = canRunParallel(senderUri, receiverUri);
        this.parallel = this.options.isParallel();
    }

    /**
     * Enable Parallel-Execution if<br>
     * - Sender and Receiver are not on the same File-Device<br>
     * - Sender and Receiver are not on the same Server<br>
     */
    boolean canRunParallel(final URI senderUri, final URI receiverUri) {
        boolean canRunParallel = false;

        if ("file".equals(senderUri.getScheme()) && "file".equals(receiverUri.getScheme())) {
            // Local
            try {
                FileStore fileStoreSender = Files.getFileStore(Paths.get(senderUri));

                Path receiverPath = Paths.get(receiverUri);

                if (Files.notExists(receiverPath, JSyncUtils.getLinkOptions(getOptions().isFollowSymLinks()))) {
                    receiverPath = receiverPath.getParent();
                }

                FileStore fileStoreReceiver = Files.getFileStore(receiverPath);

                if (!fileStoreSender.name().equals(fileStoreReceiver.name())) {
                    canRunParallel = true;
                }
            }
            catch (Exception ex) {
                // Empty
            }
        }
        else if ("rsocket".equals(senderUri.getScheme()) && "rsocket".equals(receiverUri.getScheme())) {
            // Remote
            if (!senderUri.getHost().equals(receiverUri.getHost())) {
                canRunParallel = true;
            }
        }
        else if (!senderUri.getScheme().equals(receiverUri.getScheme())) {
            // Local -> Remote
            // Remote -> Lokal
            canRunParallel = true;
        }

        getLogger().info("Parallelism = {}", canRunParallel);

        return canRunParallel;
    }

    protected Client getClient() {
        return getController().getClient();
    }

    protected JSyncController getController() {
        return this.controller;
    }

    protected Logger getLogger() {
        return this.logger;
    }

    protected Options getOptions() {
        return this.options;
    }

    protected SyncView getSyncView() {
        return getController().getSyncView();
    }

    protected boolean isParallel() {
        return this.parallel;
    }
}
