// Created: 11.07.2021
package de.freese.jsync.rsocket.client;

import io.rsocket.core.RSocketClient;

/**
 * @author Thomas Freese
 * @param <S> Server Type
 */
public interface MyRSocketClient<S>
{
    /**
     * @param serverInfo Object
     * @throws Exception Falls was schief geht.
     */
    void connect(S serverInfo) throws Exception;

    /**
     *
     */
    void disconnect();

    /**
     * @return {@link RSocketClient}
     */
    RSocketClient getClient();
}
