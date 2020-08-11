// Created: 11.07.2020
package de.freese.jsync.swing.view;

import de.freese.jsync.swing.components.ReceiverTableModel;

/**
 * @author Thomas Freese
 */
public class ReceiverView extends AbstractFileSystemView
{
    /**
     * Erstellt ein neues {@link ReceiverView} Object.
     */
    public ReceiverView()
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

        getLabelDirChooser().setText(getMessage("jsync.ziel"));

        getTable().setModel(new ReceiverTableModel());
    }
}
