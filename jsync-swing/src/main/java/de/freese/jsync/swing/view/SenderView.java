/**
 * Created: 11.07.2020
 */

package de.freese.jsync.swing.view;

/**
 * @author Thomas Freese
 */
public class SenderView extends AbstractFileSystemView
{
    /**
     * Erstellt ein neues {@link SenderView} Object.
     */
    public SenderView()
    {
        super();
    }

    /**
     * @see de.freese.jsync.swing.view.AbstractFileSystemView#initGUI()
     */
    @Override
    public void initGUI()
    {
        super.initGUI();

        getLabelDirChooser().setText(getMessages().getString("jsync.quelle"));
    }
}
