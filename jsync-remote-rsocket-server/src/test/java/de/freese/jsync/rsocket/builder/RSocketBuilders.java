// Created: 31.07.2021
package de.freese.jsync.rsocket.builder;

/**
 * @author Thomas Freese
 */
public final class RSocketBuilders
{
    /**
     * @return {@link RSocketServerLocalBuilder}
     */
    public static RSocketServerLocalBuilder serverLocal()
    {
        return new RSocketServerLocalBuilder();
    }

    /**
     * @return {@link RSocketServerRemoteBuilder}
     */
    public static RSocketServerRemoteBuilder serverRemote()
    {
        return new RSocketServerRemoteBuilder();
    }

    /**
     * Erstellt ein neues {@link RSocketBuilders} Object.
     */
    private RSocketBuilders()
    {
        super();
    }
}
