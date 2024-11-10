// Created: 14.03.2020
package de.freese.jsync.arguments;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * @author Thomas Freese
 */
public class ArgumentParserApacheCommonsCli implements ArgumentParser {
    private final CommandLine line;

    private final Options options;

    public ArgumentParserApacheCommonsCli(final String[] args) throws Exception {
        super();

        this.options = new Options();

        final OptionGroup groupParams = new OptionGroup();
        groupParams.addOption(Option.builder("d").longOpt("delete").hasArg(false).desc("Empfänger löscht Dateien vor dem Transfer").build());
        this.options.addOptionGroup(groupParams);

        this.options.addOption(Option.builder("f").longOpt("follow-symlinks").desc("Dateien von SymLinks kopieren").build());
        this.options.addOption(Option.builder("n").longOpt("dry-run").desc("Synchronisation nur Simulieren").build());
        this.options.addOption(Option.builder("c").longOpt("checksum").desc("Zusätzlich Prüfsumme für Vergleich berechnen").build());

        this.options.addOption(Option.builder("s").longOpt("sender").hasArg().argName("DIR").desc("Quell-Verzeichnis").required().build());
        this.options.addOption(Option.builder("r").longOpt("receiver").hasArg().argName("DIR").desc("Ziel-Verzeichnis").required().build());

        final CommandLineParser parser = new DefaultParser();

        try {
            this.line = parser.parse(this.options, args);
        }
        catch (ParseException pex) {
            printHelp(System.out);

            throw pex;
        }
    }

    @Override
    public boolean checksum() {
        return this.line.hasOption("checksum");
    }

    @Override
    public boolean delete() {
        return this.line.hasOption("delete");
    }

    @Override
    public boolean dryRun() {
        return this.line.hasOption("dry-run");
    }

    @Override
    public boolean followSymlinks() {
        return this.line.hasOption("follow-symlinks");
    }

    @Override
    public boolean hasArgs() {
        final Option[] opts = this.line.getOptions();

        return opts != null && opts.length > 0;
    }

    @Override
    public void printHelp(final PrintStream printStream) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        // formatter.setWidth(120);
        // formatter.printHelp("JSync\n", getCommandOptions(), true);

        final StringBuilder footer = new StringBuilder();
        footer.append(System.lineSeparator()).append("@Thomas Freese");

        try (PrintWriter pw = new PrintWriter(printStream, true, StandardCharsets.UTF_8)) {
            formatter.printHelp(pw, 120, "jsync [OPTIONS]" + System.lineSeparator(), System.lineSeparator() + "Parameter:", this.options, 3, 5, footer.toString(), true);
        }
    }

    @Override
    public String receiver() {
        return this.line.getOptionValue("receiver");
    }

    @Override
    public String sender() {
        return this.line.getOptionValue("sender");
    }
}
