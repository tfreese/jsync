// Created: 11.07.2021
package de.freese.jsync.rsocket.client;

import de.freese.jsync.rsocket.server.MyRSocketServerLocal;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.local.LocalClientTransport;
import reactor.core.publisher.Mono;

/**
 * {@link RSocketClient} für Verwendung innerhalb einer Runtime.
 *
 * @author Thomas Freese
 */
public class MyRSocketClientLocal implements MyRSocketClient<MyRSocketServerLocal>
{
    /**
     *
     */
    private RSocketClient client;

    /**
     * @see de.freese.jsync.rsocket.client.MyRSocketClient#connect(java.lang.Object)
     */
    @Override
    public void connect(final MyRSocketServerLocal serverInfo) throws Exception
    {
        ClientTransport clientTransport = LocalClientTransport.create(serverInfo.getName());

        // @formatter:off
        Mono<RSocket> rSocket = RSocketConnector.create()
                .payloadDecoder(PayloadDecoder.DEFAULT)
                .connect(clientTransport)
                ;
        // @formatter:on

        this.client = RSocketClient.from(rSocket);
    }

    /**
     * @see de.freese.jsync.rsocket.client.MyRSocketClient#disconnect()
     */
    @Override
    public void disconnect()
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.rsocket.client.MyRSocketClient#getClient()
     */
    @Override
    public RSocketClient getClient()
    {
        return this.client;
    }
}
