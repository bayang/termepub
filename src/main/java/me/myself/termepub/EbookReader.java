package me.myself.termepub;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringEscapeUtils;

import com.github.mertakdut.BookSection;
import com.github.mertakdut.Package;
import com.github.mertakdut.Reader;
import com.github.mertakdut.exception.OutOfPagesException;
import com.github.mertakdut.exception.ReadingException;

import net.htmlparser.jericho.MasonTagTypes;
import net.htmlparser.jericho.MicrosoftConditionalCommentTagTypes;
import net.htmlparser.jericho.PHPTagTypes;
import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;

public class EbookReader {

    private ExecutorService executor = null;
    static AtomicBoolean flag = new AtomicBoolean(true);
    static AtomicInteger flagI = new AtomicInteger(2000);
    private ParsedBook book;
    private final String SPACEREGEX = "\\s{3,}";
    
    public ParsedBook getBook() {
        return book;
    }

    public void setBook(ParsedBook book) {
        this.book = book;
    }

    public ParsedBook buildEbook(String epubFilePath) throws ReadingException,
            InterruptedException, ExecutionException, TimeoutException {

        Reader reader = new Reader();
        reader.setMaxContentPerSection(100000);
        reader.setIsIncludingTextContent(true);
        reader.setFullContent(epubFilePath);
        Package pk = reader.getInfoPackage();
        String bookTitle = pk.getMetadata().getTitle() != null
                ? pk.getMetadata().getTitle() : "Title_unknown";
        List<Part> parts = new ArrayList<>();
        ParsedBook book = new ParsedBook();
        book.setTitle(bookTitle);
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            Part part = null;
            try {
                BookSection bookSection = reader.readSection(i);
                if (bookSection.getLabel() != null || !bookSection
                        .getSectionTextContent().trim().isEmpty()) {
                    String content = bookSection.getSectionTextContent().trim()
                            .replaceAll("(?s)<!--.*?-->", "");
                    content = StringEscapeUtils.unescapeHtml4(content);
                    String title = bookSection.getLabel() != null
                            ? StringEscapeUtils
                                    .unescapeHtml4(bookSection.getLabel())
                            : buildChapterTitle(content);
                    content = content.replaceAll(SPACEREGEX, "\n    ");
                    part = new Part(title, bookSection.getLabel(), content, i,
                            i);
                    parts.add(part);
                }

            } catch (OutOfPagesException e) {
                break;
            }
        }
        Set<Part> uniques = new TreeSet<Part>(parts);
        List<Part> fUniques = new ArrayList<Part>(uniques);
        for (int i = 0; i < fUniques.size(); i++) {
            fUniques.get(i).setRealIndex(i);
            fUniques.get(i).setIndex(i + 1);
        }
        book.setParts(fUniques);
        this.setBook(book);
        return book;
    }

    public ParsedBook buildAsyncEbook(String epubFilePath)
            throws ReadingException, InterruptedException, ExecutionException,
            TimeoutException {
        if (this.executor == null || this.executor.isShutdown()) {
            this.executor = Executors.newSingleThreadExecutor();
        }

        Reader reader = new Reader();
        reader.setFullContent(epubFilePath);
        reader.setMaxContentPerSection(100000);
        reader.setIsIncludingTextContent(true);
        Package pk = reader.getInfoPackage();
        String title = pk.getMetadata().getTitle() != null
                ? pk.getMetadata().getTitle() : "Title_unknown";
        List<Part> parts = new ArrayList<>();
        ParsedBook book = new ParsedBook();
        book.setTitle(title);
        List<Future<Part>> fut = asyncBuildToc(reader, this.executor);

        for (int i = 0; i < fut.size(); i++) {
            Part part = fut.get(i).get(45, TimeUnit.SECONDS);
            if (part != null && !(part.getContent() == null) && !part.getContent().isEmpty()) {
                parts.add(part);
            }

        }
        for (int i = 0; i < parts.size(); i++) {
            parts.get(i).setRealIndex(i);
            parts.get(i).setIndex(i + 1);
        }
        book.setParts(parts);
        this.setBook(book);
        flag.set(true);
        flagI.set(2000);
        return book;
    }

    private List<Future<Part>> asyncBuildToc(Reader reader,
            ExecutorService executor) {
        List<Future<Part>> futList = new ArrayList<>();

        for (int i = 0; i < flagI.get(); i++) {
            if (flag.get() && !Thread.currentThread().isInterrupted()) {
                final int j = i;
                futList.add(executor.submit(new Callable<Part>() {

                    @Override
                    public Part call() {
                        return asyncGetSection(j, reader, executor);

                    }
                }));
            }
        }
        return futList;
    }

//    private Part asyncGetSection(int idx, Reader reader,
//            ExecutorService executor) {
//        Part part = null;
//        // System.out.println("meth "+idx);
//        if (flag.get()) {
//            try {
//                BookSection bookSection = reader.readSection(idx);
//                if (bookSection.getLabel() != null || !bookSection
//                        .getSectionTextContent().trim().isEmpty()) {
//                    String content = bookSection.getSectionTextContent().trim()
//                            .replaceAll("(?s)<!--.*?-->", "");
//                    content = StringEscapeUtils.unescapeHtml4(content);
//                    String title = bookSection.getLabel() != null
//                            ? StringEscapeUtils
//                                    .unescapeHtml4(bookSection.getLabel())
//                            : buildChapterTitle(content);
//                    content = content.replaceAll(SPACEREGEX, "\n    ");
//                    part = new Part(title, bookSection.getLabel(), content, idx,
//                            idx);
//                    // System.out.println(
//                    // "meth " + part.getIndex() + " " + part.getTitle());
//                }
//            } catch (ReadingException e) {
//                e.printStackTrace();
//            } catch (OutOfPagesException e) {
//                executor.shutdown();
//                flag.set(false);
//                flagI.set(idx);
//            }
//        }
//        return part;
//    }
    
    private Part asyncGetSection(int idx, Reader reader,
            ExecutorService executor) {
        MicrosoftConditionalCommentTagTypes.register();
        PHPTagTypes.register();
        MasonTagTypes.register();
        Part part = null;
        if (flag.get()) {
            try {
                BookSection bookSection = reader.readSection(idx);
                if (!bookSection.getSectionContent().trim().isEmpty()) {    
                String content = bookSection.getSectionContent().trim();
                    
                    String label = bookSection.getLabel();
                    Source source = new Source(content);
                    source.setLogger(null);
                    Renderer renderer = source.getRenderer();
                    renderer.setDecorateFontStyles(true);
                    String title = label != null
                            ? label.trim()
                            : buildChapterTitle(renderer.toString());
                    part = new Part(title, label, renderer.toString(), idx,
                            idx);
                }
            } catch (ReadingException e) {
                e.printStackTrace();
            } catch (OutOfPagesException e) {
                executor.shutdown();
                flag.set(false);
                flagI.set(idx);
            }
        }
        return part;
    }

    private String buildChapterTitle(String content) {
        String[] cleanContent = content.split("\n", 2);
            return cleanContent[0].trim();
    }

    public String getBookToc(ParsedBook book) {
        StringBuilder sb = new StringBuilder();
        for (Part part : book.getParts()) {
            sb.append(part.getIndex()).append(" - ").append(part.getTitle())
                    .append("\n");

        }
        return sb.toString();
    }

    private void closeExecutor(ExecutorService executor) {
        if (executor != null) {
            try {
                System.out.println("shutting down...");
                executor.awaitTermination(20, TimeUnit.SECONDS);
                executor.shutdown();
            } catch (InterruptedException e) {
                System.err.println("tasks interrupted");
            } finally {
                if (!executor.isTerminated()) {
                    System.err.println("cancel non-finished tasks");
                }
                executor.shutdownNow();
                System.out.println("shutdown finished");
            }
        }
    }

    public void closereaderExecutor() {
        this.closeExecutor(this.executor);
    }

}
