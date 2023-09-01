// Created: 01.08.2021
package de.freese.jsync.model;

import java.net.URI;
import java.nio.file.Paths;

/**
 * @author Thomas Freese
 */
public enum JSyncProtocol {
    FILE {
        @Override
        public String getScheme() {
            return "file";
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public URI toUri(final String hostPort, final String path) {
            return Paths.get(path).toUri();
        }
    },

    NIO {
        @Override
        public String getScheme() {
            return "nio";
        }

        @Override
        public boolean isRemote() {
            return true;
        }

        @Override
        public URI toUri(final String hostPort, final String path) {
            URI uri = Paths.get(path).toUri();

            return URI.create(getScheme() + "://" + hostPort + uri.getRawPath());
        }
    },

    RSOCKET {
        @Override
        public String getScheme() {
            return "rsocket";
        }

        @Override
        public boolean isRemote() {
            return true;
        }

        @Override
        public URI toUri(final String hostPort, final String path) {
            URI uri = Paths.get(path).toUri();

            return URI.create(getScheme() + "://" + hostPort + uri.getRawPath());

            // String[] splits = path.split("[\\/]", 2);
            // String hostAndPort = splits[0];
            // String p = splits[1];
            //
            // URI uri = Paths.get("/" + p).toUri();
            //
            // return URI.create(getScheme() + "://" + hostAndPort + uri.getRawPath());
            //
            // return URI.create(getScheme() + "://" + host + "/" + p.replace(" ", "%20"));
        }
    },

    RSOCKET_LOCAL {
        @Override
        public String getScheme() {
            return "rsocketLocal";
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public URI toUri(final String hostPort, final String path) {
            URI uri = Paths.get(path).toUri();

            return URI.create(getScheme() + "://" + uri.getRawPath());
        }
    };

    public abstract String getScheme();

    public abstract boolean isRemote();

    public abstract URI toUri(String hostPort, String path);
}
