// Created: 23.11.2018
package de.freese.jsync.generator.listener;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;

import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public class ConsoleGeneratorListener extends AbstractGeneratorListener {
    private final String prefix;

    private final PrintStream printStream;

    private final PrintStream printStreamError;

    public ConsoleGeneratorListener(final String prefix) {
        super();

        this.prefix = Objects.requireNonNull(prefix, "prefix required");

        // Console console = System.console();
        //
        // if (console != null)
        // {
        // printStream = console.writer();
        // }
        // else
        // {
        // printStream = System.out;
        // }

        this.printStream = System.out;

        this.printStreamError = System.err;
    }

    @Override
    public void checksum(final long bytesRead) {
        // Empty
    }

    @Override
    public void currentItem(final SyncItem syncItem) {
        // String message = currentMetaMessage(relativePath, this.prefix);
        //
        // getPrintStream().println(message);
    }

    @Override
    public void itemCount(final Path path, final int itemCount) {
        String message = itemCountMessage(path, itemCount, this.prefix);

        getPrintStream().println(message);
    }

    protected PrintStream getPrintStream() {
        return this.printStream;
    }

    protected PrintStream getPrintStreamError() {
        return this.printStreamError;
    }
}
