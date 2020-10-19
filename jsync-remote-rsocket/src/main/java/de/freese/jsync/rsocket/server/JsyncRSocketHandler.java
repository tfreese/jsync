// Created: 19.10.2020
package de.freese.jsync.rsocket.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.rsocket.RSocket;

/**
 * @author Thomas Freese
 */
public class JsyncRSocketHandler implements RSocket
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsyncRSocketHandler.class);

    /**
     * Erstellt ein neues {@link JsyncRSocketHandler} Object.
     */
    public JsyncRSocketHandler()
    {
        super();
    }
}
