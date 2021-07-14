// Created: 22.09.2020
package de.freese.jsync.model.serializer.adapter;

/**
 * Interface für eine Datenquelle/-senke.<br>
 *
 * @author Thomas Freese
 *
 * @param <D> Type of Source/Sink
 *
 * @see "org.springframework.core.io.buffer.DataBuffer"
 */
public interface DataAdapter<D> extends DataAdapterRead<D>, DataAdapterWrite<D>
{
}
