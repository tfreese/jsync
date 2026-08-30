package de.freese.jsync.filter;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

/**
 * Exclude-Filter, uses {@link String#endsWith(String)}.
 *
 * @author Thomas Freese
 * @since 15.08.2021
 */
public record PathFilterEndsWith(Set<String> directoryFilter, Set<String> fileFilter) implements PathFilter {
    public PathFilterEndsWith(final Set<String> directoryFilter, final Set<String> fileFilter) {

        this.directoryFilter = Set.copyOf(Objects.requireNonNull(directoryFilter, "directoryFilter required"));
        this.fileFilter = Set.copyOf(Objects.requireNonNull(fileFilter, "fileFilter required"));
    }

    @Override
    public boolean isExcludedDirectory(final Path dir) {
        return directoryFilter.stream().anyMatch(filter -> dir.toString().endsWith(filter));
    }

    @Override
    public boolean isExcludedFile(final Path file) {
        return fileFilter.stream().anyMatch(filter -> file.toString().endsWith(filter));
    }
}
