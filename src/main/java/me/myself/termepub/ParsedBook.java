package me.myself.termepub;

import java.util.List;

public class ParsedBook {

    private List<Part> parts;
    private String title;

    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ParsedBook() {
    };

    public ParsedBook(List<Part> parts) {
        super();
        this.parts = parts;
    }

    public boolean addPart(Part part) {
        return this.parts.add(part);
    }

    public Part getChapter(int chapterNumber) {
        if (chapterNumber - 1 >= 0 && chapterNumber <= this.parts.size()) {
            return this.parts.get(chapterNumber - 1);
        } else {
            throw new IllegalArgumentException(
                    "Chapter number out of bounds, try another one");
        }

    }

}
