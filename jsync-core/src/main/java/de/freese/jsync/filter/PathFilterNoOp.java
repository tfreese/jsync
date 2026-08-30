package de.freese.jsync.filter;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

/**
 * @author Thomas Freese
 * @since 15.08.2021
 */
public final class PathFilterNoOp implements PathFilter {
    public static final PathFilter INSTANCE = new PathFilterNoOp();

    private PathFilterNoOp() {
        super();
    }

    @Override
    public Set<String> directoryFilter() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> fileFilter() {
        return Collections.emptySet();
    }

    @Override
    public boolean isExcludedDirectory(final Path dir) {
        return false;
    }

    @Override
    public boolean isExcludedFile(final Path file) {
        return false;
    }
}
