// Created: 21.07.2021
package de.freese.jsync.filesystem;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

import de.freese.jsync.model.JSyncProtocol;

/**
 * @author Thomas Freese
 */
public final class FileSystemFactory {
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class FileSystemFactoryHolder {
        private static final FileSystemFactory INSTANCE = new FileSystemFactory();

        private FileSystemFactoryHolder() {
            super();
        }
    }

    public static FileSystemFactory getInstance() {
        return FileSystemFactoryHolder.INSTANCE;
    }

    private final ServiceLoader<FileSystemProvider> serviceLoader = ServiceLoader.load(FileSystemProvider.class);

    private FileSystemFactory() {
        super();
    }

    public Receiver createReceiver(final URI uri) {
        Objects.requireNonNull(uri, "uri required");

        final String scheme = uri.getScheme();

        for (FileSystemProvider provider : this.serviceLoader) {
            if (provider.supportsProtocol(scheme)) {
                return new ReceiverDelegateLogger(provider.createReceiver(uri));
            }
        }

        throw new IllegalArgumentException("unsupported protocol: " + uri.getScheme());
    }

    public Sender createSender(final URI uri) {
        Objects.requireNonNull(uri, "uri required");

        final String scheme = uri.getScheme();

        for (FileSystemProvider provider : this.serviceLoader) {
            if (provider.supportsProtocol(scheme)) {
                return new SenderDelegateLogger(provider.createSender(uri));
            }
        }

        throw new IllegalArgumentException("unsupported protocol: " + uri.getScheme());
    }

    public List<JSyncProtocol> getAvailableProtocols() {
        final List<JSyncProtocol> protocols = List.of(JSyncProtocol.values());

        final Set<JSyncProtocol> availableProtocols = new HashSet<>();

        for (FileSystemProvider provider : this.serviceLoader) {
            for (JSyncProtocol protocol : protocols) {
                if (provider.supportsProtocol(protocol.getScheme())) {
                    availableProtocols.add(protocol);
                }
            }
        }

        final List<JSyncProtocol> list = new ArrayList<>();

        // FILE ist immer dabei.
        list.add(JSyncProtocol.FILE);
        availableProtocols.remove(JSyncProtocol.FILE);

        availableProtocols.stream().sorted(Comparator.comparing(JSyncProtocol::name)).forEach(list::add);

        return list;
    }
}
