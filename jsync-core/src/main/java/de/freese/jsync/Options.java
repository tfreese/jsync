// Created: 28.10.2016
package de.freese.jsync;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Thomas Freese
 */
public final class Options {
    /**
     * Default: 4 MB
     */
    public static final int BUFFER_SIZE = 1024 * 1024 * 4;
    /**
     * Default: UTF-8
     */
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final String EMPTY_STRING = "";
    public static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().startsWith("linux");
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");

    /**
     * @author Thomas Freese
     */
    public static class Builder {
        private final Options options;

        public Builder() {
            super();

            this.options = new Options();
        }

        public Options build() {
            return this.options;
        }

        public Builder checksum(final boolean checksum) {
            this.options.checksum = checksum;
            return this;
        }

        public Builder delete(final boolean delete) {
            this.options.delete = delete;
            return this;
        }

        public Builder dryRun(final boolean dryRun) {
            this.options.dryRun = dryRun;
            return this;
        }

        public Builder followSymLinks(final boolean followSymLinks) {
            this.options.followSymLinks = followSymLinks;
            return this;
        }

        public Builder parallel(final boolean parallel) {
            this.options.parallel = parallel;
            return this;
        }
    }

    private boolean checksum;

    private boolean delete;

    private boolean dryRun = true;

    private boolean followSymLinks = true;

    private boolean parallel;

    private Options() {
        super();
    }

    public boolean isChecksum() {
        return this.checksum;
    }

    public boolean isDelete() {
        return this.delete;
    }

    public boolean isDryRun() {
        return this.dryRun;
    }

    public boolean isFollowSymLinks() {
        return this.followSymLinks;
    }

    public boolean isParallel() {
        return this.parallel;
    }
}
