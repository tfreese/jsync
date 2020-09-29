// Created: 29.09.2020
package de.freese.jsync.remote.api;

import java.io.Closeable;

/**
 * @author Thomas Freese
 * @param <T> Type of Result
 */
public interface JsyncResponse<T> extends Closeable
{
    /**
     * @return {@link Throwable}
     */
    public Throwable getException();

    /**
     * @return Object
     */
    public T getResult();
}
