// Created: 22.09.2020
package de.freese.jsync.model.serializer.adapter;

/**
 * Interface für eine Datenquelle/-senke.<br>
 *
 * @param <W> Type of Sink
 * @param <R> Type of Source
 *
 * @author Thomas Freese
 * @see "org.springframework.core.io.buffer.DataBuffer"
 */
public interface DataAdapter<W, R> extends DataAdapterWrite<W>, DataAdapterRead<R>
{
}
