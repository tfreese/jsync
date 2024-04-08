package de.freese.jsync.swing.components;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.swing.table.AbstractTableModel;

/**
 * @author Thomas Freese
 */
public abstract class AbstractListTableModel<T> extends AbstractTableModel {
    @Serial
    private static final long serialVersionUID = 8219964863357772409L;

    private final transient List<String> columnNames;
    private final transient List<T> list;

    protected AbstractListTableModel(final List<String> columnNames) {
        this(columnNames, new ArrayList<>());
    }

    protected AbstractListTableModel(final List<String> columnNames, final List<T> list) {
        super();

        this.columnNames = Objects.requireNonNull(columnNames, "columnNames required");
        this.list = Objects.requireNonNull(list, "list required");
    }

    public void add(final T object) {
        getList().add(object);

        fireTableRowsInserted(getList().size() - 1, getList().size() - 1);
    }

    public void addAll(final Collection<T> objects) {
        final int sizeOld = getList().size();

        getList().addAll(objects);

        fireTableRowsInserted(sizeOld, getList().size() - 1);
    }

    public void clear() {
        getList().clear();

        refresh();
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        if (getRowCount() != 0) {
            for (int row = 0; row < getRowCount(); row++) {
                final Object object = getValueAt(row, columnIndex);

                if (object != null) {
                    return object.getClass();
                }
            }
        }

        return super.getColumnClass(columnIndex);
    }

    @Override
    public int getColumnCount() {
        return getColumnNames().size();
    }

    @Override
    public String getColumnName(final int column) {
        return getColumnNames().get(column);
    }

    public T getObjectAt(final int rowIndex) {
        return getList().get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return getList().size();
    }

    public int getRowOf(final T object) {
        return getList().indexOf(object);
    }

    public Stream<T> getStream() {
        return getList().stream();
    }

    public void refresh() {
        fireTableDataChanged();
    }

    protected List<String> getColumnNames() {
        return this.columnNames;
    }

    protected List<T> getList() {
        return this.list;
    }
}
