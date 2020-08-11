// Created: 12.07.2020
package de.freese.jsync.swing.components;

import java.util.List;

import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.JSyncSwingApplication;

/**
 * @author Thomas Freese
 */
public class SyncStatusTableModel extends AbstractListTableModel<SyncPair>
{
    /**
     *
     */
    private static final long serialVersionUID = -1535923872962392643L;

    /**
     * Erstellt ein neues {@link SyncStatusTableModel} Object.
     */
    public SyncStatusTableModel()
    {
        super(List.of(JSyncSwingApplication.getInstance().getMessages().getString("jsync.status")));
    }

    /**
     * @see AbstractListTableModel#getColumnClass(int)
     */
    @Override
    public Class<? extends Object> getColumnClass(final int columnIndex)
    {
        return String.class;
    }

    /**
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex)
    {
        SyncPair syncPair = getObjectAt(rowIndex);

        if (syncPair == null)
        {
            return null;
        }

        Object value = syncPair.getStatus();

        return value;
    }
}
