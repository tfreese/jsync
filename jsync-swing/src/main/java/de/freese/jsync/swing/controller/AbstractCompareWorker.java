// Created: 16.08.2020
package de.freese.jsync.swing.controller;

import de.freese.jsync.filesystem.EFileSystem;

/**
 * @author Thomas Freese
 */
abstract class AbstractCompareWorker extends AbstractWorker<Void, Void>
{
    /**
     * Erstellt ein neues {@link AbstractCompareWorker} Object.
     *
     * @param controller {@link JsyncController}
     */
    AbstractCompareWorker(final JsyncController controller)
    {
        super(controller);

        controller.getSyncView().doOnCompare(button -> button.setEnabled(false));
        controller.getSyncView().doOnSyncronize(button -> button.setEnabled(false));

        controller.getSyncView().clearTable();

        controller.getSyncView().addProgressBarText(EFileSystem.SENDER, "");
        controller.getSyncView().setProgressBarIndeterminate(EFileSystem.SENDER, true);
        controller.getSyncView().addProgressBarText(EFileSystem.RECEIVER, "");
        controller.getSyncView().setProgressBarIndeterminate(EFileSystem.RECEIVER, true);
        controller.getSyncView().setProgressBarFilesMax(0);
    }

    /**
     * @see javax.swing.SwingWorker#done()
     */
    @Override
    protected void done()
    {
        try
        {
            get();
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }

        getSyncView().doOnCompare(button -> button.setEnabled(true));
        getSyncView().doOnSyncronize(button -> button.setEnabled(true));
        getSyncView().addProgressBarMinMaxText(EFileSystem.SENDER, 0, 0, "");
        getSyncView().addProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, "");
    }
}
