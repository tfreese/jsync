/// **
// * Created: 14.03.2020
// */
//
// package de.freese.jsync.arguments;
//
// import java.io.IOException;
// import java.io.PrintStream;
// import java.io.UncheckedIOException;
// import java.util.Arrays;
// import joptsimple.OptionParser;
// import joptsimple.OptionSet;
// import joptsimple.OptionSpec;
//
// / **
// * Die Parser-Klassen liegen auch intern im JDK-Package jdk.internal.joptsimple.
// *
// * @author Thomas Freese
// */
// @SuppressWarnings("javadoc")
// public class ArgumentParserJopt implements ArgumentParser {
// private final OptionParser optionParser = new OptionParser();
// private final OptionSet optionSet;
//
// private final OptionSpec<Void> optionSpecChecksum =
// optionParser.acceptsAll(Arrays.asList("c", "checksum"), "Zusätzlich Prüfsumme für Vergleich berechnen");
//
// private final OptionSpec<Void> optionSpecDelete = optionParser.acceptsAll(Arrays.asList("d", "delete"), "Empfänger löscht Dateien vor dem Transfer");
//
// private final OptionSpec<Void> optionSpecDryRun = optionParser.acceptsAll(Arrays.asList("n", "dry-run"), "Synchronisation nur Simulieren");
//
// private final OptionSpec<Void> optionSpecFollowSymlinks =
// optionParser.acceptsAll(Arrays.asList("f", "follow-symlinks"), "Dateien von SymLinks kopieren");
//
// private final OptionSpec<String> optionSpecSender =
// optionParser.acceptsAll(Arrays.asList("s", "sender"), "Quell-Verzeichnis").withRequiredArg().ofType(String.class).defaultsTo("");
//
// private final OptionSpec<String> optionSpecReceiver =
// optionParser.acceptsAll(Arrays.asList("r", "receiver"), "Ziel-Verzeichnis").withRequiredArg().ofType(String.class).defaultsTo("");
//
// public ArgumentParserJopt(final String[] args) {
// super();
//
// optionSet = optionParser.parse(args);
// }
//
// @Override
// public boolean checksum() {
// return optionSet.has(optionSpecChecksum);
// }
//
// @Override
// public boolean delete() {
// return optionSet.has(optionSpecDelete);
// }
//
// @Override
// public boolean dryRun() {
// return optionSet.has(optionSpecDryRun);
// }
//
// @Override
// public boolean followSymlinks() {
// return optionSet.has(optionSpecFollowSymlinks);
// }
//
// @Override
// public boolean hasArgs() {
// return optionSet.hasOptions();
// }
//
// @Override
// public void printHelp(final PrintStream printStream) {
// try
// {
// printStream.println("usage: jsync [OPTIONS]");
// printStream.println();
//
// optionParser.printHelpOn(printStream);
// }
// catch (IOException ex) {
// throw new UncheckedIOException(ex);
// }
// }
//
// @Override
// public String sender() {
// return optionSet.valueOf(optionSpecSender);
// }
//
// @Override
// public String receiver()  {
// return optionSet.valueOf(optionSpecReceiver);
// }
// }
