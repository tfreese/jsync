// Created: 01.08.2021
package de.freese.jsync.model;

import java.net.URI;
import java.nio.file.Paths;

/**
 * @author Thomas Freese
 */
public enum JSyncProtocol {
    FILE {
        /**
         * @see de.freese.jsync.model.JSyncProtocol#getScheme()
         */
        @Override
        public String getScheme() {
            return "file";
        }

        /**
         * @see de.freese.jsync.model.JSyncProtocol#isRemote()
         */
        @Override
        public boolean isRemote() {
            return false;
        }

        /**
         * @see de.freese.jsync.model.JSyncProtocol#toUri(java.lang.String, java.lang.String)
         */
        @Override
        public URI toUri(final String hostPort, final String path) {
            return Paths.get(path).toUri();
        }
    },

    NIO {
        /**
         * @see de.freese.jsync.model.JSyncProtocol#getScheme()
         */
        @Override
        public String getScheme() {
            return "nio";
        }

        /**
         * @see de.freese.jsync.model.JSyncProtocol#isRemote()
         */
        @Override
        public boolean isRemote() {
            return true;
        }

        /**
         * @see de.freese.jsync.model.JSyncProtocol#toUri(java.lang.String, java.lang.String)
         */
        @Override
        public URI toUri(final String hostPort, final String path) {
            URI uri = Paths.get(path).toUri();

            return URI.create(getScheme() + "://" + hostPort + uri.getRawPath());
        }
    },

    RSOCKET {
        /**
         * @see de.freese.jsync.model.JSyncProtocol#getScheme()
         */
        @Override
        public String getScheme() {
            return "rsocket";
        }

        /**
         * @see de.freese.jsync.model.JSyncProtocol#isRemote()
         */
        @Override
        public boolean isRemote() {
            return true;
        }

        /**
         * @see de.freese.jsync.model.JSyncProtocol#toUri(java.lang.String, java.lang.String)
         */
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
        /**
         * @see de.freese.jsync.model.JSyncProtocol#getScheme()
         */
        @Override
        public String getScheme() {
            return "rsocketLocal";
        }

        /**
         * @see de.freese.jsync.model.JSyncProtocol#isRemote()
         */
        @Override
        public boolean isRemote() {
            return false;
        }

        /**
         * @see de.freese.jsync.model.JSyncProtocol#toUri(java.lang.String, java.lang.String)
         */
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
