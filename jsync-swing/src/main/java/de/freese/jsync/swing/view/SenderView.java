/**
 * Created: 11.07.2020
 */

package de.freese.jsync.swing.view;

import javax.swing.JProgressBar;
import de.freese.jsync.swing.GbcBuilder;

/**
 * @author Thomas Freese
 */
public class SenderView extends AbstractFileSystemView
{
    /**
     *
     */
    private JProgressBar progressBarChecksum = null;

    /**
     *
     */
    private JProgressBar progressBarFiles = null;

    /**
     * Erstellt ein neues {@link SenderView} Object.
     */
    public SenderView()
    {
        super();
    }

    /**
     * @return {@link JProgressBar}
     */
    public JProgressBar getProgressBarChecksum()
    {
        return this.progressBarChecksum;
    }

    /**
     * @return {@link JProgressBar}
     */
    public JProgressBar getProgressBarFiles()
    {
        return this.progressBarFiles;
    }

    /**
     * @see de.freese.jsync.swing.view.AbstractFileSystemView#initGUI()
     */
    @Override
    public void initGUI()
    {
        super.initGUI();

        this.progressBarFiles = new JProgressBar();
        this.progressBarFiles.setStringPainted(true);
        getPanel().add(this.progressBarFiles, new GbcBuilder(0, 2).gridwidth(3).fillHorizontal());

        this.progressBarChecksum = new JProgressBar();
        this.progressBarChecksum.setStringPainted(true);
        this.progressBarChecksum.setVisible(false);
        getPanel().add(this.progressBarChecksum, new GbcBuilder(0, 3).gridwidth(3).fillHorizontal());

        getLabelDirChooser().setText(getMessages().getString("jsync.quelle"));
    }
}
