// Created: 31.07.2021
package de.freese.jsync.rsocket.builder;

import de.freese.jsync.rsocket.builder.client.RSocketClientLocalBuilder;
import de.freese.jsync.rsocket.builder.client.RSocketClientRemoteBuilder;
import de.freese.jsync.rsocket.builder.client.RSocketClientRemoteLoadBalancedBuilder;
import de.freese.jsync.rsocket.builder.client.RSocketClientRemoteLoadBalancedWithServiceDiscoveryBuilder;
import de.freese.jsync.rsocket.builder.server.RSocketServerLocalBuilder;
import de.freese.jsync.rsocket.builder.server.RSocketServerRemoteBuilder;

/**
 * org.springframework.boot.rsocket.netty.NettyRSocketServerFactory<br>
 * org.springframework.boot.rsocket.netty.NettyRSocketServer
 *
 * @author Thomas Freese
 */
public final class RSocketBuilders
{
    public static RSocketClientLocalBuilder clientLocal()
    {
        return new RSocketClientLocalBuilder();
    }

    public static RSocketClientRemoteBuilder clientRemote()
    {
        return new RSocketClientRemoteBuilder();
    }

    public static RSocketClientRemoteLoadBalancedBuilder clientRemoteLoadBalanced()
    {
        return new RSocketClientRemoteLoadBalancedBuilder();
    }

    public static RSocketClientRemoteLoadBalancedWithServiceDiscoveryBuilder clientRemoteLoadBalancedWithServiceDiscovery()
    {
        return new RSocketClientRemoteLoadBalancedWithServiceDiscoveryBuilder();
    }

    public static RSocketServerLocalBuilder serverLocal()
    {
        return new RSocketServerLocalBuilder();
    }

    public static RSocketServerRemoteBuilder serverRemote()
    {
        return new RSocketServerRemoteBuilder();
    }

    private RSocketBuilders()
    {
        super();
    }
}
