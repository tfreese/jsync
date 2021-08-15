/**
 * Created: 12.07.2020
 */

package de.freese.jsync.swing.components;

import java.util.List;

import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.JsyncContext;

/**
 * @author Thomas Freese
 */
public class SyncPairTableModelStatus extends AbstractListTableModel<SyncPair>
{
    /**
     *
     */
    private static final long serialVersionUID = 871089543329340184L;

    /**
     * Erstellt ein neues {@link SyncPairTableModelStatus} Object.
     */
    public SyncPairTableModelStatus()
    {
        super(List.of(JsyncContext.getMessages().getString("jsync.status")));
    }

    /**
     * @see de.freese.jsync.swing.components.AbstractListTableModel#getColumnClass(int)
     */
    @Override
    public Class<? extends Object> getColumnClass(final int columnIndex)
    {
        return switch (columnIndex)
        {
            case 0 -> String.class;
            default -> Object.class;
        };
    }

    /**
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex)
    {
        SyncPair syncPair = getObjectAt(rowIndex);

        return switch (columnIndex)
        {
            case 0 -> syncPair.getStatus();
            default -> null;
        };
    }
}