package me.myself.termepub;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.jline.builtins.Less;
import org.jline.builtins.Source;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.completer.AggregateCompleter;
import org.jline.reader.impl.completer.completer.FileNameCompleter;
import org.jline.reader.impl.completer.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.github.mertakdut.exception.ReadingException;

public class Main {

    private static final Logger LOGGER = Logger.getLogger("TERMEPUB");
    private final static String CRASH_MESSAGE = "An error occured, you should try to restart the application.";
    private static final String numberRegex = "^\\d+$";
    private static final String HELP = "**********USAGE**********\n"
            + "General concept : text is displayed through a pager/less-like screen.\n"
            + "At the prompt you can enter several commands : \n"
            + "  - help or -h : shows this page\n"
            + "  - quit or exit : quits the app\n"
            + "  - a file path : opens the epub at this path \n"
            + "     (TAB completion is available for paths)\n"
            + "  - toc : displays the table of contents of the chosen book\n"
            + "  - a number : opens the corresponding chapter (according to the toc, provided it exists in the book)\n"
            + "\n" + "*************************\n"
            + "Commands in the pager : \n"
            + "   - q to exit the pager and go back to the main screen\n"
            + "   - RETURN or down arrow to go down one line at a time\n"
            + "   - up arrow to go up one line\n"
            + "   - SPACE to go down one page\n";

    public static void main(String[] args) {
        EbookReader bookReader = new EbookReader();
        boolean firstLoop = true;
        Map<String, ParsedBook> alreadyInMemoryBooks = new HashMap<>();

        try {
//            String rightPrompt = new AttributedStringBuilder()
//                    .style(AttributedStyle.DEFAULT
//                            .background(AttributedStyle.CYAN))
//                    .append("foo").style(AttributedStyle.DEFAULT).append("@bar")
//                    .style(AttributedStyle.DEFAULT
//                            .foreground(AttributedStyle.MAGENTA))
//                    .append("\nbaz").style(AttributedStyle.DEFAULT).append("> ")
//                    .toAnsi();
            String rightPrompt = "";
            String prompt = "Enter :\n"
                    + "      * a file path to open a book\n "
                    + "     * toc to print table of content\n"
                    + "      * a chapter number to open this chapter\n" + ">>>";
            boolean color = false;

            TerminalBuilder builder = TerminalBuilder.builder();
            Completer fileCompleter = new FileNameCompleter();
            Completer opts = new StringsCompleter("help", "-h", "toc", "quit",
                    "exit");
            Completer allCompleters = new AggregateCompleter(opts,
                    fileCompleter);
            Terminal terminal = builder.build();
            LineReader reader = LineReaderBuilder.builder().terminal(terminal)
                    .completer(allCompleters).build();
            Less less = new Less(terminal);
            while (true) {

                String line = null;
                try {
                    if (firstLoop) {
                        line = reader.readLine(HELP + ">>>", rightPrompt, null,
                                null);
                        firstLoop = false;
                    } else {
                        line = reader.readLine(prompt, rightPrompt, null, null);
                    }
                } catch (UserInterruptException e) {
                    // Ignore
                } catch (EndOfFileException e) {
                    return;
                }
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }

                line = line.trim();
                if (line.equalsIgnoreCase("quit")
                        || line.equalsIgnoreCase("exit")) {
                    break;
                } else if (line.equalsIgnoreCase("toc")) {
                    if (bookReader.getBook() != null) {
                        ParsedBook book = bookReader.getBook();
                        String toc = bookReader.getBookToc(book);
                        Source source = new StringSource(toc,
                                book.getTitle() + " Table of Contents");
                        less.run(source);
                    }
                } else if (line.equalsIgnoreCase("help")
                        || line.equalsIgnoreCase("-h")) {
                    terminal.writer().println(HELP);
                    terminal.flush();
                } else if (line.matches(numberRegex)) {
                    try {
                        Part chapter = bookReader.getBook()
                                .getChapter(Integer.parseInt(line));
                        Source source = new StringSource(chapter.getContent(),
                                chapter.getTitle());
                        less.run(source);
                    } catch (NumberFormatException e) {
                        continue;
                    } catch (IllegalArgumentException e) {
                        terminal.writer().println(e.getMessage());
                        terminal.flush();
                    }
                } else if (alreadyInMemoryBooks.get(line.trim()) != null) {
                    bookReader.setBook(alreadyInMemoryBooks.get(line));
                } else if (Files.isReadable(Paths.get(line))) {
                    terminal.writer()
                            .println("Retrieving book, please wait...");
                    terminal.flush();
                    ParsedBook book = bookReader.buildAsyncEbook(line);
                    alreadyInMemoryBooks.put(line.trim(), book);
                } else {
                    continue;
                }

            }
        } catch (IOException e) {
            LOGGER.severe(CRASH_MESSAGE);
        } catch (ReadingException e) {
            LOGGER.severe(CRASH_MESSAGE);
        } catch (InterruptedException e) {
            LOGGER.severe(CRASH_MESSAGE);
        } catch (ExecutionException e) {
            LOGGER.severe(CRASH_MESSAGE);
        } catch (TimeoutException e) {
            LOGGER.severe(CRASH_MESSAGE);
        } finally {
            bookReader.closereaderExecutor();
        }
    }

}
