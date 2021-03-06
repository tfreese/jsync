// Created: 16.08.2020
package de.freese.jsync.swing.controller;

import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

import javax.swing.SwingWorker;

import de.freese.jsync.Options;
import de.freese.jsync.client.Client;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.swing.JSyncSwingApplication;
import de.freese.jsync.swing.view.SyncView;
import de.freese.jsync.utils.JSyncUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <T> Result-Type
 * @param <V> Publish-Type
 *
 * @author Thomas Freese
 */
public abstract class AbstractWorker<T, V> extends SwingWorker<T, V>
{
    /**
     * @return {@link ExecutorService}
     */
    protected static ExecutorService getExecutorService()
    {
        return JSyncSwingApplication.getInstance().getExecutorService();
    }

    /**
     * @param key String
     *
     * @return String
     */
    protected static String getMessage(final String key)
    {
        return JSyncSwingApplication.getInstance().getMessages().getString(key);
    }

    /**
     *
     */
    public final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     *
     */
    private final JsyncController controller;
    /**
     *
     */
    private final Options options;
    /**
     *
     */
    private final boolean parallelism;

    /**
     * Erstellt ein neues {@link AbstractWorker} Object.
     *
     * @param controller {@link JsyncController}
     */
    protected AbstractWorker(final JsyncController controller)
    {
        super();

        this.controller = controller;
        this.options = controller.getSyncView().getOptions();

        URI senderUri = controller.getSyncView().getUri(EFileSystem.SENDER);
        URI receiverUri = controller.getSyncView().getUri(EFileSystem.RECEIVER);

        controller.createNewClient(this.options, senderUri, receiverUri);

        this.parallelism = determineParallel(senderUri, receiverUri);
    }

    /**
     * @return {@link Client}
     */
    protected Client getClient()
    {
        return this.controller.getClient();
    }

    /**
     * @return {@link JsyncController}
     */
    protected JsyncController getController()
    {
        return this.controller;
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @return {@link Options}
     */
    protected Options getOptions()
    {
        return this.options;
    }

    /**
     * @return {@link SyncView}
     */
    protected SyncView getSyncView()
    {
        return this.controller.getSyncView();
    }

    /**
     * @return boolean
     */
    protected boolean isParallelism()
    {
        return this.parallelism;
    }

    /**
     * Parallel-Verabeitung aktivieren, wenn<br>
     * - Sender und Receiver nicht auf dem gleichen File-Device laufen
     * - Sender und Receiver nicht auf dem gleichen Server laufen
     *
     * @param senderUri   {@link URI}
     * @param receiverUri {@link URI}
     *
     * @return boolean
     */
    private boolean determineParallel(URI senderUri, URI receiverUri)
    {
        boolean parallel = false;

        if ("file".equals(senderUri.getScheme()) && "file".equals(receiverUri.getScheme()))
        {
            // Local
            try
            {
                FileStore fileStoreSender = Files.getFileStore(Paths.get(senderUri));

                Path receiverPath = Paths.get(receiverUri);

                if (Files.notExists(receiverPath, JSyncUtils.getLinkOptions(getOptions().isFollowSymLinks())))
                {
                    receiverPath = receiverPath.getParent();
                }

                FileStore fileStoreReceiver = Files.getFileStore(receiverPath);

                if (!fileStoreSender.name().equals(fileStoreReceiver.name()))
                {
                    parallel = true;
                }
            }
            catch (Exception ex)
            {
                // Empty
            }
        }
        else if ("jsync".equals(senderUri.getScheme()) && "jsync".equals(receiverUri.getScheme()))
        {
            // Remote
            if (!senderUri.getHost().equals(receiverUri.getHost()))
            {
                parallel = true;
            }
        }
        else if (!senderUri.getScheme().equals(receiverUri.getScheme()))
        {
            // Local -> Remote
            // Remote -> Lokal
            parallel = true;
        }

        getLogger().info("Parallelism = {}", parallel);

        return parallel;
    }
}
