// Created: 14.08.2021
package de.freese.jsync.swing.view;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.components.AbstractListTableModel;
import de.freese.jsync.swing.components.SyncPairTableModelFileSystem;
import de.freese.jsync.swing.components.SyncPairTableModelStatus;

/**
 * @author Thomas Freese
 */
public class TableFacade
{
    /**
     *
     */
    private TableRowSorter<AbstractListTableModel<SyncPair>> rowSorterReceiver;
    /**
     *
     */
    private TableRowSorter<AbstractListTableModel<SyncPair>> rowSorterSender;
    /**
     *
     */
    private TableRowSorter<AbstractListTableModel<SyncPair>> rowSorterStatus;
    /**
     *
     */
    private final AbstractListTableModel<SyncPair> tableModelReceiver;
    /**
     *
     */
    private final AbstractListTableModel<SyncPair> tableModelSender;
    /**
     *
     */
    private final AbstractListTableModel<SyncPair> tableModelStatus;
    /**
     *
     */
    private final JTable tableReceiver;
    /**
     *
     */
    private final JTable tableSender;
    /**
     *
     */
    private final JTable tableStatus;

    /**
     * Erstellt ein neues {@link TableFacade} Object.
     */
    public TableFacade()
    {
        super();

        this.tableSender = new JTable();
        this.tableStatus = new JTable();
        this.tableReceiver = new JTable();

        this.tableModelSender = new SyncPairTableModelFileSystem(EFileSystem.SENDER);
        this.tableModelStatus = new SyncPairTableModelStatus();
        this.tableModelReceiver = new SyncPairTableModelFileSystem(EFileSystem.RECEIVER);
    }

    /**
     * @param objects {@link Collection}
     */
    void addAll(final List<SyncPair> objects)
    {
        this.tableModelSender.addAll(objects);
        this.tableModelStatus.addAll(objects);
        this.tableModelReceiver.addAll(objects);
    }

    /**
    *
    */
    void clear()
    {
        this.tableModelSender.clear();
        this.tableModelStatus.clear();
        this.tableModelReceiver.clear();
    }

    /**
     * @param firstRow int
     * @param lastRow int
     */
    void fireTableRowsUpdated(final int firstRow, final int lastRow)
    {
        this.tableModelSender.fireTableRowsUpdated(firstRow, lastRow);
        this.tableModelStatus.fireTableRowsUpdated(firstRow, lastRow);
        this.tableModelReceiver.fireTableRowsUpdated(firstRow, lastRow);
    }

    /**
     * @return int
     */
    int getRowCount()
    {
        return this.tableModelStatus.getRowCount();
    }

    /**
     * @return {@link List}
     */
    Stream<SyncPair> getStream()
    {
        return this.tableModelStatus.getStream();
    }

    /**
     * @return {@link AbstractListTableModel}
     */
    AbstractListTableModel<SyncPair> getTableModelReceiver()
    {
        return this.tableModelReceiver;
    }

    /**
     * @return {@link AbstractListTableModel}
     */
    AbstractListTableModel<SyncPair> getTableModelSender()
    {
        return this.tableModelSender;
    }

    /**
     * @return {@link AbstractListTableModel}
     */
    AbstractListTableModel<SyncPair> getTableModelStatus()
    {
        return this.tableModelStatus;
    }

    /**
     * @return {@link JTable}
     */
    JTable getTableReceiver()
    {
        return this.tableReceiver;
    }

    /**
     * @return {@link JTable}
     */
    JTable getTableSender()
    {
        return this.tableSender;
    }

    /**
     * @return {@link JTable}
     */
    JTable getTableStatus()
    {
        return this.tableStatus;
    }

    /**
     * @param supplierPredicate {@link Supplier}
     */
    void initRowSorter(final Supplier<Predicate<SyncPair>> supplierPredicate)
    {
        RowFilter<AbstractListTableModel<SyncPair>, Integer> rowFilter = new RowFilter<>()
        {
            public boolean include(final Entry<? extends AbstractListTableModel<SyncPair>, ? extends Integer> entry)
            {
                SyncPair syncPair = entry.getModel().getObjectAt(entry.getIdentifier());

                return supplierPredicate.get().test(syncPair);
            }
        };

        this.rowSorterSender = new TableRowSorter<>(this.tableModelSender);
        this.rowSorterSender.setRowFilter(rowFilter);
        this.tableSender.setRowSorter(this.rowSorterSender);

        this.rowSorterStatus = new TableRowSorter<>(this.tableModelStatus);
        this.rowSorterStatus.setRowFilter(rowFilter);
        this.tableStatus.setRowSorter(this.rowSorterStatus);

        this.rowSorterReceiver = new TableRowSorter<>(this.tableModelReceiver);
        this.rowSorterReceiver.setRowFilter(rowFilter);
        this.tableReceiver.setRowSorter(this.rowSorterReceiver);
    }

    /**
     *
     */
    void scrollToLastRow()
    {
        int rowCount = getRowCount();

        Rectangle rectangle = this.tableSender.getCellRect(rowCount - 1, 0, false);
        this.tableSender.scrollRectToVisible(rectangle);

        rectangle = this.tableStatus.getCellRect(rowCount - 1, 0, false);
        this.tableStatus.scrollRectToVisible(rectangle);

        rectangle = this.tableReceiver.getCellRect(rowCount - 1, 0, false);
        this.tableReceiver.scrollRectToVisible(rectangle);
    }

    /**
     *
     */
    void sort()
    {
        this.rowSorterSender.sort();
        this.rowSorterStatus.sort();
        this.rowSorterReceiver.sort();
    }
}
