// Created: 31.07.2021
package de.freese.jsync.rsocket.builder;

import de.freese.jsync.rsocket.builder.client.RSocketClientBuilderLocal;
import de.freese.jsync.rsocket.builder.client.RSocketClientBuilderRemote;
import de.freese.jsync.rsocket.builder.client.RSocketClientBuilderRemoteLoadBalanced;
import de.freese.jsync.rsocket.builder.client.RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery;
import de.freese.jsync.rsocket.builder.server.RSocketServerBuilderLocal;
import de.freese.jsync.rsocket.builder.server.RSocketServerBuilderRemote;

/**
 * org.springframework.boot.rsocket.netty.NettyRSocketServerFactory<br>
 * org.springframework.boot.rsocket.netty.NettyRSocketServer
 *
 * @author Thomas Freese
 */
public final class RSocketBuilders {
    public static RSocketClientBuilderLocal clientLocal() {
        return new RSocketClientBuilderLocal();
    }

    public static RSocketClientBuilderRemote clientRemote() {
        return new RSocketClientBuilderRemote();
    }

    public static RSocketClientBuilderRemoteLoadBalanced clientRemoteLoadBalanced() {
        return new RSocketClientBuilderRemoteLoadBalanced();
    }

    public static RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery clientRemoteLoadBalancedWithServiceDiscovery() {
        return new RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery();
    }

    public static RSocketServerBuilderLocal serverLocal() {
        return new RSocketServerBuilderLocal();
    }

    public static RSocketServerBuilderRemote serverRemote() {
        return new RSocketServerBuilderRemote();
    }

    private RSocketBuilders() {
        super();
    }
}
