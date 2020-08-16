// Created: 16.08.2020
package de.freese.jsync.swing.controller;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options;
import de.freese.jsync.client.Client;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.swing.JSyncSwingApplication;
import de.freese.jsync.swing.view.SyncView;

/**
 * @author Thomas Freese
 * @param <T> Result-Type
 * @param <V> Publish-Type
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
     * @param key String
     * @return String
     */
    protected String getMessage(final String key)
    {
        return JSyncSwingApplication.getInstance().getMessages().getString(key);
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
        // boolean parallelism = !(senderUri.getScheme().equals("file") && receiverUri.getScheme().equals("file"));
        boolean parallelism = getOptions().isParallelism();

        return parallelism;
    }
}
