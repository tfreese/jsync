// Created: 15.08.2021
package de.freese.jsync.filter;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

/**
 * Exclude-Filter, uses {@link String#endsWith(String)}.
 *
 * @author Thomas Freese
 */
public class PathFilterEndsWith implements PathFilter {
    private final Set<String> directoryFilter;
    private final Set<String> fileFilter;

    public PathFilterEndsWith(final Set<String> directoryFilter, final Set<String> fileFilter) {
        super();

        this.directoryFilter = Objects.requireNonNull(directoryFilter, "directoryFilter required");
        this.fileFilter = Objects.requireNonNull(fileFilter, "fileFilter required");
    }

    @Override
    public Set<String> getDirectoryFilter() {
        return this.directoryFilter;
    }

    @Override
    public Set<String> getFileFilter() {
        return this.fileFilter;
    }

    @Override
    public boolean isExcludedDirectory(final Path dir) {
        return this.directoryFilter.stream().anyMatch(filter -> dir.toString().endsWith(filter));
    }

    @Override
    public boolean isExcludedFile(final Path file) {
        return this.fileFilter.stream().anyMatch(filter -> file.toString().endsWith(filter));
    }
}
