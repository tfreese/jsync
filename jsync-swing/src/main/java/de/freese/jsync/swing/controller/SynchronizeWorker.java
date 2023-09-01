// Created: 15.08.2020
package de.freese.jsync.swing.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;

/**
 * @author Thomas Freese
 */
class SynchronizeWorker extends AbstractWorker<Void, Void> implements ClientListener {
    SynchronizeWorker(final JSyncController controller) {
        super(controller);

        getSyncView().doOnCompare(button -> button.setEnabled(false));
        getSyncView().doOnSynchronize(button -> button.setEnabled(false));

        getSyncView().setProgressBarMinMaxText(EFileSystem.SENDER, 0, 0, "");
        getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, "");

        getSyncView().setProgressBarFilesMax(0);
    }

    @Override
    public void checksumProgress(final Options options, final SyncItem syncItem, final long bytesRead) {
        if (bytesRead == 0) {
            getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, (int) syncItem.getSize(), getMessage("jsync.files.validate") + ": " + syncItem.getRelativePath());
        }

        getSyncView().setProgressBarValue(EFileSystem.RECEIVER, (int) bytesRead);
    }

    @Override
    public void copyProgress(final Options options, final SyncItem syncItem, final long bytesTransferred) {
        if (bytesTransferred == 0) {
            getSyncView().incrementProgressBarFilesValue(1);

            getSyncView().setProgressBarMinMaxText(EFileSystem.SENDER, 0, (int) syncItem.getSize(), getMessage("jsync.files.copy") + ": " + syncItem.getRelativePath());
        }

        getSyncView().setProgressBarValue(EFileSystem.SENDER, (int) bytesTransferred);
    }

    @Override
    public void delete(final Options options, final SyncItem syncItem) {
        getSyncView().incrementProgressBarFilesValue(1);

        getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, getMessage("jsync.files.delete") + ": " + syncItem.getRelativePath());
    }

    @Override
    public void error(final String message, final Throwable th) {
        getLogger().error(message, th);
    }

    @Override
    public void update(final Options options, final SyncItem syncItem) {
        getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, getMessage("jsync.files.update") + ": " + syncItem.getRelativePath());
    }

    @Override
    public void validate(final Options options, final SyncItem syncItem) {
        getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, getMessage("jsync.files.validate") + ": " + syncItem.getRelativePath());
    }

    @Override
    protected Void doInBackground() throws Exception {
        List<SyncPair> syncPairs = getSyncView().getSyncList();

        getSyncView().setProgressBarFilesMax(syncPairs.size());

        getClient().syncReceiver(syncPairs, this);

        // Wait until all GUI-Events are processed.
        // TODO BAD SOLUTION !!!
        TimeUnit.MILLISECONDS.sleep(200);

        return null;
    }

    @Override
    protected void done() {
        getLogger().info("{}", ByteBufferPool.DEFAULT);

        try {
            get();
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }

        getSyncView().clearTable();
        getSyncView().setProgressBarMinMaxText(EFileSystem.SENDER, 0, 0, "");
        getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, "");
        getSyncView().setProgressBarFilesMax(0);
        getSyncView().doOnCompare(button -> button.setEnabled(true));
        getSyncView().doOnSynchronize(button -> button.setEnabled(true));
    }
}
