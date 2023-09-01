// Created: 18.08.20
package de.freese.jsync.swing.components;

import java.awt.Component;
import java.io.Serial;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * @author Thomas Freese
 */
public class SyncPairTableCellRendererFileSystem extends DefaultTableCellRenderer {
    @Serial
    private static final long serialVersionUID = -8974544290640941021L;

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (column == 1) {
            setHorizontalAlignment(SwingConstants.CENTER);
        }
        else {
            setHorizontalAlignment(SwingConstants.LEFT);
        }

        return this;
    }
}
